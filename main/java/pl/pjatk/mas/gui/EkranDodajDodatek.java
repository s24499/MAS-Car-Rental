package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.TypRozliczaniaDodatku;
import pl.pjatk.mas.service.DodatekService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EkranDodajDodatek {

    private final DodatekService dodatekService = new DodatekService();
    private boolean dodano = false;

    public boolean pokazDialog(Stage parentStage) {
        this.dodano = false;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Dodaj dodatek");
        dialog.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label lblTytul = new Label("Dodawanie dodatku");
        lblTytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextField tfNazwa = new TextField();
        tfNazwa.setPrefWidth(250);

        TextField tfCena = new TextField();
        tfCena.setPrefWidth(250);

        ComboBox<TypRozliczaniaDodatku> cbTyp = new ComboBox<>();
        cbTyp.getItems().addAll(TypRozliczaniaDodatku.values());
        cbTyp.setPrefWidth(250);

        // Kategorie
        CheckBox chkWszystkieKategorie = new CheckBox("Dostępny dla wszystkich kategorii");
        chkWszystkieKategorie.setStyle("-fx-font-weight: bold;");

        ListView<KategoriaSamochodu> listaKategorii = new ListView<>();
        listaKategorii.setPrefHeight(110);
        listaKategorii.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaKategorii.setItems(FXCollections.observableArrayList(KategoriaSamochodu.values()));
        listaKategorii.getItems().sort(Comparator.comparing(Enum::name));

        // domyślnie: dla wszystkich
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

        VBox pola = new VBox(12,
                pole("Nazwa:", tfNazwa),
                pole("Cena:", tfCena),
                pole("Typ rozliczania:", cbTyp),
                kategoriePole(chkWszystkieKategorie, listaKategorii)
        );

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnDodaj = new Button("Dodaj");
        btnDodaj.setPrefWidth(120);
        btnDodaj.setOnAction(e -> {
            try {
                String nazwa = tfNazwa.getText().trim();
                if (nazwa.isEmpty()) {
                    blad("Błąd", "Nazwa nie może być pusta.");
                    return;
                }

                TypRozliczaniaDodatku typ = cbTyp.getValue();
                if (typ == null) {
                    blad("Błąd", "Wybierz typ rozliczania.");
                    return;
                }

                BigDecimal cena = new BigDecimal(tfCena.getText().trim());
                if (cena.compareTo(BigDecimal.ZERO) <= 0) {
                    blad("Błąd", "Cena musi być większa niż 0.");
                    return;
                }

                boolean dlaWszystkich = chkWszystkieKategorie.isSelected();
                List<KategoriaSamochodu> kategorie = dlaWszystkich
                        ? new ArrayList<>()
                        : new ArrayList<>(listaKategorii.getSelectionModel().getSelectedItems());

                if (!dlaWszystkich && kategorie.isEmpty()) {
                    blad("Błąd", "Wybierz przynajmniej jedną kategorię albo zaznacz 'Dostępny dla wszystkich kategorii'.");
                    return;
                }

                Long noweId = wygenerujNoweId();

                Dodatek nowy = new Dodatek(noweId, nazwa, cena, typ, kategorie);

                dodatekService.dodajDodatek(nowy);

                dodano = true;
                info("Sukces", "Dodano nowy dodatek.");
                dialog.close();

            } catch (NumberFormatException ex) {
                blad("Błąd", "Cena musi być liczbą.");
            } catch (IllegalArgumentException ex) {
                blad("Błąd", ex.getMessage());
            } catch (Exception ex) {
                blad("Błąd", "Nie udało się dodać dodatku: " + ex.getMessage());
            }
        });

        Button btnAnuluj = new Button("Anuluj");
        btnAnuluj.setPrefWidth(120);
        btnAnuluj.setOnAction(e -> dialog.close());

        btnBox.getChildren().addAll(btnDodaj, btnAnuluj);

        root.getChildren().addAll(lblTytul, new Separator(), pola, btnBox);

        dialog.setScene(new Scene(root, 460, 520));
        dialog.showAndWait();

        return dodano;
    }

    private Long wygenerujNoweId() {
        return dodatekService.pobierzWszystkieDodatki().stream()
                .map(Dodatek::getId)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    private VBox pole(String etykieta, Control control) {
        Label l = new Label(etykieta);
        l.setStyle("-fx-font-weight: bold;");
        VBox b = new VBox(4, l, control);
        control.setPrefWidth(300);
        return b;
    }

    private VBox kategoriePole(CheckBox chkWszystkie, ListView<KategoriaSamochodu> lista) {
        Label l = new Label("Kategorie (Ctrl+klik):");
        l.setStyle("-fx-font-weight: bold;");
        return new VBox(6, l, chkWszystkie, lista);
    }

    private void info(String t, String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK) {{
            setTitle(t);
            setHeaderText(null);
        }}.showAndWait();
    }

    private void blad(String t, String m) {
        new Alert(Alert.AlertType.ERROR, m, ButtonType.OK) {{
            setTitle(t);
            setHeaderText(null);
        }}.showAndWait();
    }
}