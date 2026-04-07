package com.ihm.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${application.security.jwt.secret-key:ce4f01beeee4257f8ae44276369cd65974b8e114a9103cdc738f01c391a9b023}")
    private String secretKey;

    @Value("${application.security.jwt.expiration:86400000}") // 24h par défaut
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-expiration:604800000}") // 7 jours
    private long refreshExpiration;

 // Génère le token d'accès
 public String generateToken(UserDetails userDetails) {
     return generateToken(new HashMap<>(), userDetails, jwtExpiration);
 }

 // Génère un refresh token (plus long)
 public String generateRefreshToken(UserDetails userDetails) {
     return generateToken(new HashMap<>(), userDetails, refreshExpiration);
 }

 public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
     return Jwts.builder()
             .claims(extraClaims)
             .subject(userDetails.getUsername())
             .claim("role", userDetails.getAuthorities().iterator().next().getAuthority())
             .issuedAt(new Date(System.currentTimeMillis()))
             .expiration(new Date(System.currentTimeMillis() + expiration))
             .signWith(getSignInKey())
             .compact();
 }

 public String extractUsername(String token) {
     return extractClaim(token, Claims::getSubject);
 }

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

 public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
     final Claims claims = extractAllClaims(token);
     return claimsResolver.apply(claims);
 }

 private Claims extractAllClaims(String token) {
     try {
         return Jwts.parser()
                 .verifyWith(getSignInKey())
                 .build()
                 .parseSignedClaims(token)
                 .getPayload();
     } catch (JwtException | IllegalArgumentException e) {
         log.error("Invalid JWT token: {}", e.getMessage());
         throw new JwtException("Token JWT invalide ou expiré");
     }
 }

 private SecretKey getSignInKey() {
     byte[] keyBytes = Decoders.BASE64.decode(secretKey);
     return Keys.hmacShaKeyFor(keyBytes);
 }
}