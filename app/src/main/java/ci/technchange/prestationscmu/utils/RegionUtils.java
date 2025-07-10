package ci.technchange.prestationscmu.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegionUtils {

    private static final Map<Integer, String> regions = new HashMap<>();

    static {
        regions.put(33, "Agneby-Tiassa");
        regions.put(32, "Bafing");
        regions.put(31, "Bagoue");
        regions.put(30, "Belier");
        regions.put(29, "Bere");
        regions.put(28, "Bounkani");
        regions.put(27, "Cavally");
        regions.put(26, "Abidjan");
        regions.put(25, "Yamoussoukro");
        regions.put(24, "Folon");
        regions.put(23, "Gbeke");
        regions.put(22, "Gbokle");
        regions.put(21, "Goh");
        regions.put(20, "Gontougo");
        regions.put(19, "Grands Ponts");
        regions.put(18, "Guemon");
        regions.put(17, "Hambol");
        regions.put(16, "Haut-Sassandra");
        regions.put(15, "Iffou");
        regions.put(14, "Indenie-Djuablin");
        regions.put(13, "Kabadougou");
        regions.put(11, "Loh-Djiboua");
        regions.put(10, "Marahoue");
        regions.put(12, "Me");
        regions.put(9, "Moronou");
        regions.put(8, "N'Zi");
        regions.put(7, "Nawa");
        regions.put(6, "Poro");
        regions.put(5, "San Pedro");
        regions.put(4, "Sud-Comoe");
        regions.put(3, "Tchologo");
        regions.put(2, "Tonkpi");
        regions.put(1, "Worodougou");
    }

    /**
     * Extrait le numéro de région à partir du nom de fichier et renvoie le nom de région correspondant
     * @param fileName Le nom du fichier (ex: "enrole_region26.db")
     * @return Le nom de la région ou "Région inconnue" si non trouvée
     */
    public static String getRegionNameFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "Région inconnue";
        }

        // Extraire le numéro de région du nom de fichier
        Pattern pattern = Pattern.compile("enrole_region(\\d+)\\.db");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            try {
                int regionNumber = Integer.parseInt(matcher.group(1));
                return regions.getOrDefault(regionNumber, "Région inconnue");
            } catch (NumberFormatException e) {
                return "Région inconnue";
            }
        }

        // Essayer un autre format possible (newcnambd1.db, etc.)
        pattern = Pattern.compile("newcnambd(\\d+)\\.db");
        matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            try {
                int regionNumber = Integer.parseInt(matcher.group(1));
                return regions.getOrDefault(regionNumber, "Région inconnue");
            } catch (NumberFormatException e) {
                return "Région inconnue";
            }
        }

        return "Région inconnue";
    }


    public static int getRegionid(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return 0;
        }

        // Extraire le numéro de région du nom de fichier
        Pattern pattern = Pattern.compile("enrole_region(\\d+)\\.db");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            try {
                int regionNumber = Integer.parseInt(matcher.group(1));
                return regionNumber;
            } catch (NumberFormatException e) {
                return 0;
            }
        }


        return 0;
    }
}
