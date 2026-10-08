package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
    }

    // Aquí iría el método de autenticación validando el passwordHash
    public Usuario autenticar(String username, String password) {
        // TODO: Implementar validación de hash (ej. BCrypt)
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
    }
}