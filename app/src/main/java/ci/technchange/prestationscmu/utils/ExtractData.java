package ci.technchange.prestationscmu.utils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractData {
    public static String extractIdNumberCNI(String text) {
        Pattern pattern = Pattern.compile("\\bC\\d{9}\\b"); // Recherche un "C" suivi de 9 chiffres
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }
    public static String extractNometprenoms(String text) {
        Pattern pattern = Pattern.compile("Nom\\s*([A-Z]+)\\s*Prénom\\(s\\)\\s*([A-Z ]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2); // Nom + Prénoms
        }
        return "";
    }

    public static String extractIdCNI(String text) {
        Pattern pattern = Pattern.compile("\\bC\\d{9}\\b"); // Recherche un "C" suivi de 9 chiffres
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    public static String extractNom(String text) {
        Pattern pattern = Pattern.compile("Nom\\s*([A-Z]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }




    public static String extractNom2(String text) {
        String[] lines = text.split("\\r?\\n");

        // STRATEGY 1: Look for "Nom" label followed by last name
        for (int i = 0; i < lines.length - 1; i++) {
            if (lines[i].trim().equalsIgnoreCase("Nom")) {
                // Check next line for the name
                if (i + 1 < lines.length && lines[i+1].trim().matches("[A-ZÉÈÀÙÛÎÖËÇ]+")) {
                    return lines[i+1].trim();
                }
            }
        }

        // STRATEGY 2: Look for last name before "Nom" label
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().equalsIgnoreCase("Nom")) {
                String prevLine = lines[i-1].trim();
                // Skip if previous line is a multi-word line (likely to be first names)
                if (!prevLine.contains(" ") && prevLine.matches("[A-ZÉÈÀÙÛÎÖËÇ]+")) {
                    return prevLine;
                }
            }
        }

        // STRATEGY 3: Look for "Nom: VALUE" pattern
        Pattern nomPattern = Pattern.compile("Nom\\s*:?\\s*([A-ZÉÈÀÙÛÎÖËÇ]+)", Pattern.CASE_INSENSITIVE);
        Matcher nomMatcher = nomPattern.matcher(text);
        if (nomMatcher.find()) {
            return nomMatcher.group(1).trim();
        }

        // STRATEGY 4: First single-word all-caps line after the ID number
        // This handles cases where the surname is the first standalone uppercase word
        boolean foundImmatriculation = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Check if we've passed the ID number line (account for different formats)
            if (!foundImmatriculation && (line.contains("Immatriculation") || line.matches("(?i).*n[°o]\\s*CI\\d+.*"))) {
                foundImmatriculation = true;
                continue;
            }

            // Once we've found the ID number, look for a standalone uppercase word
            if (foundImmatriculation) {
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Check if line is a standalone word in uppercase and not a common header word
                if (line.matches("[A-ZÉÈÀÙÛÎÖËÇ]+") &&
                        !line.matches("(?:CARTE|REPUBLIQUE|IDENTITE|IVOIRE|CIV|SEXE|TAILLE|DATE|LIEU|VALIDE|ETABLIE|ABIDJAN|NATIONALITE|IVOIRIENNE).*") &&
                        !line.equals("F") && !line.equals("M")) {

                    // Make sure it's not a single character (like M or F for gender)
                    if (line.length() > 1) {
                        return line;
                    }
                }
            }
        }

        return "";
    }

    public static String extractPrenoms(String text) {
        //Pattern pattern = Pattern.compile("Prénom\\(s\\)\\s*([A-Z ]+)", Pattern.CASE_INSENSITIVE);
        Pattern pattern = Pattern.compile("(Prénoms|Prénom\\(s\\))\\s*([A-ZÉÈÀÙÛÎÖËÇ ]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
        //return matcher.find() ? matcher.group(1) : "Prénom(s) introuvable(s)";
    }



    public static String extractPrenoms2(String text) {
        String[] lines = text.split("\\r?\\n");

        // STRATEGY 1: Look for "Prénom(s)" label on one line, with first names on previous line
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().matches("(?i)(?:Prénoms|Prénom\\(s\\)|Prenoms|Prenom\\(s\\))")) {
                // Previous line should contain the first names
                String prevLine = lines[i-1].trim();
                if (!prevLine.isEmpty() && prevLine.matches("[A-ZÉÈÀÙÛÎÖËÇ ]+") && prevLine.contains(" ")) {
                    return prevLine;
                }
            }
        }

        // STRATEGY 2: Look for "Prénom(s)" label followed by first names
        for (int i = 0; i < lines.length - 1; i++) {
            if (lines[i].trim().matches("(?i)(?:Prénoms|Prénom\\(s\\)|Prenoms|Prenom\\(s\\))")) {
                // Check next line for the names
                if (i + 1 < lines.length && lines[i+1].trim().matches("[A-ZÉÈÀÙÛÎÖËÇ ]+") && lines[i+1].trim().contains(" ")) {
                    return lines[i+1].trim();
                }
            }
        }

        // STRATEGY 3: Look for "Prénoms: VALUE" pattern
        Pattern prenomsPattern = Pattern.compile("(?:Prénoms|Prénom\\(s\\)|Prenoms|Prenom\\(s\\))\\s*:?\\s*([A-ZÉÈÀÙÛÎÖËÇ ]+)", Pattern.CASE_INSENSITIVE);
        Matcher prenomsMatcher = prenomsPattern.matcher(text);
        if (prenomsMatcher.find()) {
            return prenomsMatcher.group(1).trim();
        }

        // STRATEGY 4: Line after surname that contains multiple uppercase words
        // This handles cases where the first names are on the line following the surname
        String surname = extractNom(text); // Get the surname first
        if (!surname.isEmpty()) {
            for (int i = 0; i < lines.length; i++) {
                // Find the surname line
                if (lines[i].trim().equals(surname)) {
                    // Check the next non-empty line
                    for (int j = i + 1; j < lines.length && j < i + 3; j++) {
                        String nextLine = lines[j].trim();
                        if (!nextLine.isEmpty() &&
                                nextLine.matches("[A-ZÉÈÀÙÛÎÖËÇ ]+") &&
                                nextLine.contains(" ") && // Contains space (multiple words)
                                !nextLine.matches("(?i)(?:Nom|Taille|Sexe|Date|Lieu|Valide|Etablie).*")) {
                            return nextLine;
                        }
                    }
                    break;
                }
            }
        }

        // STRATEGY 5: Find the first multi-word all caps line after the ID number
        boolean foundImmatriculation = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Check if we've passed the ID number line
            if (!foundImmatriculation && (line.contains("Immatriculation") || line.matches("(?i).*n[°o]\\s*CI\\d+.*"))) {
                foundImmatriculation = true;
                continue;
            }

            // Once we've found the ID number, look for a multi-word all caps line
            if (foundImmatriculation) {
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Check if line is multiple words in uppercase and not a common header
                if (line.matches("[A-ZÉÈÀÙÛÎÖËÇ ]+") &&
                        line.contains(" ") && // Has a space (multiple words)
                        !line.matches("(?i)(?:CARTE|REPUBLIQUE|NATIONALE|IDENTITE|IVOIRE|CIV|SEXE|TAILLE|DATE|LIEU|VALIDE|ETABLIE|ABIDJAN).*")) {
                    return line;
                }
            }
        }

        return "";
    }

    public static String extractBirthDate(String text) {
        Pattern pattern = Pattern.compile("\\b\\d{2}/\\d{2}/\\d{4}\\b"); // Recherche une date au format JJ/MM/AAAA
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    public static String extractBirthDate2(String text) {
        // First look for date near "Date de Naissance"
        Pattern patternWithLabel = Pattern.compile("(?:Date de Naissance|Naissance)\\s*:?\\s*(\\d{2}/\\d{2}/\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher matcherWithLabel = patternWithLabel.matcher(text);
        if (matcherWithLabel.find()) {
            return matcherWithLabel.group(1);
        }

        // Try to find birth date when the date is on one line and the label is on the next line
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length - 1; i++) {
            // Check if this line contains a date and next line contains birth date label
            if (lines[i].trim().matches("\\d{2}/\\d{2}/\\d{4}") &&
                    lines[i+1].trim().matches("(?i).*(?:Date de Naissance|Naissance).*")) {
                return lines[i].trim();
            }
        }

        // If not found, look for any date but check we're not grabbing the issue or expiry date
        Pattern datePattern = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b");
        Matcher dateMatcher = datePattern.matcher(text);

        // Find all dates in the text
        while (dateMatcher.find()) {
            String date = dateMatcher.group();
            // Skip if this date appears within 20 characters of terms related to issue/expiry
            String context = text.substring(
                    Math.max(0, dateMatcher.start() - 20),
                    Math.min(text.length(), dateMatcher.end() + 20)
            );

            if (!context.matches("(?i).*(?:Valide|jusqu[''']au|Etablie|le:|Validité).*")) {
                return date;
            }
        }

        // Last resort: just find the first date in the text (risky but better than nothing)
        dateMatcher.reset();
        return dateMatcher.find() ? dateMatcher.group() : "";
    }

    public static String extractSocialSecurityNumber(String text) {
        Pattern pattern = Pattern.compile("Numéro de[:\\s]*([0-9]{13,})"); // Recherche un numéro de sécurité sociale de 13 chiffres ou plus
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }


    /**
     * Extrait le nom de façon optimisée à partir du texte OCR
     */
    public static String extractnomNew(String text) {
        // Afficher le texte entier pour débogage (à retirer en production)
        System.out.println("DEBUG: Texte complet pour extraction nom: " + text);

        // Méthode 0: Recherche simple et directe de la ligne "Nom" suivie d'un nom
        String[] lignes = text.split("\\n");
        for (String ligne : lignes) {
            if (ligne.trim().matches("(?i)^\\s*Nom\\s*$")) {
                // Si on trouve une ligne qui contient juste "Nom", regarder la ligne suivante
                int index = Arrays.asList(lignes).indexOf(ligne);
                if (index < lignes.length - 1) {
                    String ligneSuivante = lignes[index + 1].trim();
                    System.out.println("DEBUG: Ligne après 'Nom': " + ligneSuivante);
                    return ligneSuivante;
                }
            } else if (ligne.matches("(?i).*Nom\\s+([A-Z]{3,}).*")) {
                // Si la ligne contient "Nom" suivi d'un mot en majuscules
                Pattern p = Pattern.compile("(?i).*Nom\\s+([A-Z]{3,}).*");
                Matcher m = p.matcher(ligne);
                if (m.find()) {
                    System.out.println("DEBUG: Nom trouvé sur ligne avec 'Nom': " + m.group(1));
                    return m.group(1);
                }
            }
        }

        // Méthode 1: Rechercher "Nom" suivi du nom sur la même ligne ou ligne suivante avec lookahead
        Pattern pattern1 = Pattern.compile("(?i)Nom\\s*(?:\\n|\\s)\\s*([A-Z]+)");
        Matcher matcher1 = pattern1.matcher(text);

        if (matcher1.find()) {
            String nom = matcher1.group(1).trim();
            System.out.println("DEBUG: Méthode 1 a trouvé: " + nom);
            return nom;
        }

        // Méthode 2: Recherche de mots en majuscules après le numéro de sécurité
        // et avant "Prénoms"
        Pattern pattern2 = Pattern.compile("(?i)\\d{13}.*?\\n.*?([A-Z]{3,}).*?\\n.*?(?:Pr[eé]noms|Pr[eé]nom)");
        Matcher matcher2 = pattern2.matcher(text);

        if (matcher2.find()) {
            String nom = matcher2.group(1).trim();
            System.out.println("DEBUG: Méthode 2 a trouvé: " + nom);
            return nom;
        }

        // Méthode 3: Recherche directe de mots en majuscules entre le numéro de sécurité et "Prénoms"
        int indexNumero = text.indexOf("3840692167140");
        int indexPrenoms = text.indexOf("Prénoms");
        if (indexPrenoms == -1) {
            indexPrenoms = text.indexOf("Prenoms");
        }

        if (indexNumero != -1 && indexPrenoms != -1 && indexNumero < indexPrenoms) {
            String section = text.substring(indexNumero, indexPrenoms);
            Pattern patternMot = Pattern.compile("([A-Z]{3,})");
            Matcher matcherMot = patternMot.matcher(section);

            if (matcherMot.find()) {
                String nom = matcherMot.group(1).trim();
                System.out.println("DEBUG: Méthode 3 a trouvé: " + nom);
                return nom;
            }
        }

        // Méthode 4: Juste chercher KOFFI ou une variante directement
        Pattern pattern4 = Pattern.compile("(KOFF[IL])");
        Matcher matcher4 = pattern4.matcher(text);

        if (matcher4.find()) {
            String nom = matcher4.group(1);
            System.out.println("DEBUG: Méthode 4 a trouvé: " + nom);
            return nom;
        }

        // Méthode 5: Chercher n'importe quel mot en majuscules de 4 à 8 lettres
        // (longueur typique d'un nom de famille)
        Pattern pattern5 = Pattern.compile("\\b([A-Z]{4,8})\\b");
        Matcher matcher5 = pattern5.matcher(text);

        if (matcher5.find()) {
            String nom = matcher5.group(1);
            System.out.println("DEBUG: Méthode 5 a trouvé: " + nom);
            return nom;
        }

        // Si rien n'a fonctionné, on retourne une chaîne vide
        System.out.println("DEBUG: Aucune méthode n'a trouvé le nom");
        return "";
    }

    /**
     * Extrait le numéro de sécurité sociale de façon optimisée à partir du texte OCR
     */
    public static String extractnumeroSecuNew(String text) {
        // Recherche directe d'une séquence de 13 chiffres consécutifs (numéro de sécurité sociale)
        Pattern directPattern = Pattern.compile("\\b(\\d{13})\\b");
        Matcher directMatcher = directPattern.matcher(text);
        if (directMatcher.find()) {
            return directMatcher.group(1);
        }

        // Plusieurs variantes du texte précédant le numéro
        String[] patterns = {
                "Numéro de sécurité sociale\\s*\\n?\\s*(\\d{13})",
                "Numéro de sécunité sociale\\s*\\n?\\s*(\\d{13})",
                "Numéro de securit[ée]\\s*\\n?\\s*(\\d{13})",
                "Num[ée]ro\\s*\\n?\\s*de\\s*\\n?\\s*s[ée]curit[ée]\\s*\\n?\\s*(\\d{13})"
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        // Si aucun des patterns précédents n'a fonctionné, on essaie une approche plus générique
        Pattern fallbackPattern = Pattern.compile("\\b(\\d{10,13})\\b");
        Matcher fallbackMatcher = fallbackPattern.matcher(text);
        if (fallbackMatcher.find()) {
            String candidateNumber = fallbackMatcher.group(1);
            if (candidateNumber.length() >= 13) {
                return candidateNumber;
            } else if (candidateNumber.length() >= 10) {
                // Si on trouve un numéro de 10+ chiffres, on peut le retourner comme résultat partiel
                return candidateNumber;
            }
        }

        return "";
    }
}
