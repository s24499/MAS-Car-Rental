package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.pjatk.mas.model.Klient;
import pl.pjatk.mas.model.Rezerwacja;
import pl.pjatk.mas.model.StatusRezerwacji;
import pl.pjatk.mas.service.RezerwacjaService;
import pl.pjatk.mas.util.Session;

import java.util.List;

/**
 * Ekran prezentujący rezerwacje zalogowanego klienta.
 * Umożliwia podgląd szczegółów i anulowanie nowych rezerwacji.
 */
public class EkranMojeRezerwacje {

    // Serwis do zarządzania rezerwacjami
    private final RezerwacjaService rezerwacjaService = new RezerwacjaService();

    // Lista rezerwacji widoczna w GUI
    private ObservableList<Rezerwacja> listaRezerwacji;

    // Komponent wyświetlający listę rezerwacji
    private ListView<Rezerwacja> listView;

    /**
     * Wyświetla okno z listą rezerwacji bieżącego klienta.
     */
    public void pokaz(Stage stage) {
        // Odczyt aktualnie zalogowanego klienta z Session
        Klient klient = (Klient) Session.getZalogowany();

        // Pobranie rezerwacji tylko tego klienta za pomocą serwisu
        List<Rezerwacja> rezerwacje = rezerwacjaService.pobierzRezerwacjeKlienta(klient);

        listaRezerwacji = FXCollections.observableArrayList(rezerwacje);
        listView = new ListView<>(listaRezerwacji);

        // Lewy panel – lista rezerwacji
        VBox lewyPanel = new VBox(10);
        lewyPanel.setPadding(new Insets(15));
        lewyPanel.setPrefWidth(350);

        Label labelLista = new Label("Moje rezerwacje");

        VBox.setVgrow(listView, Priority.ALWAYS);
        lewyPanel.getChildren().addAll(labelLista, listView);

        // Prawy panel – szczegóły wybranej rezerwacji i przycisk anulowania
        VBox panelPrawy = new VBox(15);
        panelPrawy.setPadding(new Insets(20));
        panelPrawy.setPrefWidth(450);

        Label labelSzczegoly = new Label("Szczegóły rezerwacji");

        TextArea szczegoly = new TextArea();
        szczegoly.setEditable(false);
        szczegoly.setPrefHeight(200);

        Button btnAnuluj = new Button("Anuluj rezerwację");
        btnAnuluj.setMaxWidth(Double.MAX_VALUE);
        btnAnuluj.setDisable(true); // na początku nie ma wybranej rezerwacji

        panelPrawy.getChildren().addAll(labelSzczegoly, szczegoly, btnAnuluj);

        // Reakcja na wybór rezerwacji z listy
        listView.getSelectionModel().selectedItemProperty().addListener((obs, stary, nowy) -> {
            if (nowy != null) {
                szczegoly.setText(
                        "Nr rezerwacji: " + nowy.getId() + "\n" +
                                "Samochód: " + nowy.getSamochod().getMarka() + " " + nowy.getSamochod().getModel() + "\n" +
                                "Okres: " + nowy.getDataOd() + " - " + nowy.getDataDo() + "\n" +
                                "Status: " + nowy.getStatus() + "\n" +
                                "Cena: " + nowy.getCenaCalkowita() + " zł"
                );

                // Anulować można tylko rezerwację o statusie NOWA
                btnAnuluj.setDisable(nowy.getStatus() != StatusRezerwacji.NOWA);
            } else {
                szczegoly.clear();
                btnAnuluj.setDisable(true);
            }
        });

        // Obsługa przycisku "Anuluj rezerwację"
        btnAnuluj.setOnAction(event -> {
            Rezerwacja wybrana = listView.getSelectionModel().getSelectedItem();
            if (wybrana != null && wybrana.getStatus() == StatusRezerwacji.NOWA) {

                Alert potwierdzenie = new Alert(Alert.AlertType.CONFIRMATION);
                potwierdzenie.setTitle("Potwierdzenie");
                potwierdzenie.setHeaderText("Czy na pewno chcesz anulować rezerwację?");
                potwierdzenie.setContentText(
                        "Nr: " + wybrana.getId() + ", " +
                                wybrana.getSamochod().getMarka() + " " + wybrana.getSamochod().getModel()
                );

                if (potwierdzenie.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    try {
                        // Anulowanie rezerwacji za pomocą serwisu
                        rezerwacjaService.anulujRezerwacje(wybrana.getId());

                        // Odświeżenie lokalnej listy
                        wybrana.setStatus(StatusRezerwacji.ANULOWANA);

                        pokazKomunikat("Sukces", "Rezerwacja została anulowana.");

                        // Odświeżenie widoku szczegółów
                        szczegoly.setText(
                                "Nr rezerwacji: " + wybrana.getId() + "\n" +
                                        "Samochód: " + wybrana.getSamochod().getMarka() + " " + wybrana.getSamochod().getModel() + "\n" +
                                        "Okres: " + wybrana.getDataOd() + " - " + wybrana.getDataDo() + "\n" +
                                        "Status: " + wybrana.getStatus() + "\n" +
                                        "Cena: " + wybrana.getCenaCalkowita() + " zł"
                        );
                        btnAnuluj.setDisable(true);
                        listView.refresh();
                    } catch (Exception e) {
                        pokazBlad("Błąd", "Nie udało się anulować rezerwacji: " + e.getMessage());
                    }
                }
            }
        });

        // Główny układ ekranu
        BorderPane root = new BorderPane();
        root.setLeft(lewyPanel);
        root.setCenter(panelPrawy);

        Scene scene = new Scene(root, 850, 600);
        stage.setScene(scene);
        stage.setTitle("Moje rezerwacje - Wypożyczalnia");
        stage.show();
    }

    // Prosty komunikat informacyjny
    private void pokazKomunikat(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }

    // Komunikat błędu
    private void pokazBlad(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }
}