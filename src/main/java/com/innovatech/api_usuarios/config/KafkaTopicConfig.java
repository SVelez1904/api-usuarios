package com.innovatech.api_usuarios.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic usuariosEventsTopic() {
        return TopicBuilder.name("usuarios-events")
                .partitions(3) // Divide la carga en 3 particiones para escalabilidad
                .replicas(1)   // 1 réplica (ajustar si estás en un clúster de producción con más nodos)
                .build();
    }
}