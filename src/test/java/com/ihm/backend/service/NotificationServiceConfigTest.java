package com.ihm.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
public class NotificationServiceConfigTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @Test
    void testMailSenderConfiguration() {
        assertThat(mailSender).isInstanceOf(JavaMailSenderImpl.class);
        JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;

        assertThat(impl.getHost()).isEqualTo("smtp.gmail.com");
        assertThat(impl.getPort()).isEqualTo(587);

        Properties props = impl.getJavaMailProperties();
        assertThat(props.getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(props.getProperty("mail.smtp.starttls.required")).isEqualTo("true");
        assertThat(props.getProperty("mail.smtp.connectiontimeout")).isEqualTo("5000");
        assertThat(props.getProperty("mail.smtp.timeout")).isEqualTo("5000");
        assertThat(props.getProperty("mail.smtp.writetimeout")).isEqualTo("5000");
    }
}
