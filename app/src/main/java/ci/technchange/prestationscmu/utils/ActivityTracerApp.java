package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActivityTracerApp {
    // Nom du fichier de préférences
    private static final String PREFS_NAME_APP = "ActivityPrefsApp";

    // Clés pour stocker les différentes informations
    private static final String KEY_LAST_ACTIVITY_APP = "last_activity_app";
    private static final String KEY_DATE_DEBUT_APP = "date_debut_app";
    private static final String KEY_DATE_FIN_APP = "date_fin_app";
    private static final String KEY_APP_STARTED = "app_started"; // Nouvelle clé

    // SharedPreferences pour stocker les données
    private final SharedPreferences sharedPreferences;

    // Formateur de date
    private final SimpleDateFormat dateFormat;

    // Constructeur
    public ActivityTracerApp(Context context) {
        // Initialisation des SharedPreferences
        sharedPreferences = context.getSharedPreferences(PREFS_NAME_APP, Context.MODE_PRIVATE);

        // Création du formateur de date
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    // Méthode pour enregistrer l'activité
    public void trackActivityApp(String activity) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_ACTIVITY_APP, activity);
        editor.apply();
    }

    // Méthode pour récupérer la dernière activité
    public String getLastActivityApp() {
        return sharedPreferences.getString(KEY_LAST_ACTIVITY_APP, "");
    }

    // Méthode pour enregistrer la date de début
    public void enregistrerDateDebutApp() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String dateActuelle = dateFormat.format(new Date());
        editor.putString(KEY_DATE_DEBUT_APP, dateActuelle);
        editor.putBoolean(KEY_APP_STARTED, true); // Marquer qu'une app a été démarrée
        editor.apply();
        Log.d("ActivityTracerApp", "Date début externe app: " + dateActuelle);
    }

    // Méthode pour enregistrer la date de fin
    public String enregistrerDateFinApp() {
        String dateFin = dateFormat.format(new Date());
        sharedPreferences.edit()
                .putString(KEY_DATE_FIN_APP, dateFin)
                .putBoolean(KEY_APP_STARTED, false) // Réinitialiser le flag
                .apply();
        Log.d("ActivityTracerApp", "Date fin externe app: " + dateFin);
        return dateFin;
    }

    // Vérifier si une app externe a été démarrée
    public boolean isAppStarted() {
        return sharedPreferences.getBoolean(KEY_APP_STARTED, false);
    }

    // Méthode pour récupérer la date de début
    public String getDateDebutApp() {
        return sharedPreferences.getString(KEY_DATE_DEBUT_APP, "");
    }

    // Méthode pour récupérer la date de fin
    public String getDateFinApp() {
        return sharedPreferences.getString(KEY_DATE_FIN_APP, "");
    }

    // Méthode pour effacer toutes les données
    public void clearDataFseEdit() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}