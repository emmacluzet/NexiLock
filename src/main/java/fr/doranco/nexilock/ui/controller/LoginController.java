package fr.doranco.nexilock.ui.controller;

import fr.doranco.nexilock.data.MasterDAO;
import fr.doranco.nexilock.data.MasterDAO.VaultCredentials;
import fr.doranco.nexilock.service.AuthService;
import fr.doranco.nexilock.service.AuthService.AuthResult;
import fr.doranco.nexilock.service.CryptoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

/**
 * Contrôleur de LoginView.fxml.
 *
 * Flux :
 *  1. Affiche le nom d'utilisateur Windows (non modifiable).
 *  2. Tente l'authentification via AuthService (JNA / LogonUser).
 *  3. Si succès → déverrouille le coffre via CryptoService.
 *  4. Si échec système → bascule vers la saisie de Recovery Key.
 *  5. Si premier lancement → initialise le coffre et affiche la Recovery Key.
 */
public class LoginController {

    @FXML private Label lblUsername;
    @FXML private PasswordField pfPassword;
    @FXML private TextField tfRecoveryKey;
    @FXML private Button btnUnlock;
    @FXML private Label lblError;
    @FXML private Label lblRecoveryHint;

    private boolean recoveryMode = false;

    @FXML
    public void initialize() {
        lblUsername.setText(AuthService.getCurrentWindowsUsername());

        // Touche Entrée → valider
        pfPassword.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleUnlock();
        });
        tfRecoveryKey.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleUnlock();
        });

        tfRecoveryKey.setVisible(false);
        tfRecoveryKey.setManaged(false);
        lblRecoveryHint.setVisible(false);
        lblRecoveryHint.setManaged(false);
        lblError.setText("");
    }

    @FXML
    private void handleUnlock() {
        lblError.setText("");
        String password = pfPassword.getText();
        if (password.isEmpty() && !recoveryMode) {
            lblError.setText("Veuillez saisir votre mot de passe.");
            return;
        }

        btnUnlock.setDisable(true);
        btnUnlock.setText("Vérification…");

        // Traitement hors fil JavaFX pour ne pas geler l'UI pendant PBKDF2
        new Thread(() -> {
            try {
                boolean vaultExists = MasterDAO.isVaultInitialized();

                if (!vaultExists) {
                    // Premier lancement : initialiser le coffre
                    initNewVault(password.toCharArray());
                    return;
                }

                VaultCredentials creds = MasterDAO.loadCredentials();

                if (recoveryMode) {
                    String rk = tfRecoveryKey.getText().trim();
                    CryptoService crypto = CryptoService.unlockWithRecoveryKey(
                        rk, creds.saltB64, creds.wrappedMkRk
                    );
                    if (crypto != null) {
                        openDashboard(crypto);
                    } else {
                        Platform.runLater(() -> showError("Clé de secours incorrecte."));
                    }
                    return;
                }

                AuthResult result = AuthService.authenticate(
                    lblUsername.getText(), password.toCharArray()
                );

                switch (result) {
                    case SUCCESS -> {
                        CryptoService crypto = CryptoService.unlock(
                            password.toCharArray(), creds.saltB64, creds.wrappedMk
                        );
                        if (crypto != null) {
                            openDashboard(crypto);
                        } else {
                            Platform.runLater(() -> showError("Erreur de déchiffrement."));
                        }
                    }
                    case WRONG_PASSWORD ->
                        Platform.runLater(() -> showError("Mot de passe incorrect."));
                    case NEED_RECOVERY_KEY ->
                        Platform.runLater(this::switchToRecoveryMode);
                }

            } catch (Exception ex) {
                Platform.runLater(() -> showError("Erreur : " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    btnUnlock.setDisable(false);
                    btnUnlock.setText("Déverrouiller");
                });
            }
        }).start();
    }

    private void initNewVault(char[] password) throws Exception {
        CryptoService.InitResult init = CryptoService.initNewVault(password);
        String recoveryKey = CryptoService.generateRecoveryKey();

        // Wrapping de la Master Key avec la Recovery Key également
<<<<<<< HEAD
        CryptoService.InitResult initRk = CryptoService.initNewVault(recoveryKey.toCharArray());
        MasterDAO.insertVaultInit(
            init.saltB64, init.hashB64,
            init.wrappedMasterKey,
            initRk.wrappedMasterKey
        );
=======
        CryptoService.InitResult initRk = CryptoService.initNewVault(recoveryKey.toCharArray()); 
        MasterDAO.insertVaultInit(init.saltB64, init.hashB64,init.wrappedMasterKey,initRk.wrappedMasterKey);
>>>>>>> 799ebd9507c15fb59d28b8f38f4bc373ca02c772

        Platform.runLater(() -> showRecoveryKeyDialog(recoveryKey, init.crypto));
    }

    private void showRecoveryKeyDialog(String recoveryKey, CryptoService crypto) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coffre initialisé — NexiLock");
        alert.setHeaderText("Votre clé de secours");
        alert.setContentText(
            "Notez cette clé en lieu sûr.\n" +
            "Elle vous permettra d'accéder au coffre si votre\n" +
            "mot de passe Windows change.\n\n" +
            "Clé : " + recoveryKey + "\n\n" +
            "⚠ Cette clé ne sera plus jamais affichée."
        );
        alert.showAndWait();
        openDashboard(crypto);
    }

    private void switchToRecoveryMode() {
        recoveryMode = true;
        pfPassword.setVisible(false);
        pfPassword.setManaged(false);
        tfRecoveryKey.setVisible(true);
        tfRecoveryKey.setManaged(true);
        lblRecoveryHint.setVisible(true);
        lblRecoveryHint.setManaged(true);
        lblRecoveryHint.setText(
            "Authentification Windows indisponible.\n" +
            "Saisissez votre clé de secours (format XXXXXX-XXXXXX-…)."
        );
        tfRecoveryKey.requestFocus();
    }

    private void openDashboard(CryptoService crypto) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/DashboardView.fxml")
                );
                Stage stage = (Stage) btnUnlock.getScene().getWindow();
                Scene scene = new Scene(loader.load(), 900, 600);
                scene.getStylesheets().add(
                    getClass().getResource("/css/nexilock.css").toExternalForm()
                );
                DashboardController ctrl = loader.getController();
                ctrl.init(crypto);
                stage.setScene(scene);
                stage.setResizable(true);
                stage.setMinWidth(700);
                stage.setMinHeight(450);
            } catch (Exception e) {
                showError("Impossible d'ouvrir le tableau de bord : " + e.getMessage());
            }
        });
    }

    private void showError(String msg) {
        lblError.setText(msg);
    }
}
