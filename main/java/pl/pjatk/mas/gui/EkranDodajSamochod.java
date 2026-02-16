package pl.pjatk.mas.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import pl.pjatk.mas.service.FlotaService;

import java.time.Year;
import java.util.Comparator;

public class EkranDodajSamochod {

    private final FlotaService flotaService = new FlotaService();
    private boolean dodano;

    /**
     * Otwiera okno i zwraca true, jeśli samochód został dodany.
     */
    public boolean pokazDialog(Stage parentStage) {
        dodano = false;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Dodaj samochód");
        dialog.setResizable(false);

        Label tytul = new Label("Dodawanie samochodu");
        tytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextField tfMarka = new TextField();
        TextField tfModel = new TextField();
        TextField tfNrRej = new TextField();
        TextField tfMoc = new TextField();
        TextField tfRocznik = new TextField();

        ComboBox<KategoriaSamochodu> cbKategoria = new ComboBox<>();
        cbKategoria.getItems().setAll(KategoriaSamochodu.values());
        cbKategoria.getItems().sort(Comparator.comparing(Enum::name));

        Label infoCennik = new Label("Cennik: zostanie przypisany na podstawie kategorii");
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

        Button btnDodaj = new Button("Dodaj");
        btnDodaj.setPrefWidth(120);
        btnDodaj.setOnAction(e -> obsluzDodanie(dialog, tfMarka, tfModel, tfNrRej, tfMoc, tfRocznik, cbKategoria));

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
     * Obsługuje kliknięcie "Dodaj" (GUI parsuje liczby, Service waliduje i zapisuje).
     */
    private void obsluzDodanie(Stage dialog,
                               TextField tfMarka,
                               TextField tfModel,
                               TextField tfNrRej,
                               TextField tfMoc,
                               TextField tfRocznik,
                               ComboBox<KategoriaSamochodu> cbKategoria) {
        try {
            String marka = tfMarka.getText();
            String model = tfModel.getText();
            String nrRej = tfNrRej.getText();

            int moc = parseInt(tfMoc.getText(), "Moc i rocznik muszą być liczbami.");
            int rocznikInt = parseInt(tfRocznik.getText(), "Moc i rocznik muszą być liczbami.");

            KategoriaSamochodu kategoria = cbKategoria.getValue();

            flotaService.dodajSamochod(marka, model, nrRej, moc, Year.of(rocznikInt), kategoria);

            dodano = true;
            pokazInfo("Sukces", "Samochód został dodany.");
            dialog.close();
        } catch (IllegalArgumentException ex) {
            pokazBlad("Błąd walidacji", ex.getMessage());
        } catch (Exception ex) {
            pokazBlad("Błąd", "Nie udało się dodać samochodu: " + ex.getMessage());
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