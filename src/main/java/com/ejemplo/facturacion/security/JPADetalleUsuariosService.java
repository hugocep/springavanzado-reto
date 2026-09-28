package com.ejemplo.facturacion.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.ejemplo.facturacion.entities.Usuario;
import com.ejemplo.facturacion.repositories.UsuarioRepository;

@Component
public class JPADetalleUsuariosService implements UserDetailsService {
    @Autowired UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        Optional<Usuario> usuario = usuarioRepository.findByNombreUsuario(nombreUsuario);

        if (usuario.isPresent()) {
            return new JPADetalleUsuarios(usuario.get());
        } else {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
    }
}
