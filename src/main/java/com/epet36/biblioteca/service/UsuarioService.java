package com.epet36.biblioteca.service;

import com.epet36.biblioteca.model.OutboxEvent;
import com.epet36.biblioteca.model.Usuario;
import com.epet36.biblioteca.repository.OutboxEventRepository;
import com.epet36.biblioteca.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // --- NUEVO MÉTODO PARA LISTAR USUARIOS ---
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    // CAMBIO: id ahora es String (UUID)
    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
    }

    public Optional<Usuario> buscarPorIdOpcional(String id) {
        return usuarioRepository.findById(id);
    }

    public Usuario obtenerUsuarioPorDefecto() {
        return usuarioRepository.findByUsername("admin")
                .orElseThrow(() -> new IllegalStateException("El usuario 'admin' no existe en la base de datos."));
    }

    public Usuario autenticar(String username, String password) {
        // TODO: Implementar validación de hash (ej. BCrypt)
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
    }

    // Método para crear o actualizar usuarios y registrar el evento Outbox
    @Transactional
    public Usuario guardar(Usuario usuario) {
        boolean esNuevo = (usuario.getId() == null || buscarPorIdOpcional(usuario.getId()).isEmpty());

        if (esNuevo && usuario.getId() == null) {
            usuario.setId(UUID.randomUUID().toString());
        }

        // Validación de unicidad de username
        var existente = usuarioRepository.findByUsername(usuario.getUsername());
        if (existente.isPresent() && !existente.get().getId().equals(usuario.getId())) {
            throw new IllegalStateException("Ya existe un usuario con el username: " + usuario.getUsername());
        }

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // --- REGISTRAMOS EL EVENTO OUTBOX ---
        registrarEventoOutbox(usuarioGuardado, esNuevo ? "INSERT" : "UPDATE");

        return usuarioGuardado;
    }

    // --- MÉTODO AUXILIAR PARA EL OUTBOX ---
    private void registrarEventoOutbox(Usuario usuario, String operacion) {
        try {
            // Opcional: Podrías evitar enviar el passwordHash a la nube si por seguridad prefieres
            // que la autenticación sea estrictamente local, o enviarlo si los usuarios pueden loguearse en cualquier máquina.
            String jsonPayload = objectMapper.writeValueAsString(usuario);

            OutboxEvent evento = OutboxEvent.builder()
                    .aggregateType("Usuario")
                    .aggregateId(usuario.getId())
                    .operation(operacion)
                    .payload(jsonPayload)
                    .build();

            outboxRepository.save(evento);
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar el evento Outbox para el usuario: " + usuario.getUsername(), e);
        }
    }
}