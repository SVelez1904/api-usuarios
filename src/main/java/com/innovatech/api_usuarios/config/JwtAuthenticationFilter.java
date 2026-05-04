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

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validarToken(jwt)) {
                String username = tokenProvider.obtenerUsernameDeJwt(jwt);
                String rolesString = tokenProvider.obtenerRolesDeJwt(jwt);

                // LOG CRÍTICO: Si esto sale vacío o nulo en el log de Docker, ahí está el 403
                System.out.println("DEBUG - Roles extraídos del JWT: [" + rolesString + "]");

                if (rolesString != null && !rolesString.isEmpty()) {
                    // Limpiamos espacios por si el string viene como "ROLE_ADMIN, ROLE_USER"
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                            .map(String::trim)
                            .filter(role -> !role.isEmpty())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("✅ Autenticación exitosa para: " + username + " | Autoridades: " + authorities);
                } else {
                    System.err.println("❌ ERROR: El token es válido pero NO contiene roles.");
                }
            } else {
                System.err.println("❌ ERROR: Token inválido o expirado para la ruta: " + path);
            }
        } catch (Exception ex) {
            System.err.println("❌ ERROR: Falló el proceso de autenticación: " + ex.getMessage());
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