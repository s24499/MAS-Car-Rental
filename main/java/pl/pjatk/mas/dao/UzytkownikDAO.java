package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.Klient;
import pl.pjatk.mas.model.Pracownik;
import pl.pjatk.mas.model.Uzytkownik;

import java.util.ArrayList;
import java.util.List;

public class UzytkownikDAO extends BazaDAO {
    private static final String PLIK = "uzytkownicy.csv";
    private static final String HEADER = "typ;id;login;haslo;imie;nazwisko;email";

    /** Odczytuje wszystkich użytkowników z CSV. */
    public List<Uzytkownik> wczytajWszystkich() {
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));
        List<Uzytkownik> uzytkownicy = new ArrayList<>();

        if (linie.size() <= 1) {
            return uzytkownicy; // brak danych lub sam nagłówek
        }

        for (int i = 1; i < linie.size(); i++) {
            Uzytkownik u = parseUzytkownik(linie.get(i));
            if (u != null) {
                uzytkownicy.add(u);
            }
        }

        return uzytkownicy;
    }

    /** Zapisuje użytkowników do CSV (nadpisuje plik). */
    public void zapiszWszystkich(List<Uzytkownik> uzytkownicy) {
        List<String> linie = new ArrayList<>();
        linie.add(HEADER);

        if (uzytkownicy != null) {
            for (Uzytkownik u : uzytkownicy) {
                String linia = toLinia(u);
                if (linia != null) {
                    linie.add(linia);
                }
            }
        }

        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    /** Wyszukuje użytkownika po ID (pełny odczyt pliku). */
    public Uzytkownik znajdzPoId(int id) {
        return wczytajWszystkich().stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /** Usuwa użytkownika po ID. */
    public void usunPoId(int id) {
        List<Uzytkownik> uzytkownicy = wczytajWszystkich();
        uzytkownicy.removeIf(u -> u.getId() == id);
        zapiszWszystkich(uzytkownicy);
    }

    /** Odpowiada za parsowanie jednej linii CSV do obiektu użytkownika. */
    private Uzytkownik parseUzytkownik(String linia) {
        try {
            String[] dane = linia.split(";", -1);

            String typ = dane[0];
            int id = Integer.parseInt(dane[1]);
            String login = dane[2];
            String haslo = dane[3];
            String imie = dane[4];
            String nazwisko = dane[5];

            if ("KLIENT".equals(typ)) {
                String email = dane.length > 6 ? dane[6] : "";
                return new Klient(id, login, haslo, imie, nazwisko, email);
            }

            if ("PRACOWNIK".equals(typ)) {
                return new Pracownik(id, login, haslo, imie, nazwisko);
            }

            return null;
        } catch (Exception e) {
            System.err.println("Błąd podczas wczytywania użytkownika: " + e.getMessage());
            return null;
        }
    }

    /** Odpowiada za budowanie jednej linii CSV na podstawie obiektu użytkownika. */
    private String toLinia(Uzytkownik u) {
        if (u instanceof Klient klient) {
            return "KLIENT;" +
                    klient.getId() + ";" +
                    klient.getLogin() + ";" +
                    klient.getHaslo() + ";" +
                    klient.getImie() + ";" +
                    klient.getNazwisko() + ";" +
                    klient.getEmail();
        }

        if (u instanceof Pracownik pracownik) {
            return "PRACOWNIK;" +
                    pracownik.getId() + ";" +
                    pracownik.getLogin() + ";" +
                    pracownik.getHaslo() + ";" +
                    pracownik.getImie() + ";" +
                    pracownik.getNazwisko() + ";";
        }

        return null;
    }
}