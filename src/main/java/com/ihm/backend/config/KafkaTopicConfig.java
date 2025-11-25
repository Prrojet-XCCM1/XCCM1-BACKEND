package com.ihm.backend.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Topics pour les cours
    @Bean
    public NewTopic coursCreatedTopic() {
        return TopicBuilder.name("course-created")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
