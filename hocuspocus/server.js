import { Server } from '@hocuspocus/server'
import { Logger } from '@hocuspocus/extension-logger'
import { Database } from '@hocuspocus/extension-database'
import jwt from 'jsonwebtoken'
import axios from 'axios'

const SPRING_API_URL = process.env.SPRING_API_URL || 'http://localhost:8080'
// Le backend Spring signe les JWT avec une clé encodée en Base64 (voir JwtService.getSignInKey()).
// jsonwebtoken doit vérifier avec les mêmes octets, donc on décode la clé avant usage.
const JWT_SECRET_KEY = Buffer.from(process.env.JWT_SECRET || '', 'base64')
const INTERNAL_API_KEY = process.env.INTERNAL_API_KEY || 'hocuspocus-internal-key'
const PORT = parseInt(process.env.PORT || '1234', 10)

const server = Server.configure({
  port: PORT,

  /**
   * Authentification: valider le JWT et vérifier l'accès au cours via Spring Boot.
   * Le documentName est l'ID du cours (ex: "42").
   * Le token est le JWT de l'utilisateur.
   */
  async onAuthenticate(data) {
    const { token, documentName } = data
    const courseId = documentName

    if (!token) throw new Error('Token JWT manquant')

    // 1. Valider le JWT localement (rapide, sans réseau)
    let decoded
    try {
      decoded = jwt.verify(token, JWT_SECRET_KEY)
    } catch (err) {
      throw new Error('Token JWT invalide ou expiré')
    }

    // 2. Vérifier l'accès au cours via Spring Boot (autorisation métier)
    try {
      await axios.get(`${SPRING_API_URL}/api/collaboration/access/${courseId}`, {
        headers: { Authorization: `Bearer ${token}` },
        timeout: 5000
      })
    } catch (err) {
      if (err.response?.status === 403) {
        throw new Error('Accès refusé au document ' + courseId)
      }
      // Si Spring Boot est indisponible, continuer (dégradé gracieux)
      console.warn('[Hocuspocus] Impossible de vérifier accès via Spring Boot:', err.message)
    }

    // Retourner les infos utilisateur pour l'awareness (curseurs)
    return {
      user: {
        id: decoded.sub || decoded.email || 'unknown',
        email: decoded.email || decoded.sub,
        name: decoded.firstName || (decoded.email ? decoded.email.split('@')[0] : 'Collaborateur'),
      }
    }
  },

  extensions: [
    new Logger({
      log: (message) => {
        if (process.env.NODE_ENV !== 'production') console.log(message)
      }
    }),

    new Database({
      /**
       * Charger l'état binaire Y.js depuis PostgreSQL via Spring Boot.
       * Retourne null si aucun état sauvegardé (nouveau document → démarrage vide).
       */
      fetch: async ({ documentName }) => {
        const courseId = documentName
        try {
          const response = await axios.get(
            `${SPRING_API_URL}/api/internal/courses/${courseId}/yjs-state`,
            {
              headers: { 'X-Internal-Key': INTERNAL_API_KEY },
              responseType: 'arraybuffer',
              timeout: 10000
            }
          )
          if (response.data && response.data.byteLength > 0) {
            console.log(`[Hocuspocus] État Y.js chargé: cours ${courseId} (${response.data.byteLength} bytes)`)
            return new Uint8Array(response.data)
          }
          return null
        } catch (err) {
          if (err.response?.status === 404) {
            console.log(`[Hocuspocus] Pas d'état Y.js pour le cours ${courseId}, démarrage vide`)
            return null
          }
          console.error(`[Hocuspocus] Erreur chargement état Y.js cours ${courseId}:`, err.message)
          return null
        }
      },

      /**
       * Sauvegarder l'état binaire Y.js dans PostgreSQL via Spring Boot.
       * Appelé automatiquement après chaque modification (debounced par Hocuspocus).
       */
      store: async ({ documentName, state }) => {
        const courseId = documentName
        try {
          await axios.put(
            `${SPRING_API_URL}/api/internal/courses/${courseId}/yjs-state`,
            Buffer.from(state),
            {
              headers: {
                'Content-Type': 'application/octet-stream',
                'X-Internal-Key': INTERNAL_API_KEY
              },
              timeout: 10000
            }
          )
          console.log(`[Hocuspocus] État Y.js sauvegardé: cours ${courseId}`)
        } catch (err) {
          console.error(`[Hocuspocus] Erreur sauvegarde état Y.js cours ${courseId}:`, err.message)
        }
      }
    })
  ]
})

server.listen().then(() => {
  console.log(`Hocuspocus démarré sur le port ${PORT}`)
  console.log(`Spring Boot API: ${SPRING_API_URL}`)
})
