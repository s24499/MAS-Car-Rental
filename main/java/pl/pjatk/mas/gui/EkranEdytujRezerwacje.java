package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.Rezerwacja;
import pl.pjatk.mas.model.StatusRezerwacji;
import pl.pjatk.mas.service.DodatekService;
import pl.pjatk.mas.service.RezerwacjaService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog do pełnej edycji rezerwacji.
 * Umożliwia edycję wszystkich parametrów (klienta, samochodu, dat, dodatków, ceny, statusu).
 * UWAGA: Ta klasa zawiera TYLKO logikę GUI, cała logika biznesowa jest w Service!
 */
public class EkranEdytujRezerwacje {

    private final RezerwacjaService rezerwacjaService = new RezerwacjaService();
    private final DodatekService dodatekService = new DodatekService();
    private boolean zmieniono = false;

    /**
     * Wyświetla dialog edycji rezerwacji.
     * @param parentStage okno nadrzędne
     * @param rezerwacja rezerwacja do edycji
     * @return true jeśli coś zmieniono, false jeśli anulowano
     */
    public boolean pokazDialog(Stage parentStage, Rezerwacja rezerwacja) {
        this.zmieniono = false;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Edytuj rezerwację #" + rezerwacja.getId());
        dialog.setResizable(false);

        // Panel główny z ScrollPane (może być dużo pól)
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label lblTytul = new Label("Edycja rezerwacji #" + rezerwacja.getId());
        lblTytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        // --- Sekcja podstawowych informacji ---
        VBox sekcejaInfoBasowej = stworzSekcjeInfoBasowej(rezerwacja);

        // --- Sekcja edycji dat ---
        DatePicker dpDataOd = new DatePicker();
        dpDataOd.setValue(rezerwacja.getDataOd());

        DatePicker dpDataDo = new DatePicker();
        dpDataDo.setValue(rezerwacja.getDataDo());

        VBox sekcjaDat = stworzSekcjeDat(dpDataOd, dpDataDo);

        // --- Sekcja edycji dodatków ---
        ListView<Dodatek> listaWszystkichDodatkow = new ListView<>();
        List<Dodatek> wszystkieDodatki = dodatekService.pobierzWszystkieDodatki();
        listaWszystkichDodatkow.setItems(FXCollections.observableArrayList(wszystkieDodatki));
        listaWszystkichDodatkow.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaWszystkichDodatkow.setPrefHeight(100);

        // Zaznacz aktualnie wybrane dodatki
        for (Dodatek d : rezerwacja.getDodatki()) {
            listaWszystkichDodatkow.getSelectionModel().select(d);
        }

        VBox sekcjaDodatkow = stworzSekcjeDodatkow(listaWszystkichDodatkow);

        // --- Sekcja szacowanej ceny ---
        Label lblSzacowanaFena = new Label("Szacowana cena:");
        lblSzacowanaFena.setStyle("-fx-font-weight: bold;");
        Label lblWartoscCeny = new Label(rezerwacja.getCenaCalkowita() + " zł");
        lblWartoscCeny.setStyle("-fx-font-size: 12; -fx-text-fill: #0066cc;");

        VBox sekcjaCeny = new VBox(5);
        sekcjaCeny.getChildren().addAll(lblSzacowanaFena, lblWartoscCeny);

        // --- Sekcja edycji statusu ---
        ComboBox<StatusRezerwacji> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll(StatusRezerwacji.values());
        cbStatus.setValue(rezerwacja.getStatus());
        cbStatus.setPrefWidth(200);

        VBox sekcjaStatusu = stworzSekcjeStatusu(cbStatus);

        // --- Sekcja przycisków ---
        HBox btnBox = stworzSeckePrzyciskow(dialog, rezerwacja, dpDataOd, dpDataDo,
                listaWszystkichDodatkow, cbStatus, lblWartoscCeny);

        // Dodaj wszystko do głównego panelu
        root.getChildren().addAll(
                lblTytul,
                new Separator(),
                sekcejaInfoBasowej,
                sekcjaDat,
                sekcjaDodatkow,
                sekcjaCeny,
                sekcjaStatusu,
                new Separator(),
                btnBox
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 600, 800);
        dialog.setScene(scene);
        dialog.showAndWait();

        return zmieniono;
    }

    /**
     * Tworzy sekcję informacji podstawowych (TYLKO GUI)
     */
    private VBox stworzSekcjeInfoBasowej(Rezerwacja rezerwacja) {
        VBox infoBox = new VBox(5);
        infoBox.setPadding(new Insets(10));
        infoBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5;");

        Label lblAuto = new Label("Samochód: " + rezerwacja.getSamochod().getMarka() + " " +
                rezerwacja.getSamochod().getModel());
        lblAuto.setStyle("-fx-font-weight: bold;");

        Label lblKlient = new Label("Klient: " + rezerwacja.getKlient().getImie() + " " +
                rezerwacja.getKlient().getNazwisko());

        infoBox.getChildren().addAll(lblAuto, lblKlient);
        return infoBox;
    }

    /**
     * Tworzy sekcję edycji dat (TYLKO GUI)
     */
    private VBox stworzSekcjeDat(DatePicker dpDataOd, DatePicker dpDataDo) {
        VBox box = new VBox(10);

        Label lblDaty = new Label("Daty rezerwacji:");
        lblDaty.setStyle("-fx-font-weight: bold;");

        HBox dataPickers = new HBox(15);
        VBox boxOd = new VBox(5);
        Label lblOd = new Label("Data Od:");
        boxOd.getChildren().addAll(lblOd, dpDataOd);

        VBox boxDo = new VBox(5);
        Label lblDo = new Label("Data Do:");
        boxDo.getChildren().addAll(lblDo, dpDataDo);

        dataPickers.getChildren().addAll(boxOd, boxDo);
        box.getChildren().addAll(lblDaty, dataPickers);

        return box;
    }

    /**
     * Tworzy sekcję edycji dodatków (TYLKO GUI)
     */
    private VBox stworzSekcjeDodatkow(ListView<Dodatek> listaWszystkichDodatkow) {
        VBox box = new VBox(8);

        Label lblDodatki = new Label("Dodatki (Ctrl+klik aby wybrać wiele):");
        lblDodatki.setStyle("-fx-font-weight: bold;");

        box.getChildren().addAll(lblDodatki, listaWszystkichDodatkow);
        VBox.setVgrow(listaWszystkichDodatkow, Priority.ALWAYS);

        return box;
    }

    /**
     * Tworzy sekcję edycji statusu (TYLKO GUI)
     */
    private VBox stworzSekcjeStatusu(ComboBox<StatusRezerwacji> cbStatus) {
        VBox box = new VBox(8);

        Label lblStatus = new Label("Status rezerwacji:");
        lblStatus.setStyle("-fx-font-weight: bold;");

        box.getChildren().addAll(lblStatus, cbStatus);

        return box;
    }

    /**
     * Tworzy sekcję przycisków (TYLKO GUI)
     */
    private HBox stworzSeckePrzyciskow(Stage dialog, Rezerwacja rezerwacja,
                                       DatePicker dpDataOd, DatePicker dpDataDo,
                                       ListView<Dodatek> listaWszystkichDodatkow,
                                       ComboBox<StatusRezerwacji> cbStatus,
                                       Label lblWartoscCeny) {
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnZapisz = new Button("Zapisz zmiany");
        btnZapisz.setPrefWidth(120);
        btnZapisz.setStyle("-fx-font-size: 11;");
        btnZapisz.setOnAction(e -> obslugazapiszWszystkieZmiany(
                rezerwacja,
                dpDataOd.getValue(),
                dpDataDo.getValue(),
                new ArrayList<>(listaWszystkichDodatkow.getSelectionModel().getSelectedItems()),
                cbStatus.getValue(),
                lblWartoscCeny
        ));

        Button btnUsun = new Button("Usuń rezerwację");
        btnUsun.setPrefWidth(120);
        btnUsun.setStyle("-fx-font-size: 11; -fx-text-fill: white; -fx-background-color: #d32f2f;");
        btnUsun.setOnAction(e -> obslugazaUsunRezerwacje(rezerwacja, dialog));

        Button btnAnuluj = new Button("Anuluj");
        btnAnuluj.setPrefWidth(120);
        btnAnuluj.setStyle("-fx-font-size: 11;");
        btnAnuluj.setOnAction(e -> dialog.close());

        btnBox.getChildren().addAll(btnZapisz, btnUsun, btnAnuluj);
        return btnBox;
    }

    /**
     * Obsługuje zapisanie WSZYSTKICH zmian (logika w Service!)
     */
    private void obslugazapiszWszystkieZmiany(Rezerwacja rezerwacja,
                                              LocalDate nowaDataOd,
                                              LocalDate nowaDataDo,
                                              List<Dodatek> noweDodatki,
                                              StatusRezerwacji nowyStatus,
                                              Label lblWartoscCeny) {
        try {
            // Walidacja GUI (tylko konieczna do komunikatu użytkownika)
            if (nowaDataOd == null || nowaDataDo == null) {
                pokazBlad("Błąd", "Daty nie mogą być puste");
                return;
            }

            if (nowaDataDo.isBefore(nowaDataOd)) {
                pokazBlad("Błąd", "Data Do nie może być wcześniejsza niż Data Od");
                return;
            }

            // Wszystka logika biznesowa w Service!
            rezerwacjaService.aktualizujRezerwacje(
                    rezerwacja.getId(),
                    nowaDataOd,
                    nowaDataDo,
                    noweDodatki,
                    nowyStatus
            );

            zmieniono = true;
            pokazKomunikat("Sukces", "Rezerwacja została zaktualizowana");
        } catch (IllegalArgumentException ex) {
            pokazBlad("Błąd walidacji", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie udało się zaktualizować rezerwacji: " + ex.getMessage());
        }
    }

    /**
     * Obsługuje usunięcie rezerwacji (logika w Service!)
     */
    private void obslugazaUsunRezerwacje(Rezerwacja rezerwacja, Stage dialog) {
        Alert potwierdzenie = new Alert(Alert.AlertType.CONFIRMATION);
        potwierdzenie.setTitle("Potwierdzenie usunięcia");
        potwierdzenie.setHeaderText("Czy na pewno chcesz usunąć tę rezerwację?");
        potwierdzenie.setContentText("Nr: " + rezerwacja.getId() + "\n" +
                rezerwacja.getSamochod().getMarka() + " " + rezerwacja.getSamochod().getModel());

        if (potwierdzenie.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                rezerwacjaService.usunRezerwacje(rezerwacja.getId());
                zmieniono = true;
                pokazKomunikat("Sukces", "Rezerwacja została usunięta");
                dialog.close();
            } catch (Exception ex) {
                pokazBlad("Błąd", "Nie udało się usunąć rezerwacji: " + ex.getMessage());
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