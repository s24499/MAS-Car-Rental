package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.TypRozliczaniaDodatku;
import pl.pjatk.mas.service.DodatekService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EkranDodajDodatek {

    private final DodatekService dodatekService = new DodatekService();
    private boolean dodano;

    /**
     * Otwiera okno i zwraca true, jeśli dodatek został dodany.
     */
    public boolean pokazDialog(Stage parentStage) {
        dodano = false;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Dodaj dodatek");
        dialog.setResizable(false);

        Label tytul = new Label("Dodawanie dodatku");
        tytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextField tfNazwa = new TextField();
        tfNazwa.setPrefWidth(250);

        TextField tfCena = new TextField();
        tfCena.setPrefWidth(250);

        ComboBox<TypRozliczaniaDodatku> cbTyp = new ComboBox<>();
        cbTyp.getItems().setAll(TypRozliczaniaDodatku.values());
        cbTyp.setPrefWidth(250);

        CheckBox chkWszystkieKategorie = new CheckBox("Dostępny dla wszystkich kategorii");
        chkWszystkieKategorie.setStyle("-fx-font-weight: bold;");

        ListView<KategoriaSamochodu> listaKategorii = new ListView<>();
        listaKategorii.setPrefHeight(110);
        listaKategorii.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaKategorii.setItems(FXCollections.observableArrayList(KategoriaSamochodu.values()));
        listaKategorii.getItems().sort(Comparator.comparing(Enum::name));

        ustawTrybKategorii(chkWszystkieKategorie, listaKategorii);

        VBox pola = new VBox(12,
                pole("Nazwa:", tfNazwa),
                pole("Cena:", tfCena),
                pole("Typ rozliczania:", cbTyp),
                poleKategorie(chkWszystkieKategorie, listaKategorii)
        );

        Button btnDodaj = new Button("Dodaj");
        btnDodaj.setPrefWidth(120);
        btnDodaj.setOnAction(e -> obsluzDodanie(dialog, tfNazwa, tfCena, cbTyp, chkWszystkieKategorie, listaKategorii));

        Button btnAnuluj = new Button("Anuluj");
        btnAnuluj.setPrefWidth(120);
        btnAnuluj.setOnAction(e -> dialog.close());

        HBox przyciski = new HBox(10, btnDodaj, btnAnuluj);
        przyciski.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15, tytul, new Separator(), pola, new Separator(), przyciski);
        root.setPadding(new Insets(20));

        dialog.setScene(new Scene(root, 520, 520));
        dialog.showAndWait();

        return dodano;
    }

    /**
     * Obsługuje kliknięcie "Dodaj" (GUI parsuje cenę, Service waliduje i zapisuje).
     */
    private void obsluzDodanie(Stage dialog,
                               TextField tfNazwa,
                               TextField tfCena,
                               ComboBox<TypRozliczaniaDodatku> cbTyp,
                               CheckBox chkWszystkieKategorie,
                               ListView<KategoriaSamochodu> listaKategorii) {
        try {
            String nazwa = tfNazwa.getText();
            BigDecimal cena = parseCena(tfCena.getText());

            TypRozliczaniaDodatku typ = cbTyp.getValue();
            List<KategoriaSamochodu> kategorie = pobierzKategorie(chkWszystkieKategorie, listaKategorii);

            dodatekService.dodajDodatek(nazwa, cena, typ, kategorie);

            dodano = true;
            pokazInfo("Sukces", "Dodatek został dodany.");
            dialog.close();
        } catch (IllegalArgumentException ex) {
            pokazBlad("Błąd walidacji", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie udało się dodać dodatku: " + ex.getMessage());
        }
    }

    /**
     * Ustawia zachowanie UI: checkbox "wszystkie" włącza/wyłącza listę kategorii.
     */
    private void ustawTrybKategorii(CheckBox chkWszystkieKategorie, ListView<KategoriaSamochodu> listaKategorii) {
        chkWszystkieKategorie.setSelected(true);
        listaKategorii.setDisable(true);

        chkWszystkieKategorie.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                listaKategorii.getSelectionModel().clearSelection();
                listaKategorii.setDisable(true);
            } else {
                listaKategorii.setDisable(false);
            }
        });
    }

    /**
     * Buduje sekcję formularza: etykieta + kontrolka.
     */
    private VBox pole(String label, Control field) {
        VBox box = new VBox(4);
        box.getChildren().addAll(new Label(label), field);
        return box;
    }

    /**
     * Buduje sekcję formularza dla kategorii.
     */
    private VBox poleKategorie(CheckBox chk, ListView<KategoriaSamochodu> lista) {
        VBox box = new VBox(6);
        box.getChildren().addAll(chk, lista);
        return box;
    }

    /**
     * Parsuje cenę wprowadzoną przez użytkownika.
     */
    private BigDecimal parseCena(String txt) {
        try {
            return new BigDecimal(txt.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cena musi być liczbą (np. 10.00).");
        }
    }

    /**
     * Pobiera listę kategorii z UI. Pusta lista oznacza "wszystkie".
     */
    private List<KategoriaSamochodu> pobierzKategorie(CheckBox chkWszystkieKategorie, ListView<KategoriaSamochodu> listaKategorii) {
        if (chkWszystkieKategorie.isSelected()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(listaKategorii.getSelectionModel().getSelectedItems());
    }

    /**
     * Pokazuje komunikat informacyjny.
     */
    private void pokazInfo(String tytul, String tresc) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(tytul);
        a.setHeaderText(null);
        a.setContentText(tresc);
        a.showAndWait();
    }

    /**
     * Pokazuje komunikat błędu.
     */
    private void pokazBlad(String tytul, String tresc) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(tytul);
        a.setHeaderText(null);
        a.setContentText(tresc);
        a.showAndWait();
    }
}