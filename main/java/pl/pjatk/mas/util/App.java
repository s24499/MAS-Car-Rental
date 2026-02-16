package pl.pjatk.mas.util;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.pjatk.mas.gui.EkranFloty;
import pl.pjatk.mas.gui.EkranRejestracji;
import pl.pjatk.mas.gui.EkranResetHasla;
import pl.pjatk.mas.model.Uzytkownik;
import pl.pjatk.mas.service.AuthService;

public class App extends Application {

    // Uruchamia pierwsze okno aplikacji (ekran logowania)
    @Override
    public void start(Stage stage) {
        stage.setTitle("Wypożyczalnia samochodów");

        // Główny kontener – wszystko wyśrodkowane, z odstępami
        VBox root = new VBox(10);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        // Wewnętrzny panel na formularz
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(350);

        Label tytul = new Label("Logowanie");

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

        panel.getChildren().addAll(
                tytul,
                loginField,
                hasloField,
                btnZaloguj,
                przyciski,
                linkGosc
        );
        root.getChildren().add(panel);

        // Obsługa logowania
        btnZaloguj.setOnAction(e -> {
            String login = loginField.getText().trim();
            String haslo = hasloField.getText().trim();

            if (login.isEmpty() || haslo.isEmpty()) {
                pokazBlad("Podaj login i hasło");
                return;
            }

            try {
                AuthService authService = new AuthService();
                Uzytkownik uzytkownik = authService.zaloguj(login, haslo);

                if (uzytkownik == null) {
                    pokazBlad("Nieprawidłowy login lub hasło");
                    return;
                }

                Session.setZalogowany(uzytkownik);

                // Wyświetlanie odpowiedniego ekranu w zależności od typu użytkownika
                if (uzytkownik instanceof pl.pjatk.mas.model.Klient) {
                    pokazSukces("Zalogowano", "Witaj " + uzytkownik.getImie() + "!");
                } else if (uzytkownik instanceof pl.pjatk.mas.model.Pracownik) {
                    pokazSukces("Zalogowano", "Witaj pracowniku " + uzytkownik.getImie() + "!");
                }

                new EkranFloty().pokaz(stage);

            } catch (IllegalArgumentException ex) {
                pokazBlad("Błąd walidacji", ex.getMessage());
            } catch (Exception ex) {
                pokazBlad("Błąd systemowy", "Wystąpił nieoczekiwany błąd: " + ex.getMessage());
                ex.printStackTrace(); // Dla debugowania
            }
        });

        // Przejście do rejestracji
        btnRejestracja.setOnAction(e -> {
            try {
                new EkranRejestracji().pokaz(new Stage());
            } catch (Exception ex) {
                pokazBlad("Błąd", "Nie można otworzyć okna rejestracji: " + ex.getMessage());
            }
        });

        // Przejście do resetu hasła
        btnResetHasla.setOnAction(e -> {
            try {
                new EkranResetHasla().pokaz(new Stage());
            } catch (Exception ex) {
                pokazBlad("Błąd", "Nie można otworzyć okna resetu hasła: " + ex.getMessage());
            }
        });

        // Kontynuacja bez logowania
        linkGosc.setOnAction(e -> {
            Session.setZalogowany(null);
            new EkranFloty().pokaz(stage);
        });

        stage.setScene(new Scene(root, 450, 450));
        stage.show();

        // Ustawienie focusu na polu login
        loginField.requestFocus();
    }

    // Wyświetla prosty komunikat o błędzie
    private void pokazBlad(String tresc) {
        Alert alert = new Alert(Alert.AlertType.ERROR, tresc, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Wyświetla komunikat o błędzie z tytułem
    private void pokazBlad(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.ERROR, tresc, ButtonType.OK);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Wyświetla komunikat sukcesu
    private void pokazSukces(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, tresc, ButtonType.OK);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Główny punkt wejścia aplikacji JavaFX
    public static void main(String[] args) {
        launch();
    }
}