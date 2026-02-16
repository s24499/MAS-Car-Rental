package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RezerwacjaDAO extends BazaDAO {
    private static final String PLIK = "rezerwacje.csv";

    private final UzytkownikDAO uzytkownikDAO = new UzytkownikDAO();
    private final DodatekDAO dodatekDAO = new DodatekDAO();

    // Wczytuje wszystkie rezerwacje
    public List<Rezerwacja> wczytajWszystkie() {
        List<Samochod> samochody = new SamochodDAO().wczytajWszystkie();
        return wczytajWszystkieZSamochodami(samochody);
    }

    // Wczytuje wszystkie rezerwacje z podaną listą samochodów
    public List<Rezerwacja> wczytajWszystkieZSamochodami(List<Samochod> samochody) {
        List<Rezerwacja> rezerwacje = new ArrayList<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        if (linie.isEmpty()) {
            return rezerwacje;
        }

        // Pobranie potrzebnych danych
        List<Klient> klienci = uzytkownikDAO.wczytajWszystkich().stream()
                .filter(u -> u instanceof Klient)
                .map(u -> (Klient) u)
                .collect(Collectors.toList());

        List<Dodatek> dodatki = dodatekDAO.wczytajWszystkie();

        // Pomijamy nagłówek
        for (int i = 1; i < linie.size(); i++) {
            try {
                String[] dane = linie.get(i).split(";", -1);

                // Zabezpieczenie przed nieprawidłowym formatem
                if (dane.length < 7) {
                    System.err.println("Pominięto nieprawidłową linię: " + linie.get(i));
                    continue;
                }

                Long id = Long.parseLong(dane[0]);
                Long klientId = Long.parseLong(dane[1]);
                Long samochodId = Long.parseLong(dane[2]);
                LocalDate dataOd = LocalDate.parse(dane[3]);
                LocalDate dataDo = LocalDate.parse(dane[4]);
                StatusRezerwacji status = StatusRezerwacji.valueOf(dane[5]);
                BigDecimal cenaCalkowita = new BigDecimal(dane[6]);
                String dodatkiIds = dane.length > 7 ? dane[7] : "";

                // Znajdź klienta
                Klient klient = klienci.stream()
                        .filter(k -> k.getId() == klientId)
                        .findFirst()
                        .orElse(null);

                // Znajdź samochód
                Samochod samochod = samochody.stream()
                        .filter(s -> s.getId().equals(samochodId))
                        .findFirst()
                        .orElse(null);

                if (klient != null && samochod != null) {
                    Rezerwacja rezerwacja = new Rezerwacja(id, klient, samochod, dataOd, dataDo);
                    rezerwacja.setStatus(status);
                    rezerwacja.setCenaCalkowita(cenaCalkowita);

                    // Dodaj dodatki
                    if (!dodatkiIds.isEmpty()) {
                        String[] idsArray = dodatkiIds.split(",");
                        for (String dodatekIdStr : idsArray) {
                            try {
                                Long dodatekId = Long.parseLong(dodatekIdStr);
                                dodatki.stream()
                                        .filter(d -> d.getId().equals(dodatekId))
                                        .findFirst()
                                        .ifPresent(d -> rezerwacja.getDodatki().add(d));
                            } catch (NumberFormatException e) {
                                System.err.println("Nieprawidłowy format ID dodatku: " + dodatekIdStr);
                            }
                        }
                    }

                    rezerwacje.add(rezerwacja);
                }
            } catch (Exception e) {
                System.err.println("Błąd podczas wczytywania rezerwacji w linii " + (i + 1) + ": " + e.getMessage());
            }
        }
        return rezerwacje;
    }

    // Zapisuje wszystkie rezerwacje
    public void zapiszWszystkie(List<Rezerwacja> rezerwacje) {
        List<String> linie = new ArrayList<>();
        linie.add("id;klientId;samochodId;dataOd;dataDo;status;cenaCalkowita;dodatkiIds");

        for (Rezerwacja r : rezerwacje) {
            String dodatkiIds = r.getDodatki().stream()
                    .map(d -> d.getId().toString())
                    .collect(Collectors.joining(","));

            linie.add(r.getId() + ";" +
                    r.getKlient().getId() + ";" +
                    r.getSamochod().getId() + ";" +
                    r.getDataOd() + ";" +
                    r.getDataDo() + ";" +
                    r.getStatus() + ";" +
                    r.getCenaCalkowita() + ";" +
                    dodatkiIds);
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    // Znajduje rezerwację po ID
    public Rezerwacja znajdzPoId(Long id) {
        return wczytajWszystkie().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Aktualizuje rezerwację
    public void aktualizuj(Rezerwacja rezerwacja) {
        List<Rezerwacja> rezerwacje = wczytajWszystkie();
        for (int i = 0; i < rezerwacje.size(); i++) {
            if (rezerwacje.get(i).getId().equals(rezerwacja.getId())) {
                rezerwacje.set(i, rezerwacja);
                break;
            }
        }
        zapiszWszystkie(rezerwacje);
    }

    // Usuwa rezerwację po ID
    public void usunPoId(Long id) {
        List<Rezerwacja> rezerwacje = wczytajWszystkie();
        rezerwacje.removeIf(r -> r.getId().equals(id));
        zapiszWszystkie(rezerwacje);
    }
}