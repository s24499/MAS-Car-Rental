package pl.pjatk.mas.service;

import pl.pjatk.mas.dao.RezerwacjaDAO;
import pl.pjatk.mas.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class RezerwacjaService {
    private final RezerwacjaDAO rezerwacjaDAO = new RezerwacjaDAO();

    /**
     * Generuje nowe ID dla rezerwacji
     */
    private Long generujNoweId() {
        List<Rezerwacja> wszystkie = pobierzWszystkieRezerwacje();
        return wszystkie.stream()
                .mapToLong(Rezerwacja::getId)
                .max()
                .orElse(0L) + 1;
    }

    /**
     * Tworzy nową rezerwację z walidacją
     */
    public Rezerwacja utworzRezerwacje(Klient klient, Samochod samochod,
                                       LocalDate dataOd, LocalDate dataDo,
                                       List<Dodatek> wybraneDodatki) {

        // Walidacja danych wejściowych
        if (klient == null) {
            throw new IllegalArgumentException("Klient nie może być null");
        }
        if (samochod == null) {
            throw new IllegalArgumentException("Samochód nie może być null");
        }
        if (dataOd == null || dataDo == null) {
            throw new IllegalArgumentException("Daty nie mogą być null");
        }
        if (dataDo.isBefore(dataOd)) {
            throw new IllegalArgumentException("Data do nie może być wcześniejsza niż data od");
        }
        if (dataOd.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Data od nie może być w przeszłości");
        }

        // Sprawdzenie dostępności
        if (!sprawdzDostepnosc(samochod, dataOd, dataDo)) {
            throw new IllegalStateException("Samochód niedostępny w wybranym terminie");
        }

        // Generowanie ID
        Long id = generujNoweId();

        // Tworzenie rezerwacji
        Rezerwacja rezerwacja = new Rezerwacja(id, klient, samochod, dataOd, dataDo);
        if (wybraneDodatki != null) {
            rezerwacja.getDodatki().addAll(wybraneDodatki);
        }

        // Obliczenie ceny
        BigDecimal cena = policzCene(rezerwacja);
        rezerwacja.setCenaCalkowita(cena);

        // Dodanie rezerwacji do listy rezerwacji samochodu
        samochod.dodajRezerwacje(rezerwacja);

        // Zapis do pliku
        zapiszRezerwacje(rezerwacja);

        return rezerwacja;
    }

    /**
     * Anuluje rezerwację (tylko jeśli ma status NOWA)
     */
    public void anulujRezerwacje(Long rezerwacjaId) {
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
     * Pobiera wszystkie rezerwacje
     */
    public List<Rezerwacja> pobierzWszystkieRezerwacje() {
        return rezerwacjaDAO.wczytajWszystkie();
    }

    /**
     * Pobiera rezerwacje konkretnego klienta
     */
    public List<Rezerwacja> pobierzRezerwacjeKlienta(Klient klient) {
        if (klient == null) {
            throw new IllegalArgumentException("Klient nie może być null");
        }
        return pobierzWszystkieRezerwacje().stream()
                .filter(r -> r.getKlient().getId() == klient.getId())
                .collect(Collectors.toList());
    }

    /**
     * Pobiera rezerwacje dla klienta po ID klienta
     */
    public List<Rezerwacja> pobierzRezerwacjeKlientaPoId(Long klientId) {
        if (klientId == null) {
            throw new IllegalArgumentException("ID klienta nie może być null");
        }
        return pobierzWszystkieRezerwacje().stream()
                .filter(r -> r.getKlient().getId() == klientId)
                .collect(Collectors.toList());
    }

    /**
     * Sprawdza dostępność samochodu w danym terminie
     */
    public boolean sprawdzDostepnosc(Samochod samochod, LocalDate dataOd, LocalDate dataDo) {
        if (samochod == null || dataOd == null || dataDo == null) {
            throw new IllegalArgumentException("Parametry nie mogą być null");
        }
        return samochod.czyDostepny(dataOd, dataDo);
    }

    /**
     * Oblicza szacowaną cenę rezerwacji bez tworzenia rezerwacji
     */
    public BigDecimal obliczSzacowanaCena(Samochod samochod, LocalDate dataOd, LocalDate dataDo, List<Dodatek> dodatki) {
        if (samochod == null) {
            throw new IllegalArgumentException("Samochód nie może być null");
        }
        if (dataOd == null || dataDo == null) {
            throw new IllegalArgumentException("Daty nie mogą być null");
        }
        if (dataDo.isBefore(dataOd)) {
            throw new IllegalArgumentException("Data do nie może być wcześniejsza niż data od");
        }

        Cennik cennik = samochod.getCennik();
        if (cennik == null) {
            throw new IllegalStateException("Samochód nie ma przypisanego cennika");
        }

        // Liczba dni rezerwacji
        long dni = ChronoUnit.DAYS.between(dataOd, dataDo);
        if (dni <= 0) dni = 1;

        // Cena bazowa
        BigDecimal bazowa = cennik.getStawkaZaDobe().multiply(BigDecimal.valueOf(dni));

        // Suma dodatków
        BigDecimal sumaDodatkow = BigDecimal.ZERO;
        if (dodatki != null) {
            for (Dodatek dodatek : dodatki) {
                switch (dodatek.getTypRozliczania()) {
                    case ZA_DOBE:
                        sumaDodatkow = sumaDodatkow.add(
                                dodatek.getCena().multiply(BigDecimal.valueOf(dni))
                        );
                        break;
                    case JEDNORAZOWY:
                        sumaDodatkow = sumaDodatkow.add(dodatek.getCena());
                        break;
                    case PROCENT_OD_STAWKI:
                        sumaDodatkow = sumaDodatkow.add(
                                bazowa.multiply(dodatek.getCena())
                        );
                        break;
                }
            }
        }

        // Suma końcowa
        return bazowa.add(sumaDodatkow).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Oblicza cenę rezerwacji
     */
    private BigDecimal policzCene(Rezerwacja rezerwacja) {
        Samochod samochod = rezerwacja.getSamochod();
        Cennik cennik = samochod.getCennik();

        if (cennik == null) {
            throw new IllegalStateException("Samochód nie ma przypisanego cennika");
        }

        // Liczba dni rezerwacji
        long dni = ChronoUnit.DAYS.between(rezerwacja.getDataOd(), rezerwacja.getDataDo());
        if (dni <= 0) dni = 1;

        // Cena bazowa
        BigDecimal bazowa = cennik.getStawkaZaDobe().multiply(BigDecimal.valueOf(dni));

        // Suma dodatków
        BigDecimal sumaDodatkow = BigDecimal.ZERO;
        for (Dodatek dodatek : rezerwacja.getDodatki()) {
            switch (dodatek.getTypRozliczania()) {
                case ZA_DOBE:
                    sumaDodatkow = sumaDodatkow.add(
                            dodatek.getCena().multiply(BigDecimal.valueOf(dni))
                    );
                    break;
                case JEDNORAZOWY:
                    sumaDodatkow = sumaDodatkow.add(dodatek.getCena());
                    break;
                case PROCENT_OD_STAWKI:
                    sumaDodatkow = sumaDodatkow.add(
                            bazowa.multiply(dodatek.getCena())
                    );
                    break;
            }
        }

        // Suma końcowa
        return bazowa.add(sumaDodatkow).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Zapisuje rezerwację do pliku
     */
    private void zapiszRezerwacje(Rezerwacja rezerwacja) {
        List<Rezerwacja> rezerwacje = pobierzWszystkieRezerwacje();
        rezerwacje.add(rezerwacja);
        rezerwacjaDAO.zapiszWszystkie(rezerwacje);
    }
}