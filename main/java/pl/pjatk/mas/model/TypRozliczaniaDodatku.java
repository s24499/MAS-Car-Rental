package pl.pjatk.mas.model;

/**
 * Sposób rozliczania dodatku do rezerwacji.
 */
public enum TypRozliczaniaDodatku {
    ZA_DOBE,           // cena dodatku liczona za każdą dobę
    JEDNORAZOWY,       // jednorazowa opłata niezależna od liczby dni
    PROCENT_OD_STAWKI  // procent od ceny bazowej (np. 10% od stawki za auto)
}
