package org.example.apigatewayservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad del punto de entrada único.
 *
 * <p>Verifican que sin token la respuesta es 401, que un rol insuficiente
 * obtiene 403 y que los endpoints internos no son alcanzables ni siquiera
 * con un token de administrador.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Sin token devuelve 401")
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/compras"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un CLIENTE no puede dar de alta usuarios")
    void clienteNoPuedeCrearUsuarios() throws Exception {
        mockMvc.perform(post("/api/auth/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un ORGANIZADOR no puede validar entradas en la puerta")
    void organizadorNoPuedeValidarEntradas() throws Exception {
        mockMvc.perform(post("/api/entradas/validar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un CLIENTE no puede publicar eventos")
    void clienteNoPuedePublicarEventos() throws Exception {
        mockMvc.perform(patch("/api/eventos/1/publicar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Solo un ADMINISTRADOR puede suspender eventos")
    void organizadorNoPuedeSuspenderEventos() throws Exception {
        mockMvc.perform(patch("/api/eventos/1/suspender")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Los endpoints internos quedan bloqueados incluso para ADMINISTRADOR")
    void endpointsInternosBloqueados() throws Exception {
        mockMvc.perform(get("/api/eventos/interno/1/notificacion")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/compras/interno/1/compradores")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un usuario autenticado atraviesa la seguridad y llega al ruteo")
    void usuarioAutenticadoPasaLaSeguridad() throws Exception {
        mockMvc.perform(get("/ruta-que-no-existe")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isNotFound());
    }
}
