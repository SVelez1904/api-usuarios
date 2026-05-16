package com.innovatech.api_usuarios.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Leer las cabeceras de confianza que envió el Gateway
        String username = request.getHeader("X-User-Username");
        String rolesString = request.getHeader("X-User-Roles");

        // 2. Si el Gateway nos envió un usuario, lo metemos al contexto de Spring
        if (StringUtils.hasText(username)) {
            List<SimpleGrantedAuthority> authorities = List.of();

            if (StringUtils.hasText(rolesString)) {
                authorities = Arrays.stream(rolesString.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("Microservicio 🔐 - Autenticado vía Gateway: " + username + " con roles: " + authorities);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // En este enfoque, el filtro NO debe apagarse nunca para las rutas protegidas.
        // Como el login y registro no traen las cabeceras X-User, este filtro simplemente las dejará pasar limpias.
        return false;
    }
}