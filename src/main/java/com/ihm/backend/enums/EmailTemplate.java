package com.ihm.backend.enums;

/**
 * Types de templates d'email disponibles
 */
public enum EmailTemplate {
    WELCOME("Bienvenue sur XCCM", "welcome.html"),
    COURSE_PUBLISHED("Nouveau cours disponible", "course-published.html"),
    ENROLLMENT_CONFIRMATION("Inscription confirmée", "enrollment-confirmation.html"),
    PASSWORD_RESET("Réinitialisation de mot de passe", "password-reset.html"),
    COMPOSITION_SHARED("Composition partagée avec vous", "composition-shared.html"),
    ADMIN_NOTIFICATION("Notification administrateur", "admin-notification.html");

    private final String subject;
    private final String templateName;

    EmailTemplate(String subject, String templateName) {
        this.subject = subject;
        this.templateName = templateName;
    }

    public String getSubject() {
        return subject;
    }

    public String getTemplateName() {
        return templateName;
    }
}