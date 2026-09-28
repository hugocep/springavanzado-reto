package com.ejemplo.facturacion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

@Component
public class JwtService {
    @Value("${jwt.service.key}")
    private String secretoJwt;

    public String generarToken(String nombreUsuario) {
        Map<String, Object> concesiones = new HashMap<>();

        return Jwts.builder()
                .claims().add(concesiones).and()
                .subject(nombreUsuario)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(generarLlaveDigital())
                .compact();
    }

    private SecretKey generarLlaveDigital() {
        byte[] llave = Decoders.BASE64.decode(secretoJwt);
        return Keys.hmacShaKeyFor(llave);
    }

    public String extraerNombreUsuario(String token) {
        return extraerConcesionEspecifica(token, Claims::getSubject);
    }

    public Date extraerExpiracion(String token) {
        return extraerConcesionEspecifica(token, Claims::getExpiration);
    }

    private <T> T extraerConcesionEspecifica(String token, Function<Claims, T> resolvedorConcesion) {
        final Claims concesiones = extraerConcesiones(token);
        return resolvedorConcesion.apply(concesiones);
    }

    private Claims extraerConcesiones(String token) {
        return Jwts.parser()
                .verifyWith(generarLlaveDigital())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean haExpiradoToken(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    public boolean validarToken(String token, UserDetails detallesUsuario) {
        final String nombreUsuario = extraerNombreUsuario(token);
        return (nombreUsuario.equals(detallesUsuario.getUsername()) && !haExpiradoToken(token));
    }
}
