package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.DownloadService;
import ci.technchange.prestationscmu.core.dbHelper;
import ci.technchange.prestationscmu.utils.Loger;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.synchroDonnees;

public class SplashscreenActivity extends AppCompatActivity /*implements LocationListener*/ {
    private Loger loger = null;
    private String[] perms;

    private static final String TAG = SplashscreenActivity.class.getSimpleName();
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_PERMISSION_WRITE = 1001;
    private int oldProgress=0;
    private static final int LOCATION_TIMEOUT = 30000; //  30 secondes
    private boolean locationObtained = false;
    private boolean downloadStarted = false; // Flag pour éviter de démarrer le téléchargement plusieurs fois

    private ProgressBar progressBar;
    private TextView progressText;
    private BroadcastReceiver progressReceiver;
    private LocationManager locationManager;
    private SharedPrefManager sharedPrefManager;
    private synchroDonnees synchro;
    private Handler locationTimeoutHandler = new Handler();

    // Enum pour représenter les différents statuts d'agent
    private enum AgentStatus {
        NO_AGENT,             // Aucun agent n'existe
        AGENT_WITHOUT_CENTER, // Agent existe mais sans centre
        AGENT_COMPLETE        // Agent complet avec centre
    }
    // Variable pour stocker le matricule de l'agent trouvé
    private String agentMatricule;

    private void addLog(String message) {
        loger.addToLog(TAG, message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        loger = new Loger();
        sharedPrefManager = new SharedPrefManager(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        synchro = new synchroDonnees(this);
        Log.d("SplashscreenActivity", "In Splash");

        // Vérification immédiate de l'existence de la base de données
        File dbFile = new File(getFilesDir(), "encryptedbd/newcnambd1.db");
        if (dbFile.exists()) {
            // Si la base existe déjà, passer directement à MainActivity
            loger.addToLog(TAG, "Base de données déjà téléchargée, redirection vers la page d'acceuil");
            navigateToMainActivity();
            return ;
        }

        // Si la base n'existe pas, continuer normalement
        setupProgressReceiver();
        //checkPermissions();
        checkDatabase();

        /*setupProgressReceiver();
        checkPermissions();
        checkDatabase();**/

    }

    /*private void navigateToMainActivity() {
        //Intent intent = new Intent(this, ConnexionActivity.class);
        Intent intent = new Intent(this, OnboardingActivity.class);
        startActivity(intent);
        finish();}*/

    private void navigateToMainActivity() {
        // Vérifier si un agent existe et si son centre est renseigné
        AgentStatus agentStatus = checkAgentStatus();

        Intent intent;

        switch(agentStatus) {
            case NO_AGENT:
                // Si aucun agent n'existe, rediriger vers l'onboarding
                intent = new Intent(this, OnboardingActivity.class);
                Log.d(TAG, "Aucun agent n'existe, redirection vers l'onboarding");
                break;

            case AGENT_WITHOUT_CENTER:
                // Récupérer toutes les informations de l'agent
                Map<String, Object> agentInfo = getAgentInfo(agentMatricule);

                if (agentInfo != null && agentInfo.containsKey("date_inscription")) {
                    try {
                        // Récupérer la date d'inscription
                        String dateInscription = (String) agentInfo.get("date_inscription");

                        // Convertir la date d'inscription en objet Date
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        Date inscriptionDate = dateFormat.parse(dateInscription);
                        Date currentDate = new Date(); // Date actuelle

                        // Calculer la différence en millisecondes
                        long differenceMs = currentDate.getTime() - inscriptionDate.getTime();
                        // Convertir en heures
                        long differenceHours = differenceMs / (60 * 60 * 1000);

                        Log.d(TAG, "Temps écoulé depuis l'inscription : " + differenceHours + " heures");
                        Log.d(TAG, "Temps écoulé depuis l'inscription : " + differenceMs + " millisecondes");

                        if (differenceHours >= 24) {
                            // Si plus de 24 heures se sont écoulées, rediriger vers ReminderActivity
                            intent = new Intent(this, ReminderActivity.class);
                            intent.putExtra("MATRICULE", agentMatricule);
                            // Vous pouvez également passer d'autres informations de l'agent si nécessaire
                            intent.putExtra("NOM", (String) agentInfo.get("nom"));
                            intent.putExtra("PRENOM", (String) agentInfo.get("prenom"));
                            intent.putExtra("NUMERO", (String) agentInfo.get("telephone"));
                            Log.d(TAG, "Plus de 24h depuis l'inscription, redirection vers l'écran de rappel");
                        } else {
                            // Si moins de 24 heures, rediriger vers MainActivity
                            intent = new Intent(this, ConnexionActivity.class);
                            intent.putExtra("MATRICULE", agentMatricule);
                            Log.d(TAG, "Moins de 24h depuis l'inscription, redirection vers l'écran principal");
                        }
                    } catch (Exception e) {
                        // En cas d'erreur de parsing de date, rediriger vers ReminderActivity par défaut
                        Log.e(TAG, "Erreur lors du calcul de la différence de temps", e);
                        intent = new Intent(this, ReminderActivity.class);
                        intent.putExtra("MATRICULE", agentMatricule);
                    }
                } else {
                    // Si les informations de l'agent n'ont pas été trouvées, rediriger vers ReminderActivity
                    intent = new Intent(this, ReminderActivity.class);
                    intent.putExtra("MATRICULE", agentMatricule);
                    Log.d(TAG, "Informations de l'agent non trouvées, redirection vers l'écran de rappel");
                }
                break;

            case AGENT_COMPLETE:
            default:
                // Si un agent existe avec centre, rediriger vers la page de connexion
                intent = new Intent(this, ConnexionActivity.class);
                Log.d(TAG, "Un agent complet existe, redirection vers l'écran de connexion");
                break;
        }

        startActivity(intent);
        finish();
    }

    /**
     * Récupère la date d'inscription d'un agent à partir de son matricule
     * @param matricule Le matricule de l'agent
     * @return La date d'inscription au format "yyyy-MM-dd HH:mm:ss" ou null si non trouvée
     */
    private String getAgentInscriptionDate(String matricule) {
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        String dateInscription = null;

        try {
            Cursor cursor = db.query(
                    "agents_inscription",
                    new String[]{"date_inscription"},
                    "matricule = ?",
                    new String[]{matricule},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int dateIndex = cursor.getColumnIndex("date_inscription");
                if (dateIndex >= 0) {
                    dateInscription = cursor.getString(dateIndex);
                    Log.d(TAG, "Date d'inscription récupérée : " + dateInscription);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération de la date d'inscription", e);
        } finally {
            db.close();
        }

        return dateInscription;
    }

    /**
     * Récupère toutes les informations d'un agent à partir de son matricule
     * @param matricule Le matricule de l'agent
     * @return Un Map contenant toutes les informations de l'agent ou null si non trouvé
     */
    private Map<String, Object> getAgentInfo(String matricule) {
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        Map<String, Object> agentInfo = null;

        try {
            Cursor cursor = db.query(
                    "agents_inscription",
                    null, // Sélectionner toutes les colonnes
                    "matricule = ?",
                    new String[]{matricule},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                agentInfo = new HashMap<>();

                // Récupérer toutes les colonnes et leurs valeurs
                String[] columnNames = cursor.getColumnNames();
                for (String columnName : columnNames) {
                    int columnIndex = cursor.getColumnIndex(columnName);

                    if (columnIndex >= 0) {
                        // Déterminer le type de colonne et récupérer la valeur correspondante
                        switch (cursor.getType(columnIndex)) {
                            case Cursor.FIELD_TYPE_STRING:
                                agentInfo.put(columnName, cursor.getString(columnIndex));
                                break;
                            case Cursor.FIELD_TYPE_INTEGER:
                                agentInfo.put(columnName, cursor.getInt(columnIndex));
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                agentInfo.put(columnName, cursor.getFloat(columnIndex));
                                break;
                            case Cursor.FIELD_TYPE_BLOB:
                                agentInfo.put(columnName, cursor.getBlob(columnIndex));
                                break;
                            case Cursor.FIELD_TYPE_NULL:
                                agentInfo.put(columnName, null);
                                break;
                        }
                    }
                }
                Log.d(TAG, "Informations de l'agent récupérées : " + agentInfo.toString());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des informations de l'agent", e);
        } finally {
            db.close();
        }

        return agentInfo;
    }

    /**
     * Vérifie si au moins un agent existe dans la base de données et si son centre est renseigné
     * @return le statut de l'agent (NO_AGENT, AGENT_WITHOUT_CENTER ou AGENT_COMPLETE)
     */
    private AgentStatus checkAgentStatus() {
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        AgentStatus status = AgentStatus.NO_AGENT;
        agentMatricule = "";

        try {
            // Vérifier d'abord si la table existe
            Cursor tableCheck = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='agents_inscription'",
                    null);

            boolean tableExists = tableCheck != null && tableCheck.moveToFirst();
            if (tableCheck != null) {
                tableCheck.close();
            }

            if (!tableExists) {
                Log.d(TAG, "La table agents_inscription n'existe pas encore");
                return AgentStatus.NO_AGENT;
            }

            // Récupérer les informations de l'agent
            Cursor cursor = db.query(
                    "agents_inscription",
                    new String[]{"matricule", "latitude_facade", "longitude_facade"},
                    null,
                    null,
                    null,
                    null,
                    null,
                    "1" // Limiter à un seul résultat
            );

            if (cursor != null && cursor.moveToFirst()) {
                // Récupérer les indices des colonnes
                int matriculeIndex = cursor.getColumnIndex("matricule");
                int latitudeFacadeIndex = cursor.getColumnIndex("latitude_facade");
                int longitudeFacadeIndex = cursor.getColumnIndex("longitude_facade");
                Log.d("SplashscreenActivity", "In check agent - index matricule: " + matriculeIndex);
                Log.d("SplashscreenActivity", "In check agent - index latitude_facade: " + latitudeFacadeIndex);
                Log.d("SplashscreenActivity", "In check agent - index longitude_facade: " + longitudeFacadeIndex);
                // Récupérer les valeurs
                if (matriculeIndex >= 0) {
                    agentMatricule = cursor.getString(matriculeIndex);

                }

                // Vérifier si le centre est renseigné
                if (latitudeFacadeIndex >= 0 && longitudeFacadeIndex >= 0) {

                    // Vérifier si les valeurs sont définies et non nulles
                    boolean hasLatitude = !cursor.isNull(latitudeFacadeIndex);
                    boolean hasLongitude = !cursor.isNull(longitudeFacadeIndex);

                    double latitudeFacade = hasLatitude ? cursor.getDouble(latitudeFacadeIndex) : 0;
                    double longitudeFacade = hasLongitude ? cursor.getDouble(longitudeFacadeIndex) : 0;

                    // Un agent est considéré comme complet si les deux coordonnées existent et sont différentes de zéro
                    if (hasLatitude && hasLongitude && latitudeFacade != 0 && longitudeFacade != 0) {
                        status = AgentStatus.AGENT_COMPLETE;
                        Log.d(TAG, "Agent trouvé avec coordonnées façade: " + agentMatricule +
                                " (Lat: " + latitudeFacade + ", Long: " + longitudeFacade + ")");
                    } else {
                        status = AgentStatus.AGENT_WITHOUT_CENTER;
                        Log.d(TAG, "Agent trouvé sans coordonnées façade valides: " + agentMatricule);
                    }
                } else {
                    status = AgentStatus.AGENT_WITHOUT_CENTER;
                    Log.d(TAG, "Agent trouvé mais pas de colonne centre_sante");
                }

                cursor.close();
            } else {
                status = AgentStatus.NO_AGENT;
                Log.d(TAG, "Aucun agent trouvé dans la base de données");
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification des agents: " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.close();
        }

        return status;
    }

    /**
     * Vérifie si au moins un agent existe dans la base de données
     * @return true si au moins un agent existe, false sinon
     */
    private boolean checkIfAgentExists() {
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        boolean exists = false;

        try {
            // Vérifier d'abord si la table existe
            Cursor tableCheck = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='agents_inscription'",
                    null);

            boolean tableExists = tableCheck != null && tableCheck.moveToFirst();
            if (tableCheck != null) {
                tableCheck.close();
            }

            if (!tableExists) {
                Log.d(TAG, "La table agents_inscription n'existe pas encore");
                return false;
            }

            // Compter le nombre d'agents dans la table
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM agents_inscription", null);

            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                exists = count > 0;

                Log.d(TAG, "Nombre d'agents trouvés: " + count);
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification des agents: " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.close();
        }

        return exists;
    }
    private void checkDatabase() {
        File dbapp = new File(getFilesDir(), "prestations_fse.db");
        if (!dbapp.exists()) {
            // Création de la base de données
            dbHelper db = new dbHelper(this.getApplicationContext());
            progressText.setText("Verification des structures de données...");
            db.checkDataBasesReferentiel();
            progressText.setText("Verification des structures de données...1/3");
            db.checkDataBasesApp();
            progressText.setText("Verification des structures de données...2/3");
            Log.d("SplashscreenActivity", "In check Database");
            navigateToMainActivity();
        }
    }

    private void checkPermissions() {
        // Vérifier les permissions d'écriture
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE);
            return; // On sort et on attendra le callback onRequestPermissionsResult
        }

        // Vérifier les permissions de localisation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
            return; // On sort et on attendra le callback onRequestPermissionsResult
        }

        //getLocation();

        // Si on arrive ici, on a toutes les permissions
        /*if (!sharedPrefManager.hasLocationData()){
            getLocation();
        }**/

    }

    private void setupProgressReceiver() {
        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadService.PROGRESS_UPDATE_ACTION.equals(intent.getAction())) {
                    int progress = intent.getIntExtra(DownloadService.PROGRESS_EXTRA, 0);
                    updateProgress(progress);

                    if (intent.hasExtra("result")) {
                        String result = intent.getStringExtra("result");
                        // Récupérer et sauvegarder le nom original du fichier
                        if (intent.hasExtra("originalFileName")) {
                            String originalFileName = intent.getStringExtra("originalFileName");
                            sharedPrefManager.saveDownloadedFileName(originalFileName);
                            System.out.println("Nom de fichier sauvegardé dans les préférences: " + originalFileName);
                        }


                        handleResult(result);
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter(DownloadService.PROGRESS_UPDATE_ACTION));
    }

    /*private void setupProgressReceiver() {
        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadService.PROGRESS_UPDATE_ACTION.equals(intent.getAction())) {
                    int progress = intent.getIntExtra(DownloadService.PROGRESS_EXTRA, 0);
                    updateProgress(progress);

                    if (intent.hasExtra("result")) {
                        String result = intent.getStringExtra("result");

                        if (intent.hasExtra("fileType") && intent.getStringExtra("fileType").equals("enroles_sql_files.zip")) {
                            // Le fichier enroles_sql_files.zip est téléchargé
                            File zipFile = new File(getFilesDir(), "encryptedbd/enroles_sql_files.zip");
                            if (zipFile.exists()) {
                                try {
                                    synchro.setProgressListener((totalFiles, currentFile, processedLines, totalLines) -> {
                                        runOnUiThread(() -> {
                                            progressText.setText("Décompression et exécution : Fichier " + currentFile + "/" + totalFiles +
                                                    ", Lignes " + processedLines + "/" + totalLines);
                                        });
                                    });
                                    synchro.unzipDossierFile(zipFile);

                                    // Nettoyer les fichiers temporaires
                                    File outputDir = new File(getFilesDir(), "unzipped");
                                    //synchro.cleanup(zipFile, outputDir);

                                    // Passer à l'activité principale
                                    handleResult("OK");
                                } catch (IOException e) {
                                    Log.e(TAG, "Erreur lors du dézippage du fichier", e);
                                    Toast.makeText(SplashscreenActivity.this, "Erreur lors du dézippage du fichier", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e(TAG, "Le fichier enroles_sql_files.zip n'existe pas");
                                Toast.makeText(SplashscreenActivity.this, "Le fichier enroles_sql_files.zip n'existe pas", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            handleResult(result);
                        }
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter(DownloadService.PROGRESS_UPDATE_ACTION));
    }**/


    /*private void getLocation() {
        progressText.setText("Chargement des données...");
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
            Log.d("GetLocation", "info");
            // Aucun fournisseur de localisation n'est disponible
            useDefaultLocationAndContinue();
            return;
        }

        // Définir un timeout pour ne pas bloquer l'application
        locationTimeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!locationObtained) {
                    locationObtained = true;

                    Location lastKnownLocation = null;

                    if (ContextCompat.checkSelfPermission(SplashscreenActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(SplashscreenActivity.this,
                                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        }

                        if (lastKnownLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }
                    }

                    if (lastKnownLocation != null) {

                        double latitude = lastKnownLocation.getLatitude();
                        double longitude = lastKnownLocation.getLongitude();
                        sharedPrefManager.saveLocation(latitude, longitude);

                        Log.d("Localisation", "Dernière localisation connue utilisée: Lat=" + latitude + ", Long=" + longitude);
                        Log.d("Localisation", "Dernière localisation connue utilisée sharePrefManger: Lat=" + sharedPrefManager.getLatitude() + ", Long=" + sharedPrefManager.getLongitude());
                        if (!downloadStarted) {
                            //startDownloadService();
                        }
                    } else {
                        // Si on n'a pas de dernière localisation connue, utiliser la localisation par défaut
                        useDefaultLocationAndContinue();
                    }
                }
            }
        }, LOCATION_TIMEOUT);

        // Demander la localisation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (isGPSEnabled) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
            } else if (isNetworkEnabled) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
            }
        }
    }*/

    private void useDefaultLocationAndContinue() {
        // Utiliser des coordonnées par défaut pour Abidjan
        sharedPrefManager.saveLocation(5.32412, -4.02059);
        progressText.setText("Utilisation de la localisation par défaut");
        //Log.d(TAG, "Utilisation des coordonnées par défaut: Lat=5.212884, Long=-3.743226");

        // Continuer avec le téléchargement
        if (!downloadStarted) {
            //startDownloadService();
        }
    }

   /* @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocation", "Ddedans");
        // Annuler le timeout car nous avons obtenu la localisation
        locationTimeoutHandler.removeCallbacksAndMessages(null);

        if (!locationObtained) {
            locationObtained = true;

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            sharedPrefManager.saveLocation(latitude, longitude);

            // Afficher les coordonnées pour debug
            Log.d(TAG, "Localisation obtenue: Lat=" + latitude + ", Long=" + longitude);
            //progressText.setText("Localisation obtenue: Lat=" + latitude + ", Long=" + longitude);

            locationManager.removeUpdates(this);

            // Maintenant que nous avons la localisation, lancer le téléchargement
            if (!downloadStarted) {
                //startDownloadService();
            }
        }
    }*/

    private void startDownloadService() {
        downloadStarted = true;

        Log.d(TAG, "startDownloadService - Lat: " + sharedPrefManager.getLatitude() + ", Long: " + sharedPrefManager.getLongitude());

        File externalDir = new File(getFilesDir(), "encryptedbd");
        if (!externalDir.exists()) externalDir.mkdirs();


        File file = new File(externalDir, "newcnambd1.db");

        if(!file.exists()){
            System.out.println("coordonnée utilisée pour téléchargement initial:");
            System.out.println("Longitude: " + sharedPrefManager.getLongitude());
            System.out.println("Latitude: " + sharedPrefManager.getLatitude());

            // Si newcnambd1.db n'existe pas, le télécharger
            String fileUrl = "http://57.128.30.4:8089/bdregion.php?latitude=" +
                    sharedPrefManager.getLatitude() + "&longitude=" + sharedPrefManager.getLongitude();
            //String fileUrl = "http://57.128.30.4:8089/bdregion.php?latitude=9.410786&longitude=-7.513318";


            Log.d("Lien ",fileUrl);
            Intent serviceIntent = new Intent(this, DownloadService.class);
            serviceIntent.putExtra("fileUrl", fileUrl);
            ContextCompat.startForegroundService(this, serviceIntent);

            navigateToMainActivity();
        }else{
            System.out.println("la base de données existe:"+file.getAbsolutePath());
            handleResult("OK");

        }

        /*if (file.exists()) {
            progressText.setText("Synchronisation des données");
            progressText.setText("Préparation du téléchargement...");
            System.out.println("coordonnée utilisée pour synchro:");
            System.out.println("Longitude: " + sharedPrefManager.getLongitude());
            System.out.println("Latitude: " + sharedPrefManager.getLatitude());
            FseServiceDb fseServiceDb = FseServiceDb.getInstance(this);
            int currentVersion = fseServiceDb.getVersionBD();
            Log.d("Version bd", "la version est à "+currentVersion);
            String apiUpdateDb = "http://51.38.224.233:8080/api/v1/synchronisation?longitude=" +
                    sharedPrefManager.getLongitude() + "&latitude=" + sharedPrefManager.getLatitude() + "&version="+currentVersion;

            Intent serviceIntent = new Intent(this, DownloadService.class);
            serviceIntent.putExtra("apiUpdateDb", apiUpdateDb);
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            System.out.println("coordonnée utilisée pour téléchargement initial:");
            System.out.println("Longitude: " + sharedPrefManager.getLongitude());
            System.out.println("Latitude: " + sharedPrefManager.getLatitude());

            // Si newcnambd1.db n'existe pas, le télécharger
            String fileUrl = "http://57.128.30.4:8089/bdregion.php?latitude=" +
                    sharedPrefManager.getLatitude() + "&longitude=" + sharedPrefManager.getLongitude();
            Log.d("Lien ",fileUrl);
            Intent serviceIntent = new Intent(this, DownloadService.class);
            serviceIntent.putExtra("fileUrl", fileUrl);
            ContextCompat.startForegroundService(this, serviceIntent);
        }**/
    }

    private void handleResult(String result) {
        progressText.setText("Téléchargement terminé");
        String downloadedFileName = sharedPrefManager.getDownloadedFileName();
        Log.d(TAG, "Nom du fichier téléchargé: " + downloadedFileName);
        if (!downloadStarted) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // S'assurer que le handler de timeout est annulé
        locationTimeoutHandler.removeCallbacksAndMessages(null);

        if (progressReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        }
    }

    /*@Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_WRITE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Maintenant, vérifier la permission de localisation
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
            } else {
                //getLocation();
            }
        } else if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //getLocation();
        } else {
            // Si une permission est refusée, utiliser des valeurs par défaut
            useDefaultLocationAndContinue();
        }
    }

    private void updateProgress(int progress) {
        if(oldProgress < progress) {
            progressBar.setProgress(progress);
            progressText.setText("Téléchargement en cours : " + progress + "%");
            oldProgress = progress;
        }
    }
}