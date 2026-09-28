package com.ejemplo.facturacion.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ejemplo.facturacion.security.JPADetalleUsuariosService;
import com.ejemplo.facturacion.security.JwtService;
import com.ejemplo.facturacion.valueobjects.Credenciales;

@RestController
public class AutorizacionController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JPADetalleUsuariosService detalleUsuariosService;

    @Autowired
    JwtService jwtService;

    @PostMapping("/autenticar")
    public String autenticar(@RequestBody Credenciales credenciales) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credenciales.getUsuario(), credenciales.getContraseña()));
        final UserDetails detalleUsuario = detalleUsuariosService.loadUserByUsername(credenciales.getUsuario());
        return jwtService.generarToken(detalleUsuario.getUsername());
    }
}
