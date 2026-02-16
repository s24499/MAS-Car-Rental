package pl.pjatk.mas.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.pjatk.mas.service.AuthService;

/**
 * Ekran zmiany hasła na podstawie loginu.
 */
public class EkranResetHasla {

    /**
     * Wyświetla okno do podania loginu i nowego hasła.
     */
    public void pokaz(Stage stage) {
        BorderPane root = new BorderPane();

        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.TOP_LEFT);

        Label tytul = new Label("Resetowanie hasła");

        Label lblLogin = new Label("Login");
        TextField txtLogin = new TextField();

        Label lblNoweHaslo = new Label("Nowe hasło");
        PasswordField txtNoweHaslo = new PasswordField();

        Label lblNoweHaslo2 = new Label("Powtórz nowe hasło");
        PasswordField txtNoweHaslo2 = new PasswordField();

        Button btnZresetuj = new Button("Zresetuj hasło");
        Button btnAnuluj = new Button("Anuluj");

        // Obsługa resetowania hasła
        btnZresetuj.setOnAction(e -> {
            String login = txtLogin.getText().trim();
            String noweHaslo = txtNoweHaslo.getText();
            String noweHaslo2 = txtNoweHaslo2.getText();

            if (login.isEmpty() || noweHaslo.isEmpty() || noweHaslo2.isEmpty()) {
                pokazBlad("Błąd", "Wypełnij wszystkie pola.");
                return;
            }

            if (!noweHaslo.equals(noweHaslo2)) {
                pokazBlad("Błąd", "Hasła się nie zgadzają.");
                return;
            }

            // Minimalna długość hasła
            if (noweHaslo.length() < 4) {
                pokazBlad("Błąd", "Hasło musi mieć co najmniej 4 znaki.");
                return;
            }

            try {
                AuthService authService = new AuthService();
                boolean ok = authService.resetujHaslo(login, noweHaslo);

                if (ok) {
                    pokazSukces("Sukces", "Hasło zostało zmienione.");
                    stage.close();
                } else {
                    pokazBlad("Błąd", "Nie znaleziono użytkownika o podanym loginie.");
                }
            } catch (IllegalArgumentException ex) {
                pokazBlad("Błąd walidacji", ex.getMessage());
            } catch (Exception ex) {
                pokazBlad("Błąd systemowy", "Wystąpił nieoczekiwany błąd: " + ex.getMessage());
            }
        });

        // Zamknięcie okna bez zmian
        btnAnuluj.setOnAction(e -> stage.close());

        panel.getChildren().addAll(
                tytul,
                new Separator(),
                lblLogin, txtLogin,
                lblNoweHaslo, txtNoweHaslo,
                lblNoweHaslo2, txtNoweHaslo2,
                btnZresetuj,
                btnAnuluj
        );

        root.setCenter(panel);

        Scene scene = new Scene(root, 450, 350);
        stage.setScene(scene);
        stage.setTitle("Reset hasła - Wypożyczalnia");
        stage.show();
    }

    // Komunikat błędu
    private void pokazBlad(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }

    // Komunikat informacyjny
    private void pokazSukces(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }
}