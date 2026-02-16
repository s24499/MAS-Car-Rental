package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pl.pjatk.mas.model.*;
import pl.pjatk.mas.service.DodatekService;
import pl.pjatk.mas.service.FlotaService;
import pl.pjatk.mas.service.RezerwacjaService;
import pl.pjatk.mas.util.App;
import pl.pjatk.mas.util.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Główny ekran aplikacji – prezentuje listę samochodów
 * oraz umożliwia utworzenie rezerwacji.
 *
 * ARCHITEKTURA: GUI → Service → DAO → CSV
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

        // ========== GÓRNY PASEK ==========
        Button btnMojeRezerwacje = new Button("Moje rezerwacje");
        Button btnWyloguj = new Button("Wyloguj");

        HBox topBar = new HBox(10, btnMojeRezerwacje, btnWyloguj);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));

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

        btnWyloguj.setOnAction(e -> {
            Session.setZalogowany(null);
            new App().start(stage);
        });

        // ========== LEWY PANEL - LISTA SAMOCHODÓW ==========
        VBox lewyPanel = new VBox(10);
        lewyPanel.setPadding(new Insets(15));
        lewyPanel.setPrefWidth(280);

        boolean pracownik = Session.getZalogowany() instanceof Pracownik;

        Label lblAuta = new Label("Dostępne samochody");

        // Pasek nad listą: tytuł + przycisk "+"
        HBox naglowekListyAut = new HBox(10);
        naglowekListyAut.setAlignment(Pos.CENTER_LEFT);

        Button btnDodajAuto = new Button("+");
        btnDodajAuto.setPrefWidth(35);
        btnDodajAuto.setFocusTraversable(false);
        btnDodajAuto.setTooltip(new Tooltip("Dodaj nowy samochód"));
        btnDodajAuto.setVisible(pracownik);
        btnDodajAuto.setManaged(pracownik);

        naglowekListyAut.getChildren().addAll(lblAuta, btnDodajAuto);

        ListView<Samochod> listaAut = new ListView<>();
        listaAut.setItems(FXCollections.observableArrayList(auta));
        VBox.setVgrow(listaAut, Priority.ALWAYS);

        lewyPanel.getChildren().addAll(naglowekListyAut, listaAut);

        // Klik "+" -> dodaj samochód
        btnDodajAuto.setOnAction(e -> {
            boolean dodano = new EkranDodajSamochod().pokazDialog(stage);
            if (dodano) {
                List<Samochod> odswiezone = flotaService.pobierzWszystkieSamochody();
                listaAut.setItems(FXCollections.observableArrayList(odswiezone));
                listaAut.refresh();
            }
        });

        // Dwuklik na auto -> edycja (tylko pracownik)
        if (pracownik) {
            listaAut.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Samochod wybrany = listaAut.getSelectionModel().getSelectedItem();
                    if (wybrany != null) {
                        // UWAGA: nazwa klasy wg Twojego projektu
                        // Jeśli masz EkranEdytujSamochod, zmień poniższą linię na new EkranEdytujSamochod()
                        boolean zmienionoAuto = new EkranEdytujSamochod().pokazDialog(stage, wybrany);
                        if (zmienionoAuto) {
                            List<Samochod> odswiezone = flotaService.pobierzWszystkieSamochody();
                            listaAut.setItems(FXCollections.observableArrayList(odswiezone));
                            listaAut.refresh();
                        }
                    }
                }
            });
        }

        // ========== PRAWY PANEL - SZCZEGÓŁY I REZERWACJA ==========
        VBox prawyPanel = new VBox(12);
        prawyPanel.setPadding(new Insets(15));

        // --- Szczegóły samochodu ---
        Label lblSzczegoly = new Label("Szczegóły samochodu");
        TextArea szczegoly = new TextArea();
        szczegoly.setEditable(false);
        szczegoly.setPrefHeight(120);

        // --- Rezerwacje (tylko dla pracownika) ---
        Label lblRezerwacje = new Label("Rezerwacje (widok pracownika)");
        ListView<Rezerwacja> listaRez = new ListView<>();
        listaRez.setPrefHeight(100);

        lblRezerwacje.setVisible(pracownik);
        lblRezerwacje.setManaged(pracownik);
        listaRez.setVisible(pracownik);
        listaRez.setManaged(pracownik);

        Separator sep = new Separator();

        // --- Formularz rezerwacji ---
        Label lblFormularz = new Label("Formularz rezerwacji");

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

        Label lblDodatki = new Label("Dodatki (Ctrl+klik):");
        ListView<Dodatek> listaDod = new ListView<>();
        listaDod.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaDod.setItems(FXCollections.observableArrayList());
        listaDod.setPrefHeight(90);

        HBox boxCena = new HBox(10);
        boxCena.setAlignment(Pos.CENTER);
        Label lblCena = new Label("Szacowana cena:");
        Label wartoscCeny = new Label("0.00 zł");
        boxCena.getChildren().addAll(lblCena, wartoscCeny);

        Button btnRezerwuj = new Button("Zarezerwuj samochód");
        btnRezerwuj.setMaxWidth(Double.MAX_VALUE);

        // Dodaj elementy formularza rezerwacji
        prawyPanel.getChildren().addAll(
                lblSzczegoly, szczegoly,
                lblRezerwacje, listaRez, sep,
                lblFormularz, daty, lblDodatki, listaDod, boxCena, btnRezerwuj
        );

        // --- Zarządzanie dodatkami (tylko dla pracownika) ---
        if (pracownik) {
            Separator sepDodatki = new Separator();

            // Nagłówek + przycisk "+"
            HBox naglowekDodatki = new HBox(10);
            naglowekDodatki.setAlignment(Pos.CENTER_LEFT);

            Label lblZarzadzanieDodatkami = new Label("Zarządzanie dodatkami");
            lblZarzadzanieDodatkami.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

            Button btnDodajDodatek = new Button("+");
            btnDodajDodatek.setPrefWidth(35);
            btnDodajDodatek.setFocusTraversable(false);
            btnDodajDodatek.setTooltip(new Tooltip("Dodaj nowy dodatek"));

            naglowekDodatki.getChildren().addAll(lblZarzadzanieDodatkami, btnDodajDodatek);

            ListView<Dodatek> listaAllDodatkow = new ListView<>();
            listaAllDodatkow.setPrefHeight(100);

            // Wczytaj wszystkie dodatki
            List<Dodatek> wszystkieDodatki = dodatekService.pobierzWszystkieDodatki();
            listaAllDodatkow.setItems(FXCollections.observableArrayList(wszystkieDodatki));

            // Klik "+" -> dodaj dodatek
            btnDodajDodatek.setOnAction(e -> {
                boolean dodano = new EkranDodajDodatek().pokazDialog(stage);
                if (dodano) {
                    // Odśwież listę wszystkich dodatków
                    List<Dodatek> odswiezone = dodatekService.pobierzWszystkieDodatki();
                    listaAllDodatkow.setItems(FXCollections.observableArrayList(odswiezone));
                    listaAllDodatkow.refresh();

                    // Odśwież dodatki dla aktualnie wybranego auta
                    Samochod aktualneAuto = listaAut.getSelectionModel().getSelectedItem();
                    if (aktualneAuto != null) {
                        List<Dodatek> dodatkiAktualnegoAuta = dodatekService.pobierzDodatkiDlaSamochodu(aktualneAuto);
                        listaDod.setItems(FXCollections.observableArrayList(dodatkiAktualnegoAuta));
                        listaDod.refresh();
                    }
                }
            });

            // Double-click obsługa na dodatek
            listaAllDodatkow.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Dodatek wybranyDodalek = listaAllDodatkow.getSelectionModel().getSelectedItem();
                    if (wybranyDodalek != null) {
                        boolean zmieniono = new EkranEdytujDodatek().pokazDialog(stage, wybranyDodalek);
                        if (zmieniono) {
                            // Odśwież listę wszystkich dodatków
                            List<Dodatek> wszystkieDodatkiOdswiezeni = dodatekService.pobierzWszystkieDodatki();
                            listaAllDodatkow.setItems(FXCollections.observableArrayList(wszystkieDodatkiOdswiezeni));

                            // Odśwież dodatki dla wybranego samochodu
                            Samochod aktualneAuto = listaAut.getSelectionModel().getSelectedItem();
                            if (aktualneAuto != null) {
                                List<Dodatek> dodatkiAktualnegoAuta = dodatekService.pobierzDodatkiDlaSamochodu(aktualneAuto);
                                listaDod.setItems(FXCollections.observableArrayList(dodatkiAktualnegoAuta));
                            }
                        }
                    }
                }
            });

            prawyPanel.getChildren().addAll(
                    sepDodatki,
                    naglowekDodatki,
                    listaAllDodatkow
            );
        }

        // Dodanie prawego panelu do ScrollPane
        ScrollPane scroll = new ScrollPane(prawyPanel);
        scroll.setFitToWidth(true);

        // ========== LISTENER: Wybór samochodu ==========
        listaAut.getSelectionModel().selectedItemProperty().addListener((obs, stary, nowy) -> {
            if (nowy != null) {
                obslugazaWyborSamochodu(nowy, szczegoly, listaRez, listaDod,
                        dataOd, dataDo, wartoscCeny, stage, pracownik);
            } else {
                // Czyszczenie gdy brak wybranego auta
                listaDod.setItems(FXCollections.observableArrayList());
                listaRez.setItems(FXCollections.observableArrayList());
                szczegoly.clear();
                wartoscCeny.setText("0.00 zł");
            }
        });

        // ========== LISTENERY: Przeliczanie ceny ==========
        dataOd.valueProperty().addListener((o, oldVal, newVal) ->
                przeliczCene(listaAut.getSelectionModel().getSelectedItem(),
                        dataOd, dataDo, listaDod, wartoscCeny)
        );
        dataDo.valueProperty().addListener((o, oldVal, newVal) ->
                przeliczCene(listaAut.getSelectionModel().getSelectedItem(),
                        dataOd, dataDo, listaDod, wartoscCeny)
        );
        listaDod.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) ->
                przeliczCene(listaAut.getSelectionModel().getSelectedItem(),
                        dataOd, dataDo, listaDod, wartoscCeny)
        );

        // ========== PRZYCISK: Zarezerwuj samochód ==========
        btnRezerwuj.setOnAction(e -> obslugazaRezerwacjasamochodu(
                listaAut, dataOd, dataDo, listaDod, wartoscCeny, pracownik, listaRez
        ));

        // ========== UKŁAD GŁÓWNY ==========
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
     * Obsługa wyboru samochodu z listy - TYLKO GUI LOGIC!
     */
    private void obslugazaWyborSamochodu(Samochod nowy,
                                         TextArea szczegoly,
                                         ListView<Rezerwacja> listaRez,
                                         ListView<Dodatek> listaDod,
                                         DatePicker dataOd,
                                         DatePicker dataDo,
                                         Label wartoscCeny,
                                         Stage stage,
                                         boolean pracownik) {
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

        // Wyświetl rezerwacje wybranego samochodu
        listaRez.setItems(FXCollections.observableArrayList(nowy.getRezerwacje()));

        // Double-click na rezerwacje (tylko dla pracownika)
        if (pracownik) {
            listaRez.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Rezerwacja wybrana = listaRez.getSelectionModel().getSelectedItem();
                    if (wybrana != null) {
                        boolean zmieniono = new EkranEdytujRezerwacje().pokazDialog(stage, wybrana);
                        if (zmieniono) {
                            // Odśwież listę rezerwacji
                            List<Samochod> zaktualizowaneAuta = flotaService.pobierzWszystkieSamochody();
                            Samochod zaktualizowaneAuto = zaktualizowaneAuta.stream()
                                    .filter(s -> s.getId().equals(nowy.getId()))
                                    .findFirst()
                                    .orElse(nowy);
                            listaRez.setItems(FXCollections.observableArrayList(
                                    zaktualizowaneAuto.getRezerwacje()
                            ));
                        }
                    }
                }
            });
        }

        // Pobierz dodatki dla wybranego samochodu
        List<Dodatek> dodatkiDlaAuta = dodatekService.pobierzDodatkiDlaSamochodu(nowy);
        listaDod.setItems(FXCollections.observableArrayList(dodatkiDlaAuta));

        przeliczCene(nowy, dataOd, dataDo, listaDod, wartoscCeny);
    }

    /**
     * Obsługa rezerwacji samochodu - TYLKO GUI LOGIC!
     * Cała logika biznesowa w Service!
     */
    private void obslugazaRezerwacjasamochodu(
            ListView<Samochod> listaAut,
            DatePicker dataOd,
            DatePicker dataDo,
            ListView<Dodatek> listaDod,
            Label wartoscCeny,
            boolean pracownik,
            ListView<Rezerwacja> listaRez) {

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
            // Logika biznesowa w Service!
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

            // Czyszczenie formularza
            dataOd.setValue(null);
            dataDo.setValue(null);
            listaDod.getSelectionModel().clearSelection();
            wartoscCeny.setText("0.00 zł");

            // Odświeżenie listy rezerwacji dla pracownika
            if (pracownik) {
                listaRez.setItems(FXCollections.observableArrayList(auto.getRezerwacje()));
            }

        } catch (IllegalArgumentException | IllegalStateException ex) {
            alert("Błąd: " + ex.getMessage());
        } catch (Exception ex) {
            alert("Błąd systemowy: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Oblicza szacowaną cenę - deleguje do Service!
     * TYLKO GUI LOGIC (wyświetlanie)!
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
            // Service oblicza cenę!
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

    /**
     * Wyświetla alert - TYLKO GUI!
     */
    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}