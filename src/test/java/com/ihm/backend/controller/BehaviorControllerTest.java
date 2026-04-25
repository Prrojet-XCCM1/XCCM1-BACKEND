package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.BehaviorEventRequest;
import com.ihm.backend.entity.StudentBehaviorEvent;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.BehaviorEventType;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.repository.jpa.StudentBehaviorEventRepository;
import com.ihm.backend.repository.jpa.UserRepository;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitaires pour BehaviorController.
 * Couvre l'enregistrement et la récupération des événements comportementaux
 * étudiants.
 */
@WebMvcTest(BehaviorController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class BehaviorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentBehaviorEventRepository behaviorRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private User studentUser;
    private final UUID studentId = UUID.fromString("BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB");

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(studentId)
                .email("student@test.com")
                .role(UserRole.STUDENT)
                .active(true)
                .verified(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(studentUser, null, List.of()));

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
    }

    // =========================================================================
    // ENREGISTREMENT D'ÉVÉNEMENTS COMPORTEMENTAUX
    // =========================================================================

    @Nested
    @DisplayName("Enregistrement d'Événements (POST /api/v1/behavior/log)")
    class LogBehaviorEvent {

        @Test
        @DisplayName("POST /api/v1/behavior/log - Lecture de contenu enregistrée avec succès")
        void logContentReadEvent_success() throws Exception {
            BehaviorEventRequest request = new BehaviorEventRequest();
            request.setEventType(BehaviorEventType.CONTENT_READ);
            request.setGranuleId(42);
            request.setNotion("Programmation Orientée Objet");
            request.setDurationSeconds(120);
            request.setReadDepthPercent(85);

            StudentBehaviorEvent savedEvent = StudentBehaviorEvent.builder()
                    .id(1L)
                    .student(studentUser)
                    .eventType(BehaviorEventType.CONTENT_READ)
                    .granuleId(42)
                    .notion("Programmation Orientée Objet")
                    .durationSeconds(120)
                    .readDepthPercent(85)
                    .build();

            when(behaviorRepository.save(any(StudentBehaviorEvent.class))).thenReturn(savedEvent);

            mockMvc.perform(post("/api/v1/behavior/log")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Event logged successfully"));

            verify(behaviorRepository).save(any(StudentBehaviorEvent.class));
        }

        @Test
        @DisplayName("POST /api/v1/behavior/log - Soumission d'exercice avec score")
        void logExerciseSubmittedEvent_success() throws Exception {
            BehaviorEventRequest request = new BehaviorEventRequest();
            request.setEventType(BehaviorEventType.EXERCISE_SUBMITTED);
            request.setGranuleId(10);
            request.setNotion("Algorithmes de Tri");
            request.setRawScore(75.0);

            StudentBehaviorEvent savedEvent = StudentBehaviorEvent.builder()
                    .id(2L)
                    .student(studentUser)
                    .eventType(BehaviorEventType.EXERCISE_SUBMITTED)
                    .granuleId(10)
                    .notion("Algorithmes de Tri")
                    .rawScore(75.0)
                    .build();

            when(behaviorRepository.save(any(StudentBehaviorEvent.class))).thenReturn(savedEvent);

            mockMvc.perform(post("/api/v1/behavior/log")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Event logged successfully"));
        }

        @Test
        @DisplayName("POST /api/v1/behavior/log - Erreur 500 si utilisateur introuvable")
        void logEvent_userNotFound_returns500() throws Exception {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.empty());

            BehaviorEventRequest request = new BehaviorEventRequest();
            request.setEventType(BehaviorEventType.VIDEO_WATCHED);
            request.setGranuleId(5);
            request.setNotion("Réseaux de Neurones");
            request.setDurationSeconds(300);

            mockMvc.perform(post("/api/v1/behavior/log")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("POST /api/v1/behavior/log - Question posée à l'IA (AI_QUESTION_ASKED)")
        void logAiQuestionAskedEvent_success() throws Exception {
            BehaviorEventRequest request = new BehaviorEventRequest();
            request.setEventType(BehaviorEventType.AI_QUESTION_ASKED);
            request.setGranuleId(7);
            request.setNotion("Calcul Différentiel");
            request.setMetadata("{\"questionLevel\":\"INTERMEDIATE\"}");

            when(behaviorRepository.save(any(StudentBehaviorEvent.class)))
                    .thenReturn(StudentBehaviorEvent.builder().id(3L).build());

            mockMvc.perform(post("/api/v1/behavior/log")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // RÉCUPÉRATION DE L'HISTORIQUE COMPORTEMENTAL
    // =========================================================================

    @Nested
    @DisplayName("Historique Comportemental (GET /api/v1/behavior/{studentId})")
    class GetStudentBehavior {

        @Test
        @DisplayName("GET /api/v1/behavior/{id} - Liste des événements récupérée avec succès")
        void getStudentBehavior_success() throws Exception {
            StudentBehaviorEvent event1 = StudentBehaviorEvent.builder()
                    .id(1L)
                    .student(studentUser)
                    .eventType(BehaviorEventType.CONTENT_READ)
                    .notion("POO")
                    .durationSeconds(60)
                    .readDepthPercent(90)
                    .build();

            StudentBehaviorEvent event2 = StudentBehaviorEvent.builder()
                    .id(2L)
                    .student(studentUser)
                    .eventType(BehaviorEventType.EXERCISE_SUBMITTED)
                    .notion("POO")
                    .rawScore(80.0)
                    .build();

            when(behaviorRepository.findByStudentIdOrderByOccurredAtDesc(eq(studentId)))
                    .thenReturn(List.of(event1, event2));

            mockMvc.perform(get("/api/v1/behavior/" + studentId))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/behavior/{id} - Liste vide si aucun événement")
        void getStudentBehavior_emptyList() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(behaviorRepository.findByStudentIdOrderByOccurredAtDesc(eq(unknownId)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/behavior/" + unknownId))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }
}
