# Etapa 1: Runtime
# Usamos una imagen ligera de JDK 21 (Eclipse Temurin es muy estable para Spring)
FROM eclipse-temurin:21-jdk-jammy

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el archivo JAR generado desde la carpeta target de tu equipo al contenedor
# Asegúrate de que el nombre coincida con el de tu pom.xml (api-usuarios-0.0.1-SNAPSHOT.jar)
COPY target/api-usuarios-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto que configuraste en tu application.properties
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]