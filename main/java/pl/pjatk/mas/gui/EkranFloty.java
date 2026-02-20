package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.Pracownik;
import pl.pjatk.mas.model.Rezerwacja;
import pl.pjatk.mas.model.Samochod;
import pl.pjatk.mas.model.StatusRezerwacji;
import pl.pjatk.mas.service.DodatekService;
import pl.pjatk.mas.service.FlotaService;
import pl.pjatk.mas.service.RezerwacjaService;
import pl.pjatk.mas.util.App;
import pl.pjatk.mas.util.Session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Główny ekran floty.
 * Odpowiada za prezentację danych i wywołania Service na akcje użytkownika.
 */
public class EkranFloty {

    private final RezerwacjaService rezerwacjaService = new RezerwacjaService();
    private final FlotaService flotaService = new FlotaService();
    private final DodatekService dodatekService = new DodatekService();

    /**
     * Buduje i pokazuje główny widok floty.
     */
    public void pokaz(Stage stage) {
        boolean pracownik = Session.getZalogowany() instanceof Pracownik;

        // --- Top bar (nawigacja) ---
        HBox topBar = zbudujTopBar(stage);

        // --- Lewy panel (lista aut) ---
        ListView<Samochod> listaAut = new ListView<>();
        VBox lewyPanel = zbudujLewyPanel(stage, pracownik, listaAut);

        // --- Prawy panel (szczegóły + formularz) ---
        TextArea szczegoly = new TextArea();
        ListView<Rezerwacja> listaRez = new ListView<>();
        ListView<Dodatek> listaDod = new ListView<>();
        DatePicker dataOd = new DatePicker();
        DatePicker dataDo = new DatePicker();
        Label wartoscCeny = new Label("0.00 zł");
        Button btnRezerwuj = new Button("Zarezerwuj samochód");

        VBox prawyPanel = zbudujPrawyPanel(stage, pracownik, szczegoly, listaRez, listaDod, dataOd, dataDo, wartoscCeny, btnRezerwuj, listaAut);

        ScrollPane scroll = new ScrollPane(prawyPanel);
        scroll.setFitToWidth(true);

        // --- Połączenie: wybór auta -> odświeżenie UI ---
        listaAut.getSelectionModel().selectedItemProperty().addListener((obs, stary, nowy) -> {
            if (nowy == null) {
                wyczyscPrawyPanel(szczegoly, listaRez, listaDod, wartoscCeny);
                return;
            }
            pokazSzczegolyAuta(nowy, szczegoly);
            ustawRezerwacjeAuta(pracownik, stage, nowy, listaRez);
            ustawDodatkiAuta(nowy, listaDod);
            przeliczCene(nowy, dataOd, dataDo, listaDod, wartoscCeny);
        });

        // --- Przeliczanie ceny ---
        dataOd.valueProperty().addListener((o, oldVal, newVal) -> przeliczCene(listaAut.getSelectionModel().getSelectedItem(), dataOd, dataDo, listaDod, wartoscCeny));
        dataDo.valueProperty().addListener((o, oldVal, newVal) -> przeliczCene(listaAut.getSelectionModel().getSelectedItem(), dataOd, dataDo, listaDod, wartoscCeny));
        listaDod.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> przeliczCene(listaAut.getSelectionModel().getSelectedItem(), dataOd, dataDo, listaDod, wartoscCeny));

        // --- Rezerwacja ---
        btnRezerwuj.setMaxWidth(Double.MAX_VALUE);
        btnRezerwuj.setOnAction(e -> obsluzRezerwacje(listaAut, dataOd, dataDo, listaDod, wartoscCeny, pracownik, listaRez));

        // --- Layout ---
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(lewyPanel);
        root.setCenter(scroll);

        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Flota - Wypożyczalnia");
        stage.show();
    }

    /**
     * Buduje górny pasek nawigacji.
     */
    private HBox zbudujTopBar(Stage stage) {
        Button btnMojeRezerwacje = new Button("Moje rezerwacje");
        Button btnWyloguj = new Button("Wyloguj");

        btnMojeRezerwacje.setOnAction(e -> new EkranMojeRezerwacje().pokaz(new Stage()));
        btnWyloguj.setOnAction(e -> {
            Session.setZalogowany(null);
            new App().start(stage);
        });

        HBox topBar = new HBox(10, btnMojeRezerwacje, btnWyloguj);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));
        return topBar;
    }

    /**
     * Buduje lewy panel z listą aut + opcje pracownika (dodaj/edytuj).
     */
    private VBox zbudujLewyPanel(Stage stage, boolean pracownik, ListView<Samochod> listaAut) {
        Label lblAuta = new Label("Dostępne samochody");

        Button btnDodajAuto = new Button("+");
        btnDodajAuto.setPrefWidth(35);
        btnDodajAuto.setFocusTraversable(false);
        btnDodajAuto.setTooltip(new Tooltip("Dodaj nowy samochód"));
        btnDodajAuto.setVisible(pracownik);
        btnDodajAuto.setManaged(pracownik);

        HBox naglowek = new HBox(10, lblAuta, btnDodajAuto);
        naglowek.setAlignment(Pos.CENTER_LEFT);

        odswiezListeAut(listaAut);
        VBox.setVgrow(listaAut, Priority.ALWAYS);

        btnDodajAuto.setOnAction(e -> {
            boolean dodano = new EkranDodajSamochod().pokazDialog(stage);
            if (dodano) {
                odswiezListeAut(listaAut);
            }
        });

        if (pracownik) {
            listaAut.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Samochod wybrany = listaAut.getSelectionModel().getSelectedItem();
                    if (wybrany == null) return;

                    boolean zmienionoAuto = new EkranEdytujSamochod().pokazDialog(stage, wybrany);
                    if (zmienionoAuto) {
                        odswiezListeAut(listaAut);
                    }
                }
            });
        }

        VBox lewyPanel = new VBox(10, naglowek, listaAut);
        lewyPanel.setPadding(new Insets(15));
        lewyPanel.setPrefWidth(280);
        return lewyPanel;
    }

    /**
     * Buduje prawy panel ze szczegółami, formularzem rezerwacji i widokiem pracownika.
     */
    private VBox zbudujPrawyPanel(Stage stage, boolean pracownik, TextArea szczegoly, ListView<Rezerwacja> listaRez, ListView<Dodatek> listaDod,
                                  DatePicker dataOd, DatePicker dataDo, Label wartoscCeny, Button btnRezerwuj, ListView<Samochod> listaAut) {

        szczegoly.setEditable(false);
        szczegoly.setPrefHeight(120);

        Label lblSzczegoly = new Label("Szczegóły samochodu");

        Label lblRezerwacje = new Label("Rezerwacje (widok pracownika)");
        listaRez.setPrefHeight(100);
        lblRezerwacje.setVisible(pracownik);
        lblRezerwacje.setManaged(pracownik);
        listaRez.setVisible(pracownik);
        listaRez.setManaged(pracownik);

        HBox daty = new HBox(10,
                new VBox(3, new Label("Od:"), dataOd),
                new VBox(3, new Label("Do:"), dataDo)
        );

        Label lblDodatki = new Label("Dodatki (Ctrl+klik):");
        listaDod.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaDod.setItems(FXCollections.observableArrayList());
        listaDod.setPrefHeight(90);

        HBox boxCena = new HBox(10, new Label("Szacowana cena:"), wartoscCeny);
        boxCena.setAlignment(Pos.CENTER);

        VBox prawyPanel = new VBox(12,
                lblSzczegoly, szczegoly,
                lblRezerwacje, listaRez, new Separator(),
                new Label("Formularz rezerwacji"),
                daty,
                lblDodatki, listaDod,
                boxCena,
                btnRezerwuj
        );
        prawyPanel.setPadding(new Insets(15));

        if (pracownik) {
            prawyPanel.getChildren().addAll(new Separator(), zbudujPanelDodatkow(stage, listaDod, listaAut));
        }

        return prawyPanel;
    }

    /**
     * Buduje panel zarządzania dodatkami (tylko pracownik).
     */
    private VBox zbudujPanelDodatkow(Stage stage, ListView<Dodatek> listaDodDoFormularza, ListView<Samochod> listaAut) {
        Label lbl = new Label("Zarządzanie dodatkami");
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

        Button btnDodajDodatek = new Button("+");
        btnDodajDodatek.setPrefWidth(35);
        btnDodajDodatek.setFocusTraversable(false);
        btnDodajDodatek.setTooltip(new Tooltip("Dodaj nowy dodatek"));

        HBox naglowek = new HBox(10, lbl, btnDodajDodatek);
        naglowek.setAlignment(Pos.CENTER_LEFT);

        ListView<Dodatek> listaAllDodatkow = new ListView<>();
        listaAllDodatkow.setPrefHeight(100);
        odswiezListeDodatkow(listaAllDodatkow);

        btnDodajDodatek.setOnAction(e -> {
            boolean dodano = new EkranDodajDodatek().pokazDialog(stage);
            if (!dodano) return;

            odswiezListeDodatkow(listaAllDodatkow);

            Samochod aktualneAuto = listaAut.getSelectionModel().getSelectedItem();
            if (aktualneAuto != null) {
                listaDodDoFormularza.setItems(FXCollections.observableArrayList(dodatekService.pobierzDodatkiDlaSamochodu(aktualneAuto)));
            }
        });

        listaAllDodatkow.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Dodatek wybrany = listaAllDodatkow.getSelectionModel().getSelectedItem();
                if (wybrany == null) return;

                boolean zmieniono = new EkranEdytujDodatek().pokazDialog(stage, wybrany);
                if (!zmieniono) return;

                odswiezListeDodatkow(listaAllDodatkow);

                Samochod aktualneAuto = listaAut.getSelectionModel().getSelectedItem();
                if (aktualneAuto != null) {
                    listaDodDoFormularza.setItems(FXCollections.observableArrayList(dodatekService.pobierzDodatkiDlaSamochodu(aktualneAuto)));
                }
            }
        });

        return new VBox(10, naglowek, listaAllDodatkow);
    }

    /**
     * Ustawia tekst szczegółów dla wybranego auta.
     */
    private void pokazSzczegolyAuta(Samochod auto, TextArea szczegoly) {
        String cena = (auto.getCennik() != null)
                ? auto.getCennik().getStawkaZaDobe() + " zł/dobę"
                : "Brak cennika";

        szczegoly.setText(
                "Marka: " + auto.getMarka() + "\n" +
                        "Model: " + auto.getModel() + "\n" +
                        "Rejestracja: " + auto.getNumerRejestracyjny() + "\n" +
                        "Moc: " + auto.getMocKM() + " KM\n" +
                        "Rocznik: " + auto.getRocznik() + "\n" +
                        "Kategoria: " + auto.getKategoria() + "\n" +
                        "Cena: " + cena
        );
    }

    /**
     * Ustawia listę rezerwacji auta i opcję edycji rezerwacji (tylko pracownik).
     */
    private void ustawRezerwacjeAuta(boolean pracownik, Stage stage, Samochod auto, ListView<Rezerwacja> listaRez) {
        listaRez.setItems(FXCollections.observableArrayList(auto.getRezerwacje()));

        if (!pracownik) {
            listaRez.setOnMouseClicked(null);
            return;
        }

        listaRez.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Rezerwacja wybrana = listaRez.getSelectionModel().getSelectedItem();
                if (wybrana == null) return;

                boolean zmieniono = new EkranEdytujRezerwacje().pokazDialog(stage, wybrana);
                if (!zmieniono) return;

                List<Samochod> odswiezone = flotaService.pobierzWszystkieSamochody();
                Samochod autoPo = odswiezone.stream()
                        .filter(s -> s.getId().equals(auto.getId()))
                        .findFirst()
                        .orElse(auto);

                listaRez.setItems(FXCollections.observableArrayList(autoPo.getRezerwacje()));
            }
        });
    }

    /**
     * Ustawia dodatki dostępne dla wybranego auta.
     */
    private void ustawDodatkiAuta(Samochod auto, ListView<Dodatek> listaDod) {
        listaDod.setItems(FXCollections.observableArrayList(dodatekService.pobierzDodatkiDlaSamochodu(auto)));
    }

    /**
     * Czyści panel szczegółów, gdy nie ma wybranego auta.
     */
    private void wyczyscPrawyPanel(TextArea szczegoly, ListView<Rezerwacja> listaRez, ListView<Dodatek> listaDod, Label wartoscCeny) {
        szczegoly.clear();
        listaRez.setItems(FXCollections.observableArrayList());
        listaDod.setItems(FXCollections.observableArrayList());
        wartoscCeny.setText("0.00 zł");
    }

    /**
     * Odświeża listę aut z danych z Service.
     */
    private void odswiezListeAut(ListView<Samochod> listaAut) {
        List<Samochod> auta = flotaService.pobierzWszystkieSamochody();
        listaAut.setItems(FXCollections.observableArrayList(auta));
        listaAut.refresh();
    }

    /**
     * Odświeża listę dodatków z danych z Service.
     */
    private void odswiezListeDodatkow(ListView<Dodatek> listaAllDodatkow) {
        listaAllDodatkow.setItems(FXCollections.observableArrayList(dodatekService.pobierzWszystkieDodatki()));
        listaAllDodatkow.refresh();
    }

    /**
     * Obsługuje utworzenie rezerwacji dla wybranego auta.
     */
    private void obsluzRezerwacje(ListView<Samochod> listaAut, DatePicker dataOd, DatePicker dataDo, ListView<Dodatek> listaDod,
                                  Label wartoscCeny, boolean pracownik, ListView<Rezerwacja> listaRez) {
        Samochod auto = listaAut.getSelectionModel().getSelectedItem();

        try {
            Rezerwacja rez = rezerwacjaService.utworzRezerwacjeDlaZalogowanego(
                    Session.getZalogowany(),
                    auto,
                    dataOd.getValue(),
                    dataDo.getValue(),
                    new ArrayList<>(listaDod.getSelectionModel().getSelectedItems())
            );

            alert("Rezerwacja utworzona:\n" +
                    "Nr rezerwacji: " + rez.getId() + "\n" +
                    rez.getDataOd() + " - " + rez.getDataDo() +
                    "\nCena: " + rez.getCenaCalkowita() + " zł");

            dataOd.setValue(null);
            dataDo.setValue(null);
            listaDod.getSelectionModel().clearSelection();
            wartoscCeny.setText("0.00 zł");

            if (pracownik && auto != null) {
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
     * Przelicza szacowaną cenę rezerwacji na podstawie wyboru dat i dodatków.
     */
    private void przeliczCene(Samochod auto, DatePicker od, DatePicker doDo, ListView<Dodatek> listaDodatkow, Label labelCena) {
        try {
            BigDecimal cena = rezerwacjaService.obliczSzacowanaCena(
                    auto,
                    od.getValue(),
                    doDo.getValue(),
                    new ArrayList<>(listaDodatkow.getSelectionModel().getSelectedItems())
            );
            labelCena.setText(cena + " zł");
        } catch (Exception e) {
            labelCena.setText("0.00 zł");
        }
    }

    /**
     * Pokazuje prosty komunikat informacyjny.
     */
    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}