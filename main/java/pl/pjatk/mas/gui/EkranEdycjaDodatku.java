package pl.pjatk.mas.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.TypRozliczaniaDodatku;
import pl.pjatk.mas.service.DodatekService;

import java.math.BigDecimal;

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

        // Panel główny
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label lblTytul = new Label("Edycja dodatku");
        lblTytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        // Sekcja pól edycji
        TextField tfNazwa = new TextField();
        tfNazwa.setText(dodatek.getNazwa());
        tfNazwa.setPrefWidth(250);

        TextField tfCena = new TextField();
        tfCena.setText(dodatek.getCena().toString());
        tfCena.setPrefWidth(250);

        ComboBox<TypRozliczaniaDodatku> cbTyp = new ComboBox<>();
        cbTyp.getItems().addAll(TypRozliczaniaDodatku.values());
        cbTyp.setValue(dodatek.getTypRozliczania());
        cbTyp.setPrefWidth(250);

        VBox poleEdycji = stworzPoleEdycji(tfNazwa, tfCena, cbTyp);

        // Sekcja przycisków
        HBox btnBox = stworzSeckePrzyciskow(dialog, dodatek, tfNazwa, tfCena, cbTyp);

        root.getChildren().addAll(
                lblTytul,
                new Separator(),
                poleEdycji,
                btnBox
        );

        Scene scene = new Scene(root, 450, 400);
        dialog.setScene(scene);
        dialog.showAndWait();

        return zmieniono;
    }

    private VBox stworzPoleEdycji(TextField tfNazwa, TextField tfCena, ComboBox<TypRozliczaniaDodatku> cbTyp) {
        VBox boxNazwa = new VBox(5);
        Label lblNazwa = new Label("Nazwa:");
        lblNazwa.setStyle("-fx-font-weight: bold;");
        boxNazwa.getChildren().addAll(lblNazwa, tfNazwa);

        VBox boxCena = new VBox(5);
        Label lblCena = new Label("Cena:");
        lblCena.setStyle("-fx-font-weight: bold;");
        boxCena.getChildren().addAll(lblCena, tfCena);

        VBox boxTyp = new VBox(5);
        Label lblTyp = new Label("Typ rozliczania:");
        lblTyp.setStyle("-fx-font-weight: bold;");
        boxTyp.getChildren().addAll(lblTyp, cbTyp);

        VBox poleEdycji = new VBox(15);
        poleEdycji.getChildren().addAll(boxNazwa, boxCena, boxTyp);
        return poleEdycji;
    }

    private HBox stworzSeckePrzyciskow(Stage dialog, Dodatek dodatek, TextField tfNazwa, TextField tfCena, ComboBox<TypRozliczaniaDodatku> cbTyp) {
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnZapisz = new Button("Zapisz zmiany");
        btnZapisz.setPrefWidth(120);
        btnZapisz.setStyle("-fx-font-size: 11;");
        btnZapisz.setOnAction(e -> obslugaZapiszZmiany(dodatek, tfNazwa.getText(),
                tfCena.getText(), cbTyp.getValue()));

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

    private void obslugaZapiszZmiany(Dodatek dodatek, String nazwa, String cena, TypRozliczaniaDodatku typ) {
        try {
            // Walidacja GUI (tylko wymagana do komunikatu użytkownikowi)
            if (nazwa.trim().isEmpty()) {
                pokazBlad("Błąd", "Nazwa nie może być pusta");
                return;
            }

            BigDecimal cenaBigDecimal = new BigDecimal(cena.trim());

            // Logika biznesowa (wszystko w Service!)
            dodatekService.aktualizujDodatek(dodatek.getId(), nazwa.trim(),
                    cenaBigDecimal, typ);
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