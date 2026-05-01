package com.innovatech.api_usuarios.dto;

import lombok.Data;

@Data
public class LoginRequest {
    public String username;
    public String password;
}