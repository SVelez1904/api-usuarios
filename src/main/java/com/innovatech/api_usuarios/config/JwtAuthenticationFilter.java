package com.innovatech.api_usuarios.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(@Lazy JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validarToken(jwt)) {
                String username = tokenProvider.obtenerUsernameDeJwt(jwt);
                String rolesString = tokenProvider.obtenerRolesDeJwt(jwt);

                if (StringUtils.hasText(rolesString)) {
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                            .map(String::trim)
                            .filter(role -> !role.isEmpty())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("✅ JWT procesado: " + username + " con roles: " + authorities);
                }
            }
        } catch (Exception ex) {
            // No bloqueamos la petición aquí, dejamos que SecurityContext decida si era necesaria la auth
            System.err.println("❌ Error procesando JWT: " + ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    String path = request.getRequestURI();
    String method = request.getMethod();

    // 1. Omitir siempre OPTIONS (CORS)
    if ("OPTIONS".equalsIgnoreCase(method)) {
        return true;
    }

    // 2. Definir rutas públicas de forma flexible
    // Usamos .contains() sin el prefijo inicial para que no importe si llega 
    // como "/usuarios/login", "/api/usuarios/login" o simplemente "/login"
    boolean isPublicPath = path.contains("/login") || 
                           path.contains("/registro") ||
                           path.contains("/v3/api-docs") ||
                           path.contains("/swagger-ui") ||
                           path.contains("/swagger-resources") ||
                           path.contains("/webjars");

    // LOG CRÍTICO: Mira esto en tu consola de Docker. 
    // Si ves que path es "/login" pero isPublicPath es false, ahí está el error.
    System.out.println("DEBUG - Filtro JWT en: " + path + " | ¿Omitir?: " + isPublicPath);
    
    return isPublicPath;
}

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}