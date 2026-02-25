# Politique de Sécurité - NexiLock
Cette politique définit les standards de sécurité et les procédures de gestion des vulnérabilités pour le projet NexiLock.

## Versions supportées (Supported Versions)
Nous ne maintenons que les versions majeures les plus récentes pour garantir une sécurité maximale.

## Classification des données (Data Handling)
Nous classons les informations manipulées par l'application:
- Niveau 1 : Public (Nom de l'application, icônes)
- Niveau 2 :Interne (Noms des services enregistrés par l'utilisateur, identifiants).
- Niveau 3 : Critique (Mots de passe des services, Master Key, Recovery Key).
  - *Règle : Le niveau 3 ne doit jamais être écrit dans les logs (`System.out.println`) ou rester en mémoire vive (`RAM`) plus de 30 secondes.*

## Principes de Développement (Architecture)
Tout développement doit respecter les piliers suivants :
- Zéro Secret en Clair : Aucun mot de passe, clé de chiffrement ou jeton ne doit être écrit en clair dans le code, les logs ou la base de données SQLite.
- Principe du moindre privilège : L'application n'utilise que les droits nécessaires (Accès utilisateur standard, pas d'élévation Admin sauf pour l'installation).
- Isolation Mémoire : Les variables contenant des secrets (mots de passe Windows, Master Key) doivent être purgées de la mémoire dès que l'opération est terminée.

## Standards cryptographiques
Nous utilisons exclusivement des algorithmes reconnus par l'ANSSI :
- Chiffrement des données : AES-256-GCM (pour l'intégrité et la confidentialité).
- Dérivation de clé (KDF) : PBKDF2 avec HMAC-SHA256 et un minimum de 100 000 itérations.
- Gestion des sels : Un sel unique et aléatoire de 128 bits minimum par installation.

## Sécurité de la base de donnée
- Intégrité : Chaque entrée dans la table `accounts` doit être accompagnée d'un tag d'authentification (via le mode AES-CGM) pour détecter toute modification manuelle du fichier `.db`.
- Sanitisation : Utilisation systématique de `PreparedStatements` pour éviter toute injection SQL, même si la base est locale.

## Cycle de vie des clés (Key Management)
- Génération : Utilisation de `SecureRandom` pour toute création de salt ou de clé
- Rotation : Si l'utilisateur change sa Recovery Key, l'ancienne doit être immédiatement invalidée et écrasée physiquement sur le disque.

## Gestion des dépendances
- Audit : Toute nouvelle bibliothèque (Maven) doit être validée avant ajout au `pom.xml`.
- Mises à jour : Les dépendances JNA, SQLite et JavaFX doivent être maintenues à jourr pour corriger les failles connues (CVE).

## Politique de "commits" et git
- Interdiction de push des secrets : il est strictement interdit de "commit" des fichiers `.db` de test ou des fichiers de configuration personnels.
- Revue de code : Validation de chaque Pull Request, particulièrement sur les modules de chiffrement.

## Signalement de Vulnérabilités
Si un membre de l'équipe découvre une faille :
1. Ne pas ouvrir d'Issue publique
2. Alerter immédiatement la Lead Dev en privée
3. Une correction doit être priorisée dans le Sprint actuel (Patch d'urgence).
