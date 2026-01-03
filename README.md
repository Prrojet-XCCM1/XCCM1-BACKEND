# 🎓 XCCM1 - Backend

> **Projet de Groupe - UE IHM**  
> **4ème Année Génie Informatique (4GI)**  
> **École Nationale Supérieure Polytechnique de Yaoundé (ENSPY)**

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.0-green?logo=springboot)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Kafka](https://img.shields.io/badge/Kafka-3.7-black?logo=apachekafka)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue?logo=docker)

## 📝 Présentation

Bienvenue sur le dépôt du **Backend du projet XCCM1**. Ce projet a été réalisé dans le cadre de l'Unité d'Enseignement **IHM (Interface Homme-Machine)** du semestre 1 de la filière Génie Informatique à l'ENSPY.

Il s'agit de l'API RESTful robuste qui propulse l'application, gérant la logique métier, la persistance des données et la communication en temps réel.

## 🛠️ Stack Technique

Ce projet utilise des technologies modernes et performantes :

*   **Langage** : Java 21
*   **Framework** : Spring Boot 3.5.0
*   **Base de Données** : PostgreSQL
*   **Messaging / Streaming** : Apache Kafka (pour la communication asynchrone/temps réel)
*   **Sécurité** : Spring Security & JWT (Json Web Token)
*   **Outils de Build** : Maven
*   **Conteneurisation** : Docker & Docker Compose
*   **Déploiement** : Configuration prête pour Render

## 🚀 Installation et Démarrage

### Prérequis

*   Docker & Docker Compose (Recommandé)
*   Java 21 (Si lancement local sans Docker)
*   Maven

### Option 1 : Démarrage Rapide (Docker Compose) - Recommandé

C'est la méthode la plus simple, elle lance l'API, la base de données PostgreSQL, Zookeeper et Kafka automatiquement.

```bash
# Cloner le projet
git clone <votre-repo-url>
cd XCCM1-BACKEND

# Lancer tous les services
docker-compose up -d --build
```

L'API sera accessible sur : `http://localhost:8080`

### Option 2 : Démarrage Local (Développement)

Si vous souhaitez lancer l'application via votre IDE (IntelliJ, VS Code) ou Maven :

1.  Assurez-vous que **PostgreSQL** et **Kafka** sont en cours d'exécution (vous pouvez utiliser `docker-compose up postgres kafka zookeeper -d`).
2.  Configurez les variables d'environnement dans un fichier `.env` ou directement dans votre IDE.
3.  Lancez l'application :

```bash
./mvnw spring-boot:run
```

## 📂 Structure du Projet

```
XCCM1-BACKEND/
├── src/
│   ├── main/
│   │   ├── java/com/ihm/backend/  # Code Source
│   │   │   ├── controller/        # Endpoints REST
│   │   │   ├── service/           # Logique Métier
│   │   │   ├── repository/        # Accès Données
│   │   │   ├── model/             # Entités JPA
│   │   │   └── ...
│   │   └── resources/
│   │       └── application.yml    # Configuration
├── Dockerfile                     # Configuration Image Docker (Multi-stage)
├── docker-compose.yml             # Orchestration locale
├── render.yaml                    # Blueprint de déploiement Render
└── pom.xml                        # Dépendances Maven
```

## ☁️ Déploiement

Le projet est configuré pour un déploiement continu sur **Render**.
Un fichier `render.yaml` est inclus à la racine pour utiliser la fonctionnalité "Blueprint" de Render, qui provisionne automatiquement :
*   Le service Web (API Spring Boot)
*   La base de données PostgreSQL gérée

> **Note** : Pour Kafka en production, il est recommandé d'utiliser un fournisseur externe comme Upstash ou Confluent Cloud et de configurer la variable `SPRING_KAFKA_BOOTSTRAP_SERVERS`.

## 👥 L'Équipe (Team 4GI)

Ce projet est le fruit du travail collaboratif de **12 étudiants** ingénieurs de la promotion 4GI.

**Chef de Projet :**
*   👤 **AZANGUE LEONEL DELMAT**

**Membres de l'équipe :**
*   👤 [Membre 2]
*   👤 [Membre 3]
*   👤 [Membre 4]
*   👤 [Membre 5]
*   👤 [Membre 6]
*   👤 [Membre 7]
*   👤 [Membre 8]
*   👤 [Membre 9]
*   👤 [Membre 10]
*   👤 [Membre 11]
*   👤 [Membre 12]

---
*Réalisé avec ❤️ à l'École Nationale Supérieure Polytechnique de Yaoundé - 2026*
