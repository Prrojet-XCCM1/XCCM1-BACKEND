# Fiche de Configuration Moodle Client — Jour 2
**Date de la réunion :** _à compléter_  
**Participants :** Personne 1 (FE1) + Personne 3 (BE) + équipe Moodle Client  
**Durée :** 1h30

---

## Informations à recueillir (à remplir pendant la réunion)

### 1. Plateforme Moodle

| Paramètre | Valeur | Statut |
|---|---|---|
| URL de la plateforme | `https://` | ⬜ À remplir |
| Version Moodle | ex: `4.2.x` | ⬜ À remplir |
| LTI 1.3 supporté ? | Oui / Non | ⬜ À confirmer |
| Environnement | Dev / Staging / Production | ⬜ À remplir |

> ⚠️ **CRITIQUE :** La version Moodle doit être **≥ 3.9** pour supporter LTI 1.3.  
> Si la version est antérieure, prévoir un fallback LTI 1.1.

---

### 2. Endpoints LTI de Moodle

| Paramètre | Valeur | Statut |
|---|---|---|
| JWKS endpoint | `https://.../mod/lti/certs.php` | ⬜ À remplir |
| Authorization endpoint (OIDC login) | `https://.../mod/lti/auth.php` | ⬜ À remplir |
| Token endpoint | `https://.../mod/lti/token.php` | ⬜ À remplir |
| Issuer (iss attendu dans le JWT) | `https://...` | ⬜ À remplir |

---

### 3. Enregistrement de l'outil XCCM1 dans Moodle

Ces informations sont à **fournir à l'équipe Moodle** pour qu'ils configurent XCCM1 côté Moodle :

| Paramètre XCCM1 | Valeur |
|---|---|
| Nom de l'outil | `XCCM1 — Éditeur de cours` |
| URL de lancement (Tool URL) | `https://api.xccm1.tld/lti/launch` |
| URL OIDC Initiation Login | `https://api.xccm1.tld/lti/login` _(à créer J3)_ |
| JWKS URL de XCCM1 | `https://api.xccm1.tld/lti/.well-known/jwks.json` _(à créer J10)_ |
| Deep Linking URL | _(non requis pour ce sprint)_ |
| Redirect URI(s) | `https://api.xccm1.tld/lti/launch` |

---

### 4. client_id attribué par Moodle

| Paramètre | Valeur | Statut |
|---|---|---|
| `client_id` | _(fourni par Moodle après enregistrement)_ | ⬜ À remplir |
| `deployment_id` | _(facultatif, si multi-déploiement)_ | ⬜ À remplir |

---

### 5. Configuration réseau & accès

| Paramètre | Valeur | Statut |
|---|---|---|
| L'instance Moodle peut-elle atteindre le backend XCCM1 ? | Oui / Non | ⬜ À vérifier |
| Accès au JWKS XCCM1 possible depuis Moodle ? | Oui / Non | ⬜ À vérifier |
| Environnement de test disponible (date) ? | _(date J10)_ | ⬜ À planifier |

---

### 6. Créneaux pour les tests conjoints

| Session | Date prévue | Participants |
|---|---|---|
| Première intégration réelle (J10) | ⬜ | Personne 1, Personne 3, équipe Moodle |
| Tests de bout en bout (J12) | ⬜ | Toute l'équipe + équipe Moodle |

---

## Variables d'environnement à configurer après la réunion

Une fois ces informations recueillies, mettre à jour le `.env` / `application.properties` :

```properties
# LTI 1.3 — À compléter avec les valeurs Moodle
MOODLE_JWKS_URL=https://<URL_MOODLE>/mod/lti/certs.php
MOODLE_PLATFORM_URL=https://<URL_MOODLE>
MOODLE_CLIENT_ID=<client_id_fourni_par_moodle>
LTI_XCCM1_ISSUER=https://api.xccm1.tld
```

---

## Notes de la réunion

_Zone libre pour noter les points discutés, questions, décisions prises._

```
...
```

---

**Statut :** ⬜ Réunion non encore tenue — à compléter le Jour 2
