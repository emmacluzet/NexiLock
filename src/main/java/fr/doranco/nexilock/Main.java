package fr.doranco.nexilock;

import fr.doranco.nexilock.data.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée de NexiLock.
 * Étend Application pour initialiser le runtime JavaFX correctement.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseManager.initDatabase();

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/LoginView.fxml")
            );
            Scene scene = new Scene(loader.load(), 480, 320);
            scene.getStylesheets().add(
                getClass().getResource("/css/nexilock.css").toExternalForm()
            );

            primaryStage.setTitle("NexiLock \uD83D\uDEE1\uFE0F");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("[FATAL] Impossible de démarrer NexiLock : " + e.getMessage());
            javafx.application.Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
