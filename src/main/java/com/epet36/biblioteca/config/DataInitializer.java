package com.epet36.biblioteca.config;

import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioService usuarioService;

    @Override
    public void run(String... args) {
        // Verificamos si existe el admin a través del servicio
        try {
            usuarioService.obtenerUsuarioPorDefecto();
        } catch (IllegalStateException e) {
            // Si salta la excepción indicando que no existe, lo creamos
            Usuario admin = Usuario.builder()
                    .username("admin")
                    .passwordHash("123456") // En un futuro lo encriptaremos (ej: BCrypt)
                    .nombre("Administrador Principal")
                    .rol("ADMIN")
                    .activo(true)
                    .build();

            // Usamos el servicio para guardar en SQLite Y generar el evento Outbox hacia Turso
            usuarioService.guardar(admin);
            log.info("✅ Usuario 'admin' inyectado localmente y encolado para sincronización en Outbox.");
        }
    }
}