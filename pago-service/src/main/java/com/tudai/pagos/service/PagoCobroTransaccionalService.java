package com.tudai.pagos.service;
import com.tudai.pagos.entity.Pago;
import com.tudai.pagos.enums.EstadoPago;
import com.tudai.pagos.pasarela.PasarelaNoDisponibleException;
import com.tudai.pagos.pasarela.PasarelaPagos;
import com.tudai.pagos.pasarela.ResultadoCobro;
import com.tudai.pagos.pasarela.SolicitudCobro;
import com.tudai.pagos.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class PagoCobroTransaccionalService {

    private final PagoRepository pagoRepository;
    private final PasarelaPagos pasarela;
    private final Duration duracionIdempotencia;

    public PagoCobroTransaccionalService(
            PagoRepository pagoRepository,
            PasarelaPagos pasarela,
            @Value("${pagos.idempotencia.duracion-horas:24}") long duracionHoras) {

        this.pagoRepository = pagoRepository;
        this.pasarela = pasarela;
        this.duracionIdempotencia = Duration.ofHours(duracionHoras);
    }

    @Transactional
    public Pago cobrar(String claveIdempotencia, String hashRequest,
                       Long compraId, Long clienteId,
                       Integer monto, String moneda) {

        Optional<Pago> existente = pagoRepository
                .findByClaveIdempotenciaParaActualizar(claveIdempotencia);

        if (existente.isPresent()) {
            Pago pago = existente.get();

            if (!pago.claveIdempotenciaVencida(LocalDateTime.now())) {
                validarMismoPedido(pago, hashRequest);

                // Incluye APROBADO, RECHAZADO e INCIERTO:
                // el retry HTTP recibe exactamente el resultado registrado.
                return pago;
            }

            // Conserva el historial del pago, pero libera la clave vencida.
            pago.setClaveIdempotencia(null);
            pagoRepository.saveAndFlush(pago);
        }

        return crearYProcesar(
                claveIdempotencia, hashRequest,
                compraId, clienteId, monto, moneda);
    }

    /*
     * Se invoca si dos instancias no encontraron inicialmente el pago
     * y ambas intentaron crear la misma Idempotency-Key.
     *
     * La segunda inserción falla por la restricción UNIQUE luego de que
     * la primera transacción termina. Esta lectura toma el lock pesimista
     * y obtiene el resultado definitivo que dejó la ganadora.
     */
    @Transactional
    public Pago obtenerResultadoGanador(String claveIdempotencia,
                                        String hashRequest) {

        Pago pago = pagoRepository
                .findByClaveIdempotenciaParaActualizar(claveIdempotencia)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el pago ganador de la concurrencia"));

        validarMismoPedido(pago, hashRequest);
        return pago;
    }

    private Pago crearYProcesar(String claveIdempotencia, String hashRequest,
                                Long compraId, Long clienteId,
                                Integer monto, String moneda) {

        LocalDateTime ahora = LocalDateTime.now();

        Pago pago = new Pago(
                EstadoPago.PENDIENTE,
                moneda,
                monto,
                clienteId,
                hashRequest,
                claveIdempotencia,
                compraId,
                ahora,
                ahora.plus(duracionIdempotencia),
                null,
                null
        );

        /*
         * Fuerza el INSERT ahora.
         * Si otra instancia ganó la misma clave, falla aquí por el UNIQUE.
         */
        pago = pagoRepository.saveAndFlush(pago);

        try {
            ResultadoCobro resultado = pasarela.cobrar(
                    new SolicitudCobro(
                            pago.getClaveIdempotencia(),
                            pago.getMonto(),
                            pago.getMoneda()));

            switch (resultado.estado()) {
                case APROBADO -> {
                    pago.setEstado(EstadoPago.APROBADO);
                    pago.setReferencia(resultado.referencia());
                }
                case RECHAZADO -> {
                    pago.setEstado(EstadoPago.RECHAZADO);
                    pago.setMotivoRechazo(resultado.motivo());
                }
                case INCIERTO -> pago.setEstado(EstadoPago.INCIERTO);
            }

        } catch (PasarelaNoDisponibleException ex) {
            pago.setEstado(EstadoPago.INCIERTO);
        }

        return pagoRepository.save(pago);
    }

    private void validarMismoPedido(Pago pago, String hashRequest) {
        if (!Objects.equals(pago.getHashRequest(), hashRequest)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La clave de idempotencia ya se usó con un pedido distinto");
        }
    }
}
