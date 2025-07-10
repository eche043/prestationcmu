package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ci.technchange.prestationscmu.models.Metrique;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MetriqueApiSender {
    private static final String TAG = "MetriqueApiSender";
    private Context context;
    private MetriqueServiceDb metriqueServiceDb;
    private SharedPrefManager sharedPrefManager;
    private final Object syncLock = new Object();

    public MetriqueApiSender(Context context) {
        this.context = context;
        this.metriqueServiceDb = MetriqueServiceDb.getInstance(context);
        sharedPrefManager = new SharedPrefManager(context);
        Log.d(TAG, "Initialisation de MetriqueApiSender");
    }

    public void sendUnsyncedMetrics(double lat, double lng) {
        synchronized (syncLock) {
            Log.d(TAG, "Début de la synchronisation des métriques");

            if (!isNetworkAvailable()) {
                Log.w(TAG, "Pas de connexion Internet, annulation de la synchronisation");
                showToast("Pas de connexion Internet");
                return;
            }

            List<Metrique> unsyncedMetrics = metriqueServiceDb.getMetriqueNonSynchro();
            Log.d(TAG, "Nombre de métriques non synchronisées trouvées: " + unsyncedMetrics.size());

            // Log des métriques à synchroniser
            for (Metrique m : unsyncedMetrics) {
                Log.d(TAG, "Métrique à synchroniser - ID: " + m.getId()
                        + ", Activité: " + m.getActivite()
                        + ", Début: " + m.getDateDebut()
                        + ", Fin: " + m.getDateFin());
            }

            if (unsyncedMetrics.isEmpty()) {
                Log.d(TAG, "Aucune métrique à synchroniser");
                //showToast("Aucune donnée à synchroniser");
                return;
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                int successCount = 0;
                int failCount = 0;

                for (Metrique metrique : unsyncedMetrics) {
                    try {
                        Log.d(TAG, "Traitement de la métrique ID: " + metrique.getId());

                        boolean sendSuccess = sendMetricToApi(metrique, lat, lng);

                        if (sendSuccess) {
                            Log.d(TAG, "Métrique ID " + metrique.getId() + " envoyée avec succès");
                            boolean updateSuccess = metriqueServiceDb.updateSyncStatus(metrique.getId(), 1);

                            if (updateSuccess) {
                                Log.d(TAG, "Statut de synchronisation mis à jour pour ID: " + metrique.getId());
                                successCount++;
                            } else {
                                Log.e(TAG, "Échec de la mise à jour du statut pour ID: " + metrique.getId());
                                // Log supplémentaire pour diagnostiquer le problème
                                logMetricDetails(metrique);
                                failCount++;
                            }
                        } else {
                            Log.e(TAG, "Échec de l'envoi de la métrique ID: " + metrique.getId());
                            failCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur critique lors du traitement de la métrique ID: " + metrique.getId(), e);
                        failCount++;
                    }
                }

                final String resultMessage = "Sync terminée: " + successCount + " réussites, " + failCount + " échecs";
                Log.d(TAG, resultMessage);

                handler.post(() -> {
                    showToast(resultMessage);
                    Log.d(TAG, "Toutes les métriques ont été traitées");
                });
            });

            executor.shutdown();
        }
    }

    private void logMetricDetails(Metrique metrique) {
        Log.d(TAG, "Détails de la métrique problématique - " +
                "ID: " + metrique.getId() + ", " +
                "Activité: " + metrique.getActivite() + ", " +
                "Début: " + metrique.getDateDebut() + ", " +
                "Fin: " + metrique.getDateFin() + ", " +
                "Région: " + metrique.getIdRegion() + ", " +
                "Famoco: " + metrique.getIdFamoco());
    }

    private boolean sendMetricToApi(Metrique metrique, double lat, double lng) {
        Log.d(TAG, "Préparation de l'envoi pour ID: " + metrique.getId());

        JSONObject json = new JSONObject();
        try {
            json.put("activite", metrique.getActivite());
            json.put("date_debut", metrique.getDateDebut());
            json.put("date_fin", metrique.getDateFin());
            json.put("code_ets", sharedPrefManager.getCodeEts());
            json.put("id_region", metrique.getIdRegion());
            json.put("id_famoco", metrique.getIdFamoco());
            json.put("latitude", lat);
            json.put("longitude", lng);
            json.put("code_agac", sharedPrefManager.getCodeAgent());

            Log.d(TAG, "Données JSON préparées: " + json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Erreur de création JSON pour ID: " + metrique.getId(), e);
            return false;
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("http://57.128.30.4:8090/api/v1/saveMetrics")
                .post(body)
                .build();

        Log.d(TAG, "Envoi de la requête pour ID: " + metrique.getId());

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            boolean isSuccess = response.isSuccessful();
            String responseBody = response.body() != null ? response.body().string() : "null";

            Log.d(TAG, "Réponse du serveur pour ID " + metrique.getId() +
                    ": Code=" + response.code() +
                    ", Success=" + isSuccess +
                    ", Body=" + responseBody);

            return isSuccess;
        } catch (IOException e) {
            Log.e(TAG, "Erreur réseau lors de l'envoi pour ID: " + metrique.getId(), e);
            return false;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "Vérification réseau: " + (isAvailable ? "Connecté" : "Déconnecté"));
        return isAvailable;
    }

    private void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        });
    }
}