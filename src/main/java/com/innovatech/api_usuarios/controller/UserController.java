package com.innovatech.api_usuarios.controller;

import com.innovatech.api_usuarios.model.User;
import com.innovatech.api_usuarios.dto.LoginRequest;  // Asegúrate de que estén en el paquete DTO
import com.innovatech.api_usuarios.dto.LoginResponse;
import com.innovatech.api_usuarios.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Delegamos la lógica pesada al servicio
        String token = userService.autenticar(request);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody User user) {
        return ResponseEntity.ok(userService.registrarUsuario(user));
    }

    @GetMapping
    public ResponseEntity<List<User>> obtenerUsuarios() {
        return ResponseEntity.ok(userService.listarTodos());
    }
}