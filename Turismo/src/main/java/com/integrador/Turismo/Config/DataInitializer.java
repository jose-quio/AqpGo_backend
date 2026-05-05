package com.integrador.Turismo.Config;

import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdmin() {
        return args -> {
            String adminEmail = "admin@aqpgo.com";

            if (usuarioRepository.existsByEmail(adminEmail)) {
                log.info("✅ Admin ya existe, no se crea de nuevo.");
                return;
            }

            Usuario admin = Usuario.builder()
                    .nombreCompleto("Administrador AqpGo")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin1234!"))
                    .rol(Usuario.Rol.ADMIN)
                    .build();

            usuarioRepository.save(admin);
            log.info("🚀 Admin de prueba creado:");
            log.info("   Email:    {}", adminEmail);
            log.info("   Password: Admin1234!");
        };
    }
}
