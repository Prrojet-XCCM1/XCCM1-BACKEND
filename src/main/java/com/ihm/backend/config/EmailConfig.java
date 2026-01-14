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

        // Récupération des variables d'environnement (Render)
        String host = System.getenv("EMAIL_HOST");
        String portStr = System.getenv("EMAIL_PORT");
        String username = System.getenv("EMAIL_USERNAME");
        String password = System.getenv("EMAIL_PASSWORD");

        // Validation de sécurité pour éviter le "NumberFormatException: null"
        if (host == null || portStr == null || username == null || password == null) {
            System.err.println("ATTENTION : Une ou plusieurs variables d'environnement EMAIL_ sont manquantes !");
            // Optionnel : mettre des valeurs par défaut si nécessaire
            host = (host == null) ? "smtp.gmail.com" : host;
            portStr = (portStr == null) ? "587" : portStr;
        }

        mailSender.setHost(host);
        mailSender.setPort(Integer.parseInt(portStr));
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        // Configuration des propriétés SMTP
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false"); // Passez à "true" pour voir les logs d'envoi détaillés

        return mailSender;
    }
}