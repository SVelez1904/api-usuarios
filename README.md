# api-usuarios
Este microservicio gestiona de forma centralizada la lógica de usuarios, roles y la **asignación de usuarios hacia la API de Proyectos (`api-proyectos`)**. Al estar el ecosistema completamente dockerizado, la comunicación entre servicios se realiza de forma nativa e interna mediante nombres de servicio y eventos asíncronos con **Apache Kafka**.

---

## 🔄 Flujo de Trabajo Ecosistema Docker



1. **Petición HTTP:** Entra al contenedor `api-proyectos` / `api-usuarios`.
2. **Persistencia (JPA/Hibernate):** Se consolida la asignación en la base de datos (Flush).
3. **Despacho Kafka:** El productor embebido de Spring recolecta los IDs de los usuarios y despacha el evento al contenedor `kafka:29092` usando la red interna de Docker.

---

## 🛠️ Stack Tecnológico

- **Java 21** & **Spring Boot 3.x**
- **Spring Data JPA** & **Hibernate**
- **Spring Kafka** (Productor Idempotente)
- **Docker** & **Docker Compose** (Entorno completo)

---

## ⚙️ Configuración del Entorno (`application.properties`)

Al correr dentro de un contenedor Docker, las propiedades de conexión deben apuntar a los **nombres de los servicios** definidos en el archivo de Docker Compose, no a `localhost`.

```properties
# Configuración del Servidor Interno
server.port=8081

# Conexión a la Base de Datos en Docker
spring.datasource.url=jdbc:postgresql://postgres-db:5432/db_proyectos
spring.datasource.username=postgres
spring.datasource.password=secret

# Conexión Nativa Docker-to-Docker para Kafka
spring.kafka.bootstrap-servers=kafka:29092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
