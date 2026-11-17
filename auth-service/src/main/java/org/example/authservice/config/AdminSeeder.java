package org.example.authservice.config;

import org.example.authservice.entity.Rol;
import org.example.authservice.entity.Usuario;
import org.example.authservice.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Semilla del primer ADMINISTRADOR (necesario para el alta privilegiada).
 * La contraseña se toma de la configuración/entorno y NUNCA se loguea.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.admin.email:admin@ticketya.com}")
    private String adminEmail;

    @Value("${auth.admin.password:Admin123!}")
    private String adminPassword;

    @Value("${auth.admin.nombre:Administrador General}")
    private String adminNombre;

    public AdminSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByEmailIgnoreCase(adminEmail)) {
            log.info("El ADMINISTRADOR inicial ya existe, se omite la semilla");
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre(adminNombre);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRol(Rol.ADMINISTRADOR);
        usuarioRepository.save(admin);
        log.info("ADMINISTRADOR inicial creado para el email configurado (contraseña no registrada en logs)");
    }
}
