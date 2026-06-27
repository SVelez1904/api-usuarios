package com.innovatech.api_usuarios;

import com.innovatech.api_usuarios.model.Role;
import com.innovatech.api_usuarios.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@ComponentScan(basePackages = "com.innovatech.api_usuarios")
public class ApiUsuariosApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiUsuariosApplication.class, args);
	}

	@Bean
	@Profile("!test")
	CommandLineRunner init(RoleRepository roleRepository) {
		return args -> {
			// Verificamos si los roles ya existen para no duplicarlos cada vez que se reinicia Docker
			crearRolSiNoExiste(roleRepository, "ROLE_LEADER");
			crearRolSiNoExiste(roleRepository, "ROLE_DEVELOPER");
			crearRolSiNoExiste(roleRepository, "ROLE_ADMIN");

			System.out.println("✅ Roles de Innovatech inicializados correctamente.");
		};
	}

	private void crearRolSiNoExiste(RoleRepository repository, String nombreRol) {
		if (repository.findByName(nombreRol).isEmpty()) {
			Role role = new Role();
			role.setName(nombreRol);
			repository.save(role);
		}
	}
}