package com.innovatech.api_usuarios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovatech.api_usuarios.config.JwtTokenProvider;
import com.innovatech.api_usuarios.dto.LoginRequest;
import com.innovatech.api_usuarios.dto.UserDTO;
import com.innovatech.api_usuarios.model.User;
import com.innovatech.api_usuarios.service.RoleService;
import com.innovatech.api_usuarios.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private com.innovatech.api_usuarios.service.RoleService roleService;

    // 🔥 MOCKS CRÍTICOS DE SEGURIDAD: Evitan que el contexto falle al inicializar UserController
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private User userMock;
    private UserDTO userDtoMock;

    @BeforeEach
    void setUp() {
        userMock = new User();
        userMock.setId(1L);
        userMock.setUsername("sebastian_v");
        userMock.setEmail("seba@innovatech.com");

        userDtoMock = new UserDTO();
        userDtoMock.setUsername("sebastian_v");
        userDtoMock.setEmail("seba@innovatech.com");
        userDtoMock.setPassword("123456");
        userDtoMock.setRoles(Collections.singleton("ROLE_USER"));
    }

    // --- 1. TEST POST: /usuarios/login ---

    @Test
    @DisplayName("POST /usuarios/login - Debe retornar 200 y LoginResponse si las credenciales son validas")
    void login_Exitoso() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("sebastian_v");
        loginRequest.setPassword("123456");

        when(userService.autenticar(any(LoginRequest.class))).thenReturn("mocked-jwt-token");

        mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
    }

    // --- 2. TEST POST: /usuarios/registro ---

    @Test
    @DisplayName("POST /usuarios/registro - Debe retornar 201 y el usuario creado")
    void registrar_Exitoso() throws Exception {
        when(userService.registrarUsuario(any(UserDTO.class))).thenReturn(userMock);

        mockMvc.perform(post("/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDtoMock)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("sebastian_v"));
    }

    @Test
    @DisplayName("POST /usuarios/registro - Debe retornar 400 si el servicio lanza una excepcion")
    void registrar_FallaEstructura() throws Exception {
        when(userService.registrarUsuario(any(UserDTO.class)))
                .thenThrow(new RuntimeException("Error: Rol no encontrado."));

        mockMvc.perform(post("/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDtoMock)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Error: Rol no encontrado."));
    }

    // --- 3. TEST GET: /usuarios ---

    @Test
    @DisplayName("GET /usuarios - Debe retornar 200 y la lista de usuarios")
    void obtenerUsuarios_Exitoso() throws Exception {
        when(userService.listarTodos()).thenReturn(List.of(userMock));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("sebastian_v"));
    }

    // --- 4. TEST GET: /usuarios/{id} ---

    @Test
    @DisplayName("GET /usuarios/{id} - Debe retornar 200 si el usuario existe")
    void obtenerPorId_Exitoso() throws Exception {
        when(userService.buscarPorId(1L)).thenReturn(userMock);

        mockMvc.perform(get("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("sebastian_v"));
    }

    @Test
    @DisplayName("GET /usuarios/{id} - Debe retornar 404 si el usuario no existe")
    void obtenerPorId_NoEncontrado() throws Exception {
        // CAMBIO AQUÍ: Lanza una RuntimeException común para que entre limpio al catch de tu controlador
        when(userService.buscarPorId(99L))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        mockMvc.perform(get("/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Usuario no encontrado: Usuario no encontrado"));
    }

    // --- 5. TEST PUT: /usuarios/{id} ---

    @Test
    @DisplayName("PUT /usuarios/{id} - Debe retornar 200 y el usuario modificado")
    void actualizar_Exitoso() throws Exception {
        when(userService.actualizarUsuario(eq(1L), any(UserDTO.class))).thenReturn(userMock);

        mockMvc.perform(put("/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDtoMock)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sebastian_v"));
    }

    @Test
    @DisplayName("PUT /usuarios/{id} - Debe retornar 404 si el ID no existe al actualizar")
    void actualizar_NoEncontrado() throws Exception {
        when(userService.actualizarUsuario(eq(99L), any(UserDTO.class)))
                .thenThrow(new RuntimeException("Usuario no encontrado con ID: 99"));

        mockMvc.perform(put("/usuarios/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDtoMock)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Usuario no encontrado con ID: 99"));
    }

    // --- 6. TEST DELETE: /usuarios/{id} ---

    @Test
    @DisplayName("DELETE /usuarios/{id} - Debe retornar 200 si se elimina correctamente")
    void eliminar_Exitoso() throws Exception {
        doNothing().when(userService).eliminarUsuario(1L);

        mockMvc.perform(delete("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario eliminado correctamente y sincronizado en el ecosistema."));

        verify(userService, times(1)).eliminarUsuario(1L);
    }

    @Test
    @DisplayName("DELETE /usuarios/{id} - Debe retornar 404 si falla la eliminacion")
    void eliminar_NoEncontrado() throws Exception {
        doThrow(new RuntimeException("Usuario no encontrado")).when(userService).eliminarUsuario(99L);

        mockMvc.perform(delete("/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Usuario no encontrado"));
    }

    // --- 7. TEST GET: /usuarios/count ---

    @Test
    @DisplayName("GET /usuarios/count - Debe retornar 200 y el numero total de registros")
    void getCountTotalUsuarios_Exitoso() throws Exception {
        when(userService.contarTotalUsuarios()).thenReturn(10L);

        mockMvc.perform(get("/usuarios/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }
}