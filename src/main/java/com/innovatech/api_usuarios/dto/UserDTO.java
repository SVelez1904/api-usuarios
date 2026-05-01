package com.innovatech.api_usuarios.dto;

import lombok.Data;
import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles; // Solo los nombres de los roles
}
