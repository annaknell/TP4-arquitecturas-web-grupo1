package org.example.e2e;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Sistema {

    public static final String URL = System.getProperty("ticketYa.url", "http://localhost:8080");
    public static final String ADMIN = "admin@ticketya.com";
    public static final String ADMIN_PASS = "Admin123!";

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private Sistema() {
    }

    public record Respuesta(int codigo, String cuerpo) {

        public String texto(String patron) {
            Matcher m = Pattern.compile(patron).matcher(cuerpo);
            return m.find() ? m.group(1) : null;
        }

        public long numero(String patron) {
            String valor = texto(patron);
            if (valor == null) {
                throw new AssertionError("No se encontro '" + patron + "' en: " + cuerpo);
            }
            return Long.parseLong(valor);
        }
    }

    public static Respuesta pedir(String metodo, String ruta, String cuerpo, String token,
                                  Map<String, String> cabeceras) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(URL + ruta))
                .timeout(Duration.ofSeconds(120))
                .header("Accept", "application/json");
        if (cuerpo != null) {
            builder.header("Content-Type", "application/json");
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (cabeceras != null) {
            cabeceras.forEach(builder::header);
        }
        builder.method(metodo, cuerpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(cuerpo));
        try {
            HttpResponse<String> respuesta = CLIENTE.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Respuesta(respuesta.statusCode(), respuesta.body() == null ? "" : respuesta.body());
        } catch (Exception e) {
            throw new IllegalStateException("Fallo " + metodo + " " + ruta + ": " + e.getMessage(), e);
        }
    }

    public static Respuesta get(String ruta, String token) {
        return pedir("GET", ruta, null, token, null);
    }

    public static Respuesta post(String ruta, String cuerpo, String token) {
        return pedir("POST", ruta, cuerpo, token, null);
    }

    public static Respuesta post(String ruta, String cuerpo, String token, Map<String, String> cabeceras) {
        return pedir("POST", ruta, cuerpo, token, cabeceras);
    }

    public static Respuesta patch(String ruta, String token) {
        return pedir("PATCH", ruta, null, token, null);
    }

    public static boolean stackArriba() {
        try {
            return get("/api/eventos", null).codigo() == 200;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static String login(String email, String password) {
        Respuesta r = post("/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", null);
        if (r.codigo() != 200) {
            throw new AssertionError("Login de " + email + " devolvio " + r.codigo() + ": " + r.cuerpo());
        }
        return r.texto("\"token\":\"([^\"]+)\"");
    }

    public static String tokenAdmin() {
        return login(ADMIN, ADMIN_PASS);
    }

    public static String email(String prefijo) {
        return prefijo + "-" + System.nanoTime() + "@ticketya.com";
    }

    public static String registrarCliente(String email) {
        Respuesta r = post("/api/auth/register",
                "{\"nombre\":\"E2E\",\"email\":\"" + email + "\",\"password\":\"E2Epass123!\"}", null);
        if (r.codigo() != 201 && r.codigo() != 200) {
            throw new AssertionError("Registro devolvio " + r.codigo() + ": " + r.cuerpo());
        }
        return login(email, "E2Epass123!");
    }

    public static Map<String, String> idempotencia(String clave) {
        Map<String, String> cabeceras = new LinkedHashMap<>();
        cabeceras.put("Idempotency-Key", clave);
        return cabeceras;
    }
}
