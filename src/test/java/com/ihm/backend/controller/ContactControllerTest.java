package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.ContactRequest;
import com.ihm.backend.dto.request.NewsletterRequest;
import com.ihm.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ContactController contactController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contactController).build();
    }

    @Test
    @DisplayName("POST /api/v1/public/contact - Envoyer un message de contact")
    void contactUs_success() throws Exception {
        ContactRequest req = new ContactRequest();
        req.setName("John Doe");
        req.setEmail("john@test.com");
        req.setSubject("Test Subject");
        req.setMessage("Hello world");

        mockMvc.perform(post("/api/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Votre message a été envoyé avec succès"));

        verify(notificationService).sendContactAcknowledgement("John Doe", "john@test.com", "Test Subject");
    }

    @Test
    @DisplayName("POST /api/v1/public/newsletter - Inscription newsletter")
    void subscribeNewsletter_success() throws Exception {
        NewsletterRequest req = new NewsletterRequest();
        req.setEmail("tester@test.com");

        mockMvc.perform(post("/api/v1/public/newsletter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Succès ! Vous êtes maintenant inscrit à notre newsletter"));

        verify(notificationService).sendNewsletterConfirmation("tester@test.com");
    }
}
