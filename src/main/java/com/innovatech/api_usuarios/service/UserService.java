package com.innovatech.api_usuarios.service;

import jakarta.transaction.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Constructor completo con inyecciones
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       @org.springframework.context.annotation.Lazy AuthenticationManager authenticationManager,
                       KafkaTemplate<String, String> kafkaTemplate) { // 👈 LE QUITAMOS EL @Lazy AQUÍ
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.kafkaTemplate = kafkaTemplate; // Ahora inyecta la instancia real directo al arrancar
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

    // 🔥 EL MÉTODO QUE REQUERÍA LA INTERFAZ (Ya no dará error)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.innovatech.api_usuarios.model.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

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

    public User buscarPorId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ID: " + id));
    }

    public User actualizarUsuario(Long id, UserDTO userDto) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(userDto.getUsername());
            user.setEmail(userDto.getEmail());

            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            }

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

    // Método de eliminación sincronizado con Kafka
    // Reemplaza tu método eliminarUsuario en UserService.java por este:
    public void eliminarUsuario(Long id) {
        // 1. Borramos de la DB usando un método puramente transaccional
        ejecutarBorradoEnBaseDatos(id);

        // 2. FUERA de la transacción de la DB, enviamos el mensaje a Kafka de forma síncrona
        System.out.println("🚀 [FUERA DE TX] Forzando envío síncrono a Kafka para ID: " + id);
        try {
            this.kafkaTemplate.send("usuarios-events", String.valueOf(id)).get();
            System.out.println("✅ [FUERA DE TX] CONFIRMADO: Kafka recibió el ID " + id);
        } catch (Exception e) {
            System.err.println("🚨 ERROR EN KAFKA: " + e.getMessage());
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void ejecutarBorradoEnBaseDatos(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.getRoles().clear();
        userRepository.saveAndFlush(user);
        userRepository.delete(user);
        userRepository.flush();
        System.out.println("💾 DB: Usuario " + id + " borrado físicamente de Postgres.");
    }
    public Long contarTotalUsuarios() {
        return userRepository.count();
    }
}