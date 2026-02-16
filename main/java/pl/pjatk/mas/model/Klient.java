package pl.pjatk.mas.model;

public class Klient extends Uzytkownik {

    protected String email;

    public Klient(int id, String login, String haslo, String imie, String nazwisko, String email) {
        super(id, login, haslo, imie, nazwisko);
        this.email = email;
    }

    /**
     * GETTERY I SETTERY
     **/

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return imie + ' ' + nazwisko;
    }
}
