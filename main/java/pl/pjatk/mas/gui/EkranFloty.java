package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pl.pjatk.mas.util.App;
import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.Klient;
import pl.pjatk.mas.model.Pracownik;
import pl.pjatk.mas.model.Rezerwacja;
import pl.pjatk.mas.model.Samochod;
import pl.pjatk.mas.service.DodatekService;
import pl.pjatk.mas.service.FlotaService;
import pl.pjatk.mas.service.RezerwacjaService;
import pl.pjatk.mas.util.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Główny ekran aplikacji – prezentuje listę samochodów
 * oraz umożliwia utworzenie rezerwacji.
 */
public class EkranFloty {

    // Serwisy
    private final RezerwacjaService rezerwacjaService = new RezerwacjaService();
    private final FlotaService flotaService = new FlotaService();
    private final DodatekService dodatekService = new DodatekService();

    /**
     * Wyświetla okno z listą samochodów i formularzem rezerwacji.
     */
    public void pokaz(Stage stage) {
        // Wczytanie wszystkich samochodów wraz z rezerwacjami
        List<Samochod> auta = flotaService.pobierzWszystkieSamochody();

        // Górny pasek – przycisk "Moje rezerwacje" i "Wyloguj"
        Button btnMojeRezerwacje = new Button("Moje rezerwacje");
        Button btnWyloguj = new Button("Wyloguj");

        HBox topBar = new HBox(10, btnMojeRezerwacje, btnWyloguj);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));

        // Obsługa przycisku "Moje rezerwacje"
        btnMojeRezerwacje.setOnAction(e -> {
            if (Session.getZalogowany() == null) {
                alert("Tylko zalogowani użytkownicy mogą przeglądać rezerwacje");
                return;
            }
            if (!(Session.getZalogowany() instanceof Klient)) {
                alert("Tylko klienci mogą przeglądać swoje rezerwacje");
                return;
            }
            new EkranMojeRezerwacje().pokaz(new Stage());
        });

        // Wylogowanie – powrót do ekranu logowania
        btnWyloguj.setOnAction(e -> {
            Session.setZalogowany(null);
            new App().start(stage);
        });

        // Lewy panel – lista dostępnych samochodów
        VBox lewyPanel = new VBox(10);
        lewyPanel.setPadding(new Insets(15));
        lewyPanel.setPrefWidth(280);

        Label lblAuta = new Label("Dostępne samochody");

        ListView<Samochod> listaAut = new ListView<>();
        listaAut.setItems(FXCollections.observableArrayList(auta));
        VBox.setVgrow(listaAut, Priority.ALWAYS);

        lewyPanel.getChildren().addAll(lblAuta, listaAut);

        // Prawy panel – szczegóły auta i formularz rezerwacji
        VBox prawyPanel = new VBox(12);
        prawyPanel.setPadding(new Insets(15));

        // Szczegóły wybranego samochodu
        Label lblSzczegoly = new Label("Szczegóły samochodu");
        TextArea szczegoly = new TextArea();
        szczegoly.setEditable(false);
        szczegoly.setPrefHeight(120);

        // Lista rezerwacji widoczna tylko dla pracownika
        Label lblRezerwacje = new Label("Rezerwacje (widok pracownika)");
        ListView<Rezerwacja> listaRez = new ListView<>();
        listaRez.setPrefHeight(100);

        boolean pracownik = Session.getZalogowany() instanceof Pracownik;
        lblRezerwacje.setVisible(pracownik);
        lblRezerwacje.setManaged(pracownik);
        listaRez.setVisible(pracownik);
        listaRez.setManaged(pracownik);

        Separator sep = new Separator();

        // Formularz rezerwacji: daty, dodatki, cena i przycisk
        Label lblFormularz = new Label("Formularz rezerwacji");

        // Wybór dat
        HBox daty = new HBox(10);

        VBox boxOd = new VBox(3);
        Label lblOd = new Label("Od:");
        DatePicker dataOd = new DatePicker();
        boxOd.getChildren().addAll(lblOd, dataOd);

        VBox boxDo = new VBox(3);
        Label lblDo = new Label("Do:");
        DatePicker dataDo = new DatePicker();
        boxDo.getChildren().addAll(lblDo, dataDo);

        daty.getChildren().addAll(boxOd, boxDo);

        // Lista dodatków do rezerwacji - POCZĄTKOWO PUSTA!
        Label lblDodatki = new Label("Dodatki (Ctrl+klik):");
        ListView<Dodatek> listaDod = new ListView<>();
        listaDod.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaDod.setItems(FXCollections.observableArrayList()); // Pusta lista na start
        listaDod.setPrefHeight(90);

        // Pole z podglądem szacowanej ceny
        HBox boxCena = new HBox(10);
        boxCena.setAlignment(Pos.CENTER);
        Label lblCena = new Label("Szacowana cena:");
        Label wartoscCeny = new Label("0.00 zł");
        boxCena.getChildren().addAll(lblCena, wartoscCeny);

        // Przycisk tworzenia rezerwacji
        Button btnRezerwuj = new Button("Zarezerwuj samochód");
        btnRezerwuj.setMaxWidth(Double.MAX_VALUE);

        prawyPanel.getChildren().addAll(
                lblSzczegoly, szczegoly,
                lblRezerwacje, listaRez, sep,
                lblFormularz, daty, lblDodatki, listaDod, boxCena, btnRezerwuj
        );

        // Dodanie prawego panelu do ScrollPane (w razie mniejszych ekranów)
        ScrollPane scroll = new ScrollPane(prawyPanel);
        scroll.setFitToWidth(true);

        // Po wyborze samochodu wypełniamy szczegóły, listę rezerwacji i dodatki
        listaAut.getSelectionModel().selectedItemProperty().addListener((obs, stary, nowy) -> {
            if (nowy != null) {
                String cena = (nowy.getCennik() != null)
                        ? nowy.getCennik().getStawkaZaDobe() + " zł/dobę"
                        : "Brak cennika";

                szczegoly.setText(
                        "Marka: " + nowy.getMarka() + "\n" +
                                "Model: " + nowy.getModel() + "\n" +
                                "Rejestracja: " + nowy.getNumerRejestracyjny() + "\n" +
                                "Moc: " + nowy.getMocKM() + " KM\n" +
                                "Rocznik: " + nowy.getRocznik() + "\n" +
                                "Kategoria: " + nowy.getKategoria() + "\n" +
                                "Cena: " + cena
                );

                // Wyświetla istniejące rezerwacje wybranego samochodu w liście pracownika
                listaRez.setItems(FXCollections.observableArrayList(nowy.getRezerwacje()));

                // Pobieramy dodatki TYLKO dla wybranej kategorii samochodu
                List<Dodatek> dodatkiDlaAuta = dodatekService.pobierzDodatkiDlaSamochodu(nowy);
                listaDod.setItems(FXCollections.observableArrayList(dodatkiDlaAuta));

                przeliczCene(nowy, dataOd, dataDo, listaDod, wartoscCeny);
            } else {
                // Czyszczenie gdy brak wybranego auta
                listaDod.setItems(FXCollections.observableArrayList());
                listaRez.setItems(FXCollections.observableArrayList());
                szczegoly.clear();
                wartoscCeny.setText("0.00 zł");
            }
        });

        // Przeliczanie ceny przy zmianie dat lub dodatków
        dataOd.valueProperty().addListener((o, oldVal, newVal) ->
                przeliczCene(listaAut.getSelectionModel().getSelectedItem(), dataOd, dataDo, listaDod, wartoscCeny)
        );
        dataDo.valueProperty().addListener((o, oldVal, newVal) ->
                przeliczCene(listaAut.getSelectionModel().getSelectedItem(), dataOd, dataDo, listaDod, wartoscCeny)
        );
        listaDod.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) ->
                przeliczCene(listaAut.getSelectionModel().getSelectedItem(), dataOd, dataDo, listaDod, wartoscCeny)
        );

        // Obsługa przycisku "Zarezerwuj samochód"
        btnRezerwuj.setOnAction(e -> {
            if (Session.getZalogowany() == null) {
                alert("Tylko zalogowani użytkownicy mogą dokonywać rezerwacji");
                return;
            }

            if (!(Session.getZalogowany() instanceof Klient)) {
                alert("Tylko klienci mogą dokonywać rezerwacji");
                return;
            }

            Samochod auto = listaAut.getSelectionModel().getSelectedItem();
            if (auto == null) {
                alert("Wybierz samochód");
                return;
            }
            if (dataOd.getValue() == null || dataDo.getValue() == null) {
                alert("Wybierz daty");
                return;
            }
            if (dataDo.getValue().isBefore(dataOd.getValue())) {
                alert("Nieprawidłowy zakres dat");
                return;
            }

            try {
                // Utworzenie nowej rezerwacji za pomocą serwisu
                // ID jest generowane automatycznie w serwisie
                Rezerwacja rez = rezerwacjaService.utworzRezerwacje(
                        (Klient) Session.getZalogowany(),
                        auto,
                        dataOd.getValue(),
                        dataDo.getValue(),
                        new ArrayList<>(listaDod.getSelectionModel().getSelectedItems())
                );

                alert("Rezerwacja utworzona:\n" +
                        "Nr rezerwacji: " + rez.getId() + "\n" +
                        rez.getDataOd() + " - " + rez.getDataDo() +
                        "\nCena: " + rez.getCenaCalkowita() + " zł");

                // Wyczyszczenie formularza
                dataOd.setValue(null);
                dataDo.setValue(null);
                listaDod.getSelectionModel().clearSelection();
                wartoscCeny.setText("0.00 zł");

                // Odświeżenie listy rezerwacji dla pracownika
                if (pracownik) {
                    listaRez.setItems(FXCollections.observableArrayList(auto.getRezerwacje()));
                    listaRez.refresh();
                }

            } catch (IllegalArgumentException | IllegalStateException ex) {
                alert("Błąd: " + ex.getMessage());
            } catch (Exception ex) {
                alert("Błąd systemowy: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Ułożenie całego ekranu w BorderPane
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(lewyPanel);
        root.setCenter(scroll);

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Flota - Wypożyczalnia");
        stage.show();
    }

    /**
     * Przelicza szacowaną cenę na podstawie wybranego auta,
     * dat i dodatków - używając RezerwacjaService.
     */
    private void przeliczCene(Samochod auto,
                              DatePicker od,
                              DatePicker doDo,
                              ListView<Dodatek> listaDodatkow,
                              Label labelCena) {

        if (auto == null || auto.getCennik() == null ||
                od.getValue() == null || doDo.getValue() == null) {
            labelCena.setText("0.00 zł");
            return;
        }

        LocalDate dataOd = od.getValue();
        LocalDate dataDo = doDo.getValue();

        if (dataDo.isBefore(dataOd)) {
            labelCena.setText("Błąd");
            return;
        }

        try {
            // Użycie serwisu do obliczenia ceny
            BigDecimal cena = rezerwacjaService.obliczSzacowanaCena(
                    auto,
                    dataOd,
                    dataDo,
                    new ArrayList<>(listaDodatkow.getSelectionModel().getSelectedItems())
            );
            labelCena.setText(cena + " zł");
        } catch (Exception e) {
            labelCena.setText("Błąd");
        }
    }

    // Prosta metoda pomocnicza do wyświetlania komunikatów
    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}