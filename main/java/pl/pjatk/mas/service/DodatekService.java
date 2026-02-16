package pl.pjatk.mas.service;

import pl.pjatk.mas.dao.DodatekDAO;
import pl.pjatk.mas.model.Dodatek;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.Samochod;
import pl.pjatk.mas.model.TypRozliczaniaDodatku;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serwis odpowiedzialny za zarządzanie dodatkami.
 * Udostępnia odczyt, filtrowanie po kategorii oraz operacje CRUD.
 */
public class DodatekService {

    private final DodatekDAO dodatekDAO = new DodatekDAO();

    /**
     * Zwraca wszystkie dodatki.
     */
    public List<Dodatek> pobierzWszystkieDodatki() {
        return dodatekDAO.wczytajWszystkie();
    }

    /**
     * Zwraca dodatki dostępne dla samochodu (wg kategorii samochodu).
     */
    public List<Dodatek> pobierzDodatkiDlaSamochodu(Samochod samochod) {
        if (samochod == null) {
            throw new IllegalArgumentException("Samochód nie może być null");
        }
        return pobierzDodatkiDlaKategorii(samochod.getKategoria());
    }

    /**
     * Zwraca dodatki dostępne dla podanej kategorii.
     */
    public List<Dodatek> pobierzDodatkiDlaKategorii(KategoriaSamochodu kategoria) {
        if (kategoria == null) {
            throw new IllegalArgumentException("Kategoria nie może być null");
        }

        return pobierzWszystkieDodatki().stream()
                .filter(d -> d.czyDostepnyDlaKategorii(kategoria))
                .collect(Collectors.toList());
    }

    /**
     * Zwraca dodatek po ID (lub null jeśli nie istnieje).
     */
    public Dodatek znajdzDodatekPoId(Long id) {
        return dodatekDAO.znajdzPoId(id);
    }

    /**
     * Dodaje nowy dodatek na podstawie danych z GUI.
     * Waliduje dane, wyznacza ID i zapisuje do pliku.
     */
    public Dodatek dodajDodatek(String nazwa,
                                BigDecimal cena,
                                TypRozliczaniaDodatku typ,
                                List<KategoriaSamochodu> kategorie) {
        String nazwaOk = wymaganaNazwa(nazwa);
        walidujCene(cena);
        wymaganyTyp(typ);

        List<KategoriaSamochodu> kategorieOk = bezpieczneKategorie(kategorie);

        List<Dodatek> dodatki = pobierzWszystkieDodatki();
        long noweId = wyznaczNoweId(dodatki);

        Dodatek nowy = new Dodatek(noweId, nazwaOk, cena, typ, kategorieOk);
        dodatki.add(nowy);
        dodatekDAO.zapiszWszystkie(dodatki);

        return nowy;
    }

    /**
     * Aktualizuje dane istniejącego dodatku.
     */
    public void aktualizujDodatek(Long dodatekId,
                                  String nazwa,
                                  BigDecimal cena,
                                  TypRozliczaniaDodatku typ,
                                  List<KategoriaSamochodu> kategorie) {
        if (dodatekId == null) {
            throw new IllegalArgumentException("ID dodatku nie może być null");
        }

        String nazwaOk = wymaganaNazwa(nazwa);
        walidujCene(cena);
        wymaganyTyp(typ);

        List<KategoriaSamochodu> kategorieOk = bezpieczneKategorie(kategorie);

        Dodatek dodatek = dodatekDAO.znajdzPoId(dodatekId);
        if (dodatek == null) {
            throw new IllegalArgumentException("Nie znaleziono dodatku o ID: " + dodatekId);
        }

        dodatek.setNazwa(nazwaOk);
        dodatek.setCena(cena);
        dodatek.setTypRozliczania(typ);
        dodatek.setDostepneKategorie(new ArrayList<>(kategorieOk));

        dodatekDAO.aktualizuj(dodatek);
    }

    /**
     * Usuwa dodatek o podanym ID.
     */
    public void usunDodatek(Long dodatekId) {
        if (dodatekId == null) {
            throw new IllegalArgumentException("ID dodatku nie może być null");
        }

        Dodatek dodatek = dodatekDAO.znajdzPoId(dodatekId);
        if (dodatek == null) {
            throw new IllegalArgumentException("Nie znaleziono dodatku o ID: " + dodatekId);
        }

        dodatekDAO.usunPoId(dodatekId);
    }

    /**
     * Waliduje nazwę i zwraca wersję po trim().
     */
    private String wymaganaNazwa(String nazwa) {
        if (nazwa == null || nazwa.trim().isEmpty()) {
            throw new IllegalArgumentException("Nazwa nie może być pusta");
        }
        return nazwa.trim();
    }

    /**
     * Waliduje cenę dodatku.
     */
    private void walidujCene(BigDecimal cena) {
        if (cena == null || cena.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cena musi być większa niż 0");
        }
    }

    /**
     * Waliduje typ rozliczania dodatku.
     */
    private void wymaganyTyp(TypRozliczaniaDodatku typ) {
        if (typ == null) {
            throw new IllegalArgumentException("Typ rozliczania nie może być null");
        }
    }

    /**
     * Normalizuje listę kategorii: null/pusta lista oznacza "dostępny dla wszystkich".
     */
    private List<KategoriaSamochodu> bezpieczneKategorie(List<KategoriaSamochodu> kategorie) {
        return (kategorie == null) ? new ArrayList<>() : new ArrayList<>(kategorie);
    }

    /**
     * Wyznacza kolejne ID dodatku na podstawie danych w pliku.
     */
    private long wyznaczNoweId(List<Dodatek> dodatki) {
        return dodatki.stream()
                .mapToLong(Dodatek::getId)
                .max()
                .orElse(0L) + 1;
    }
}