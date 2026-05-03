package com.innovatech.api_usuarios.controller;

import com.innovatech.api_usuarios.dto.UserDTO;
import com.innovatech.api_usuarios.model.User;
import com.innovatech.api_usuarios.dto.LoginRequest;
import com.innovatech.api_usuarios.dto.LoginResponse;
import com.innovatech.api_usuarios.service.UserService;
import jakarta.validation.Valid; // Importante para las validaciones
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*") // Permite peticiones de otros orígenes (Postman/Gateway)
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
            userService.eliminarUsuario(id);
            return ResponseEntity.ok("Usuario eliminado correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}