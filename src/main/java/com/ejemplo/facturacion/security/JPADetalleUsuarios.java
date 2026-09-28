package com.ejemplo.facturacion.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ejemplo.facturacion.entities.Usuario;

public class JPADetalleUsuarios implements UserDetails  {
    private final Usuario usuario;

    public JPADetalleUsuarios(Usuario usuario) {
      this.usuario = usuario;
    }

    @Override
    public String getUsername() {
        return usuario.getNombreUsuario();
    }

    @Override
    public String getPassword() {
        return usuario.getContraseña();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(usuario::getAutoridad);
    }
}

