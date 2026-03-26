package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.StudentExerciseSubmissionRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.ExerciseResponse;
import com.ihm.backend.dto.response.StudentExerciseResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.service.ExerciseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExerciseControllerTest {

    @Mock
    private ExerciseService exerciseService;

    @InjectMocks
    private ExerciseController exerciseController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User studentUser;
    private UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(studentId)
                .email("student@test.com")
                .role(UserRole.STUDENT)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(exerciseController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return studentUser;
                    }
                })
                .build();
    }

    private RequestPostProcessor asStudent() {
        return (MockHttpServletRequest request) -> {
            request.setUserPrincipal(
                    new UsernamePasswordAuthenticationToken(studentUser, null,
                            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
            return request;
        };
    }

    @Test
    @DisplayName("GET /api/v1/exercises/course/{courseId} - Lister les exos d'un cours")
    void getExercisesForCourse_success() throws Exception {
        ExerciseResponse res = ExerciseResponse.builder().id(1).title("Exo 1").build();
        when(exerciseService.getExercisesForCourse(eq(10), eq(studentId)))
                .thenReturn(ApiResponse.success("Success", List.of(res)));

        mockMvc.perform(get("/api/v1/exercises/course/10").with(asStudent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Exo 1"));
    }

    @Test
    @DisplayName("POST /api/v1/exercises/{exerciseId}/submit - Soumettre un exo")
    void submitExercise_success() throws Exception {
        StudentExerciseSubmissionRequest req = new StudentExerciseSubmissionRequest();
        req.setContent(Map.of("answer", "Test Answer"));
        StudentExerciseResponse res = StudentExerciseResponse.builder().id(1L).build();

        when(exerciseService.submitExercise(eq(1), eq(studentId), any()))
                .thenReturn(ApiResponse.success("Success", res));

        mockMvc.perform(post("/api/v1/exercises/1/submit")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/exercises/my-submissions - Mes soumissions")
    void getMySubmissions_success() throws Exception {
        StudentExerciseResponse res = StudentExerciseResponse.builder().id(1L).score(10.0).build();
        when(exerciseService.getMySubmissions(studentId))
                .thenReturn(ApiResponse.success("Success", List.of(res)));

        mockMvc.perform(get("/api/v1/exercises/my-submissions").with(asStudent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
