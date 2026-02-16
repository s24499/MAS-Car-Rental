package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.Cennik;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CennikDAO extends BazaDAO {
    private static final String PLIK = "cenniki.csv";

    // Wczytuje wszystkie cenniki
    public List<Cennik> wczytajWszystkie() {
        List<Cennik> cenniki = new ArrayList<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";");
                Long id = Long.parseLong(dane[0]);
                BigDecimal stawkaZaDobe = new BigDecimal(dane[1]);
                BigDecimal procentDodatkowyKierowca = new BigDecimal(dane[2]);

                cenniki.add(new Cennik(id, stawkaZaDobe, procentDodatkowyKierowca));
            } catch (Exception e) {
                System.err.println("Błąd podczas wczytywania cennika: " + e.getMessage());
            }
        }
        return cenniki;
    }

    // Zapisuje wszystkie cenniki
    public void zapiszWszystkie(List<Cennik> cenniki) {
        List<String> linie = new ArrayList<>();
        linie.add("id;stawkaZaDobe;procentDodatkowyKierowca");

        for (Cennik c : cenniki) {
            linie.add(c.getId() + ";" +
                    c.getStawkaZaDobe() + ";" +
                    c.getProcentDodatkowyKierowca());
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    // Znajduje cennik po ID
    public Cennik znajdzPoId(Long id) {
        return wczytajWszystkie().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Aktualizuje cennik
    public void aktualizuj(Cennik cennik) {
        List<Cennik> cenniki = wczytajWszystkie();
        for (int i = 0; i < cenniki.size(); i++) {
            if (cenniki.get(i).getId().equals(cennik.getId())) {
                cenniki.set(i, cennik);
                break;
            }
        }
        zapiszWszystkie(cenniki);
    }

    // Usuwa cennik po ID
    public void usunPoId(Long id) {
        List<Cennik> cenniki = wczytajWszystkie();
        cenniki.removeIf(c -> c.getId().equals(id));
        zapiszWszystkie(cenniki);
    }
}