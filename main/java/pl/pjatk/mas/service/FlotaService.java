package pl.pjatk.mas.service;

import pl.pjatk.mas.dao.RezerwacjaDAO;
import pl.pjatk.mas.dao.SamochodDAO;
import pl.pjatk.mas.model.KategoriaSamochodu;
import pl.pjatk.mas.model.Rezerwacja;
import pl.pjatk.mas.model.Samochod;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlotaService {
    private final SamochodDAO samochodDAO = new SamochodDAO();
    private final RezerwacjaDAO rezerwacjaDAO = new RezerwacjaDAO();

    // Zwraca wszystkie samochody wraz z ich rezerwacjami
    public List<Samochod> pobierzWszystkieSamochody() {
        List<Samochod> samochody = samochodDAO.wczytajWszystkie();
        List<Rezerwacja> rezerwacje = rezerwacjaDAO.wczytajWszystkieZSamochodami(samochody);

        // Mapowanie samochodów po ID dla szybkiego dostępu
        Map<Long, Samochod> mapaSamochodow = samochody.stream()
                .collect(Collectors.toMap(Samochod::getId, s -> s));

        // Dodajemy rezerwacje do odpowiednich samochodów
        for (Rezerwacja r : rezerwacje) {
            Samochod s = mapaSamochodow.get(r.getSamochod().getId());
            if (s != null && !s.getRezerwacje().contains(r)) {
                s.dodajRezerwacje(r);
            }
        }
        return samochody;
    }

    // Zwraca samochody dostępne w danym terminie
    public List<Samochod> pobierzDostepneSamochody(LocalDate dataOd, LocalDate dataDo) {
        if (dataOd == null || dataDo == null) {
            throw new IllegalArgumentException("Daty nie mogą być null");
        }
        if (dataDo.isBefore(dataOd)) {
            throw new IllegalArgumentException("Data do nie może być wcześniejsza niż data od");
        }

        return pobierzWszystkieSamochody().stream()
                .filter(s -> s.czyDostepny(dataOd, dataDo))
                .collect(Collectors.toList());
    }

    // Filtruje samochody po kategorii
    public List<Samochod> filtrujPoKategorii(List<Samochod> samochody, KategoriaSamochodu kategoria) {
        if (kategoria == null) {
            throw new IllegalArgumentException("Kategoria nie może być null");
        }

        if (samochody == null) {
            samochody = pobierzWszystkieSamochody();
        }

        return samochody.stream()
                .filter(s -> s.getKategoria() == kategoria)
                .collect(Collectors.toList());
    }

    // Filtruje samochody dostępne w terminie i po kategorii
    public List<Samochod> pobierzDostepneSamochodyFiltrowane(LocalDate dataOd, LocalDate dataDo,
                                                             KategoriaSamochodu kategoria) {
        List<Samochod> dostepne = pobierzDostepneSamochody(dataOd, dataDo);
        return filtrujPoKategorii(dostepne, kategoria);
    }

    // Znajduje samochód po ID
    public Samochod znajdzSamochodPoId(Long id) {
        return pobierzWszystkieSamochody().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Znajduje samochód po numerze rejestracyjnym
    public Samochod znajdzSamochodPoNumerzeRejestracyjnym(String numer) {
        if (numer == null || numer.trim().isEmpty()) {
            throw new IllegalArgumentException("Numer rejestracyjny nie może być pusty");
        }

        return pobierzWszystkieSamochody().stream()
                .filter(s -> s.getNumerRejestracyjny().equalsIgnoreCase(numer.trim()))
                .findFirst()
                .orElse(null);
    }
}