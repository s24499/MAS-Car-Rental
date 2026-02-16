package pl.pjatk.mas.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Rezerwacja samochodu dokonana przez klienta.
 */
public class Rezerwacja {

    private Long id;
    private Klient klient;
    private Samochod samochod;
    private LocalDate dataOd;
    private LocalDate dataDo;
    private StatusRezerwacji status;
    private BigDecimal cenaCalkowita;
    private List<Dodatek> dodatki = new ArrayList<>();

    public Rezerwacja(Long id,
                      Klient klient,
                      Samochod samochod,
                      LocalDate dataOd,
                      LocalDate dataDo) {
        this.id = id;
        this.klient = klient;
        this.samochod = samochod;
        this.dataOd = dataOd;
        this.dataDo = dataDo;
        this.status = StatusRezerwacji.NOWA;
    }

    public Long getId() {
        return id;
    }

    public Klient getKlient() {
        return klient;
    }

    public Samochod getSamochod() {
        return samochod;
    }

    public LocalDate getDataOd() {
        return dataOd;
    }

    public LocalDate getDataDo() {
        return dataDo;
    }

    public StatusRezerwacji getStatus() {
        return status;
    }

    public void setStatus(StatusRezerwacji status) {
        this.status = status;
    }

    public BigDecimal getCenaCalkowita() {
        return cenaCalkowita;
    }

    public void setCenaCalkowita(BigDecimal cenaCalkowita) {
        this.cenaCalkowita = cenaCalkowita;
    }

    public List<Dodatek> getDodatki() {
        return dodatki;
    }

    public void setDataOd(LocalDate dataOd) {
        this.dataOd = dataOd;
    }

    public void setDataDo(LocalDate dataDo) {
        this.dataDo = dataDo;
    }

    @Override
    public String toString() {
        return "Rezerwacja #" + id + " - " +
                samochod.getMarka() + " " + samochod.getModel() +
                " (" + dataOd + " - " + dataDo + ") [" + status + "]";
    }
}
