package pl.pjatk.mas.model;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Samochód dostępny w wypożyczalni.
 */

public class Samochod {

    private Long id;
    private String marka;
    private String model;
    private String numerRejestracyjny;
    private int mocKM;
    private Year rocznik;
    private KategoriaSamochodu kategoria;
    private Cennik cennik;
    private List<Rezerwacja> rezerwacje = new ArrayList<>();

    public Samochod(Long id,
                    String marka,
                    String model,
                    String numerRejestracyjny,
                    int mocKM,
                    Year rocznik,
                    KategoriaSamochodu kategoria) {
        this.id = id;
        this.marka = marka;
        this.model = model;
        this.numerRejestracyjny = numerRejestracyjny;
        this.mocKM = mocKM;
        this.rocznik = rocznik;
        this.kategoria = kategoria;
    }

    public Long getId() {
        return id;
    }

    public String getMarka() {
        return marka;
    }

    public String getModel() {
        return model;
    }

    public String getNumerRejestracyjny() {
        return numerRejestracyjny;
    }

    public int getMocKM() {
        return mocKM;
    }

    public Year getRocznik() {
        return rocznik;
    }

    public KategoriaSamochodu getKategoria() {
        return kategoria;
    }

    public Cennik getCennik() {
        return cennik;
    }

    public void setCennik(Cennik cennik) {
        this.cennik = cennik;
    }

    public List<Rezerwacja> getRezerwacje() {
        return rezerwacje;
    }

    /**
     * Sprawdza, czy samochód jest dostępny w podanym zakresie dat.
     * Zakłada, że rezerwacje anulowane nie blokują terminu.
     */
    public boolean czyDostepny(LocalDate dataOd, LocalDate dataDo) {
        for (Rezerwacja r : rezerwacje) {
            if (r.getStatus() == StatusRezerwacji.ANULOWANA) {
                continue;
            }
            boolean nachodzi =
                    !dataDo.isBefore(r.getDataOd()) &&
                            !dataOd.isAfter(r.getDataDo());
            if (nachodzi) {
                return false;
            }
        }
        return true;
    }

    // Dodaje nową rezerwację do listy rezerwacji tego samochodu
    public void dodajRezerwacje(Rezerwacja rezerwacja) {
        this.rezerwacje.add(rezerwacja);
    }

    @Override
    public String toString() {
        return marka + " " + model + " (" + numerRejestracyjny + ")";
    }
}
