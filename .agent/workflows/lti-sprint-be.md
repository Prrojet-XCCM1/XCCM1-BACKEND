---
description: Workflow Sprint LTI 1.3 — Personne 3 (Backend Engineer) — XCCM1 × Moodle Client
---

# Sprint LTI 1.3 — Personne 3 (Backend) — 15 jours

Ce workflow détaille exactement ce que tu dois faire chaque jour en tant que Personne 3 (BE)
sur le dépôt `XCCM1-BACKEND` (Spring Boot 3.3 / Java 21).

---

## PHASE 1 — Fondations (Jours 1–5)

### Jour 1 — Audit du backend existant

**Objectif :** Cartographier les points d'entrée d'authentification existants.

1. Identifier les classes clés de sécurité :
   - `src/main/java/com/ihm/backend/security/SecurityConfig.java` ✅ (JWT stateless)
   - `src/main/java/com/ihm/backend/security/JwtAuthenticationFilter.java` ✅ (filtre Bearer)
   - `src/main/java/com/ihm/backend/service/JwtService.java` ✅ (JJWT 0.12.6)
   - `src/main/java/com/ihm/backend/controller/AuthController.java`
   - `src/main/java/com/ihm/backend/entity/User.java` ✅ (UUID, email, UserRole)

2. Rédiger le **rapport d'audit** (fichier `docs/audit-backend-j1.md`) :
   - Stack : Spring Boot 3.3.1, Java 21, JJWT 0.12.6, PostgreSQL
   - Auth actuelle : JWT HMAC stateless, filtre `JwtAuthenticationFilter`
   - Dépendances LTI : **aucune** → à ajouter (J2)
   - Point d'entrée auth : `POST /api/v1/auth/**`
   - CORS configuré en dur (à étendre pour domaine Moodle)
   - Table `users` : colonnes UUID, email, password, role, firstName, lastName

**Livrable :** `docs/audit-backend-j1.md`

---

### Jour 2 — Intégration de la dépendance LTI

**Objectif :** Ajouter la librairie LTI 1.3 dans `pom.xml`.

1. Ajouter dans `pom.xml` (section `<dependencies>`) :

```xml
<!-- LTI 1.3 — IMSGlobal reference implementation -->
<dependency>
    <groupId>io.github.emeritus-tech</groupId>
    <artifactId>lti-1-3-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Nimbus JOSE+JWT pour JWKS/RSA -->
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.37.3</version>
</dependency>
```

> **Note :** Si `lti-1-3-core` n'est pas disponible dans Maven Central, utiliser
> `com.nimbusds:nimbus-jose-jwt` seul (suffisant pour valider JWT + fetch JWKS RSA).

2. Vérifier la compilation :
```bash
./mvnw compile -q
```

3. Recueillir auprès de l'équipe Moodle Client (réunion J2) :
   - URL de la plateforme Moodle (ex: `https://moodle.client.tld`)
   - Version Moodle (doit être ≥ 3.9 pour LTI 1.3)
   - URL du JWKS endpoint Moodle (ex: `.../mod/lti/certs.php`)
   - `client_id` à enregistrer dans XCCM1

**Livrable :** `pom.xml` compilable + `docs/fiche-config-moodle-j2.md`

---

### Jours 3–4 — Contrôleur LTI + Fetch JWKS

**Objectif :** Créer le point d'entrée LTI et récupérer les clés publiques Moodle.

1. Créer le package `com.ihm.backend.lti` avec les classes suivantes :

**`LtiLaunchController.java`** — Endpoint POST `/lti/launch`
```java
@RestController
@RequestMapping("/lti")
@RequiredArgsConstructor
@Slf4j
public class LtiLaunchController {

    private final LtiJwksService ltiJwksService;

    @PostMapping("/launch")
    public ResponseEntity<?> launch(HttpServletRequest request) {
        String idToken = request.getParameter("id_token");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body("id_token manquant");
        }
        log.info("LTI Launch reçu, id_token présent (longueur={})", idToken.length());
        // validation JWT → J6
        return ResponseEntity.ok("LTI launch reçu");
    }
}
```

**`LtiJwksService.java`** — Fetch et cache des clés publiques Moodle
```java
@Service
@Slf4j
public class LtiJwksService {

    @Value("${lti.moodle.jwks-url}")
    private String moodleJwksUrl;

    private JWKSet cachedJwkSet;
    private Instant cacheTime;

    public JWKSet getMoodleJwkSet() throws Exception {
        if (cachedJwkSet == null || Duration.between(cacheTime, Instant.now()).toHours() > 1) {
            log.info("Récupération JWKS Moodle depuis {}", moodleJwksUrl);
            cachedJwkSet = JWKSet.load(new URL(moodleJwksUrl));
            cacheTime = Instant.now();
        }
        return cachedJwkSet;
    }
}
```

2. Ajouter dans `application.properties` :
```properties
# ==============================================================
# CONFIGURATION LTI 1.3
# ==============================================================
lti.moodle.jwks-url=${MOODLE_JWKS_URL:https://moodle.client.tld/mod/lti/certs.php}
lti.moodle.platform-url=${MOODLE_PLATFORM_URL:https://moodle.client.tld}
lti.moodle.client-id=${MOODLE_CLIENT_ID:XCCM1_LTI_CLIENT}
lti.xccm1.issuer=${LTI_XCCM1_ISSUER:https://api.xccm1.tld}
```

3. Exposer le endpoint LTI en accès public dans `SecurityConfig.java` :
```java
.requestMatchers("/lti/**").permitAll()
```

**Livrable :** Classes `LtiLaunchController`, `LtiJwksService`, endpoint compilable

---

### Jour 5 — Premier test unitaire JUnit (JWKS)

**Objectif :** Vérifier la récupération JWKS depuis une URL mockée.

1. Créer `src/test/java/com/ihm/backend/lti/LtiJwksServiceTest.java` :

```java
@SpringBootTest
class LtiJwksServiceTest {

    @Test
    void shouldFetchJwksFromMockUrl() throws Exception {
        // Mock WireMock ou utiliser un JWKS statique en ressource de test
        String mockJwks = "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"test\",\"n\":\"...\",\"e\":\"AQAB\"}]}";
        JWKSet set = JWKSet.parse(mockJwks);
        assertNotNull(set);
        assertFalse(set.getKeys().isEmpty());
    }
}
```

2. Lancer les tests :
```bash
./mvnw test -Dtest=LtiJwksServiceTest -q
```

**Livrable :** Test JUnit passant (vert)

---

## PHASE 2 — Développement (Jours 6–11)

### Jours 6–7 — Validation complète du JWT LTI

**Objectif :** Implémenter la validation du JWT signé par Moodle (RSA256).

1. Créer `LtiJwtValidatorService.java` dans `com.ihm.backend.lti` :

**Algorithme de validation :**
- Extraire le `kid` du header du JWT
- Récupérer la clé RSA correspondante depuis le JWKS Moodle
- Vérifier la signature RSA256
- Valider les claims : `iss` (= URL Moodle), `aud` (= `client_id` de XCCM1), `exp`, `iat`
- Vérifier le nonce (stocké en `ConcurrentHashMap<String, Instant>` en mémoire pour dev)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class LtiJwtValidatorService {

    private final LtiJwksService jwksService;

    @Value("${lti.moodle.platform-url}")
    private String expectedIssuer;

    @Value("${lti.moodle.client-id}")
    private String expectedAudience;

    private final Map<String, Instant> usedNonces = new ConcurrentHashMap<>();

    public JWTClaimsSet validateLtiJwt(String idToken) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(idToken);
        String kid = signedJWT.getHeader().getKeyID();

        JWKSet jwkSet = jwksService.getMoodleJwkSet();
        RSAKey rsaKey = (RSAKey) jwkSet.getKeyByKeyId(kid);
        if (rsaKey == null) throw new Exception("Clé JWK introuvable pour kid=" + kid);

        RSASSAVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
        if (!signedJWT.verify(verifier)) throw new Exception("Signature JWT invalide");

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        // Valider iss
        if (!expectedIssuer.equals(claims.getIssuer()))
            throw new Exception("Issuer invalide: " + claims.getIssuer());

        // Valider aud
        if (!claims.getAudience().contains(expectedAudience))
            throw new Exception("Audience invalide");

        // Valider exp
        if (claims.getExpirationTime().before(new Date()))
            throw new Exception("JWT expiré");

        // Valider nonce
        String nonce = (String) claims.getClaim("nonce");
        if (nonce == null || usedNonces.containsKey(nonce))
            throw new Exception("Nonce invalide ou rejoué");
        usedNonces.put(nonce, Instant.now());

        return claims;
    }
}
```

2. Brancher dans `LtiLaunchController.launch()` :
```java
JWTClaimsSet claims = ltiJwtValidatorService.validateLtiJwt(idToken);
log.info("JWT LTI validé pour sub={}", claims.getSubject());
// → continuer vers LtiAccountMappingService (J8)
return ResponseEntity.ok("JWT validé, sub=" + claims.getSubject());
```

**Livrable :** Validation JWT RSA256 opérationnelle

---

### Jours 8–9 — Service de mapping de comptes (JIT Provisioning)

**Objectif :** Associer un utilisateur Moodle à un compte XCCM1 (créer si absent).

1. Ajouter la colonne `moodle_sub` sur l'entité `User` :
```java
@Column(name = "moodle_sub", unique = true)
private String moodleSub;
```

2. Ajouter la méthode dans `UserRepository` :
```java
Optional<User> findByMoodleSub(String moodleSub);
```

3. Créer `LtiAccountMappingService.java` :
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class LtiAccountMappingService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public String resolveAndGenerateToken(JWTClaimsSet claims) {
        String sub = claims.getSubject();
        String email = (String) claims.getClaim("email");
        String givenName = (String) claims.getClaim("given_name");
        String familyName = (String) claims.getClaim("family_name");

        User user = userRepository.findByMoodleSub(sub)
            .orElseGet(() -> {
                // JIT Provisioning : création automatique du compte
                log.info("JIT Provisioning pour sub Moodle={}", sub);
                User newUser = User.builder()
                    .moodleSub(sub)
                    .email(email != null ? email : sub + "@moodle.lti")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(UserRole.TEACHER) // rôle par défaut pour les LTI
                    .firstName(givenName != null ? givenName : "LTI")
                    .lastName(familyName != null ? familyName : "User")
                    .registrationDate(LocalDateTime.now())
                    .active(true)
                    .verified(true)
                    .build();
                return userRepository.save(newUser);
            });

        return jwtService.generateToken(user);
    }
}
```

4. Mettre à jour le contrôleur pour retourner le JWT XCCM1 :
```java
String xccm1Token = ltiAccountMappingService.resolveAndGenerateToken(claims);
return ResponseEntity.ok(Map.of("token", xccm1Token));
```

**Livrable :** Service de mapping fonctionnel, JIT Provisioning testé

---

### Jour 10 — JWKS endpoint public XCCM1

**Objectif :** Exposer les clés publiques RSA de XCCM1 pour que Moodle puisse vérifier
les tokens signés par XCCM1 (si nécessaire en LTI 1.3 Tool Services).

1. Générer une paire de clés RSA XCCM1 (à stocker dans `application.properties`) :
```bash
# Générer clé privée RSA 2048
openssl genrsa -out lti-private.pem 2048
openssl rsa -in lti-private.pem -pubout -out lti-public.pem
```

2. Créer `LtiWellKnownController.java` :
```java
@RestController
@RequestMapping("/lti/.well-known")
public class LtiWellKnownController {

    @GetMapping("/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwks() {
        // Retourner la clé publique RSA de XCCM1 au format JWK
        Map<String, Object> key = Map.of(
            "kty", "RSA",
            "use", "sig",
            "alg", "RS256",
            "kid", "xccm1-lti-key-v1",
            "n", "...", // modulus base64url de la clé publique
            "e", "AQAB"
        );
        return ResponseEntity.ok(Map.of("keys", List.of(key)));
    }
}
```

3. Vérifier l'accessibilité depuis l'URL Moodle partenaire via curl.

**Livrable :** `GET /lti/.well-known/jwks.json` accessible publiquement

---

### Jour 11 — Headers de sécurité inter-domaine (CSP & Cookies)

**Objectif :** Configurer les en-têtes HTTP pour l'intégration iframe.

1. Configurer `Content-Security-Policy` dans `SecurityConfig.java` :
```java
http.headers(headers -> headers
    .frameOptions(frame -> frame.disable()) // On gère nous-mêmes
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("frame-ancestors 'self' https://moodle.client.tld")
    )
);
```

2. Configurer `SameSite=None; Secure` pour les cookies de session :
```properties
# application.properties
server.servlet.session.cookie.same-site=none
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
```

3. Étendre la liste CORS pour inclure le domaine Moodle :
   - Ajouter l'URL Moodle partenaire dans `CORS_ALLOWED_ORIGINS` (variable d'env)

4. Tester les en-têtes réponse :
```bash
curl -I -X POST http://localhost:8082/lti/launch
# Vérifier : Content-Security-Policy: frame-ancestors ...
```

**Livrable :** En-têtes CSP et cookie SameSite configurés et vérifiés

---

## PHASE 3 — Intégration & Tests (Jours 12–15)

### Jours 12–13 — Tests d'intégration Spring + Bug fixes

**Objectif :** Suite de tests d'intégration Spring Boot couvrant les cas critiques.

1. Créer `src/test/java/com/ihm/backend/lti/LtiIntegrationTest.java` :

**Cas de test à couvrir :**
- ✅ JWT valide → mapping compte existant → token XCCM1 retourné
- ✅ JWT valide + nouveau sub → JIT provisioning → compte créé + token retourné
- ❌ JWT expiré → 401
- ❌ Nonce rejoué → 401
- ❌ Mauvais `aud` → 401
- ❌ Signature invalide → 401
- ❌ `id_token` absent → 400

2. Lancer les tests :
```bash
./mvnw test -Dtest=LtiIntegrationTest
./mvnw test  # Suite complète
```

3. Corriger les bugs backend remontés lors des tests J12.

**Livrable :** Suite de tests intégration Spring (tous verts)

---

### Jour 14 — Documentation backend

**Objectif :** Rédiger la documentation technique backend LTI.

1. Créer `docs/backend-lti-docs.md` :
   - Architecture LTI 1.3 côté XCCM1
   - Diagramme de séquence : Moodle → `POST /lti/launch` → validation JWT → mapping → token XCCM1
   - Paramètres `application.properties` / variables d'environnement à configurer
   - Procédure de renouvellement des clés JWK RSA
   - Endpoints exposés : `/lti/launch`, `/lti/.well-known/jwks.json`

**Livrable :** `docs/backend-lti-docs.md`

---

### Jour 15 — Déploiement staging

**Objectif :** Déployer la version LTI sur l'environnement de staging.

1. Vérifier les variables d'environnement en staging :
```
MOODLE_JWKS_URL=https://moodle.client.tld/mod/lti/certs.php
MOODLE_PLATFORM_URL=https://moodle.client.tld
MOODLE_CLIENT_ID=...
LTI_XCCM1_ISSUER=https://api.xccm1.tld
CORS_ALLOWED_ORIGINS=https://moodle.client.tld,https://frontend-xccm-12027.vercel.app
```

2. Build et run Docker :
```bash
./mvnw package -DskipTests
docker build -t xccm1-backend:v1.0-lti .
# Ou déployer via render.yaml (déjà configuré)
```

3. Vérifier les logs en conditions réelles :
```bash
# Chercher "LTI Launch reçu" et "JWT LTI validé"
```

4. Figer la configuration de sécurité.

**Livrable :** Déploiement staging opérationnel, logs de lancement LTI vérifiés

---

## Résumé des fichiers à créer/modifier

| Fichier | Action | Jour |
|---|---|---|
| `pom.xml` | Ajouter nimbus-jose-jwt | J2 |
| `application.properties` | Ajouter config LTI | J3 |
| `SecurityConfig.java` | Autoriser `/lti/**`, CSP headers | J3, J11 |
| `lti/LtiLaunchController.java` | **[NEW]** Endpoint POST `/lti/launch` | J3 |
| `lti/LtiJwksService.java` | **[NEW]** Fetch + cache JWKS Moodle | J3 |
| `lti/LtiJwtValidatorService.java` | **[NEW]** Validation JWT RSA256 + nonce | J6 |
| `lti/LtiAccountMappingService.java` | **[NEW]** JIT Provisioning + token XCCM1 | J8 |
| `lti/LtiWellKnownController.java` | **[NEW]** JWKS endpoint public | J10 |
| `entity/User.java` | Ajouter `moodleSub` | J8 |
| `repository/UserRepository.java` | Ajouter `findByMoodleSub()` | J8 |
| `docs/audit-backend-j1.md` | **[NEW]** Rapport d'audit J1 | J1 |
| `docs/fiche-config-moodle-j2.md` | **[NEW]** Config Moodle collectée J2 | J2 |
| `docs/backend-lti-docs.md` | **[NEW]** Documentation backend finale | J14 |
| Tests JUnit | Créer tests JWKS + intégration | J5, J12 |

---

## Variables d'environnement à configurer

```bash
# LTI 1.3
MOODLE_JWKS_URL=https://moodle.client.tld/mod/lti/certs.php
MOODLE_PLATFORM_URL=https://moodle.client.tld
MOODLE_CLIENT_ID=<client_id_fourni_par_equipe_moodle>
LTI_XCCM1_ISSUER=https://api.xccm1.tld

# Sécurité inter-domaine
CORS_ALLOWED_ORIGINS=https://moodle.client.tld,https://frontend-xccm-12027.vercel.app

# Existants (déjà configurés)
JWT_SECRET=...
DB_HOST=...
```
