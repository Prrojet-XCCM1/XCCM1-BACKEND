# LTI 1.3 — Rapport de tests (sans Moodle) et référence des endpoints

**Date du rapport :** 2026-04-10  
**Contexte :** validation du backend en attendant une instance Moodle pleinement configurée. Les tests **HTTP** ci‑dessous supposent le backend démarré (ex. `./mvnw spring-boot:run` sur le port configuré, par défaut **8080**).

**Documents liés :** [guide-lti-test-et-integration.md](./guide-lti-test-et-integration.md) (flux, curl, intégration), [backend-lti-docs.md](./backend-lti-docs.md) (implémentation).

---

## 1. Synthèse des tests exécutés

### 1.1 Tests unitaires / composants (Maven)

Commande :

```bash
./mvnw test -Dtest=LtiJwksServiceTest,LtiOidcStateServiceTest,LtiRoleMapperTest
```

| Suite | Fichier source test | Tests | Échecs | Erreurs |
|-------|---------------------|------:|-------:|--------:|
| JWKS Moodle (parse / cache) | `LtiJwksServiceTest.java` | 2 | 0 | 0 |
| State OIDC (anti-CSRF) | `LtiOidcStateServiceTest.java` | 3 | 0 | 0 |
| Rôles LTI → `UserRole` | `LtiRoleMapperTest.java` | 3 | 0 | 0 |
| **Total** | | **8** | **0** | **0** |

**Résultat :** `BUILD SUCCESS` (exit code 0).

**Non couvert par ces tests :** contrôleurs HTTP (`/lti/login`, `/lti/launch`), validation JWT bout en bout avec un `id_token` réel signé par Moodle (`LtiJwtValidatorService`), mapping compte + émission du JWT applicatif (`LtiAccountMappingService`). Ces parties se valident avec les **smoke tests HTTP** ci‑dessous et un **flux Moodle réel** une fois la plateforme prête.

### 1.2 Smoke tests HTTP (backend vivant, pas de Moodle requis pour les 302/400)

Environnement d’exécution du rapport : `BASE=http://localhost:8080`, `LTI_MOODLE_PLATFORM_URL=http://localhost:8081` (valeurs par défaut du projet ; Moodle n’a pas besoin de répondre pour ces appels).

| # | Scénario | Commande / observation | HTTP | Résultat attendu / observé |
|---|----------|-------------------------|------|----------------------------|
| 1 | JWKS outil | `GET ${BASE}/lti/.well-known/jwks.json` | **200** | JSON `keys` (JWK RSA), `kid` présent |
| 2 | Login OIDC paramètres valides | `GET /lti/login?iss=…&login_hint=…&target_link_uri=${BASE}/lti/launch` avec `iss` = `lti.moodle.platform-url` | **302** | `Location: …/mod/lti/auth.php?…&state=…&nonce=…&client_id=…&redirect_uri=…` |
| 3 | Login issuer inconnu | `iss` ≠ plateforme configurée | **400** | Corps erreur Spring / Bad Request |
| 4 | `target_link_uri` invalide | `target_link_uri` ≠ `{lti.xccm1.issuer}/lti/launch` | **400** | Idem |
| 5 | Launch sans `id_token` | `POST /lti/launch` corps vide | **400** | `{"error":"id_token manquant"}` |
| 6 | Launch `id_token` invalide + `state` valide issu du login | `POST` avec `id_token=fake…` et `state` extrait de la `Location` du login | **401** | Message d’erreur JWT / validation (comportement normal sans JWT Moodle) |

**Remarque :** un **200** sur `/lti/launch` avec `token` applicatif exige un **`id_token`** signé par Moodle, un **`state`** correspondant au flux, un JWKS Moodle joignable (`lti.moodle.jwks-url`), et des claims conformes (`iss`, `aud`, `exp`, `nonce`, etc. — voir `LtiJwtValidatorService`).

---

## 2. Inventaire des endpoints LTI (code source)

Toutes les routes sont sous **`/lti/**`**. Elles sont **publiques** (pas de JWT XCCM1 requis) : `SecurityConfig` autorise `permitAll()` sur `/lti/**`.

Classes concernées :

| Classe | Fichier |
|--------|---------|
| `LtiWellKnownController` | `src/main/java/com/ihm/backend/lti/LtiWellKnownController.java` |
| `LtiLoginController` | `src/main/java/com/ihm/backend/lti/LtiLoginController.java` |
| `LtiLaunchController` | `src/main/java/com/ihm/backend/lti/LtiLaunchController.java` |

---

### 2.1 `GET /lti/.well-known/jwks.json`

| Champ | Détail |
|--------|--------|
| **Méthode / chemin** | `GET /lti/.well-known/jwks.json` |
| **Query / body** | Aucun |
| **Content-Type réponse** | `application/json` |
| **Réponse succès (200)** | Objet JWKS : `keys[]` avec au minimum `kty`, `kid`, `n`, `e` (RSA) |
| **Rôle** | Expose la clé **publique** de l’outil XCCM1 pour que Moodle (ou un proxy) vérifie les messages signés par l’outil si nécessaire |
| **Moodle — champ à renseigner** | **Public keyset URL** (ou équivalent) : `{LTI_XCCM1_ISSUER}/lti/.well-known/jwks.json` |

---

### 2.2 `GET /lti/login` — initiation OIDC (LTI 1.3)

| Champ | Détail |
|--------|--------|
| **Méthode / chemin** | `GET /lti/login` |
| **Content-Type** | Requête sans corps (query string) |

| Paramètre | Requis | Description | Exemple / contrainte |
|-----------|--------|-------------|----------------------|
| `iss` | Oui | Issuer = URL de base de la **plateforme** (Moodle) | Doit égaler `lti.moodle.platform-url` (normalisation sans slash final) |
| `login_hint` | Oui | Indice utilisateur côté LMS | Ex. identifiant interne Moodle (souvent un entier en string) |
| `target_link_uri` | Oui | URI de lancement après auth | **Doit** être exactement `{lti.xccm1.issuer}/lti/launch` ( partie query du `target_link_uri` ignorée pour la comparaison ) |
| `lti_message_hint` | Non | Hint message LTI | Relayé tel quel vers `auth.php` si présent |

| Réponse | Code | Contenu |
|---------|------|---------|
| Redirection vers Moodle | **302** | `Location: {lti.moodle.platform-url}/mod/lti/auth.php?response_type=id_token&scope=openid&response_mode=form_post&client_id={lti.moodle.client-id}&redirect_uri={encode(lti.xccm1.issuer + "/lti/launch")}&login_hint=…&state=…&nonce=…` (+ `lti_message_hint` si fourni) |
| Issuer inconnu | **400** | Message : « Issuer non reconnu » (corps d’erreur peut être filtré par Spring) |
| `target_link_uri` invalide | **400** | « target_link_uri invalide » |

| **Moodle — champs à renseigner** | Valeur type |
|-----------------------------------|-------------|
| Initiate login / OIDC login URL | `{LTI_XCCM1_ISSUER}/lti/login` |
| (interne Moodle) lors du lancement, Moodle appelle cette URL avec `iss`, `login_hint`, `target_link_uri` conformes au tool registration |

---

### 2.3 `POST /lti/launch` — réception `id_token`

| Champ | Détail |
|--------|--------|
| **Méthode / chemin** | `POST /lti/launch` |
| **Content-Type attendu** | `application/x-www-form-urlencoded` (comme `response_mode=form_post`) |

| Paramètre (formulaire) | Requis | Description |
|------------------------|--------|-------------|
| `id_token` | Oui | JWT signé (RSA) par Moodle |
| `state` | Oui pour le flux réel | Doit correspondre à un `state` enregistré par `LtiOidcStateService` lors du `GET /lti/login`, **usage unique** |

| Réponse | Code | Corps (JSON) |
|---------|------|----------------|
| Succès SSO | **200** | `status`, `token` (JWT XCCM1), `user` : `email`, `name` |
| `id_token` absent | **400** | `error`: `id_token manquant` |
| `state` invalide / manquant / déjà consommé | **400** | `error`: message explicatif |
| JWT ou claims invalides | **401** | `error`: préfixe `Authentification LTI échouée : …` |

Validations principales côté `id_token` (voir `LtiJwtValidatorService`) : signature via JWKS (`lti.moodle.jwks-url`), `iss` = `lti.moodle.platform-url`, `aud` contient `lti.moodle.client-id`, `exp`, claim `nonce`, etc.

| **Moodle — champs à renseigner** | Valeur type |
|-----------------------------------|-------------|
| Target link URL / Launch URL | `{LTI_XCCM1_ISSUER}/lti/launch` |
| Redirect URI(s) | `{LTI_XCCM1_ISSUER}/lti/launch` (doit coïncider avec `target_link_uri` attendu par le backend) |
| Client ID | Identique à `LTI_MOODLE_CLIENT_ID` / `lti.moodle.client-id` |

---

## 3. Fiche « à compléter » — valeurs Moodle (quand la plateforme sera prête)

Remplir et garder aligné avec `application.properties` / variables d’environnement :

| Élément | Valeur à reporter |
|---------|-------------------|
| URL publique Moodle (base) | `________________________________` → `LTI_MOODLE_PLATFORM_URL` |
| JWKS Moodle | `________________________________` → `LTI_MOODLE_JWKS_URL` (typ. `…/mod/lti/certs.php`) |
| Client ID outil (Moodle ↔ backend) | `________________________________` → `LTI_MOODLE_CLIENT_ID` |
| URL publique backend XCCM1 (issuer) | `________________________________` → `LTI_XCCM1_ISSUER` |
| Tool URL / launch | `{issuer}/lti/launch` |
| Login initiation URL | `{issuer}/lti/login` |
| JWKS outil (lecture seule) | `{issuer}/lti/.well-known/jwks.json` |
| CSP iframe (`lti.security.frame-ancestors`) | `________________________________` |

---

## 4. Prochaines étapes de test (avec Moodle)

1. Vérifier que `curl` ou le navigateur peut joindre `lti.moodle.jwks-url` **depuis le serveur** qui héberge XCCM1.
2. Créer / déployer l’outil LTI 1.3 dans Moodle avec les URLs de la section 3.
3. Lancer l’activité depuis un utilisateur test : enchaînement **login** → **auth Moodle** → **POST launch** ; attendre **200** et un `token` dans le JSON.
4. (Optionnel) Rejouer un `POST /lti/launch` capturé depuis les outils développeur du navigateur pour déboguer.

---

*Rapport généré à partir du code dans `com.ihm.backend.lti` et d’exécutions locales documentées le 2026-04-10.*
