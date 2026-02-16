package pl.pjatk.mas.dao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstrakcyjna klasa bazowa dla DAO.
 * Zapewnia metody do wczytywania i zapisywania plików tekstowych.
 */
public abstract class BazaDAO {

    // Buduje ścieżkę do pliku w katalogu resources/DB
    protected String sciezkaDoPliku(String nazwaPliku) {
        return "src/main/resources/DB/" + nazwaPliku;
    }

    // Wczytuje wszystkie linie z pliku o podanej ścieżce
    protected List<String> wczytajLinie(String sciezka) {
        List<String> linie = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(sciezka), StandardCharsets.UTF_8))) {

            String linia;
            while ((linia = reader.readLine()) != null) {
                linie.add(linia);
            }
        } catch (FileNotFoundException e) {
            // Jeśli plik nie istnieje, zwracamy pustą listę
            return linie;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return linie;
    }

    // Zapisuje listę linii do podanego pliku
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
