package com.ejemplo.facturacion.config;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ejemplo.facturacion.security.JwtFilter;

/**
 * Configuracion de autenticacion y autorizacion del API.
 *
 * El API queda protegida con tokens JWT:
 * - /autenticar y /actuator/health son publicos
 * - el resto de los endpoints exige un token valido en el encabezado
 *   Authorization con el prefijo "Bearer "
 * - los endpoints de negocio (/v1 y /v2) ademas exigen la autoridad "facturar"
 */
@Configuration
public class GestionUsuariosConfig {

    /** Rutas que pueden invocarse sin token. */
    private static final String[] RUTAS_PUBLICAS = {
            "/autenticar",
            "/actuator/health",
            "/actuator/health/**"
    };

    /** Rutas de negocio protegidas. */
    private static final String[] RUTAS_FACTURACION = {
            "/v1/**",
            "/v2/**"
    };

    /** Autoridad que otorga el derecho de facturar (ver data.sql). */
    private static final String AUTORIDAD_FACTURAR = "facturar";

    @Bean
    public PasswordEncoder passwordEncoder() throws NoSuchAlgorithmException {
        SecureRandom s = SecureRandom.getInstanceStrong();
        return new BCryptPasswordEncoder(4, s);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter)
            throws Exception {

        // Se desactiva la autenticacion basica: la unica via de acceso valida es
        // el token JWT emitido por /autenticar. Dejarla activa permitiria saltarse
        // el token enviando usuario y contrasena en cada peticion.
        http.httpBasic(basica -> basica.disable());
        http.formLogin(formulario -> formulario.disable());

        // Un API con tokens no usa cookies de sesion, asi que CSRF no aplica.
        http.csrf(csrf -> csrf.disable());

        // Sin sesiones en el servidor: cada peticion se autentica con su token.
        http.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Reglas de acceso.
        http.authorizeHttpRequests(peticiones -> peticiones
                .requestMatchers(RUTAS_PUBLICAS).permitAll()
                .requestMatchers(RUTAS_FACTURACION).hasAuthority(AUTORIDAD_FACTURAR)
                .anyRequest().authenticated());

        // El filtro de JWT se ejecuta antes del filtro de usuario y contrasena para
        // que la autenticacion ya este resuelta cuando se evaluen las reglas.
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // Sin token valido se responde 401 Unauthorized en lugar de 403.
        http.exceptionHandling(excepciones -> excepciones
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
