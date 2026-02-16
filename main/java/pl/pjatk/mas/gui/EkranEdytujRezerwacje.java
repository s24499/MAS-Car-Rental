package pl.pjatk.mas.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
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

public class EkranEdytujRezerwacje {

    private final RezerwacjaService rezerwacjaService = new RezerwacjaService();
    private final DodatekService dodatekService = new DodatekService();
    private boolean zmieniono;

    /**
     * Otwiera okno i zwraca true, jeśli rezerwacja została zaktualizowana.
     */
    public boolean pokazDialog(Stage parentStage, Rezerwacja rezerwacja) {
        zmieniono = false;

        Stage dialog = utworzDialog(parentStage, rezerwacja);

        Label tytul = new Label("Edycja rezerwacji #" + rezerwacja.getId());
        tytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextArea info = zbudujInfoReadonly(rezerwacja);

        DatePicker dpDataOd = new DatePicker(rezerwacja.getDataOd());
        DatePicker dpDataDo = new DatePicker(rezerwacja.getDataDo());
        VBox sekcjaDat = new VBox(8, new Label("Data od:"), dpDataOd, new Label("Data do:"), dpDataDo);

        ListView<Dodatek> listaDodatkow = zbudujListeDodatkow(rezerwacja);
        VBox sekcjaDodatkow = new VBox(8, new Label("Dodatki (Ctrl+klik):"), listaDodatkow);

        ComboBox<StatusRezerwacji> cbStatus = new ComboBox<>();
        cbStatus.getItems().setAll(StatusRezerwacji.values());
        cbStatus.setValue(rezerwacja.getStatus());
        VBox sekcjaStatusu = new VBox(8, new Label("Status:"), cbStatus);

        Button btnZapisz = new Button("Zapisz");
        btnZapisz.setPrefWidth(120);
        btnZapisz.setOnAction(e -> obsluzZapis(dialog, rezerwacja, dpDataOd, dpDataDo, cbStatus, listaDodatkow));

        Button btnAnuluj = new Button("Anuluj");
        btnAnuluj.setPrefWidth(120);
        btnAnuluj.setOnAction(e -> dialog.close());

        HBox przyciski = new HBox(10, btnZapisz, btnAnuluj);
        przyciski.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(listaDodatkow, Priority.ALWAYS);

        VBox root = new VBox(15,
                tytul,
                new Separator(),
                new Label("Informacje:"), info,
                new Separator(),
                sekcjaDat,
                new Separator(),
                sekcjaDodatkow,
                new Separator(),
                sekcjaStatusu,
                new Separator(),
                przyciski
        );
        root.setPadding(new Insets(20));

        dialog.setScene(new Scene(root, 520, 720));
        dialog.showAndWait();

        return zmieniono;
    }

    /**
     * Tworzy okno dialogu.
     */
    private Stage utworzDialog(Stage parentStage, Rezerwacja rezerwacja) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Edytuj rezerwację #" + rezerwacja.getId());
        dialog.setResizable(false);
        return dialog;
    }

    /**
     * Buduje pole tekstowe z informacjami o rezerwacji (tylko do odczytu).
     */
    private TextArea zbudujInfoReadonly(Rezerwacja rezerwacja) {
        TextArea info = new TextArea();
        info.setEditable(false);
        info.setWrapText(true);
        info.setPrefHeight(110);

        String klientTxt = rezerwacja.getKlient() != null ? rezerwacja.getKlient().toString() : "brak";
        String autoTxt = rezerwacja.getSamochod() != null ? rezerwacja.getSamochod().toString() : "brak";

        info.setText(
                "Klient: " + klientTxt + "\n" +
                        "Samochód: " + autoTxt + "\n" +
                        "Aktualny termin: " + rezerwacja.getDataOd() + " - " + rezerwacja.getDataDo() + "\n" +
                        "Aktualny status: " + rezerwacja.getStatus() + "\n" +
                        "Aktualna cena: " + rezerwacja.getCenaCalkowita() + " zł"
        );

        return info;
    }

    /**
     * Buduje listę dodatków oraz zaznacza dodatki aktualnie przypisane do rezerwacji.
     */
    private ListView<Dodatek> zbudujListeDodatkow(Rezerwacja rezerwacja) {
        ListView<Dodatek> lista = new ListView<>();
        lista.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lista.setPrefHeight(120);

        List<Dodatek> wszystkie = dodatekService.pobierzWszystkieDodatki();
        lista.setItems(FXCollections.observableArrayList(wszystkie));

        if (rezerwacja.getDodatki() != null) {
            for (Dodatek d : rezerwacja.getDodatki()) {
                lista.getSelectionModel().select(d);
            }
        }

        return lista;
    }

    /**
     * Obsługuje zapis zmian (GUI pobiera dane z kontrolek, Service waliduje i aktualizuje).
     */
    private void obsluzZapis(Stage dialog,
                             Rezerwacja rezerwacja,
                             DatePicker dpDataOd,
                             DatePicker dpDataDo,
                             ComboBox<StatusRezerwacji> cbStatus,
                             ListView<Dodatek> listaDodatkow) {
        LocalDate nowaOd = dpDataOd.getValue();
        LocalDate nowaDo = dpDataDo.getValue();
        StatusRezerwacji nowyStatus = cbStatus.getValue();
        List<Dodatek> noweDodatki = new ArrayList<>(listaDodatkow.getSelectionModel().getSelectedItems());

        try {
            rezerwacjaService.aktualizujRezerwacje(
                    rezerwacja.getId(),
                    nowaOd,
                    nowaDo,
                    noweDodatki,
                    nowyStatus
            );

            zmieniono = true;
            pokazInfo("Sukces", "Rezerwacja została zaktualizowana.");
            dialog.close();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            pokazBlad("Błąd", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd systemowy", "Nieoczekiwany błąd: " + ex.getMessage());
        }
    }

    /**
     * Pokazuje komunikat informacyjny.
     */
    private void pokazInfo(String tytul, String tresc) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, tresc, ButtonType.OK);
        a.setTitle(tytul);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /**
     * Pokazuje komunikat błędu.
     */
    private void pokazBlad(String tytul, String tresc) {
        Alert a = new Alert(Alert.AlertType.ERROR, tresc, ButtonType.OK);
        a.setTitle(tytul);
        a.setHeaderText(null);
        a.showAndWait();
    }
}