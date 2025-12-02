package cm.enspy.xccm.service;


import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public Mono<Void> sendPasswordResetEmail(String to, String token) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Réinitialisation de votre mot de passe - XCCM");
            message.setText(
                "Bonjour,\n\n" +
                "Vous avez demandé la réinitialisation de votre mot de passe.\n\n" +
                "Cliquez sur le lien suivant pour réinitialiser votre mot de passe :\n" +
                "http://localhost:3000/reset-password?token=" + token + "\n\n" +
                "Ce lien est valable pendant 24 heures.\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet email.\n\n" +
                "Cordialement,\nL'équipe XCCM"
            );
            
            mailSender.send(message);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}