package fr.doranco.nexilock.data;

import fr.doranco.nexilock.service.CryptoService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Couche d'accès aux données pour la table accounts.
 * Toutes les requêtes utilisent des PreparedStatements (protection contre l'injection SQL).
 * Les mots de passe sont chiffrés/déchiffrés via CryptoService avant tout stockage.
 */
public class AccountDAO {

    private final CryptoService crypto;

    public AccountDAO(CryptoService crypto) {
        this.crypto = crypto;
    }

    /** Insère un nouveau compte dans la base. */
    public void insert(Account account) throws Exception {
        String sql = "INSERT INTO accounts (service, username, password_enc) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            byte[] encrypted = crypto.encrypt(account.getPassword());
            ps.setString(1, account.getService());
            ps.setString(2, account.getUsername());
            ps.setBytes(3, encrypted);
            ps.executeUpdate();

            // Récupère l'id auto-généré
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) account.setId(rs.getInt(1));
            }
        }
    }

    /** Retourne tous les comptes déchiffrés. */
    public List<Account> findAll() throws Exception {
        String sql = "SELECT id, service, username, password_enc FROM accounts ORDER BY service";
        List<Account> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Account a = new Account();
                a.setId(rs.getInt("id"));
                a.setService(rs.getString("service"));
                a.setUsername(rs.getString("username"));
                a.setPassword(crypto.decrypt(rs.getBytes("password_enc")));
                list.add(a);
            }
        }
        return list;
    }

    /** Met à jour un compte existant (identifié par son id). */
    public void update(Account account) throws Exception {
        String sql = "UPDATE accounts SET service=?, username=?, password_enc=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            byte[] encrypted = crypto.encrypt(account.getPassword());
            ps.setString(1, account.getService());
            ps.setString(2, account.getUsername());
            ps.setBytes(3, encrypted);
            ps.setInt(4, account.getId());
            ps.executeUpdate();
        }
    }

    /** Supprime un compte par son id. */
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM accounts WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Recherche les comptes dont le service ou le nom d'utilisateur contient le terme. */
    public List<Account> search(String term) throws Exception {
        String sql = "SELECT id, service, username, password_enc FROM accounts " +
                     "WHERE LOWER(service) LIKE ? OR LOWER(username) LIKE ? ORDER BY service";
        List<Account> list = new ArrayList<>();
        String pattern = "%" + term.toLowerCase() + "%";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Account a = new Account();
                    a.setId(rs.getInt("id"));
                    a.setService(rs.getString("service"));
                    a.setUsername(rs.getString("username"));
                    a.setPassword(crypto.decrypt(rs.getBytes("password_enc")));
                    list.add(a);
                }
            }
        }
        return list;
    }
}
