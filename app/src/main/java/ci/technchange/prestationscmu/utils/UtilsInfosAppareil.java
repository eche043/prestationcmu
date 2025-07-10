package ci.technchange.prestationscmu.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.Map;

public class UtilsInfosAppareil {
    private static final String TAG = "UtilsInfosAppareil";
    private static final String PREFS_NAME = "AppareilPrefs";
    private static final String KEY_DEVICE_ID = "device_id";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;


    public UtilsInfosAppareil(Context context) {
        // Initialisation des SharedPreferences
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * Récupère les informations essentielles du système de l'appareil.
     *
     * @param context Contexte de l'application
     * @return Map contenant les détails système de l'appareil
     */
    @SuppressLint("HardwareIds")
    public Map<String, String> obtenirInformationsSysteme(Context context) {
        Log.d(TAG,"in obtenirInformationsSysteme");
        Map<String, String> infosAppareil = new HashMap<>();
        String deviceId = null;



        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telephonyManager = (TelephonyManager)
                        context.getSystemService(Context.TELEPHONY_SERVICE);

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        deviceId = telephonyManager.getImei();
                        infosAppareil.put("IMEI", deviceId);
                        sauvegarderIdAppareil(deviceId);
                    } else {
                        deviceId = telephonyManager.getDeviceId();
                        infosAppareil.put("IMEI", deviceId);
                        sauvegarderIdAppareil(deviceId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la récupération de l'IMEI: " + e.getMessage());
                }
            } else {
                Log.w(TAG, "Permission READ_PHONE_STATE non accordée - IMEI non disponible");
            }


            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String serialNumber = Build.getSerial();
                infosAppareil.put("ID FAMOCO par getSerial", serialNumber);
                //sauvegarderIdAppareil(serialNumber);
            }*/


        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des informations de l'appareil", e);
        }

        return infosAppareil;
    }

    /**
     * Sauvegarde l'ID de l'appareil dans les SharedPreferences
     *
     * @param deviceId Identifiant de l'appareil à sauvegarder
     */
    public void sauvegarderIdAppareil(String deviceId) {
        if (deviceId != null && !deviceId.isEmpty()) {
            editor.putString(KEY_DEVICE_ID, deviceId);
            editor.apply();
            Log.i(TAG, "ID de l'appareil sauvegardé avec succès"+deviceId);
        }
    }

    /**
     * Récupère l'ID de l'appareil sauvegardé
     *
     * @return ID de l'appareil ou chaîne vide si non trouvé
     */
    public String recupererIdAppareil() {
        return sharedPreferences.getString(KEY_DEVICE_ID, "");
    }

    /**
     * Imprime les informations de l'appareil dans le journal logcat.
     *
     * @param context Contexte de l'application
     */
    public void afficherInformationsAppareil(Context context) {
        Map<String, String> infosAppareil = obtenirInformationsSysteme(context);
        StringBuilder sb = new StringBuilder("Informations de l'appareil :\n");

        for (Map.Entry<String, String> entree : infosAppareil.entrySet()) {
            sb.append(entree.getKey()).append(" : ").append(entree.getValue()).append("\n");
        }

        // Ajouter l'ID sauvegardé
        sb.append("ID Appareil sauvegardé : ").append(recupererIdAppareil());

        Log.i(TAG, sb.toString());
    }
}