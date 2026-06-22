package com.innovatech.api_usuarios.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping; // Importante para las validaciones
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.innovatech.api_usuarios.dto.LoginRequest;
import com.innovatech.api_usuarios.dto.LoginResponse;
import com.innovatech.api_usuarios.dto.UserDTO;
import com.innovatech.api_usuarios.model.User;
import com.innovatech.api_usuarios.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.autenticar(request);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody UserDTO userDto) {
        try {
            // En esta lógica utilizo el DTO
            return ResponseEntity.status(201).body(userService.registrarUsuario(userDto));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
    // Listar usuarios
    @GetMapping
    public ResponseEntity<List<User>> obtenerUsuarios() {
        return ResponseEntity.ok(userService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            // El servicio debe retornar el User o el UserDTO
            User user = userService.buscarPorId(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            // Si el usuario  no existe, esto ayuda a que Feign no lance un 500
            return ResponseEntity.status(404).body("Usuario no encontrado: " + e.getMessage());
        }
    }

    // Modificar usuario
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody UserDTO userDto) {
        try {
            return ResponseEntity.ok(userService.actualizarUsuario(id, userDto));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // Eliminar usuario
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            // Al ejecutar este método, se borra de Postgres y se gatilla el mensaje a Kafka
            userService.eliminarUsuario(id);
            return ResponseEntity.ok("Usuario eliminado correctamente y sincronizado en el ecosistema.");
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCountTotalUsuarios() {
        return ResponseEntity.ok(userService.contarTotalUsuarios());
    }
}