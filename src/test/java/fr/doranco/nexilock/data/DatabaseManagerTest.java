package fr.doranco.nexilock.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/* Tests unitiares pour vérifier le bon fonctionnement de la base de données */
public class DatabaseManagerTest {

    @BeforeAll
    static void setup() {
        // préparatation fr l'environnement de test
        DatabaseManager.initDatabase();
    }

    @Test
    void testFileExists() {
        // Vérifie que le fichier physique 'coffre.db' a bien été créé
        File dbFile = new File("coffre.db");
        assertTrue(dbFile.exists(), "Le fichier coffre.db devrait être créé à la racine.");
    }

    @Test
    void testTableExists() {
        // vérifie que la table 'accounts' est bien présente à l'intérieur de la base
        String url = "jdbc:sqlite:coffre.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, "accounts", null);
            
            assertTrue(rs.next(), "La table'accounts' n'existe pas en base.");
        } catch (Exception e) {
            fail("Erreur lors de l'accès aux tables : " + e.getMessage());
        }
    }
}
