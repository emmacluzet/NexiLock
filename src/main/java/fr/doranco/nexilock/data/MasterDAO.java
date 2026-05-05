package fr.doranco.nexilock.data;

import java.sql.*;
import java.util.Base64;

/**
 * Accès à la table master : sel PBKDF2, hash de vérification
 * et Master Key wrappée (AES-256-GCM).
 *
 * La table master ne contient qu'une seule ligne (id = 1).
 */
public class MasterDAO {

    /** Vérifie si le coffre a déjà été initialisé (table master non vide). */
    public static boolean isVaultInitialized() throws Exception {
        String sql = "SELECT COUNT(*) FROM master";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /**
     * Persiste les données d'initialisation du coffre (premier lancement uniquement).
     *
     * @param saltB64 sel encodé Base64
     * @param hashB64 hash de vérification encodé Base64
     * @param wrappedMk Master Key wrappée (bytes)
     * @param wrappedMkRk Master Key wrappée avec Recovery Key (bytes)
     * @param recoveryKeyEnc Recovery Key chiffrée (pour l'affichage unique)
     */
    public static void insertVaultInit(String saltB64, String hashB64,byte[] wrappedMk, byte[] wrappedMkRk) throws Exception {
        // Ajout des colonnes wrapped_mk et wrapped_mk_rk si elles n'existent pas encore
        ensureColumns();

        String sql = "INSERT INTO master (id, salt, hash, wrapped_mk, wrapped_mk_rk) " +
                     "VALUES (1, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, saltB64);
            ps.setString(2, hashB64);
            ps.setBytes(3, wrappedMk);
            ps.setBytes(4, wrappedMkRk);
            ps.executeUpdate();
        }
    }

    /** Lit les données nécessaires au déverrouillage. */
    public static VaultCredentials loadCredentials() throws Exception {
        ensureColumns();
        String sql = "SELECT salt, hash, wrapped_mk, wrapped_mk_rk FROM master WHERE id = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) throw new IllegalStateException("Coffre non initialisé.");
            return new VaultCredentials(
                rs.getString("salt"),
                rs.getString("hash"),
                rs.getBytes("wrapped_mk"),
                rs.getBytes("wrapped_mk_rk")
            );
        }
    }

    /** Ajoute les colonnes wrapped_mk et wrapped_mk_rk si elles n'existent pas (migration). */
    private static void ensureColumns() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             Statement s = conn.createStatement()) {
            s.execute("ALTER TABLE master ADD COLUMN wrapped_mk  BLOB");
        } catch (SQLException ignored) {} // colonne déjà présente
        try (Connection conn = DatabaseManager.getConnection();
             Statement s = conn.createStatement()) {
            s.execute("ALTER TABLE master ADD COLUMN wrapped_mk_rk BLOB");
        } catch (SQLException ignored) {}
    }

    // DTO
    public static class VaultCredentials {
        public final String saltB64;
        public final String hashB64;
        public final byte[] wrappedMk;
        public final byte[] wrappedMkRk;

        VaultCredentials(String saltB64, String hashB64, byte[] wrappedMk, byte[] wrappedMkRk) {
            this.saltB64 = saltB64;
            this.hashB64 = hashB64;
            this.wrappedMk = wrappedMk;
            this.wrappedMkRk = wrappedMkRk;
        }
    }
}
