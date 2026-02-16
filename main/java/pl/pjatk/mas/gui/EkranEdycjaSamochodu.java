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

public class EkranEdycjaSamochodu {

    private final SamochodDAO samochodDAO = new SamochodDAO();
    private final CennikDAO cennikDAO = new CennikDAO();
    private boolean zmieniono = false;

    public boolean pokazDialog(Stage parentStage, Samochod samochod) {
        this.zmieniono = false;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Edytuj samochód: " + samochod);
        dialog.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label lblTytul = new Label("Edycja samochodu");
        lblTytul.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextField tfMarka = new TextField(samochod.getMarka());
        TextField tfModel = new TextField(samochod.getModel());
        TextField tfNrRej = new TextField(samochod.getNumerRejestracyjny());
        TextField tfMoc = new TextField(String.valueOf(samochod.getMocKM()));
        TextField tfRocznik = new TextField(String.valueOf(samochod.getRocznik().getValue()));

        ComboBox<KategoriaSamochodu> cbKategoria = new ComboBox<>();
        cbKategoria.getItems().addAll(KategoriaSamochodu.values());
        cbKategoria.setValue(samochod.getKategoria());

        Label lblCennikInfo = new Label();
        lblCennikInfo.setStyle("-fx-font-weight: bold;");
        ustawCennikInfo(lblCennikInfo, cbKategoria.getValue());

        cbKategoria.valueProperty().addListener((obs, oldVal, newVal) -> ustawCennikInfo(lblCennikInfo, newVal));

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

        Button btnZapisz = new Button("Zapisz zmiany");
        btnZapisz.setPrefWidth(120);
        btnZapisz.setOnAction(e -> {
            try {
                String marka = tfMarka.getText().trim();
                String model = tfModel.getText().trim();
                String nr = tfNrRej.getText().trim();

                if (marka.isEmpty() || model.isEmpty() || nr.isEmpty()) {
                    blad("Błąd", "Marka, model i numer rejestracyjny nie mogą być puste.");
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

                // Samochod nie ma setterów pól -> tworzymy nowy obiekt z tym samym ID
                Samochod zaktualizowany = new Samochod(
                        samochod.getId(),
                        marka,
                        model,
                        nr,
                        moc,
                        Year.of(rok),
                        kat
                );
                zaktualizowany.setCennik(cennik);

                samochodDAO.aktualizuj(zaktualizowany);

                zmieniono = true;
                info("Sukces", "Samochód został zaktualizowany.");
                dialog.close();
            } catch (NumberFormatException ex) {
                blad("Błąd", "Moc i rocznik muszą być liczbami.");
            } catch (Exception ex) {
                blad("Błąd", "Nie udało się zapisać zmian: " + ex.getMessage());
            }
        });

        Button btnAnuluj = new Button("Anuluj");
        btnAnuluj.setPrefWidth(120);
        btnAnuluj.setOnAction(e -> dialog.close());

        btnBox.getChildren().addAll(btnZapisz, btnAnuluj);

        root.getChildren().addAll(lblTytul, new Separator(), pola, btnBox);

        dialog.setScene(new Scene(root, 460, 490));
        dialog.showAndWait();

        return zmieniono;
    }

    private void ustawCennikInfo(Label lbl, KategoriaSamochodu kat) {
        if (kat == null) {
            lbl.setText("Cennik: (wybierz kategorię)");
            return;
        }
        Cennik c = cennikDAO.znajdzPoKategorii(kat);
        if (c == null) {
            lbl.setText("Cennik: brak dla kategorii " + kat);
        } else {
            lbl.setText("Cennik: ID " + c.getId() + " / " + c.getStawkaZaDobe() + " zł/doba");
        }
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