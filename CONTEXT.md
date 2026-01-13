# Contexte Technique & Architecture - OsteoLinkSMS

Ce fichier sert de référence pour les développeurs et agents IA intervenant sur le projet. Il détaille la logique interne, les choix d'implémentation et les règles métier.

## 🎯 Objectif du Projet
Automatisation de la gestion des appels manqués pour les praticiens de santé.
L'application doit être autonome, robuste, et fonctionner 100% en local (confidentialité patient).

## 🏗 Architecture & Flux de Données

### 1. Détection des Appels (`CallReceiver.kt`)
*   **Trigger** : `BroadcastReceiver` sur `PHONE_STATE`.
*   **Logique d'État** :
    *   `RINGING` : On stocke `was_ringing = true`.
    *   `IDLE` (Raccroché) : Si `was_ringing` était vrai, on lance la procédure de vérification.
*   **Mécanisme Critique ("Smart Polling")** :
    *   Android met parfois plusieurs secondes à écrire l'appel manqué dans le `CallLog`.
    *   **Solution** : Une boucle (`Coroutine`) qui vérifie le `CallLog` toutes les 500ms pendant 5 secondes max.
    *   **Critère** : Appel de type `MISSED_TYPE` datant de moins de 60 secondes.

### 2. Filtrage & Décision (`CallReceiver.kt`)
Avant d'envoyer un SMS, plusieurs filtres sont appliqués (ordre d'exécution) :
1.  **Master Switch** : Si `KEY_APP_ENABLED` est faux, on arrête tout.
2.  **Type de Numéro** : Vérification des préfixes mobiles selon les pays activés (FR `+336/7`, BE `+324`, etc.).
3.  **Whitelist** : Vérification si le numéro est dans la liste d'exclusion.
4.  **Anti-Spam** : Vérification dans l'historique interne si un SMS a déjà été envoyé à ce numéro il y a moins de X minutes (`KEY_DELAY_MINUTES`).
5.  **Contacts** : Si option activée, ignore les numéros présents dans le répertoire Android.

### 3. Gestion des Messages (`EditMessagesActivity.kt`)
*   Les messages sont construits dynamiquement : `Prefixe` + `Corps` + `Lien (Optionnel)`.
*   **Prefixe** : Dépend du mode (Travail, Hors Horaires, Vacances) et inclut "Cabinet d'Ostéopathie [NOM] :" si configuré.
*   **Lien** : Doctolib ou autre, géré intelligemment (ajout automatique de `/praticien/` si ID simple).

### 4. Envoi SMS (`SmsSender.kt`)
*   Utilise `SmsManager`.
*   **Support Double SIM** : Tente de récupérer le `SubscriptionId` correspondant au slot SIM choisi par l'utilisateur (0 ou 1). Fallback sur la SIM par défaut.
*   **Multipart** : Gère les messages longs (>160 chars).
*   **Feedback** : Utilise `PendingIntent` vers `SmsResultReceiver` pour confirmer l'envoi et notifier l'utilisateur.

---

## 💾 Persistance des Données (`SharedPreferences`)

Tout est stocké dans `OsteoLinkPrefs` (sauf l'historique brut dans `HistoryPrefs`).

| Clé | Type | Description |
| :--- | :--- | :--- |
| `appEnabled` | Boolean | Master Switch (ON/OFF). |
| `simSlot` | Int | -1 (Défaut), 0 (SIM 1), 1 (SIM 2). |
| `countryFR`, `countryBE`... | Boolean | Autorisation des numéros étrangers. |
| `includeBookingLink` | Boolean | Ajout du lien RDV dans le SMS. |
| `doctolibId` | String | URL ou ID Doctolib. |
| `practitionerName` | String | Nom du cabinet/praticien. |
| `whitelist` | String | Numéros séparés par des virgules. |
| `workDays` | String | Jours de travail (ex: "2,3,4,5,6"). |

L'historique (`HistoryManager`) est stocké sous forme de liste de Strings dans une préférence dédiée. (Candidat pour migration vers Room DB si problèmes de performance).

---

## ⚠️ Points d'Attention pour Modifications Futures

1.  **Permissions** : L'application dépend de `READ_CALL_LOG` et `SEND_SMS`. Toute modification de ces flux doit être testée rigoureusement car Google restreint ces permissions.
2.  **Doze Mode** : Le `CallReceiver` utilise `goAsync()`. Pour des tâches plus longues, envisager `WorkManager`.
3.  **Formats Numéros** : La détection `isMobileNumber` est basée sur des préfixes (`startsWith`). Si ajout de nouveaux pays, vérifier les formats locaux mobiles vs fixes.
