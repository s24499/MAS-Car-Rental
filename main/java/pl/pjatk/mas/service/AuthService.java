package pl.pjatk.mas.service;

import pl.pjatk.mas.dao.UzytkownikDAO;
import pl.pjatk.mas.model.Klient;
import pl.pjatk.mas.model.Uzytkownik;

import java.util.List;

/**
 * Serwis odpowiedzialny za autoryzację i zarządzanie kontami użytkowników.
 * Waliduje dane wejściowe i zapisuje zmiany przez UzytkownikDAO.
 */
public class AuthService {

    private static final int MIN_DLUGOSC_HASLA = 4;

    private final UzytkownikDAO uzytkownikDAO = new UzytkownikDAO();

    /**
     * Weryfikuje login i hasło użytkownika.
     * Zwraca użytkownika jeśli dane są poprawne, w przeciwnym razie null.
     */
    public Uzytkownik zaloguj(String login, String haslo) {
        String l = wymaganyTekst(login, "Login nie może być pusty");
        String h = wymaganyTekst(haslo, "Hasło nie może być puste");

        return uzytkownikDAO.wczytajWszystkich().stream()
                .filter(u -> u.getLogin().equals(l) && u.getHaslo().equals(h))
                .findFirst()
                .orElse(null);
    }

    /**
     * Rejestruje nowego klienta.
     * Zwraca false, jeśli login jest zajęty.
     */
    public boolean zarejestruj(String login, String haslo, String imie, String nazwisko, String email) {
        String l = wymaganyTekst(login, "Login nie może być pusty");
        String h = wymaganyTekst(haslo, "Hasło nie może być puste");
        String i = wymaganyTekst(imie, "Imię nie może być puste");
        String n = wymaganyTekst(nazwisko, "Nazwisko nie może być puste");
        String e = wymaganyTekst(email, "Email nie może być pusty");

        walidujEmail(e);
        walidujHaslo(h);

        List<Uzytkownik> uzytkownicy = uzytkownikDAO.wczytajWszystkich();
        if (czyLoginZajety(uzytkownicy, l)) {
            return false;
        }

        int noweId = wyznaczNoweId(uzytkownicy);
        uzytkownicy.add(new Klient(noweId, l, h, i, n, e));
        uzytkownikDAO.zapiszWszystkich(uzytkownicy);

        return true;
    }

    /**
     * Resetuje hasło użytkownika o podanym loginie.
     * Zwraca false, jeśli użytkownik nie istnieje.
     */
    public boolean resetujHaslo(String login, String noweHaslo) {
        String l = wymaganyTekst(login, "Login nie może być pusty");
        String h = wymaganyTekst(noweHaslo, "Nowe hasło nie może być puste");

        walidujHaslo(h);

        List<Uzytkownik> uzytkownicy = uzytkownikDAO.wczytajWszystkich();

        for (Uzytkownik u : uzytkownicy) {
            if (u.getLogin().equals(l)) {
                u.setHaslo(h);
                uzytkownikDAO.zapiszWszystkich(uzytkownicy);
                return true;
            }
        }

        return false;
    }

    /**
     * Sprawdza czy tekst jest wymagany i zwraca jego wersję bez spacji na brzegach.
     */
    private String wymaganyTekst(String value, String komunikatBledu) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(komunikatBledu);
        }
        return value.trim();
    }

    /**
     * Waliduje minimalne wymagania dla hasła.
     */
    private void walidujHaslo(String haslo) {
        if (haslo.length() < MIN_DLUGOSC_HASLA) {
            throw new IllegalArgumentException("Hasło musi mieć co najmniej " + MIN_DLUGOSC_HASLA + " znaki");
        }
    }

    /**
     * Waliduje format adresu email (podstawowa walidacja).
     */
    private void walidujEmail(String email) {
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Podaj prawidłowy adres email");
        }
    }

    /**
     * Sprawdza czy login jest już używany.
     */
    private boolean czyLoginZajety(List<Uzytkownik> uzytkownicy, String login) {
        return uzytkownicy.stream().anyMatch(u -> u.getLogin().equals(login));
    }

    /**
     * Wyznacza kolejne ID użytkownika na podstawie danych w pliku.
     */
    private int wyznaczNoweId(List<Uzytkownik> uzytkownicy) {
        int maxId = uzytkownicy.stream()
                .mapToInt(Uzytkownik::getId)
                .max()
                .orElse(0);
        return maxId + 1;
    }
}