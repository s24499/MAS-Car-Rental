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
 * Ekran rejestracji nowego klienta.
 * Pozwala utworzyć konto na podstawie loginu, hasła i danych osobowych.
 */
public class EkranRejestracji {

    /**
     * Wyświetla okno z prostym formularzem rejestracji.
     */
    public void pokaz(Stage stage) {
        BorderPane root = new BorderPane();

        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.TOP_LEFT);

        Label tytul = new Label("Rejestracja nowego klienta");

        Label lblLogin = new Label("Login");
        TextField txtLogin = new TextField();

        Label lblHaslo = new Label("Hasło");
        PasswordField txtHaslo = new PasswordField();

        Label lblHaslo2 = new Label("Powtórz hasło");
        PasswordField txtHaslo2 = new PasswordField();

        Label lblImie = new Label("Imię");
        TextField txtImie = new TextField();

        Label lblNazwisko = new Label("Nazwisko");
        TextField txtNazwisko = new TextField();

        Label lblEmail = new Label("Email");
        TextField txtEmail = new TextField();

        Button btnZarejestruj = new Button("Zarejestruj");
        Button btnAnuluj = new Button("Anuluj");

        // Obsługa przycisku "Zarejestruj"
        btnZarejestruj.setOnAction(e -> {
            String login = txtLogin.getText().trim();
            String haslo = txtHaslo.getText();
            String haslo2 = txtHaslo2.getText();
            String imie = txtImie.getText().trim();
            String nazwisko = txtNazwisko.getText().trim();
            String email = txtEmail.getText().trim();

            // Walidacja pól
            if (login.isEmpty() || haslo.isEmpty() || haslo2.isEmpty()
                    || imie.isEmpty() || nazwisko.isEmpty() || email.isEmpty()) {
                pokazBlad("Błąd", "Wypełnij wszystkie pola.");
                return;
            }

            if (!haslo.equals(haslo2)) {
                pokazBlad("Błąd", "Hasła się nie zgadzają.");
                return;
            }

            // Walidacja email (prosta)
            if (!email.contains("@") || !email.contains(".")) {
                pokazBlad("Błąd", "Podaj prawidłowy adres email.");
                return;
            }

            try {
                AuthService authService = new AuthService();
                boolean ok = authService.zarejestruj(login, haslo, imie, nazwisko, email);

                if (ok) {
                    pokazSukces("Sukces", "Konto zostało utworzone. Możesz się teraz zalogować.");
                    stage.close();
                } else {
                    pokazBlad("Błąd", "Podany login jest już zajęty.");
                }
            } catch (IllegalArgumentException ex) {
                pokazBlad("Błąd walidacji", ex.getMessage());
            } catch (Exception ex) {
                pokazBlad("Błąd systemowy", "Wystąpił nieoczekiwany błąd: " + ex.getMessage());
            }
        });

        // Zamknięcie okna bez rejestracji
        btnAnuluj.setOnAction(e -> stage.close());

        panel.getChildren().addAll(
                tytul,
                new Separator(),
                lblLogin, txtLogin,
                lblHaslo, txtHaslo,
                lblHaslo2, txtHaslo2,
                lblImie, txtImie,
                lblNazwisko, txtNazwisko,
                lblEmail, txtEmail,
                btnZarejestruj,
                btnAnuluj
        );

        root.setCenter(panel);

        Scene scene = new Scene(root, 450, 500);
        stage.setScene(scene);
        stage.setTitle("Rejestracja - Wypożyczalnia");
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