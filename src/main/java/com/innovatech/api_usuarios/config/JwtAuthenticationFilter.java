package com.innovatech.api_usuarios.config;

import org.springframework.context.annotation.Lazy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(@Lazy JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String path = request.getRequestURI();

        System.out.println("DEBUG - Procesando petición a: " + path); // Esto DEBE salir siempre

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("DEBUG - No hay Bearer token para: " + path);
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validarToken(jwt)) {
                String username = tokenProvider.obtenerUsernameDeJwt(jwt);
                String roles = tokenProvider.obtenerRolesDeJwt(jwt);

                // Convertimos la cadena de roles (ej: "ROLE_USER,ROLE_ADMIN") en autoridades de Spring
                List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Aquí es donde "desbloqueas" el acceso para el resto de la petición
                System.out.println("✅ Usuario autenticado: " + username + " con roles: " + authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("No se pudo establecer la autenticación de usuario", ex);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Definimos qué rutas son públicas (Login, Registro y Swagger)
        boolean isAuthPath = path.endsWith("/login") ||
                path.endsWith("/registro");

        boolean isSwaggerPath = path.contains("/v3/api-docs") ||
                path.contains("/swagger-ui") ||
                path.contains("/swagger-resources") ||
                path.equals("/swagger-ui.html");

        boolean skipFilter = isAuthPath || isSwaggerPath;

        System.out.println("DEBUG - Intentando entrar a: " + path + " | ¿Es ruta pública?: " + skipFilter);

        return skipFilter;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}