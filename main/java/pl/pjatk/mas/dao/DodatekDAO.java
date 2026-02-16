package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.TypRozliczaniaDodatku;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DodatekDAO extends BazaDAO {
    private static final String PLIK = "dodatki.csv";
    private static final String HEADER = "id;nazwa;cena;typRozliczania;kategorie";

    /** Odczytuje wszystkie dodatki z CSV. */
    public List<Dodatek> wczytajWszystkie() {
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));
        List<Dodatek> dodatki = new ArrayList<>();

        if (linie.size() <= 1) {
            return dodatki; // brak danych lub sam nagłówek
        }

        for (int i = 1; i < linie.size(); i++) {
            Dodatek d = parseLinia(linie.get(i));
            if (d != null) {
                dodatki.add(d);
            }
        }

        return dodatki;
    }

    /** Zapisuje dodatki do CSV (nadpisuje plik). */
    public void zapiszWszystkie(List<Dodatek> dodatki) {
        List<String> linie = new ArrayList<>();
        linie.add(HEADER);

        if (dodatki != null) {
            for (Dodatek d : dodatki) {
                linie.add(toLinia(d));
            }
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    /** Wyszukuje dodatek po ID (wykonuje pełny odczyt pliku). */
    public Dodatek znajdzPoId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID dodatku nie może być null");
        }

        return wczytajWszystkie().stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst()
                .orElse(null);
    }

    /** Aktualizuje dodatek (podmienia rekord o tym samym ID). */
    public void aktualizuj(Dodatek dodatek) {
        if (dodatek == null) {
            throw new IllegalArgumentException("Dodatek nie może być null");
        }

        List<Dodatek> dodatki = wczytajWszystkie();
        for (int i = 0; i < dodatki.size(); i++) {
            if (dodatek.getId().equals(dodatki.get(i).getId())) {
                dodatki.set(i, dodatek);
                zapiszWszystkie(dodatki);
                return;
            }
        }

        // Jeśli nie znaleziono wpisu, zachowujemy poprzednie zachowanie (brak wyjątku).
        zapiszWszystkie(dodatki);
    }

    /** Usuwa dodatek po ID. */
    public void usunPoId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID dodatku nie może być null");
        }

        List<Dodatek> dodatki = wczytajWszystkie();
        dodatki.removeIf(d -> id.equals(d.getId()));
        zapiszWszystkie(dodatki);
    }

    /** Parsuje pojedynczą linię CSV do obiektu Dodatek. */
    private Dodatek parseLinia(String linia) {
        try {
            String[] dane = linia.split(";", -1);

            Long id = Long.parseLong(dane[0]);
            String nazwa = dane[1];
            BigDecimal cena = new BigDecimal(dane[2]);
            TypRozliczaniaDodatku typ = TypRozliczaniaDodatku.valueOf(dane[3]);

            List<KategoriaSamochodu> kategorie = parseKategorie(dane.length > 4 ? dane[4] : "");

            return new Dodatek(id, nazwa, cena, typ, kategorie);
        } catch (Exception e) {
            System.err.println("Błąd podczas wczytywania dodatku: " + e.getMessage());
            return null;
        }
    }

    /**  Buduje linię CSV z obiektu Dodatek. */
    private String toLinia(Dodatek d) {
        String kategorieStr = d.getDostepneKategorie().stream()
                .map(KategoriaSamochodu::name)
                .collect(Collectors.joining(","));

        return d.getId() + ";" +
                d.getNazwa() + ";" +
                d.getCena() + ";" +
                d.getTypRozliczania() + ";" +
                kategorieStr;
    }

    /** Zamienia zapis CSV "SUV,SEDAN" na listę z enum. Pusty string oznacza "dostępny dla wszystkich". */
    private List<KategoriaSamochodu> parseKategorie(String csv) {
        List<KategoriaSamochodu> kategorie = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return kategorie;
        }

        String[] parts = csv.split(",");
        for (String p : parts) {
            if (!p.isBlank()) {
                kategorie.add(KategoriaSamochodu.valueOf(p.trim()));
            }
        }
        return kategorie;
    }
}