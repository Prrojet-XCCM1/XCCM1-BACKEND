package com.ihm.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.ihm.backend.repository.elasticsearch")
public class ElasticsearchConfig {
}
