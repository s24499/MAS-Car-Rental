package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.Cennik;
import pl.pjatk.mas.model.KategoriaSamochodu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CennikDAO extends BazaDAO {
    private static final String PLIK = "cenniki.csv";
    private static final String HEADER = "id;kategoria;stawkaZaDobe;procentDodatkowyKierowca";

    /**  Odczytuje wszystkie rekordy cennika z CSV. */
    public List<Cennik> wczytajWszystkie() {
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));
        List<Cennik> cenniki = new ArrayList<>();

        if (linie.size() <= 1) {
            return cenniki; // brak danych lub sam nagłówek
        }

        for (int i = 1; i < linie.size(); i++) {
            Cennik c = parseLinia(linie.get(i));
            if (c != null) {
                cenniki.add(c);
            }
        }

        return cenniki;
    }

    /** Zapisuje wszystkie rekordy do CSV (nadpisuje plik). */
    public void zapiszWszystkie(List<Cennik> cenniki) {
        List<String> linie = new ArrayList<>();
        linie.add(HEADER);

        if (cenniki != null) {
            for (Cennik c : cenniki) {
                linie.add(toLinia(c));
            }
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    /** Wyszukuje cennik po ID (wykonuje pełny odczyt pliku). */
    public Cennik znajdzPoId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID nie może być null");
        }

        return wczytajWszystkie().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElse(null);
    }

    /** Wyszukuje cennik po kategorii (wykonuje pełny odczyt pliku). */
    public Cennik znajdzPoKategorii(KategoriaSamochodu kategoria) {
        if (kategoria == null) {
            throw new IllegalArgumentException("Kategoria nie może być null");
        }

        return wczytajWszystkie().stream()
                .filter(c -> c.getKategoria() == kategoria)
                .findFirst()
                .orElse(null);
    }

    /** Parsuje pojedynczą linię CSV do obiektu Cennik. */
    private Cennik parseLinia(String linia) {
        try {
            String[] dane = linia.split(";", -1);

            Long id = Long.parseLong(dane[0]);
            KategoriaSamochodu kategoria = KategoriaSamochodu.valueOf(dane[1]);
            BigDecimal stawkaZaDobe = new BigDecimal(dane[2]);
            BigDecimal procent = new BigDecimal(dane[3]);

            return new Cennik(id, kategoria, stawkaZaDobe, procent);
        } catch (Exception e) {
            System.err.println("Błąd podczas wczytywania cennika: " + e.getMessage());
            return null;
        }
    }

    /** Buduje linię CSV z obiektu Cennik. */
    private String toLinia(Cennik c) {
        return c.getId() + ";" +
                c.getKategoria().name() + ";" +
                c.getStawkaZaDobe() + ";" +
                c.getProcentDodatkowyKierowca();
    }
}