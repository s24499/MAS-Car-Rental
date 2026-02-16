package pl.pjatk.mas.util;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.pjatk.mas.gui.EkranFloty;
import pl.pjatk.mas.gui.EkranRejestracji;
import pl.pjatk.mas.gui.EkranResetHasla;
import pl.pjatk.mas.model.Klient;
import pl.pjatk.mas.model.Pracownik;
import pl.pjatk.mas.model.Uzytkownik;
import pl.pjatk.mas.service.AuthService;

/**
 * Punkt startowy aplikacji (ekran logowania).
 * GUI deleguje logowanie do AuthService i ustawia Session.
 */
public class App extends Application {

    private final AuthService authService = new AuthService();

    /**
     * Uruchamia pierwsze okno aplikacji (ekran logowania).
     */
    @Override
    public void start(Stage stage) {
        stage.setTitle("Wypożyczalnia samochodów");

        TextField loginField = new TextField();
        loginField.setPromptText("Login");

        PasswordField hasloField = new PasswordField();
        hasloField.setPromptText("Hasło");

        Button btnZaloguj = new Button("Zaloguj");
        btnZaloguj.setMaxWidth(Double.MAX_VALUE);

        Button btnRejestracja = new Button("Rejestracja");
        Button btnResetHasla = new Button("Reset hasła");
        HBox przyciski = new HBox(10, btnRejestracja, btnResetHasla);
        przyciski.setAlignment(Pos.CENTER);

        Hyperlink linkGosc = new Hyperlink("Kontynuuj jako gość");

        VBox panel = new VBox(10,
                new Label("Logowanie"),
                loginField,
                hasloField,
                btnZaloguj,
                przyciski,
                linkGosc
        );
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(350);

        VBox root = new VBox(10, panel);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        btnZaloguj.setOnAction(e -> obsluzLogowanie(stage, loginField, hasloField));
        btnRejestracja.setOnAction(e -> otworzOknoRejestracji());
        btnResetHasla.setOnAction(e -> otworzOknoResetuHasla());

        linkGosc.setOnAction(e -> {
            Session.setZalogowany(null);
            new EkranFloty().pokaz(stage);
        });

        stage.setScene(new Scene(root, 450, 450));
        stage.show();

        loginField.requestFocus();
    }

    /**
     * Obsługuje logowanie: waliduje wprowadzenie pól, wywołuje AuthService, ustawia Session i przechodzi do floty.
     */
    private void obsluzLogowanie(Stage stage, TextField loginField, PasswordField hasloField) {
        String login = loginField.getText() != null ? loginField.getText().trim() : "";
        String haslo = hasloField.getText() != null ? hasloField.getText().trim() : "";

        if (login.isEmpty() || haslo.isEmpty()) {
            pokazBlad("Podaj login i hasło");
            return;
        }

        try {
            Uzytkownik uzytkownik = authService.zaloguj(login, haslo);
            if (uzytkownik == null) {
                pokazBlad("Nieprawidłowy login lub hasło");
                return;
            }

            Session.setZalogowany(uzytkownik);
            pokazPowitanie(uzytkownik);

            new EkranFloty().pokaz(stage);
        } catch (IllegalArgumentException ex) {
            pokazBlad("Błąd walidacji", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd systemowy", "Wystąpił nieoczekiwany błąd: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Otwiera okno rejestracji.
     */
    private void otworzOknoRejestracji() {
        try {
            new EkranRejestracji().pokaz(new Stage());
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie można otworzyć okna rejestracji: " + ex.getMessage());
        }
    }

    /**
     * Otwiera okno resetu hasła.
     */
    private void otworzOknoResetuHasla() {
        try {
            new EkranResetHasla().pokaz(new Stage());
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie można otworzyć okna resetu hasła: " + ex.getMessage());
        }
    }

    /**
     * Pokazuje komunikat powitalny zależny od typu użytkownika.
     */
    private void pokazPowitanie(Uzytkownik uzytkownik) {
        if (uzytkownik instanceof Klient) {
            pokazInfo("Zalogowano", "Witaj " + uzytkownik.getImie() + "!");
        } else if (uzytkownik instanceof Pracownik) {
            pokazInfo("Zalogowano", "Witaj pracowniku " + uzytkownik.getImie() + "!");
        } else {
            pokazInfo("Zalogowano", "Witaj " + uzytkownik.getImie() + "!");
        }
    }

    /**
     * Pokazuje prosty komunikat o błędzie.
     */
    private void pokazBlad(String tresc) {
        Alert alert = new Alert(Alert.AlertType.ERROR, tresc, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Pokazuje komunikat o błędzie z tytułem.
     */
    private void pokazBlad(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.ERROR, tresc, ButtonType.OK);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Pokazuje komunikat informacyjny.
     */
    private void pokazInfo(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, tresc, ButtonType.OK);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Główny punkt wejścia aplikacji JavaFX.
     */
    public static void main(String[] args) {
        launch();
    }
}