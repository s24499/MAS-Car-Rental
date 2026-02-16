package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.Cennik;
import pl.pjatk.mas.model.KategoriaSamochodu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CennikDAO extends BazaDAO {
    private static final String PLIK = "cenniki.csv";

    public List<Cennik> wczytajWszystkie() {
        List<Cennik> cenniki = new ArrayList<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        if (linie.isEmpty()) {
            return cenniki;
        }

        // Pomijamy nagłówek
        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";", -1);

                // Oczekiwany format:
                // id;kategoria;stawkaZaDobe;procentDodatkowyKierowca
                Long id = Long.parseLong(dane[0]);
                KategoriaSamochodu kategoria = KategoriaSamochodu.valueOf(dane[1]);
                BigDecimal stawkaZaDobe = new BigDecimal(dane[2]);
                BigDecimal procent = new BigDecimal(dane[3]);

                cenniki.add(new Cennik(id, kategoria, stawkaZaDobe, procent));
            } catch (Exception e) {
                System.err.println("Błąd podczas wczytywania cennika: " + e.getMessage());
            }
        }

        return cenniki;
    }

    public void zapiszWszystkie(List<Cennik> cenniki) {
        List<String> linie = new ArrayList<>();
        linie.add("id;kategoria;stawkaZaDobe;procentDodatkowyKierowca");

        for (Cennik c : cenniki) {
            linie.add(c.getId() + ";" +
                    c.getKategoria().name() + ";" +
                    c.getStawkaZaDobe() + ";" +
                    c.getProcentDodatkowyKierowca());
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    public Cennik znajdzPoId(Long id) {
        return wczytajWszystkie().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public Cennik znajdzPoKategorii(KategoriaSamochodu kategoria) {
        if (kategoria == null) {
            throw new IllegalArgumentException("Kategoria nie może być null");
        }

        return wczytajWszystkie().stream()
                .filter(c -> c.getKategoria() == kategoria)
                .findFirst()
                .orElse(null);
    }
}