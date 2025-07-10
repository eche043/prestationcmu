package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ci.technchange.prestationscmu.core.dbHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {

    private static final String TAG = "ApiService";
    private static final String BASE_URL = "http://57.128.30.4:8090/api/v1/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Context context;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    public ApiService(Context context) {
        this.context = context;

        // Configurer OkHttpClient avec timeout
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Envoie les données d'inscription de l'agent au serveur
     */
    public void registerAgent(String nom, String prenoms, String contact, String codeEts,
                              String codeAgac, String photoPath,String idFamoco, ApiCallback callback) {
        try {
            // Obtenir la date et l'heure actuelles
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date now = new Date();
            String dateEnregistrement = dateFormat.format(now);
            String heureEnregistrement = timeFormat.format(now);

            // Récupérer les empreintes de l'agent
            List<byte[]> empreintes = getEmpreintesForAgent(codeAgac);

            // Créer un multipart body pour envoyer à la fois le JSON et la photo
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            // Ajouter les champs texte
            multipartBuilder.addFormDataPart("nom", nom);
            multipartBuilder.addFormDataPart("prenoms", prenoms);
            multipartBuilder.addFormDataPart("contact", contact);
            multipartBuilder.addFormDataPart("code_ets", codeEts);
            multipartBuilder.addFormDataPart("code_agac", codeAgac);
            multipartBuilder.addFormDataPart("date_enregistrement", dateEnregistrement);
            multipartBuilder.addFormDataPart("heure_enregistrement", heureEnregistrement);
            multipartBuilder.addFormDataPart("id_famoco", idFamoco);

            // Ajouter les templates d'empreintes
            for (int i = 0; i < empreintes.size(); i++) {
                String encodedTemplate = android.util.Base64.encodeToString(empreintes.get(i), android.util.Base64.DEFAULT);
                multipartBuilder.addFormDataPart("empreinte[" + i + "]", encodedTemplate);
            }

            // Ajouter la photo si le chemin est valide
            if (photoPath != null && !photoPath.isEmpty()) {
                File photoFile = new File(photoPath);
                if (photoFile.exists()) {
                    MediaType mediaType = MediaType.parse("image/jpeg");
                    multipartBuilder.addFormDataPart("photo", photoFile.getName(),
                            RequestBody.create(photoFile, mediaType));
                    Log.d(TAG, "Photo ajoutée Selfie: " + photoFile.getAbsolutePath() + " (" + photoFile.length() + " bytes)");
                } else {
                    Log.e(TAG, "Fichier photo introuvable: " + photoPath);
                }
            }

            // Créer la requête
            RequestBody requestBody = multipartBuilder.build();
            Request request = new Request.Builder()
                    .url(BASE_URL + "registerAgent")
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Envoi de la requête avec photo");

            // Exécuter la requête de manière asynchrone
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur lors de l'envoi des données", e);
                    callback.onError("Erreur réseau: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseString = response.body().string();
                    Log.d(TAG, "Réponse du serveur: " + responseString);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseString);
                            // Vérifier si le message existe, ce qui indique généralement un succès
                            if (jsonResponse.has("message")) {
                                String message = jsonResponse.getString("message");

                                // Si le message contient "succès" ou si un token est présent, considérer comme réussi
                                if (message.contains("succès") || jsonResponse.has("token")) {
                                    callback.onSuccess(jsonResponse);
                                } else {
                                    callback.onError("Réponse non attendue: " + message);
                                }
                            } else {
                                callback.onSuccess(jsonResponse); // Considérer comme succès même sans message
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse JSON", e);
                            callback.onError("Erreur lors de l'analyse de la réponse");
                        }
                    } else {
                        callback.onError("Erreur serveur: " + response.code() + " - " + responseString);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            callback.onError("Erreur lors de la préparation des données: " + e.getMessage());
        }
    }

    public void updateAgent(String nom, String prenoms, String contact,
                            String codeAgac, String codeAgacInitiale, ApiCallback callback) {

        try {
            // Obtenir la date et l'heure actuelles
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date now = new Date();
            String dateEnregistrement = dateFormat.format(now);
            String heureEnregistrement = timeFormat.format(now);

            // Créer le JSON body directement
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("nom", nom);
            jsonBody.put("prenoms", prenoms);
            jsonBody.put("code_agac", codeAgac);
            jsonBody.put("contact", contact);
            // Note: "contact" n'est pas dans votre exemple JSON, mais je l'ajoute au cas où
            // jsonBody.put("contact", contact);

            // Créer le RequestBody avec le JSON
            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            // Créer la requête
            Request request = new Request.Builder()
                    .url(BASE_URL + "updateAgent/" + codeAgacInitiale)
                    .put(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "Envoi de la requête JSON: " + jsonBody.toString());

            // Exécuter la requête de manière asynchrone
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur lors de l'envoi des données", e);
                    callback.onError("Erreur réseau: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseString = response.body().string();
                    Log.d(TAG, "Réponse du serveur: " + responseString);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseString);
                            // Vérifier si le message existe, ce qui indique généralement un succès
                            if (jsonResponse.has("message")) {
                                String message = jsonResponse.getString("message");

                                // Si le message contient "succès" ou si un token est présent, considérer comme réussi
                                if (message.contains("success")) {
                                    callback.onSuccess(jsonResponse);
                                } else {
                                    callback.onError("Réponse non attendue: " + message);
                                }
                            } else {
                                callback.onSuccess(jsonResponse); // Considérer comme succès même sans message
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse JSON", e);
                            callback.onError("Erreur lors de l'analyse de la réponse");
                        }
                    } else {
                        callback.onError("Erreur serveur: " + response.code() + " - " + responseString);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            callback.onError("Erreur lors de la préparation des données: " + e.getMessage());
        }
    }

    public void storeBdDownload(String idRegion, String idFamoco, String codeEts,
                                String codeAgac, String dateRemontee, String heureDebut,
                                String heureFin, ApiCallback callback) {
        try {
            // Créer un multipart body pour envoyer les données
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);
            Log.d(TAG,"id Famaco"+idFamoco);
            // Ajouter les champs de formulaire
            multipartBuilder.addFormDataPart("id_region", idRegion);
            multipartBuilder.addFormDataPart("id_famoco", idFamoco);
            multipartBuilder.addFormDataPart("code_ets", codeEts);
            multipartBuilder.addFormDataPart("code_agac", codeAgac);
            multipartBuilder.addFormDataPart("date_remontee", dateRemontee);
            multipartBuilder.addFormDataPart("heure_debut", heureDebut);
            multipartBuilder.addFormDataPart("heure_fin", heureFin);

            // Créer la requête
            RequestBody requestBody = multipartBuilder.build();
            Request request = new Request.Builder()
                    .url(BASE_URL + "storeBdDownload")
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Envoi de la requête storeBdDownload");

            // Exécuter la requête de manière asynchrone
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur lors de l'envoi des données", e);
                    callback.onError("Erreur réseau: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseString = response.body().string();
                    Log.d(TAG, "Réponse du serveur: " + responseString);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseString);
                            // Vérifier si le message existe
                            if (jsonResponse.has("message")) {
                                String message = jsonResponse.getString("message");

                                // Si le message contient "succès", considérer comme réussi
                                if (message.contains("succès")) {
                                    callback.onSuccess(jsonResponse);
                                } else {
                                    callback.onError("Réponse non attendue: " + message);
                                }
                            } else {
                                callback.onSuccess(jsonResponse); // Considérer comme succès même sans message
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse JSON", e);
                            callback.onError("Erreur lors de l'analyse de la réponse");
                        }
                    } else {
                        callback.onError("Erreur serveur: " + response.code() + " - " + responseString);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            callback.onError("Erreur lors de la préparation des données: " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les empreintes d'un agent depuis la base de données locale
     */
    private List<byte[]> getEmpreintesForAgent(String matricule) {
        List<byte[]> empreintes = new ArrayList<>();
        dbHelper helper = new dbHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        try {
            // Requête pour récupérer les templates d'empreintes
            Cursor cursor = db.query(
                    "empreintes",
                    new String[]{"template"},
                    "matricule = ?",
                    new String[]{matricule},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int templateIndex = cursor.getColumnIndex("template");

                if (templateIndex >= 0) {
                    do {
                        byte[] template = cursor.getBlob(templateIndex);
                        if (template != null && template.length > 0) {
                            empreintes.add(template);
                            Log.d(TAG, "Empreinte récupérée pour " + matricule + ", taille: " + template.length);
                        }
                    } while (cursor.moveToNext());
                }

                cursor.close();
            }

            Log.d(TAG, "Total des empreintes récupérées pour " + matricule + ": " + empreintes.size());
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des empreintes", e);
        } finally {
            db.close();
        }

        return empreintes;
    }



    /**
     * Supprime un agent du serveur et de la base de données locale
     */
    public void deleteAgent(String id, String matricule, String nom, String prenoms, ApiCallback callback) {
        try {
            // Créer le JSON body
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("code_agac", matricule);
            jsonBody.put("nom", nom);
            jsonBody.put("prenoms", prenoms);

            // Créer le RequestBody avec le JSON
            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            // Créer la requête
            Request request = new Request.Builder()
                    .url(BASE_URL + "deleteAgent")
                    .delete(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "Envoi de la requête de suppression: " + jsonBody.toString());

            // Exécuter la requête de manière asynchrone
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur lors de la suppression de l'agent", e);
                    // Exécuter le callback sur le thread principal
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError("Erreur réseau: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseString = response.body().string();
                    Log.d(TAG, "Réponse du serveur pour suppression: " + responseString);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseString);

                            // Vérifier si la réponse indique un succès
                            if (jsonResponse.has("message")) {
                                String message = jsonResponse.getString("message");

                                // Si le message contient "succès" ou "success", procéder à la suppression locale
                                if (message.toLowerCase().contains("succès") ||
                                        message.toLowerCase().contains("success")) {

                                    // Supprimer de la base de données locale
                                    if (deleteAgentFromLocalDatabase(id, matricule)) {
                                        // Exécuter le callback sur le thread principal
                                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                        mainHandler.post(() -> callback.onSuccess(jsonResponse));
                                    } else {
                                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                        mainHandler.post(() -> callback.onError("Agent supprimé du serveur mais erreur lors de la suppression locale"));
                                    }
                                } else {
                                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                    mainHandler.post(() -> callback.onError("Réponse du serveur: " + message));
                                }
                            } else {
                                // Si pas de message mais réponse successful, procéder à la suppression locale
                                if (deleteAgentFromLocalDatabase(id, matricule)) {
                                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                    mainHandler.post(() -> callback.onSuccess(jsonResponse));
                                } else {
                                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                    mainHandler.post(() -> callback.onError("Agent supprimé du serveur mais erreur lors de la suppression locale"));
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse JSON", e);
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(() -> callback.onError("Erreur lors de l'analyse de la réponse"));
                        }
                    } else {
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onError("Erreur serveur: " + response.code() + " - " + responseString));
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête de suppression", e);
            callback.onError("Erreur lors de la préparation des données: " + e.getMessage());
        }
    }

    /**
     * Supprime un agent de la base de données locale
     */
    private boolean deleteAgentFromLocalDatabase(String id, String matricule) {
        try {
            dbHelper helper = new dbHelper(context);
            SQLiteDatabase db = helper.getWritableDatabase();

            // Supprimer l'agent par ID (plus sûr)
            int deletedRows = db.delete("agents_inscription", "id = ?", new String[]{id});

            if (deletedRows > 0) {
                Log.d(TAG, "Agent supprimé de la base locale - ID: " + id + ", Matricule: " + matricule);

                // Optionnel: Supprimer aussi les empreintes associées
                //int deletedFingerprints = db.delete("empreintes", "matricule = ?", new String[]{matricule});
                //Log.d(TAG, "Empreintes supprimées: " + deletedFingerprints);

                db.close();
                return true;
            } else {
                Log.e(TAG, "Aucun agent trouvé avec l'ID: " + id);
                db.close();
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la suppression locale de l'agent", e);
            return false;
        }
    }
}
