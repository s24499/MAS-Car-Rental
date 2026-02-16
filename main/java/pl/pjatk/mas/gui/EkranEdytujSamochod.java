package pl.pjatk.mas.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.Samochod;
import pl.pjatk.mas.service.FlotaService;

import java.time.Year;

public class EkranEdytujSamochod {

    private final FlotaService flotaService = new FlotaService();
    private boolean zmieniono;

    /**
     * Otwiera okno i zwraca true, jeśli samochód został zaktualizowany lub usunięty.
     */
    public boolean pokazDialog(Stage parentStage, Samochod samochod) {
        zmieniono = false;

        Stage dialog = utworzDialog(parentStage, samochod);

        Label tytul = new Label("Edycja samochodu");
        tytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextField tfMarka = new TextField(samochod.getMarka());
        TextField tfModel = new TextField(samochod.getModel());
        TextField tfNrRej = new TextField(samochod.getNumerRejestracyjny());
        TextField tfMoc = new TextField(String.valueOf(samochod.getMocKM()));
        TextField tfRocznik = new TextField(String.valueOf(samochod.getRocznik().getValue()));

        ComboBox<KategoriaSamochodu> cbKategoria = new ComboBox<>();
        cbKategoria.getItems().setAll(KategoriaSamochodu.values());
        cbKategoria.setValue(samochod.getKategoria());

        Label infoCennik = new Label("Cennik: przypisywany wg kategorii (logika w Service)");
        infoCennik.setStyle("-fx-font-weight: bold;");

        VBox pola = new VBox(12,
                pole("Marka:", tfMarka),
                pole("Model:", tfModel),
                pole("Numer rejestracyjny:", tfNrRej),
                pole("Moc (KM):", tfMoc),
                pole("Rocznik (YYYY):", tfRocznik),
                pole("Kategoria:", cbKategoria),
                infoCennik
        );

        Button btnZapisz = new Button("Zapisz zmiany");
        btnZapisz.setPrefWidth(120);
        btnZapisz.setOnAction(e -> obsluzZapis(dialog, samochod, tfMarka, tfModel, tfNrRej, tfMoc, tfRocznik, cbKategoria));

        Button btnUsun = new Button("Usuń samochód");
        btnUsun.setPrefWidth(120);
        btnUsun.setStyle("-fx-text-fill: white; -fx-background-color: #d32f2f;");
        btnUsun.setOnAction(e -> obsluzUsuniecie(dialog, samochod));

        Button btnAnuluj = new Button("Anuluj");
        btnAnuluj.setPrefWidth(120);
        btnAnuluj.setOnAction(e -> dialog.close());

        HBox przyciski = new HBox(10, btnZapisz, btnUsun, btnAnuluj);
        przyciski.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15, tytul, new Separator(), pola, new Separator(), przyciski);
        root.setPadding(new Insets(20));

        dialog.setScene(new Scene(root, 520, 540));
        dialog.showAndWait();

        return zmieniono;
    }

    /**
     * Tworzy okno dialogu.
     */
    private Stage utworzDialog(Stage parentStage, Samochod samochod) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Edytuj samochód: " + samochod);
        dialog.setResizable(false);
        return dialog;
    }

    /**
     * Obsługuje zapis zmian (GUI parsuje liczby, Service waliduje i aktualizuje).
     */
    private void obsluzZapis(Stage dialog,
                             Samochod samochod,
                             TextField tfMarka,
                             TextField tfModel,
                             TextField tfNrRej,
                             TextField tfMoc,
                             TextField tfRocznik,
                             ComboBox<KategoriaSamochodu> cbKategoria) {
        try {
            int moc = parseInt(tfMoc.getText(), "Moc i rocznik muszą być liczbami.");
            int rocznikInt = parseInt(tfRocznik.getText(), "Moc i rocznik muszą być liczbami.");

            flotaService.aktualizujSamochod(
                    samochod.getId(),
                    tfMarka.getText(),
                    tfModel.getText(),
                    tfNrRej.getText(),
                    moc,
                    Year.of(rocznikInt),
                    cbKategoria.getValue()
            );

            zmieniono = true;
            pokazInfo("Sukces", "Samochód został zaktualizowany.");
            dialog.close();
        } catch (IllegalArgumentException ex) {
            pokazBlad("Błąd", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie udało się zapisać zmian: " + ex.getMessage());
        }
    }

    /**
     * Obsługuje usunięcie samochodu (z potwierdzeniem).
     */
    private void obsluzUsuniecie(Stage dialog, Samochod samochod) {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Czy na pewno chcesz usunąć samochód?\n\n" + samochod,
                ButtonType.OK,
                ButtonType.CANCEL
        );
        confirm.setTitle("Potwierdzenie usunięcia");
        confirm.setHeaderText(null);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            flotaService.usunSamochod(samochod.getId());

            zmieniono = true;
            pokazInfo("Sukces", "Samochód został usunięty.");
            dialog.close();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            pokazBlad("Błąd", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie udało się usunąć samochodu: " + ex.getMessage());
        }
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
     * Parsuje liczbę całkowitą z komunikatem dla użytkownika.
     */
    private int parseInt(String txt, String bladMsg) {
        try {
            return Integer.parseInt(txt.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(bladMsg);
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