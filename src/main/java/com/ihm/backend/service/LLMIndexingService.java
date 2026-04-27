package com.ihm.backend.service;

import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.Exercise;
import com.ihm.backend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import org.springframework.http.ResponseEntity;

@Slf4j
@Service
public class LLMIndexingService {

    @Value("${llm.service.url:http://localhost:8000}")
    private String llmServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void indexCourse(Course course) {
        try {
            String url = llmServiceUrl + "/api/v1/index/course";
            
            Map<String, Object> body = new HashMap<>();
            body.put("id", course.getId());
            body.put("title", course.getTitle());
            body.put("content", course.getDescription() != null ? course.getDescription() : course.getTitle());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("author", course.getAuthor().getFullName());
            metadata.put("category", course.getCategory());
            body.put("metadata", metadata);

            sendRequest(url, body);
            log.info("Successfully requested indexing for course: {}", course.getTitle());
        } catch (Exception e) {
            log.error("Failed to index course {}: {}", course.getId(), e.getMessage());
        }
    }

    @Async
    public void indexExercise(Exercise exercise) {
        try {
            String url = llmServiceUrl + "/api/v1/index/exercise";
            
            Map<String, Object> body = new HashMap<>();
            body.put("id", exercise.getId());
            body.put("title", exercise.getTitle());
            body.put("content", exercise.getContent() != null ? exercise.getContent() : exercise.getTitle());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("courseId", exercise.getCourse().getId());
            body.put("metadata", metadata);

            sendRequest(url, body);
            log.info("Successfully requested indexing for exercise: {}", exercise.getTitle());
        } catch (Exception e) {
            log.error("Failed to index exercise {}: {}", exercise.getId(), e.getMessage());
        }
    }

    @Async
    public void evaluateKnowledge(UUID studentId, String notion) {
        try {
            String url = llmServiceUrl + "/api/v1/knowledge/evaluate";
            
            Map<String, Object> body = new HashMap<>();
            body.put("student_id", studentId.toString());
            body.put("notion", notion);

            sendRequest(url, body);
            log.info("Successfully triggered LLM evaluation for student {} on notion {}", studentId, notion);
        } catch (Exception e) {
            log.error("Failed to trigger LLM evaluation: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> recommendCourses(String title, String description) {
        try {
            String url = llmServiceUrl + "/api/v1/index/recommend-courses";
            
            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            body.put("description", description != null ? description : "");
            body.put("top_k", 5);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<List> response = restTemplate.postForEntity(url, entity, List.class);
            return (List<Map<String, Object>>) response.getBody();
        } catch (Exception e) {
            log.error("Failed to get course recommendations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void sendRequest(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }
}
