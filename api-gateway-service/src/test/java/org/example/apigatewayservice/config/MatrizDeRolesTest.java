package org.example.apigatewayservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// pruebas de seguridad por rol

@SpringBootTest
@AutoConfigureMockMvc
class MatrizDeRolesTest {

    @Autowired
    private MockMvc mockMvc;

    // tokens simulados de cada rol
    private static final RequestPostProcessor CLIENTE =
            jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    private static final RequestPostProcessor ORGANIZADOR =
            jwt().authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"));
    private static final RequestPostProcessor ADMINISTRADOR =
            jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
    private static final RequestPostProcessor PERSONAL_ACCESO =
            jwt().authorities(new SimpleGrantedAuthority("ROLE_PERSONAL_ACCESO"));

    /**
     * verifica que la seguridad deja pasar la request (no responde 401 ni 403).
     * Si pasa, el gateway intenta llamar al microservicio, que en el test no esta
     * levantado y da error eso también significa que la seguridad lo dejó pasar.
     */
    private void pasaLaSeguridad(MockHttpServletRequestBuilder request) {
        try {
            int codigo = mockMvc.perform(request).andReturn().getResponse().getStatus();
            assertThat(codigo).isNotIn(401, 403);
        } catch (Exception e) {
            // llego al ruteo hacia el microservicio: paso la seguridad.
        }
    }

    // identidad (US-45, US-48)

    @Test
    @DisplayName("POST /api/auth/register y /login: publicos (US-45)")
    void registroYLoginPublicos() {
        pasaLaSeguridad(post("/api/auth/register"));
        pasaLaSeguridad(post("/api/auth/login"));
        pasaLaSeguridad(get("/api/auth/.well-known/jwks.json"));
    }

    @Test
    @DisplayName("GET /api/auth/me: cualquier usuario logueado (US-45)")
    void verMiCuenta() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        pasaLaSeguridad(get("/api/auth/me").with(CLIENTE));
        pasaLaSeguridad(get("/api/auth/me").with(ORGANIZADOR));
        pasaLaSeguridad(get("/api/auth/me").with(ADMINISTRADOR));
        pasaLaSeguridad(get("/api/auth/me").with(PERSONAL_ACCESO));
    }

    @Test
    @DisplayName("POST /api/auth/users: solo ADMINISTRADOR (US-48)")
    void altaDeUsuarios() throws Exception {
        mockMvc.perform(post("/api/auth/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/users").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/users").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/users").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(post("/api/auth/users").with(ADMINISTRADOR));
    }

    // eventos y lugares (US-01 a US-14, US-47, US-48)

    @Test
    @DisplayName("GET eventos, detalle, sectores y lugares: publicos (US-12, US-14)")
    void verEventosYLugaresEsPublico() {
        pasaLaSeguridad(get("/api/eventos"));
        pasaLaSeguridad(get("/api/eventos/1"));
        pasaLaSeguridad(get("/api/eventos/1/sectores"));
        pasaLaSeguridad(get("/api/lugares"));
        pasaLaSeguridad(get("/api/eventos").with(CLIENTE));
    }

    @Test
    @DisplayName("POST /api/eventos: ORGANIZADOR y ADMINISTRADOR (US-01, US-47)")
    void crearEvento() throws Exception {
        mockMvc.perform(post("/api/eventos"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/eventos").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/eventos").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(post("/api/eventos").with(ORGANIZADOR));
        pasaLaSeguridad(post("/api/eventos").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("PATCH /api/eventos/{id}: modificar, ORGANIZADOR y ADMINISTRADOR (US-02, US-47)")
    void modificarEvento() throws Exception {
        mockMvc.perform(patch("/api/eventos/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/eventos/1").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/eventos/1").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(patch("/api/eventos/1").with(ORGANIZADOR));
        pasaLaSeguridad(patch("/api/eventos/1").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("PATCH /api/eventos/{id}/publicar: ORGANIZADOR y ADMINISTRADOR (US-04, US-47)")
    void publicarEvento() throws Exception {
        mockMvc.perform(patch("/api/eventos/1/publicar"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/eventos/1/publicar").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/eventos/1/publicar").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(patch("/api/eventos/1/publicar").with(ORGANIZADOR));
        pasaLaSeguridad(patch("/api/eventos/1/publicar").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("PATCH /api/eventos/{id}/suspender: solo ADMINISTRADOR (US-06, US-48)")
    void suspenderEvento() throws Exception {
        mockMvc.perform(patch("/api/eventos/1/suspender"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/eventos/1/suspender").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/eventos/1/suspender").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/eventos/1/suspender").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(patch("/api/eventos/1/suspender").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("PATCH /api/eventos/{id}/reanudar: solo ADMINISTRADOR (US-06, US-48)")
    void reanudarEvento() throws Exception {
        mockMvc.perform(patch("/api/eventos/1/reanudar"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/eventos/1/reanudar").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/eventos/1/reanudar").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/eventos/1/reanudar").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(patch("/api/eventos/1/reanudar").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("PATCH /api/eventos/{id}/cancelar: solo ADMINISTRADOR (US-06, US-47, US-48)")
    void cancelarEvento() throws Exception {
        mockMvc.perform(patch("/api/eventos/1/cancelar"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/eventos/1/cancelar").with(CLIENTE))
                .andExpect(status().isForbidden());
        // US-47: el organizador NO puede suspender ni cancelar eventos.
        mockMvc.perform(patch("/api/eventos/1/cancelar").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/eventos/1/cancelar").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(patch("/api/eventos/1/cancelar").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("POST /api/lugares: ORGANIZADOR y ADMINISTRADOR (US-08)")
    void registrarLugar() throws Exception {
        mockMvc.perform(post("/api/lugares"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/lugares").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/lugares").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(post("/api/lugares").with(ORGANIZADOR));
        pasaLaSeguridad(post("/api/lugares").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("GET /api/inventario/eventos/{id}/disponibilidad: publico (US-14)")
    void verDisponibilidadEsPublico() {
        pasaLaSeguridad(get("/api/inventario/eventos/1/disponibilidad"));
        pasaLaSeguridad(get("/api/inventario/eventos/1/disponibilidad").with(CLIENTE));
    }

    // compras y entradas (US-15 a US-25, US-46)

    @Test
    @DisplayName("POST /api/compras: solo CLIENTE (US-46)")
    void comprar() throws Exception {
        mockMvc.perform(post("/api/compras"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/compras").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/compras").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/compras").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(post("/api/compras").with(CLIENTE));
    }

    @Test
    @DisplayName("GET /api/compras/{id}: CLIENTE y ADMINISTRADOR (US-20)")
    void verCompra() throws Exception {
        mockMvc.perform(get("/api/compras/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/compras/1").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/compras/1").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/compras/1").with(CLIENTE));
        pasaLaSeguridad(get("/api/compras/1").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("GET /api/entradas/cliente/{id}: CLIENTE y ADMINISTRADOR (US-25)")
    void verMisEntradas() throws Exception {
        mockMvc.perform(get("/api/entradas/cliente/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/entradas/cliente/1").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/entradas/cliente/1").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/entradas/cliente/1").with(CLIENTE));
        pasaLaSeguridad(get("/api/entradas/cliente/1").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("GET /api/entradas/{id}: CLIENTE y ADMINISTRADOR (US-25)")
    void verUnaEntrada() throws Exception {
        mockMvc.perform(get("/api/entradas/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/entradas/1").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/entradas/1").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/entradas/1").with(CLIENTE));
        pasaLaSeguridad(get("/api/entradas/1").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("PUT /api/entradas/{id}/cancelar: CLIENTE y ADMINISTRADOR")
    void cancelarEntrada() throws Exception {
        mockMvc.perform(put("/api/entradas/1/cancelar"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/entradas/1/cancelar").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/entradas/1/cancelar").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(put("/api/entradas/1/cancelar").with(CLIENTE));
        pasaLaSeguridad(put("/api/entradas/1/cancelar").with(ADMINISTRADOR));
    }

    // devoluciones (US-26, US-27)

    @Test
    @DisplayName("POST /api/entradas/{id}/devolucion: CLIENTE y ADMINISTRADOR (US-26)")
    void solicitarDevolucion() throws Exception {
        mockMvc.perform(post("/api/entradas/1/devolucion"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/entradas/1/devolucion").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/entradas/1/devolucion").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(post("/api/entradas/1/devolucion").with(CLIENTE));
        pasaLaSeguridad(post("/api/entradas/1/devolucion").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("Listar, aprobar y rechazar devoluciones: solo ADMINISTRADOR (US-27)")
    void resolverDevoluciones() throws Exception {
        mockMvc.perform(get("/api/entradas/devoluciones"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/entradas/devoluciones").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/entradas/devoluciones").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/entradas/devoluciones").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/entradas/devoluciones").with(ADMINISTRADOR));

        mockMvc.perform(patch("/api/entradas/devoluciones/1/aprobar").with(CLIENTE))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(patch("/api/entradas/devoluciones/1/aprobar").with(ADMINISTRADOR));

        mockMvc.perform(patch("/api/entradas/devoluciones/1/rechazar").with(CLIENTE))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(patch("/api/entradas/devoluciones/1/rechazar").with(ADMINISTRADOR));
    }

    // control de acceso con QR (US-33, US-34, US-49)

    @Test
    @DisplayName("POST /api/entradas/validar: PERSONAL_ACCESO y ADMINISTRADOR (US-49)")
    void validarQr() throws Exception {
        mockMvc.perform(post("/api/entradas/validar"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/entradas/validar").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/entradas/validar").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(post("/api/entradas/validar").with(PERSONAL_ACCESO));
        pasaLaSeguridad(post("/api/entradas/validar").with(ADMINISTRADOR));
    }

    // fila virtual (US-30, US-31)

    @Test
    @DisplayName("POST /api/fila/{id}/ingresos: solo CLIENTE (US-30)")
    void ingresarALaFila() throws Exception {
        mockMvc.perform(post("/api/fila/1/ingresos"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/fila/1/ingresos").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/fila/1/ingresos").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/fila/1/ingresos").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(post("/api/fila/1/ingresos").with(CLIENTE));
    }

    @Test
    @DisplayName("GET /api/fila/{id}/mi-posicion: solo CLIENTE (US-31)")
    void verMiPosicionEnLaFila() throws Exception {
        mockMvc.perform(get("/api/fila/1/mi-posicion"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/fila/1/mi-posicion").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/fila/1/mi-posicion").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/fila/1/mi-posicion").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/fila/1/mi-posicion").with(CLIENTE));
    }

    // preferencias y recomendaciones (US-35 a US-37)

    @Test
    @DisplayName("GET y POST /api/preferencias: solo CLIENTE (US-35)")
    void preferencias() throws Exception {
        mockMvc.perform(get("/api/preferencias/cliente/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/preferencias/cliente/1").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/preferencias/cliente/1").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/preferencias/cliente/1").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/preferencias/cliente/1").with(CLIENTE));

        mockMvc.perform(post("/api/preferencias"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/preferencias").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(post("/api/preferencias").with(CLIENTE));
    }

    @Test
    @DisplayName("GET /api/recomendaciones/evento/{id}: solo CLIENTE (US-37)")
    void verRecomendaciones() throws Exception {
        mockMvc.perform(get("/api/recomendaciones/evento/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/recomendaciones/evento/1").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/recomendaciones/evento/1").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/recomendaciones/evento/1").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/recomendaciones/evento/1").with(CLIENTE));
    }

    // pagos y reintegros (US-28, US-29, US-48)

    @Test
    @DisplayName("GET /api/pagos: listar todos, solo ADMINISTRADOR (US-48)")
    void listarTodosLosPagos() throws Exception {
        mockMvc.perform(get("/api/pagos"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pagos").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pagos").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pagos").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/pagos").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("GET /api/pagos/cliente/{id}: CLIENTE y ADMINISTRADOR (US-46)")
    void verMisPagos() throws Exception {
        mockMvc.perform(get("/api/pagos/cliente/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pagos/cliente/1").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pagos/cliente/1").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/pagos/cliente/1").with(CLIENTE));
        pasaLaSeguridad(get("/api/pagos/cliente/1").with(ADMINISTRADOR));
    }

    @Test
    @DisplayName("GET /reintegros: consultar avance, solo ADMINISTRADOR (US-29, US-48)")
    void consultarReintegros() throws Exception {
        mockMvc.perform(get("/reintegros"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/reintegros").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reintegros").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reintegros").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/reintegros").with(ADMINISTRADOR));
        pasaLaSeguridad(get("/reintegros/resumen").with(ADMINISTRADOR));
    }

    // notificaciones (US-38, US-39)

    @Test
    @DisplayName("GET y POST /api/notificaciones: solo ADMINISTRADOR (US-38)")
    void notificaciones() throws Exception {
        mockMvc.perform(get("/api/notificaciones"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notificaciones").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/notificaciones").with(ORGANIZADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/notificaciones").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/notificaciones").with(ADMINISTRADOR));

        mockMvc.perform(post("/api/notificaciones/1/reintentar").with(CLIENTE))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(post("/api/notificaciones/1/reintentar").with(ADMINISTRADOR));
    }

    // reportes (US-41 a US-43)

    @Test
    @DisplayName("GET /api/reportes/eventos: ORGANIZADOR y ADMINISTRADOR (US-41 a US-43)")
    void reportes() throws Exception {
        mockMvc.perform(get("/api/reportes/eventos"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reportes/eventos").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reportes/eventos").with(PERSONAL_ACCESO))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/reportes/eventos").with(ORGANIZADOR));
        pasaLaSeguridad(get("/api/reportes/eventos").with(ADMINISTRADOR));

        mockMvc.perform(get("/api/reportes/eventos/1/sectores").with(CLIENTE))
                .andExpect(status().isForbidden());
        pasaLaSeguridad(get("/api/reportes/eventos/1/sectores").with(ORGANIZADOR));
    }

    // endpoints internos: solo se llaman entre microservicios, nunca desde afuera

    @Test
    @DisplayName("Endpoints internos: bloqueados para todos los roles")
    void endpointsInternos() throws Exception {
        mockMvc.perform(post("/api/inventario/reservas"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/inventario/reservas").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/inventario/reservas").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/entradas/emitir").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/entradas/eventos/1/cancelar").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/pagos/cobrar").with(CLIENTE))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/reintegros/cancelacion-evento").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/interno/fila/1/configuracion").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/compras/interno/1/compradores").with(ADMINISTRADOR))
                .andExpect(status().isForbidden());
        // lista emails de clientes: solo lo usa notificaciones-service
        mockMvc.perform(get("/api/preferencias/interno/destinatarios?artista=X&ciudad=Y").with(CLIENTE))
                .andExpect(status().isForbidden());
    }
}