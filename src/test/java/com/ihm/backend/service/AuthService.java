package com.ihm.backend.service;

import com.ihm.backend.entity.Utilisateur;
import com.ihm.backend.repository.UtilisateurRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;  // Import supplémentaire pour Keys
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;  // Import pour SecretKey (optionnel, mais utile)

@Service
public class AuthService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    public String login(String email, String password) {
        Utilisateur user = utilisateurRepository.findByEmail(email);
        if (user != null && user.getMotDePasse().equals(password)) {  // Simplifié, en prod use BCrypt
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole().name());

            // Générez la clé secrète
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            return Jwts.builder()
                    .claims(claims)
                    .subject(user.getEmail())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(key)  // Utilisez signWith(Key) – l'algo est inféré (HS512 pour clé appropriée)
                    .compact();
        }
        throw new RuntimeException("Invalid credentials");
    }
}