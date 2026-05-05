package fr.doranco.nexilock.ui.controller;

import fr.doranco.nexilock.data.Account;
import fr.doranco.nexilock.data.AccountDAO;
import fr.doranco.nexilock.service.CryptoService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Contrôleur du tableau de bord NexiLock.
 * Gère l'affichage en cartes et les opérations CRUD.
 */
public class DashboardController {

    @FXML private TextField tfSearch;
    @FXML private ListView<Account> listAccounts;
    @FXML private TextField tfService;
    @FXML private TextField tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private TextField tfPasswordVisible;
    @FXML private Slider sliderLength;
    @FXML private Label lblLength;
    @FXML private Button btnSave;
    @FXML private Button btnDelete;
    @FXML private Button btnNew;
    @FXML private Label lblStatus;

    private AccountDAO dao;
    private Account selected;
    private boolean showPassword = false;

    private final ObservableList<Account> items = FXCollections.observableArrayList();

    /** Appelé depuis LoginController après déverrouillage du coffre. */
    public void init(CryptoService crypto) {
        this.dao = new AccountDAO(crypto);
        listAccounts.setItems(items);
        listAccounts.setCellFactory(lv -> new AccountCell());

        listAccounts.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, account) -> populateForm(account)
        );

        tfSearch.textProperty().addListener((obs, old, val) -> search(val));

        sliderLength.valueProperty().addListener((obs, old, val) ->
            lblLength.setText(String.valueOf(val.intValue()))
        );
        sliderLength.setValue(16);

        // Liaison champ visible ↔ champ masqué
        pfPassword.textProperty().addListener((obs, old, val) -> {
            if (!showPassword) tfPasswordVisible.setText(val);
        });
        tfPasswordVisible.textProperty().addListener((obs, old, val) -> {
            if (showPassword) pfPassword.setText(val);
        });
        tfPasswordVisible.setVisible(false);
        tfPasswordVisible.setManaged(false);

        loadAll();
        clearForm();
    }

    @FXML
    private void handleNew() {
        listAccounts.getSelectionModel().clearSelection();
        clearForm();
        tfService.requestFocus();
    }

    @FXML
    private void handleSave() {
        String service = tfService.getText().trim();
        String username = tfUsername.getText().trim();
        String password = showPassword ? tfPasswordVisible.getText() : pfPassword.getText();

        if (service.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showStatus("Tous les champs sont obligatoires.", false);
            return;
        }

        new Thread(() -> {
            try {
                if (selected == null) {
                    Account a = new Account(service, username, password);
                    dao.insert(a);
                    Platform.runLater(() -> {
                        items.add(a);
                        showStatus("Compte créé.", true);
                        clearForm();
                    });
                } else {
                    selected.setService(service);
                    selected.setUsername(username);
                    selected.setPassword(password);
                    dao.update(selected);
                    Platform.runLater(() -> {
                        listAccounts.refresh();
                        showStatus("Compte mis à jour.", true);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Erreur : " + e.getMessage(), false));
            }
        }).start();
    }

    @FXML
    private void handleDelete() {
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer le compte « " + selected.getService() + " » ?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Account toDelete = selected;
            new Thread(() -> {
                try {
                    dao.delete(toDelete.getId());
                    Platform.runLater(() -> {
                        items.remove(toDelete);
                        clearForm();
                        showStatus("Compte supprimé.", true);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showStatus("Erreur : " + e.getMessage(), false));
                }
            }).start();
        }
    }

    @FXML
    private void handleGeneratePassword() {
        int length = (int) sliderLength.getValue();
        String pwd = CryptoService.generatePassword(length);
        if (showPassword) {
            tfPasswordVisible.setText(pwd);
        } else {
            pfPassword.setText(pwd);
        }
    }

    @FXML
    private void handleTogglePassword() {
        showPassword = !showPassword;
        if (showPassword) {
            tfPasswordVisible.setText(pfPassword.getText());
            pfPassword.setVisible(false);
            pfPassword.setManaged(false);
            tfPasswordVisible.setVisible(true);
            tfPasswordVisible.setManaged(true);
        } else {
            pfPassword.setText(tfPasswordVisible.getText());
            tfPasswordVisible.setVisible(false);
            tfPasswordVisible.setManaged(false);
            pfPassword.setVisible(true);
            pfPassword.setManaged(true);
        }
    }

    private void search(String term) {
        if (term == null || term.isBlank()) {
            loadAll();
            return;
        }
        new Thread(() -> {
            try {
                List<Account> results = dao.search(term);
                Platform.runLater(() -> { items.setAll(results); });
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Erreur recherche : " + e.getMessage(), false));
            }
        }).start();
    }

    private void loadAll() {
        new Thread(() -> {
            try {
                List<Account> all = dao.findAll();
                Platform.runLater(() -> items.setAll(all));
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Erreur chargement : " + e.getMessage(), false));
            }
        }).start();
    }

    private void populateForm(Account account) {
        if (account == null) { clearForm(); return; }
        selected = account;
        tfService.setText(account.getService());
        tfUsername.setText(account.getUsername());
        pfPassword.setText(account.getPassword());
        tfPasswordVisible.setText(account.getPassword());
        btnDelete.setDisable(false);
        btnSave.setText("Modifier");
    }

    private void clearForm() {
        selected = null;
        tfService.clear();
        tfUsername.clear();
        pfPassword.clear();
        tfPasswordVisible.clear();
        btnDelete.setDisable(true);
        btnSave.setText("Enregistrer");
        lblStatus.setText("");
    }

    private void showStatus(String msg, boolean ok) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: " + (ok ? "#1E8449" : "#C0392B") + ";");
    }

    // Cellule personnalisée pour la liste des comptes (style "carte")
    private static class AccountCell extends ListCell<Account> {
        @Override
        protected void updateItem(Account account, boolean empty) {
            super.updateItem(account, empty);
            if (empty || account == null) {
                setGraphic(null);
                return;
            }
            Label lblService = new Label(account.getService());
            Label lblUsername = new Label(account.getUsername());
            lblService.getStyleClass().add("card-service");
            lblUsername.getStyleClass().add("card-username");
            VBox card = new VBox(4, lblService, lblUsername);
            card.getStyleClass().add("account-card");
            setGraphic(card);
        }
    }
}
