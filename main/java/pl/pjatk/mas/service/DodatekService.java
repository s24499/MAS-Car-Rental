package pl.pjatk.mas.service;

import pl.pjatk.mas.dao.DodatekDAO;
import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.Samochod;

import java.util.List;
import java.util.stream.Collectors;

public class DodatekService {
    private final DodatekDAO dodatekDAO = new DodatekDAO();

    // Zwraca listę wszystkich dodatków
    public List<Dodatek> pobierzWszystkieDodatki() {
        return dodatekDAO.wczytajWszystkie();
    }

    // Zwraca dodatki dostępne dla konkretnego samochodu (wg kategorii)
    public List<Dodatek> pobierzDodatkiDlaSamochodu(Samochod samochod) {
        if (samochod == null) {
            throw new IllegalArgumentException("Samochód nie może być null");
        }

        return pobierzWszystkieDodatki().stream()
                .filter(d -> d.czyDostepnyDlaKategorii(samochod.getKategoria()))
                .collect(Collectors.toList());
    }

    // Zwraca dodatki dla konkretnej kategorii
    public List<Dodatek> pobierzDodatkiDlaKategorii(KategoriaSamochodu kategoria) {
        if (kategoria == null) {
            throw new IllegalArgumentException("Kategoria nie może być null");
        }

        return pobierzWszystkieDodatki().stream()
                .filter(d -> d.czyDostepnyDlaKategorii(kategoria))
                .collect(Collectors.toList());
    }

    // Znajduje dodatek po ID
    public Dodatek znajdzDodatekPoId(Long id) {
        return dodatekDAO.znajdzPoId(id);
    }

    // Dodaje nowy dodatek
    public void dodajDodatek(Dodatek dodatek) {
        List<Dodatek> dodatki = pobierzWszystkieDodatki();

        // Sprawdź unikalność ID
        if (dodatki.stream().anyMatch(d -> d.getId().equals(dodatek.getId()))) {
            throw new IllegalArgumentException("Dodatek o tym ID już istnieje");
        }

        dodatki.add(dodatek);
        dodatekDAO.zapiszWszystkie(dodatki);
    }

    // Aktualizuje istniejący dodatek
    public void aktualizujDodatek(Dodatek dodatek) {
        dodatekDAO.aktualizuj(dodatek);
    }

    // Usuwa dodatek
    public void usunDodatek(Long id) {
        dodatekDAO.usunPoId(id);
    }
}