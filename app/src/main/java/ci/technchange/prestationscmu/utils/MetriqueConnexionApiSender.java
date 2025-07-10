package ci.technchange.prestationscmu.utils;

import static androidx.camera.core.impl.utils.ContextUtil.getApplicationContext;

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
import java.util.concurrent.atomic.AtomicInteger;

import ci.technchange.prestationscmu.models.MetriqueConnexion;
import ci.technchange.prestationscmu.views.InscriptionStepperActivity;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MetriqueConnexionApiSender {
    private static final String TAG = "MetriqueConnexionApiSender";
    private Context context;
    private MetriqueConnexionServiceDb metriqueConnexionServiceDb;
    private final Object syncLock = new Object();
    private static final String API_URL = "http://57.128.30.4:8090/api/v1/saveAgentActivity";

    public MetriqueConnexionApiSender(Context context) {
        this.context = context.getApplicationContext();
        this.metriqueConnexionServiceDb = MetriqueConnexionServiceDb.getInstance(context);
        Log.d(TAG, "Initialisation de MetriqueConnexionApiSender");
    }

    public void sendUnsyncedConnexionMetrics(double lat, double lng,String idFamoco) {
        synchronized (syncLock) {
            Log.d(TAG, "Début de la synchronisation des métriques de connexion");

            List<MetriqueConnexion> unsyncedMetrics = metriqueConnexionServiceDb.getMetriquesConnexionNonSynchro();
            Log.d(TAG, "Nombre de métriques de connexion non synchronisées trouvées: " + unsyncedMetrics.size());





            // Log des métriques à synchroniser
            for (MetriqueConnexion m : unsyncedMetrics) {
                Log.d(TAG, "Métrique connexion à synchroniser - ID: " + m.getId()
                        + ", Code ETS: " + m.getCodeEts()
                        + ", Code AGAC: " + m.getCodeAgac()
                        + ", Date: " + m.getDateConnexion()
                        + ", Heure: " + m.getHeureConnexion());
            }

            if (!isNetworkAvailable()) {
                Log.w(TAG, "Pas de connexion Internet, annulation de la synchronisation");
                showToast("Pas de connexion Internet");
                DataSMSManager smsManager = new DataSMSManager();
                String latitude_value = String.valueOf(lat);
                String longitude_value = String.valueOf(lng);
                for (MetriqueConnexion metrique : unsyncedMetrics) {
                    smsManager.connexionViaSMS(context,metrique,"co",latitude_value,longitude_value, idFamoco,
                            new DataSMSManager.SMSSendCallback() {
                                @Override
                                public void onSMSSendSuccess() {
                                    Log.d("SMS_SYNC", "SMS envoyé avec succès pour l'agent: ");
                                    metriqueConnexionServiceDb.updateSyncStatus(metrique.getId(), 1);
                                    // Marquer l'agent comme "envoyé par SMS" (is_synchronized = 2)
                                    //markAgentAsDoNotSendBySMS(matricule);

                                    // Afficher une notification

                                }

                                @Override
                                public void onSMSSendFailure(String errorMessage) {
                                    Log.e("SMS_SYNC", "Échec d'envoi SMS: " + errorMessage);
                                    //Afficher une notification d'erreur

                                }
                            }
                    );
                }

                return;
            }

            if (unsyncedMetrics.isEmpty()) {
                Log.d(TAG, "Aucune métrique de connexion à synchroniser");
                return;
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                int successCount = 0;
                int failCount = 0;

                for (MetriqueConnexion metrique : unsyncedMetrics) {
                    try {
                        Log.d(TAG, "Traitement de la métrique de connexion ID: " + metrique.getId());

                        boolean sendSuccess = sendConnexionMetricToApi(metrique, lat,lng,idFamoco);

                        if (sendSuccess) {
                            Log.d(TAG, "Métrique connexion ID " + metrique.getId() + " envoyée avec succès");
                            boolean updateSuccess = metriqueConnexionServiceDb.updateSyncStatus(metrique.getId(), 1);

                            if (updateSuccess) {
                                Log.d(TAG, "Statut de synchronisation mis à jour pour ID: " + metrique.getId());
                                successCount++;
                            } else {
                                Log.e(TAG, "Échec de la mise à jour du statut pour ID: " + metrique.getId());
                                logConnexionMetricDetails(metrique);
                                failCount++;
                            }
                        } else {
                            Log.e(TAG, "Échec de l'envoi de la métrique de connexion ID: " + metrique.getId());
                            failCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur critique lors du traitement de la métrique ID: " + metrique.getId(), e);
                        failCount++;
                    }
                }

                final int finalSuccessCount = successCount;
                final int finalFailCount = failCount;
                final String finalResultMessage = "Sync connexion terminée: " + successCount + " réussites, " + failCount + " échecs";

                Log.d(TAG, finalResultMessage);

                handler.post(() -> {
                    if (finalFailCount == 0 && finalSuccessCount > 0) {
                        showToast("Synchronisation réussie");
                    } else if (finalFailCount > 0) {
                        showToast(finalResultMessage);
                    }
                    Log.d(TAG, "Toutes les métriques de connexion ont été traitées");
                });
            });

            executor.shutdown();
        }
    }

    // Nouvelle méthode pour envoyer les métriques par SMS



    private void logConnexionMetricDetails(MetriqueConnexion metrique) {
        Log.d(TAG, "Détails de la métrique de connexion problématique - " +
                "ID: " + metrique.getId() + ", " +
                "Code ETS: " + metrique.getCodeEts() + ", " +
                "Code AGAC: " + metrique.getCodeAgac() + ", " +
                "Nom: " + metrique.getNomComplet() + ", " +
                "Date: " + metrique.getDateConnexion() + ", " +
                "Heure: " + metrique.getHeureConnexion());
    }
    private boolean sendConnexionMetricToApi(MetriqueConnexion metrique, double lat, double lng,String idFamoco) {
        Log.d(TAG, "Préparation de l'envoi pour ID: " + metrique.getId());


        JSONObject json = new JSONObject();
        try {
            json.put("code_ets", metrique.getCodeEts());
            json.put("id_region", metrique.getIdRegion());
            json.put("nom_complet", metrique.getNomComplet());
            json.put("date_connexion", metrique.getDateConnexion());
            json.put("heure_connexion", metrique.getHeureConnexion());
            json.put("code_agac", metrique.getCodeAgac());
            json.put("latitude",String.valueOf(lat) );
            json.put("longitude", String.valueOf(lng));
            json.put("id_famoco", idFamoco);

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
                .url(API_URL)
                .post(body)
                .build();

        Log.d(TAG, "Envoi de la requête POST pour ID: " + metrique.getId());

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "null";
            Log.d(TAG, "Réponse du serveur pour ID " + metrique.getId() +
                    ": Code=" + response.code() +
                    ", Body=" + responseBody);

            if (response.isSuccessful() && responseBody != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    boolean find = jsonResponse.optBoolean("find", false);
                    String message = jsonResponse.optString("message", "");

                    Log.d(TAG, "Réponse API - find: " + find + ", message: " + message);


                    return find && message.equalsIgnoreCase("Enregistrement effectué avec succès");
                } catch (JSONException e) {
                    Log.e(TAG, "Erreur d'analyse JSON de la réponse", e);
                    return false;
                }
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Erreur réseau lors de l'envoi pour ID: " + metrique.getId(), e);
            return false;
        }
    }


    /*private boolean sendConnexionMetricToApi(MetriqueConnexion metrique) {
        Log.d(TAG, "Préparation de l'envoi pour ID: " + metrique.getId());


        String url = API_URL +
                "?code_ets=" + metrique.getCodeEts() +
                "&id_region=" + metrique.getIdRegion() +
                "&nom_complet=" + metrique.getNomComplet() +
                "&date_connexion=" + metrique.getDateConnexion() +
                "&id_region="+metrique.getIdRegion()+
                "&heure_connexion=" + metrique.getHeureConnexion() +
                "&code_agac=" + metrique.getCodeAgac();

        Log.d(TAG, "URL construite: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Log.d(TAG, "Envoi de la requête pour ID: " + metrique.getId());

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            boolean isSuccess = response.isSuccessful();
            String responseBody = response.body() != null ? response.body().string() : "null";

            Log.d(TAG, "Réponse du serveur pour ID " + metrique.getId() +
                    ": Code=" + response.code() +
                    ", find=" + isSuccess +
                    ", message=" + responseBody);

            return isSuccess;
        } catch (IOException e) {
            Log.e(TAG, "Erreur réseau lors de l'envoi pour ID: " + metrique.getId(), e);
            return false;
        }
    }*/

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