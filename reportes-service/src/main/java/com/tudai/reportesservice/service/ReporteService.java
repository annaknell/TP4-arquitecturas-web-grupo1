package com.tudai.reportesservice.service;
import java.util.*;
import com.tudai.reportesservice.dto.ReporteEventoResponse;
import com.tudai.reportesservice.dto.ReporteSectorResponse;
import com.tudai.reportesservice.entity.ReporteEvento;
import com.tudai.reportesservice.entity.ReporteSector;
import com.tudai.reportesservice.repository.ReporteEventoRepository;
import com.tudai.reportesservice.repository.ReporteSectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
@Service
public class ReporteService {
    private final ReporteEventoRepository eventos;
    private final ReporteSectorRepository sectores;
    public ReporteService(ReporteEventoRepository e, ReporteSectorRepository s){
        eventos=e;
        sectores=s;
    }
    @Transactional
    @CacheEvict(value = {"reportes-evento", "reportes-sectores", "reportes-listado"}, allEntries = true)
    public ReporteEvento configurar(Long id, Long organizador, String nombre, String moneda, List<SectorConfig> datos){
        ReporteEvento e=eventos.findById(id).orElseGet(ReporteEvento::new);
        e.setEventoId(id);
        e.setOrganizadorId(organizador);
        e.setNombre(nombre);
        e.setMoneda(moneda == null || moneda.isBlank() ? "ARS" : moneda);
        Map<Long, ReporteSector> actuales=new HashMap<>();
        e.getSectores().forEach(s->actuales.put(s.getSectorId(),s));
        for(var d:datos){
            ReporteSector s=actuales.get(d.sectorId());
            if(s==null){
                s=new ReporteSector();
                s.setEvento(e);
                s.setSectorId(d.sectorId());
                e.getSectores().add(s);
            }
            s.setNombre(d.nombre());
            s.setCapacidad(d.capacidad());
        }
        e.actualizar();
        return eventos.save(e);
    }

    @Transactional
    @CacheEvict(value = {"reportes-evento", "reportes-sectores", "reportes-listado"}, allEntries = true)
    public void venta(Long evento, Long sector, long importe){
        sector(evento,sector).registrarVenta(importe);
    }

    @Transactional
    @CacheEvict(value = {"reportes-evento", "reportes-sectores", "reportes-listado"}, allEntries = true)
    public void reintegro(Long evento, Long sector, long importe, boolean aplicado){
        if(aplicado)sector(evento,sector).registrarReintegroAplicado(importe);
        else sector(evento,sector).registrarPendiente(importe);
    }

    @Cacheable(value = "reportes-sectores", key = "#evento")
    public List<ReporteSectorResponse> sectores(Long evento){
        return sectores.findByEventoEventoId(evento).stream().map(this::sectorResponse).toList();
    }

    @Cacheable("reportes-listado")
    public List<ReporteEventoResponse> listar(){
        return eventos.findAll().stream()
                .map(evento -> resumen(evento.getEventoId()))
                .toList();
    }
    @Cacheable(value = "reportes-listado", key = "#organizadorId")
    public List<ReporteEventoResponse> listarPorOrganizador(Long organizadorId) {
        return eventos.findAll().stream()
                .filter(evento -> organizadorId.equals(evento.getOrganizadorId()))
                .map(evento -> resumen(evento.getEventoId()))
                .toList();
    }

    @Cacheable(value = "reportes-evento", key = "#id")
    public ReporteEventoResponse resumen(Long id){
        ReporteEvento e=evento(id);
        var ss=e.getSectores().stream().map(this::sectorResponse).toList();
        long v=ss.stream().mapToLong(ReporteSectorResponse::vendidas).sum(),d=ss.stream().mapToLong(ReporteSectorResponse::devueltas).sum(),valid=ss.stream().mapToLong(ReporteSectorResponse::entradasValidas).sum(),b=ss.stream().mapToLong(ReporteSectorResponse::importeBrutoCentavos).sum(),r=ss.stream().mapToLong(ReporteSectorResponse::importeReintegradoCentavos).sum(),p=ss.stream().mapToLong(ReporteSectorResponse::importePendienteCentavos).sum();
        int c=ss.stream().mapToInt(ReporteSectorResponse::capacidad).sum();
        return new ReporteEventoResponse(id,e.getNombre(),e.getMoneda(),v,d,valid,c,b,r,p,b-r,c==0?0:valid*100d/c,e.getActualizadoEn(),ss);
    }

    public ReporteEvento evento(Long id){
        return eventos.findById(id).orElseThrow(()->new NoSuchElementException("Evento sin proyección"));
    }

    private ReporteSector sector(Long e, Long s){
        return sectores.findByEventoEventoIdAndSectorId(e,s).orElseThrow(()->new NoSuchElementException("Sector sin proyección"));
    }

    private ReporteSectorResponse sectorResponse(ReporteSector s){
        return new ReporteSectorResponse(s.getSectorId(),s.getNombre(),s.getVendidas(),s.getDevueltas(),s.getEntradasValidas(),s.getCapacidad(),s.getCapacidad()==0?0:s.getEntradasValidas()*100d/s.getCapacidad(),s.getImporteBrutoCentavos(),s.getImporteReintegradoCentavos(),s.getImportePendienteCentavos(),s.getImporteNetoCentavos());
    }

    public record SectorConfig(Long sectorId, String nombre, int capacidad){ }
}
