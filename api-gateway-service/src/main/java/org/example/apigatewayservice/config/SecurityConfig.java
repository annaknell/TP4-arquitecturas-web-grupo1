package org.example.apigatewayservice.config;

import org.example.apigatewayservice.filter.IdentityHeadersFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.util.List;

/**
 * Seguridad del punto de entrada único.
 *
 * <p>El gateway valida la firma y el vencimiento del JWT contra el JWKS que
 * publica {@code auth-service} y aplica la autorización por rol antes de
 * rutear. Los microservicios quedan dentro de la red interna: en el compose
 * solo se publica este puerto.</p>
 *
 * <p>La identidad del cliente nunca se toma de los headers entrantes: se
 * descarta y se reconstruye desde los claims en {@link IdentityHeadersFilter}.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${auth.jwk-set-uri:http://localhost:8081/api/auth/.well-known/jwks.json}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Infraestructura del propio gateway
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints internos: no se exponen por el gateway
                        .requestMatchers("/api/interno/**").denyAll()
                        .requestMatchers("/api/eventos/interno/**", "/api/compras/interno/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/entradas/emitir", "/api/entradas/eventos/*/cancelar").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/cobrar").denyAll()
                        .requestMatchers(HttpMethod.POST, "/reintegros/**").denyAll()
                        .requestMatchers("/pagos/mock/**").denyAll()

                        // Autenticación
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/.well-known/jwks.json").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/users").hasRole("ADMINISTRADOR")

                        // Catálogo: lectura pública, escritura de organizadores/administradores
                        .requestMatchers(HttpMethod.GET, "/api/eventos/**", "/api/lugares/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/eventos", "/api/lugares")
                        .hasAnyRole("ORGANIZADOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/eventos/*/suspender", "/api/eventos/*/reanudar")
                        .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/eventos/**")
                        .hasAnyRole("ORGANIZADOR", "ADMINISTRADOR")

                        // Compras
                        .requestMatchers(HttpMethod.POST, "/api/compras").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/compras/**").hasAnyRole("CLIENTE", "ADMINISTRADOR")

                        // Entradas: el personal de acceso valida QR; el cliente consulta las propias
                        .requestMatchers(HttpMethod.POST, "/api/entradas/validar")
                        .hasAnyRole("PERSONAL_ACCESO", "ADMINISTRADOR")
                        .requestMatchers("/api/entradas/**").hasAnyRole("CLIENTE", "ADMINISTRADOR")

                        // Fila virtual
                        .requestMatchers("/api/fila/**").hasRole("CLIENTE")

                        // Preferencias y recomendaciones
                        .requestMatchers("/api/preferencias/**", "/api/recomendaciones/**").hasRole("CLIENTE")

                        // Pagos y reintegros
                        .requestMatchers(HttpMethod.GET, "/api/pagos").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/pagos/**").hasAnyRole("CLIENTE", "ADMINISTRADOR")
                        .requestMatchers("/reintegros/**").hasRole("ADMINISTRADOR")

                        // Inventario: disponibilidad pública, operaciones internas bloqueadas
                        .requestMatchers(HttpMethod.GET, "/api/inventario/eventos/*/disponibilidad").permitAll()
                        .requestMatchers("/api/inventario/**").denyAll()

                        // Notificaciones: backoffice
                        .requestMatchers("/api/notificaciones/**").hasRole("ADMINISTRADOR")

                        // Reportes
                        .requestMatchers("/api/reportes/interno/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/reportes/**").hasAnyRole("ADMINISTRADOR", "ORGANIZADOR")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterAfter(new IdentityHeadersFilter(), AuthorizationFilter.class);

        return http.build();
    }

    /**
     * Convierte el claim {@code role} del JWT en una autoridad {@code ROLE_<rol>}.
     */
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            String role = jwt.getClaimAsString("role");
            List<GrantedAuthority> authorities = (role == null || role.isBlank())
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + role));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }
}
