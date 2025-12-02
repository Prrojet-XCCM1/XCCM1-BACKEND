package cm.enspy.xccm.service;

import cm.enspy.xccm.domain.dto.EmailDto;
import cm.enspy.xccm.domain.entity.Course;
import cm.enspy.xccm.domain.entity.User;
import cm.enspy.xccm.domain.enums.EmailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de notification par email (réactif)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:XCCM}")
    private String appName;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    /**
     * Envoie un email de manière réactive
     */
    public Mono<Void> sendEmail(EmailDto emailDto) {
        return Mono.fromCallable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(emailDto.getTo());
                helper.setSubject(emailDto.getSubject());
                
                String emailContent = emailDto.getBody();
                
                // Si un template est spécifié, générer le HTML
                if (emailDto.getTemplate() != null) {
                    emailContent = generateEmailFromTemplate(
                        emailDto.getTemplate(), 
                        emailDto.getTemplateVariables()
                    );
                }
                
                helper.setText(emailContent, emailDto.isHtml());
                
                mailSender.send(message);
                
                log.info("Email envoyé avec succès à: {}", emailDto.getTo());
                return null;
                
            } catch (MessagingException e) {
                log.error("Erreur lors de l'envoi de l'email à: {}", emailDto.getTo(), e);
                throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic()) // Exécution sur un thread séparé
        .then();
    }

    /**
     * Email de bienvenue après inscription
     */
    public Mono<Void> sendWelcomeEmail(User user) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", user.getFirstName() + " "+user.getLastName() );
        variables.put("userEmail", user.getEmail());
        variables.put("appUrl", appUrl);
        variables.put("appName", appName);

        EmailDto emailDto = EmailDto.builder()
            .to(user.getEmail())
            .subject(EmailTemplate.WELCOME.getSubject())
            .template(EmailTemplate.WELCOME)
            .templateVariables(variables)
            .isHtml(true)
            .build();

        return sendEmail(emailDto);
    }

    /**
     * Notification de publication de cours
     */
    public Mono<Void> sendCoursePublishedEmail(User teacher, Course course) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("teacherName", teacher.getFirstName() + " "+teacher.getLastName());
        variables.put("courseTitle", course.getTitle());
        variables.put("courseDescription", course.getDescription());
        variables.put("courseUrl", appUrl + "/courses/" + course.getId());
        variables.put("appName", appName);

        EmailDto emailDto = EmailDto.builder()
            .to(teacher.getEmail())
            .subject(EmailTemplate.COURSE_PUBLISHED.getSubject())
            .template(EmailTemplate.COURSE_PUBLISHED)
            .templateVariables(variables)
            .isHtml(true)
            .build();

        return sendEmail(emailDto);
    }

    /**
     * Confirmation d'inscription à un cours
     */
    public Mono<Void> sendEnrollmentConfirmationEmail(User student, Course course) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("studentName", student.getFirstName() + " "+student.getLastName());
        variables.put("courseTitle", course.getTitle());
        variables.put("courseUrl", appUrl + "/courses/" + course.getId());
        variables.put("enrollmentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        variables.put("appName", appName);

        EmailDto emailDto = EmailDto.builder()
            .to(student.getEmail())
            .subject(EmailTemplate.ENROLLMENT_CONFIRMATION.getSubject())
            .template(EmailTemplate.ENROLLMENT_CONFIRMATION)
            .templateVariables(variables)
            .isHtml(true)
            .build();

        return sendEmail(emailDto);
    }

    /**
     * Email de réinitialisation de mot de passe
     */
    public Mono<Void> sendPasswordResetEmail(User user, String resetToken) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", user.getFirstName() + " "+user.getLastName());
        variables.put("resetUrl", appUrl + "/reset-password?token=" + resetToken);
        variables.put("expirationTime", "24 heures");
        variables.put("appName", appName);

        EmailDto emailDto = EmailDto.builder()
            .to(user.getEmail())
            .subject(EmailTemplate.PASSWORD_RESET.getSubject())
            .template(EmailTemplate.PASSWORD_RESET)
            .templateVariables(variables)
            .isHtml(true)
            .build();

        return sendEmail(emailDto);
    }

    /**
     * Notification de partage de composition
     */
    public Mono<Void> sendCompositionSharedEmail(User recipient, User sharer, String compositionTitle) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", recipient.getFirstName() + " "+recipient.getLastName());
        variables.put("sharerName", sharer.getFirstName() + " "+sharer.getLastName());
        variables.put("compositionTitle", compositionTitle);
        variables.put("appUrl", appUrl);
        variables.put("appName", appName);

        EmailDto emailDto = EmailDto.builder()
            .to(recipient.getEmail())
            .subject(EmailTemplate.COMPOSITION_SHARED.getSubject())
            .template(EmailTemplate.COMPOSITION_SHARED)
            .templateVariables(variables)
            .isHtml(true)
            .build();

        return sendEmail(emailDto);
    }

    /**
     * Génère le HTML d'un email à partir d'un template
     */
    private String generateEmailFromTemplate(EmailTemplate template, Map<String, Object> variables) {
        // Remplacer les variables dans le template
        String html = getTemplateHtml(template);
        
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                html = html.replace(placeholder, value);
            }
        }
        
        return html;
    }

    /**
     * Retourne le HTML du template demandé
     */
    private String getTemplateHtml(EmailTemplate template) {
        return switch (template) {
            case WELCOME -> getWelcomeTemplate();
            case COURSE_PUBLISHED -> getCoursePublishedTemplate();
            case ENROLLMENT_CONFIRMATION -> getEnrollmentConfirmationTemplate();
            case PASSWORD_RESET -> getPasswordResetTemplate();
            case COMPOSITION_SHARED -> getCompositionSharedTemplate();
            case ADMIN_NOTIFICATION -> getAdminNotificationTemplate();
        };
    }

    // ========================================================================
    // TEMPLATES HTML
    // ========================================================================

    private String getWelcomeTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4F46E5; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .button { display: inline-block; padding: 12px 30px; background: #4F46E5; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Bienvenue sur {{appName}} ! 🎓</h1>
                    </div>
                    <div class="content">
                        <h2>Bonjour {{userName}},</h2>
                        <p>Nous sommes ravis de vous accueillir sur <strong>{{appName}}</strong>, votre plateforme de gestion de contenu pédagogique modulaire.</p>
                        
                        <p>Votre compte a été créé avec succès avec l'adresse email : <strong>{{userEmail}}</strong></p>
                        
                        <h3>Que pouvez-vous faire maintenant ?</h3>
                        <ul>
                            <li>📚 Créer et gérer vos compositions pédagogiques</li>
                            <li>🧩 Organiser votre contenu en granules réutilisables</li>
                            <li>📤 Publier vos cours pour les étudiants</li>
                            <li>📊 Suivre l'engagement des étudiants</li>
                        </ul>
                        
                        <a href="{{appUrl}}" class="button">Accéder à la plateforme</a>
                        
                        <p>Si vous avez des questions, n'hésitez pas à nous contacter.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 {{appName}} - École Nationale Supérieure Polytechnique de Yaoundé</p>
                        <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getCoursePublishedTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #10B981; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .course-box { background: white; padding: 20px; border-left: 4px solid #10B981; margin: 20px 0; }
                    .button { display: inline-block; padding: 12px 30px; background: #10B981; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Cours publié avec succès !</h1>
                    </div>
                    <div class="content">
                        <h2>Félicitations {{teacherName}} ! 🎉</h2>
                        <p>Votre cours a été publié avec succès sur {{appName}}.</p>
                        
                        <div class="course-box">
                            <h3>{{courseTitle}}</h3>
                            <p>{{courseDescription}}</p>
                        </div>
                        
                        <p>Les étudiants peuvent maintenant découvrir et s'inscrire à votre cours.</p>
                        
                        <a href="{{courseUrl}}" class="button">Voir mon cours</a>
                        
                        <p><strong>Prochaines étapes :</strong></p>
                        <ul>
                            <li>Partagez le lien de votre cours avec vos étudiants</li>
                            <li>Suivez les inscriptions depuis votre tableau de bord</li>
                            <li>Continuez à enrichir votre contenu</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>© 2025 {{appName}}</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getEnrollmentConfirmationTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #3B82F6; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .info-box { background: white; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .button { display: inline-block; padding: 12px 30px; background: #3B82F6; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎓 Inscription confirmée !</h1>
                    </div>
                    <div class="content">
                        <h2>Bonjour {{studentName}},</h2>
                        <p>Votre inscription au cours suivant a été confirmée avec succès :</p>
                        
                        <div class="info-box">
                            <h3>{{courseTitle}}</h3>
                            <p><strong>Date d'inscription :</strong> {{enrollmentDate}}</p>
                        </div>
                        
                        <p>Vous avez maintenant accès à tout le contenu du cours.</p>
                        
                        <a href="{{courseUrl}}" class="button">Accéder au cours</a>
                        
                        <p><strong>Conseils pour bien commencer :</strong></p>
                        <ul>
                            <li>📖 Explorez le contenu à votre rythme</li>
                            <li>📝 Prenez des notes directement dans la plateforme</li>
                            <li>💬 N'hésitez pas à poser des questions</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>© 2025 {{appName}}</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getPasswordResetTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #EF4444; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .warning { background: #FEF2F2; border-left: 4px solid #EF4444; padding: 15px; margin: 20px 0; }
                    .button { display: inline-block; padding: 12px 30px; background: #EF4444; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Réinitialisation de mot de passe</h1>
                    </div>
                    <div class="content">
                        <h2>Bonjour {{userName}},</h2>
                        <p>Vous avez demandé à réinitialiser votre mot de passe pour votre compte {{appName}}.</p>
                        
                        <p>Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe :</p>
                        
                        <a href="{{resetUrl}}" class="button">Réinitialiser mon mot de passe</a>
                        
                        <div class="warning">
                            <p><strong>⚠️ Important :</strong></p>
                            <ul>
                                <li>Ce lien est valide pendant {{expirationTime}}</li>
                                <li>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email</li>
                                <li>Ne partagez jamais ce lien avec personne</li>
                            </ul>
                        </div>
                        
                        <p>Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :</p>
                        <p style="word-break: break-all; color: #666;">{{resetUrl}}</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 {{appName}}</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getCompositionSharedTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #8B5CF6; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .button { display: inline-block; padding: 12px 30px; background: #8B5CF6; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📤 Composition partagée</h1>
                    </div>
                    <div class="content">
                        <h2>Bonjour {{recipientName}},</h2>
                        <p><strong>{{sharerName}}</strong> a partagé une composition avec vous :</p>
                        
                        <div style="background: white; padding: 20px; border-left: 4px solid #8B5CF6; margin: 20px 0;">
                            <h3>{{compositionTitle}}</h3>
                        </div>
                        
                        <a href="{{appUrl}}" class="button">Voir la composition</a>
                        
                        <p>Vous pouvez maintenant consulter, modifier et réutiliser cette composition dans vos propres cours.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 {{appName}}</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getAdminNotificationTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #64748B; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔔 Notification Administrateur</h1>
                    </div>
                    <div class="content">
                        <p>Notification administrative pour {{appName}}.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 {{appName}}</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
}