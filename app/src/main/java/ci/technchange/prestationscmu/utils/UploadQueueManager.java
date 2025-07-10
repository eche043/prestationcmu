package ci.technchange.prestationscmu.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.core.dbHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadQueueManager {
    public static final String ACTION_SYNC_STARTED = "ci.technchange.prestationscmu.SYNC_STARTED";
    public static final String ACTION_SYNC_COMPLETED = "ci.technchange.prestationscmu.SYNC_COMPLETED";
    private static final String TAG = "UploadQueueManager";
    private static final String PREF_NAME = "upload_queue";
    private static final String QUEUE_KEY = "pending_uploads";
    private static final int RETRY_INTERVAL_MS = 60000; // 1 minute

    // Dans UploadQueueManager, ajoutez:


    private final Context context;
    private final ExecutorService executorService;
    private final Handler handler;
    private boolean isRetryScanActive = false;

    private static UploadQueueManager instance;
    private ReferentielService referentielService;



    // Constructeur privé pour Singleton
    private UploadQueueManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
    }

    // Obtenir l'instance
    public static synchronized UploadQueueManager getInstance(Context context) {
        if (instance == null) {
            instance = new UploadQueueManager(context);
        }
        return instance;
    }

    /**
     * Vérifie l'état de la connexion internet
     * @return true si la connexion est bonne, false sinon
     */
    public boolean isNetworkGood() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (capabilities == null) return false;

        // Vérifier si nous sommes connectés et sur quel type de réseau
        boolean isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        // Optionnel: vérifier la qualité de la connexion
        boolean hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

        // Vous pouvez aussi vérifier la bande passante si nécessaire
        // int downSpeed = capabilities.getLinkDownstreamBandwidthKbps();

        return isConnected;
    }

    /**
     * Ajoute une image à la file d'attente pour envoi ultérieur
     */
    /*public void addToQueue(File imageFile) {
        try {
            // Créer un dossier de stockage permanent si nécessaire
            File storageDir = new File(context.getFilesDir(), "pending_uploads");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // Copier l'image dans le stockage permanent
            String timestamp = String.valueOf(System.currentTimeMillis());
            File destFile = new File(storageDir, "pending_" + timestamp + ".png");

            // Copier le fichier
            FileUtils.copyFile(imageFile, destFile);

            // Ajouter à la liste persistante
            JSONObject uploadItem = new JSONObject();
            uploadItem.put("path", destFile.getAbsolutePath());
            uploadItem.put("timestamp", timestamp);
            uploadItem.put("attempts", 0);

            JSONArray queueArray = getQueueArray();
            queueArray.put(uploadItem);
            saveQueueArray(queueArray);

            Log.d(TAG, "Image ajoutée à la file d'attente: " + destFile.getAbsolutePath());

            // Démarrer le processus de vérification périodique
            startRetryScan();

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ajout à la file d'attente", e);
        }
    }*/
    /**
     * Ajoute une image à la file d'attente pour envoi ultérieur
     * @return true si l'ajout a réussi, false sinon
     */
    public boolean addToQueue(File imageFile, String numTrans) {
        try {
            // Créer un dossier de stockage permanent si nécessaire
            File storageDir = new File(context.getFilesDir(), "pending_uploads");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // Copier l'image dans le stockage permanent
            String timestamp = String.valueOf(System.currentTimeMillis());
            File destFile = new File(storageDir, "pending_" + timestamp + ".png");

            // Copier le fichier
            FileUtils.copyFile(imageFile, destFile);

            // Ajouter à la liste persistante
            JSONObject uploadItem = new JSONObject();
            uploadItem.put("path", destFile.getAbsolutePath());
            uploadItem.put("timestamp", timestamp);
            uploadItem.put("attempts", 0);
            uploadItem.put("status", "non_traite");
            uploadItem.put("numTrans", numTrans);

            JSONArray queueArray = getQueueArray();
            queueArray.put(uploadItem);
            saveQueueArray(queueArray);

            Log.d(TAG, "Image ajoutée à la file d'attente: " + destFile.getAbsolutePath());

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ajout à la file d'attente", e);
            return false;
        }
    }

    /**
     * Démarre la vérification périodique et les tentatives d'envoi
     */

    ///////////////
    public void startRetryScan() {
        if (isRetryScanActive) {
            Log.d(TAG, "La synchronisation est déjà active, ignoré");
            return;
        }

        Log.d(TAG, "Démarrage de la synchronisation");
        isRetryScanActive = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Vérification du réseau pour sync");
                if (isNetworkGood()) {
                    Log.d(TAG, "Réseau disponible, traitement de la file");
                    processQueue();


                } else {
                    Log.d(TAG, "Réseau indisponible, report de la synchronisation");
                }

                // Reprogrammer la vérification si la file n'est pas vide
                if (!isQueueEmpty()) {
                    Log.d(TAG, "File non vide, reprogrammation de la vérification");
                    isRetryScanActive = true;
                    handler.postDelayed(this, RETRY_INTERVAL_MS);
                } else {
                    Log.d(TAG, "File vide, arrêt de la vérification périodique");
                    isRetryScanActive = false;
                }
            }
        });
    }


    public void sendAgentByApi(String idFamoco) {


        Log.d(TAG, "Démarrage de la synchronisation");
        isRetryScanActive = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Vérification du réseau pour sync");
                //if (isNetworkGood()) {
                    Log.d(TAG, "Réseau disponible, traitement de la file");
                    //processQueue();
                    synchronizeAgents(idFamoco);


                //} else {
                    //Log.d(TAG, "Réseau indisponible, report de la synchronisation");
                //}
            }
        });
    }


    /**
     * Synchronise les agents non synchronisés avec le serveur
     */
    private void synchronizeAgents(String idFamoco) {
        DataSMSManager smsManager = new DataSMSManager();
        executorService.execute(() -> {
            dbHelper helper = new dbHelper(context);
            SQLiteDatabase db = null;
            Cursor cursor = null;

            //SQLiteDatabase db = helper.getReadableDatabase();

            try {
                db = helper.getReadableDatabase();
                // Vérifier si la colonne is_synchronized existe
                Cursor columnCursor = db.rawQuery("PRAGMA table_info(agents_inscription)", null);
                boolean hasSyncColumn = false;

                if (columnCursor != null) {
                    int nameIndex = columnCursor.getColumnIndex("name");
                    while (columnCursor.moveToNext()) {
                        if (nameIndex != -1 && "is_synchronized".equals(columnCursor.getString(nameIndex))) {
                            hasSyncColumn = true;
                            break;
                        }
                    }
                    columnCursor.close();
                }

                // Ajouter la colonne si elle n'existe pas
                if (!hasSyncColumn) {
                    SQLiteDatabase writeDb = helper.getWritableDatabase();
                    writeDb.execSQL("ALTER TABLE agents_inscription ADD COLUMN is_synchronized INTEGER DEFAULT 0");
                    writeDb.close();
                }

                // Récupérer tous les agents non synchronisés
                String query = "SELECT * FROM agents_inscription WHERE is_synchronized IS NULL OR is_synchronized = 0 OR is_synchronized = 2";
                cursor = db.rawQuery(query, null);

                // Créer une liste de tous les agents à traiter
                List<Map<String, Object>> agentsToProcess = new ArrayList<>();

                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        Map<String, Object> agent = new HashMap<>();

                        // Extraire les informations de l'agent
                        @SuppressLint("Range") String nom = cursor.getString(cursor.getColumnIndex("nom"));
                        @SuppressLint("Range") String prenom = cursor.getString(cursor.getColumnIndex("prenom"));
                        @SuppressLint("Range") String telephone = cursor.getString(cursor.getColumnIndex("telephone"));
                        @SuppressLint("Range") String matricule = cursor.getString(cursor.getColumnIndex("matricule"));
                        @SuppressLint("Range") String centre_sante = cursor.getString(cursor.getColumnIndex("centre_sante"));
                        @SuppressLint("Range") String photo_path = cursor.getString(cursor.getColumnIndex("photo_path"));

                        // Récupérer le statut de synchronisation
                        int syncStatus = 0;
                        try {
                            @SuppressLint("Range") int is_synchronized = cursor.getInt(cursor.getColumnIndex("is_synchronized"));
                            syncStatus = is_synchronized;
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de la récupération du statut de synchronisation: " + e.getMessage());
                        }

                        // Récupérer les coordonnées GPS si elles existent
                        double latitudeFacade = 0, longitudeFacade = 0, latitudeInterieur = 0, longitudeInterieur = 0;

                        try {
                            @SuppressLint("Range") double lat_facade = cursor.getDouble(cursor.getColumnIndex("latitude_facade"));
                            @SuppressLint("Range") double lng_facade = cursor.getDouble(cursor.getColumnIndex("longitude_facade"));
                            @SuppressLint("Range") double lat_int = cursor.getDouble(cursor.getColumnIndex("latitude_interieur"));
                            @SuppressLint("Range") double lng_int = cursor.getDouble(cursor.getColumnIndex("longitude_interieur"));

                            latitudeFacade = lat_facade;
                            longitudeFacade = lng_facade;
                            latitudeInterieur = lat_int;
                            longitudeInterieur = lng_int;
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de la récupération des coordonnées: " + e.getMessage());
                        }

                        agent.put("nom", nom);
                        agent.put("prenom", prenom);
                        agent.put("telephone", telephone);
                        agent.put("matricule", matricule);
                        agent.put("centre_sante", centre_sante);
                        agent.put("photo_path", photo_path);
                        agent.put("latitude_facade", latitudeFacade);
                        agent.put("longitude_facade", longitudeFacade);
                        agent.put("latitude_interieur", latitudeInterieur);
                        agent.put("longitude_interieur", longitudeInterieur);
                        agent.put("sync_status", syncStatus);

                        agentsToProcess.add(agent);
                    }
                }

                // Fermer la base de données et le curseur maintenant que nous avons collecté toutes les données
                if (cursor != null) {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }

                // Maintenant traiter chaque agent de la liste
                for (Map<String, Object> agent : agentsToProcess) {
                    String nom = (String) agent.get("nom");
                    String prenom = (String) agent.get("prenom");
                    String telephone = (String) agent.get("telephone");
                    String matricule = (String) agent.get("matricule");
                    String photo_path = (String) agent.get("photo_path");
                    double latitudeFacade = (double) agent.get("latitude_facade");
                    double longitudeFacade = (double) agent.get("longitude_facade");
                    double latitudeInterieur = (double) agent.get("latitude_interieur");
                    double longitudeInterieur = (double) agent.get("longitude_interieur");
                    int syncStatus = (int) agent.get("sync_status");

                    // Obtenir le code ETS
                    SharedPrefManager sharedPrefManager = new SharedPrefManager(context);
                    String codeEts = sharedPrefManager.getCodeEts();
                    if(isNetworkGood()){
                        // Appeler l'API pour envoyer les données
                        ApiService apiService = new ApiService(context);
                        final String finalMatricule = matricule; // Pour utiliser dans le callback

                        apiService.registerAgent(
                                nom,
                                prenom,
                                telephone,
                                codeEts,
                                matricule,
                                photo_path,
                                idFamoco,
                                new ApiService.ApiCallback() {
                                    @Override
                                    public void onSuccess(JSONObject response) {
                                        // Vérifier si un message existe
                                        String message = response.optString("message", "Données synchronisées avec succès");

                                        // Vérifier s'il y a un token ou un utilisateur
                                        boolean hasToken = response.has("token");
                                        boolean hasUser = response.has("user");
                                        Log.d(TAG, "response: " + response);
                                        Log.d(TAG, "Agent synchronisé avec succès: " + finalMatricule);

                                        // Marquer l'agent comme synchronisé dans la base de données locale
                                        markAgentAsSynchronized(finalMatricule);

                                        // Récupérer les données du centre et les envoyer si disponibles
                                        sendCenterDataIfAvailable(finalMatricule);

                                    }

                                    @Override
                                    public void onError(String message) {
                                        Log.e(TAG, "Erreur lors de la synchronisation de l'agent: " + message);
                                    }
                                }
                        );
                    }else {


                        //Appeler le sms
                        if (syncStatus != 2) {
                            Log.d(TAG, "Connexion non disponible, envoi par SMS pour l'agent: " + matricule);
                            handleSmsSending(smsManager, nom, prenom, matricule, telephone,String.valueOf(latitudeFacade),
                                    String.valueOf(longitudeFacade),
                                    String.valueOf(latitudeInterieur),
                                    String.valueOf(longitudeInterieur));
                        }

                    }


                    // Pause entre chaque envoi pour éviter de surcharger
                    Thread.sleep(2000);
                }

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la synchronisation des agents", e);
            } finally {
                db.close();
            }
        });
    }



    private void handleSmsSending(DataSMSManager smsManager, String nom, String prenoms,
                                  String matricule, String contact, String lat,
                                  String lng, String latInt, String lngInt) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                // Variable pour la date
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());

                // Variable pour l'heure
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH'h'mm'm'ss's'", Locale.getDefault());
                String currentTime = timeFormat.format(new Date());

                SharedPrefManager sharedPrefManager = new SharedPrefManager(context);
                String code_ets = sharedPrefManager.getCodeEts();
                String downloadedFileName = sharedPrefManager.getDownloadedFileName();

                // Initialize ReferentielService if not already
                if (referentielService == null) {
                    referentielService = new ReferentielService(context);
                }

                // Fetch establishment name and region
                String nomEtablissement = fetchEtablissementName(code_ets);
                String regionEts = referentielService.getRegionomByEtablissement(nomEtablissement);

                // Get device ID
                UtilsInfosAppareil utilsInfos = new UtilsInfosAppareil(context);
                String idFamoco = utilsInfos.recupererIdAppareil();
                Log.d("SMS_SYNC", "ID famoco est : " + idFamoco);

                // Get region ID
                RegionCoordUtils regionCoordUtils = new RegionCoordUtils(context);
                int id = regionCoordUtils.getIdForRegion(regionEts);
                String idString = Integer.toString(id);

                // 'i' is the letter key for agent registration
                String lettreCle = "i";

                // Send SMS
                smsManager.sendAgentAndCentreViaSMS(
                        context, lettreCle, nom, prenoms, matricule, code_ets,
                        idFamoco, currentDate, currentTime, contact, idString,
                        lat, lng, latInt, lngInt,
                        new DataSMSManager.SMSSendCallback() {
                            @Override
                            public void onSMSSendSuccess() {
                                Log.d("SMS_SYNC", "SMS envoyé avec succès pour l'agent: " + matricule);

                                // Marquer l'agent comme "envoyé par SMS" (is_synchronized = 2)
                                markAgentAsDoNotSendBySMS(matricule);

                                // Afficher une notification
                                handler.post(() -> {
                                    Toast.makeText(context, "Données envoyées par SMS avec succès",
                                            Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onSMSSendFailure(String errorMessage) {
                                Log.e("SMS_SYNC", "Échec d'envoi SMS: " + errorMessage);

                                // Afficher une notification d'erreur
                                handler.post(() -> {
                                    Toast.makeText(context, "Échec d'envoi SMS: " + errorMessage,
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                );

                // Log success
                Log.d("SMS_SYNC", "SMS envoyé avec succès pour l'agent: " + matricule);

                // Show a toast notification
                handler.post(() -> {
                    Toast.makeText(context, "Données envoyées par SMS pour synchronisation",
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("SMS_SYNC", "Échec d'envoi SMS: " + e.getMessage(), e);
                handler.post(() -> {
                    Toast.makeText(context, "Échec envoi SMS: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            Log.e("SMS_SYNC", "Permission SMS non accordée");
            handler.post(() -> {
                Toast.makeText(context, "Permission d'envoi SMS requise",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Marque un agent comme "ne pas envoyer par SMS à nouveau" (is_synchronized = 2)
     * Cette méthode est appelée après un envoi SMS réussi pour éviter les envois multiples
     * @param matricule Matricule de l'agent à marquer
     */
    private void markAgentAsDoNotSendBySMS(String matricule) {
        dbHelper helper = new dbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();

        try {
            // Vérifier si la colonne existe
            Cursor cursor = db.rawQuery("PRAGMA table_info(agents_inscription)", null);
            boolean hasSyncColumn = false;

            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1 && "is_synchronized".equals(cursor.getString(nameIndex))) {
                        hasSyncColumn = true;
                        break;
                    }
                }
                cursor.close();
            }

            // Ajouter la colonne si elle n'existe pas
            if (!hasSyncColumn) {
                db.execSQL("ALTER TABLE agents_inscription ADD COLUMN is_synchronized INTEGER DEFAULT 0");
            }

            // Mettre à jour le statut de synchronisation à 2 (ne pas envoyer par SMS à nouveau)
            ContentValues values = new ContentValues();
            values.put("is_synchronized", 2);

            int rowsUpdated = db.update(
                    "agents_inscription",
                    values,
                    "matricule = ?",
                    new String[]{matricule}
            );

            Log.d(TAG, "Agent " + matricule + " marqué comme 'synchronisé par SMS', lignes mises à jour: " + rowsUpdated);

            // Si aucune ligne n'a été mise à jour, cela pourrait indiquer un problème
            if (rowsUpdated == 0) {
                Log.w(TAG, "Aucune ligne mise à jour pour l'agent " + matricule);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du marquage de l'agent comme 'synchronisé par SMS': " + e.getMessage(), e);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Récupère le nom de l'établissement à partir du code ETS
     */
    private String fetchEtablissementName(String code_ets) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        String etablissement = null;

        try {
            // Initialisation de la base de données
            if (GlobalClass.getInstance().cnxDbReferentiel == null) {
                GlobalClass.getInstance().initDatabase("referentiel");
            }
            db = GlobalClass.getInstance().cnxDbReferentiel;

            if (db == null) {
                Log.e("fetchEtablissementName", "La connexion à la base de données est null");
                return null;
            }

            if (code_ets == null || code_ets.isEmpty()) {
                Log.e("fetchEtablissementName", "Aucun code ETS fourni");
                return null;
            }

            // Requête paramétrée
            String query = "SELECT etablissement FROM etablissements WHERE code_ets = ?";
            cursor = db.rawQuery(query, new String[]{code_ets});

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("etablissement");
                if (columnIndex != -1) {
                    etablissement = cursor.getString(columnIndex);
                } else {
                    Log.e("fetchEtablissementName", "Colonne 'etablissement' non trouvée");
                }
            } else {
                Log.e("fetchEtablissementName", "Aucun résultat pour code_ets: " + code_ets);
            }
        } catch (Exception e) {
            Log.e("fetchEtablissementName", "Erreur: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Ne pas fermer db car elle est gérée par GlobalClass
        }

        return etablissement;
    }

    private void markAgentAsSynchronized(String matricule) {
        dbHelper helper = new dbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();

        try {
            // Vérifier si la colonne existe, sinon l'ajouter
            Cursor cursor = db.rawQuery("PRAGMA table_info(agents_inscription)", null);
            boolean hasSyncColumn = false;

            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1 && "is_synchronized".equals(cursor.getString(nameIndex))) {
                        hasSyncColumn = true;
                        break;
                    }
                }
                cursor.close();
            }

            // Ajouter la colonne si elle n'existe pas
            if (!hasSyncColumn) {
                db.execSQL("ALTER TABLE agents_inscription ADD COLUMN is_synchronized INTEGER DEFAULT 0");
            }

            // Mettre à jour le statut de synchronisation
            ContentValues values = new ContentValues();
            values.put("is_synchronized", 1);

            int rowsUpdated = db.update(
                    "agents_inscription",
                    values,
                    "matricule = ?",
                    new String[]{matricule}
            );

            Log.d(TAG, "Agent marqué comme synchronisé, lignes mises à jour: " + rowsUpdated);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du marquage de l'agent comme synchronisé", e);
        } finally {
            db.close();
        }
    }


    /**
     * Envoie les données du centre si elles sont disponibles pour l'agent
     */
    private void sendCenterDataIfAvailable(String matricule) {
        dbHelper helper = new dbHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        try {
            // Récupérer les données du centre
            Cursor cursor = db.query(
                    "agents_inscription",
                    new String[]{"nom", "prenom", "telephone", "centre_sante", "photo_facade_path",
                            "photo_interieur_path", "latitude_facade", "longitude_facade",
                            "latitude_interieur", "longitude_interieur"},
                    "matricule = ?",
                    new String[]{matricule},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // Extraire les informations
                @SuppressLint("Range") String nom = cursor.getString(cursor.getColumnIndex("nom"));
                @SuppressLint("Range") String prenom = cursor.getString(cursor.getColumnIndex("prenom"));
                @SuppressLint("Range") String telephone = cursor.getString(cursor.getColumnIndex("telephone"));
                @SuppressLint("Range") String centre_sante = cursor.getString(cursor.getColumnIndex("centre_sante"));
                @SuppressLint("Range") String photoPathFacade = cursor.getString(cursor.getColumnIndex("photo_facade_path"));
                @SuppressLint("Range") String photoPathInterieur = cursor.getString(cursor.getColumnIndex("photo_interieur_path"));
                @SuppressLint("Range") double latitudeFacade = cursor.getDouble(cursor.getColumnIndex("latitude_facade"));
                @SuppressLint("Range") double longitudeFacade = cursor.getDouble(cursor.getColumnIndex("longitude_facade"));
                @SuppressLint("Range") double latitudeInterieur = cursor.getDouble(cursor.getColumnIndex("latitude_interieur"));
                @SuppressLint("Range") double longitudeInterieur = cursor.getDouble(cursor.getColumnIndex("longitude_interieur"));

                // Vérifier si les données du centre sont complètes
                if (photoPathFacade != null && photoPathInterieur != null) {
                    // Préparer les données pour l'API
                    Map<String, Object> formData = new HashMap<>();
                    formData.put("nom_agent", nom + " " + prenom);
                    formData.put("contact", telephone);
                    formData.put("nom_etablissement", centre_sante);

                    SharedPrefManager sharedPrefManager = new SharedPrefManager(context);
                    formData.put("code_ets", sharedPrefManager.getCodeEts());

                    UtilsInfosAppareil utilsInfos = new UtilsInfosAppareil(context);
                    String idFamoco = utilsInfos.recupererIdAppareil();
                    formData.put("idFamoco", idFamoco);

                    Map<String, Double> coordFacade = new HashMap<>();
                    coordFacade.put("latitude", latitudeFacade);
                    coordFacade.put("longitude", longitudeFacade);
                    formData.put("coordonnees_facade", coordFacade);

                    Map<String, Double> coordInterieur = new HashMap<>();
                    coordInterieur.put("latitude", latitudeInterieur);
                    coordInterieur.put("longitude", longitudeInterieur);
                    formData.put("coordonnees_interieur", coordInterieur);

                    // Appeler l'API pour envoyer les données du centre
                    sendDataToApiCentre(formData, matricule);
                }

                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des données du centre", e);
        } finally {
            db.close();
        }
    }



    /**
     * Envoie les données du centre au serveur
     * @param formData Données du centre à envoyer
     * @param matriculeUtilisateur Matricule de l'agent associé au centre
     */
    private void sendDataToApiCentre(Map<String, Object> formData, String matriculeUtilisateur) {
        OkHttpClient client = new OkHttpClient();

        try {
            // Récupérer les chemins des fichiers
            String photoPathFacade = null;
            String photoPathInterieur = null;

            // Récupérer les informations du centre depuis la base de données
            dbHelper helper = new dbHelper(context);
            SQLiteDatabase db = helper.getReadableDatabase();

            Cursor cursor = db.query(
                    "agents_inscription",
                    new String[]{"photo_facade_path", "photo_interieur_path"},
                    "matricule = ?",
                    new String[]{matriculeUtilisateur},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") String facadePath = cursor.getString(cursor.getColumnIndex("photo_facade_path"));
                @SuppressLint("Range") String interieurPath = cursor.getString(cursor.getColumnIndex("photo_interieur_path"));

                photoPathFacade = facadePath;
                photoPathInterieur = interieurPath;

                cursor.close();
            }
            db.close();

            if (photoPathFacade == null || photoPathInterieur == null) {
                Log.e(TAG, "Chemins des photos non trouvés pour l'agent: " + matriculeUtilisateur);
                return;
            }

            //File fileFacade = new File(photoPathFacade);
            //File fileInterieur = new File(photoPathInterieur);

            File fileFacade = compressImage(new File(photoPathFacade));
            File fileInterieur = compressImage(new File(photoPathInterieur));

            if (!fileFacade.exists() || !fileInterieur.exists()) {
                Log.e(TAG, "Fichiers image introuvables");
                return;
            }

            Log.d(TAG, "Fichier façade: " + fileFacade.getAbsolutePath() + " (" + fileFacade.length() + " bytes)");
            Log.d(TAG, "Fichier intérieur: " + fileInterieur.getAbsolutePath() + " (" + fileInterieur.length() + " bytes)");

            MediaType mediaType = MediaType.parse("image/jpeg");
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

            builder.addFormDataPart("nom_agent", (String) formData.get("nom_agent"));
            builder.addFormDataPart("contact", (String) formData.get("contact"));
            builder.addFormDataPart("code_ets", (String) formData.get("code_ets"));
            builder.addFormDataPart("id_famoco", (String) formData.get("idFamoco"));

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentDateTime = dateFormat.format(new Date());
            builder.addFormDataPart("date_heure", currentDateTime);
            builder.addFormDataPart("id_agent", matriculeUtilisateur);

            Map<String, Double> coordFacade = (Map<String, Double>) formData.get("coordonnees_facade");
            Map<String, Double> coordInterieur = (Map<String, Double>) formData.get("coordonnees_interieur");

            builder.addFormDataPart("latitude_font", String.valueOf(coordFacade.get("latitude")));
            builder.addFormDataPart("longitude_font", String.valueOf(coordFacade.get("longitude")));
            builder.addFormDataPart("latitude_in", String.valueOf(coordInterieur.get("latitude")));
            builder.addFormDataPart("longitude_in", String.valueOf(coordInterieur.get("longitude")));

            builder.addFormDataPart("image_font", fileFacade.getName(),
                    RequestBody.create(fileFacade, mediaType));
            builder.addFormDataPart("image_in", fileInterieur.getName(),
                    RequestBody.create(fileInterieur, mediaType));

            RequestBody requestBody = builder.build();
            Log.d(TAG, "API requestBody: Code: " + requestBody);
            Request request = new Request.Builder()
                    .url("http://57.128.30.4:8090/api/v1/addEtablissementLocalisation")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur réseau", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.d(TAG, "API Response: Code: " + response.code() + ", Body: " + responseBody);

                    if (response.isSuccessful()) {
                        Log.d(TAG, "Données envoyées avec succès pour le centre !");
                    } else {
                        Log.e(TAG, "Erreur: " + response.message());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation envoi", e);
        }
    }

    // Ajouter une fonction pour compresser l'image
    private File compressImage(File originalFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath());
            File compressedFile = new File(context.getCacheDir(), "compressed_" + originalFile.getName());

            FileOutputStream fos = new FileOutputStream(compressedFile);
            // Compresser avec une qualité de 70% (ajuster selon vos besoins)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
            fos.flush();
            fos.close();

            Log.d(TAG, "Image compressée: " + compressedFile.getAbsolutePath() +
                    " (" + compressedFile.length() + " bytes vs " + originalFile.length() + " bytes)");

            return compressedFile;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la compression", e);
            return originalFile; // En cas d'échec, retourner l'original
        }
    }

    /*public void startRetryScan() {
        if (isRetryScanActive) return;

        isRetryScanActive = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isNetworkGood()) {
                    processQueue();
                }

                // Reprogrammer la vérification si la file n'est pas vide
                if (!isQueueEmpty()) {
                    isRetryScanActive = true;
                    handler.postDelayed(this, RETRY_INTERVAL_MS);
                } else {
                    isRetryScanActive = false;
                }
            }
        });
    }*/

    //////////////////

    /**
     * Traite la file d'attente d'envoi
     */

    /*private void processQueue() {
        executorService.execute(() -> {
            try {
                JSONArray queueArray = getQueueArray();

                if (queueArray.length() > 0) {
                    // Envoyer un broadcast pour notifier que la synchronisation a commencé
                    Intent syncIntent = new Intent(ACTION_SYNC_STARTED);
                    context.sendBroadcast(syncIntent);
                }

                List<Integer> itemsToRemove = new ArrayList<>();


                for (int i = 0; i < queueArray.length(); i++) {
                    JSONObject item = queueArray.getJSONObject(i);
                    String path = item.getString("path");
                    int attempts = item.getInt("attempts");

                    File file = new File(path);
                    if (!file.exists()) {
                        // Fichier supprimé, le retirer de la file
                        itemsToRemove.add(i);
                        continue;
                    }

                    // Limiter le nombre de tentatives
                    if (attempts > 10) {
                        Log.w(TAG, "Trop de tentatives pour " + path + ", fichier abandonné");
                        file.delete();
                        itemsToRemove.add(i);
                        continue;
                    }

                    // Mettre à jour le compteur de tentatives
                    item.put("attempts", attempts + 1);
                    queueArray.put(i, item);
                    saveQueueArray(queueArray);

                    // Tenter d'envoyer le fichier
                    NetworkUtils.uploadImage(context, file, new NetworkUtils.UploadCallback() {
                        @Override
                        public void onSuccess() {
                            // Supprimer l'élément de la file et le fichier
                            Log.d(TAG, "Upload réussi depuis la file: " + path);
                            removeFromQueue(path);
                            file.delete();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Échec d'upload depuis la file: " + error);
                            // L'élément reste dans la file pour une prochaine tentative
                        }
                    });

                    // Pause entre chaque upload pour éviter de surcharger
                    Thread.sleep(2000);
                }

                // Supprimer les éléments marqués
                if (!itemsToRemove.isEmpty()) {
                    // Parcourir en ordre inverse pour éviter les décalages d'index
                    for (int i = itemsToRemove.size() - 1; i >= 0; i--) {
                        queueArray.remove(itemsToRemove.get(i));
                    }
                    saveQueueArray(queueArray);
                }

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du traitement de la file", e);
            }
        });
    }*/
    /*private void processQueue() {
        executorService.execute(() -> {
            try {
                JSONArray queueArray = getQueueArray();
                if (queueArray.length() == 0) {
                    return; // Rien à traiter
                }

                // Vérifier la connectivité avant de commencer le traitement
                if (!isNetworkGood()) {
                    Log.d(TAG, "Connexion insuffisante, report du traitement");
                    return; // Reporter à plus tard
                }

                // Notifier qu'une synchronisation commence
                if (queueArray.length() > 0) {
                    Intent syncIntent = new Intent(ACTION_SYNC_STARTED);
                    context.sendBroadcast(syncIntent);
                }

                List<Integer> itemsToRemove = new ArrayList<>();

                for (int i = 0; i < queueArray.length(); i++) {
                    JSONObject item = queueArray.getJSONObject(i);
                    String path = item.getString("path");
                    int attempts = item.getInt("attempts");

                    File file = new File(path);
                    if (!file.exists()) {
                        // Fichier supprimé, le retirer de la file
                        itemsToRemove.add(i);
                        continue;
                    }

                    // Limiter le nombre de tentatives
                    if (attempts > 10) {
                        Log.w(TAG, "Trop de tentatives pour " + path + ", fichier abandonné");
                        file.delete();
                        itemsToRemove.add(i);
                        continue;
                    }

                    // Mettre à jour le compteur de tentatives
                    item.put("attempts", attempts + 1);
                    queueArray.put(i, item);
                    saveQueueArray(queueArray);

                    // Tenter d'envoyer le fichier
                    NetworkUtils.uploadImage(context, file, new NetworkUtils.UploadCallback() {
                        @Override
                        public void onSuccess() {
                            // Supprimer l'élément de la file et le fichier
                            Log.d(TAG, "Upload réussi depuis la file: " + path);
                            removeFromQueue(path);
                            file.delete();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Échec d'upload depuis la file: " + error);
                            // L'élément reste dans la file pour une prochaine tentative
                        }
                    });

                    // Pause entre chaque upload pour éviter de surcharger
                    Thread.sleep(2000);
                }

                // Supprimer les éléments marqués
                if (!itemsToRemove.isEmpty()) {
                    // Parcourir en ordre inverse pour éviter les décalages d'index
                    for (int i = itemsToRemove.size() - 1; i >= 0; i--) {
                        queueArray.remove(itemsToRemove.get(i));
                    }
                    saveQueueArray(queueArray);
                }

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du traitement de la file", e);
            }
        });
    }*/

    private void processQueue() {
        executorService.execute(() -> {
            try {
                JSONArray queueArray = getQueueArray();
                if (queueArray.length() > 0) {
                    // Envoyer le broadcast AVANT de commencer les uploads
                    Intent syncIntent = new Intent(ACTION_SYNC_STARTED);
                    context.sendBroadcast(syncIntent);
                    Log.d(TAG, "Broadcast de début de synchronisation envoyé");
                }
                if (queueArray.length() == 0) {
                    return; // Rien à traiter
                }

                // Vérifier la connectivité avant de commencer le traitement
                if (!isNetworkGood()) {
                    Log.d(TAG, "Connexion insuffisante, report du traitement");
                    return; // Reporter à plus tard
                }

                // Notifier qu'une synchronisation commence
                if (queueArray.length() > 0) {
                    Intent syncIntent = new Intent(ACTION_SYNC_STARTED);
                    context.sendBroadcast(syncIntent);
                    Log.d(TAG, "Broadcast de début de synchronisation envoyé");
                }

                List<Integer> itemsToRemove = new ArrayList<>();
                final AtomicInteger pendingUploads = new AtomicInteger(0);

                for (int i = 0; i < queueArray.length(); i++) {
                    JSONObject item = queueArray.getJSONObject(i);
                    String path = item.getString("path");
                    String numTrans = item.getString("numTrans");
                    int attempts = item.getInt("attempts");

                    File file = new File(path);
                    if (!file.exists()) {
                        // Fichier supprimé, le retirer de la file
                        itemsToRemove.add(i);
                        continue;
                    }

                    // Limiter le nombre de tentatives
                    if (attempts > 10) {
                        Log.w(TAG, "Trop de tentatives pour " + path + ", fichier abandonné");
                        file.delete();
                        itemsToRemove.add(i);
                        continue;
                    }

                    // Mettre à jour le compteur de tentatives
                    item.put("attempts", attempts + 1);
                    queueArray.put(i, item);
                    saveQueueArray(queueArray);

                    // Incrémenter le compteur d'uploads en cours
                    pendingUploads.incrementAndGet();

                    // Tenter d'envoyer le fichier
                    NetworkUtils.uploadImage(context, file,numTrans, new NetworkUtils.UploadCallback() {
                        @Override
                        public void onSuccess() {
                            // Supprimer l'élément de la file et le fichier
                            Log.d(TAG, "Upload réussi depuis la file: " + path);
                            removeFromQueue(path);
                            file.delete();

                            // Décrémenter le compteur et vérifier si c'est le dernier
                            if (pendingUploads.decrementAndGet() == 0) {
                                // Envoyer le broadcast de fin de synchronisation
                                Intent completedIntent = new Intent(ACTION_SYNC_COMPLETED);
                                context.sendBroadcast(completedIntent);
                                Log.d(TAG, "Broadcast de fin de synchronisation envoyé (succès)");
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Échec d'upload depuis la file: " + error);
                            // L'élément reste dans la file pour une prochaine tentative

                            // Décrémenter le compteur et vérifier si c'est le dernier
                            if (pendingUploads.decrementAndGet() == 0) {
                                // Envoyer le broadcast de fin de synchronisation même en cas d'erreur
                                Intent completedIntent = new Intent(ACTION_SYNC_COMPLETED);
                                context.sendBroadcast(completedIntent);
                                Log.d(TAG, "Broadcast de fin de synchronisation envoyé (avec erreurs)");
                            }
                        }
                    });

                    // Pause entre chaque upload pour éviter de surcharger
                    Thread.sleep(2000);
                }

                // Supprimer les éléments marqués
                if (!itemsToRemove.isEmpty()) {
                    // Parcourir en ordre inverse pour éviter les décalages d'index
                    for (int i = itemsToRemove.size() - 1; i >= 0; i--) {
                        queueArray.remove(itemsToRemove.get(i));
                    }
                    saveQueueArray(queueArray);
                }

                // Si aucun upload n'a été démarré (tous les fichiers étaient invalides par exemple)
                if (pendingUploads.get() == 0) {
                    // Envoyer quand même le broadcast de fin
                    Intent completedIntent = new Intent(ACTION_SYNC_COMPLETED);
                    context.sendBroadcast(completedIntent);
                    Log.d(TAG, "Broadcast de fin de synchronisation envoyé (aucun traitement)");
                }

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du traitement de la file", e);

                // En cas d'erreur générale, envoyer quand même le broadcast de fin
                Intent completedIntent = new Intent(ACTION_SYNC_COMPLETED);
                context.sendBroadcast(completedIntent);
                Log.d(TAG, "Broadcast de fin de synchronisation envoyé (après erreur)");
            }
        });
    }


    /**
     * Supprime un élément de la file d'attente
     */
    private void removeFromQueue(String filePath) {
        try {
            JSONArray queueArray = getQueueArray();
            JSONArray newArray = new JSONArray();

            for (int i = 0; i < queueArray.length(); i++) {
                JSONObject item = queueArray.getJSONObject(i);
                if (!item.getString("path").equals(filePath)) {
                    newArray.put(item);
                }
            }

            saveQueueArray(newArray);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la suppression de la file", e);
        }
    }

    /**
     * Vérifie si la file d'attente est vide
     */
    public boolean isQueueEmpty() {
        return getQueueArray().length() == 0;
    }

    /**
     * Récupère la file d'attente d'envoi
     */
    private JSONArray getQueueArray() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String queueString = prefs.getString(QUEUE_KEY, "[]");
        try {
            return new JSONArray(queueString);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    /**
     * Sauvegarde la file d'attente d'envoi
     */
    private void saveQueueArray(JSONArray queueArray) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(QUEUE_KEY, queueArray.toString());
        editor.apply();
    }
}