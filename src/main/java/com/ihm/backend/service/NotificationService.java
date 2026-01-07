// src/main/java/com/ihm/backend/service/NotificationService.java

package com.ihm.backend.service;

import com.ihm.backend.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:XCCM}")
    private String appName;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    // ================================================================
    // ENVOI D'EMAIL ASYNCHRONE GÉNÉRIQUE
    // ================================================================
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email envoyé avec succès à : {}", to);

        } catch (Exception e) {
            log.error("Échec de l'envoi d'email à {} : {}", to, e.getMessage());
            // On ne fait pas planter l'app si l'email échoue
        }
    }

    // ================================================================
    // EMAIL DE BIENVENUE
    // ================================================================
    @Async
    public void sendWelcomeEmail(User user) {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; padding: 40px 20px; text-align: center; }
                        .content { padding: 40px 30px; color: #333; line-height: 1.7; }
                        .button { display: inline-block; background: #6366f1; color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0; }
                        .footer { background: #f8fafc; padding: 25px; text-align: center; color: #64748b; font-size: 13px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Bienvenue sur %s !</h1>
                        </div>
                        <div class="content">
                            <h2>Bonjour %s,</h2>
                            <p>Nous sommes ravis de vous accueillir dans la communauté <strong>%s</strong>.</p>
                            <p>Votre compte a été créé avec succès. Vous pouvez dès maintenant vous connecter et découvrir la plateforme.</p>
                            <div style="text-align: center;">
                                <a href="%s" class="button">Accéder à mon compte</a>
                            </div>
                            <p>À très bientôt !</p>
                        </div>
                        <div class="footer">
                            © 2025 %s • ENSP Yaoundé<br>
                            Cet email a été envoyé automatiquement.
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(appName, user.getFullName(), appName, frontendUrl, appName);

        sendHtmlEmail(user.getEmail(), "Bienvenue sur " + appName + " !", html);
    }

    // ================================================================
    // EMAIL DE RÉINITIALISATION DE MOT DE PASSE
    // ================================================================
    @Async
    public void sendPasswordResetEmail(User user, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1); }
                        .header { background: #ef4444; color: white; padding: 40px 20px; text-align: center; }
                        .content { padding: 40px 30px; color: #333; line-height: 1.7; }
                        .button { display: inline-block; background: #ef4444f4; color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0; }
                        .warning { background: #fee2e2; border: 1px solid #fecaca; padding: 15px; border-radius: 8px; margin: 20px 0; }
                        .footer { background: #f8fafc; padding: 25px; text-align: center; color: #64748b; font-size: 13px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Réinitialisation de mot de passe</h1>
                        </div>
                        <div class="content">
                            <p>Bonjour <strong>%s</strong>,</p>
                            <p>Vous avez demandé à réinitialiser votre mot de passe sur <strong>%s</strong>.</p>
                            <p>Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :</p>
                            <div style="text-align: center;">
                                <a href="%s" class="button">Créer un nouveau mot de passe</a>
                            </div>
                            <div class="warning">
                                Ce lien expire dans <strong>1 heure</strong>.<br>
                                Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
                            </div>
                            <p>Lien direct : <a href="%s">%s</a></p>
                        </div>
                        <div class="footer">
                            © 2025 %s • Support technique
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(user.getFullName(), appName, resetUrl, resetUrl, resetUrl, appName);

        sendHtmlEmail(user.getEmail(), "Réinitialisez votre mot de passe", html);
    }

    // ================================================================
    // EMAIL DE PUBLICATION DE COURS
    // ================================================================
    @Async
    public void sendCoursePublishedEmail(User teacher, String courseTitle) {
        String html = """
                <div style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #4f46e5;">Félicitations ! Votre cours est en ligne</h2>
                    <p>Bonjour %s,</p>
                    <p>Nous avons le plaisir de vous informer que votre cours "<strong>%s</strong>" vient d'être publié avec succès sur <strong>%s</strong>.</p>
                    <p>Il est désormais visible par tous les étudiants.</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: #4f46e5; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;">Voir mes cours</a>
                    </div>
                </div>
                """
                .formatted(teacher.getFirstName(), courseTitle, appName, frontendUrl + "/teacher/courses");

        sendHtmlEmail(teacher.getEmail(), "Votre cours est maintenant publié !", html);
    }

    // ================================================================
    // EMAIL NOUVEL ENRÔLEMENT (Pour l'enseignant)
    // ================================================================
    @Async
    public void sendNewEnrollmentNotification(User teacher, String studentName, String courseTitle) {
        String html = """
                <div style="font-family: sans-serif; padding: 20px;">
                    <h3>Nouvel enrôlement sur votre cours</h3>
                    <p>Bonjour %s,</p>
                    <p>L'étudiant <strong>%s</strong> vient de s'enrôler à votre cours "<strong>%s</strong>".</p>
                    <p>Vous pouvez valider sa demande depuis votre tableau de bord.</p>
                    <a href="%s">Gérer les enrôlements</a>
                </div>
                """.formatted(teacher.getFirstName(), studentName, courseTitle, frontendUrl + "/teacher/enrollments");

        sendHtmlEmail(teacher.getEmail(), "Nouveau participant : " + studentName, html);
    }

    // ================================================================
    // EMAIL ENRÔLEMENT ACCEPTÉ (Pour l'étudiant)
    // ================================================================
    @Async
    public void sendEnrollmentAcceptedEmail(User student, String courseTitle) {
        String html = """
                <div style="font-family: sans-serif; padding: 20px; border-left: 4px solid #10b981;">
                    <h2 style="color: #059669;">Accès au cours accordé !</h2>
                    <p>Bonjour %s,</p>
                    <p>Bonne nouvelle ! Votre demande d'enrôlement au cours "<strong>%s</strong>" a été acceptée par l'enseignant.</p>
                    <p>Vous pouvez maintenant commencer à apprendre.</p>
                    <a href="%s" style="display: inline-block; background: #10b981; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Commencer le cours</a>
                </div>
                """
                .formatted(student.getFirstName(), courseTitle, frontendUrl + "/student/courses");

        sendHtmlEmail(student.getEmail(), "Admission acceptée : " + courseTitle, html);
    }

    // ================================================================
    // EMAIL DE CONTACT / NEWSLETTER
    // ================================================================
    @Async
    public void sendNewsletterConfirmation(String email) {
        String html = """
                <div style="text-align: center; padding: 30px;">
                    <h1 style="color: #6366f1;">Merci de votre confiance !</h1>
                    <p>Vous êtes maintenant inscrit à la newsletter de <strong>%s</strong>.</p>
                    <p>Vous recevrez nos prochaines actualités et nouveaux cours en avant-première.</p>
                </div>
                """.formatted(appName);
        sendHtmlEmail(email, "Confirmation d'inscription newsletter", html);
    }

    @Async
    public void sendContactAcknowledgement(String name, String email, String subject) {
        String html = """
                <p>Bonjour %s,</p>
                <p>Nous avons bien reçu votre message concernant : "<strong>%s</strong>".</p>
                <p>Notre équipe vous répondra dans les plus brefs délais.</p>
                <p>Merci de nous avoir contactés.</p>
                """.formatted(name, subject);
        sendHtmlEmail(email, "Accusé de réception : " + subject, html);
    }
}