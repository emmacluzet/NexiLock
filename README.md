# NexiLock 🛡️
**NexiLock** est une application desktop sécurisée permettant de stocker, consulter et gérer vos identifiants de connexion dans un coffre-forrt numérique local et personne.

## 📋Présentation du projet
Dans un contexte où la réutilisation de mots de passe constitue un risque majeur, NexiLock propose une solution locale, gratuite, simple et sécurisée, sans synchronisation cloud ni abonnement.

### Fonctionnalités clés
* **Gestion complète CRUD** : Centralisation, consultation, modification et suppression des identifiants.
* **Sécurité technique** : Accès protégé par mot de passe maître (hash SHA-256) et chiffrement des données en AES-256-GCM.
* **Générateur robuste** : Création de mots de passe complexes et aléatoires (8 à 32 caractères).
* **Interface intuitive** : Expérience utilisateur moderne basée sur un système de "cartes" JavaFX.
* **Fonctionnement hors-ligne** : Aucune donnée ne quitte l'ordinateur de l'utilisateur.
## 🛠️ Spécifications Techniques
L'application repose sur une architecture en couches (UI / Business / Data) :
* **Langage** : Java 21 (LTS).
* **Interface** : JavaFX 21 + FXML & CSS.
* **Base de données** : SQLite 3.45+ via JDBC.
* **Sécurité** : PBKDF2 avec HmacSHA256 (100 000 itérations) pour la dérivation de clé.
* **Build tool** : Maven 3.9.

## 👥 Équipe de développement (CFA Doranco)
Projet réalisé en méthodologie Agile (Scrum simplifié) par :
* **Ethan MEIDECK** : Scrum Master & Développeur (Interface utilisateur et Intégration).
* **Ruben DESIR** : Développeur (Sécurité et Persistance des données).
* **Emma CLUZET** : Responsable du cahier des charges (Documentation et Tests).

## 📅 Planification des Sprints
Le projet est découpé en 4 sprints d'une semaine :
1. **Sprint 1** : Projet Maven, base SQLite, authentification et couche crypto.
2. **Sprint 2** : Implémentation du DAO et du CRUD complet.
3. **Sprint 3** : Interface JavaFX, générateur de mots de passe et système de cartes.
4. **Sprint 4** : Tests unitaires (JUnit 5), ergonomie et packaging final.

## 🚀 Installation & Lancement
### Prérequis
* Java JDK 21.
* Maven 3.9.

### Commandes
1. Cloner le projet :
   ```bash
   git clone [https://github.com/emmacluzet/NexiLock.gi]
2. Compiler et exécuter :
   ```bash
   mvn clean javafx:run

Projet réalisé au CFA Doranco Espace Multimédia (75010 Paris) - Février 2026
