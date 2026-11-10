package com.tudai.pagos.util;

import com.tudai.pagos.dto.CobroRequest;
import com.tudai.pagos.dto.ReintegroRequest;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.security.MessageDigest;
public final class HashRequest {

    private HashRequest() {}

    /** SHA-256 de los campos del pedido. La clave de idempotencia NO entra en el hash. */
    public static String de(CobroRequest r) {
        String monedaNormalizada = (r.moneda() != null) ? r.moneda().trim().toUpperCase() : "ARS";
        String canonico = r.compraId() + "|" + r.clienteId() + "|" + r.monto() + "|" + monedaNormalizada;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonico.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    public static String de(ReintegroRequest r) {
        String canonico = r.pagoId() + "|" + r.monto() + "|" + r.motivo().name();
        // mismo SHA-256 que en el método existente
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonico.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
