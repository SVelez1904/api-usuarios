package com.innovatech.api_usuarios.service;

import com.innovatech.api_usuarios.model.Role;
import com.innovatech.api_usuarios.repository.RoleRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> listarTodos() {
        return roleRepository.findAll();
    }

    public Role crearRol(Role role) {
        // Aseguramos que el nombre siempre esté en mayúsculas por convención de Spring Security
        role.setName(role.getName().toUpperCase());
        return roleRepository.save(role);
    }

    public Role buscarPorNombre(String name) {
        return roleRepository.findByName(name.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + name));
    }

    public void eliminarRol(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: El ID del rol no existe.");
        }
        roleRepository.deleteById(id);
    }
}