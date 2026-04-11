# Rapport d'audit Backend — Jour 1
**Date :** 04 avril 2026  
**Auteur :** Personne 3 (BE)  
**Projet :** Sprint LTI 1.3 — XCCM1 × Moodle Client

---

## 1. Stack technique

| Élément | Valeur |
|---|---|
| Framework | Spring Boot **3.3.1** |
| Langage | Java **21** |
| Build | Maven (`mvnw`) |
| Base de données | PostgreSQL (driver `42.x`) |
| ORM | Spring Data JPA / Hibernate |
| Auth JWT | **JJWT 0.12.6** (HMAC SHA-256) |
| Sécurité | Spring Security 6 |
| Dépendances supplémentaires | Lombok, MapStruct, Swagger (springdoc 2.5), Kafka, Actuator, Mail |

---

## 2. Architecture de sécurité existante

### 2.1 Filtre JWT

**Classe :** `com.ihm.backend.security.JwtAuthenticationFilter`  
**Type :** `OncePerRequestFilter`  
**Fonctionnement :**
- Lit le header `Authorization: Bearer <token>`
- Extrait le username (email) via `JwtService.extractUsername()`
- Charge l'utilisateur via `UserDetailsService` (→ `UserRepository.findByEmail()`)
- Valide le token et injecte l'authentification dans le `SecurityContextHolder`

### 2.2 Configuration de sécurité

**Classe :** `com.ihm.backend.security.SecurityConfig`

| Aspect | Valeur |
|---|---|
| Session | `STATELESS` (aucune session HTTP serveur) |
| CSRF | Désactivé (API REST) |
| CORS | Configurable via `CORS_ALLOWED_ORIGINS` (env var) |
| Origines autorisées par défaut | `localhost:3000`, `192.168.1.x:3000`, `frontend-xccm-12027.vercel.app` |

**Routes publiques (sans auth) :**
- `/api/v1/auth/**` — login, register, reset password
- `/swagger-ui/**`, `/v3/api-docs/**` — documentation
- `/courses/**` — consultation des cours
- `/actuator/health` — santé applicative

**Routes protégées :**
- `/api/v1/admin/**` → ROLE_ADMIN uniquement
- Toutes les autres → authentification requise

> ⚠️ **À faire (J3) :** ajouter `/lti/**` dans les routes publiques pour le handshake LTI 1.3

### 2.3 Service JWT

**Classe :** `com.ihm.backend.service.JwtService`
- Algorithme : `HMAC-SHA256` (clé symétrique en Base64)
- Claims générés : `sub` (email), `role`, `iat`, `exp`
- Expiration access token : 24h (configurable via `JWT_EXPIRATION`)
- Refresh token : 7 jours (configurable via `JWT_REFRESH_EXPIRATION`)

> ⚠️ **Implication LTI :** Le JWT Moodle est signé en **RSA256** (clé asymétrique).  
> La dépendance `nimbus-jose-jwt` doit être ajoutée pour valider les signatures RSA.  
> Le `JwtService` existant (HMAC) reste inchangé — il servira à générer le token XCCM1 après SSO.

---

## 3. Entité User

**Classe :** `com.ihm.backend.entity.User`  
**Table :** `users`

| Colonne | Type | Contraintes |
|---|---|---|
| `id` | UUID | PK, auto-généré |
| `email` | String | NOT NULL, UNIQUE |
| `password` | String | NOT NULL (BCrypt) |
| `role` | `UserRole` | NOT NULL (`STUDENT`, `TEACHER`, `ADMIN`) |
| `first_name` | String | NOT NULL |
| `last_name` | String | NOT NULL |
| `photo_url` | String | nullable |
| `city` | String | nullable |
| `university` | String | nullable |
| `specialization` | String | nullable (étudiants) |
| `grade` | String | nullable (enseignants) |
| `subjects` | TEXT | nullable (JSON array en string) |
| `certification` | String | nullable |
| `registration_date` | LocalDateTime | — |
| `last_login` | LocalDateTime | — |
| `updated_at` | LocalDateTime | — |
| `active` | boolean | default `true` |
| `verified` | boolean | default `true` |

> ⚠️ **Colonne à ajouter (J8) :** `moodle_sub` (String, UNIQUE) — identifiant `sub` Moodle  
> pour le **JIT Provisioning** et le mapping de comptes.

---

## 4. Points d'entrée d'authentification

**Contrôleur :** `com.ihm.backend.controller.AuthController` (`/api/v1/auth`)

| Endpoint | Méthode | Description |
|---|---|---|
| `/login` | POST | Authentification email + password → JWT XCCM1 |
| `/register` | POST | Inscription générique (dépréciée) |
| `/register/student` | POST | Inscription étudiant |
| `/register/teacher` | POST | Inscription enseignant |
| `/forgot-password` | POST | Demande reset password (envoi email) |
| `/reset-password` | POST | Reset avec token |

**Service :** `AuthServiceImpl`
- Crée un `User` en BDD, encode le mot de passe (BCrypt), génère un JWT via `JwtService`
- Envoie un email de bienvenue via `NotificationService` > `EmailService`

---

## 5. Repository

**Interface :** `com.ihm.backend.repository.UserRepository`

Méthodes actuelles :
- `findByEmail(String email)` ← utilisée pour le login et le `UserDetailsService`
- `existsByEmail(String email)` ← vérification unicité
- `findByRole(UserRole role)`
- `countByRole(UserRole role)`
- `findByIdAndRole(UUID id, UserRole role)`

> ⚠️ **À ajouter (J8) :** `findByMoodleSub(String moodleSub)` — recherche par identifiant Moodle

---

## 6. Dépendances LTI 1.3 — État actuel

| Ce qu'on a | Ce qu'il manque |
|---|---|
| JJWT (HMAC JWT) ✅ | `nimbus-jose-jwt` pour RSA256 ❌ |
| Spring Security ✅ | Logique de validation JWT LTI ❌ |
| UserRepository ✅ | Champ `moodle_sub` sur `User` ❌ |
| JwtService (génération) ✅ | `LtiLaunchController`, `LtiJwksService` ❌ |
| CORS configurable ✅ | CORS étendu au domaine Moodle ❌ |
| — | CSP `frame-ancestors` pour iframe ❌ |
| — | JWKS endpoint public `/lti/.well-known/jwks.json` ❌ |

---

## 7. Informations à recueillir auprès de l'équipe Moodle Client (J2)

- [ ] URL de la plateforme Moodle (ex: `https://moodle.client.tld`)
- [ ] Version Moodle (doit être **≥ 3.9** pour LTI 1.3)
- [ ] URL du JWKS endpoint Moodle (ex: `.../mod/lti/certs.php`)
- [ ] `client_id` que Moodle attribuera à XCCM1 lors de l'enregistrement de l'outil
- [ ] URL de lancement de l'activité LTI (pour tester en J10)
- [ ] Créneaux disponibles pour les tests conjoints (J10, J12)

---

## 8. Risques identifiés

| Risque | Probabilité | Impact | Mitigation |
|---|---|---|---|
| Moodle ≤ 3.8 (pas LTI 1.3) | Faible | Élevé | Vérifier version dès J2, prévoir LTI 1.1 fallback |
| JWKS Moodle inaccessible en réseau local | Moyen | Élevé | Mock JWKS statique pour les tests dev |
| Cookies tiers bloqués (Safari/iOS) | Élevé | Moyen | Utiliser `sessionStorage` côté frontend (Personne 1) |
| Nonce-replay attack | Faible | Élevé | Stocker nonces en mémoire/Redis, TTL = `exp` du token |

---

## 9. Conclusion

Le backend XCCM1 dispose d'une base solide (Spring Security stateless, JWT, PostgreSQL).  
**Aucune dépendance LTI n'est présente** : tout le travail LTI est à construire from scratch
dans un nouveau package `com.ihm.backend.lti`.  
La logique d'authentification existante (login/register/JWT) n'est **pas à modifier** —
le LTI viendra la compléter comme une voie d'entrée alternative.

**Prochaine étape (J2) :** ajouter `nimbus-jose-jwt` dans `pom.xml` et tenir la réunion
inter-équipes pour obtenir la configuration Moodle.
