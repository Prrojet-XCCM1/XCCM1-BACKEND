# Transformation de XCCM1 en Plugin PHP

Ce plan détaille comment transformer l'application XCCM1 (Backend Spring Boot + Frontend Next.js) en un "module" ou "plugin" intégrable dans un projet PHP existant.

## Architecture Proposée : Mode "Service-First"

L'approche la plus robuste consiste à traiter XCCM1 comme un micro-service :
1. **Backend (Java)** : Fournit l'API REST.
2. **Frontend (Next.js)** : Fournit l'interface utilisateur (UI).
3. **Application PHP** : Agit comme hôte, gère l'authentification initiale et embarque l'UI de XCCM1.

## Changements Proposés

### [Component] Backend Java (API)
Le backend doit être configuré pour accepter les requêtes provenant du domaine PHP et valider l'authentification déléguée.

#### [MODIFY] [CORS Configuration](file:///home/delmat/4GI%20NEW/SEMESTRE1/IHM/XCCM1/XCCM1-BACKEND/src/main/java/com/ihm/backend/config/SecurityConfig.java)
- Configurer `CorsConfiguration` pour autoriser le domaine de l'application PHP.
- Autoriser les headers standard (Authorization, Content-Type).

#### [MODIFY] [Authentication Strategy](file:///home/delmat/4GI%20NEW/SEMESTRE1/IHM/XCCM1/XCCM1-BACKEND/src/main/java/com/ihm/backend/security/JwtAuthenticationFilter.java)
- Permettre l'authentification via un JWT généré par le PHP (en utilisant une clé secrète partagée).
- Optionnellement, ajouter un endpoint de "auto-login" qui accepte un token à usage unique venant du PHP.

### [Component] Frontend Next.js
Le frontend doit pouvoir être "incrusté" sans conflit.

- **Embedding** : Utilisation d'une `<iframe>` dans le projet PHP.
- **Communication** : Utilisation de `window.postMessage` pour synchroniser les états entre le PHP et le plugin si nécessaire.

### [Component] Intégration PHP (Côté Client)
- Création d'un script PHP qui génère le token d'accès.
- Inclusion de l'iframe pointant vers l'URL Next.js avec le token en paramètre ou via header.

## Plan de Vérification

### Tests Automatisés
- **Backend** : Exécuter `mvn test` pour s'assurer que les changements de sécurité ne cassent pas l'authentification existante.
- **CORS** : Utiliser `curl` pour vérifier les headers `Access-Control-Allow-Origin`.

### Vérification Manuelle
1. **Démarrage** : Lancer le backend avec `mvn spring-boot:run` et vérifier l'absence d'erreur [UserMapper](file:///home/delmat/4GI%20NEW/SEMESTRE1/IHM/XCCM1/XCCM1-BACKEND/src/main/java/com/ihm/backend/mappers/UserMapper.java#16-64).
2. **Accès API** : Tenter d'appeler un endpoint protégé depuis un script PHP simple utilisant `cURL`.
3. **Visualisation** : Créer une page `.php` test avec une `<iframe>` pointant vers le frontend Next.js et vérifier que l'affichage est correct.

> [!IMPORTANT]
> La priorité immédiate est de corriger l'erreur de démarrage `UnsatisfiedDependencyException` liée à [UserMapper](file:///home/delmat/4GI%20NEW/SEMESTRE1/IHM/XCCM1/XCCM1-BACKEND/src/main/java/com/ihm/backend/mappers/UserMapper.java#16-64) pour permettre les tests.
