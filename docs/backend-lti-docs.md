# Documentation Technique : Intégration LTI 1.3 (XCCM1-BACKEND)

Ce document décrit l'implémentation de l'authentification LTI 1.3 pour permettre le SSO (Single Sign-On) depuis Moodle.

Pour un guide plus large (principes LTI, checklist Moodle, commandes `curl`, test E2E), voir [guide-lti-test-et-integration.md](./guide-lti-test-et-integration.md). Pour le **rapport de tests** et la **référence paramétrée des endpoints** `/lti/**`, voir [lti-tests-et-endpoints.md](./lti-tests-et-endpoints.md).

## 1. Flux d'Authentification (handshake OIDC)

Le flux suit le standard LTI 1.3 / OpenID Connect :

1.  **Initiation (GET `/lti/login`)** :
    - Reçoit `iss` (Moodle URL) et `login_hint`.
    - Redirige vers Moodle avec `client_id`, `redirect_uri` et un `nonce` (anti-rejou).
2.  **Lancement (POST `/lti/launch`)** :
    - Reçoit un `id_token` (JWT signé par Moodle).
    - **Validation** : Vérifie la signature RSA256 en utilisant le JWKS public de Moodle.
    - **Extraction** : Récupère l'identifiant unique de l'utilisateur (`sub`), son email et son nom.
3.  **SSO / Mapping** :
    - Si l'utilisateur possède déjà un `moodle_sub` correspondant, il est connecté.
    - Sinon, un compte est créé (JIT Provisioning) ou lié par email.
    - Un token JWT XCCM1 est généré et renvoyé.

## 2. Configuration (`application.properties`)

```properties
# URL Moodle et identifiants
lti.moodle.platform-url=https://moodle.client.tld
lti.moodle.jwks-url=https://moodle.client.tld/mod/lti/certs.php
lti.moodle.client-id=XCCM1_LTI_CLIENT

# Sécurité Iframe
server.servlet.session.cookie.same-site=none
server.servlet.session.cookie.secure=true
```

## 3. Endpoints Publics

- `/lti/login` : Initiation OIDC.
- `/lti/launch` : Réception du token.
- `/lti/.well-known/jwks.json` : Exposition des clés publiques de XCCM1.

## 4. Sécurité Inter-domaine

Pour permettre l'affichage dans l'iframe Moodle, la Politique de Sécurité du Contenu (CSP) a été mise à jour :
`Content-Security-Policy: frame-ancestors 'self' https://moodle.client.tld`

## 5. Maintenance des Clés

Le service `LtiKeyService` génère une paire de clés RSA au démarrage. En production, ces clés devraient être persistées ou chargées depuis un gestionnaire de secrets.
