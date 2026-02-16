package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.pjatk.mas.model.Rezerwacja;
import pl.pjatk.mas.service.RezerwacjaService;
import pl.pjatk.mas.util.Session;

import java.util.List;

/**
 * Ekran pokazujący rezerwacje zalogowanego klienta.
 * Dostęp i filtrowanie danych realizuje RezerwacjaService.
 */
public class EkranMojeRezerwacje {

    private final RezerwacjaService rezerwacjaService = new RezerwacjaService();

    private final ObservableList<Rezerwacja> listaRezerwacji = FXCollections.observableArrayList();
    private final ListView<Rezerwacja> listView = new ListView<>(listaRezerwacji);

    /**
     * Buduje i pokazuje widok.
     */
    public void pokaz(Stage stage) {
        if (!wczytajRezerwacje(stage)) {
            return;
        }

        VBox lewyPanel = zbudujLewyPanel();
        TextArea szczegoly = new TextArea();
        Button btnAnuluj = new Button("Anuluj rezerwację");
        VBox prawyPanel = zbudujPrawyPanel(szczegoly, btnAnuluj);

        polaczZachowania(stage, szczegoly, btnAnuluj);

        BorderPane root = new BorderPane();
        root.setLeft(lewyPanel);
        root.setCenter(prawyPanel);

        stage.setScene(new Scene(root, 800, 450));
        stage.setTitle("Moje rezerwacje");
        stage.show();
    }

    /**
     * Wczytuje rezerwacje zalogowanego użytkownika i obsługuje błędy dostępu.
     */
    private boolean wczytajRezerwacje(Stage stage) {
        try {
            List<Rezerwacja> rezerwacje = rezerwacjaService.pobierzRezerwacjeDlaZalogowanego(Session.getZalogowany());
            listaRezerwacji.setAll(rezerwacje);
            return true;
        } catch (IllegalStateException ex) {
            pokazBlad("Brak dostępu", ex.getMessage());
            stage.close();
            return false;
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie udało się wczytać rezerwacji: " + ex.getMessage());
            stage.close();
            return false;
        }
    }

    /**
     * Buduje panel z listą rezerwacji.
     */
    private VBox zbudujLewyPanel() {
        Label labelLista = new Label("Moje rezerwacje");

        VBox.setVgrow(listView, Priority.ALWAYS);

        VBox lewyPanel = new VBox(10, labelLista, listView);
        lewyPanel.setPadding(new Insets(15));
        lewyPanel.setPrefWidth(350);
        return lewyPanel;
    }

    /**
     * Buduje panel szczegółów oraz przycisk anulowania.
     */
    private VBox zbudujPrawyPanel(TextArea szczegoly, Button btnAnuluj) {
        Label labelSzczegoly = new Label("Szczegóły rezerwacji");

        szczegoly.setEditable(false);
        szczegoly.setPrefHeight(200);

        btnAnuluj.setMaxWidth(Double.MAX_VALUE);
        btnAnuluj.setDisable(true);

        VBox panelPrawy = new VBox(15, labelSzczegoly, szczegoly, btnAnuluj);
        panelPrawy.setPadding(new Insets(20));
        panelPrawy.setPrefWidth(450);
        return panelPrawy;
    }

    /**
     * Podpina akcje UI: wybór rezerwacji i anulowanie.
     */
    private void polaczZachowania(Stage stage, TextArea szczegoly, Button btnAnuluj) {
        listView.getSelectionModel().selectedItemProperty().addListener((obs, stary, nowy) -> {
            if (nowy == null) {
                szczegoly.clear();
                btnAnuluj.setDisable(true);
                return;
            }

            szczegoly.setText(formatSzczegoly(nowy));
            btnAnuluj.setDisable(false);
        });

        btnAnuluj.setOnAction(e -> anulujWybrana(stage, szczegoly, btnAnuluj));
    }

    /**
     * Formatuje tekst szczegółów rezerwacji do pola tekstowego.
     */
    private String formatSzczegoly(Rezerwacja r) {
        return "ID: " + r.getId() + "\n" +
                "Samochód: " + r.getSamochod() + "\n" +
                "Termin: " + r.getDataOd() + " - " + r.getDataDo() + "\n" +
                "Status: " + r.getStatus() + "\n" +
                "Cena: " + r.getCenaCalkowita() + " zł\n" +
                "Dodatki: " + r.getDodatki();
    }

    /**
     * Anuluje wybraną rezerwację i odświeża listę.
     */
    private void anulujWybrana(Stage stage, TextArea szczegoly, Button btnAnuluj) {
        Rezerwacja wybrana = listView.getSelectionModel().getSelectedItem();
        if (wybrana == null) return;

        if (!potwierdzAnulowanie(wybrana)) {
            return;
        }

        try {
            rezerwacjaService.anulujRezerwacje(wybrana.getId());

            // Odśwież dane po zmianie
            if (!wczytajRezerwacje(stage)) {
                return;
            }

            listView.getSelectionModel().clearSelection();
            btnAnuluj.setDisable(true);
            szczegoly.clear();

            pokazInfo("Sukces", "Rezerwacja została anulowana.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            pokazBlad("Błąd", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd systemowy", "Nieoczekiwany błąd: " + ex.getMessage());
        }
    }

    /**
     * Pyta użytkownika o potwierdzenie anulowania.
     */
    private boolean potwierdzAnulowanie(Rezerwacja wybrana) {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Czy na pewno anulować rezerwację #" + wybrana.getId() + "?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Potwierdzenie");
        confirm.setHeaderText(null);

        return confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /**
     * Pokazuje komunikat informacyjny.
     */
    private void pokazInfo(String tytul, String tresc) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, tresc, ButtonType.OK);
        a.setTitle(tytul);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /**
     * Pokazuje komunikat błędu.
     */
    private void pokazBlad(String tytul, String tresc) {
        Alert a = new Alert(Alert.AlertType.ERROR, tresc, ButtonType.OK);
        a.setTitle(tytul);
        a.setHeaderText(null);
        a.showAndWait();
    }
}