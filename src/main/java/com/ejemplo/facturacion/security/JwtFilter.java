package com.ejemplo.facturacion.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que lee el token JWT del encabezado Authorization y, si es valido,
 * coloca al usuario autenticado en el contexto de seguridad.
 *
 * Si el token viene ausente, mal formado, alterado o vencido, el filtro deja el
 * contexto sin autenticacion y continua la cadena. La decision de rechazar la
 * peticion queda entonces en manos de las reglas de GestionUsuariosConfig, que
 * responden 401.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtFilter.class);

    private static final String PREFIJO_TOKEN_JW_STRING = "Bearer ";
    private static final String NOMBRE_ENCABEZADO_AUTORIZACION = "Authorization";

    @Autowired private JwtService jwtService;
    @Autowired private ApplicationContext contextoAplicacion;

    private JPADetalleUsuariosService getJPADetalleUsuariosService() {
        return contextoAplicacion.getBean(JPADetalleUsuariosService.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String encabezadoAutorizacion = request.getHeader(NOMBRE_ENCABEZADO_AUTORIZACION);

        if (encabezadoAutorizacion != null && encabezadoAutorizacion.startsWith(PREFIJO_TOKEN_JW_STRING)) {
            String token = encabezadoAutorizacion.substring(PREFIJO_TOKEN_JW_STRING.length());

            try {
                String nombreUsuario = jwtService.extraerNombreUsuario(token);

                if (nombreUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails detallesUsuario = getJPADetalleUsuariosService().loadUserByUsername(nombreUsuario);

                    if (jwtService.validarToken(token, detallesUsuario)) {
                        UsernamePasswordAuthenticationToken tokenAutenticado = new UsernamePasswordAuthenticationToken(
                                detallesUsuario, null, detallesUsuario.getAuthorities());
                        tokenAutenticado.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(tokenAutenticado);
                    }
                }
            } catch (Exception e) {
                // Token invalido o vencido: no se autentica y se limpia el contexto.
                SecurityContextHolder.clearContext();
                LOG.warn("Token JWT rechazado en {} {}: {}",
                        request.getMethod(), request.getRequestURI(), e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
