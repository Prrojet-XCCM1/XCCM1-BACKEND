package com.ihm.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.CourseAIGenerateRequest;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.Exercise;
import com.ihm.backend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Slf4j
@Service
public class LLMIndexingService {

    @Value("${llm.service.url:http://localhost:8000}")
    private String llmServiceUrl;

    /** RestTemplate conservé pour les appels non-streaming (indexation, recommandations). */
    private final RestTemplate restTemplate = new RestTemplate();

    /** WebClient non-bloquant pour le streaming SSE vers le LLM Service. */
    private final WebClient webClient;

    @Autowired
    @Lazy  // Injection différée pour rompre le cycle CourseService ↔ LLMIndexingService
    private CourseService courseService;

    public LLMIndexingService(@Value("${llm.service.url:http://localhost:8000}") String llmServiceUrl) {
        this.llmServiceUrl = llmServiceUrl;
        this.webClient = WebClient.builder()
                .baseUrl(llmServiceUrl)
                // Timeout de réponse : 5 minutes (cours longs)
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }

    // ── Indexation asynchrone ────────────────────────────────────────────────

    /** Taille max du contenu indexé (caractères) pour maîtriser la taille de l'embedding. */
    private static final int MAX_INDEX_CONTENT_CHARS = 8000;

    @Async
    public void indexCourse(Course course) {
        try {
            String url = llmServiceUrl + "/api/v1/index/course";

            // Contenu riche pour le RAG : on aplatit le corps TipTap en texte plutôt
            // que d'indexer uniquement la description (trop maigre). Fallback description/titre.
            String bodyText = extractPlainText(course.getContent());
            StringBuilder full = new StringBuilder();
            if (course.getDescription() != null && !course.getDescription().isBlank()) {
                full.append(course.getDescription()).append("\n\n");
            }
            if (!bodyText.isBlank()) {
                full.append(bodyText);
            }
            String content = full.toString().strip();
            if (content.isBlank()) {
                content = course.getTitle();
            }
            if (content.length() > MAX_INDEX_CONTENT_CHARS) {
                content = content.substring(0, MAX_INDEX_CONTENT_CHARS);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("id", course.getId());
            body.put("title", course.getTitle());
            body.put("content", content);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("author", course.getAuthor().getFullName());
            metadata.put("category", course.getCategory());
            body.put("metadata", metadata);

            sendRequest(url, body);
            log.info("Successfully requested indexing for course {} ({} chars)", course.getTitle(), content.length());
        } catch (Exception e) {
            log.error("Failed to index course {}: {}", course.getId(), e.getMessage());
        }
    }

    /**
     * Aplatit un document TipTap (JSON déjà désérialisé en Map/List) en texte brut,
     * en concaténant récursivement tous les champs "text" des nœuds.
     */
    @SuppressWarnings("unchecked")
    private String extractPlainText(Object node) {
        if (node == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;
            Object text = map.get("text");
            if (text instanceof String) {
                sb.append((String) text).append(' ');
            }
            Object children = map.get("content");
            if (children != null) {
                sb.append(extractPlainText(children));
            }
            // Sauts de ligne entre blocs de type titre/paragraphe pour la lisibilité
            Object type = map.get("type");
            if ("heading".equals(type) || "paragraph".equals(type)) {
                sb.append('\n');
            }
        } else if (node instanceof List) {
            for (Object child : (List<Object>) node) {
                sb.append(extractPlainText(child));
            }
        }
        return sb.toString();
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

    // ── Recommandations ──────────────────────────────────────────────────────

    public List<Map<String, Object>> recommendCourses(String title, String description, String content) {
        if (title == null || title.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String url = llmServiceUrl + "/api/v1/index/recommend-courses";
            log.info("Requesting recommendations from LLM at: {} for title: {}", url, title);

            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            body.put("description", description != null ? description : "");
            body.put("content", content != null ? content : "");
            body.put("top_k", 5);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Object> response = restTemplate.postForEntity(url, entity, Object.class);
            Object responseBody = response.getBody();

            if (responseBody instanceof List) {
                return (List<Map<String, Object>>) responseBody;
            } else if (responseBody instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) responseBody;
                if (map.containsKey("recommendations") && map.get("recommendations") instanceof List) {
                    return (List<Map<String, Object>>) map.get("recommendations");
                } else if (map.containsKey("data") && map.get("data") instanceof List) {
                    return (List<Map<String, Object>>) map.get("data");
                }
            }

            log.warn("Unexpected response format from LLM service: {}",
                    responseBody != null ? responseBody.getClass().getName() : "null");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to get course recommendations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Streaming SSE (non-bloquant via WebClient) ───────────────────────────

    /**
     * Génère un cours en streaming depuis le LLM Service en utilisant WebClient
     * non-bloquant. Chaque événement SSE reçu est retransmis directement au
     * SseEmitter du frontend.
     *
     * Quand l'événement "done" est reçu, le contenu du cours est automatiquement
     * sauvegardé en base via {@link CourseService#saveContentAsync}.
     *
     * @param request    Paramètres de génération
     * @param emitter    Émetteur SSE vers le frontend
     * @param courseId   ID du cours à mettre à jour après génération (nullable)
     */
    public void streamGenerateCourse(CourseAIGenerateRequest request,
                                     SseEmitter emitter,
                                     Integer courseId) {
        ObjectMapper mapper = new ObjectMapper();
        final StringBuilder sseBuffer = new StringBuilder();

        Map<String, Object> body = new HashMap<>();
        body.put("description", request.getDescription());
        body.put("discipline", request.getDiscipline());
        body.put("level", request.getLevel());
        body.put("language", request.getLanguage());
        body.put("exercises_per_chapter", request.getExercisesPerChapter());

        webClient.post()
                .uri("/api/v1/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(6))
                .subscribe(
                        chunk -> {
                            try {
                                sseBuffer.append(chunk);
                                forwardParsedSseEvents(sseBuffer, emitter, mapper, courseId);
                            } catch (Exception e) {
                                log.warn("Error forwarding SSE chunk: {}", e.getMessage());
                            }
                        },
                        error -> {
                            log.error("Error streaming course generation: {}", error.getMessage());
                            try {
                                String msg = error.getMessage() != null
                                        ? error.getMessage().replace("\"", "'")
                                        : "Erreur inconnue";
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("{\"message\":\"" + msg + "\"}"));
                            } catch (Exception ignored) {}
                            emitter.completeWithError(error);
                        },
                        () -> {
                            try {
                                forwardParsedSseEvents(sseBuffer, emitter, mapper, courseId);
                            } catch (Exception ignored) {}
                            emitter.complete();
                        }
                );
    }

    /**
     * Parse les blocs SSE du LLM Service et les retransmet proprement au frontend.
     * Sans cela, SseEmitter.send(rawChunk) double-encapsule le flux et le client
     * ne reçoit jamais l'événement {@code done}.
     */
    private void forwardParsedSseEvents(StringBuilder buffer,
                                        SseEmitter emitter,
                                        ObjectMapper mapper,
                                        Integer courseId) throws Exception {
        while (true) {
            int[] separator = findSseEventSeparator(buffer);
            if (separator == null) {
                return;
            }

            String block = buffer.substring(0, separator[0]);
            buffer.delete(0, separator[0] + separator[1]);

            String eventName = "message";
            StringBuilder dataBuilder = new StringBuilder();
            for (String rawLine : block.split("\n")) {
                String line = rawLine.endsWith("\r") ? rawLine.substring(0, rawLine.length() - 1) : rawLine;
                if (line.startsWith("event:")) {
                    eventName = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    if (dataBuilder.length() > 0) {
                        dataBuilder.append('\n');
                    }
                    dataBuilder.append(line.substring(5).trim());
                }
            }

            String data = dataBuilder.toString().trim();
            if (data.isEmpty()) {
                continue;
            }

            emitter.send(SseEmitter.event().name(eventName).data(data));

            if ("done".equals(eventName) && courseId != null) {
                try {
                    Map<?, ?> parsed = mapper.readValue(data, Map.class);
                    if (parsed.containsKey("content")) {
                        String contentJson = mapper.writeValueAsString(parsed.get("content"));
                        courseService.saveContentAsync(courseId, contentJson);
                        log.info("Auto-save triggered for course {}", courseId);
                    }
                } catch (Exception ex) {
                    log.warn("Could not parse done payload for auto-save: {}", ex.getMessage());
                }
            }
        }
    }

    /** Retourne [index, length] du séparateur d'événement SSE, ou null si incomplet. */
    private int[] findSseEventSeparator(StringBuilder buffer) {
        String text = buffer.toString();
        int lf = text.indexOf("\n\n");
        int crlf = text.indexOf("\r\n\r\n");

        if (lf < 0 && crlf < 0) {
            return null;
        }
        if (lf < 0) {
            return new int[] { crlf, 4 };
        }
        if (crlf < 0) {
            return new int[] { lf, 2 };
        }
        if (crlf <= lf) {
            return new int[] { crlf, 4 };
        }
        return new int[] { lf, 2 };
    }

    // ── Surcharge de compatibilité (sans courseId) ───────────────────────────

    public void streamGenerateCourse(CourseAIGenerateRequest request, SseEmitter emitter) {
        streamGenerateCourse(request, emitter, null);
    }

    // ── Helper interne ───────────────────────────────────────────────────────

    private void sendRequest(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }
}
