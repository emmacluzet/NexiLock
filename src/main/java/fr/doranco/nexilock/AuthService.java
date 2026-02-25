package fr.doranco.nexilock;

public class AuthService {
// TODO: Importer le hash de la BDD
// TODO: Comparer et vérifier le hash

    public boolean authenticate(String username, String password){
        boolean result;
        // Credential verification.
        if (username == "USERNAME" && password == "HASH") {
            result = true;
            return result;
        }
        else {
            result = false;
            return result;
        }
    }
}
