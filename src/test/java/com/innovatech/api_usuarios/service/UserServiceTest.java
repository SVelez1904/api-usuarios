package com.innovatech.api_usuarios.service;

import com.innovatech.api_usuarios.config.JwtTokenProvider;
import com.innovatech.api_usuarios.dto.LoginRequest;
import com.innovatech.api_usuarios.dto.UserDTO;
import com.innovatech.api_usuarios.model.Role;
import com.innovatech.api_usuarios.model.User;
import com.innovatech.api_usuarios.repository.RoleRepository;
import com.innovatech.api_usuarios.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private UserService userService;

    private User userMock;
    private Role roleMock;
    private UserDTO userDtoMock;

    @BeforeEach
    void setUp() {
        roleMock = new Role();
        roleMock.setId(1L);
        roleMock.setName("ROLE_USER");

        userMock = new User();
        userMock.setId(1L);
        userMock.setUsername("sebastian_v");
        userMock.setEmail("seba@innovatech.com");
        userMock.setPassword("encodedPassword");
        userMock.setRoles(new HashSet<>(Collections.singletonList(roleMock)));

        userDtoMock = new UserDTO();
        userDtoMock.setUsername("sebastian_v");
        userDtoMock.setEmail("seba@innovatech.com");
        userDtoMock.setPassword("123456");
        userDtoMock.setRoles(Collections.singleton("ROLE_USER"));
    }

    // --- 1. AUTENTICAR ---

    @Test
    @DisplayName("autenticar: Debe retornar token JWT si las credenciales son válidas")
    void autenticar_Exitoso() {
        LoginRequest request = new LoginRequest("sebastian_v", "123456");
        Authentication authMock = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authMock);
        when(jwtTokenProvider.generarToken(authMock)).thenReturn("mocked-jwt-token");

        String token = userService.autenticar(request);

        assertEquals("mocked-jwt-token", token);
    }

    @Test
    @DisplayName("autenticar: Debe lanzar ResponseStatusException UNAUTHORIZED si falla la autenticación")
    void autenticar_FallaCredenciales() {
        LoginRequest request = new LoginRequest("usuario_incorrecto", "wrong");
        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Bad credentials"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.autenticar(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Credenciales inválidas", ex.getReason());
    }

    // --- 2. REGISTRAR USUARIO ---

    @Test
    @DisplayName("registrarUsuario: Debe codificar contraseña, asignar roles y guardar correctamente")
    void registrarUsuario_Exitoso() {
        when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleMock));
        when(userRepository.save(any(User.class))).thenReturn(userMock);

        User guardado = userService.registrarUsuario(userDtoMock);

        assertNotNull(guardado);
        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registrarUsuario: Debe lanzar excepción si el rol del DTO no existe")
    void registrarUsuario_RolNoEncontrado() {
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.registrarUsuario(userDtoMock));

        assertEquals("Error: Rol no encontrado.", ex.getMessage());
    }

    // --- 3. LOAD USER BY USERNAME (SPRING SECURITY) ---

    @Test
    @DisplayName("loadUserByUsername: Debe mapear UserDetails correctamente si el usuario existe")
    void loadUserByUsername_Exitoso() {
        when(userRepository.findByUsername("sebastian_v")).thenReturn(Optional.of(userMock));

        UserDetails details = userService.loadUserByUsername("sebastian_v");

        assertNotNull(details);
        assertEquals("sebastian_v", details.getUsername());
        assertEquals("encodedPassword", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("loadUserByUsername: Debe lanzar UsernameNotFoundException si el usuario no existe")
    void loadUserByUsername_NoEncontrado() {
        when(userRepository.findByUsername("desconocido")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("desconocido"));
    }

    // --- 4. LISTAR Y BUSCAR POR ID ---

    @Test
    @DisplayName("listarTodos: Debe retornar el listado completo de usuarios")
    void listarTodos_Exitoso() {
        when(userRepository.findAll()).thenReturn(List.of(userMock));
        List<User> resultado = userService.listarTodos();
        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("buscarPorId: Debe retornar el usuario correspondiente")
    void buscarPorId_Exitoso() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userMock));
        User encontrado = userService.buscarPorId(1L);
        assertNotNull(encontrado);
    }

    @Test
    @DisplayName("buscarPorId: Debe lanzar ResponseStatusException NOT_FOUND si no existe el ID")
    void buscarPorId_NoEncontrado() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.buscarPorId(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- 5. ACTUALIZAR USUARIO (Cubre todas las ramas de actualización opcional) ---

    @Test
    @DisplayName("actualizarUsuario: Debe actualizar datos, nueva contraseña y nuevos roles")
    void actualizarUsuario_CambioTotal() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userMock));
        when(passwordEncoder.encode("123456")).thenReturn("newEncoded");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleMock));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User actualizado = userService.actualizarUsuario(1L, userDtoMock);

        assertEquals("newEncoded", actualizado.getPassword());
        verify(userRepository).save(actualizado);
    }

    @Test
    @DisplayName("actualizarUsuario: No debe re-hashear la contraseña si llega vacía o nula")
    void actualizarUsuario_SinContrasenaNiRoles() {
        userDtoMock.setPassword("");
        userDtoMock.setRoles(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(userMock));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User actualizado = userService.actualizarUsuario(1L, userDtoMock);

        assertEquals("encodedPassword", actualizado.getPassword()); // Mantiene la anterior
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("actualizarUsuario: Debe fallar si el rol enviado no existe en la actualización")
    void actualizarUsuario_RolInvalido() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userMock));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.actualizarUsuario(1L, userDtoMock));
    }

    @Test
    @DisplayName("actualizarUsuario: Debe lanzar excepción si el usuario origen no existe")
    void actualizarUsuario_UsuarioInexistente() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.actualizarUsuario(99L, userDtoMock));
    }

    // --- 6. ELIMINACIÓN Y KAFKA ---

    @Test
    @DisplayName("eliminarUsuario: Debe borrar de DB y mandar mensaje síncrono a Kafka exitosamente")
    void eliminarUsuario_HappyPath() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userMock));

        CompletableFuture<Object> futureMock = CompletableFuture.completedFuture(mock(Object.class));
        when(kafkaTemplate.send(eq("usuarios-events"), eq("1"))).thenAnswer(inv -> futureMock);

        assertDoesNotThrow(() -> userService.eliminarUsuario(1L));

        verify(userRepository).delete(userMock);
        verify(kafkaTemplate).send("usuarios-events", "1");
    }

    @Test
    @DisplayName("eliminarUsuario: Debe atrapar la excepción de Kafka en el bloque catch sin romper el flujo")
    void eliminarUsuario_FallaKafkaCatchBlock() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userMock));

        CompletableFuture<Object> futureFallido = new CompletableFuture<>();
        futureFallido.completeExceptionally(new RuntimeException("Kafka Broker Down"));
        when(kafkaTemplate.send(eq("usuarios-events"), eq("1"))).thenAnswer(inv -> futureFallido);

        // Se verifica que NO propague la excepción (el catch absorbe el error)
        assertDoesNotThrow(() -> userService.eliminarUsuario(1L));
        verify(userRepository).delete(userMock);
    }

    @Test
    @DisplayName("ejecutarBorradoEnBaseDatos: Debe lanzar ResponseStatusException NOT_FOUND si el usuario no existe")
    void ejecutarBorradoEnBaseDatos_Inexistente() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.ejecutarBorradoEnBaseDatos(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("contarTotalUsuarios: Debe retornar el conteo del repositorio")
    void contarTotalUsuarios_Exitoso() {
        when(userRepository.count()).thenReturn(15L);
        assertEquals(15L, userService.contarTotalUsuarios());
    }
}