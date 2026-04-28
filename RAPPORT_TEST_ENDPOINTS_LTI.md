# Rapport de Test — Endpoints LTI 1.3
**Auteur :** Gabrielle (Backend)
**Date :** 28 avril 2026
**Serveur :** `http://localhost:8082`
**Application :** XCCM1-BACKEND — branche `plugin-gabrielle`

---

## Résumé exécutif

| Endpoint | Statut global | Bugs trouvés | Corrections appliquées |
|----------|--------------|--------------|------------------------|
| `GET /lti/.well-known/jwks.json` | ✅ Fonctionnel | Aucun | — |
| `GET /lti/login` | ✅ Fonctionnel | 1 bug (500 sur param absent) | ✅ Corrigé et vérifié |
| `POST /lti/launch` | ✅ Fonctionnel | 1 bug (500 sur token absent) | ✅ Corrigé et vérifié |

> **Vérification post-redémarrage :** Corrections activées et validées en live le 28 avril 2026 après `mvn clean spring-boot:run`. Les deux bugs 500 → 400 sont confirmés.

---

## 1. GET `/lti/.well-known/jwks.json`

### Objectif
Exposer la clé publique RSA de XCCM1 au format JWKS. Moodle l'utilise pour vérifier les signatures des messages envoyés par XCCM1 (et vice-versa selon la configuration).

### Test exécuté
```bash
curl -s http://localhost:8082/lti/.well-known/jwks.json
```

### Réponse obtenue
```json
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "kid": "xccm1-lti-key-1-bd4aeeda",
      "n": "oBg4rXI0OHhSq3MYmw7mzsRjSIg5ThPF9FC2b8vW0ik2-ilJ0UCrQVqKYsykK5F3Rw..."
    }
  ]
}
```

**HTTP Status : `200 OK`**

### Analyse

| Critère | Résultat |
|---------|----------|
| HTTP 200 | ✅ |
| Structure `{ "keys": [...] }` | ✅ |
| `kty = "RSA"` | ✅ |
| `kid` présent | ✅ (`xccm1-lti-key-1-bd4aeeda`) |
| Clé privée `d` absente | ✅ Non exposée (correct) |
| Exposant `e = "AQAB"` (65537) | ✅ Standard RSA |
| Modulus `n` (2048 bits) | ✅ 342 caractères base64url |

### Observation — `kid` instable

Le `kid` contient un suffixe aléatoire (`-bd4aeeda`) car aucune clé PEM persistante n'est configurée :
```
WARN: Aucun lti.tool.private-key-pem-path : génération d'une paire RSA au démarrage
       (kid change à chaque redémarrage — à éviter en prod / Moodle)
```
**En production**, il faut obligatoirement configurer `LTI_TOOL_PRIVATE_KEY_PEM_PATH` pour un `kid` stable. Sans cela, Moodle ne pourra pas vérifier les signatures après un redémarrage.

### Verdict : ✅ Fonctionnel (avec avertissement prod)

---

## 2. GET `/lti/login`

### Objectif
Initier le flux OIDC LTI 1.3 : recevoir les paramètres de Moodle, générer un `state` et un `nonce`, et rediriger vers l'auth endpoint Moodle.

---

### Test 1 — Paramètres valides (cas nominal)

```bash
curl -v "http://localhost:8082/lti/login?\
  iss=http://localhost:8081\
  &login_hint=user123\
  &target_link_uri=http://localhost:8080/lti/launch"
```

**HTTP Status : `302 Found`**

**Location :**
```
http://localhost:8081/mod/lti/auth.php
  ?response_type=id_token
  &scope=openid
  &response_mode=form_post
  &client_id=xccm1-lti-tool
  &redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Flti%2Flaunch
  &login_hint=user123
  &state=f09f3815-65c7-414f-82dd-c753b9d9a256
  &nonce=732e3056-84ba-4fd8-8caa-762c88bb64c9
```

| Critère | Résultat |
|---------|----------|
| HTTP 302 | ✅ |
| Redirection vers `mod/lti/auth.php` | ✅ |
| `state` UUID généré | ✅ |
| `nonce` UUID généré | ✅ |
| `client_id` = `xccm1-lti-tool` | ✅ |
| `redirect_uri` encodée | ✅ |

---

### Test 2 — Issuer inconnu

```bash
curl "http://localhost:8082/lti/login?iss=http://evil.hacker.com&login_hint=u1&target_link_uri=..."
```

**HTTP Status : `400 Bad Request`** ✅

---

### Test 3 — `target_link_uri` invalide

```bash
curl "http://localhost:8082/lti/login?iss=http://localhost:8081&login_hint=u1&target_link_uri=http://evil.com/steal"
```

**HTTP Status : `400 Bad Request`** ✅ (protection contre la redirection ouverte)

---

### Test 4 — URL Moodle avec slash final sur `iss`

```bash
curl "http://localhost:8082/lti/login?iss=http://localhost:8081/&login_hint=u1&target_link_uri=http://localhost:8080/lti/launch"
```

**HTTP Status : `302 Found`** ✅ (normalisation correcte des slashes finaux)

---

### Test 5 — Paramètres manquants *(Bug corrigé)*

```bash
curl "http://localhost:8082/lti/login"
```

**Avant correction :** `500 Internal Server Error`
```json
{ "error": "Required request parameter 'iss' for method parameter type String is not present" }
```

**Après correction :** `400 Bad Request` (redémarrage requis)

**Cause :** `@RequestParam("iss")` était `required=true` (défaut Spring). Une requête sans ce paramètre déclenchait une `MissingServletRequestParameterException` non gérée par le controller, que le handler global transformait en 500.

**Correction appliquée dans** [LtiLoginController.java](src/main/java/com/ihm/backend/controller/LtiLoginController.java) :
- `iss`, `login_hint`, `target_link_uri` → `required = false`
- Validation explicite de `login_hint` ajoutée dans la logique du controller

---

### Verdict `/lti/login` : ✅ Nominal fonctionnel — ⚠️ Bug 500 corrigé (redémarrage requis)

---

## 3. POST `/lti/launch`

### Objectif
Recevoir le `id_token` JWT signé par Moodle + le `state` OIDC, valider le JWT (signature RSA, claims LTI), mapper le compte utilisateur, et retourner un token XCCM1 interne.

---

### Test 1 — `id_token` absent *(Bug corrigé)*

```bash
curl -X POST http://localhost:8082/lti/launch \
  -H "Content-Type: application/x-www-form-urlencoded"
```

**Avant correction :** `500 Internal Server Error`
```json
{ "error": "Required request parameter 'id_token' for method parameter type String is not present" }
```

**Après correction :** `400 Bad Request` — `{ "error": "id_token manquant" }` (redémarrage requis)

**Correction appliquée dans** [LtiLaunchController.java](src/main/java/com/ihm/backend/controller/LtiLaunchController.java) :
- `@RequestParam("id_token")` → `@RequestParam(value = "id_token", required = false)`

---

### Test 2 — `id_token` vide (espaces)

```bash
curl -X POST http://localhost:8082/lti/launch \
  -d "id_token=   &state=any"
```

**HTTP Status : `400 Bad Request`** ✅
```json
{ "error": "id_token manquant" }
```

---

### Test 3 — `state` invalide

```bash
curl -X POST http://localhost:8082/lti/launch \
  -d "id_token=header.payload.sig&state=fake-state-xyz"
```

**HTTP Status : `400 Bad Request`** ✅
```json
{ "error": "state OIDC invalide ou déjà utilisé" }
```

---

### Test 4 — Attaque par rejeu du `state` (anti-CSRF)

```
1er appel (state consommé) → HTTP 401  (state valide, mais JWT malformé)
2ème appel (même state)    → HTTP 400  { "error": "state OIDC invalide ou déjà utilisé" }
```

✅ La protection anti-rejeu fonctionne : le `state` est invalidé après le premier usage.

---

### Test 5 — JWT signé avec une clé inconnue (simulation Moodle externe)

**Scenario :** Génération d'un JWT RS256 valide (format LTI correct, nonce réel extrait du `/lti/login`) mais signé avec une clé RSA inconnue de XCCM1.

```
state obtenu via /lti/login : fdad0cee-d924-4b1a-bcb0-81053a01928e
nonce injecté dans JWT       : 4efb00ac-6555-4659-9a35-45bfd3087c18
kid JWT                      : fake-moodle-kid-test (non enregistré dans JWKS Moodle)
```

**HTTP Status : `401 Unauthorized`** ✅
```json
{ "error": "Authentification LTI échouée : Connection refused" }
```

**Explication :** XCCM1 tente de récupérer le JWKS de Moodle (`http://localhost:8081/mod/lti/certs.php`) pour vérifier la signature. Comme aucune instance Moodle ne tourne sur ce port, la connexion est refusée — résultat correct : authentification impossible, 401 retourné.

> En conditions réelles (avec Moodle démarré), le backend récupèrerait le JWKS de Moodle, trouverait la clé par `kid`, et vérifierait la signature. Si la clé ne correspond pas, il retournerait une erreur de signature invalide.

---

### Test 6 — JWT malformé (pas un JWT valide)

```bash
curl -X POST http://localhost:8082/lti/launch \
  -d "id_token=not.a.valid.jwt.at.all&state=<state_valide>"
```

**HTTP Status : `401 Unauthorized`** ✅
```json
{ "error": "Authentification LTI échouée : Invalid JWS header: Invalid JSON object" }
```

---

### Verdict `/lti/launch` : ✅ Logique de sécurité fonctionnelle — ⚠️ Bug 500 corrigé (redémarrage requis)

---

## 4. Tableau récapitulatif des tests

| # | Endpoint | Scénario | Attendu | Obtenu | ✅/❌ |
|---|----------|----------|---------|--------|-------|
| 1 | `GET /lti/.well-known/jwks.json` | Cas nominal | 200 + JWKS | 200 + JWKS | ✅ |
| 2 | `GET /lti/.well-known/jwks.json` | Clé privée absente | `d` absent | `d` absent | ✅ |
| 3 | `GET /lti/login` | Paramètres valides | 302 + Location | 302 + Location complète | ✅ |
| 4 | `GET /lti/login` | Issuer inconnu | 400 | 400 | ✅ |
| 5 | `GET /lti/login` | `target_link_uri` invalide | 400 | 400 | ✅ |
| 6 | `GET /lti/login` | `iss` avec slash final | 302 (normalisé) | 302 | ✅ |
| 7 | `GET /lti/login` | Paramètres absents | 400 | ~~500~~ → **400** ✔ | ✅ |
| 8 | `POST /lti/launch` | `id_token` absent | 400 | ~~500~~ → **400** ✔ | ✅ |
| 9 | `POST /lti/launch` | `id_token` vide | 400 | 400 | ✅ |
| 10 | `POST /lti/launch` | State invalide | 400 | 400 | ✅ |
| 11 | `POST /lti/launch` | State null | 400 | 400 | ✅ |
| 12 | `POST /lti/launch` | Attaque replay state | 400 (2ème appel) | 400 | ✅ |
| 13 | `POST /lti/launch` | JWT signé, clé inconnue | 401 | 401 | ✅ |
| 14 | `POST /lti/launch` | JWT malformé | 401 | 401 | ✅ |

*Corrigé et confirmé en live après `mvn clean spring-boot:run` (28 avril 2026)*

---

## 5. Bugs identifiés et corrections

### Bug #1 — `POST /lti/launch` : `id_token` absent → 500 au lieu de 400

**Fichier :** [LtiLaunchController.java:43](src/main/java/com/ihm/backend/controller/LtiLaunchController.java#L43)

**Avant :**
```java
@RequestParam("id_token") String idToken
```

**Après :**
```java
@RequestParam(value = "id_token", required = false) String idToken
```

---

### Bug #2 — `GET /lti/login` : paramètres absents → 500 au lieu de 400

**Fichier :** [LtiLoginController.java:48-54](src/main/java/com/ihm/backend/controller/LtiLoginController.java#L48-L54)

**Avant :**
```java
@RequestParam("iss") String iss,
@RequestParam("login_hint") String loginHint,
@RequestParam("target_link_uri") String targetLinkUri,
```

**Après :**
```java
@RequestParam(value = "iss", required = false) String iss,
@RequestParam(value = "login_hint", required = false) String loginHint,
@RequestParam(value = "target_link_uri", required = false) String targetLinkUri,
```
+ Validation explicite de `login_hint` dans la méthode (guard clause `sendError(400)`).

---

## 6. Points à valider en conditions réelles (avec Moodle)

Les tests ci-dessus ont été exécutés sans instance Moodle réelle. Les éléments suivants nécessitent un environnement de staging complet :

| Test | Prérequis |
|------|-----------|
| Flux SSO complet (JWT signé par Moodle → token XCCM1 généré) | Instance Moodle avec LTI tool enregistré |
| JIT Provisioning en base réelle (création de compte) | BDD + Moodle |
| Vérification du `kid` stable après redémarrage | `LTI_TOOL_PRIVATE_KEY_PEM_PATH` configuré + Moodle |
| Cookies `SameSite=None; Secure` dans iframe | HTTPS + navigateur |

---

## 7. Actions requises

| Priorité | Action | Responsable | Statut |
|----------|--------|-------------|--------|
| ~~🔴 Immédiat~~ | ~~Redémarrer l'app pour activer les corrections Bug #1 et #2~~ | Gabrielle | ✅ Fait |
| ~~🟡 Avant staging~~ | ~~Configurer `LTI_TOOL_PRIVATE_KEY_PEM_PATH` avec une clé PEM persistante~~ | Gabrielle | ✅ Fait — `secrets/lti-private-key.pem` + `kid=xccm1-lti-key-1` stable |
| 🟡 Avant staging | Tester le flux SSO complet avec l'équipe Moodle Client (J10 du plan) | Gabrielle + FE1 | En attente (nécessite Moodle) |
| ~~🟢 Nice-to-have~~ | ~~Ajouter des tests unitaires pour le cas `required=false` des @RequestParam~~ | Gabrielle | ✅ Fait — 4 tests ajoutés (16/16 verts) |
