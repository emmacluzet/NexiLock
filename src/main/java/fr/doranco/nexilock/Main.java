package fr.doranco.nexilock;

import fr.doranco.nexilock.data.DatabaseManager;

public class Main {
    public static void main(String[] args) {
        System.out.println("NEXILOCK : Démarrage du système");
        DatabaseManager.initDatabase();
        System.out.println("Système prêt et sécurisé");
    }

    public static void loginUser(String username, String password) {
        // Creating AuthService instance.
        AuthService auth = new AuthService();
        int attempt = 0;
 
        // User login & password verification with 3 attempts.
        while (attempt < 3)
            if(auth.authenticate(username, password)){
                System.out.println("Accès autorisé !");
                break;
            }
            // Verification for empty inputs
            else if(username.isBlank() || password.isBlank()) {
                System.out.println("Veuillez entrez un identifiant et mot de passe valide !");
                attempt = attempt + 1;
            }
            else{
                System.out.println("Accès refusé, veuillez réesayer.");
                attempt = attempt + 1;
        }
    }
}