# Guide de Validation LTI 1.3 (Jours 1 à 5)
**Projet :** XCCM1 × Moodle Client  
**Auteur :** Personne 3 (BE)

Ce document explique comment vérifier le travail accompli durant la première phase du sprint (Fondations).

---

## 1. Tests Automatisés (Jour 5)

Le test JUnit `LtiJwksServiceTest` valide l'intégration de la librairie **nimbus-jose-jwt** et la capacité du backend à parser les clés publiques (JWKS) de Moodle.

**Commande à lancer :**
```bash
./mvnw test -Dtest=LtiJwksServiceTest
```
**Résultat attendu :** `BUILD SUCCESS` (les 2 tests doivent être au vert).

---

## 2. Tests Manuels des Endpoints (Jours 3-4)

Une fois le serveur Spring Boot démarré (`./mvnw spring-boot:run`), vous pouvez tester les nouveaux points d'entrée LTI.

### 2.1 Initiation du Login OIDC
**Endpoint :** `GET /lti/login`  
**Test :** Simuler un appel de Moodle.
```bash
curl -v "http://localhost:8082/lti/login?iss=https://moodle.client.tld&login_hint=123"
```
**Résultat attendu :** Une redirection (`302 Found`) vers l'URL Moodle avec tous les paramètres OIDC nécessaires (`client_id`, `redirect_uri`, `nonce`, etc.).

### 2.2 Lancement LTI (Launch)
**Endpoint :** `POST /lti/launch`  
**Test :** Simuler un envoi de token (sans validation pour l'instant).
```bash
curl -X POST "http://localhost:8082/lti/launch" -d "id_token=fake-token"
```
**Résultat attendu :** `200 OK` avec un message indiquant que le lancement a été reçu et que le JWKS Moodle est accessible.

---

## 3. Vérification de la Sécurité (Jour 11 anticipé)

Nous avons déjà configuré les en-têtes CSP pour autoriser l'affichage dans l'iframe Moodle.

**Commande :**
```bash
curl -I http://localhost:8082/lti/launch
```
**Résultat attendu :** Vérifier la présence de l'en-tête suivant :
`Content-Security-Policy: frame-ancestors 'self' http://localhost:3000 https://moodle.client.tld`

---

## 4. Configuration

Vérifiez que le fichier `src/main/resources/application.properties` contient bien la section **CONFIGURATION LTI 1.3** à la fin.

---

**Statut du Sprint :**
- [x] J1 : Audit Backend
- [x] J2 : Dépendances LTI
- [x] J3-J4 : Endpoints & Security
- [x] J5 : Test JUnit JWKS
