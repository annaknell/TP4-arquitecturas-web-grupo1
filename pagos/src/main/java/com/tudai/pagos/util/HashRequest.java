package com.tudai.pagos.util;

import com.tudai.pagos.dto.CobroRequest;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.security.MessageDigest;
public final class HashRequest {

    private HashRequest() {}

    /** SHA-256 de los campos del pedido. La clave de idempotencia NO entra en el hash. */
    public static String de(CobroRequest r) {
        String canonico = r.compraId() + "|" + r.clienteId() + "|" + r.monto() + "|"
                + r.moneda().trim().toUpperCase();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonico.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
