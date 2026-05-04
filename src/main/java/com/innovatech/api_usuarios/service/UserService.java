package com.innovatech.api_usuarios.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.innovatech.api_usuarios.dto.UserDTO;
import com.innovatech.api_usuarios.model.Role;
import com.innovatech.api_usuarios.model.User;
import com.innovatech.api_usuarios.dto.LoginRequest;
import com.innovatech.api_usuarios.repository.UserRepository;
import com.innovatech.api_usuarios.repository.RoleRepository;
import com.innovatech.api_usuarios.config.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       @org.springframework.context.annotation.Lazy AuthenticationManager authenticationManager) { // Agrega @Lazy aquí
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }
    public String autenticar(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            return jwtTokenProvider.generarToken(authentication);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
    }

    public User registrarUsuario(UserDTO userDto) {
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());


        user.setPassword(passwordEncoder.encode(userDto.getPassword()));


        Set<Role> roles = new HashSet<>();
        for (String roleName : userDto.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
            roles.add(role);
        }
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList()))
                .build();
    }
    public List<User> listarTodos() {
        return userRepository.findAll();
    }

    // Metodo para buscar un usuario específico por su ID
    public User buscarPorId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ID: " + id));
    }

    public User actualizarUsuario(Long id, UserDTO userDto) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(userDto.getUsername());
            user.setEmail(userDto.getEmail());

            // Solo actualizamos la contraseña si viene en el DTO
            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            }

            // Actualización de roles (opcional, dependiendo de tu lógica)
            if (userDto.getRoles() != null) {
                Set<Role> roles = new HashSet<>();
                for (String roleName : userDto.getRoles()) {
                    Role role = roleRepository.findByName(roleName)
                            .orElseThrow(() -> new RuntimeException("Role no encontrado: " + roleName));
                    roles.add(role);
                }
                user.setRoles(roles);
            }

            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    public void eliminarUsuario(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Usuario no encontrado con ID: " + id);
        }
        userRepository.deleteById(id);
    }
}