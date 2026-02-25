package fr.doranco.nexilock.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/* 
Gère la connexion et la structure de la base de données SQlite 
*/
public class DatabaseManager {

    // Chemin vers le fichier de la base de données
    private static final String URL = "jdbc:sqlite:coffre.db";

    /* 
    Initialise la base de données et crée les tables si elles n'existent pas 
    */
    public static void initDatabase(){
        // SQL pour créer la table des comptes
        String sqlMaster = "CREATE TABLE IF NOT EXISTS master (" +
                        "id INTEGER PRIMARY KEY, " +
                        "salt TEXT NOT NULL, " +
                        "hash TEXT NOT NULL);";
        
        // SQL pour le stockage des mots de passe
        // id : clé unique | service : nom du site | username : identifiant | password_enc : mot de passe hashé
        String sqlAccounts = "CREATE TABLE IF NOT EXISTS accounts ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "service TEXT NOT NULL,"
                            + "username TEXT NOT NULL,"
                            + "password_enc BLOB NOT NULL" // BLOB pour les données hashé
                            + ");";

        // Connexion à la base et exécution de la requête
        try (Connection conn = DriverManager.getConnection(URL)){
            Statement stmt = conn.createStatement();

            stmt.execute(sqlMaster);
            stmt.execute(sqlAccounts);

            System.out.println("[BDD] Initialisation réussie : tables 'master' et 'accounts' prêtes.");
            
        } catch (Exception e){
            System.err.println("[Erreur BDD] Impossible d'initialiser la table : " + e.getMessage());
        }
    }
}
