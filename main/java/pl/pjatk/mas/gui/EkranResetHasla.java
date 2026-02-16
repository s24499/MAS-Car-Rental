package pl.pjatk.mas.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.pjatk.mas.service.AuthService;

/**
 * Ekran resetowania hasła na podstawie loginu.
 * GUI zbiera dane i deleguje walidację oraz zapis do AuthService.
 */
public class EkranResetHasla {

    private final AuthService authService = new AuthService();

    /**
     * Buduje i pokazuje widok resetowania hasła.
     */
    public void pokaz(Stage stage) {
        TextField txtLogin = new TextField();
        PasswordField txtNoweHaslo = new PasswordField();
        PasswordField txtNoweHaslo2 = new PasswordField();

        Button btnZresetuj = new Button("Zresetuj hasło");
        Button btnAnuluj = new Button("Anuluj");

        btnZresetuj.setOnAction(e -> obsluzReset(
                stage,
                txtLogin.getText(),
                txtNoweHaslo.getText(),
                txtNoweHaslo2.getText()
        ));

        btnAnuluj.setOnAction(e -> stage.close());

        VBox panel = new VBox(10,
                new Label("Resetowanie hasła"),
                new Separator(),
                new Label("Login"), txtLogin,
                new Label("Nowe hasło"), txtNoweHaslo,
                new Label("Powtórz nowe hasło"), txtNoweHaslo2,
                btnZresetuj,
                btnAnuluj
        );
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.TOP_LEFT);

        BorderPane root = new BorderPane(panel);

        stage.setScene(new Scene(root, 450, 350));
        stage.setTitle("Reset hasła");
        stage.show();
    }

    /**
     * Obsługuje reset hasła (GUI sprawdza tylko zgodność haseł, reszta w Service).
     */
    private void obsluzReset(Stage stage, String login, String noweHaslo, String noweHaslo2) {
        if (noweHaslo != null && !noweHaslo.equals(noweHaslo2)) {
            pokazBlad("Błąd", "Hasła się nie zgadzają.");
            return;
        }

        try {
            boolean ok = authService.resetujHaslo(login, noweHaslo);

            if (ok) {
                pokazInfo("Sukces", "Hasło zostało zmienione.");
                stage.close();
            } else {
                pokazBlad("Błąd", "Nie znaleziono użytkownika o podanym loginie.");
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