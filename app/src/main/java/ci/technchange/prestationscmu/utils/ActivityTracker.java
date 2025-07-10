package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActivityTracker {
    // Nom du fichier de préférences
    private static final String PREFS_NAME = "ActivityPrefs";

    // Clés pour stocker les différentes informations
    private static final String KEY_LAST_ACTIVITY = "last_activity";
    private static final String KEY_DATE_DEBUT = "date_debut";
    private static final String KEY_DATE_FIN = "date_fin";

    // SharedPreferences pour stocker les données
    private final SharedPreferences sharedPreferences;

    // Formateur de date
    private final SimpleDateFormat dateFormat;

    // Constructeur
    public ActivityTracker(Context context) {
        // Initialisation des SharedPreferences
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Création du formateur de date
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    // Méthode pour enregistrer l'activité
    public void trackActivity(String activity) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_ACTIVITY, activity);
        editor.apply();
    }

    // Méthode pour récupérer la dernière activité
    public String getLastActivity() {
        return sharedPreferences.getString(KEY_LAST_ACTIVITY, "");
    }

    // Méthode pour enregistrer la date de début
    public void enregistrerDateDebut() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String dateActuelle = dateFormat.format(new Date());
        editor.putString(KEY_DATE_DEBUT, dateActuelle);
        editor.apply();
    }

    // Méthode pour enregistrer la date de fin
    public String enregistrerDateFin() {
        String dateFin = dateFormat.format(new Date());
        sharedPreferences.edit().putString(KEY_DATE_FIN, dateFin).apply();
        return dateFin;
    }

    // Méthode pour récupérer la date de début
    public String getDateDebut() {
        return sharedPreferences.getString(KEY_DATE_DEBUT, "");
    }

    // Méthode pour récupérer la date de fin
    public String getDateFin() {
        return sharedPreferences.getString(KEY_DATE_FIN, "");
    }

    // Méthode pour effacer toutes les données
    public void clearData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}