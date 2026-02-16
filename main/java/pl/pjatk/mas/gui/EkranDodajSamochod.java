package pl.pjatk.mas.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.pjatk.mas.dao.CennikDAO;
import pl.pjatk.mas.dao.SamochodDAO;
import pl.pjatk.mas.model.Cennik;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.Samochod;

import java.time.Year;
import java.util.Comparator;
import java.util.List;

public class EkranDodajSamochod {

    private final SamochodDAO samochodDAO = new SamochodDAO();
    private final CennikDAO cennikDAO = new CennikDAO();
    private boolean dodano = false;

    public boolean pokazDialog(Stage parentStage) {
        this.dodano = false;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Dodaj samochód");
        dialog.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label lblTytul = new Label("Dodawanie samochodu");
        lblTytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextField tfMarka = new TextField();
        TextField tfModel = new TextField();
        TextField tfNrRej = new TextField();
        TextField tfMoc = new TextField();
        TextField tfRocznik = new TextField();

        ComboBox<KategoriaSamochodu> cbKategoria = new ComboBox<>();
        cbKategoria.getItems().addAll(KategoriaSamochodu.values());
        cbKategoria.getItems().sort(Comparator.comparing(Enum::name));

        Label lblCennikInfo = new Label("Cennik: (wybierz kategorię)");
        lblCennikInfo.setStyle("-fx-font-weight: bold;");

        cbKategoria.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                lblCennikInfo.setText("Cennik: (wybierz kategorię)");
                return;
            }
            Cennik c = cennikDAO.znajdzPoKategorii(newVal);
            if (c == null) {
                lblCennikInfo.setText("Cennik: brak dla kategorii " + newVal);
            } else {
                lblCennikInfo.setText("Cennik: ID " + c.getId() + " / " + c.getStawkaZaDobe() + " zł/doba");
            }
        });

        VBox pola = new VBox(12,
                pole("Marka:", tfMarka),
                pole("Model:", tfModel),
                pole("Numer rejestracyjny:", tfNrRej),
                pole("Moc (KM):", tfMoc),
                pole("Rocznik (YYYY):", tfRocznik),
                pole("Kategoria:", cbKategoria),
                lblCennikInfo
        );

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnDodaj = new Button("Dodaj");
        btnDodaj.setPrefWidth(120);
        btnDodaj.setOnAction(e -> {
            try {
                String marka = tfMarka.getText().trim();
                String model = tfModel.getText().trim();
                String nr = tfNrRej.getText().trim();

                if (marka.isEmpty() || model.isEmpty() || nr.isEmpty()) {
                    blad("Błąd", "Marka, model i numer rejestracyjny nie mogą być puste.");
                    return;
                }

                if (samochodDAO.znajdzPoNumerzeRejestracyjnym(nr) != null) {
                    blad("Błąd", "Samochód o takim numerze rejestracyjnym już istnieje.");
                    return;
                }

                int moc = Integer.parseInt(tfMoc.getText().trim());
                if (moc <= 0) {
                    blad("Błąd", "Moc musi być większa niż 0.");
                    return;
                }

                int rok = Integer.parseInt(tfRocznik.getText().trim());
                if (rok < 1900 || rok > Year.now().getValue() + 1) {
                    blad("Błąd", "Nieprawidłowy rocznik.");
                    return;
                }

                KategoriaSamochodu kat = cbKategoria.getValue();
                if (kat == null) {
                    blad("Błąd", "Wybierz kategorię.");
                    return;
                }

                Cennik cennik = cennikDAO.znajdzPoKategorii(kat);
                if (cennik == null) {
                    blad("Błąd", "Brak cennika dla kategorii " + kat + ". Uzupełnij cenniki.csv.");
                    return;
                }

                Long noweId = wygenerujNoweId();

                Samochod nowy = new Samochod(
                        noweId,
                        marka,
                        model,
                        nr,
                        moc,
                        Year.of(rok),
                        kat
                );
                nowy.setCennik(cennik);

                List<Samochod> wszystkie = samochodDAO.wczytajWszystkie();
                wszystkie.add(nowy);
                samochodDAO.zapiszWszystkie(wszystkie);

                dodano = true;
                info("Sukces", "Dodano nowy samochód.");
                dialog.close();
            } catch (NumberFormatException ex) {
                blad("Błąd", "Moc i rocznik muszą być liczbami.");
            } catch (Exception ex) {
                blad("Błąd", "Nie udało się dodać samochodu: " + ex.getMessage());
            }
        });

        Button btnAnuluj = new Button("Anuluj");
        btnAnuluj.setPrefWidth(120);
        btnAnuluj.setOnAction(e -> dialog.close());

        btnBox.getChildren().addAll(btnDodaj, btnAnuluj);

        root.getChildren().addAll(lblTytul, new Separator(), pola, btnBox);

        dialog.setScene(new Scene(root, 460, 490));
        dialog.showAndWait();

        return dodano;
    }

    private Long wygenerujNoweId() {
        return samochodDAO.wczytajWszystkie().stream()
                .map(Samochod::getId)
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