# Fonctionnalités d'Interactions des Cours & Inscription des Enseignants

Ce document détaille l'implémentation des fonctionnalités d'interactions sociales sur les cours (likes, vues, commentaires) ainsi que la possibilité pour les enseignants de s'enrôler aux cours.

---

## 1. Inscription des Enseignants aux Cours

Les enseignants peuvent désormais s'inscrire (s'enrôler) aux cours publiés par d'autres enseignants de la même manière que les étudiants.

### Règles Métier
- **Accès Multi-Rôle :** L'endpoint `POST /api/enrollments/courses/{courseId}` accepte désormais les rôles `STUDENT` et `TEACHER`.
- **Validation Automatique :** Contrairement aux étudiants dont l'inscription reste en attente (`PENDING`), l'inscription d'un enseignant est **automatiquement approuvée** (`APPROVED`).
- **Restriction :** Un enseignant ne peut pas s'inscrire à son propre cours.
- **Notifications :** L'auteur du cours ne reçoit une notification que lorsqu'un **étudiant** s'inscrit (pour éviter de spammer le créateur du cours si d'autres enseignants le consultent).

---

## 2. Interactions sur les Cours (Likes, Vues, Commentaires)

Nous avons mis en place un système complet de tracking par utilisateur pour les interactions, afin d'éviter les doublons (qu'un même utilisateur like deux fois ou gonfle artificiellement le compteur de vues).

### A. Architecture (Couche de Données)
- **Entities Créées :**
  - `CourseLike` : Associe un `User` et un `Course` avec une contrainte d'unicité (un like max par personne).
  - `CourseView` : Associe un `User` et un `Course` (une vue comptabilisée max par personne).
  - `CourseComment` : Contient le texte du commentaire, l'auteur (`User`), le cours associé (`Course`), et des timestamps (`createdAt`, `updatedAt`).
- **Caches Dénormalisés :**
  - L'entité principale `Course` conserve ses champs `likeCount` et `viewCount` pour des performances de lecture optimales. Ces compteurs sont mis à jour dynamiquement par le service lors d'une nouvelle interaction.

### B. Fonctionnement des Likes (Toggle)
- L'endpoint est pensé comme un interrupteur (toggle).
- **1er appel** sur `POST /api/courses/{id}/interactions/like` : Crée le like, incrémente `course.likeCount`, renvoie `liked: true`.
- **2ème appel** : Retire le like, décrémente `course.likeCount`, renvoie `liked: false`.

### C. Fonctionnement des Vues (Vues Uniques)
- Dès qu'un utilisateur consulte le détail d'un cours (via l'IHM), le frontend doit appeler `POST /api/courses/{id}/interactions/view`.
- Si c'est sa **première vue** : l'enregistrement est créé en base, `course.viewCount` est incrémenté, et l'API renvoie `recorded: true`.
- Si l'utilisateur **a déjà vu** le cours : l'API l'ignore silencieusement, le compteur ne bouge pas, et elle renvoie `recorded: false`.

### D. Gestion des Commentaires (CRUD)
- **Liste (Publique) :** N'importe qui peut lire les commentaires (`GET /api/courses/{id}/interactions/comments`). Aucune authentification n'est requise (`permitAll()`).
- **Création :** Un utilisateur authentifié peut ajouter un commentaire (`POST`).
- **Modification/Suppression :** Seul l'**auteur** du commentaire possède les droits pour le modifier (`PUT /.../{commentId}`) ou le supprimer (`DELETE /.../{commentId}`). Toute autre tentative déclenche une erreur `403 AccessDenied`.

---

## 3. Endpoints de l'API REST

Définis dans `CourseInteractionController` (Base Path: `/api/courses/{courseId}/interactions`) :

| Méthode | Route | Accès | Description |
|---|---|---|---|
| **POST** | `/like` | `STUDENT`, `TEACHER` | Ajoute ou retire un like sur un cours |
| **GET** | `/like` | `STUDENT`, `TEACHER` | Vérifie si l'utilisateur actuel a liké le cours |
| **POST** | `/view` | `STUDENT`, `TEACHER` | Enregistre une vue unique pour l'utilisateur |
| **GET** | `/comments` | *Public* | Liste tous les commentaires du cours (plus récents en premier) |
| **POST** | `/comments` | `STUDENT`, `TEACHER` | Ajoute un nouveau commentaire (max 1000 caractères) |
| **PUT** | `/comments/{commentId}` | Auteur | Modifie le contenu d'un commentaire existant |
| **DELETE** | `/comments/{commentId}` | Auteur | Supprime un commentaire existant |

---

## 4. Tests Unitaires & Couverture

La totalité de ces fonctionnalités est couverte par plus de **50 tests unitaires mockés** (MockMvc + Mockito), s'ajoutant aux tests déjà existants du projet :

- `EnrollmentControllerTest` : Vérifie toutes les règles liées aux inscriptions (auto-approbation enseignant, interdiction d'enrôler son propre cours, etc).
- `CourseInteractionControllerTest` : Vérifie les accès, les retours JSON (DTOs), les cas de non-autorisation (modifier le commentaire d'autrui), etc.
- `CourseInteractionServiceTest` : Vérifie le cœur logique (mise à jour des compteurs, vérification de l'existence des vues/likes pour éviter les doublons dynamiques).

**Résultat des tests :** Build Maven 100% SUCCESS (`./mvnw test`).
