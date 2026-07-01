# Déploiement XCCM1-BACKEND sur le VPS Contabo

Le projet tourne déjà en production sur un VPS Contabo (`109.199.105.251`, domaine
`xccm1.duckdns.org`). Ce guide couvre :
- **Section A** : ce qu'il faut faire *maintenant* pour mettre en ligne le nouveau serveur
  Hocuspocus (Y.js) sur ce VPS déjà déployé.
- **Section B** : la référence complète (utile si un jour il faut recréer le VPS depuis zéro).

Le frontend (Next.js) n'est **pas** couvert ici : il est déployé séparément sur Vercel.

---

## A. Déployer Hocuspocus sur le VPS déjà en production

### A.1 Ce qui a changé dans le repo

- `docker-compose.prod.yml` contient maintenant deux nouveaux services : `redis` (requis par
  `LockService`, qui n'était rattaché à aucun conteneur Redis auparavant) et `hocuspocus`
  (serveur de collaboration temps réel Y.js, code dans [hocuspocus/](hocuspocus/)).
- `.github/workflows/deploy.yml` build et pousse maintenant **deux** images sur `ghcr.io` :
  `xccm1-backend` (déjà existant) et `xccm1-backend-hocuspocus` (nouveau). Le VPS n'a **pas** le
  repo cloné (seulement Docker) : le job `deploy` copie donc désormais `docker-compose.prod.yml`
  directement sur le VPS via `scp` (`appleboy/scp-action`) avant de lancer `docker compose pull/up`,
  plutôt que de faire un `git pull` qui échouerait en l'absence de dépôt git local.
- `nginx.conf` (copie locale de référence, non versionnée sur GitHub — voir A.4) a été mis à jour
  avec un nouveau bloc `location /collab/` pour router le trafic WebSocket vers Hocuspocus.

### A.2 Pré-requis sur le VPS (`.env`)

Le fichier `.env` situé à côté de `docker-compose.prod.yml` sur le VPS (dans
`~/xccm/XCCM1-BACKEND` ou l'équivalent de votre installation) doit contenir une clé
`INTERNAL_API_KEY` — elle sécurise les endpoints `/api/internal/**` que Hocuspocus appelle
pour lire/écrire l'état Y.js d'un cours. Si elle n'existe pas encore :

```bash
ssh deploy@109.199.105.251
cd ~/xccm/XCCM1-BACKEND   # adapter si le chemin réel diffère
grep INTERNAL_API_KEY .env || echo "INTERNAL_API_KEY=$(openssl rand -hex 32)" >> .env
```

`JWT_SECRET` existe déjà (utilisé par le backend) : Hocuspocus le réutilise tel quel pour
vérifier les tokens des utilisateurs, pas besoin d'y toucher.

### A.3 Déployer les conteneurs

Le plus simple : poussez sur `main`, le pipeline CI/CD s'occupe de tout (build des deux images,
copie de `docker-compose.prod.yml` sur le VPS par `scp`, puis `docker compose pull && up -d`).

Pour forcer un déploiement immédiat sans attendre/pousser sur `main`, il faut d'abord amener le
`docker-compose.prod.yml` à jour sur le VPS vous-même (puisqu'il n'y a pas de dépôt git cloné
là-bas), puis lancer les conteneurs :

```bash
# Depuis votre machine locale, dans le dossier du repo
scp docker-compose.prod.yml deploy@109.199.105.251:~/xccm/XCCM1-BACKEND/docker-compose.prod.yml

ssh deploy@109.199.105.251
cd ~/xccm/XCCM1-BACKEND

echo "<GITHUB_TOKEN_read:packages>" | docker login ghcr.io -u VOTRE_USER_GITHUB --password-stdin
export DOCKER_IMAGE=ghcr.io/prrojet-xccm1/xccm1-backend:latest
export HOCUSPOCUS_IMAGE=ghcr.io/prrojet-xccm1/xccm1-backend-hocuspocus:latest
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d

docker compose -f docker-compose.prod.yml ps      # redis, backend, hocuspocus doivent être "Up"
docker compose -f docker-compose.prod.yml logs hocuspocus --tail=50
```

Le conteneur `hocuspocus` n'est publié que sur `127.0.0.1:1234` (voir
`docker-compose.prod.yml`), donc à ce stade il n'est pas encore joignable depuis Internet —
c'est Nginx qui doit faire le pont, à l'étape suivante.

### A.4 Mettre à jour Nginx sur le VPS

`nginx.conf` à la racine de ce repo est une **copie locale de référence** du fichier réellement
utilisé sur le VPS — il n'est pas synchronisé automatiquement (pas de `git pull` ni de volume
Docker ne le pousse sur le serveur). Il faut le copier à la main.

> ⚠️ **Bug trouvé en le testant** (`nginx -t`) : la copie que vous m'avez donnée n'avait pas
> l'accolade fermante du bloc `http { ... }` (il manquait un `}` à la toute fin du fichier).
> Je l'ai corrigée dans `nginx.conf`. Si le fichier réellement en place sur le VPS a la bonne
> accolade, tant mieux ; sinon c'est probablement pour ça qu'un `nginx -t` échouerait si vous
> deviez le recharger tel quel. Repartez bien de la version corrigée de ce repo.

Le nouveau bloc ajouté (à adapter si vous préférez un autre chemin que `/collab/`) :

```nginx
    # Serveur de collaboration Y.js (Hocuspocus) - édition de texte temps réel
    # Le conteneur n'est publié que sur 127.0.0.1:1234 (voir docker-compose.prod.yml),
    # donc on cible localhost ici, pas l'IP publique.
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

Notez qu'il cible `127.0.0.1:1234` et non l'IP publique `109.199.105.251:1234` comme le font
les autres blocs (`/ws`, `/ai/`, `/llm/`) : c'est volontaire et plus sûr, puisque
`docker-compose.prod.yml` ne publie Hocuspocus que sur loopback. Nginx tournant sur la même
machine, `127.0.0.1` fonctionne très bien et évite d'exposer le port 1234 sur l'IP publique.

Pour appliquer sur le VPS :

```bash
# Depuis votre machine locale : copier le fichier corrigé vers le VPS
scp "nginx.conf" deploy@109.199.105.251:/tmp/nginx.conf

# Sur le VPS
ssh deploy@109.199.105.251
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.bak-$(date +%F)   # sauvegarde de sécurité
sudo cp /tmp/nginx.conf /etc/nginx/nginx.conf
sudo nginx -t                     # DOIT afficher "syntax is ok" / "test is successful"
sudo systemctl reload nginx
```

> Le fichier remplace le `nginx.conf` **principal** (`/etc/nginx/nginx.conf`), pas un site dans
> `sites-available/` — c'est la convention déjà utilisée sur ce VPS (le fichier contient ses
> propres blocs `events {}` et `http {}`), gardez-la pour rester cohérent avec l'existant.

### A.5 Vérification

```bash
# Doit répondre 200
curl -I https://xccm1.duckdns.org/actuator/health

# Doit initier un handshake WebSocket (101 Switching Protocols) une fois un client Y.js connecté
curl -i -N \
  -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  https://xccm1.duckdns.org/collab/
```

Côté frontend (Vercel), pointez le client Hocuspocus vers `wss://xccm1.duckdns.org/collab/<courseId>`
et le WebSocket STOMP existant reste sur `wss://xccm1.duckdns.org/ws` (inchangé).

---

## B. Référence complète (recréer le VPS depuis zéro)

Cette section documente l'ensemble de la stack pour le cas où il faudrait reconstruire le VPS
(nouveau serveur, migration, etc.). Si le VPS actuel fonctionne déjà, vous pouvez l'ignorer.

### B.1 Architecture

```
Internet
   │
   ▼
Nginx (installé sur l'hôte, PAS dans Docker) — TLS via Let's Encrypt (xccm1.duckdns.org)
   │
   ├─ /            → 127.0.0.1 ou IP publique:8080  (backend Spring Boot, REST)
   ├─ /ws          → :8080  (STOMP WebSocket, collaboration structurelle : LOCK/MOVE/DELETE…)
   ├─ /collab/     → 127.0.0.1:1234  (Hocuspocus, édition de texte Y.js temps réel)
   ├─ /llm/, /api/v1/notebooks/*  → :8000  (service LLM, hors de ce repo)
   └─ /ai/         → :5000  (agent IA, hors de ce repo)
   ▼
┌─────────────────────────────────────────────────────────┐
│ Docker (réseau "xccm-network")                          │
│  backend (8080) ── postgres                              │
│         │        ── kafka + zookeeper                    │
│         │        ── elasticsearch                        │
│         │        ── redis (verrous de collaboration)     │
│  hocuspocus (1234) ── appelle backend via /api/internal   │
└─────────────────────────────────────────────────────────┘
```

Le service LLM (`:8000`) et l'agent IA (`:5000`) référencés dans `nginx.conf` ne font pas partie
de ce repo Spring Boot : ils sont déployés séparément sur le même VPS.

### B.2 Prérequis

- VPS Contabo (2 vCPU / 8 Go RAM minimum : Elasticsearch + Kafka sont gourmands), Ubuntu 22.04/24.04.
- Domaine ou sous-domaine DuckDNS pointant vers l'IP du VPS.
- Accès `sudo` sur le VPS et droits admin sur le repo GitHub (pour les secrets Actions).

### B.3 Préparation initiale du VPS

```bash
ssh root@VOTRE_IP_CONTABO
adduser deploy
usermod -aG sudo deploy
rsync --archive --chown=deploy:deploy ~/.ssh /home/deploy
# Durcir /etc/ssh/sshd_config : PermitRootLogin no / PasswordAuthentication no
systemctl restart ssh

curl -fsSL https://get.docker.com | sh
usermod -aG docker deploy
```

Pare-feu (seuls 22, 80, 443 ouverts vers l'extérieur ; Postgres/Kafka/Elasticsearch/Redis et les
ports internes des services applicatifs restent en loopback ou réseau Docker interne) :

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### B.4 DNS

Enregistrement `A` chez votre registrar (ou DuckDNS) vers l'IP publique du VPS :

```bash
dig +short xccm1.duckdns.org
```

### B.5 Réseau Docker externe

```bash
docker network create xccm-network
```

### B.6 Nginx + HTTPS

```bash
sudo apt install -y nginx certbot python3-certbot-nginx
```

Copier `nginx.conf` de ce repo vers `/etc/nginx/nginx.conf` sur le VPS (voir A.4 pour la
procédure et le contenu à jour), puis :

```bash
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d xccm1.duckdns.org
```

### B.7 Créer le dossier de déploiement et configurer `.env`

Le VPS n'a pas besoin du code source : Docker Compose n'a besoin que de
`docker-compose.prod.yml` et de `.env` pour tourner (les images sont construites ailleurs, par
GitHub Actions, et récupérées via `docker pull`). Créez simplement le dossier attendu par le
pipeline CI/CD :

```bash
mkdir -p ~/xccm/XCCM1-BACKEND && cd ~/xccm/XCCM1-BACKEND
```

Puis, depuis votre machine locale (où le repo est cloné), envoyez `docker-compose.prod.yml` une
première fois — les déploiements suivants le referont automatiquement via le CI/CD (voir A.1) :

```bash
scp docker-compose.prod.yml deploy@109.199.105.251:~/xccm/XCCM1-BACKEND/docker-compose.prod.yml
```

Sur le VPS, créez ensuite `.env` à côté :

```bash
nano ~/xccm/XCCM1-BACKEND/.env
```

```dotenv
# --- PostgreSQL ---
POSTGRES_DB=xccm
POSTGRES_USER=admin
POSTGRES_PASSWORD=<mot_de_passe_fort>

# --- JWT & sécurité ---
JWT_SECRET=<clé_base64_d'au_moins_64_caractères>
INTERNAL_API_KEY=<chaîne_aléatoire_d'au_moins_32_caractères>

# --- Email ---
EMAIL_USERNAME=votre_email@gmail.com
EMAIL_PASSWORD=<mot_de_passe_application_gmail>

# --- OAuth2 (si utilisé) ---
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=

# --- LLM externe (si utilisé) ---
LLM_SERVICE_URL=http://xccm-llm-service:8000

# --- Frontend / CORS ---
FRONTEND_URL=https://frontend-xccm-12027.vercel.app
CORS_ALLOWED_ORIGINS=https://frontend-xccm-12027.vercel.app
IP_ADDRESS=https://xccm1.duckdns.org
```

> `JWT_SECRET` doit être identique entre le backend et Hocuspocus — `docker-compose.prod.yml`
> le repasse automatiquement au conteneur `hocuspocus`. Ne le régénérez jamais après une mise
> en prod : ça invaliderait tous les tokens existants.

### B.8 Secrets GitHub Actions

| Secret | Valeur |
|---|---|
| `SSH_HOST` | IP publique du VPS Contabo |
| `SSH_USER` | `deploy` |
| `SSH_KEY` | Clé privée SSH correspondant à la clé publique autorisée sur le VPS |
| `DEPLOY_PATH` | *(optionnel)* chemin si différent de `~/xccm` |

### B.9 Vérifications post-déploiement

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
curl -I https://xccm1.duckdns.org/actuator/health
```

### B.10 Maintenance courante

```bash
# Logs
docker compose -f docker-compose.prod.yml logs -f hocuspocus

# Redémarrer un service
docker compose -f docker-compose.prod.yml restart backend

# Sauvegarder PostgreSQL
docker exec xccm-postgres pg_dump -U admin xccm > backup_$(date +%F).sql

# Mise à jour manuelle (le VPS n'a pas de dépôt git : on renvoie le compose par scp si besoin)
scp docker-compose.prod.yml deploy@109.199.105.251:~/xccm/XCCM1-BACKEND/docker-compose.prod.yml
cd ~/xccm/XCCM1-BACKEND
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

### B.11 Limites connues

- Le `healthcheck` du service `hocuspocus` dans `docker-compose.yml` (dev) cible
  `http://localhost:1234/health`, une route qui n'existe pas dans un serveur Hocuspocus par
  défaut (WebSocket only). Le conteneur peut apparaître `unhealthy` même s'il fonctionne
  normalement — sans impact fonctionnel, juste cosmétique dans `docker compose ps`.
- Le service LLM et l'agent IA référencés dans `nginx.conf` (`:8000`, `:5000`) ne sont déployés
  par aucun `docker-compose*.yml` de ce repo : ils vivent ailleurs sur le même VPS et doivent
  être maintenus indépendamment.
- Kafka/Zookeeper et Elasticsearch sont gourmands en RAM ; sur un VPS à 4 Go ou moins,
  surveillez `docker stats`.
