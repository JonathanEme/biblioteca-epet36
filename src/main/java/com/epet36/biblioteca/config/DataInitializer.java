package com.epet36.biblioteca.config;

import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component // <-- ¡Muy importante para que Spring la ejecute!
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        // Verificamos si la base de datos está vacía
        if (usuarioRepository.findByUsername("admin").isEmpty()) {

            // Creamos al administrador por defecto
            Usuario admin = Usuario.builder()
                    .username("admin")
                    .passwordHash("123456") // En un futuro lo encriptaremos
                    .nombre("Administrador Principal")
                    .rol("ADMIN")
                    .activo(true)
                    .build();

            usuarioRepository.save(admin);
            System.out.println("✅ IMPORTANTE: Usuario 'admin' inyectado en la base de datos exitosamente.");
        }
    }
}