package pl.pjatk.mas.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Dodatek {
    private Long id;
    private String nazwa;
    private BigDecimal cena;
    private TypRozliczaniaDodatku typRozliczania;
    private List<KategoriaSamochodu> dostepneKategorie = new ArrayList<>();

    public Dodatek(Long id, String nazwa, BigDecimal cena,
                   TypRozliczaniaDodatku typRozliczania,
                   List<KategoriaSamochodu> dostepneKategorie) {
        this.id = id;
        this.nazwa = nazwa;
        this.cena = cena;
        this.typRozliczania = typRozliczania;
        if (dostepneKategorie != null) {
            this.dostepneKategorie = dostepneKategorie;
        }
    }

    // GETTERY I SETTERY
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNazwa() {
        return nazwa;
    }

    public void setNazwa(String nazwa) {
        this.nazwa = nazwa;
    }

    public BigDecimal getCena() {
        return cena;
    }

    public void setCena(BigDecimal cena) {
        this.cena = cena;
    }

    public TypRozliczaniaDodatku getTypRozliczania() {
        return typRozliczania;
    }

    public void setTypRozliczania(TypRozliczaniaDodatku typRozliczania) {
        this.typRozliczania = typRozliczania;
    }

    public List<KategoriaSamochodu> getDostepneKategorie() {
        return dostepneKategorie;
    }

    public void setDostepneKategorie(List<KategoriaSamochodu> dostepneKategorie) {
        this.dostepneKategorie = dostepneKategorie;
    }

    public boolean czyDostepnyDlaKategorii(KategoriaSamochodu kategoria) {
        return dostepneKategorie.isEmpty() || dostepneKategorie.contains(kategoria);
    }

    @Override
    public String toString() {
        return nazwa + " (" + cena + " zł - " + typRozliczania + ")";
    }
}