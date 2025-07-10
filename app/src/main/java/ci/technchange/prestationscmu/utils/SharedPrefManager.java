package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {
    private static final String PREF_NAME = "LocationPreferencesAndVersionBD";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_NOM_COMPLET = "";
    private static final String KEY_CODE_NOM_AGENT = "";

    private static final String KEY_CODE_AGENT = "";
    private static final String KEY_LONGITUDE = "longitude";
    private static  final String KEY_VERSIONBD ="version";
    private static final String KEY_CODE_ETS = "code_ets";
    private static final String ID_FAMOCO = "id famoco";

    private static final String KEY_REGION_NAME = "region_name";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public SharedPrefManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveLocation(double latitude, double longitude) {
        editor.putFloat(KEY_LATITUDE, (float) latitude);
        editor.putFloat(KEY_LONGITUDE, (float) longitude);
        editor.apply();
    }

    public void setagentName(String nom, String prenom) {
        editor.putString(KEY_NOM_COMPLET, (String) nom +" "+ prenom);
        editor.apply();
    }

    public void setagentCodeAndName(String code_nom) {
        editor.putString(KEY_CODE_NOM_AGENT, (String) code_nom);
        editor.apply();
    }

    public void setCodeAgent(String code) {
        editor.putString(KEY_CODE_AGENT, (String) code);
        editor.apply();
    }

    public void saveVesrion(int version_bd){
        editor.putInt(KEY_VERSIONBD, (int) version_bd);
        editor.apply();
    }

    public void saveIdFamoco(String idFamoco){
        editor.putString(ID_FAMOCO, (String) idFamoco);
        editor.apply();
    }

    public String getIdFamoco(){
        return  sharedPreferences.getString(ID_FAMOCO, "");
    }
    public String getNomAgent(){
        return  sharedPreferences.getString(KEY_NOM_COMPLET, "");
    }
    public String getCodeAndNomAgent(){
        return  sharedPreferences.getString(KEY_CODE_NOM_AGENT, "");
    }

    public String getCodeAgent(){
        return  sharedPreferences.getString(KEY_CODE_AGENT, "");
    }

    public float getLatitude() {
        return sharedPreferences.getFloat(KEY_LATITUDE, 0.0f);
    }


    public float getLongitude() {
        return sharedPreferences.getFloat(KEY_LONGITUDE, 0.0f);
    }

    public void clearLocation() {
        editor.remove(KEY_LATITUDE);
        editor.remove(KEY_LONGITUDE);
        editor.apply();
    }

    public void saveDownloadedFileName(String fileName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("downloaded_file_name", fileName);
        editor.apply();
    }

    public String getDownloadedFileName() {
        return sharedPreferences.getString("downloaded_file_name", "");
    }
    public void saveCodeEts(String codeEts) {
        editor.putString(KEY_CODE_ETS, codeEts);
        editor.apply();
    }

    public void saveRegionName(String regionName) {
        editor.putString(KEY_REGION_NAME, regionName);
        editor.apply();
    }

    public String getRegionName() {
        return sharedPreferences.getString(KEY_REGION_NAME, "");
    }

    public String getCodeEts() {
        return sharedPreferences.getString(KEY_CODE_ETS, "");
    }

    public void clearCodeEts() {
        editor.remove(KEY_CODE_ETS);
        editor.apply();
    }

    public boolean hasLocationData() {

        if (!sharedPreferences.contains(KEY_LATITUDE) || !sharedPreferences.contains(KEY_LONGITUDE)) {
            return false;
        }

        // Récupère les valeurs
        float latitude = sharedPreferences.getFloat(KEY_LATITUDE, 0.0f);
        float longitude = sharedPreferences.getFloat(KEY_LONGITUDE, 0.0f);

        // Vérifie si les valeurs sont différentes de la valeur par défaut (0,0)
        // Note: Si (0,0) est une position valide dans votre application, vous devrez utiliser
        // une autre approche pour vérifier la validité des coordonnées
        return (latitude != 0.0f || longitude!=0.0f);
    }
}
