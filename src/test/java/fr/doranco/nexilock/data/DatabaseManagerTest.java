package fr.doranco.nexilock.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/* Tests unitaires pour vérifier le bon fonctionnement de la base de données */
public class DatabaseManagerTest {

    private static final String URL = "jdbc:sqlite:coffre.db";

    @BeforeAll
    static void setup() {
        // Préparation de l'environnement de test
        DatabaseManager.initDatabase();
    }

    @Test
    void testFileExists() {
        // Vérifie que le fichier physique 'coffre.db' a bien été créé
        File dbFile = new File("coffre.db");
        assertTrue(dbFile.exists(), "Le fichier coffre.db devrait être créé à la racine.");
    }

    @Test
    void testTablesExist() {
        // Vérifie que les tables 'master' et 'accounts' sont présentes
        try (Connection conn = DriverManager.getConnection(URL)) {
            DatabaseMetaData meta = conn.getMetaData();

            // Vérification de la table 'accounts'
            try (ResultSet rsAccounts = meta.getTables(null, null, "accounts", null)) {
                assertTrue(rsAccounts.next(), "La table 'accounts' n'existe pas en base.");
            }

            // Vérification de la table 'master'
            try (ResultSet rsMaster = meta.getTables(null, null, "master", null)) {
                assertTrue(rsMaster.next(), "La table 'master' n'existe pas en base.");
            }

        } catch (Exception e) {
            fail("Erreur lors de l'accès aux métadonnées de la base : " + e.getMessage());
        }
    }
}