package fr.doranco.nexilock;

import fr.doranco.nexilock.data.DatabaseManager;

public class Main {
    public static void main(String[] args) {
        System.out.println("NEXILOCK : Démarrage du système");
        DatabaseManager.initDatabase();
        System.out.println("Système prêt et sécurisé");
    }
}
