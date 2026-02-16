package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.Samochod;

import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SamochodDAO extends BazaDAO {
    private static final String PLIK = "samochody.csv";
    private static final String HEADER = "id;marka;model;numerRejestracyjny;mocKM;rocznik;kategoria;cennikId";

    public List<Samochod> wczytajWszystkie() {
        List<Samochod> samochody = new ArrayList<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        if (linie.size() <= 1) {
            return samochody;
        }

        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";", -1);

                Long id = Long.parseLong(dane[0]);
                String marka = dane[1];
                String model = dane[2];
                String numerRejestracyjny = dane[3];
                int mocKM = Integer.parseInt(dane[4]);
                Year rocznik = Year.of(Integer.parseInt(dane[5]));
                KategoriaSamochodu kategoria = KategoriaSamochodu.valueOf(dane[6]);

                Samochod samochod = new Samochod(id, marka, model, numerRejestracyjny, mocKM, rocznik, kategoria);
                samochod.setCennik(null); // relacja dopinana w Service
                samochody.add(samochod);
            } catch (Exception e) {
                System.err.println("Błąd podczas wczytywania samochodu: " + e.getMessage());
            }
        }

        return samochody;
    }

    public Map<Long, Long> wczytajMapeCennikIdPoSamochodId() {
        Map<Long, Long> mapa = new HashMap<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        if (linie.size() <= 1) {
            return mapa;
        }

        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";", -1);
                Long samochodId = Long.parseLong(dane[0]);
                Long cennikId = Long.parseLong(dane[7]);
                mapa.put(samochodId, cennikId);
            } catch (Exception ignored) {
            }
        }

        return mapa;
    }

    public void zapiszWszystkie(List<Samochod> samochody) {
        List<String> linie = new ArrayList<>();
        linie.add(HEADER);

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

    public void dodaj(Samochod samochod) {
        if (samochod == null) {
            throw new IllegalArgumentException("Samochód nie może być null");
        }

        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));
        if (linie.isEmpty()) {
            linie = new ArrayList<>();
            linie.add(HEADER);
        }

        Long cennikId = (samochod.getCennik() != null) ? samochod.getCennik().getId() : 0L;

        String nowaLinia = samochod.getId() + ";" +
                samochod.getMarka() + ";" +
                samochod.getModel() + ";" +
                samochod.getNumerRejestracyjny() + ";" +
                samochod.getMocKM() + ";" +
                samochod.getRocznik() + ";" +
                samochod.getKategoria() + ";" +
                cennikId;

        linie.add(nowaLinia);
        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    public Samochod znajdzPoId(Long id) {
        return wczytajWszystkie().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public Samochod znajdzPoNumerzeRejestracyjjnym(String numer) {
        return wczytajWszystkie().stream()
                .filter(s -> s.getNumerRejestracyjny().equalsIgnoreCase(numer))
                .findFirst()
                .orElse(null);
    }

    /** Aktualizuje pojedynczy rekord w CSV bez przepisywania pozostałych samochodów na obiekty (bez relacji). Dzięki temu nie gubi się cennikId dla aut, które nie były edytowane. */
    public void aktualizuj(Samochod samochod) {
        if (samochod == null) {
            throw new IllegalArgumentException("Samochód nie może być null");
        }

        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));
        if (linie.isEmpty()) {
            linie = new ArrayList<>();
            linie.add(HEADER);
        }

        Long cennikId = (samochod.getCennik() != null) ? samochod.getCennik().getId() : 0L;

        String nowaLinia = samochod.getId() + ";" +
                samochod.getMarka() + ";" +
                samochod.getModel() + ";" +
                samochod.getNumerRejestracyjny() + ";" +
                samochod.getMocKM() + ";" +
                samochod.getRocznik() + ";" +
                samochod.getKategoria() + ";" +
                cennikId;

        boolean podmieniono = false;

        for (int i = 1; i < linie.size(); i++) {
            String[] dane = linie.get(i).split(";", -1);
            if (dane.length < 1) continue;

            try {
                Long id = Long.parseLong(dane[0]);
                if (samochod.getId().equals(id)) {
                    linie.set(i, nowaLinia);
                    podmieniono = true;
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        // Jeśli nie znaleziono rekordu, dopisujemy (zachowanie "upsert" jest bezpieczne)
        if (!podmieniono) {
            linie.add(nowaLinia);
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    public void usunPoId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID samochodu nie może być null");
        }

        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));
        if (linie.size() <= 1) {
            return; // brak danych lub tylko nagłówek
        }

        // Zostaw nagłówek, usuń tylko rekord o danym ID
        List<String> wynik = new ArrayList<>();
        wynik.add(linie.get(0));

        for (int i = 1; i < linie.size(); i++) {
            String linia = linie.get(i);
            try {
                String[] dane = linia.split(";", -1);
                Long wierszId = Long.parseLong(dane[0]);

                if (!wierszId.equals(id)) {
                    wynik.add(linia); // przepisujemy linie "as-is" => cennikId się nie zmieni
                }
            } catch (Exception e) {
                // jeśli linia jest uszkodzona, zostawiamy ją (żeby nic nie zgubić przypadkiem)
                wynik.add(linia);
            }
        }

        zapiszLinie(sciezkaDoPliku(PLIK), wynik);
    }
}