package pl.pjatk.mas.model;

import java.math.BigDecimal;

/**
 * Cennik przypisany do samochodu.
 * Określa podstawową cenę za dobę i ewentualny procent dopłaty.
 */

public class Cennik {

    private Long id;
    private BigDecimal stawkaZaDobe;
    private BigDecimal procentDodatkowyKierowca;

    public Cennik(Long id,
                  BigDecimal stawkaZaDobe,
                  BigDecimal procentDodatkowyKierowca) {
        this.id = id;
        this.stawkaZaDobe = stawkaZaDobe;
        this.procentDodatkowyKierowca = procentDodatkowyKierowca;
    }

    /**
     * GETTERY I SETTERY
     */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
