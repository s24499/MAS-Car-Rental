package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.Samochod;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class SamochodDAO extends BazaDAO {
    private static final String PLIK = "samochody.csv";
    private final CennikDAO cennikDAO = new CennikDAO();

    // Wczytuje wszystkie samochody
    public List<Samochod> wczytajWszystkie() {
        List<Samochod> samochody = new ArrayList<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";");
                Long id = Long.parseLong(dane[0]);
                String marka = dane[1];
                String model = dane[2];
                String numerRejestracyjny = dane[3];
                int mocKM = Integer.parseInt(dane[4]);
                Year rocznik = Year.of(Integer.parseInt(dane[5]));
                KategoriaSamochodu kategoria = KategoriaSamochodu.valueOf(dane[6]);
                Long cennikId = Long.parseLong(dane[7]);

                Samochod samochod = new Samochod(id, marka, model,
                        numerRejestracyjny, mocKM, rocznik, kategoria);
                samochod.setCennik(cennikDAO.znajdzPoId(cennikId));
                samochody.add(samochod);
            } catch (Exception e) {
                System.err.println("Błąd podczas wczytywania samochodu: " + e.getMessage());
            }
        }
        return samochody;
    }

    // Zapisuje wszystkie samochody
    public void zapiszWszystkie(List<Samochod> samochody) {
        List<String> linie = new ArrayList<>();
        linie.add("id;marka;model;numerRejestracyjny;mocKM;rocznik;kategoria;cennikId");

        for (Samochod s : samochody) {
            Long cennikId = (s.getCennik() != null) ? s.getCennik().getId() : 0L;
            linie.add(s.getId() + ";" +
                    s.getMarka() + ";" +
                    s.getModel() + ";" +
                    s.getNumerRejestracyjny() + ";" +
                    s.getMocKM() + ";" +
                    s.getRocznik() + ";" +
                    s.getKategoria() + ";" +
                    cennikId);
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    // Znajduje samochód po ID - zbędne, czy nie można wykorzystać?
    public Samochod znajdzPoId(Long id) {
        return wczytajWszystkie().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Znajduje samochód po numerze rejestracyjnym
    public Samochod znajdzPoNumerzeRejestracyjnym(String numer) {
        return wczytajWszystkie().stream()
                .filter(s -> s.getNumerRejestracyjny().equalsIgnoreCase(numer))
                .findFirst()
                .orElse(null);
    }

    // Aktualizuje samochód
    public void aktualizuj(Samochod samochod) {
        List<Samochod> samochody = wczytajWszystkie();
        for (int i = 0; i < samochody.size(); i++) {
            if (samochody.get(i).getId().equals(samochod.getId())) {
                samochody.set(i, samochod);
                break;
            }
        }
        zapiszWszystkie(samochody);
    }

    // Usuwa samochód po ID - zbędne, czy nie można wykorzystać?
    public void usunPoId(Long id) {
        List<Samochod> samochody = wczytajWszystkie();
        samochody.removeIf(s -> s.getId().equals(id));
        zapiszWszystkie(samochody);
    }
}