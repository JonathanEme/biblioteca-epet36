package com.epet36.biblioteca.config;

import com.epet36.biblioteca.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class SesionManager {

    private Usuario usuarioLogueado;

    public void iniciarSesion(Usuario usuario) {
        this.usuarioLogueado = usuario;
    }

    public void cerrarSesion() {
        this.usuarioLogueado = null;
    }

    public Usuario getUsuarioLogueado() {
        if (usuarioLogueado == null) {
            throw new IllegalStateException("No hay un usuario autenticado.");
        }
        return usuarioLogueado;
    }

    public boolean isAdmin() {
        return usuarioLogueado != null && "ADMIN".equalsIgnoreCase(usuarioLogueado.getRol());
    }
}