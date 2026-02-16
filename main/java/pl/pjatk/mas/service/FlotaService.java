package pl.pjatk.mas.service;

import pl.pjatk.mas.dao.CennikDAO;
import pl.pjatk.mas.dao.SamochodDAO;
import pl.pjatk.mas.model.Cennik;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.Rezerwacja;
import pl.pjatk.mas.model.Samochod;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serwis odpowiedzialny za zarządzanie flotą.
 * Składa relacje: Samochod -> Cennik oraz Samochod -> Rezerwacje.
 */
public class FlotaService {

    private final SamochodDAO samochodDAO = new SamochodDAO();
    private final CennikDAO cennikDAO = new CennikDAO();

    // Leniwa inicjalizacja - unika cyklicznego tworzenia serwisów.
    private RezerwacjaService rezerwacjaService;

    /**
     * Zwraca wszystkie samochody z dopiętym cennikiem i listą rezerwacji.
     */
    public List<Samochod> pobierzWszystkieSamochody() {
        List<Samochod> samochody = samochodDAO.wczytajWszystkie();

        dopnijCenniki(samochody);
        dopnijRezerwacje(samochody);

        return samochody;
    }

    /**
     * Dodaje nowy samochód (waliduje dane i przypisuje cennik na podstawie kategorii).
     */
    public Samochod dodajSamochod(String marka,
                                  String model,
                                  String numerRejestracyjny,
                                  int mocKM,
                                  Year rocznik,
                                  KategoriaSamochodu kategoria) {
        String markaN = normalize(marka);
        String modelN = normalize(model);
        String nrRejN = normalize(numerRejestracyjny);

        walidujDaneSamochodu(markaN, modelN, nrRejN, mocKM, rocznik, kategoria);

        List<Samochod> wszystkie = pobierzWszystkieSamochody();
        if (czyNumerZajety(wszystkie, null, nrRejN)) {
            throw new IllegalArgumentException("Samochód o tym numerze rejestracyjnym już istnieje.");
        }

        Cennik cennik = wymaganyCennik(kategoria);
        long noweId = wyznaczNoweId(wszystkie);

        Samochod nowy = new Samochod(noweId, markaN, modelN, nrRejN, mocKM, rocznik, kategoria);
        nowy.setCennik(cennik);

        samochodDAO.dodaj(nowy);
        return nowy;
    }

    /**
     * Aktualizuje dane samochodu (utrzymuje istniejące rezerwacje i aktualizuje cennik wg kategorii).
     */
    public void aktualizujSamochod(Long id,
                                   String marka,
                                   String model,
                                   String numerRejestracyjny,
                                   int mocKM,
                                   Year rocznik,
                                   KategoriaSamochodu kategoria) {
        if (id == null) {
            throw new IllegalArgumentException("ID samochodu nie może być null");
        }

        String markaN = normalize(marka);
        String modelN = normalize(model);
        String nrRejN = normalize(numerRejestracyjny);

        walidujDaneSamochodu(markaN, modelN, nrRejN, mocKM, rocznik, kategoria);

        List<Samochod> wszystkie = pobierzWszystkieSamochody();

        Samochod istniejacy = wszystkie.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono samochodu o ID: " + id));

        if (czyNumerZajety(wszystkie, id, nrRejN)) {
            throw new IllegalArgumentException("Inny samochód ma już ten numer rejestracyjny.");
        }

        Cennik cennik = wymaganyCennik(kategoria);

        Samochod zakt = new Samochod(id, markaN, modelN, nrRejN, mocKM, rocznik, kategoria);
        zakt.setCennik(cennik);

        // Przeniesienie rezerwacji, żeby nie utracić relacji po update.
        for (Rezerwacja r : istniejacy.getRezerwacje()) {
            zakt.dodajRezerwacje(r);
        }

        samochodDAO.aktualizuj(zakt);
    }

    /**
     * Zwraca samochody dostępne w podanym terminie.
     */
    public List<Samochod> pobierzDostepneSamochody(LocalDate dataOd, LocalDate dataDo) {
        walidujZakresDat(dataOd, dataDo);

        return pobierzWszystkieSamochody().stream()
                .filter(s -> s.czyDostepny(dataOd, dataDo))
                .collect(Collectors.toList());
    }

    /**
     * Filtruje listę samochodów po kategorii (gdy lista jest null, pobiera wszystkie).
     */
    public List<Samochod> filtrujPoKategorii(List<Samochod> samochody, KategoriaSamochodu kategoria) {
        if (kategoria == null) {
            throw new IllegalArgumentException("Kategoria nie może być null");
        }

        List<Samochod> baza = (samochody != null) ? samochody : pobierzWszystkieSamochody();

        return baza.stream()
                .filter(s -> s.getKategoria() == kategoria)
                .collect(Collectors.toList());
    }

    /**
     * Zwraca samochody dostępne w terminie i pasujące do kategorii.
     */
    public List<Samochod> pobierzDostepneSamochodyFiltrowane(LocalDate dataOd,
                                                             LocalDate dataDo,
                                                             KategoriaSamochodu kategoria) {
        return filtrujPoKategorii(pobierzDostepneSamochody(dataOd, dataDo), kategoria);
    }

    /**
     * Wyszukuje samochód po ID.
     */
    public Samochod znajdzSamochodPoId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID samochodu nie może być null");
        }

        return pobierzWszystkieSamochody().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Wyszukuje samochód po numerze rejestracyjnym.
     */
    public Samochod znajdzSamochodPoNumerzeRejestracyjjnym(String numer) {
        String n = normalize(numer);
        if (n.isEmpty()) {
            throw new IllegalArgumentException("Numer rejestracyjny nie może być pusty");
        }

        return pobierzWszystkieSamochody().stream()
                .filter(s -> s.getNumerRejestracyjny().equalsIgnoreCase(n))
                .findFirst()
                .orElse(null);
    }

    /**
     * Usuwa samochód po ID.
     */
    public void usunSamochod(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID samochodu nie może być null");
        }
        samochodDAO.usunPoId(id);
    }

    /**
     * Dopina relację Samochod -> Cennik na podstawie mapy ID.
     */
    private void dopnijCenniki(List<Samochod> samochody) {
        Map<Long, Long> cennikIdPoSamochodId = samochodDAO.wczytajMapeCennikIdPoSamochodId();
        Map<Long, Cennik> cennikiPoId = cennikDAO.wczytajWszystkie().stream()
                .collect(Collectors.toMap(Cennik::getId, Function.identity()));

        for (Samochod s : samochody) {
            Long cennikId = cennikIdPoSamochodId.get(s.getId());
            if (cennikId != null) {
                s.setCennik(cennikiPoId.get(cennikId));
            }
        }
    }

    /**
     * Dopina relację Samochod -> Rezerwacje na podstawie danych z RezerwacjaService.
     */
    private void dopnijRezerwacje(List<Samochod> samochody) {
        List<Rezerwacja> rezerwacje = rezerwacje().pobierzWszystkieRezerwacje();

        Map<Long, Samochod> samochodyPoId = samochody.stream()
                .collect(Collectors.toMap(Samochod::getId, Function.identity()));

        for (Rezerwacja r : rezerwacje) {
            Samochod s = samochodyPoId.get(r.getSamochod().getId());
            if (s != null) {
                s.dodajRezerwacje(r);
            }
        }
    }

    /**
     * Zwraca cennik dla kategorii lub rzuca wyjątek, jeśli go brakuje.
     */
    private Cennik wymaganyCennik(KategoriaSamochodu kategoria) {
        Cennik cennik = cennikDAO.znajdzPoKategorii(kategoria);
        if (cennik == null) {
            throw new IllegalStateException("Brak cennika dla kategorii: " + kategoria);
        }
        return cennik;
    }

    /**
     * Waliduje dane wejściowe samochodu.
     */
    private void walidujDaneSamochodu(String marka,
                                      String model,
                                      String numerRejestracyjny,
                                      int mocKM,
                                      Year rocznik,
                                      KategoriaSamochodu kategoria) {
        if (marka.isEmpty() || model.isEmpty() || numerRejestracyjny.isEmpty()) {
            throw new IllegalArgumentException("Marka, model i numer rejestracyjny są wymagane.");
        }
        if (kategoria == null) {
            throw new IllegalArgumentException("Kategoria jest wymagana.");
        }
        if (mocKM <= 0) {
            throw new IllegalArgumentException("Moc (KM) musi być większa od 0.");
        }
        if (rocznik == null) {
            throw new IllegalArgumentException("Rocznik jest wymagany.");
        }
    }

    /**
     * Waliduje zakres dat (dataDo nie może być przed dataOd).
     */
    private void walidujZakresDat(LocalDate dataOd, LocalDate dataDo) {
        if (dataOd == null || dataDo == null) {
            throw new IllegalArgumentException("Daty nie mogą być null");
        }
        if (dataDo.isBefore(dataOd)) {
            throw new IllegalArgumentException("Data do nie może być wcześniejsza niż data od");
        }
    }

    /**
     * Sprawdza czy numer rejestracyjny jest zajęty (opcjonalnie z pominięciem auta o podanym ID).
     */
    private boolean czyNumerZajety(List<Samochod> wszystkie, Long ignorujId, String nrRej) {
        return wszystkie.stream()
                .anyMatch(s -> (ignorujId == null || !s.getId().equals(ignorujId))
                        && s.getNumerRejestracyjny().equalsIgnoreCase(nrRej));
    }

    /**
     * Wyznacza kolejne ID samochodu na podstawie danych w pliku.
     */
    private long wyznaczNoweId(List<Samochod> wszystkie) {
        return wszystkie.stream()
                .mapToLong(Samochod::getId)
                .max()
                .orElse(0L) + 1;
    }

    /**
     * Normalizuje tekst wejściowy (trim, null -> "").
     */
    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Dostarcza RezerwacjaService z leniwą inicjalizacją.
     */
    private RezerwacjaService rezerwacje() {
        if (rezerwacjaService == null) {
            rezerwacjaService = new RezerwacjaService();
        }
        return rezerwacjaService;
    }
}