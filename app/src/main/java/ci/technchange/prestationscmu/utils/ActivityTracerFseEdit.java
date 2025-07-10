package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActivityTracerFseEdit {
    // Nom du fichier de préférences
    private static final String PREFS_NAME_FSE_EDIT = "ActivityPrefsFseEdit";

    // Clés pour stocker les différentes informations
    private static final String KEY_LAST_ACTIVITY_FSE_EDIT = "last_activity_fse_edit";
    private static final String KEY_DATE_DEBUT_FSE_EDIT = "date_debut_fse_edit";
    private static final String KEY_DATE_FIN_FSE_EDIT = "date_fin_fse_edit";

    // SharedPreferences pour stocker les données
    private final SharedPreferences sharedPreferences;

    // Formateur de date
    private final SimpleDateFormat dateFormat;

    // Constructeur
    public ActivityTracerFseEdit(Context context) {
        // Initialisation des SharedPreferences
        sharedPreferences = context.getSharedPreferences(PREFS_NAME_FSE_EDIT, Context.MODE_PRIVATE);

        // Création du formateur de date
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    // Méthode pour enregistrer l'activité
    public void trackActivityFseEdit(String activity) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_ACTIVITY_FSE_EDIT, activity);
        editor.apply();
    }

    // Méthode pour récupérer la dernière activité
    public String getLastActivityFseEdit() {
        return sharedPreferences.getString(KEY_LAST_ACTIVITY_FSE_EDIT, "");
    }

    // Méthode pour enregistrer la date de début
    public void enregistrerDateDebutFseEdit() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String dateActuelle = dateFormat.format(new Date());
        editor.putString(KEY_DATE_DEBUT_FSE_EDIT, dateActuelle);
        editor.apply();
    }

    // Méthode pour enregistrer la date de fin
    public String enregistrerDateFinFseEdit() {
        String dateFin = dateFormat.format(new Date());
        sharedPreferences.edit().putString(KEY_DATE_FIN_FSE_EDIT, dateFin).apply();
        return dateFin;
    }

    // Méthode pour récupérer la date de début
    public String getDateDebutFseEdit() {
        return sharedPreferences.getString(KEY_DATE_DEBUT_FSE_EDIT, "");
    }

    // Méthode pour récupérer la date de fin
    public String getDateFinFseEdit() {
        return sharedPreferences.getString(KEY_DATE_FIN_FSE_EDIT, "");
    }

    // Méthode pour effacer toutes les données
    public void clearDataFseEdit() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
