package pl.pjatk.mas.service;

import pl.pjatk.mas.dao.UzytkownikDAO;
import pl.pjatk.mas.model.Klient;
import pl.pjatk.mas.model.Uzytkownik;

import java.util.List;

public class AuthService {
    private final UzytkownikDAO uzytkownikDAO = new UzytkownikDAO();

    /**
     * Loguje użytkownika
     */
    public Uzytkownik zaloguj(String login, String haslo) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("Login nie może być pusty");
        }
        if (haslo == null || haslo.trim().isEmpty()) {
            throw new IllegalArgumentException("Hasło nie może być puste");
        }

        List<Uzytkownik> uzytkownicy = uzytkownikDAO.wczytajWszystkich();

        return uzytkownicy.stream()
                .filter(u -> u.getLogin().equals(login) && u.getHaslo().equals(haslo))
                .findFirst()
                .orElse(null);
    }

    /**
     * Rejestruje nowego klienta
     */
    public boolean zarejestruj(String login, String haslo,
                               String imie, String nazwisko, String email) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("Login nie może być pusty");
        }
        if (haslo == null || haslo.trim().isEmpty()) {
            throw new IllegalArgumentException("Hasło nie może być puste");
        }
        if (imie == null || imie.trim().isEmpty()) {
            throw new IllegalArgumentException("Imię nie może być puste");
        }
        if (nazwisko == null || nazwisko.trim().isEmpty()) {
            throw new IllegalArgumentException("Nazwisko nie może być puste");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email nie może być pusty");
        }

        // Walidacja formatu email (podstawowa)
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Podaj prawidłowy adres email");
        }

        // Walidacja długości hasła
        if (haslo.length() < 4) {
            throw new IllegalArgumentException("Hasło musi mieć co najmniej 4 znaki");
        }

        List<Uzytkownik> uzytkownicy = uzytkownikDAO.wczytajWszystkich();

        // Sprawdź unikalność loginu
        if (uzytkownicy.stream().anyMatch(u -> u.getLogin().equals(login))) {
            return false;
        }

        // Znajdź najwyższe ID
        int maxId = uzytkownicy.stream()
                .mapToInt(Uzytkownik::getId)
                .max()
                .orElse(0);

        // Utwórz nowego klienta
        Klient nowyKlient = new Klient(maxId + 1, login, haslo, imie, nazwisko, email);
        uzytkownicy.add(nowyKlient);
        uzytkownikDAO.zapiszWszystkich(uzytkownicy);

        return true;
    }

    /**
     * Resetuje hasło
     */
    public boolean resetujHaslo(String login, String noweHaslo) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("Login nie może być pusty");
        }
        if (noweHaslo == null || noweHaslo.trim().isEmpty()) {
            throw new IllegalArgumentException("Nowe hasło nie może być puste");
        }

        // Walidacja długości hasła
        if (noweHaslo.length() < 4) {
            throw new IllegalArgumentException("Hasło musi mieć co najmniej 4 znaki");
        }

        List<Uzytkownik> uzytkownicy = uzytkownikDAO.wczytajWszystkich();

        for (Uzytkownik u : uzytkownicy) {
            if (u.getLogin().equals(login)) {
                u.setHaslo(noweHaslo);
                uzytkownikDAO.zapiszWszystkich(uzytkownicy);
                return true;
            }
        }
        return false;
    }

    /**
     * Znajduje użytkownika po ID
     */
    public Uzytkownik znajdzUzytkownikaPoId(int id) {
        return uzytkownikDAO.znajdzPoId(id);
    }

    /**
     * Sprawdza czy login jest dostępny
     */
    public boolean czyLoginWolny(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("Login nie może być pusty");
        }

        List<Uzytkownik> uzytkownicy = uzytkownikDAO.wczytajWszystkich();
        return uzytkownicy.stream().noneMatch(u -> u.getLogin().equals(login));
    }
}