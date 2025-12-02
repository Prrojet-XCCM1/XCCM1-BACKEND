package cm.enspy.xccm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Fournisseur de Token JWT (JSON Web Token)
 * Gère la génération, l'extraction et la validation des tokens.
 */
@Service
public class JwtTokenProvider {

    // Clé secrète stockée dans application.yml/properties
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    // Durée de validité du token (par défaut: 24 heures)
    @Value("${application.security.jwt.expiration:86400000}")
    private long jwtExpiration;
    
    // Durée de validité du token de rafraîchissement (par défaut: 7 jours)
    @Value("${application.security.jwt.refresh-token.expiration:604800000}")
    private long refreshExpiration;

    // --- Génération de Token ---

    /**
     * Génère un token JWT simple sans claims supplémentaires.
     * @param userDetails Les détails de l'utilisateur (email, rôles)
     * @return Le token JWT généré
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Génère un token JWT avec des claims supplémentaires (par exemple, le rôle).
     * @param extraClaims Claims supplémentaires à ajouter au token
     * @param userDetails Les détails de l'utilisateur
     * @return Le token JWT généré
     */
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }
    
    /**
     * Génère un token de rafraîchissement
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // Subject est l'email
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // --- Extraction des informations ---

    /**
     * Extrait l'email (le subject) du token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // --- Validation du Token ---

    /**
     * Vérifie si le token est valide.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // --- Gestion de la Clé Secrète ---

    /**
     * Crée la clé de signature (Key) à partir de la chaîne de caractères Base64.
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}