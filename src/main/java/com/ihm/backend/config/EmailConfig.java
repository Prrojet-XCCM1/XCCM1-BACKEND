package com.ihm.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfig {
    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        //  Configuration pour Gmail (le plus simple)
        mailSender.setHost(System.getProperty("EMAIL_HOST"));
        mailSender.setPort(Integer.parseInt(System.getProperty("EMAIL_PORT")));

        // ️ À REMPLACER par vos vraies credentials
        mailSender.setUsername(System.getProperty("EMAIL_USERNAME"));
        mailSender.setPassword(System.getProperty("EMAIL_PASSWORD"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }

}
