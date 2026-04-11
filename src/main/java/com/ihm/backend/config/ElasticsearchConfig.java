package com.ihm.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(prefix = "app.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableElasticsearchRepositories(basePackages = "com.ihm.backend.repository.elasticsearch")
public class ElasticsearchConfig {
}
