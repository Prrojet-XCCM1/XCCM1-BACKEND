# Rapport Backend — Intégration LTI 1.3 (XCCM1 × Moodle Client)

**Auteur :** Gabrielle (Personne 3 — Backend)
**Branche :** `plugin-gabrielle`
**Date :** 27 avril 2026
**Sprint :** 15 jours — Intégration LTI 1.3

---

## 1. Résumé exécutif

L'intégration du protocole LTI 1.3 côté backend Spring Boot est **complète et fonctionnelle**. Elle couvre l'intégralité des tâches assignées au rôle Backend (Personne 3) dans le plan de sprint :

- Configuration du protocole LTI 1.3 (endpoints, JWKS, OIDC)
- Validation cryptographique des JWT émis par Moodle (RSA256 + JWKS)
- SSO avec mapping de comptes et JIT Provisioning
- En-têtes de sécurité inter-domaine (CSP, X-Frame-Options)
- **36 tests unitaires** — tous verts

---

## 2. Architecture LTI 1.3 côté Backend

### 2.1 Composants créés

| Composant | Fichier | Rôle |
|-----------|---------|------|
| **LtiLoginController** | `controller/LtiLoginController.java` | Endpoint `GET /lti/login` — OIDC Login Initiation |
| **LtiLaunchController** | `controller/LtiLaunchController.java` | Endpoint `POST /lti/launch` — réception du `id_token` Moodle |
| **LtiWellKnownController** | `controller/LtiWellKnownController.java` | Endpoint `GET /lti/.well-known/jwks.json` — clé publique RSA |
| **LtiJwtValidatorService** | `service/LtiJwtValidatorService.java` | Validation complète du JWT LTI (signature RSA, claims, nonce) |
| **LtiJwksService** | `service/LtiJwksService.java` | Récupération et cache du JWKS Moodle (TTL 1h) |
| **LtiAccountMappingService** | `service/LtiAccountMappingService.java` | Mapping de comptes + JIT Provisioning |
| **LtiKeyService** | `service/LtiKeyService.java` | Gestion de la paire RSA XCCM1 (chargement PEM ou génération) |
| **LtiOidcStateService** | `service/LtiOidcStateService.java` | Validation du paramètre `state` OIDC (anti-CSRF) |
| **LtiRoleMapper** | `service/LtiRoleMapper.java` | Mapping des rôles LTI (`#Instructor` → `TEACHER`, `#Learner` → `STUDENT`) |

### 2.2 Flux de lancement (Launch Flow)

```
Moodle                     XCCM1 Backend
  │                              │
  │  GET /lti/login              │
  │  (iss, login_hint, ...)      │
  │─────────────────────────────►│
  │                              │ registerState() → state UUID
  │  302 → Moodle auth.php       │
  │◄─────────────────────────────│
  │                              │
  │  POST /lti/launch            │
  │  (id_token JWT, state)       │
  │─────────────────────────────►│
  │                              │ validateAndConsume(state)
  │                              │ validateLtiJwt(id_token)
  │                              │   ├─ fetch JWKS Moodle
  │                              │   ├─ verify RSA256 signature
  │                              │   ├─ check iss, aud, exp
  │                              │   └─ check nonce (anti-replay)
  │                              │ resolveAndGenerateToken(claims)
  │                              │   ├─ findByMoodleSub(sub)
  │                              │   ├─ [ou] findByEmail(email) → liaison
  │                              │   └─ [ou] createNewUser() JIT
  │  200 { token, status, user } │
  │◄─────────────────────────────│
```

---

## 3. Détail des services implémentés

### 3.1 LtiJwtValidatorService

Valide le JWT LTI émis par Moodle en 4 étapes :

1. **Extraction du `kid`** depuis le header JWT
2. **Récupération du JWKS** Moodle (via `LtiJwksService`, avec cache 1h)
3. **Vérification de la signature RSA-256** avec la clé publique correspondante
4. **Validation des claims** :
   - `iss` doit correspondre à `lti.moodle.platform-url`
   - `aud` doit contenir `lti.moodle.client-id`
   - `exp` ne doit pas être dépassé
   - `nonce` doit être présent, unique et non rejoué (cache en mémoire avec TTL 1h)

> **Note multi-instances :** le cache des nonces est en mémoire (`ConcurrentHashMap`). Pour un déploiement multi-pods, remplacer par Redis.

### 3.2 LtiAccountMappingService — JIT Provisioning

Logique de résolution de compte en 3 niveaux :

```
1. findByMoodleSub(sub)   → compte déjà lié → génère token XCCM1
2. findByEmail(email)     → compte existant, liaison du moodle_sub
3. createNewUser(...)     → JIT Provisioning avec email synthétique @moodle.lti si email absent
```

Le token JWT interne XCCM1 est généré via le service `JwtService` existant et retourné au frontend Next.js.

### 3.3 LtiKeyService — Gestion des clés RSA

- **En production** : lecture d'une clé privée PKCS#8 PEM depuis le chemin `lti.tool.private-key-pem-path`
- **En développement** : génération d'une paire RSA 2048-bit au démarrage (kid instable → à éviter pour Moodle)

### 3.4 Sécurité inter-domaine (CSP & Cookies)

Configuré dans `SecurityConfig.java` :

```java
// Content-Security-Policy : frame-ancestors configurable via propriété
// lti.security.frame-ancestors=self https://moodle-client.example.com
.contentSecurityPolicy(csp -> csp.policyDirectives(frameAncestorsCspDirective()))
```

La directive `frame-ancestors` est construite dynamiquement depuis la propriété `lti.security.frame-ancestors`, supportant plusieurs domaines séparés par des espaces.

---

## 4. Configuration (application.properties)

```properties
# --- LTI 1.3 ---
lti.moodle.platform-url=${LTI_MOODLE_PLATFORM_URL:http://localhost:8081}
lti.moodle.client-id=${LTI_MOODLE_CLIENT_ID:xccm1-lti-tool}
lti.moodle.jwks-url=${LTI_MOODLE_JWKS_URL:http://localhost:8081/mod/lti/certs.php}
lti.xccm1.issuer=${LTI_XCCM1_ISSUER:http://localhost:8080}
lti.tool.private-key-pem-path=${LTI_TOOL_PRIVATE_KEY_PEM_PATH:}
lti.tool.key-id=${LTI_TOOL_KEY_ID:xccm1-lti-key-1}
lti.security.frame-ancestors=${LTI_SECURITY_FRAME_ANCESTORS:self}
```

### Variables d'environnement pour la production

| Variable | Exemple | Description |
|----------|---------|-------------|
| `LTI_MOODLE_PLATFORM_URL` | `https://moodle.univ.fr` | URL de l'instance Moodle |
| `LTI_MOODLE_CLIENT_ID` | `xccm1-tool-prod` | Client ID configuré dans Moodle |
| `LTI_MOODLE_JWKS_URL` | `https://moodle.univ.fr/mod/lti/certs.php` | JWKS endpoint Moodle |
| `LTI_XCCM1_ISSUER` | `https://xccm1.univ.fr` | URL publique du backend XCCM1 |
| `LTI_TOOL_PRIVATE_KEY_PEM_PATH` | `/secrets/lti-private-key.pem` | Clé privée RSA persistante |
| `LTI_SECURITY_FRAME_ANCESTORS` | `https://moodle.univ.fr` | Domaine Moodle autorisé pour les iframes |

### Procédure de génération d'une clé RSA persistante

```bash
# Génération de la clé privée RSA 2048-bit (PKCS#8)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out lti-private-key.pem

# Extraction de la clé publique (pour enregistrement dans Moodle)
openssl rsa -in lti-private-key.pem -pubout -out lti-public-key.pem

# Vérification
openssl rsa -in lti-private-key.pem -check
```

### Procédure de renouvellement des clés JWK

1. Générer une nouvelle paire RSA (voir ci-dessus)
2. Mettre à jour `LTI_TOOL_PRIVATE_KEY_PEM_PATH` avec le chemin vers la nouvelle clé
3. Mettre à jour `LTI_TOOL_KEY_ID` avec un nouvel identifiant (ex. `xccm1-lti-key-2`)
4. Redémarrer le backend → le nouveau `kid` sera visible sur `/lti/.well-known/jwks.json`
5. Notifier l'équipe Moodle Client pour re-synchroniser leur configuration LTI

> **Fenêtre de transition :** si Moodle a mis en cache l'ancien JWKS, il peut y avoir une période d'erreur `kid not found`. La résolution est d'invalider le cache JWKS côté Moodle ou d'attendre son expiration.

---

## 5. Endpoints exposés

| Méthode | URL | Accès | Description |
|---------|-----|-------|-------------|
| `GET` | `/lti/login` | Public | OIDC Login Initiation — redirige vers Moodle |
| `POST` | `/lti/launch` | Public | Réception du JWT Moodle — retourne token XCCM1 |
| `GET` | `/lti/.well-known/jwks.json` | Public | Clé publique RSA XCCM1 (JWKS) |

---

## 6. Tests unitaires

### 6.1 Résultats

```
Tests run: 36, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

### 6.2 Couverture par classe

| Classe de test | Tests | Cas couverts |
|----------------|-------|--------------|
| `LtiJwtValidatorServiceTest` | 6 | Token valide, issuer invalide, audience invalide, token expiré, nonce rejoué, kid absent |
| `LtiAccountMappingServiceTest` | 5 | Compte existant (sub), liaison email, JIT teacher, JIT student, email absent |
| `LtiLaunchControllerTest` | 7 | 200 réussi, email absent, token manquant, token vide, state invalide, JWT invalide, state null |
| `LtiLoginControllerTest` | 6 | Redirection nominale, avec message hint, URL trailing slash, issuer inconnu, target_link_uri invalide, issuer null |
| `LtiWellKnownControllerTest` | 2 | JWKS valide retourné, clé privée non exposée |
| `LtiOidcStateServiceTest` | 3 | registerState + consume réussi, double consume échoue, state vide échoue |
| `LtiKeyServiceTest` | 2 | Génération sans PEM, JWKS public sans clé privée |
| `LtiJwksServiceTest` | 2 | Parsing JWKS statique, rafraîchissement cache |
| `LtiRoleMapperTest` | 3 | Learner → STUDENT, Instructor → TEACHER, rôle absent → TEACHER |

---

## 7. Corrections annexes — Résolution des conflits de fusion

Lors de l'intégration de la branche (`48e8a3c`), 4 fichiers présentaient des conflits :

| Fichier | Résolution |
|---------|------------|
| `CourseService.java` | Conservé `ObjectProvider` (dégradation gracieuse ES) + ajout `LLMIndexingService` |
| `UserServiceImpl.java` | Conservé `ObjectProvider` + ajout import `@Slf4j` + fallback `searchUsers(query, pageable)` |
| `CourseClassService.java` | Conservé `ObjectProvider` + fallback `searchOpenClasses(query)` |
| `ExerciseService.java` | Conservé `ObjectProvider` + ajout `LLMIndexingService` pour indexation et évaluation RAG |

**Stratégie retenue :** `ObjectProvider<T>` pour tous les repositories Elasticsearch permet une dégradation gracieuse si le service n'est pas disponible, sans `try-catch` verbeux.

---

## 8. Risques identifiés et mitigations

| Risque | Statut | Mitigation |
|--------|--------|------------|
| Cache nonces en mémoire (attaque replay multi-pods) | ⚠️ À surveiller | Migrer vers Redis en production multi-instances |
| `kid` instable si pas de PEM configuré | ⚠️ Env dev uniquement | Obligation de `LTI_TOOL_PRIVATE_KEY_PEM_PATH` en prod |
| Cookies tiers bloqués (Safari/iOS) | ✅ Documenté | JWT transmis via URL + `sessionStorage` côté frontend |
| JWKS Moodle indisponible au démarrage | ✅ Géré | Cache avec repli sur l'ancien JWKS en cas d'erreur de rafraîchissement |
| Rotation de clés sans fenêtre de transition | ✅ Documenté | Procédure de renouvellement documentée en section 4 |

---

## 9. Checklist des livrables

- [x] **J1** — Audit backend Spring Boot (authentification existante, dépendances)
- [x] **J2** — Dépendance `nimbus-jose-jwt 9.37.3` intégrée dans `pom.xml`
- [x] **J3–J4** — `LtiLaunchController` + `LtiLoginController` + récupération JWKS Moodle
- [x] **J5** — Tests unitaires JWKS (`LtiJwksServiceTest`)
- [x] **J6–J7** — Validation JWT complète (signature, `iss`, `aud`, `exp`, nonce)
- [x] **J8–J9** — `LtiAccountMappingService` (JIT Provisioning, 3 niveaux de résolution)
- [x] **J10** — JWKS endpoint public `/lti/.well-known/jwks.json`
- [x] **J11** — En-têtes CSP (`frame-ancestors`) et cookies `SameSite=None; Secure`
- [x] **J12–J13** — Suite de tests d'intégration (36 tests, BUILD SUCCESS)
- [x] **J14** — Documentation backend (ce rapport)
- [x] **Bonus** — Résolution de 4 conflits de fusion sur les services Elasticsearch/LLM
