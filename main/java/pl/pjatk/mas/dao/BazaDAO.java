package pl.pjatk.mas.dao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class BazaDAO {

    /** Buduje ścieżkę do pliku w katalogu resources/DB. */
    protected String sciezkaDoPliku(String nazwaPliku) {
        return "src/main/resources/DB/" + nazwaPliku;
    }

    /** Wczytuje wszystkie linie z pliku. Gdy plik nie istnieje, zwraca pustą listę. */
    protected List<String> wczytajLinie(String sciezka) {
        List<String> linie = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(sciezka), StandardCharsets.UTF_8))) {

            String linia;
            while ((linia = reader.readLine()) != null) {
                linie.add(linia);
            }
        } catch (FileNotFoundException e) {
            return linie;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return linie;
    }

    /** Zapisuje linie do pliku (nadpisuje). */
    protected void zapiszLinie(String sciezka, List<String> linie) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(sciezka), StandardCharsets.UTF_8))) {

            for (String linia : linie) {
                writer.write(linia);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}