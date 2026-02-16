package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.Rezerwacja;
import pl.pjatk.mas.model.StatusRezerwacji;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DAO odpowiedzialne za zapis/odczyt rezerwacji w pliku rezerwacje.csv.
 * Przechowuje wyłącznie pola prymitywne oraz ID relacji (klientId/samochodId/dodatkiIds).
 * Składanie relacji do obiektów domenowych odbywa się w warstwie Service.
 */

public class RezerwacjaDAO extends BazaDAO {
    private static final String PLIK = "rezerwacje.csv";
    private static final String HEADER = "id;klientId;samochodId;dataOd;dataDo;status;cenaCalkowita;dodatkiIds";

    /** Odczytuje rezerwacje bez relacji (klient/samochód/dodatki są dopinane w Service). */
    public List<Rezerwacja> wczytajWszystkie() {
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));
        List<Rezerwacja> rezerwacje = new ArrayList<>();

        if (linie.size() <= 1) {
            return rezerwacje; // brak danych lub sam nagłówek
        }

        for (int i = 1; i < linie.size(); i++) {
            Rezerwacja r = parseRezerwacjaBezRelacji(linie.get(i));
            if (r != null) {
                rezerwacje.add(r);
            }
        }

        return rezerwacje;
    }

    /** Zapisuje wszystkie rezerwacje do CSV (nadpisuje plik). Service musi zapewnić ustawione relacje przed zapisem (klient/samochód/dodatki). */
    public void zapiszWszystkie(List<Rezerwacja> rezerwacje) {
        List<String> linie = new ArrayList<>();
        linie.add(HEADER);

        if (rezerwacje != null) {
            for (Rezerwacja r : rezerwacje) {
                linie.add(toLinia(r));
            }
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    /** Wyszukuje rezerwację po ID (pełny odczyt pliku). */
    public Rezerwacja znajdzPoId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID rezerwacji nie może być null");
        }

        return wczytajWszystkie().stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .orElse(null);
    }

    /** Aktualizuje rezerwację (podmienia rekord o tym samym ID). */
    public void aktualizuj(Rezerwacja rezerwacja) {
        if (rezerwacja == null) {
            throw new IllegalArgumentException("Rezerwacja nie może być null");
        }

        List<Rezerwacja> rezerwacje = wczytajWszystkie();
        for (int i = 0; i < rezerwacje.size(); i++) {
            if (rezerwacja.getId().equals(rezerwacje.get(i).getId())) {
                rezerwacje.set(i, rezerwacja);
                zapiszWszystkie(rezerwacje);
                return;
            }
        }

        // Brak wpisu w pliku: zachowujemy poprzednie zachowanie (brak wyjątku).
        zapiszWszystkie(rezerwacje);
    }

    /** Usuwa rezerwację po ID. */
    public void usunPoId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID rezerwacji nie może być null");
        }

        List<Rezerwacja> rezerwacje = wczytajWszystkie();
        rezerwacje.removeIf(r -> id.equals(r.getId()));
        zapiszWszystkie(rezerwacje);
    }

    /** Zwraca mapę: rezerwacjaId -> klientId. */
    public Map<Long, Long> wczytajMapeKlientIdPoRezerwacjaId() {
        Map<Long, Long> mapa = new HashMap<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";", -1);
                if (dane.length < 3) continue;

                Long rezerwacjaId = Long.parseLong(dane[0]);
                Long klientId = Long.parseLong(dane[1]);
                mapa.put(rezerwacjaId, klientId);
            } catch (Exception ignored) {
            }
        }

        return mapa;
    }

    /** Zwraca mapę: rezerwacjaId -> samochodId. */
    public Map<Long, Long> wczytajMapeSamochodIdPoRezerwacjaId() {
        Map<Long, Long> mapa = new HashMap<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";", -1);
                if (dane.length < 3) continue;

                Long rezerwacjaId = Long.parseLong(dane[0]);
                Long samochodId = Long.parseLong(dane[2]);
                mapa.put(rezerwacjaId, samochodId);
            } catch (Exception ignored) {
            }
        }

        return mapa;
    }

    /** Zwraca mapę: rezerwacjaId -> lista dodatekId. */
    public Map<Long, List<Long>> wczytajMapeDodatekIdsPoRezerwacjaId() {
        Map<Long, List<Long>> mapa = new HashMap<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";", -1);
                if (dane.length < 8) continue;

                Long rezerwacjaId = Long.parseLong(dane[0]);
                String dodatkiIds = dane[7];

                mapa.put(rezerwacjaId, parseListaId(dodatkiIds));
            } catch (Exception ignored) {
            }
        }

        return mapa;
    }

    /** Odpowiada za parsowanie jednej linii CSV do obiektu Rezerwacja (bez relacji). */
    private Rezerwacja parseRezerwacjaBezRelacji(String linia) {
        try {
            String[] dane = linia.split(";", -1);

            // id;klientId;samochodId;dataOd;dataDo;status;cenaCalkowita;dodatkiIds
            if (dane.length < 7) {
                return null;
            }

            Long id = Long.parseLong(dane[0]);
            LocalDate dataOd = LocalDate.parse(dane[3]);
            LocalDate dataDo = LocalDate.parse(dane[4]);
            StatusRezerwacji status = StatusRezerwacji.valueOf(dane[5]);
            BigDecimal cenaCalkowita = new BigDecimal(dane[6]);

            Rezerwacja r = new Rezerwacja(id, null, null, dataOd, dataDo);
            r.setStatus(status);
            r.setCenaCalkowita(cenaCalkowita);

            return r;
        } catch (Exception e) {
            System.err.println("Błąd podczas wczytywania rezerwacji: " + e.getMessage());
            return null;
        }
    }

    /** Odpowiada za budowanie jednej linii CSV na podstawie obiektu Rezerwacja. Service musi zapewnić, że relacje klient/samochód/dodatki są ustawione. */
    private String toLinia(Rezerwacja r) {
        String dodatkiIds = r.getDodatki().stream()
                .map(d -> d.getId().toString())
                .collect(Collectors.joining(","));

        return r.getId() + ";" +
                r.getKlient().getId() + ";" +
                r.getSamochod().getId() + ";" +
                r.getDataOd() + ";" +
                r.getDataDo() + ";" +
                r.getStatus() + ";" +
                r.getCenaCalkowita() + ";" +
                dodatkiIds;
    }

    /** Odpowiada za parsowanie listy ID zapisanej w CSV w formacie "1,2,3". Pusty string oznacza brak dodatków. */
    private List<Long> parseListaId(String csv) {
        List<Long> ids = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return ids;
        }

        for (String part : csv.split(",")) {
            if (!part.isBlank()) {
                ids.add(Long.parseLong(part.trim()));
            }
        }

        return ids;
    }
}