# OsteoLinkSMS

**OsteoLinkSMS** est une application Android d'automatisation conçue pour les professionnels de santé (ostéopathes, kinésithérapeutes, médecins) afin de gérer efficacement les appels manqués lors des consultations ou en dehors des horaires de travail.

L'objectif est d'assurer qu'aucun patient ne reste sans réponse, en envoyant automatiquement un SMS informatif (prise de RDV, indisponibilité, congés) lorsqu'un appel est manqué sur un téléphone mobile.

---

## 🚀 Fonctionnalités Clés

### 1. Gestion Intelligente des Appels
*   **Détection Fiable** : Utilise un algorithme de "Smart Polling" pour détecter les appels manqués dans le journal d'appels, même si le système Android met quelques secondes à l'écrire.
*   **Filtrage** : Ne répond qu'aux numéros mobiles (06/07 en France, et équivalents étrangers configurables). Ignore les numéros fixes et masqués.
*   **Anti-Spam** : Empêche l'envoi multiple de SMS au même numéro dans un intervalle donné (par défaut 5 minutes).
*   **Liste d'Exclusion (Whitelist)** : Permet d'ignorer certains numéros (famille, amis) pour ne pas leur envoyer de SMS pro.

### 2. Modes de Fonctionnement
*   **Mode Travail** : Activé selon vos horaires (ex: Lun-Ven, 8h-19h). Envoie un message "En consultation".
*   **Mode Hors Horaires** : Activé soirs et week-ends. Envoie un message "Indisponible".
*   **Mode Vacances** : Interrupteur manuel prioritaire. Envoie un message "En congés".
*   **Master Switch** : Un interrupteur général "Surveillance Active" permet de désactiver totalement l'application en un clic.

### 3. Support International & Multi-SIM
*   **Frontaliers** : Support natif et configurable des numéros mobiles de : France (+33), Belgique (+32), Luxembourg (+352), Allemagne (+49), Suisse (+41), Espagne (+34).
*   **Double SIM** : Option pour choisir quelle carte SIM (SIM 1, SIM 2 ou Défaut) utiliser pour l'envoi des SMS.

### 4. Intégration Prise de RDV
*   **Lien Dynamique** : Génère automatiquement un lien vers votre page Doctolib (ou autre URL personnalisée).
*   **Modulaire** : Option pour inclure ou exclure ce lien des messages envoyés.

### 5. Suivi & Historique
*   **Historique Visuel** : Liste claire des événements avec icônes (Appels manqués, SMS envoyés, Erreurs).
*   **Export CSV** : Possibilité d'exporter l'historique pour vos archives ou preuves de contact.
*   **Notifications** : Notifie l'utilisateur lorsqu'un SMS automatique a été envoyé avec succès. Une notification persistante indique si la surveillance est active.
*   **Actualités** : Système d'annonces à distance pour informer les utilisateurs des mises à jour.

---

## 🛠 Architecture Technique

L'application est construite en **Kotlin** et suit une architecture robuste basée sur les composants Android standards.

### Composants Principaux

1.  **`CallReceiver` (BroadcastReceiver)** :
    *   Écoute les changements d'état du téléphone (`PHONE_STATE`).
    *   **Logique** : Lorsqu'un appel passe de `RINGING` à `IDLE` (raccroché), il déclenche une vérification asynchrone (`goAsync`).
    *   **Smart Polling** : Tente de lire le `CallLog` jusqu'à 10 fois (toutes les 500ms) pour identifier le dernier appel manqué récent (< 60s). Cela contourne les délais d'écriture système.

2.  **`SmsSender` (Object)** :
    *   Gère l'envoi technique des SMS.
    *   Gère le découpage des messages longs (Multipart).
    *   Gère la sélection de la **Subscription ID** pour le support Double SIM.

3.  **`MainActivity` & `EditMessagesActivity`** :
    *   Interfaces utilisateurs pour le tableau de bord et la configuration.
    *   Utilisent `SharedPreferences` pour stocker les réglages (horaires, messages, whitelist, etc.).

4.  **`NewsChecker` & `RegistrationManager`** :
    *   Gestion de la communication distante (News et Enregistrement utilisateur).
    *   Utilisent des appels HTTP simples vers GitHub Gist et Google Forms.

### Flux de Données (Workflow)

1.  **Appel Entrant** : `CallReceiver` détecte `RINGING`. Mémorise `was_ringing = true`.
2.  **Fin d'Appel** : `CallReceiver` détecte `IDLE`.
3.  **Vérification** : Si `was_ringing` est vrai et `MasterSwitch` est ON :
    *   Lance une coroutine.
    *   Scanne le journal d'appels (`CallLog.Calls`).
    *   Si un appel `MISSED_TYPE` récent est trouvé :
        *   Vérifie si c'est un mobile (selon pays autorisés).
        *   Vérifie la whitelist.
        *   Vérifie le délai anti-spam.
        *   Détermine le message (Vacances > Travail > Off).
        *   Appelle `SmsSender`.
4.  **Envoi** : `SmsSender` envoie le SMS via l'API Android.
5.  **Confirmation** : `SmsResultReceiver` reçoit la confirmation de l'opérateur et déclenche une notification utilisateur + log dans l'historique.

---

## 🔧 Gestion & Maintenance (Admin)

Cette section explique comment interagir avec les utilisateurs de l'application (APK hors-store).

### 1. Envoyer une Annonce / News
L'application vérifie automatiquement un fichier hébergé sur Internet pour afficher des messages aux utilisateurs (Mise à jour dispo, Info importante, etc.).

*   **Outil** : GitHub Gist
*   **Lien d'édition** : [Modifier le fichier news.json](https://gist.github.com/QuentinLACHENAL/329daadcd453e8f5397c565e462e9ed7)
*   **Format** :
    ```json
    {
      "id": 2,                // Incrémentez ce chiffre pour que le message s'affiche !
      "title": "Titre",
      "message": "Votre message ici...",
      "link_url": "https://...",
      "link_label": "Voir"
    }
    ```
*   **Note** : Tant que vous ne changez pas l'ID (ex: passer de 1 à 2), les utilisateurs qui ont déjà vu le message ne le reverront pas.

### 2. Récupérer les Utilisateurs (Base de Données)
Au premier lancement, l'application demande le Nom et l'Email de l'utilisateur. Ces données arrivent dans votre Google Form.

*   **Outil** : Google Forms
*   **Lien du Formulaire** : [Éditer le formulaire](https://docs.google.com/forms/d/1kMxtD_yM5u3d6-hJVyMAoYPyLfTSL_RNpldDQWCDdOA/edit)
*   **Voir les inscrits** : Cliquez sur l'onglet **Réponses** dans le formulaire. Vous pouvez cliquer sur l'icône verte "Sheets" pour créer un tableau Excel automatique.

### 3. Support & Contact
*   **Email Développeur** : `contact@osteolink.fr`
*   **Bug** : Les utilisateurs ont un bouton "Signaler un bug" dans l'application qui envoie un mail à cette adresse.

---

## 🔒 Confidentialité & Permissions

L'application fonctionne **100% en local**. Aucune donnée n'est envoyée vers un serveur tiers (sauf l'enregistrement volontaire au démarrage vers Google Forms).

*   **READ_CALL_LOG** : Indispensable pour détecter *qui* a appelé et si c'est un appel *manqué*.
*   **SEND_SMS** : Pour envoyer la réponse automatique.
*   **READ_CONTACTS** : (Optionnel) Pour ne pas répondre aux numéros déjà enregistrés dans votre répertoire (si l'option est cochée).
*   **READ_PHONE_STATE** : Pour détecter quand le téléphone sonne et gérer la Double SIM.
*   **INTERNET** : Uniquement pour récupérer les "News" (Gist) et envoyer l'inscription (Google Forms).

---

## 📦 Compilation

L'application utilise Gradle.

```bash
# Compiler l'APK de debug
./gradlew assembleDebug

# Compiler l'APK de release
./gradlew assembleRelease
```