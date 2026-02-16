package pl.pjatk.mas.model;

/**
 * Status rezerwacji samochodu.
 * Pozwala śledzić, na jakim etapie jest rezerwacja.
 */
public enum StatusRezerwacji {
    NOWA,       // rezerwacja właśnie utworzona
    W_TRAKCIE,  // rezerwacja realizowana (klient korzysta z auta)
    ZAKONCZONA, // rezerwacja zakończona
    ANULOWANA   // rezerwacja anulowana
}
