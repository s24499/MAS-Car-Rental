package pl.pjatk.mas.dao;

import pl.pjatk.mas.model.Klient;
import pl.pjatk.mas.model.Pracownik;
import pl.pjatk.mas.model.Uzytkownik;

import java.util.ArrayList;
import java.util.List;

public class UzytkownikDAO extends BazaDAO {
    private static final String PLIK = "uzytkownicy.csv";

    // Wczytuje wszystkich użytkowników
    public List<Uzytkownik> wczytajWszystkich() {
        List<Uzytkownik> uzytkownicy = new ArrayList<>();
        List<String> linie = wczytajLinie(sciezkaDoPliku(PLIK));

        // Pominięcie nagłówka
        for (int i = 1; i < linie.size(); i++) {
            String[] dane = linie.get(i).split(";", -1); // -1 aby zachować puste pola
            String typ = dane[0];
            int id = Integer.parseInt(dane[1]);
            String login = dane[2];
            String haslo = dane[3];
            String imie = dane[4];
            String nazwisko = dane[5];

            if ("KLIENT".equals(typ)) {
                String email = dane.length > 6 ? dane[6] : "";
                uzytkownicy.add(new Klient(id, login, haslo, imie, nazwisko, email));
            } else if ("PRACOWNIK".equals(typ)) {
                uzytkownicy.add(new Pracownik(id, login, haslo, imie, nazwisko));
            }
        }
        return uzytkownicy;
    }

    // Zapisuje wszystkich użytkowników do pliku
    public void zapiszWszystkich(List<Uzytkownik> uzytkownicy) {
        List<String> linie = new ArrayList<>();
        linie.add("typ;id;login;haslo;imie;nazwisko;email");

        for (Uzytkownik u : uzytkownicy) {
            if (u instanceof Klient klient) {
                linie.add("KLIENT;" +
                        klient.getId() + ";" +
                        klient.getLogin() + ";" +
                        klient.getHaslo() + ";" +
                        klient.getImie() + ";" +
                        klient.getNazwisko() + ";" +
                        klient.getEmail());
            } else if (u instanceof Pracownik pracownik) {
                linie.add("PRACOWNIK;" +
                        pracownik.getId() + ";" +
                        pracownik.getLogin() + ";" +
                        pracownik.getHaslo() + ";" +
                        pracownik.getImie() + ";" +
                        pracownik.getNazwisko() + ";");
            }
        }
        zapiszLinie(sciezkaDoPliku(PLIK), linie);
    }

    // Znajduje użytkownika po ID
    public Uzytkownik znajdzPoId(int id) {
        return wczytajWszystkich().stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // Aktualizuje istniejącego użytkownika
    public void aktualizuj(Uzytkownik uzytkownik) {
        List<Uzytkownik> uzytkownicy = wczytajWszystkich();
        for (int i = 0; i < uzytkownicy.size(); i++) {
            if (uzytkownicy.get(i).getId() == uzytkownik.getId()) {
                uzytkownicy.set(i, uzytkownik);
                break;
            }
        }
        zapiszWszystkich(uzytkownicy);
    }

    // Usuwa użytkownika po ID
    public void usunPoId(int id) {
        List<Uzytkownik> uzytkownicy = wczytajWszystkich();
        uzytkownicy.removeIf(u -> u.getId() == id);
        zapiszWszystkich(uzytkownicy);
    }
}
