package pl.pjatk.mas.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.pjatk.mas.service.AuthService;

/**
 * Ekran rejestracji klienta.
 * GUI zbiera dane i deleguje walidację oraz zapis do AuthService.
 */
public class EkranRejestracji {

    private final AuthService authService = new AuthService();

    /**
     * Buduje i pokazuje widok rejestracji.
     */
    public void pokaz(Stage stage) {
        TextField txtLogin = new TextField();
        PasswordField txtHaslo = new PasswordField();
        PasswordField txtHaslo2 = new PasswordField();
        TextField txtImie = new TextField();
        TextField txtNazwisko = new TextField();
        TextField txtEmail = new TextField();

        Button btnZarejestruj = new Button("Zarejestruj");
        Button btnAnuluj = new Button("Anuluj");

        btnZarejestruj.setOnAction(e -> obsluzRejestracje(
                stage,
                txtLogin.getText(),
                txtHaslo.getText(),
                txtHaslo2.getText(),
                txtImie.getText(),
                txtNazwisko.getText(),
                txtEmail.getText()
        ));

        btnAnuluj.setOnAction(e -> stage.close());

        VBox panel = new VBox(10,
                new Label("Rejestracja nowego klienta"),
                new Separator(),
                new Label("Login"), txtLogin,
                new Label("Hasło"), txtHaslo,
                new Label("Powtórz hasło"), txtHaslo2,
                new Label("Imię"), txtImie,
                new Label("Nazwisko"), txtNazwisko,
                new Label("Email"), txtEmail,
                btnZarejestruj,
                btnAnuluj
        );
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.TOP_LEFT);

        BorderPane root = new BorderPane(panel);

        stage.setScene(new Scene(root, 450, 450));
        stage.setTitle("Rejestracja");
        stage.show();
    }

    /**
     * Obsługuje rejestrację (GUI sprawdza tylko zgodność haseł, reszta w Service).
     */
    private void obsluzRejestracje(Stage stage,
                                   String login,
                                   String haslo,
                                   String haslo2,
                                   String imie,
                                   String nazwisko,
                                   String email) {
        if (haslo != null && !haslo.equals(haslo2)) {
            pokazBlad("Błąd", "Hasła się nie zgadzają.");
            return;
        }

        try {
            boolean ok = authService.zarejestruj(login, haslo, imie, nazwisko, email);

            if (ok) {
                pokazInfo("Sukces", "Konto zostało utworzone. Możesz się teraz zalogować.");
                stage.close();
            } else {
                pokazBlad("Błąd", "Podany login jest już zajęty.");
            }
        } catch (IllegalArgumentException ex) {
            pokazBlad("Błąd walidacji", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd systemowy", "Wystąpił nieoczekiwany błąd: " + ex.getMessage());
        }
    }

    /**
     * Pokazuje komunikat informacyjny.
     */
    private void pokazInfo(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }

    /**
     * Pokazuje komunikat błędu.
     */
    private void pokazBlad(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }
}