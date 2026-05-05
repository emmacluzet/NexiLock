package fr.doranco.nexilock.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Gère la connexion et la structure de la base de données SQLite.
 * Le fichier coffre.db est créé automatiquement au premier lancement.
 */
public class DatabaseManager {

    /** URL JDBC de la base locale. Package-private pour les tests. */
    static String DB_URL = "jdbc:sqlite:coffre.db";

    /**
     * Initialise la base de données et crée les tables si elles n'existent pas.
     *
     * @throws RuntimeException si la base ne peut pas être initialisée
     */
    public static void initDatabase() {
        String sqlMaster = "CREATE TABLE IF NOT EXISTS master (" + "  id   INTEGER PRIMARY KEY, " + "  salt TEXT    NOT NULL, " + "  hash TEXT    NOT NULL" + ");";

        // password_enc : ciphertext AES-256-GCM (BLOB réversible, pas un hash)
        String sqlAccounts = "CREATE TABLE IF NOT EXISTS accounts (" + "  id INTEGER PRIMARY KEY AUTOINCREMENT, " + "  service TEXT NOT NULL, " + "  username TEXT NOT NULL, " + "  password_enc BLOB NOT NULL" + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement  stmt = conn.createStatement()) {

            stmt.execute(sqlMaster);
            stmt.execute(sqlAccounts);
            System.out.println("[BDD] Tables 'master' et 'accounts' prêtes.");

        } catch (Exception e) {
            System.err.println("[Erreur BDD] Initialisation impossible : " + e.getMessage());
            throw new RuntimeException("Impossible d'initialiser la base de données.", e);
        }
    }

    /** Retourne une nouvelle connexion à la base. À fermer par l'appelant. */
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }
}
