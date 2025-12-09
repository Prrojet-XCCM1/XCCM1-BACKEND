package com.ihm.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;  // CET IMPORT MANQUAIT !

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private final JavaMailSender mailSender;

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "XCCM Support");
            helper.setTo(to);
            helper.setSubject("Réinitialisation de votre mot de passe");

            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                    <h2 style="color: #2563eb;">Réinitialisation de mot de passe</h2>
                    <p>Bonjour,</p>
                    <p>Vous avez demandé une réinitialisation de mot de passe.</p>
                    <p>Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" 
                           style="background-color: #2563eb; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold;">
                            Réinitialiser mon mot de passe
                        </a>
                    </div>
                    <p>Ce lien expire dans 1 heure.</p>
                    <p>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>
                    <hr>
                    <small style="color: #666;">© 2025 XCCM - Tous droits réservés</small>
                </div>
                """.formatted(resetLink);

            

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Email de réinitialisation envoyé à {}", to);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Échec de l'envoi de l'email à {} : {}", to, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation", e);
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "XCCM Plateforme");
            helper.setTo(to);
            helper.setSubject("Bienvenue sur XCCM !");

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px;">
                    <h2 style="color: #16a34a;">Bienvenue %s !</h2>
                    <p>Votre compte a été créé avec succès.</p>
                    <p>Vous pouvez maintenant vous connecter et compléter votre profil.</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" 
                           style="background-color: #16a34a; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px;">
                            Aller à mon tableau de bord
                        </a>
                    </div>
                    <p>À très bientôt !</p>
                </div>
                """.formatted(name, frontendUrl);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Email de bienvenue envoyé à {}", to);

        } catch (Exception e) {
            log.warn("Échec envoi email bienvenue à {} : {}", to, e.getMessage());
        }
    }
}