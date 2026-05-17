package com.innovatech.api_usuarios.controller;

import com.innovatech.api_usuarios.model.Role;
import com.innovatech.api_usuarios.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<Role>> obtenerRoles() {
        System.out.println("--> ¡ENTRÉ AL CONTROLADOR DE ROLES!");
        return ResponseEntity.ok(roleService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Role> guardarRol(@RequestBody Role role) {
        return ResponseEntity.status(201).body(roleService.crearRol(role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        roleService.eliminarRol(id);
        return ResponseEntity.ok("Rol eliminado con éxito");
    }
}