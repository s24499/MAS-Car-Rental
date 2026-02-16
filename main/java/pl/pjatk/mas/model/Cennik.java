package pl.pjatk.mas.model;

import java.math.BigDecimal;

/**
 * Cennik przypisany do kategorii samochodu.
 * Określa podstawową cenę za dobę i ewentualny procent dopłaty.
 */
public class Cennik {

    private Long id;
    private KategoriaSamochodu kategoria;
    private BigDecimal stawkaZaDobe;
    private BigDecimal procentDodatkowyKierowca;

    public Cennik(Long id,
                  KategoriaSamochodu kategoria,
                  BigDecimal stawkaZaDobe,
                  BigDecimal procentDodatkowyKierowca) {
        this.id = id;
        this.kategoria = kategoria;
        this.stawkaZaDobe = stawkaZaDobe;
        this.procentDodatkowyKierowca = procentDodatkowyKierowca;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KategoriaSamochodu getKategoria() {
        return kategoria;
    }

    public void setKategoria(KategoriaSamochodu kategoria) {
        this.kategoria = kategoria;
    }

    public BigDecimal getStawkaZaDobe() {
        return stawkaZaDobe;
    }

    public void setStawkaZaDobe(BigDecimal stawkaZaDobe) {
        this.stawkaZaDobe = stawkaZaDobe;
    }

    public BigDecimal getProcentDodatkowyKierowca() {
        return procentDodatkowyKierowca;
    }

    public void setProcentDodatkowyKierowca(BigDecimal procentDodatkowyKierowca) {
        this.procentDodatkowyKierowca = procentDodatkowyKierowca;
    }

    @Override
    public String toString() {
        return "Cennik{" +
                "id=" + id +
                ", kategoria=" + kategoria +
                ", stawkaZaDobe=" + stawkaZaDobe +
                ", procentDodatkowyKierowca=" + procentDodatkowyKierowca +
                '}';
    }
}