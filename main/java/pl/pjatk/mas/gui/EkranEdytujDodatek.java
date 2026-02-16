package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.TypRozliczaniaDodatku;
import pl.pjatk.mas.service.DodatekService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EkranEdytujDodatek {

    private final DodatekService dodatekService = new DodatekService();
    private boolean zmieniono;

    /**
     * Otwiera okno i zwraca true, jeśli dodatek został zmieniony/usunięty.
     */
    public boolean pokazDialog(Stage parentStage, Dodatek dodatek) {
        zmieniono = false;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Edytuj dodatek: " + dodatek.getNazwa());
        dialog.setResizable(false);

        Label tytul = new Label("Edycja dodatku");
        tytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextField tfNazwa = new TextField(dodatek.getNazwa());
        tfNazwa.setPrefWidth(250);

        TextField tfCena = new TextField(dodatek.getCena().toString());
        tfCena.setPrefWidth(250);

        ComboBox<TypRozliczaniaDodatku> cbTyp = new ComboBox<>();
        cbTyp.getItems().setAll(TypRozliczaniaDodatku.values());
        cbTyp.setValue(dodatek.getTypRozliczania());
        cbTyp.setPrefWidth(250);

        CheckBox chkWszystkieKategorie = new CheckBox("Dostępny dla wszystkich kategorii");
        chkWszystkieKategorie.setStyle("-fx-font-weight: bold;");

        ListView<KategoriaSamochodu> listaKategorii = new ListView<>();
        listaKategorii.setPrefHeight(110);
        listaKategorii.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaKategorii.setItems(FXCollections.observableArrayList(KategoriaSamochodu.values()));
        listaKategorii.getItems().sort(Comparator.comparing(Enum::name));

        ustawKategorieStartowe(dodatek, chkWszystkieKategorie, listaKategorii);
        ustawTrybKategorii(chkWszystkieKategorie, listaKategorii);

        VBox formularz = new VBox(15,
                pole("Nazwa:", tfNazwa),
                pole("Cena:", tfCena),
                pole("Typ rozliczania:", cbTyp),
                poleKategorie(chkWszystkieKategorie, listaKategorii)
        );

        HBox przyciski = new HBox(10,
                przyciskZapisz(dialog, dodatek, tfNazwa, tfCena, cbTyp, chkWszystkieKategorie, listaKategorii),
                przyciskUsun(dialog, dodatek),
                przyciskAnuluj(dialog)
        );
        przyciski.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15, tytul, new Separator(), formularz, new Separator(), przyciski);
        root.setPadding(new Insets(20));

        dialog.setScene(new Scene(root, 450, 520));
        dialog.showAndWait();

        return zmieniono;
    }

    /**
     * Ustawia stan początkowy kategorii na podstawie edytowanego dodatku.
     */
    private void ustawKategorieStartowe(Dodatek dodatek,
                                        CheckBox chkWszystkieKategorie,
                                        ListView<KategoriaSamochodu> listaKategorii) {
        boolean dlaWszystkich = dodatek.getDostepneKategorie() == null || dodatek.getDostepneKategorie().isEmpty();
        chkWszystkieKategorie.setSelected(dlaWszystkich);
        listaKategorii.setDisable(dlaWszystkich);

        if (!dlaWszystkich && dodatek.getDostepneKategorie() != null) {
            for (KategoriaSamochodu kat : dodatek.getDostepneKategorie()) {
                listaKategorii.getSelectionModel().select(kat);
            }
        }
    }

    /**
     * Ustawia zachowanie UI: checkbox "wszystkie" włącza/wyłącza listę kategorii.
     */
    private void ustawTrybKategorii(CheckBox chkWszystkieKategorie, ListView<KategoriaSamochodu> listaKategorii) {
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
    private VBox pole(String etykieta, Control kontrolka) {
        Label lbl = new Label(etykieta);
        lbl.setStyle("-fx-font-weight: bold;");
        return new VBox(5, lbl, kontrolka);
    }

    /**
     * Buduje sekcję formularza dla kategorii.
     */
    private VBox poleKategorie(CheckBox chk, ListView<KategoriaSamochodu> lista) {
        Label lbl = new Label("Kategorie pojazdu (Ctrl+klik):");
        lbl.setStyle("-fx-font-weight: bold;");
        return new VBox(6, lbl, chk, lista);
    }

    /**
     * Tworzy przycisk zapisujący zmiany dodatku.
     */
    private Button przyciskZapisz(Stage dialog,
                                  Dodatek dodatek,
                                  TextField tfNazwa,
                                  TextField tfCena,
                                  ComboBox<TypRozliczaniaDodatku> cbTyp,
                                  CheckBox chkWszystkieKategorie,
                                  ListView<KategoriaSamochodu> listaKategorii) {
        Button btn = new Button("Zapisz zmiany");
        btn.setPrefWidth(120);
        btn.setStyle("-fx-font-size: 11;");
        btn.setOnAction(e -> obsluzZapisz(dialog, dodatek, tfNazwa, tfCena, cbTyp, chkWszystkieKategorie, listaKategorii));
        return btn;
    }

    /**
     * Tworzy przycisk usuwający dodatek.
     */
    private Button przyciskUsun(Stage dialog, Dodatek dodatek) {
        Button btn = new Button("Usuń dodatek");
        btn.setPrefWidth(120);
        btn.setStyle("-fx-font-size: 11; -fx-text-fill: white; -fx-background-color: #d32f2f;");
        btn.setOnAction(e -> obsluzUsun(dialog, dodatek));
        return btn;
    }

    /**
     * Tworzy przycisk anulujący (zamyka okno).
     */
    private Button przyciskAnuluj(Stage dialog) {
        Button btn = new Button("Anuluj");
        btn.setPrefWidth(120);
        btn.setStyle("-fx-font-size: 11;");
        btn.setOnAction(e -> dialog.close());
        return btn;
    }

    /**
     * Obsługuje zapis (GUI parsuje cenę, Service waliduje i aktualizuje).
     */
    private void obsluzZapisz(Stage dialog,
                              Dodatek dodatek,
                              TextField tfNazwa,
                              TextField tfCena,
                              ComboBox<TypRozliczaniaDodatku> cbTyp,
                              CheckBox chkWszystkieKategorie,
                              ListView<KategoriaSamochodu> listaKategorii) {
        try {
            BigDecimal cena = parseCena(tfCena.getText());

            List<KategoriaSamochodu> kategorie = chkWszystkieKategorie.isSelected()
                    ? new ArrayList<>()
                    : new ArrayList<>(listaKategorii.getSelectionModel().getSelectedItems());

            dodatekService.aktualizujDodatek(dodatek.getId(), tfNazwa.getText(), cena, cbTyp.getValue(), kategorie);

            zmieniono = true;
            pokazInfo("Sukces", "Dodatek został zaktualizowany");
            dialog.close();
        } catch (IllegalArgumentException ex) {
            pokazBlad("Błąd", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie udało się zaktualizować dodatku: " + ex.getMessage());
        }
    }

    /**
     * Obsługuje usunięcie dodatku (z potwierdzeniem).
     */
    private void obsluzUsun(Stage dialog, Dodatek dodatek) {
        Alert potwierdzenie = new Alert(Alert.AlertType.CONFIRMATION,
                "Czy na pewno chcesz usunąć ten dodatek?\n\nNazwa: " + dodatek.getNazwa(),
                ButtonType.OK, ButtonType.CANCEL);
        potwierdzenie.setTitle("Potwierdzenie usunięcia");
        potwierdzenie.setHeaderText(null);

        if (potwierdzenie.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            dodatekService.usunDodatek(dodatek.getId());
            zmieniono = true;
            pokazInfo("Sukces", "Dodatek został usunięty");
            dialog.close();
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie udało się usunąć dodatku: " + ex.getMessage());
        }
    }

    /**
     * Parsuje cenę wprowadzoną przez użytkownika.
     */
    private BigDecimal parseCena(String txt) {
        try {
            return new BigDecimal(txt.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cena musi być liczbą (np. 10.00)");
        }
    }

    /**
     * Pokazuje komunikat informacyjny.
     */
    private void pokazInfo(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }

    /**
     * Pokazuje komunikat błędu.
     */
    private void pokazBlad(String tytul, String tresc) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(tytul);
        alert.setHeaderText(null);
        alert.setContentText(tresc);
        alert.showAndWait();
    }
}