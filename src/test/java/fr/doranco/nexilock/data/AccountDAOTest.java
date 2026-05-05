package fr.doranco.nexilock.data;

public package fr.doranco.nexilock.data;

import fr.doranco.nexilock.service.CryptoService;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

/**
 * Tests d'intégration du DAO : vérifie le CRUD complet
 * avec chiffrement/déchiffrement réel sur une base isolée.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountDAOTest {

    private static AccountDAO dao;
    private static File tmpDb;
    private static int insertedId;

    @BeforeAll
    static void setup() throws Exception {
        tmpDb = File.createTempFile("nexilock_dao_test_", ".db");
        tmpDb.deleteOnExit();
        DatabaseManager.DB_URL = "jdbc:sqlite:" + tmpDb.getAbsolutePath();
        DatabaseManager.initDatabase();

        CryptoService crypto = CryptoService.initNewVault("TestDAO@2026".toCharArray()).crypto;
        dao = new AccountDAO(crypto);
    }

    @AfterAll
    static void teardown() {
        DatabaseManager.DB_URL = "jdbc:sqlite:coffre.db";
        if (tmpDb != null && tmpDb.exists()) tmpDb.delete();
    }

    @Test
    @Order(1)
    void testInsert() throws Exception {
        Account a = new Account("GitHub", "dev@example.com", "GhPass@123");
        dao.insert(a);
        assertTrue(a.getId() > 0, "L'id doit être auto-généré après insertion.");
        insertedId = a.getId();
    }

    @Test
    @Order(2)
    void testFindAll() throws Exception {
        List<Account> all = dao.findAll();
        assertEquals(1, all.size(), "findAll() doit retourner 1 compte.");
        Account found = all.get(0);
        assertEquals("GitHub", found.getService());
        assertEquals("dev@example.com", found.getUsername());
        assertEquals("GhPass@123", found.getPassword(),
            "Le mot de passe déchiffré doit correspondre à l'original.");
    }

    @Test
    @Order(3)
    void testPasswordIsStoredEncrypted() throws Exception {
        // Vérification directe en base : le BLOB ne doit pas contenir le mot de passe en clair
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement("SELECT password_enc FROM accounts WHERE id=?")) {
            ps.setInt(1, insertedId);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                byte[] blob = rs.getBytes("password_enc");
                String blobStr = new String(blob);
                assertFalse(blobStr.contains("GhPass@123"),
                    "Le mot de passe ne doit jamais apparaître en clair dans la base.");
            }
        }
    }

    @Test
    @Order(4)
    void testUpdate() throws Exception {
        Account a = dao.findAll().get(0);
        a.setPassword("NouveauMdp!456");
        a.setService("GitHub Enterprise");
        dao.update(a);

        List<Account> updated = dao.findAll();
        assertEquals("GitHub Enterprise", updated.get(0).getService());
        assertEquals("NouveauMdp!456", updated.get(0).getPassword());
    }

    @Test
    @Order(5)
    void testSearch() throws Exception {
        dao.insert(new Account("Gmail", "user@gmail.com", "GmailPwd!1"));
        dao.insert(new Account("GitLab", "dev@gitlab.com", "GitLabPwd!2"));

        List<Account> results = dao.search("git");
        assertEquals(2, results.size(), "Recherche 'git' doit trouver GitHub Enterprise et GitLab.");
    }

    @Test
    @Order(6)
    void testDelete() throws Exception {
        int countBefore = dao.findAll().size();
        dao.delete(insertedId);
        int countAfter = dao.findAll().size();
        assertEquals(countBefore - 1, countAfter, "findAll() doit retourner un compte de moins après suppression.");
    }
}
 {
    
}
