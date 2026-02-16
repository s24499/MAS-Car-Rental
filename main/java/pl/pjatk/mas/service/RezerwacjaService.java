package pl.pjatk.mas.service;

import pl.pjatk.mas.dao.CennikDAO;
import pl.pjatk.mas.dao.DodatekDAO;
import pl.pjatk.mas.dao.RezerwacjaDAO;
import pl.pjatk.mas.dao.SamochodDAO;
import pl.pjatk.mas.dao.UzytkownikDAO;
import pl.pjatk.mas.model.Cennik;
import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.Klient;
import pl.pjatk.mas.model.Rezerwacja;
import pl.pjatk.mas.model.Samochod;
import pl.pjatk.mas.model.StatusRezerwacji;
import pl.pjatk.mas.model.TypRozliczaniaDodatku;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serwis odpowiedzialny za tworzenie i edycję rezerwacji.
 * Unika zależności od FlotaService (żeby nie tworzyć cyklu) i składa relacje ręcznie na bazie DAO.
 */

public class RezerwacjaService {

    private final RezerwacjaDAO rezerwacjaDAO = new RezerwacjaDAO();
    private final UzytkownikDAO uzytkownikDAO = new UzytkownikDAO();
    private final DodatekDAO dodatekDAO = new DodatekDAO();
    private final SamochodDAO samochodDAO = new SamochodDAO();
    private final CennikDAO cennikDAO = new CennikDAO();

    /**
     * Tworzy rezerwację dla aktualnie zalogowanego użytkownika (musi być Klient).
     */
    public Rezerwacja utworzRezerwacjeDlaZalogowanego(Object zalogowany,
                                                      Samochod samochod,
                                                      LocalDate dataOd,
                                                      LocalDate dataDo,
                                                      List<Dodatek> wybraneDodatki) {
        Klient klient = wymagajKlienta(zalogowany, "Tylko klienci mogą dokonywać rezerwacji.");
        return utworzRezerwacje(klient, samochod, dataOd, dataDo, wybraneDodatki);
    }

    /**
     * Zwraca rezerwacje aktualnie zalogowanego użytkownika (musi być Klient).
     */
    public List<Rezerwacja> pobierzRezerwacjeDlaZalogowanego(Object zalogowany) {
        Klient klient = wymagajKlienta(zalogowany, "Tylko klienci mogą przeglądać swoje rezerwacje.");
        return pobierzRezerwacjeKlienta(klient);
    }

    /**
     * Tworzy nową rezerwację dla klienta.
     */
    public Rezerwacja utworzRezerwacje(Klient klient,
                                       Samochod samochod,
                                       LocalDate dataOd,
                                       LocalDate dataDo,
                                       List<Dodatek> wybraneDodatki) {

        if (klient == null) throw new IllegalArgumentException("Klient nie może być null");
        if (samochod == null) throw new IllegalArgumentException("Samochód nie może być null");
        walidujDatyNowejRezerwacji(dataOd, dataDo);

        if (!sprawdzDostepnosc(samochod, dataOd, dataDo)) {
            throw new IllegalStateException("Samochód niedostępny w wybranym terminie");
        }

        Long id = generujNoweId();
        Rezerwacja rezerwacja = new Rezerwacja(id, klient, samochod, dataOd, dataDo);

        if (wybraneDodatki != null) {
            rezerwacja.getDodatki().addAll(wybraneDodatki);
        }

        rezerwacja.setCenaCalkowita(policzCene(samochod, dataOd, dataDo, rezerwacja.getDodatki()));
        samochod.dodajRezerwacje(rezerwacja);

        zapiszRezerwacje(rezerwacja);
        return rezerwacja;
    }

    /**
     * Anuluje rezerwację (dozwolone tylko dla statusu NOWA).
     */
    public void anulujRezerwacje(Long rezerwacjaId) {
        if (rezerwacjaId == null) {
            throw new IllegalArgumentException("ID rezerwacji nie może być null");
        }

        List<Rezerwacja> wszystkie = pobierzWszystkieRezerwacje();

        Rezerwacja doAnulowania = wszystkie.stream()
                .filter(r -> r.getId().equals(rezerwacjaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono rezerwacji o ID: " + rezerwacjaId));

        if (doAnulowania.getStatus() != StatusRezerwacji.NOWA) {
            throw new IllegalStateException("Można anulować tylko rezerwacje ze statusem NOWA");
        }

        doAnulowania.setStatus(StatusRezerwacji.ANULOWANA);
        rezerwacjaDAO.zapiszWszystkie(wszystkie);
    }

    /**
     * Zwraca wszystkie rezerwacje, ze złożonymi relacjami: klient, samochód, dodatki.
     */
    public List<Rezerwacja> pobierzWszystkieRezerwacje() {
        List<Rezerwacja> rezerwacjeCsv = rezerwacjaDAO.wczytajWszystkie();

        Map<Long, Long> klientIdPoRezId = rezerwacjaDAO.wczytajMapeKlientIdPoRezerwacjaId();
        Map<Long, Long> samochodIdPoRezId = rezerwacjaDAO.wczytajMapeSamochodIdPoRezerwacjaId();
        Map<Long, List<Long>> dodatekIdsPoRezId = rezerwacjaDAO.wczytajMapeDodatekIdsPoRezerwacjaId();

        Map<Long, Klient> klienciPoId = wczytajKlientowPoId();
        Map<Long, Samochod> samochodyPoId = wczytajSamochodyBezRezerwacjiPoId();
        Map<Long, Dodatek> dodatkiPoId = wczytajDodatkiPoId();

        List<Rezerwacja> wynik = new ArrayList<>();

        for (Rezerwacja r : rezerwacjeCsv) {
            Long rezId = r.getId();

            Long klientId = klientIdPoRezId.get(rezId);
            Long samochodId = samochodIdPoRezId.get(rezId);

            Klient klient = (klientId != null) ? klienciPoId.get(klientId) : null;
            Samochod samochod = (samochodId != null) ? samochodyPoId.get(samochodId) : null;

            if (klient == null || samochod == null) {
                continue;
            }

            Rezerwacja nowa = new Rezerwacja(r.getId(), klient, samochod, r.getDataOd(), r.getDataDo());
            nowa.setStatus(r.getStatus());
            nowa.setCenaCalkowita(r.getCenaCalkowita());

            List<Long> dodatekIds = dodatekIdsPoRezId.getOrDefault(rezId, List.of());
            for (Long did : dodatekIds) {
                Dodatek d = dodatkiPoId.get(did);
                if (d != null) {
                    nowa.getDodatki().add(d);
                }
            }

            wynik.add(nowa);
        }

        return wynik;
    }

    /**
     * Zwraca rezerwacje danego klienta.
     */
    public List<Rezerwacja> pobierzRezerwacjeKlienta(Klient klient) {
        if (klient == null) throw new IllegalArgumentException("Klient nie może być null");

        final Long klientId = (long) klient.getId();

        return pobierzWszystkieRezerwacje().stream()
                .filter(r -> r.getKlient() != null)
                .filter(r -> (long) r.getKlient().getId() == klientId)
                .collect(Collectors.toList());
    }

    /**
     * Zwraca rezerwacje klienta po jego ID.
     */
    public List<Rezerwacja> pobierzRezerwacjeKlientaPoId(Long klientId) {
        if (klientId == null) throw new IllegalArgumentException("ID klienta nie może być null");

        return pobierzWszystkieRezerwacje().stream()
                .filter(r -> r.getKlient() != null && (long) r.getKlient().getId() == klientId)
                .collect(Collectors.toList());
    }

    /**
     * Sprawdza dostępność samochodu w terminie.
     */
    public boolean sprawdzDostepnosc(Samochod samochod, LocalDate dataOd, LocalDate dataDo) {
        if (samochod == null || dataOd == null || dataDo == null) {
            throw new IllegalArgumentException("Parametry nie mogą być null");
        }
        return samochod.czyDostepny(dataOd, dataDo);
    }

    /**
     * Liczy szacowaną cenę rezerwacji.
     */
    public BigDecimal obliczSzacowanaCena(Samochod samochod,
                                          LocalDate dataOd,
                                          LocalDate dataDo,
                                          List<Dodatek> dodatki) {
        if (samochod == null) throw new IllegalArgumentException("Samochód nie może być null");
        if (dataOd == null || dataDo == null) throw new IllegalArgumentException("Daty nie mogą być null");
        if (dataDo.isBefore(dataOd)) throw new IllegalArgumentException("Data do nie może być wcześniejsza niż data od");

        return policzCene(samochod, dataOd, dataDo, dodatki);
    }

    /**
     * Usuwa rezerwację po ID.
     */
    public void usunRezerwacje(Long rezerwacjaId) {
        if (rezerwacjaId == null) throw new IllegalArgumentException("ID rezerwacji nie może być null");

        boolean istnieje = pobierzWszystkieRezerwacje().stream()
                .anyMatch(r -> r.getId().equals(rezerwacjaId));
        if (!istnieje) {
            throw new IllegalArgumentException("Nie znaleziono rezerwacji o ID: " + rezerwacjaId);
        }

        rezerwacjaDAO.usunPoId(rezerwacjaId);
    }

    /**
     * Aktualizuje rezerwację: daty, dodatki i status oraz przelicza cenę.
     */
    public void aktualizujRezerwacje(Long rezerwacjaId,
                                     LocalDate nowaDataOd,
                                     LocalDate nowaDataDo,
                                     List<Dodatek> noweDodatki,
                                     StatusRezerwacji nowyStatus) {

        if (rezerwacjaId == null) throw new IllegalArgumentException("ID rezerwacji nie może być null");
        if (nowaDataOd == null || nowaDataDo == null) throw new IllegalArgumentException("Daty nie mogą być null");
        if (nowaDataDo.isBefore(nowaDataOd)) throw new IllegalArgumentException("Data do nie może być wcześniejsza niż data od");
        if (nowyStatus == null) throw new IllegalArgumentException("Status nie może być null");

        List<Rezerwacja> wszystkie = pobierzWszystkieRezerwacje();

        Rezerwacja rezerwacja = wszystkie.stream()
                .filter(r -> r.getId().equals(rezerwacjaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono rezerwacji o ID: " + rezerwacjaId));

        rezerwacja.setDataOd(nowaDataOd);
        rezerwacja.setDataDo(nowaDataDo);
        rezerwacja.setStatus(nowyStatus);

        rezerwacja.getDodatki().clear();
        if (noweDodatki != null) {
            rezerwacja.getDodatki().addAll(noweDodatki);
        }

        rezerwacja.setCenaCalkowita(policzCene(rezerwacja.getSamochod(), nowaDataOd, nowaDataDo, rezerwacja.getDodatki()));

        rezerwacjaDAO.zapiszWszystkie(wszystkie);
    }

    /**
     * Zapisuje nową rezerwację (pełny odczyt + dopisanie + zapis).
     */
    private void zapiszRezerwacje(Rezerwacja rezerwacja) {
        List<Rezerwacja> rezerwacje = pobierzWszystkieRezerwacje();
        rezerwacje.add(rezerwacja);
        rezerwacjaDAO.zapiszWszystkie(rezerwacje);
    }

    /**
     * Wylicza cenę całkowitą (bazowa stawka + dodatki).
     */
    private BigDecimal policzCene(Samochod samochod, LocalDate dataOd, LocalDate dataDo, List<Dodatek> dodatki) {
        Cennik cennik = samochod.getCennik();
        if (cennik == null) {
            throw new IllegalStateException("Samochód nie ma przypisanego cennika");
        }

        long dni = ChronoUnit.DAYS.between(dataOd, dataDo);
        if (dni <= 0) dni = 1;

        BigDecimal bazowa = cennik.getStawkaZaDobe().multiply(BigDecimal.valueOf(dni));

        BigDecimal sumaDodatkow = BigDecimal.ZERO;
        if (dodatki != null) {
            for (Dodatek dodatek : dodatki) {
                TypRozliczaniaDodatku typ = dodatek.getTypRozliczania();
                if (typ == null) continue;

                switch (typ) {
                    case ZA_DOBE:
                        sumaDodatkow = sumaDodatkow.add(dodatek.getCena().multiply(BigDecimal.valueOf(dni)));
                        break;
                    case JEDNORAZOWY:
                        sumaDodatkow = sumaDodatkow.add(dodatek.getCena());
                        break;
                    case PROCENT_OD_STAWKI:
                        sumaDodatkow = sumaDodatkow.add(bazowa.multiply(dodatek.getCena()));
                        break;
                }
            }
        }

        return bazowa.add(sumaDodatkow).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generuje kolejne ID rezerwacji.
     */
    private Long generujNoweId() {
        return pobierzWszystkieRezerwacje().stream()
                .mapToLong(Rezerwacja::getId)
                .max()
                .orElse(0L) + 1;
    }

    /**
     * Waliduje daty dla nowej rezerwacji.
     */
    private void walidujDatyNowejRezerwacji(LocalDate dataOd, LocalDate dataDo) {
        if (dataOd == null || dataDo == null) {
            throw new IllegalArgumentException("Daty nie mogą być null");
        }
        if (dataDo.isBefore(dataOd)) {
            throw new IllegalArgumentException("Data do nie może być wcześniejsza niż data od");
        }
        if (dataOd.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Data od nie może być w przeszłości");
        }
    }

    /**
     * Sprawdza czy zalogowany obiekt istnieje i jest klientem.
     */
    private Klient wymagajKlienta(Object zalogowany, String komunikatDlaNieKlienta) {
        if (zalogowany == null) {
            throw new IllegalStateException("Tylko zalogowani użytkownicy mogą wykonać tę operację.");
        }
        if (!(zalogowany instanceof Klient)) {
            throw new IllegalStateException(komunikatDlaNieKlienta);
        }
        return (Klient) zalogowany;
    }

    /**
     * Wczytuje klientów i mapuje ich po ID.
     */
    private Map<Long, Klient> wczytajKlientowPoId() {
        return uzytkownikDAO.wczytajWszystkich().stream()
                .filter(u -> u instanceof Klient)
                .map(u -> (Klient) u)
                .collect(Collectors.toMap(k -> (long) k.getId(), Function.identity()));
    }

    /**
     * Wczytuje dodatki i mapuje je po ID.
     */
    private Map<Long, Dodatek> wczytajDodatkiPoId() {
        return dodatekDAO.wczytajWszystkie().stream()
                .collect(Collectors.toMap(Dodatek::getId, Function.identity()));
    }

    /**
     * Wczytuje samochody z DAO i dopina cenniki, ale NIE dopina rezerwacji (brak cyklu z FlotaService).
     */
    private Map<Long, Samochod> wczytajSamochodyBezRezerwacjiPoId() {
        List<Samochod> samochody = samochodDAO.wczytajWszystkie();

        Map<Long, Long> cennikIdPoSamochodId = samochodDAO.wczytajMapeCennikIdPoSamochodId();
        Map<Long, Cennik> cennikiPoId = cennikDAO.wczytajWszystkie().stream()
                .collect(Collectors.toMap(Cennik::getId, Function.identity()));

        for (Samochod s : samochody) {
            Long cennikId = cennikIdPoSamochodId.get(s.getId());
            if (cennikId != null) {
                s.setCennik(cennikiPoId.get(cennikId));
            }
        }

        return samochody.stream()
                .collect(Collectors.toMap(Samochod::getId, Function.identity()));
    }
}