package fr.doranco.nexilock.data;

/**
 * Représente un compte (identifiant) stocké dans le coffre.
 */
public class Account {

    private int id;
    private String service;
    private String username;
    private String password; // mot de passe en clair, uniquement en mémoire

    public Account() {}

    public Account(String service, String username, String password) {
        this.service  = service;
        this.username = username;
        this.password = password;
    }

    public int getId() { return id; }
    public String getService() { return service; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public void setId(int id) { this.id = id; }
    public void setService(String s) { this.service = s; }
    public void setUsername(String u){ this.username = u; }
    public void setPassword(String p) { this.password = p; }

    @Override
    public String toString() {
        return "Account{id=" + id + ", service='" + service + "', username='" + username + "'}";
    }
}
