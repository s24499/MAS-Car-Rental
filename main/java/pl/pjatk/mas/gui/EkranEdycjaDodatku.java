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

/**
 * Dialog do edycji dodatków.
 * UWAGA: Ta klasa zawiera TYLKO logikę GUI, cała logika biznesowa jest w Service!
 */
public class EkranEdycjaDodatku {

    private final DodatekService dodatekService = new DodatekService();
    private boolean zmieniono = false;

    public boolean pokazDialog(Stage parentStage, Dodatek dodatek) {
        this.zmieniono = false;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Edytuj dodatek: " + dodatek.getNazwa());
        dialog.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label lblTytul = new Label("Edycja dodatku");
        lblTytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        // --- Pola podstawowe ---
        TextField tfNazwa = new TextField(dodatek.getNazwa());
        tfNazwa.setPrefWidth(250);

        TextField tfCena = new TextField(dodatek.getCena().toString());
        tfCena.setPrefWidth(250);

        ComboBox<TypRozliczaniaDodatku> cbTyp = new ComboBox<>();
        cbTyp.getItems().addAll(TypRozliczaniaDodatku.values());
        cbTyp.setValue(dodatek.getTypRozliczania());
        cbTyp.setPrefWidth(250);

        // --- NOWE: Kategorie ---
        CheckBox chkWszystkieKategorie = new CheckBox("Dostępny dla wszystkich kategorii");
        chkWszystkieKategorie.setStyle("-fx-font-weight: bold;");

        ListView<KategoriaSamochodu> listaKategorii = new ListView<>();
        listaKategorii.setPrefHeight(110);
        listaKategorii.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaKategorii.setItems(FXCollections.observableArrayList(KategoriaSamochodu.values()));
        listaKategorii.getItems().sort(Comparator.comparing(Enum::name));

        boolean dlaWszystkich = dodatek.getDostepneKategorie() == null || dodatek.getDostepneKategorie().isEmpty();
        chkWszystkieKategorie.setSelected(dlaWszystkich);
        listaKategorii.setDisable(dlaWszystkich);

        if (!dlaWszystkich && dodatek.getDostepneKategorie() != null) {
            for (KategoriaSamochodu kat : dodatek.getDostepneKategorie()) {
                listaKategorii.getSelectionModel().select(kat);
            }
        }

        chkWszystkieKategorie.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                listaKategorii.getSelectionModel().clearSelection();
                listaKategorii.setDisable(true);
            } else {
                listaKategorii.setDisable(false);
            }
        });

        VBox poleEdycji = stworzPoleEdycji(tfNazwa, tfCena, cbTyp, chkWszystkieKategorie, listaKategorii);

        // Przyciski
        HBox btnBox = stworzSeckePrzyciskow(dialog, dodatek, tfNazwa, tfCena, cbTyp, chkWszystkieKategorie, listaKategorii);

        root.getChildren().addAll(lblTytul, new Separator(), poleEdycji, btnBox);

        Scene scene = new Scene(root, 450, 520);
        dialog.setScene(scene);
        dialog.showAndWait();

        return zmieniono;
    }

    private VBox stworzPoleEdycji(TextField tfNazwa,
                                  TextField tfCena,
                                  ComboBox<TypRozliczaniaDodatku> cbTyp,
                                  CheckBox chkWszystkieKategorie,
                                  ListView<KategoriaSamochodu> listaKategorii) {

        VBox boxNazwa = new VBox(5, labelBold("Nazwa:"), tfNazwa);
        VBox boxCena = new VBox(5, labelBold("Cena:"), tfCena);
        VBox boxTyp = new VBox(5, labelBold("Typ rozliczania:"), cbTyp);

        Label lblKategorie = labelBold("Kategorie pojazdu (Ctrl+klik):");
        VBox boxKategorie = new VBox(6, lblKategorie, chkWszystkieKategorie, listaKategorii);

        VBox poleEdycji = new VBox(15);
        poleEdycji.getChildren().addAll(boxNazwa, boxCena, boxTyp, boxKategorie);
        return poleEdycji;
    }

    private Label labelBold(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private HBox stworzSeckePrzyciskow(Stage dialog,
                                       Dodatek dodatek,
                                       TextField tfNazwa,
                                       TextField tfCena,
                                       ComboBox<TypRozliczaniaDodatku> cbTyp,
                                       CheckBox chkWszystkieKategorie,
                                       ListView<KategoriaSamochodu> listaKategorii) {
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnZapisz = new Button("Zapisz zmiany");
        btnZapisz.setPrefWidth(120);
        btnZapisz.setStyle("-fx-font-size: 11;");
        btnZapisz.setOnAction(e -> obslugaZapiszZmiany(
                dodatek,
                tfNazwa.getText(),
                tfCena.getText(),
                cbTyp.getValue(),
                chkWszystkieKategorie.isSelected(),
                new ArrayList<>(listaKategorii.getSelectionModel().getSelectedItems())
        ));

        Button btnUsun = new Button("Usuń dodatek");
        btnUsun.setPrefWidth(120);
        btnUsun.setStyle("-fx-font-size: 11; -fx-text-fill: white; -fx-background-color: #d32f2f;");
        btnUsun.setOnAction(e -> obslugaUsunDodatek(dodatek, dialog));

        Button btnAnuluj = new Button("Anuluj");
        btnAnuluj.setPrefWidth(120);
        btnAnuluj.setStyle("-fx-font-size: 11;");
        btnAnuluj.setOnAction(e -> dialog.close());

        btnBox.getChildren().addAll(btnZapisz, btnUsun, btnAnuluj);
        return btnBox;
    }

    private void obslugaZapiszZmiany(Dodatek dodatek,
                                     String nazwa,
                                     String cena,
                                     TypRozliczaniaDodatku typ,
                                     boolean dlaWszystkichKategorii,
                                     List<KategoriaSamochodu> wybraneKategorie) {
        try {
            if (nazwa == null || nazwa.trim().isEmpty()) {
                pokazBlad("Błąd", "Nazwa nie może być pusta");
                return;
            }
            if (typ == null) {
                pokazBlad("Błąd", "Wybierz typ rozliczania");
                return;
            }

            BigDecimal cenaBigDecimal = new BigDecimal(cena.trim());

            if (!dlaWszystkichKategorii && (wybraneKategorie == null || wybraneKategorie.isEmpty())) {
                pokazBlad("Błąd", "Wybierz przynajmniej jedną kategorię albo zaznacz 'Dostępny dla wszystkich kategorii'");
                return;
            }

            List<KategoriaSamochodu> kategorieDoZapisu = dlaWszystkichKategorii
                    ? new ArrayList<>()
                    : new ArrayList<>(wybraneKategorie);

            // TU: zapis przez Service (wymaga metody z kategoriami)
            dodatekService.aktualizujDodatek(dodatek.getId(), nazwa.trim(), cenaBigDecimal, typ, kategorieDoZapisu);

            zmieniono = true;
            pokazKomunikat("Sukces", "Dodatek został zaktualizowany");
        } catch (NumberFormatException ex) {
            pokazBlad("Błąd", "Cena musi być liczbą");
        } catch (IllegalArgumentException ex) {
            pokazBlad("Błąd", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie udało się zaktualizować dodatku: " + ex.getMessage());
        }
    }

    private void obslugaUsunDodatek(Dodatek dodatek, Stage dialog) {
        Alert potwierdzenie = new Alert(Alert.AlertType.CONFIRMATION);
        potwierdzenie.setTitle("Potwierdzenie usunięcia");
        potwierdzenie.setHeaderText("Czy na pewno chcesz usunąć ten dodatek?");
        potwierdzenie.setContentText("Nazwa: " + dodatek.getNazwa());

        if (potwierdzenie.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                dodatekService.usunDodatek(dodatek.getId());
                zmieniono = true;
                pokazKomunikat("Sukces", "Dodatek został usunięty");
                dialog.close();
            } catch (Exception ex) {
                pokazBlad("Błąd", "Nie udało się usunąć dodatku: " + ex.getMessage());
            }
        }
    }

    private void pokazKomunikat(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }

    private void pokazBlad(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }
}