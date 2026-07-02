# Déploiement XCCM1-BACKEND sur le VPS Contabo

Le projet tourne en production sur un VPS Contabo (`109.199.105.251`, domaine
`xccm1.duckdns.org`), partagé avec plusieurs autres projets (IAXCCM, XCCM1-LLM-SERVICE,
etc.), chacun dans son propre dossier sous `/root/xccm/`.

**État réel confirmé (2026-07-01)** :
- Le déploiement de XCCM1-BACKEND tourne depuis `/root/xccm` (racine), avec un seul fichier
  `/root/xccm/docker-compose.yml` et un seul `/root/xccm/.env` — **il n'y a pas de
  `docker-compose.prod.yml` séparé** (ce fichier existait dans le repo mais n'a jamais
  correspondu à ce qui tourne réellement ; il a été supprimé).
- Le service Hocuspocus (Y.js) et Redis (requis par `LockService`) sont maintenant déployés
  et fonctionnels sur ce VPS.
- Le pipeline CI/CD (`.github/workflows/deploy.yml`) a été corrigé pour cibler ce vrai chemin.

Le frontend (Next.js) n'est **pas** couvert ici : il est déployé séparément sur Vercel.

---

## A. Comment Hocuspocus a été mis en ligne (référence / à refaire si besoin)

### A.1 Diagnostic qui a mené ici

Le premier essai de déploiement automatique échouait silencieusement car le pipeline CI/CD
ciblait `~/xccm/XCCM1-BACKEND/docker-compose.prod.yml`, un chemin et un fichier qui n'ont
jamais existé dans la vraie installation. La vraie pile (Postgres nommé `xccm-db`, backend
nommé `xccm1-backend`, etc.) tourne depuis `/root/xccm/docker-compose.yml`. Cette confusion
venait d'un fichier `docker-compose.prod.yml` présent dans le repo mais jamais réellement
utilisé sur le VPS — il a été supprimé pour éviter que ça se reproduise.

### A.2 Ce qui a été fait pour corriger

1. Ajout de `INTERNAL_API_KEY` dans `/root/xccm/.env` (clé partagée entre le backend et
   Hocuspocus pour sécuriser `/api/internal/**`).
2. Ajout des services `redis` et `hocuspocus` directement dans `/root/xccm/docker-compose.yml`,
   en respectant les conventions déjà en place dans ce fichier (`${APP_CONTAINER_NAME}` pour
   joindre le backend, réseau `xccm-network` déclaré localement — pas externe).
3. Ajout de `REDIS_HOST`/`REDIS_PORT` dans l'environnement du service `app`.
4. `docker compose up -d` (sans préciser de service) : ça a recréé `redis` (nouveau),
   `hocuspocus` (nouveau) et `app` (recréé pour prendre en compte les nouvelles variables
   d'environnement), et laissé `postgres`/`kafka`/`zookeeper`/`elasticsearch` totalement
   intacts (leur config n'avait pas changé).
5. Ajout d'un bloc `location /collab/` dans `nginx.conf` (voir A.3) pour exposer Hocuspocus
   depuis Internet via `wss://xccm1.duckdns.org/collab/`.

Le repo (`docker-compose.yml`, `.github/workflows/deploy.yml`) a été mis à jour en miroir de
cet état réel, pour que les prochains `git push` sur `main` maintiennent tout ça synchronisé
automatiquement (voir section A.4).

### A.3 Nginx

`nginx.conf` à la racine de ce repo est une **copie locale de référence** du fichier réellement
utilisé sur le VPS (`/etc/nginx/nginx.conf`, remplace le fichier nginx principal, pas un site
dans `sites-available/`) — il n'est jamais synchronisé automatiquement, il faut le copier à la
main après modification :

```bash
# Depuis votre machine locale
scp "nginx.conf" root@109.199.105.251:/tmp/nginx.conf

# Sur le VPS
ssh root@109.199.105.251
cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.bak-$(date +%F)
cp /tmp/nginx.conf /etc/nginx/nginx.conf
nginx -t && systemctl reload nginx
```

Le bloc ajouté pour Hocuspocus (cible `127.0.0.1:1234`, pas l'IP publique, puisque
`docker-compose.yml` ne publie ce port qu'en loopback) :

```nginx
    location /collab/ {
        proxy_pass http://127.0.0.1:1234/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
        proxy_buffering off;
    }
```

Côté frontend (Vercel), le client Hocuspocus doit pointer vers
`wss://xccm1.duckdns.org/collab/<courseId>`. Le WebSocket STOMP existant reste sur
`wss://xccm1.duckdns.org/ws` (inchangé).

### A.4 Ce que fait maintenant le CI/CD à chaque push sur `main`

1. **Job `build`** : construit et pousse deux images sur `ghcr.io` — `xccm1-backend` et
   `xccm1-backend-hocuspocus` (buildée depuis [hocuspocus/](hocuspocus/)).
2. **Job `deploy`** :
   - copie `docker-compose.yml` du repo vers `/root/xccm/docker-compose.yml` sur le VPS
     (`appleboy/scp-action`) — donc gardez ce fichier comme source de vérité, ne le modifiez
     plus à la main sur le VPS sans reporter le changement dans le repo (sinon le prochain
     push l'écrasera) ;
   - se connecte en SSH, fait `docker login` (avec `GHCR_TOKEN`/`GHCR_USER`, un PAT — plus
     fiable que le `GITHUB_TOKEN` éphémère pour cet usage), puis `docker compose pull` +
     `docker compose up -d` (sans filtrer de service : ne recrée que ce qui a changé).

**Secrets GitHub Actions requis** (Settings → Secrets and variables → Actions) :

| Secret | Rôle |
|---|---|
| `SSH_HOST` | IP du VPS |
| `SSH_USER` | utilisateur SSH (`root` actuellement) |
| `SSH_KEY` | clé privée SSH correspondante |
| `GHCR_TOKEN` | PAT GitHub avec droit `read:packages` (pour `docker login` sur le VPS) |
| `GHCR_USER` | nom d'utilisateur GitHub associé au PAT |
| `DEPLOY_PATH` | *(optionnel)* chemin si différent de `/root/xccm` |

---

## B. Référence complète (recréer le VPS depuis zéro)

Si un jour il faut reconstruire le VPS entièrement. Si le VPS actuel fonctionne déjà, ignorez
cette section.

### B.1 Architecture

```
Internet
   │
   ▼
Nginx (installé sur l'hôte, PAS dans Docker) — TLS via Let's Encrypt (xccm1.duckdns.org)
   │
   ├─ /            → :8080  (backend Spring Boot, REST)
   ├─ /ws          → :8080  (STOMP WebSocket, collaboration structurelle : LOCK/MOVE/DELETE…)
   ├─ /collab/     → 127.0.0.1:1234  (Hocuspocus, édition de texte Y.js temps réel)
   ├─ /llm/, /api/v1/notebooks/*  → :8000  (service LLM, hors de ce repo, projet séparé)
   └─ /ai/         → :5000  (agent IA, hors de ce repo, projet séparé)
   ▼
┌─────────────────────────────────────────────────────────┐
│ Docker (réseau "xccm-network", déclaré dans              │
│ /root/xccm/docker-compose.yml)                           │
│  app (backend, 8080) ── postgres (xccm-db)                │
│         │             ── kafka + zookeeper                │
│         │             ── elasticsearch                    │
│         │             ── redis (verrous de collaboration) │
│  hocuspocus (1234) ── appelle backend via /api/internal    │
└─────────────────────────────────────────────────────────┘
```

Le service LLM (`:8000`) et l'agent IA (`:5000`) sont des projets à part entière, déployés
séparément dans `/root/xccm/XCCM1-LLM-SERVICE/` et `/root/xccm/IAXCCM/`.

### B.2 Prérequis

- VPS Contabo (2 vCPU / 8 Go RAM minimum : Elasticsearch + Kafka sont gourmands), Ubuntu 22.04/24.04.
- Domaine ou sous-domaine DuckDNS pointant vers l'IP du VPS.
- Accès root (ou sudo) sur le VPS, et droits admin sur le repo GitHub.

### B.3 Préparation initiale du VPS

```bash
curl -fsSL https://get.docker.com | sh
```

Pare-feu (seuls 22, 80, 443 ouverts vers l'extérieur) :

```bash
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

### B.4 DNS

```bash
dig +short xccm1.duckdns.org
```

### B.5 Nginx + HTTPS

```bash
apt install -y nginx certbot python3-certbot-nginx
```

Copier `nginx.conf` de ce repo vers `/etc/nginx/nginx.conf` (voir A.3), puis :

```bash
nginx -t && systemctl reload nginx
certbot --nginx -d xccm1.duckdns.org
```

### B.6 Créer le dossier de déploiement et `.env`

```bash
mkdir -p /root/xccm && cd /root/xccm
```

Envoyer `docker-compose.yml` depuis votre machine locale — les déploiements suivants le
referont automatiquement via le CI/CD (voir A.4) :

```bash
scp docker-compose.yml root@109.199.105.251:/root/xccm/docker-compose.yml
```

Puis créer `.env` à côté :

```dotenv
# --- Docker : noms de conteneurs & ports ---
POSTGRES_CONTAINER_NAME=xccm-db
POSTGRES_VERSION=17
POSTGRES_DB=xccm1
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<mot_de_passe_fort>
POSTGRES_PORT=5432
APP_CONTAINER_NAME=xccm1-backend
APP_PORT=8080
DOCKER_IMAGE=ghcr.io/prrojet-xccm1/xccm1-backend:latest

# --- JWT & sécurité ---
JWT_SECRET=<clé_base64_d'au_moins_64_caractères>
INTERNAL_API_KEY=<chaîne_aléatoire_d'au_moins_32_caractères>

# --- Email ---
EMAIL_USERNAME=votre_email@gmail.com
EMAIL_PASSWORD=<mot_de_passe_application_gmail>

# --- LLM externe (si utilisé) ---
LLM_SERVICE_URL=http://xccm-llm-service:8000

# --- Frontend / CORS ---
FRONTEND_URL=https://frontend-xccm-12027.vercel.app
CORS_ALLOWED_ORIGINS=https://frontend-xccm-12027.vercel.app
IP_ADDRESS=https://xccm1.duckdns.org

SPRING_PROFILES_ACTIVE=prod
```

> `JWT_SECRET` doit être identique entre le backend et Hocuspocus — `docker-compose.yml` le
> repasse automatiquement au conteneur `hocuspocus`. Ne le régénérez jamais après une mise en
> prod : ça invaliderait tous les tokens existants.

### B.7 Secrets GitHub Actions

Voir le tableau en A.4.

### B.8 Vérifications post-déploiement

```bash
docker compose ps
docker compose logs -f app
curl -I https://xccm1.duckdns.org/actuator/health
```

### B.9 Maintenance courante

```bash
# Logs
docker compose logs -f hocuspocus

# Redémarrer un service
docker compose restart app

# Sauvegarder PostgreSQL
docker exec xccm-db pg_dump -U postgres xccm1 > backup_$(date +%F).sql

# Mise à jour manuelle (le VPS n'a pas de dépôt git : on renvoie le compose par scp si besoin)
scp docker-compose.yml root@109.199.105.251:/root/xccm/docker-compose.yml
cd /root/xccm
docker compose pull
docker compose up -d
```

### B.10 Limites connues

- Le service LLM et l'agent IA référencés dans `nginx.conf` (`:8000`, `:5000`) sont des projets
  séparés déployés indépendamment sur le même VPS — pas gérés par ce repo.
- Kafka/Zookeeper et Elasticsearch sont gourmands en RAM ; sur un VPS à 4 Go ou moins,
  surveillez `docker stats`.
- Le VPS héberge plusieurs projets non liés (IAXCCM, XCCM1-LLM-SERVICE, securite_sociale,
  medical_api, datapipe, ml-iot, iot_soil...) dans des sous-dossiers séparés de `/root/xccm/` —
  vérifiez toujours le bon dossier avant toute commande `docker compose`.
