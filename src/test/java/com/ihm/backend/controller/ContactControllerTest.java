package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.ContactRequest;
import com.ihm.backend.dto.request.NewsletterRequest;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Services Publics")
    class PublicServices {
        @Test
        @DisplayName("POST /api/v1/public/contact - Succès")
        void contactUs_success() throws Exception {
            ContactRequest req = new ContactRequest();
            req.setName("John Doe");
            req.setEmail("john@test.com");
            req.setSubject("Question");
            req.setMessage("Bonjour");

            mockMvc.perform(post("/api/v1/public/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Votre message a été envoyé avec succès"));

            verify(notificationService).sendContactAcknowledgement("John Doe", "john@test.com", "Question");
        }

        @Test
        @DisplayName("POST /api/v1/public/newsletter - Succès")
        void subscribeNewsletter_success() throws Exception {
            NewsletterRequest req = new NewsletterRequest();
            req.setEmail("tester@test.com");

            mockMvc.perform(post("/api/v1/public/newsletter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Succès ! Vous êtes maintenant inscrit à notre newsletter"));

            verify(notificationService).sendNewsletterConfirmation("tester@test.com");
        }

        @Test
        @DisplayName("POST /api/v1/public/contact - Erreur Interne (500)")
        void contactUs_internalError() throws Exception {
            ContactRequest req = new ContactRequest();
            req.setName("Error");
            req.setEmail("error@test.com");
            req.setSubject("Error Subject");
            req.setMessage("Error message for test");

            doThrow(new RuntimeException("Mail server down"))
                    .when(notificationService).sendContactAcknowledgement(any(), any(), any());

            mockMvc.perform(post("/api/v1/public/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
