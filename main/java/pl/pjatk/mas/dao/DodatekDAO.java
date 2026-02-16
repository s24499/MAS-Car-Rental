package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.TypRozliczaniaDodatku;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DodatekDAO extends BazaDAO {
    private static final String PLIK = "dodatki.csv";

    // Wczytuje wszystkie dodatki
    public List<Dodatek> wczytajWszystkie() {
        List<Dodatek> dodatki = new ArrayList<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        // Pomijamy nagłówek
        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";", -1);
                Long id = Long.parseLong(dane[0]);
                String nazwa = dane[1];
                BigDecimal cena = new BigDecimal(dane[2]);
                TypRozliczaniaDodatku typ = TypRozliczaniaDodatku.valueOf(dane[3]);

                List<KategoriaSamochodu> kategorie = new ArrayList<>();
                if (dane.length > 4 && !dane[4].isEmpty()) {
                    String[] kategorieArray = dane[4].split(",");
                    for (String kat : kategorieArray) {
                        kategorie.add(KategoriaSamochodu.valueOf(kat));
                    }
                }

                dodatki.add(new Dodatek(id, nazwa, cena, typ, kategorie));
            } catch (Exception e) {
                System.err.println("Błąd podczas wczytywania dodatku: " + e.getMessage());
            }
        }
        return dodatki;
    }

    // Zapisuje wszystkie dodatki do pliku
    public void zapiszWszystkie(List<Dodatek> dodatki) {
        List<String> linie = new ArrayList<>();
        linie.add("id;nazwa;cena;typRozliczania;kategorie");

        for (Dodatek d : dodatki) {
            String kategorieStr = d.getDostepneKategorie().stream()
                    .map(KategoriaSamochodu::name)
                    .collect(Collectors.joining(","));

            linie.add(d.getId() + ";" +
                    d.getNazwa() + ";" +
                    d.getCena() + ";" +
                    d.getTypRozliczania() + ";" +
                    kategorieStr);
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    // Znajduje dodatek po ID
    public Dodatek znajdzPoId(Long id) {
        return wczytajWszystkie().stream()
                .filter(d -> d.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Aktualizuje dodatek
    public void aktualizuj(Dodatek dodatek) {
        List<Dodatek> dodatki = wczytajWszystkie();
        for (int i = 0; i < dodatki.size(); i++) {
            if (dodatki.get(i).getId().equals(dodatek.getId())) {
                dodatki.set(i, dodatek);
                break;
            }
        }
        zapiszWszystkie(dodatki);
    }

    // Usuwa dodatek po ID
    public void usunPoId(Long id) {
        List<Dodatek> dodatki = wczytajWszystkie();
        dodatki.removeIf(d -> d.getId().equals(id));
        zapiszWszystkie(dodatki);
    }
}