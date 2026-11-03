package com.epet36.biblioteca.config;

import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    // CAMBIO VITAL: Inyectamos el Repositorio directamente en lugar del Servicio
    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {

        // Verificamos de forma segura si el admin ya existe en SQLite
        Optional<Usuario> adminExistente = usuarioRepository.findByUsername("admin");

        if (adminExistente.isEmpty()) {
            Usuario admin = Usuario.builder()
                    .id("admin-user-0000-0000-000000000000") // <--- UUID FIJO Y ETERNO
                    .username("admin")
                    .passwordHash("123456")
                    .nombre("Administrador Principal")
                    .rol("ADMIN")
                    .activo(true)
                    .build();

            usuarioRepository.save(admin);
            log.info("✅ Usuario 'admin' inyectado localmente de forma silenciosa.");
            }
         else {
            log.info("ℹ️ El usuario 'admin' ya se encuentra configurado en la base de datos local.");
        }
    }
}