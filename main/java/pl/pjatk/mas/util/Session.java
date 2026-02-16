package pl.pjatk.mas.util;

import pl.pjatk.mas.model.Uzytkownik;

public class Session {

    // Aktualnie zalogowany użytkownik (może być null = gość)
    private static Uzytkownik zalogowany;

    // Zwraca zalogowanego użytkownika
    public static Uzytkownik getZalogowany() {
        return zalogowany;
    }

    // Ustawia zalogowanego użytkownika (lub null, gdy wylogowanie / gość)
    public static void setZalogowany(Uzytkownik uzytkownik) {
        zalogowany = uzytkownik;
    }
}
