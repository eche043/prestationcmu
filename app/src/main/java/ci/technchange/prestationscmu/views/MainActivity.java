package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.core.dbHelper;
import ci.technchange.prestationscmu.models.Metrique;
import ci.technchange.prestationscmu.receivers.SyncNotificationReceiver;
import ci.technchange.prestationscmu.utils.ActivityTracerApp;
import ci.technchange.prestationscmu.utils.ActivityTracker;
import ci.technchange.prestationscmu.utils.AgentManager;
import ci.technchange.prestationscmu.utils.DataSMSMetriqueManager;
import ci.technchange.prestationscmu.utils.FseServiceDb;
import ci.technchange.prestationscmu.utils.LocalisationManager;
import ci.technchange.prestationscmu.utils.LocationService;
import ci.technchange.prestationscmu.utils.MetriqueApiSender;
import ci.technchange.prestationscmu.utils.MetriqueConnexionApiSender;
import ci.technchange.prestationscmu.utils.MetriqueConnexionServiceDb;
import ci.technchange.prestationscmu.utils.MetriqueServiceDb;
import ci.technchange.prestationscmu.utils.ReferentielService;
import ci.technchange.prestationscmu.utils.RegionCoordUtils;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UploadQueueManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;
import ci.technchange.prestationscmu.utils.synchroDonnees;
import ci.technchange.prestationscmu.core.DownloadService;
import ci.technchange.prestationscmu.utils.synchroDonneesReferentiel;

import android.os.Handler;
import android.os.Looper;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 123;
    private SyncNotificationReceiver syncReceiver;
    private static final String TAG = "Synchronisation";
    private static final String TAG1 = "Famoco ID";
    private View rootView;  // La vue principale de votre activité
    private synchroDonnees synchro;
    private synchroDonneesReferentiel synchroReferentiel;
    private SharedPrefManager sharedPrefManager;
    private TextView tvDatabaseInfo;
    private ActivityTracker activityTracker;
    private ActivityTracerApp activityTracerApp;
    private MetriqueServiceDb metriqueServiceDb;
    private FseServiceDb fseServiceDb;
    private List<Metrique> listeMetrique;
    private String idFamoco,code_ets, nombre_recherche;
    private int nombre_fse_edit, nombre_fse_finalise;
    private UtilsInfosAppareil utilsInfos ;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private RegionCoordUtils regionCoordUtils;
    private RequestQueue requestQueue;

    // Variables pour stocker les informations de l'agent
    private String agentNom = "";
    private String agentPrenom = "";
    private String agentMatricule = "";
    private String agentTelephone = "";
    private String agentCentre = "";
    String nomEtablissement;
    // Déclarez la variable pour le TextView de l'agent
    //private TextView tvAgentInfo;

    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private LocationListener locationListener;

    private double latitude, longitude;
    private LocalisationManager localisationManager;

    private FrameLayout flAgentPhoto;

    private AgentManager agentManager;
    private static final int REQUEST_CODE_PROFILE_EDIT = 1001;



    private ReferentielService referentielService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        rootView = findViewById(android.R.id.content);
        synchro = new synchroDonnees(this);
        synchroReferentiel = new synchroDonneesReferentiel(this);
        sharedPrefManager = new SharedPrefManager(this);
        activityTracker = new ActivityTracker(this);
        activityTracerApp = new ActivityTracerApp(this);
        metriqueServiceDb = MetriqueServiceDb.getInstance(this);
        listeMetrique = metriqueServiceDb.getMetriqueNonSynchro();
        fseServiceDb = FseServiceDb.getInstance(this);
        referentielService = new ReferentielService(this);
        utilsInfos = new UtilsInfosAppareil(this);
        regionCoordUtils = new RegionCoordUtils(this);
        agentManager = AgentManager.getInstance(this);
        // Nouveau TextView pour l'agent
        //tvAgentInfo = findViewById(R.id.tvAgentInfo);
        requestQueue = Volley.newRequestQueue(this);
        flAgentPhoto = findViewById(R.id.flAgentPhoto);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        MetriqueApiSender metriqueApiSender = new MetriqueApiSender(this);
        MetriqueConnexionApiSender metriqueConexion = new MetriqueConnexionApiSender(this);
        MetriqueConnexionServiceDb metriqueConnexionServiceDb = new MetriqueConnexionServiceDb(this);
        flAgentPhoto.setOnClickListener(v -> showAgentMenu());

        localisationManager = new LocalisationManager(this);

        code_ets = sharedPrefManager.getCodeEts();
        nombre_recherche = metriqueServiceDb.countAllMetriques();
        nombre_fse_edit = fseServiceDb.countFseAmbulatoireProgresFalse();
        nombre_fse_finalise = fseServiceDb.countFseAmbulatoireProgresTrue();
        idFamoco = utilsInfos.recupererIdAppareil();


        scheduleStatisticsSending();

        //checkLocationPermissionsAndStartService();

        //updateAgentInfoDisplay();
        setupLocationListener();

        // Demander les permissions de localisation et démarrer les mises à jour de la localisation
        requestLocationUpdates();
        // 2. Définir le callback pour recevoir les coordonnées
        /*localisationManager.setLocationUpdateCallback(new LocalisationManager.LocationUpdateCallback() {

            @Override
            public void onLocationUpdated(double lat, double lng) {
                // Coordonnées reçues avec succès
                Log.d("MainActivity", "Coordonnées reçues: " + lat + ", " + lng);

                latitude = lat;
                longitude = lng;
                // Utiliser les coordonnées selon vos besoins
                //utiliserCoordonnees(latitude, longitude);
            }

            @Override
            public void onLocationError(String error) {
                // Erreur lors de la récupération
                Log.e("MainActivity", "Erreur de localisation: " + error);
                Toast.makeText(MainActivity.this, "Erreur: " + error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPermissionRequired() {
                // Permissions nécessaires
                Log.w("MainActivity", "Permissions de localisation requises");
                //demanderPermissions();
            }
        });

        localisationManager.requestLocationUpdates();*/

        metriqueApiSender.sendUnsyncedMetrics(latitude, longitude);
        // Récupérer les informations de l'agent depuis l'Intent
        Intent intent = getIntent();
        if (intent != null && intent.getStringExtra("AGENT_NOM") != null) {
            agentNom = intent.getStringExtra("AGENT_NOM") != null ? intent.getStringExtra("AGENT_NOM") : "";
            agentPrenom = intent.getStringExtra("AGENT_PRENOM") != null ? intent.getStringExtra("AGENT_PRENOM") : "";
            agentMatricule = intent.getStringExtra("AGENT_MATRICULE") != null ? intent.getStringExtra("AGENT_MATRICULE") : "";
            agentTelephone = intent.getStringExtra("AGENT_TELEPHONE") != null ? intent.getStringExtra("AGENT_TELEPHONE") : "";
            agentCentre = intent.getStringExtra("AGENT_CENTRE") != null ? intent.getStringExtra("AGENT_CENTRE") : "";
            nomEtablissement = fetchEtablissementName();
            // Récupérer la date actuelle en objet Date
            Date currentDate = new Date();

            // Formatter en String avec le format souhaité
            // Format: "2025-05-08" (année-mois-jour)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String formattedDate = dateFormat.format(currentDate);

            SimpleDateFormat timeWithSecondsFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String currentTimeWithSeconds = timeWithSecondsFormat.format(currentDate);

            String regionEts = referentielService.getRegionomByEtablissement(nomEtablissement);
            String idFamoco = utilsInfos.recupererIdAppareil();
            Log.d("Famoco", "ID famoco est : "+idFamoco);
            //int id = RegionUtils.getRegionid(downloadedFileName);
            int id = regionCoordUtils.getIdForRegion(regionEts);
            String idString = Integer.toString(id);

            metriqueConnexionServiceDb.insertMetriqueConnexion(sharedPrefManager.getCodeEts(), agentMatricule, agentNom + " " + agentPrenom,formattedDate,currentTimeWithSeconds,idString,0);
            // Afficher les informations de l'agent dans le log
            Log.d("AGENT_INFO", "Nom: " + agentNom);
            Log.d("AGENT_INFO", "Prénom: " + agentPrenom);
            Log.d("AGENT_INFO", "Matricule: " + agentMatricule);
            Log.d("AGENT_INFO", "Téléphone: " + agentTelephone);
            Log.d("AGENT_INFO", "Centre: " + agentCentre);



            metriqueConexion.sendUnsyncedConnexionMetrics(latitude, longitude,idFamoco);

            // Sauvegarder les informations de l'agent dans SharedPreferences pour les utiliser ailleurs

            sharedPrefManager.setagentName(agentNom, agentPrenom);
            sharedPrefManager.setCodeAgent(agentMatricule);

            agentManager.verifyDatabaseStructure();

            updateAgentDisplay();


            // Vous pouvez également sauvegarder le téléphone et le centre si nécessaire
            // sharedPrefManager.saveAgentPhone(agentTelephone);
            // sharedPrefManager.saveAgentCentre(agentCentre);
        }else{
            System.out.println("in else*******");

            agentMatricule = sharedPrefManager.getCodeAgent();
            Log.d("agentInfo", agentMatricule);
            agentManager.verifyDatabaseStructure();

            updateAgentDisplay();
            //AgentManager.AgentInfo agentInfo = agentManager.getAgentInfo(agentMatricule);
            //updateAgentDisplay();
            //Log.d("agentInfo", agentInfo.toString());

        }


        Log.d(TAG, "Code ETS: " + code_ets);
        Log.d(TAG, "Nombre total de recherches: " + nombre_recherche);
        Log.d(TAG, "Nombre de FSE en édition: " + nombre_fse_edit);
        Log.d(TAG, "Nombre de FSE finalisées: " + nombre_fse_finalise);
        Log.d(TAG, "ID Famoco: " + idFamoco);


        checkAndRequestSmsPermission();
        setupDailySmsAlarm();
        registerBootReceiver();

        updateListFseButtonText();
        UtilsInfosAppareil utilsInfos = new UtilsInfosAppareil(this);
        utilsInfos.obtenirInformationsSysteme(this);
        String deviceId = utilsInfos.recupererIdAppareil();

        Log.d(TAG1, "le id famoco est : "+deviceId );


        // Récupérer et afficher le nom du fichier téléchargé
        tvDatabaseInfo = findViewById(R.id.tvDatabaseInfo);
        SharedPrefManager sharedPrefManager = new SharedPrefManager(this);
        String downloadedFileName = sharedPrefManager.getDownloadedFileName();

        // Vérifier si le fichier existe
        File dbFile = new File(getFilesDir(), "encryptedbd/newcnambd1.db");
        if (dbFile.exists()) {



            if (downloadedFileName != null && !downloadedFileName.isEmpty() && !"newcnambd1.db".equals(downloadedFileName)) {

                // Convertir le nom de fichier en nom de région
                String regionName = RegionUtils.getRegionNameFromFileName(downloadedFileName);
                //sharedPrefManager.saveRegionName(regionName);


                //Toast.makeText(this, "name2:"+regionName, Toast.LENGTH_LONG).show();

                tvDatabaseInfo.setText("Region: " + regionName);
                tvDatabaseInfo.setTextColor(getResources().getColor(android.R.color.black));
            } else {

                tvDatabaseInfo.setText("Refaire la synchronisation");
                tvDatabaseInfo.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        } else {
            tvDatabaseInfo.setText("Aucune base trouvée");
            tvDatabaseInfo.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }


        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            Log.d("APP_LIST", "Package: " + packageInfo.packageName + " - Nom: " + packageManager.getApplicationLabel(packageInfo));
        }


        findViewById(R.id.btnVersAppEcard).setOnClickListener(view -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("ci.cnam.ecard_reader");
            if (launchIntent != null) {
                activityTracerApp.trackActivityApp("vers_autre_app");
                activityTracerApp.enregistrerDateDebutApp();
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, "Application de ecard non-installée", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnVersAppEmpreintes).setOnClickListener(view -> {
            Intent intentEmpreintes = new Intent();
            intentEmpreintes.setComponent(new ComponentName("com.dermalog.ltoreader", "com.dermalog.barcodedemo.LoginActivity"));
            try {
                activityTracerApp.trackActivityApp("vers_autre_app");
                activityTracerApp.enregistrerDateDebutApp();
                startActivity(intentEmpreintes);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Aucune application disponible pour gérer cette action", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnVersAppNewDroits).setOnClickListener(view -> {
            Intent intentNewDroits = new Intent();
            intentNewDroits.setComponent(new ComponentName("com.dermalog.ltoreader", "com.dermalog.barcodedemo.MainActivity"));
            try {
                activityTracerApp.trackActivityApp("vers_autre_app");
                activityTracerApp.enregistrerDateDebutApp();
                startActivity(intentNewDroits);
            } catch (java.lang.SecurityException x) {
                intentNewDroits = new Intent();
                intentNewDroits.setComponent(new ComponentName("com.dermalog.ltoreader", "com.dermalog.barcodedemo.LoginActivity"));
                try {
                    activityTracerApp.trackActivityApp("vers_autre_app");
                    activityTracerApp.enregistrerDateDebutApp();
                    startActivity(intentNewDroits);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Aucune application disponible pour gérer cette action", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Aucune application disponible pour gérer cette action", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnVersAppOldDroits).setOnClickListener(view -> {
            Intent i4 = new Intent();
            i4.setPackage("com.zetes.android.biometrics");
            if (i4.resolveActivity(getPackageManager()) != null) {
                activityTracerApp.trackActivityApp("vers_autre_app");
                activityTracerApp.enregistrerDateDebutApp();
                startActivity(i4);
            } else {
                Toast.makeText(this, "Aucune application disponible pour gérer cette action", Toast.LENGTH_SHORT).show();
            }
        });


        findViewById(R.id.btnVersActRechEnrole).setOnClickListener(v -> {
            activityTracker.trackActivity("rech_bio");
            activityTracker.enregistrerDateDebut();
            String startDate = activityTracker.getDateDebut();
            Log.d("ActivityTracker", "Début recherche bio: " + startDate);
            Intent intentRechEnrole = new Intent(MainActivity.this, SearchEnroleActivity.class);
            startActivity(intentRechEnrole);
        });

        findViewById(R.id.btnVersActScanqrcode).setOnClickListener(v -> {
            activityTracker.trackActivity("scan_qr");
            activityTracker.enregistrerDateDebut();
            String startDate = activityTracker.getDateDebut();
            Log.d("ActivityTracker", "Début scan QR: " + startDate);
            Intent intentScanQR = new Intent(MainActivity.this, ScanqrCodeActivity.class);
            startActivity(intentScanQR);
        });

        findViewById(R.id.btnVersActNumsecu).setOnClickListener(v -> {
            activityTracker.trackActivity("saisir_num_secu");
            activityTracker.enregistrerDateDebut();
            String startDate = activityTracker.getDateDebut();
            Log.d("ActivityTracker", "Début saisie num secu: " + startDate);
            Intent intentNumSecu = new Intent(MainActivity.this, NumSecuSaisieActivity.class);
            startActivity(intentNumSecu);
        });
        findViewById(R.id.btnVersActScanFSEpapier).setOnClickListener(v ->{
            Intent intentScanFSE = new Intent(MainActivity.this, SelectCodeActeAffectionActivity.class);
            startActivity(intentScanFSE);
        });





        findViewById(R.id.btnVerslistfse).setOnClickListener(v -> {
            Intent intentListeFSE = new Intent(MainActivity.this, ListeFseActivity.class);
            startActivity(intentListeFSE);
        });



        findViewById(R.id.btnSynchro).setOnClickListener(v -> {
            if (!isNetworkAvailable()) {
                showNoInternetDialog();
                return;
            }

            if (sharedPrefManager == null) {
                Toast.makeText(MainActivity.this, "Erreur: Gestionnaire de préférences non initialisé", Toast.LENGTH_SHORT).show();
                return;
            }


            File externalDir = new File(getFilesDir(), "encryptedbd");
            File file = new File(externalDir, "newcnambd1.db");

            if (!file.exists() || "newcnambd1.db".equals(downloadedFileName) || "".equals(downloadedFileName) || downloadedFileName == null) {
                Log.d("TAGGGGG", "onCreate: ******");
                System.out.println(downloadedFileName);
                System.out.println(file.exists());
                String regionEts = sharedPrefManager.getRegionName();

                int id = regionCoordUtils.getIdForRegion(regionEts);
                String idString = Integer.toString(id);
                downloadInitialDatabase(idString);
            } else {
                startDatabaseUpdate();
            }
        });

    }

    private void updateAgentDisplay() {
        ImageView ivAgentPhoto = findViewById(R.id.ivAgentPhoto);
        View vStatusIndicator = findViewById(R.id.vStatusIndicator);

        if (ivAgentPhoto == null) {
            Log.e(TAG, "ImageView ivAgentPhoto non trouvée");
            return;
        }

        // Utiliser le gestionnaire de photos pour appliquer la photo
        agentManager.applyCircularResizedAgentPhotoToImageView(
                ivAgentPhoto,
                agentMatricule,
                220, 220, // Taille max 200x200
                true // Utiliser l'icône par défaut si pas de photo
        );

        // Mettre à jour l'indicateur de statut
        //updateStatusIndicator();
    }


    private void scheduleStatisticsSending() {
        if (isNetworkAvailable()) {

            sendStatisticsToApi();
        } else {

            setupDailySmsAlarm();
            Toast.makeText(this, "Les statistiques seront envoyées à 18h", Toast.LENGTH_SHORT).show();
        }
    }
    private void showAgentMenu() {
        // Créer le PopupWindow
        View popupView = LayoutInflater.from(this).inflate(R.layout.menu_agent_popup, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        // Permettre de fermer en touchant à l'extérieur
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        // Listeners pour les options du menu
        LinearLayout menuItemProfile = popupView.findViewById(R.id.menuItemProfile);
        LinearLayout menuItemLogout = popupView.findViewById(R.id.menuItemLogout);

        menuItemProfile.setOnClickListener(v -> {
            popupWindow.dismiss();
            openProfileEditActivity(); // Nouvelle méthode
            Toast.makeText(this, "Ouvrir profil", Toast.LENGTH_SHORT).show();
        });

        menuItemLogout.setOnClickListener(v -> {
            popupWindow.dismiss();
            showLogoutConfirmation();
        });

        // Afficher le popup à côté de la photo
        popupWindow.showAsDropDown(flAgentPhoto, -50, 0);
    }

    /**
     * Ouvre l'activité de modification du profil
     */
    private void openProfileEditActivity() {
        Intent intent = new Intent(this, ProfileEditActivity.class);

        // Passer les données actuelles de l'agent
        intent.putExtra("AGENT_NOM", agentNom);
        intent.putExtra("AGENT_PRENOM", agentPrenom);
        intent.putExtra("AGENT_MATRICULE", agentMatricule);
        intent.putExtra("AGENT_TELEPHONE", agentTelephone);
        intent.putExtra("AGENT_CENTRE", agentCentre);


        startActivityForResult(intent, REQUEST_CODE_PROFILE_EDIT);
    }

    // Méthode pour confirmer la déconnexion
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Effacer les données de l'agent
                    agentNom = "";
                    agentPrenom = "";
                    agentMatricule = "";
                    agentTelephone = "";
                    agentCentre = "";

                    Intent intent = new Intent(MainActivity.this, ConnexionActivity.class);

                    // Flags pour fermer toutes les activités et créer une nouvelle pile
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    // Optionnel : passer un indicateur que c'est une déconnexion
                    intent.putExtra("LOGOUT", true);
                    intent.putExtra("MESSAGE", "Vous avez été déconnecté avec succès");

                    // 5. Démarrer ConnexionActivity
                    startActivity(intent);

                    // 6. Terminer cette activité
                    finish();
                    // TODO: Nettoyer SharedPreferences si nécessaire
                    // sharedPrefManager.clearAgentData();

                    //updateAgentDisplay();
                    Toast.makeText(this, "Déconnecté", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    /*private void updateAgentInfoDisplay() {
        String agentInfo = sharedPrefManager.getCodeAndNomAgent();
        if (agentInfo != null && !agentInfo.isEmpty()) {
            tvAgentInfo.setText("Agent: " + agentInfo);
        } else {
            tvAgentInfo.setText("Agent: Non connecté");
        }
    }*/

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Log.d(TAG, "Service de localisation démarré");
    }

    // Appelez cette méthode dans onCreate() après avoir vérifié les permissions
    private void checkLocationPermissionsAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                // Les permissions seront demandées et le service démarré dans onRequestPermissionsResult
                requestLocationUpdates(); // Votre méthode existante
            }
        } else {
            startLocationService();
        }

        // Vérifier la permission SMS

    }

    private void setupLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Log.d(TAG, "Facade - Lat: " + latitude + ", Long: " + longitude);

                locationManager.removeUpdates(this);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
    }

    // Nouvelle méthode pour demander les mises à jour de localisation
    private void requestLocationUpdates() {
        // Vérifier les permissions pour Android 6.0 et versions ultérieures
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Demander les permissions si elles ne sont pas accordées
                requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE
                );
                return;
            }
        }

        // Vérifier si le GPS est activé
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Pour Android 9 (Pie) et versions antérieures
            // ATTENTION : Cette méthode ne fonctionne plus sur Android 10+ en raison des restrictions de sécurité
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                try {
                    // Activer le GPS via les paramètres système
                    Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.LOCATION_MODE,
                            Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);

                    Log.d(TAG, "Fournisseurs de localisation activés avec succès");
                } catch (SecurityException e) {
                    Log.e(TAG, "Impossible d'activer les fournisseurs de localisation", e);
                }
            } else {
                // Pour Android 10+, l'activation directe n'est plus possible
                // Il faut rediriger l'utilisateur vers les paramètres ou utiliser des coordonnées par défaut
                Log.w(TAG, "L'activation automatique des fournisseurs n'est pas possible sur Android 10+");

                showEnableLocationDialog();
            }
            // Afficher une alerte pour demander à l'utilisateur d'activer le GPS

            return;
        }

        // Essayer d'obtenir la dernière position connue
        Location lastKnownLocation = null;

        // D'abord essayer avec le GPS (plus précis)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        // Si pas de localisation GPS, essayer avec le réseau
        if (lastKnownLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        // Utiliser la dernière position connue si disponible
        if (lastKnownLocation != null) {
            latitude = lastKnownLocation.getLatitude();
            longitude = lastKnownLocation.getLongitude();
            Log.d(TAG, "Dernière position connue - Lat: " + latitude + ", Long: " + longitude);
        }

        // Demander des mises à jour régulières de la position
        // Paramètres: fournisseur, temps minimal entre les mises à jour (ms), distance minimale (m), listener
        try {
            // Essayer d'abord avec le GPS
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,  // 5 secondes
                        10,    // 10 mètres
                        locationListener
                );
                Log.d(TAG, "Demande de mises à jour GPS démarrée");
            }

            // Ajouter aussi les mises à jour réseau pour plus de fiabilité
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,  // 5 secondes
                        10,    // 10 mètres
                        locationListener
                );
                Log.d(TAG, "Demande de mises à jour réseau démarrée");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Erreur de sécurité lors de la demande de mises à jour de localisation", e);
        }
    }



    // Méthode pour afficher une boîte de dialogue pour activer le GPS
    private void showEnableLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Localisation désactivée")
                .setMessage("La localisation GPS est désactivée. Voulez-vous l'activer maintenant?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Rediriger vers les paramètres de localisation
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Non", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(this, "La localisation est nécessaire pour cette fonctionnalité", Toast.LENGTH_LONG).show();
                })
                .create()
                .show();
    }
    private String fetchEtablissementName() {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            // Initialisation de la base de données
            if (GlobalClass.getInstance().cnxDbReferentiel == null) {
                GlobalClass.getInstance().initDatabase("referentiel");
            }
            db = GlobalClass.getInstance().cnxDbReferentiel;

            if (db == null) {
                Log.e("fetchEtablissementName", "La connexion à la base de données est null");
                Toast.makeText(this, "Erreur de connexion à la base de données", Toast.LENGTH_SHORT).show();
                return null;
            }

            // Récupération du code ETS
            String code_ets = sharedPrefManager.getCodeEts();
            if (code_ets == null || code_ets.isEmpty()) {
                Log.e("fetchEtablissementName", "Aucun code ETS trouvé dans les préférences");
                Toast.makeText(this, "Aucun établissement sélectionné", Toast.LENGTH_SHORT).show();
                return null;
            }

            // Requête paramétrée (plus sécurisée contre les injections SQL)
            String query = "SELECT etablissement FROM etablissements WHERE code_ets = ?";
            cursor = db.rawQuery(query, new String[]{code_ets});

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("etablissement");
                if (columnIndex != -1) {
                    @SuppressLint("Range") String etablissement = cursor.getString(columnIndex);

                    return etablissement;

                } else {
                    Log.e("fetchEtablissementName", "Colonne 'etablissement' non trouvée");
                    Toast.makeText(this, "Erreur: Structure de base de données invalide", Toast.LENGTH_SHORT).show();
                    return null;
                }
            } else {
                Log.e("fetchEtablissementName", "Aucun résultat pour code_ets: " + code_ets);
                Toast.makeText(this, "Aucun établissement trouvé pour ce code", Toast.LENGTH_SHORT).show();
                return null;
            }
        } catch (Exception e) {
            Log.e("fetchEtablissementName", "Erreur: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de la récupération", Toast.LENGTH_SHORT).show();
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Ne pas fermer db car elle est gérée par GlobalClass
        }
    }


    private void downloadInitialDatabase(String idRegion) {
        String fileUrl = "http://57.128.30.4:8089/bdregion.php?latitude=" +
                sharedPrefManager.getLatitude() + "&longitude=" + sharedPrefManager.getLongitude();
        //String fileUrl = "http://57.128.30.4:8089/bdregion.php?latitude=9.410786&longitude=-7.513318";
        //String fileUrl ="http://57.128.30.4:8089/bdall.php";
        Log.d("Lien ", fileUrl);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Date nowHeure = new Date();

        // Afficher l'état initial
        tvDatabaseInfo.setText("Préparation du téléchargement...");
        Log.d(TAG, "IDFAMOCOMAIN"+idFamoco);
        Log.d(TAG, "IDREGIONMAIN"+idRegion);
        Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.putExtra("fileUrl", fileUrl);
        serviceIntent.putExtra("id_region", idRegion);
        serviceIntent.putExtra("id_famoco", idFamoco);
        serviceIntent.putExtra("code_ets", sharedPrefManager.getCodeEts()); // Utiliser le code ETS enregistré
        serviceIntent.putExtra("code_agac", sharedPrefManager.getCodeAgent()); // À remplacer si vous avez ce code stocké ailleurs
        serviceIntent.putExtra("date_remontee", dateFormat.format(now)); // Vous pouvez aussi générer la date actuelle
        serviceIntent.putExtra("heure_debut", timeFormat.format(nowHeure));
        ContextCompat.startForegroundService(this, serviceIntent);
    }


    private void updateListFseButtonText() {
        Button btnListFse = findViewById(R.id.btnVerslistfse);
        FseServiceDb fseServiceDb = FseServiceDb.getInstance(this);
        Map<String, Integer> counts = fseServiceDb.countFseAmbulatoireByStatus();

        int enRetard = counts.get("en_retard");
        int enAttente = counts.get("en_attente");

        Log.d("En retard", "nombre :"+ enRetard);
        Log.d("En attente", "nombre : "+ enAttente);


        btnListFse.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        if (enRetard > 0 && enAttente > 0) {
            btnListFse.setText(enRetard + " FSE en retard | " + enAttente + " en attente");
            btnListFse.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
        else if (enRetard > 0) {
            btnListFse.setText(enRetard + " FSE en retard de préfinalisation");
            btnListFse.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
        else if (enAttente > 0) {
            btnListFse.setText(enAttente + " FSE en attente de finalisation");
            btnListFse.setTextColor(ContextCompat.getColor(this, R.color.orange));
        }
        else {
            btnListFse.setText("Liste FSE");

        }
    }
    private void startDatabaseUpdate() {
        findViewById(R.id.btnSynchro).setEnabled(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            final AtomicReference<String> errorRef = new AtomicReference<>("");

            try {
                FseServiceDb fseServiceDb = FseServiceDb.getInstance(this);
                int currentVersion = fseServiceDb.getVersionBD();
                Log.d("Version bd", "la version est à " + currentVersion);
                String downloadedFileName = sharedPrefManager.getDownloadedFileName();
                /*String apiUpdateDb = "http://51.38.224.233:8080/api/v1/synchronisation?longitude=" +
                        sharedPrefManager.getLongitude() + "&latitude=" + sharedPrefManager.getLatitude() + "&version=" + currentVersion;**/
                int id = RegionUtils.getRegionid(downloadedFileName);
                String apiUpdateDb = "http://57.128.30.4:8090/api/v1/synchronisation/"+id+"/"+currentVersion;

                Log.d("Lien synchro", apiUpdateDb);

                String nomFichier = "enroles_sql_files.zip";

                final CountDownLatch downloadLatch = new CountDownLatch(1);
                final AtomicBoolean downloadSuccess = new AtomicBoolean(false);

                handler.post(() -> {
                    synchro.telechargerEtDezipperDossierZip(apiUpdateDb, nomFichier, new synchroDonnees.DownloadCallback() {
                        @Override
                        public void onDownloadComplete(boolean success, String downloadError) {
                            downloadSuccess.set(success);
                            if (!success) {
                                errorRef.set(downloadError);
                            }
                            downloadLatch.countDown();
                        }
                    });
                });

                boolean waitSuccess = downloadLatch.await(60, TimeUnit.SECONDS);

                if (!waitSuccess) {
                    Log.e(TAG, "Timeout lors de l'attente du téléchargement");
                    errorRef.set("Timeout lors du téléchargement");
                }

                if (downloadSuccess.get() && waitSuccess) {
                    File zipFile = new File(getFilesDir(), "enroles_sql_files.zip");
                    if (zipFile.exists()) {
                        try {
                            synchro.unzipDossierFile(zipFile);
                            File outputDir = new File(getFilesDir(), "unzipped");
                            int nombreDossier = synchro.getSavedFolderCount();
                            int nombreVersion = nombreDossier + currentVersion;
                            boolean updateSuccess = fseServiceDb.updateVersionBD(nombreVersion);
                            if (updateSuccess) {
                                Log.d("Version Update", "Mise à jour de la version réussie");
                            } else {
                                Log.e("Version Update", "Échec de la mise à jour de la version");
                            }
                            synchro.cleanup(zipFile, outputDir);
                            success = true;
                        } catch (IOException e) {
                            Log.e(TAG, "Erreur lors du dézippage du fichier", e);
                            errorRef.set("Erreur lors du dézippage du fichier: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Le fichier enroles_sql_files.zip n'existe pas");
                        errorRef.set("Le fichier enroles_sql_files.zip n'existe pas");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur durant la synchronisation", e);
                errorRef.set("Erreur durant la synchronisation: " + e.getMessage());
            }

            final boolean finalSuccess = success;
            final String finalErrorMessage = errorRef.get();

            handler.post(() -> {
                findViewById(R.id.btnSynchro).setEnabled(true);

                if (finalSuccess) {
                    Toast.makeText(MainActivity.this, "Synchronisation terminée avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();
                }

                startDatabaseUpdateReferentiel();
            });
        });
    }


    private void startDatabaseUpdateReferentiel() {
        findViewById(R.id.btnSynchro).setEnabled(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            final AtomicReference<String> errorRef = new AtomicReference<>("");

            try {
                FseServiceDb fseServiceDb = FseServiceDb.getInstance(this);
                int currentVersion = fseServiceDb.getVersionBDReferentiel();
                Log.d("Version bd", "la version est à " + currentVersion);
                String downloadedFileName = sharedPrefManager.getDownloadedFileName();
                /*String apiUpdateDb = "http://51.38.224.233:8080/api/v1/synchronisation?longitude=" +
                        sharedPrefManager.getLongitude() + "&latitude=" + sharedPrefManager.getLatitude() + "&version=" + currentVersion;**/
                int id = RegionUtils.getRegionid(downloadedFileName);
                String apiUpdateDbReferentiel = "http://57.128.30.4:8090/api/v1/synchronisationReferentiel/"+currentVersion;

                Log.d("Lien synchro", apiUpdateDbReferentiel);



                String nomFichierReferentiel = "etablissement_referentiel.zip";

                final CountDownLatch downloadLatch = new CountDownLatch(1);
                final AtomicBoolean downloadSuccess = new AtomicBoolean(false);

                handler.post(() -> {
                    synchroReferentiel.telechargerEtDezipperDossierZipReferentiel(apiUpdateDbReferentiel, nomFichierReferentiel, new synchroDonneesReferentiel.DownloadCallbackReferentiel(){
                        @Override
                        public void onDownloadCompleteReferentiel(boolean success, String errorMessage) {
                            downloadSuccess.set(success);
                            if (!success) {
                                errorRef.set(errorMessage);
                            }
                            downloadLatch.countDown();
                        }

                    });
                });

                boolean waitSuccess = downloadLatch.await(60, TimeUnit.SECONDS);

                if (!waitSuccess) {
                    Log.e(TAG, "Timeout lors de l'attente du téléchargement");
                    errorRef.set("Timeout lors du téléchargement");
                }

                if (downloadSuccess.get() && waitSuccess) {
                    File zipFile = new File(getFilesDir(), "etablissement_referentiel.zip");
                    if (zipFile.exists()) {
                        try {
                            synchroReferentiel.unzipDossierFileReferentiel(zipFile);
                            File outputDir = new File(getFilesDir(), "unzipped");
                            int nombreDossier = synchroReferentiel.getSavedFolderCountReferentiel();
                            int nombreVersion = nombreDossier + currentVersion;
                            boolean updateSuccess = fseServiceDb.updateVersionBDReferentiel(nombreVersion);
                            if (updateSuccess) {
                                Log.d("Version Update", "Mise à jour de la version réussie");
                            } else {
                                Log.e("Version Update", "Échec de la mise à jour de la version");
                            }
                            synchroReferentiel.cleanupReferentiel(zipFile, outputDir);
                            success = true;
                        } catch (IOException e) {
                            Log.e(TAG, "Erreur lors du dézippage du fichier", e);
                            errorRef.set("Erreur lors du dézippage du fichier: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Le fichier etablissement_referentiel.zip n'existe pas");
                        errorRef.set("Le fichier enroles_sql_files.zip n'existe pas");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur durant la synchronisation", e);
                errorRef.set("Erreur durant la synchronisation: " + e.getMessage());
            }

            final boolean finalSuccess = success;
            final String finalErrorMessage = errorRef.get();

            handler.post(() -> {
                findViewById(R.id.btnSynchro).setEnabled(true);

                if (finalSuccess) {
                    Toast.makeText(MainActivity.this, "Synchronisation terminée avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /*private void handleSmsSending(DataSMSMetriqueManager smsManager) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                code_ets = sharedPrefManager.getCodeEts();
                nombre_recherche = metriqueServiceDb.countAllMetriques();
                nombre_fse_edit = fseServiceDb.countFseAmbulatoireProgresFalse();
                nombre_fse_finalise = fseServiceDb.countFseAmbulatoireProgresTrue();
                String strNombreFseEdit = String.valueOf(nombre_fse_edit);
                String strNombreFseFinalise = String.valueOf(nombre_fse_finalise);
                idFamoco = utilsInfos.recupererIdAppareil();
                String lettreCle = "m";
                smsManager.sendDataMetriqueViaSMS(lettreCle,code_ets,idFamoco,nombre_recherche,strNombreFseEdit,strNombreFseFinalise);
            } catch (Exception e) {
                Toast.makeText(this, "Échec envoi SMS", Toast.LENGTH_SHORT).show();

            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }**/

    private boolean saveMetriqueApp() {
        try {
            Metrique metrique = new Metrique();
            String downloadedFileName = sharedPrefManager.getDownloadedFileName();
            String idFamoco = utilsInfos.recupererIdAppareil();
            Log.d("Famoco", "ID famoco est : " + idFamoco);
            int id = RegionUtils.getRegionid(downloadedFileName);


            String activite = activityTracerApp.getLastActivityApp();
            String date_debut = activityTracerApp.getDateDebutApp();
            String date_fin = activityTracerApp.getDateFinApp();

            metrique.setActivite(activite);
            metrique.setDateDebut(date_debut);
            metrique.setDateFin(date_fin);
            metrique.setIdRegion(id);
            metrique.setIdFamoco(idFamoco);
            metrique.setStatusSynchro(0);

            Log.d("Metrique", "Métrique app externe: " + metrique.toString());

            long result = metriqueServiceDb.insertMetrique(metrique);

            if (result != -1) {
                Log.d("metrique_info", "La métrique pour l'application externe " + activite + " est enregistrée");
                return true;
            } else {
                Log.d("metrique_info", "La métrique pour l'application externe " + activite + " n'a pas pu être enregistrée");
                return false;
            }
        } catch (Exception e) {
            Log.e("metrique_info", "Erreur lors de l'enregistrement de la métrique app externe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    private void showNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pas de connexion Internet");
        builder.setMessage("Vous devez être connecté à Internet pour synchroniser les données.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadService.PROGRESS_UPDATE_ACTION.equals(intent.getAction())) {
                // Récupérer la progression et l'afficher
                if (intent.hasExtra(DownloadService.PROGRESS_EXTRA)) {
                    int progress = intent.getIntExtra(DownloadService.PROGRESS_EXTRA, 0);
                    runOnUiThread(() -> {
                        tvDatabaseInfo.setText("Téléchargement en cours : " + progress + "%");
                    });
                }

                // Traiter la fin du téléchargement
                if (intent.hasExtra("result")) {
                    String result = intent.getStringExtra("result");
                    if (intent.hasExtra("originalFileName")) {
                        String originalFileName = intent.getStringExtra("originalFileName");
                        sharedPrefManager.saveDownloadedFileName(originalFileName);
                        System.out.println("Nom de fichier sauvegardé dans les préférences: " + originalFileName);

                        if (result != null && originalFileName != null) {
                            if (result.startsWith("Fichier téléchargé avec succès")) {
                                Log.d("DownloadReceiver", "Téléchargement réussi, affichage du modal");

                                // Mettre à jour tvDatabaseInfo sur le thread principal
                                runOnUiThread(() -> {
                                    if ("newcnambd1.db".equals(originalFileName)) {
                                        tvDatabaseInfo.setText("Refaire la synchronisation");
                                    } else {
                                        String regionName = RegionUtils.getRegionNameFromFileName(originalFileName);
                                        tvDatabaseInfo.setText("Region: " + regionName);
                                        tvDatabaseInfo.setTextColor(getResources().getColor(android.R.color.black));
                                    }
                                });

                                showDownloadCompleteDialog();
                            } else {
                                runOnUiThread(() -> {
                                    tvDatabaseInfo.setText("Erreur lors du téléchargement");
                                    tvDatabaseInfo.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                });
                                Toast.makeText(MainActivity.this, "Erreur lors du téléchargement", Toast.LENGTH_LONG).show();


                            }
                            startDatabaseUpdateReferentiel();
                        } else {
                            Log.e("DownloadReceiver", "Erreur : result ou originalFileName est null");
                            Toast.makeText(MainActivity.this, "Erreur lors du téléchargement : données manquantes", Toast.LENGTH_LONG).show();
                            startDatabaseUpdateReferentiel();
                        }
                    }else{
                        startDatabaseUpdateReferentiel();
                    }
                }
            }
        }
    };







    private void showDownloadCompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Téléchargement terminé");
        builder.setMessage("La base de données a été téléchargée avec succès.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (activityTracerApp.isAppStarted()) {
            String dateFin = activityTracerApp.enregistrerDateFinApp();
            String dateDebut = activityTracerApp.getDateDebutApp();

            if (saveMetriqueApp()) {
                Log.d("MainActivity", "Métrique d'utilisation d'app externe enregistrée avec succès");
            } else {
                Log.e("MainActivity", "Échec de l'enregistrement de la métrique d'utilisation d'app externe");
            }
            Log.d("MainActivity", "App externe fermée - Durée d'utilisation: du " + dateDebut + " au " + dateFin);
        }

        localisationManager.requestLocationUpdates();
        requestLocationUpdates();

        syncReceiver = new SyncNotificationReceiver(rootView);
        MetriqueApiSender metriqueApiSender = new MetriqueApiSender(this);
        metriqueApiSender.sendUnsyncedMetrics(latitude, longitude);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UploadQueueManager.ACTION_SYNC_STARTED);
        filter.addAction(UploadQueueManager.ACTION_SYNC_COMPLETED);
        registerReceiver(syncReceiver, filter);

        if (isPendingStatisticsAlarmSet() && isNetworkAvailable()) {
            cancelPendingStatisticsAlarm();
            sendStatisticsToApi();
        }

        if (!UploadQueueManager.getInstance(this).isQueueEmpty()) {
            UploadQueueManager.getInstance(this).startRetryScan();
        }
        if(countUnsynchronizedAgents() > 0){

            UploadQueueManager.getInstance(this).sendAgentByApi(idFamoco);
        }
        UploadQueueManager.getInstance(this).startRetryScan();
        IntentFilter filters = new IntentFilter(DownloadService.PROGRESS_UPDATE_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadReceiver, filters);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadReceiver);
        if (locationManager != null && locationListener != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(locationListener);
            }
        }
        if (syncReceiver != null) {
            unregisterReceiver(syncReceiver);
            syncReceiver = null;
        }
        localisationManager.stopLocationUpdates();
    }

    private boolean isPendingStatisticsAlarmSet() {
        Intent intent = new Intent(this, StatisticsSenderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        return pendingIntent != null;
    }

    private void cancelPendingStatisticsAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, StatisticsSenderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
    private int countUnsynchronizedAgents() {
        int count = 0;
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();

        try {
            // Vérifier d'abord si la colonne is_synchronized existe
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

            // Si la colonne existe, compter les agents non synchronisés
            if (hasSyncColumn) {
                // Compter les agents où is_synchronized est NULL ou 0
                String query = "SELECT COUNT(*) FROM agents_inscription WHERE is_synchronized IS NULL OR is_synchronized = 0 OR is_synchronized = 2";
                Cursor cursor = db.rawQuery(query, null);

                if (cursor != null && cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                    cursor.close();
                }
            } else {
                // Si la colonne n'existe pas, on considère que tous les agents sont non synchronisés
                String query = "SELECT COUNT(*) FROM agents_inscription";
                Cursor cursor = db.rawQuery(query, null);

                if (cursor != null && cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                    cursor.close();
                }
            }

            Log.d(TAG, "Nombre d'agents non synchronisés : " + count);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du comptage des agents non synchronisés", e);
        } finally {
            db.close();
        }
        Log.d(TAG, "Counttttttttttt"+ count);
        return count;
    }

    private void setupDailySmsAlarm() {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, DailySmsReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);


        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }

        Log.d(TAG, "Alarme configurée pour 18h00 quotidiennement");
    }

    // Vérification des permissions SMS
    private void checkAndRequestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }


    private boolean sendStatisticsToApi() {
        try {

            String STATISTICS_API_URL = "http://57.128.30.4:8090/api/v1/SaveStatistiquesUtilisationApp";


            JSONObject jsonBody = new JSONObject();


            String code_ets = sharedPrefManager.getCodeEts();
            String idFamoco = utilsInfos.recupererIdAppareil();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());

            String nombre_recherche = metriqueServiceDb.countActivitesSpecifiques();
            int nombre_fse_edit = metriqueServiceDb.countActiviteFseEdit();
            int nombre_fse_finalise = fseServiceDb.countFseAmbulatoireProgresTrue();

            // Remplir les champs requis
            jsonBody.put("code_ets", code_ets != null ? code_ets : "");
            jsonBody.put("id_famoco", idFamoco != null ? idFamoco : "");
            jsonBody.put("nbr_recherche", nombre_recherche);
            jsonBody.put("nombre_fse_edite", nombre_fse_edit);
            jsonBody.put("nbr_fse_finalise", nombre_fse_finalise);
            jsonBody.put("date_remontee", currentDate);

            // Log pour déboguer
            Log.d("STATS_API_REQUEST", "Envoi statistiques: " + jsonBody.toString());

            // Création de la requête
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, STATISTICS_API_URL, jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("STATS_API_RESPONSE", "Réponse statistiques: " + response.toString());

                            try {
                                // Vérifier si la réponse contient un message d'erreur
                                boolean hasError = false;
                                String errorMessage = "";

                                if (response.has("message")) {
                                    String message = response.getString("message");
                                    if ("error".equals(message)) {
                                        hasError = true;
                                        if (response.has("data")) {
                                            errorMessage = response.getString("data");
                                        }
                                    }
                                }

                                if (hasError) {
                                    Log.e("STATS_API_ERROR", "Erreur API statistiques: " + errorMessage);
                                    Toast.makeText(MainActivity.this,
                                            "Erreur envoi statistiques: " + errorMessage, Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.d("STATS_API_RESPONSE", "Statistiques envoyées avec succès: " + response.toString());
                                    Toast.makeText(MainActivity.this,
                                            "Statistiques envoyées avec succès", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e("STATS_API_ERROR", "Erreur traitement réponse statistiques: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("STATS_API_ERROR", "Erreur envoi statistiques: " + error.toString());
                            Toast.makeText(MainActivity.this,
                                    "Erreur lors de l'envoi des statistiques: " +
                                            (error.getMessage() != null ? error.getMessage() : "Erreur inconnue"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

            // Ajouter la requête à la file d'attente
            requestQueue.add(jsonObjectRequest);
            return true;

        } catch (Exception e) {
            Log.e("STATS_API_ERROR", "Exception envoi statistiques: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de la préparation des statistiques: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public static class DailySmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                UtilsInfosAppareil utilsInfos = new UtilsInfosAppareil(context);
                MetriqueServiceDb metriqueServiceDb = MetriqueServiceDb.getInstance(context);
                FseServiceDb fseServiceDb = FseServiceDb.getInstance(context);
                SharedPrefManager sharedPrefManager = new SharedPrefManager(context);

                String code_ets = sharedPrefManager.getCodeEts();
                String nombre_recherche = metriqueServiceDb.countActivitesSpecifiques();
                int nombre_fse_edit = metriqueServiceDb.countActiviteFseEdit();
                int nombre_fse_finalise = fseServiceDb.countFseAmbulatoireProgresTrue();
                int nombre_fse_non_finalise = fseServiceDb.countFseAmbulatoireProgresFalse();
                String idFamoco = utilsInfos.recupererIdAppareil();
                String dateRapport = String.valueOf(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()));
                String code_agac = sharedPrefManager.getCodeAgent();

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                        == PackageManager.PERMISSION_GRANTED) {
                    new DataSMSMetriqueManager(context).sendDataMetriqueViaSMS(
                            "m",
                            code_ets,
                            idFamoco,
                            nombre_recherche,
                            String.valueOf(nombre_fse_edit),
                            String.valueOf(nombre_fse_finalise),
                            dateRapport,
                            code_agac,
                            String.valueOf(nombre_fse_non_finalise)
                    );
                    Log.d(TAG, "SMS envoyé avec succès à " + new java.util.Date());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'envoi du SMS", e);
            }
        }
    }
    public static class StatisticsSenderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Vérifier à nouveau la connexion
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                // Si connecté, envoyer les statistiques
                MainActivity activity = new MainActivity();
                activity.sendStatisticsToApi();
            } else {

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent newIntent = new Intent(context, StatisticsSenderReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        newIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, 18);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.add(Calendar.DAY_OF_YEAR, 1); // Demain à la même heure

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }

                Log.d(TAG, "Pas de connexion, reprogrammation des statistiques pour demain 18h");
            }
        }
    }

    /*public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (isConnected) {

                MainActivity activity = new MainActivity();
                activity.sendStatisticsToApi();
            } else {

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            }


            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent alarmIntent = new Intent(context, DailySmsReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, 18);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                    );
                }

                Log.d(TAG, "Alarme reprogrammée après reboot");
            }
        }
    }**/

    private void registerBootReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        registerReceiver(new StatisticsSenderReceiver(), filter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission SMS accordée");
            } else {
                Toast.makeText(this, "Les SMS ne seront pas envoyés sans permission", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Les permissions ont été accordées, demander les mises à jour
                requestLocationUpdates();
                startLocationService();
            } else {
                // Les permissions ont été refusées
                Toast.makeText(this, "Les permissions de localisation sont nécessaires pour cette fonctionnalité", Toast.LENGTH_LONG).show();
            }
        }
    }
    /**
     * Intercepte le bouton retour pour afficher la confirmation de déconnexion
     */
    @SuppressWarnings("MissingSuperCall")
    @Override
    public void onBackPressed() {

        // Afficher la confirmation de déconnexion au lieu de fermer l'app
        showLogoutConfirmation();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }

        localisationManager.cleanup();
    }
}

