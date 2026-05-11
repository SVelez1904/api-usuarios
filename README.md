# Innovatech Users API

**Desarrollado por:** Sebastián Vélez  
**Proyecto:** Innovatech Solutions

Este microservicio es el núcleo de seguridad y gestión de identidad de **Innovatech**. Se encarga de la administración de usuarios, perfiles y, fundamentalmente, de la emisión de tokens de autenticación para el resto del ecosistema.

---

## Funcionalidades Principales

*   **Gestión de Usuarios:** Registro, actualización y consulta de perfiles de usuario.
*   **Autenticación y Autorización:** Implementación de seguridad para el acceso al sistema.
*   **Generación de JWT:** Emisión de **JSON Web Tokens** firmados para permitir la comunicación segura entre microservicios a través del API Gateway.
*   **Persistencia de Datos:** Manejo de información sensible utilizando estándares de encriptación para contraseñas.

##  Stack Tecnológico

*   **Lenguaje:** Java 17+
*   **Framework:** Spring Boot 3
*   **Seguridad:** Spring Security & JWT (io.jsonwebtoken)
*   **Base de Datos:** PostgreSQL
*   **Herramientas:** Spring Data JPA, Hibernate

##  Endpoints Principales (Resumen)

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Autenticación y generación de token. |
| `POST` | `/api/usuarios/registro` | Registro de nuevos usuarios. |

##  Despliegue

Este microservicio está diseñado para correr como un contenedor Docker. Se recomienda su ejecución a través del orquestador principal del proyecto:

`
