package ci.technchange.prestationscmu.views;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.DownloadService;
import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.core.dbHelper;
import ci.technchange.prestationscmu.receivers.SyncNotificationReceiver;
import ci.technchange.prestationscmu.utils.AgentManager;
import ci.technchange.prestationscmu.utils.ApiService;
import ci.technchange.prestationscmu.utils.DataSMSManager;
import ci.technchange.prestationscmu.utils.MetriqueApiSender;
import ci.technchange.prestationscmu.utils.ReferentielService;
import ci.technchange.prestationscmu.utils.RegionCoordUtils;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UploadQueueManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

import static android.content.Context.RECEIVER_NOT_EXPORTED;

import com.famoco.biometryservicelibrary.BiometryServiceAccess;

import org.json.JSONException;
import org.json.JSONObject;

import ci.technchange.prestationscmu.R;

public class InscriptionStepperActivity extends AppCompatActivity {

    private static final String TAG = "InscriptionStepper";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_CAMERA = 101;
    private static final int REQUEST_STORAGE_PERMISSION = 103;
    private static final int REQUEST_EMPREINTES = 2;
    private static final int REQUEST_IMAGE_CAPTURE_FACADE = 3;
    private static final int REQUEST_IMAGE_CAPTURE_INTERIEUR = 4;
    private static final int REQUEST_LOCATION_PERMISSION = 105;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 123;

    // Étape actuelle
    private int currentStep = 1;
    private static final int TOTAL_STEPS = 4;

    // Données utilisateur
    private String photoPath;
    private String photoPathFacade;
    private String photoPathInterieur;
    private String nomUtilisateur;
    private String prenomUtilisateur;
    private String numeroUtilisateur;
    private String matriculeUtilisateur;
    private String centreSanteUtilisateur;
    private Bitmap photoUtilisateur;
    private boolean empreintesEnregistrees = false;

    // Coordonnées GPS
    private double latitudeFacade, longitudeFacade;
    private double latitudeInterieur, longitudeInterieur;

    // Composants UI - Stepper
    private TextView[] stepperCircles;
    private LinearLayout[] stepLayouts;
    private Button btnPrevious, btnNext;

    // Composants UI - Étape 1 (Informations)
    private EditText editNom, editPrenom, editNumeroTel, editMatricule;

    // Composants UI - Étape 2 (Photo)
    private Button btnPrendrePhoto;
    private ImageView imgUtilisateur;
    private LinearLayout layoutPhotoButton;

    // Composants UI - Étape 3 (Empreintes)
    private Button btnEnregistrerEmpreinte;
    private LinearLayout layoutEmpreinteButton, layoutEmpreintesCapturees;
    private CheckBox checkboxIndexDroit, checkboxIndexGauche, checkboxPouceDroit, checkboxPouceGauche;

    // Composants UI - Étape 4 (Centre de santé)
    private AutoCompleteTextView autoCompleteCentreSante;
    private ImageView imageViewFacade, imageViewInterieur;
    private ImageButton btnCaptureFacade, btnCaptureInterieur;

    // Services
    private ReferentielService referentielService;
    private LocationManager locationManager;
    private Uri photoUri;
    private Uri photoUriFacade;
    private Uri photoUriInterieur;
    private int currentPhotoRequest;

    // LocationListener pour les coordonnées GPS
    private LocationListener locationListener;
    private static final int REQUEST_SELFIE = 101;

    private boolean isAnyAgentExisting = false;
    private String existingCentreSante;
    private String existingPhotoFacadePath;
    private String existingPhotoInterieurPath;
    private double existingLatitudeFacade;
    private double existingLongitudeFacade;
    private double existingLatitudeInterieur;
    private double existingLongitudeInterieur;

    private static final int TOTAL_STEPS_WITH_EXISTING_AGENT = 3;

    private View divider3;
    private LinearLayout stepperCentreLayout;

    private ArrayList<Object> empreintesCapturees = new ArrayList<>();

    private boolean isDialogShown = false;
    private boolean registerCenterLater = false;
    private SharedPrefManager sharedPrefManager;

    private String idFamoco;

    private UtilsInfosAppareil utilsInfos;

    String nomEtablissement;

    private RegionCoordUtils regionCoordUtils;

    private ProgressDialog loadingDialog;
    private TextView tvDatabaseInfoInscription;

    private AgentManager agentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inscription_stepper);


        // Initialiser tous les autres composants
        referentielService = new ReferentielService(this);
        sharedPrefManager = new SharedPrefManager(this);
        // Initialiser le gestionnaire de localisation
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        utilsInfos = new UtilsInfosAppareil(this);
        utilsInfos.obtenirInformationsSysteme(this);
        agentManager = AgentManager.getInstance(this);
        tvDatabaseInfoInscription = findViewById(R.id.tvDatabaseInfoInscription);

        regionCoordUtils = new RegionCoordUtils(this); // Initialiser l'utilitaire des coordonnées

        nomEtablissement = fetchEtablissementName();

        idFamoco = utilsInfos.recupererIdAppareil();

        System.out.println("------------"+idFamoco+"---------------");
        setupLocationListener();
        create_table();

        // Vérifier si un agent existe déjà dans la base de données
        checkIfAnyAgentExists();

        initializeViews();
        setupStepperControls();
        loadCentreSante();
        setupListeners();

        // Si un agent existe, ajuster l'interface
        if (isAnyAgentExisting) {
            hideStep4FromStepper();
        }

        System.out.println("------------"+nomEtablissement+"-----------");

        updateStepperUI();
        String downloadedFileName = sharedPrefManager.getDownloadedFileName();
        File dbFile = new File(getFilesDir(), "encryptedbd/newcnambd1.db");
        Log.d("TAGGGGGG", "onCreate: "+downloadedFileName);
        if (dbFile.exists()) {
            if (downloadedFileName != null && !downloadedFileName.isEmpty() && !"newcnambd1.db".equals(downloadedFileName)) {
                // Convertir le nom de fichier en nom de région
                String regionName = RegionUtils.getRegionNameFromFileName(downloadedFileName);
                //sharedPrefManager.saveRegionName(regionName);


                //Toast.makeText(this, "name2:"+regionName, Toast.LENGTH_LONG).show();

                tvDatabaseInfoInscription.setText("Région: " + regionName);
                tvDatabaseInfoInscription.setTextColor(getResources().getColor(android.R.color.black));
            } else {

                tvDatabaseInfoInscription.setVisibility(View.GONE);

            }
        } else {
            tvDatabaseInfoInscription.setVisibility(View.GONE);

        }
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadService.PROGRESS_UPDATE_ACTION.equals(intent.getAction())) {
                // Récupérer la progression et l'afficher
                if (intent.hasExtra(DownloadService.PROGRESS_EXTRA)) {
                    int progress = intent.getIntExtra(DownloadService.PROGRESS_EXTRA, 0);
                    runOnUiThread(() -> {
                        tvDatabaseInfoInscription.setText("Téléchargement en cours : " + progress + "%");
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
                                        tvDatabaseInfoInscription.setVisibility(View.GONE);
                                    } else {
                                        String regionName = RegionUtils.getRegionNameFromFileName(originalFileName);
                                        tvDatabaseInfoInscription.setText("Base de données: " + regionName);
                                        tvDatabaseInfoInscription.setTextColor(getResources().getColor(android.R.color.black));
                                    }
                                });


                            } else {
                                runOnUiThread(() -> {
                                    tvDatabaseInfoInscription.setVisibility(View.GONE);

                                });
                                Toast.makeText(InscriptionStepperActivity.this, "Erreur lors du téléchargement", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("DownloadReceiver", "Erreur : result ou originalFileName est null");
                            Toast.makeText(InscriptionStepperActivity.this, "Erreur lors du téléchargement : données manquantes", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    };

    // Méthode pour vérifier si un agent existe déjà dans la base de données
    private void checkIfAnyAgentExists() {
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();

        try {
            // Requête pour vérifier si la table contient au moins un agent
            String query = "SELECT * FROM agents_inscription LIMIT 1";
            Cursor cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                // Au moins un agent existe déjà, récupérer les informations
                isAnyAgentExisting = true;

                // Récupérer l'index des colonnes
                int centreSanteIndex = cursor.getColumnIndex("centre_sante");
                int photoFacadeIndex = cursor.getColumnIndex("photo_facade_path");
                int photoInterieurIndex = cursor.getColumnIndex("photo_interieur_path");
                int latFacadeIndex = cursor.getColumnIndex("latitude_facade");
                int longFacadeIndex = cursor.getColumnIndex("longitude_facade");
                int latInterieurIndex = cursor.getColumnIndex("latitude_interieur");
                int longInterieurIndex = cursor.getColumnIndex("longitude_interieur");

                // Récupérer les valeurs
                if (centreSanteIndex >= 0) existingCentreSante = cursor.getString(centreSanteIndex);
                if (photoFacadeIndex >= 0) existingPhotoFacadePath = cursor.getString(photoFacadeIndex);
                if (photoInterieurIndex >= 0) existingPhotoInterieurPath = cursor.getString(photoInterieurIndex);
                if (latFacadeIndex >= 0) existingLatitudeFacade = cursor.getDouble(latFacadeIndex);
                if (longFacadeIndex >= 0) existingLongitudeFacade = cursor.getDouble(longFacadeIndex);
                if (latInterieurIndex >= 0) existingLatitudeInterieur = cursor.getDouble(latInterieurIndex);
                if (longInterieurIndex >= 0) existingLongitudeInterieur = cursor.getDouble(longInterieurIndex);

                Log.d(TAG, "Un agent existant a été trouvé. Centre: " + existingCentreSante);

                // Informer l'utilisateur
                runOnUiThread(() -> {
                    Toast.makeText(InscriptionStepperActivity.this,
                            "Un agent précédent existe déjà. L'étape du centre de santé sera automatiquement sautée.",
                            Toast.LENGTH_LONG).show();
                });
            } else {
                isAnyAgentExisting = false;
                Log.d(TAG, "Aucun agent n'existe encore dans la base de données");
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification d'agents existants", e);
            isAnyAgentExisting = false;
        } finally {
            db.close();
        }
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


    private void setupLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (currentPhotoRequest == REQUEST_IMAGE_CAPTURE_FACADE) {
                    latitudeFacade = location.getLatitude();
                    longitudeFacade = location.getLongitude();
                    Log.d(TAG, "Facade - Lat: " + latitudeFacade + ", Long: " + longitudeFacade);
                } else if (currentPhotoRequest == REQUEST_IMAGE_CAPTURE_INTERIEUR) {
                    latitudeInterieur = location.getLatitude();
                    longitudeInterieur = location.getLongitude();
                    Log.d(TAG, "Interieur - Lat: " + latitudeInterieur + ", Long: " + longitudeInterieur);
                }
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

    private void initializeViews() {
        // Initialiser les éléments du stepper
        stepperCircles = new TextView[TOTAL_STEPS];
        stepperCircles[0] = findViewById(R.id.stepperCircle1);
        stepperCircles[1] = findViewById(R.id.stepperCircle2);
        stepperCircles[2] = findViewById(R.id.stepperCircle3);
        stepperCircles[3] = findViewById(R.id.stepperCircle4);

        // Nouveaux éléments
        divider3 = findViewById(R.id.divider3);
        stepperCentreLayout = findViewById(R.id.stepperCentreLayout);

        stepLayouts = new LinearLayout[TOTAL_STEPS];
        stepLayouts[0] = findViewById(R.id.stepIdentite);
        stepLayouts[1] = findViewById(R.id.stepPhoto);
        stepLayouts[2] = findViewById(R.id.stepEmpreintes);
        stepLayouts[3] = findViewById(R.id.stepCentre);

        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        // Initialiser les champs de l'étape 1
        editNom = findViewById(R.id.editNom);
        editPrenom = findViewById(R.id.editPrenom);
        editNumeroTel = findViewById(R.id.editNumeroTelephone);
        editMatricule = findViewById(R.id.editMatricule);

        // Initialiser les champs de l'étape 2
        btnPrendrePhoto = findViewById(R.id.btnPrendrePhoto);
        imgUtilisateur = findViewById(R.id.imagePhotoProfil);
        layoutPhotoButton = findViewById(R.id.layoutPhotoButton);

        // Initialiser les champs de l'étape 3
        btnEnregistrerEmpreinte = findViewById(R.id.btnEnregistrerEmpreinte);
        layoutEmpreinteButton = findViewById(R.id.layoutEmpreinteButton);
        layoutEmpreintesCapturees = findViewById(R.id.layoutEmpreintesCapturees);
        checkboxIndexDroit = findViewById(R.id.checkboxIndexDroit);
        checkboxIndexGauche = findViewById(R.id.checkboxIndexGauche);
        checkboxPouceDroit = findViewById(R.id.checkboxPouceDroit);
        checkboxPouceGauche = findViewById(R.id.checkboxPouceGauche);

        // Initialiser les champs de l'étape 4
        autoCompleteCentreSante = findViewById(R.id.autoCompleteCentreSante);
        imageViewFacade = findViewById(R.id.imageViewFacade);
        imageViewInterieur = findViewById(R.id.imageViewInterieur);
        btnCaptureFacade = findViewById(R.id.btnCaptureFacade);
        btnCaptureInterieur = findViewById(R.id.btnCaptureInterieur);

        // Préremplir avec le nom de l'établissement et désactiver la modification
        if (nomEtablissement != null && !nomEtablissement.isEmpty()) {
            autoCompleteCentreSante.setText(nomEtablissement);
            autoCompleteCentreSante.setEnabled(false);  // Désactiver les modifications
            autoCompleteCentreSante.setFocusable(false);  // Empêcher le focus
            autoCompleteCentreSante.setClickable(false);  // Empêcher les clics

            // Changer l'apparence pour montrer qu'il n'est pas modifiable
            //autoCompleteCentreSante.setBackgroundResource(android.R.drawable.edit_text_normal);
            autoCompleteCentreSante.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    // Ajoutez cette méthode pour masquer l'étape 4 du stepper
    private void hideStep4FromStepper() {
        // Masquer le cercle de l'étape 4 et son libellé
        stepperCentreLayout.setVisibility(View.GONE);

        // Masquer également le séparateur qui précède l'étape 4
        divider3.setVisibility(View.GONE);

        // Informer l'utilisateur
        Toast.makeText(this,
                "Un agent existe déjà. Les informations du centre seront réutilisées automatiquement.",
                Toast.LENGTH_LONG).show();
    }

    private void setupStepperControls() {
        btnPrevious.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                updateStepperUI();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (validateCurrentStep()) {
                int totalSteps = isAnyAgentExisting ? TOTAL_STEPS_WITH_EXISTING_AGENT : TOTAL_STEPS;

                if (currentStep < totalSteps) {
                    currentStep++;
                    // Si nous avons un agent existant et que nous sommes à l'étape 3,
                    // nous voulons terminer l'inscription au lieu de passer à l'étape 4
                    updateStepperUI();
                } else {
                    // C'est la dernière étape, on peut terminer l'inscription
                    completeInscription();
                }
            }
        });
    }

    // Modifiez la méthode updateStepperUI pour ajuster le nombre total d'étapes
    private void updateStepperUI() {
        int totalSteps = isAnyAgentExisting ? TOTAL_STEPS_WITH_EXISTING_AGENT : TOTAL_STEPS;

        // Mise à jour des cercles du stepper
        for (int i = 0; i < TOTAL_STEPS; i++) {
            // Ignorer le dernier cercle si un agent existe déjà
            if (isAnyAgentExisting && i == 3) {
                continue;
            }

            if (i + 1 < currentStep) {
                // Étape terminée
                stepperCircles[i].setBackgroundResource(R.drawable.circle_active);
                stepperCircles[i].setText("✓");
            } else if (i + 1 == currentStep) {
                // Étape actuelle
                stepperCircles[i].setBackgroundResource(R.drawable.circle_active);
                stepperCircles[i].setText(String.valueOf(i + 1));
            } else if (i < totalSteps) {
                // Étape future valide
                stepperCircles[i].setBackgroundResource(R.drawable.circle_inactive);
                stepperCircles[i].setText(String.valueOf(i + 1));
            }
        }

        // Afficher uniquement le layout de l'étape actuelle
        for (int i = 0; i < TOTAL_STEPS; i++) {
            stepLayouts[i].setVisibility(i + 1 == currentStep ? View.VISIBLE : View.GONE);
        }

        // Mise à jour des boutons de navigation
        btnPrevious.setEnabled(currentStep > 1);

        if (currentStep == totalSteps) {
            btnNext.setText("Terminer");
        } else {
            btnNext.setText("Suivant");
        }

        // Si on arrive à l'étape 4 et qu'aucun agent n'existe (première inscription)
        // et que l'on n'a pas déjà demandé à l'utilisateur
        if (currentStep == 4 && !isAnyAgentExisting && !isDialogShown) {
            showCenterRegistrationDialog();
        }
    }

    private void showCenterRegistrationDialog() {
        isDialogShown = true;

        new AlertDialog.Builder(this)
                .setTitle("Enregistrement du centre")
                .setMessage("Souhaitez-vous enregistrer les informations du centre maintenant ? Vous devez être physiquement présent au centre pour compléter cette étape.")
                .setPositiveButton("Maintenant", (dialog, which) -> {
                    // L'utilisateur veut enregistrer le centre maintenant
                    // Ne rien faire de spécial, laisser l'utilisateur continuer l'étape 4
                    registerCenterLater = false;
                })
                .setNegativeButton("Plus tard", (dialog, which) -> {
                    // L'utilisateur veut enregistrer le centre plus tard
                    registerCenterLater = true;

                    // Continuer vers la complétion de l'inscription sans les données du centre
                    completeInscriptionWithoutCenter();
                })
                .setCancelable(false)
                .show();
    }

    private void completeInscriptionWithoutCenter() {
        // Définir les valeurs par défaut pour les champs du centre
        centreSanteUtilisateur = "";
        photoPathFacade = null;
        photoPathInterieur = null;
        photoUriFacade = null;
        photoUriInterieur = null;
        latitudeFacade = 0;
        longitudeFacade = 0;
        latitudeInterieur = 0;
        longitudeInterieur = 0;

        // Enregistrer les données sans les informations du centre
        if (saveDataToSQLiteWithoutCenter()) {
            // Afficher un message à l'utilisateur
            new AlertDialog.Builder(this)
                    .setTitle("Inscription partielle")
                    .setMessage("Vos informations personnelles ont été enregistrées. Vous avez 24h compléter les informations du centre en vous rendant physiquement au centre de santé.")
                    .setPositiveButton("Compris", (dialog, which) -> {
                        // Rediriger l'utilisateur vers la page d'accueil ou de connexion
                        sharedPrefManager.setagentName(nomUtilisateur, prenomUtilisateur);
                        sharedPrefManager.setCodeAgent(matriculeUtilisateur);
                        navigateToNextActivity();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private boolean saveDataToSQLiteWithoutCenter() {
        dbHelper helper = new dbHelper(this);
        DataSMSManager smsManager = new DataSMSManager();

        try {
            // Insérer un nouvel agent sans les informations du centre
            ContentValues values = new ContentValues();
            values.put("nom", nomUtilisateur);
            values.put("prenom", prenomUtilisateur);
            values.put("telephone", numeroUtilisateur);
            values.put("matricule", matriculeUtilisateur);
            values.put("centre_sante", sharedPrefManager.getCodeEts()); // Centre vide
            values.put("photo_path", photoPath);
            values.put("empreintes", empreintesEnregistrees ? 1 : 0);
            // Ne pas mettre les informations du centre

            // Format de date actuel
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String date = dateFormat.format(new Date());
            values.put("date_inscription", date);

            SQLiteDatabase db = helper.getWritableDatabase();
            long newRowId = db.insert("agents_inscription", null, values);
            db.close();

            if (newRowId != -1) {
                Log.d(TAG, "Inscription partielle enregistrée avec ID: " + newRowId);
                Toast.makeText(this, "Inscription partielle enregistrée", Toast.LENGTH_SHORT).show();
                if (isNetworkAvailable()) {

                    // Appeler l'API pour envoyer les données
                    ApiService apiService = new ApiService(this);
                    apiService.registerAgent(
                            nomUtilisateur,
                            prenomUtilisateur,
                            numeroUtilisateur,
                            sharedPrefManager.getCodeEts(),
                            matriculeUtilisateur,
                            photoPath,
                            idFamoco,
                            new ApiService.ApiCallback() {
                                @Override
                                public void onSuccess(JSONObject response) {
                                    runOnUiThread(() -> {
                                        //progressDialog.dismiss();

                                        // Vérifier si un message existe
                                        String message = response.optString("message", "Données synchronisées avec succès");

                                        // Vérifier s'il y a un token ou un utilisateur
                                        boolean hasToken = response.has("token");
                                        boolean hasUser = response.has("user");

                                        if (hasToken || hasUser) {
                                            Toast.makeText(InscriptionStepperActivity.this,
                                                    "Synchronisation réussie: " + message,
                                                    Toast.LENGTH_LONG).show();

                                            // Marquer l'agent comme synchronisé dans la base de données locale
                                            markAgentAsSynchronized(matriculeUtilisateur);
                                            //sendDataToApiCentre(formData, matriculeUtilisateur);

                                            // Sauvegarder le token si présent pour de futures authentifications
                                            /*if (hasToken) {
                                                String token = response.getString("token");
                                                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                                                prefs.edit().putString("auth_token", token).apply();
                                                Log.d(TAG, "Token sauvegardé: " + token);
                                            }*/
                                        } else {
                                            Toast.makeText(InscriptionStepperActivity.this,
                                                    "Réponse reçue, mais manque d'informations: " + message,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    runOnUiThread(() -> {
                                        //progressDialog.dismiss();
                                        Log.e(TAG, "Erreur API: " + message);

                                        // Afficher une alerte avec le message d'erreur
                                        new AlertDialog.Builder(InscriptionStepperActivity.this)
                                                .setTitle("Erreur de synchronisation")
                                                .setMessage("Impossible d'envoyer les données au serveur: " + message +
                                                        "\n\nLes données sont sauvegardées localement et seront synchronisées automatiquement plus tard.")
                                                .setPositiveButton("OK", null)
                                                .show();
                                    });
                                }
                            }
                    );

                }else{
                    handleSmsSending(smsManager, nomUtilisateur, prenomUtilisateur, matriculeUtilisateur, numeroUtilisateur, "", "", "", "");
                }
                return true;
            } else {
                Log.e(TAG, "Erreur lors de l'enregistrement des données partielles");
                Toast.makeText(this, "Erreur lors de l'enregistrement des données", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de l'enregistrement partiel dans SQLite", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // Ajoutez cette méthode pour naviguer vers l'activité suivante
    private void navigateToNextActivity() {
        // Créer un Intent vers l'activité de rappel pour compléter l'inscription
        //Intent intent = new Intent(this, ReminderActivity.class);
        Intent intent = new Intent(this, ConnexionActivity.class);
        intent.putExtra("MATRICULE", matriculeUtilisateur);
        startActivity(intent);
        finish();
    }

    // Modifiez la méthode validateCurrentStep()
    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1:
                // Validation de l'identité (inchangée)
                nomUtilisateur = editNom.getText().toString().trim();
                prenomUtilisateur = editPrenom.getText().toString().trim();
                numeroUtilisateur = editNumeroTel.getText().toString().trim();
                matriculeUtilisateur = editMatricule.getText().toString().trim();

                if (TextUtils.isEmpty(nomUtilisateur)) {
                    editNom.setError("Le nom est requis");
                    return false;
                }
                if (TextUtils.isEmpty(prenomUtilisateur)) {
                    editPrenom.setError("Le prénom est requis");
                    return false;
                }
                if (TextUtils.isEmpty(numeroUtilisateur)) {
                    editNumeroTel.setError("Le numéro de téléphone est requis");
                    return false;
                }
                if (TextUtils.isEmpty(matriculeUtilisateur)) {
                    editMatricule.setError("Le matricule est requis");
                    return false;
                }

                if (agentManager.isMatriculeExistsInscription(matriculeUtilisateur)) {
                    editMatricule.setError("Ce matricule existe déjà");

                    return false;
                }
                return true;

            case 2: // Validation de la photo (inchangée)
                if (photoUtilisateur == null) {
                    Toast.makeText(this, "Veuillez prendre une photo", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;

            case 3: // Validation des empreintes (inchangée)
                if (!empreintesEnregistrees) {
                    Toast.makeText(this, "Veuillez enregistrer vos empreintes", Toast.LENGTH_SHORT).show();
                    return false;
                }

                // Si un agent existe et que nous sommes à l'étape 3,
                // c'est notre dernière étape, donc préparer les données du centre
                if (isAnyAgentExisting) {
                    // Utiliser les données du centre de l'agent existant
                    centreSanteUtilisateur = existingCentreSante;
                    photoPathFacade = existingPhotoFacadePath;
                    photoPathInterieur = existingPhotoInterieurPath;
                    latitudeFacade = existingLatitudeFacade;
                    longitudeFacade = existingLongitudeFacade;
                    latitudeInterieur = existingLatitudeInterieur;
                    longitudeInterieur = existingLongitudeInterieur;

                    // Créer les URI pour les photos si les chemins existent
                    if (photoPathFacade != null && !photoPathFacade.isEmpty()) {
                        File facadeFile = new File(photoPathFacade);
                        if (facadeFile.exists()) {
                            photoUriFacade = Uri.fromFile(facadeFile);
                        }
                    }

                    if (photoPathInterieur != null && !photoPathInterieur.isEmpty()) {
                        File interieurFile = new File(photoPathInterieur);
                        if (interieurFile.exists()) {
                            photoUriInterieur = Uri.fromFile(interieurFile);
                        }
                    }
                }
                return true;

            case 4: // Validation du centre de santé
                // Si l'utilisateur a choisi de remplir les infos du centre plus tard
                if (registerCenterLater) {
                    return true; // Pas de validation nécessaire
                }

                // Validation normale pour l'étape 4
                //centreSanteUtilisateur = autoCompleteCentreSante.getText().toString().trim();
                centreSanteUtilisateur = nomEtablissement;
                if (TextUtils.isEmpty(centreSanteUtilisateur)) {
                    Toast.makeText(this, "Le centre de santé est requis", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (photoUriFacade == null) {
                    Toast.makeText(this, "Veuillez prendre la photo de la façade", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (photoUriInterieur == null) {
                    Toast.makeText(this, "Veuillez prendre la photo de l'intérieur", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;

            default:
                return true;
        }
    }

    // Méthode pour préparer l'interface de l'étape 4 si un agent existe déjà
    private void prepareStep4ForExistingAgent() {
        runOnUiThread(() -> {
            if (isAnyAgentExisting) {
                // Pré-remplir le champ du centre de santé
                autoCompleteCentreSante.setText(existingCentreSante);
                // Désactiver les contrôles car les informations sont déjà disponibles
                btnCaptureFacade.setEnabled(false);
                btnCaptureInterieur.setEnabled(false);
                autoCompleteCentreSante.setEnabled(false);

                // Ajouter un message d'information
                TextView infoTextView = new TextView(InscriptionStepperActivity.this);
                infoTextView.setText("Les informations du centre sont automatiquement réutilisées d'une inscription précédente.");
                infoTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                infoTextView.setPadding(0, 20, 0, 20);

                // Vérifier si ce message n'est pas déjà ajouté
                boolean messageExists = false;
                LinearLayout step4Layout = findViewById(R.id.stepCentre);
                for (int i = 0; i < step4Layout.getChildCount(); i++) {
                    if (step4Layout.getChildAt(i) instanceof TextView) {
                        TextView tv = (TextView) step4Layout.getChildAt(i);
                        if (tv.getText().toString().contains("automatiquement réutilisées")) {
                            messageExists = true;
                            break;
                        }
                    }
                }

                if (!messageExists) {
                    // Ajouter ce TextView au layout de l'étape 4
                    step4Layout.addView(infoTextView, 0);
                }

                // Afficher les images si les fichiers existent
                if (photoPathFacade != null && !photoPathFacade.isEmpty()) {
                    File facadeFile = new File(photoPathFacade);
                    if (facadeFile.exists()) {
                        Bitmap facadeBitmap = BitmapFactory.decodeFile(photoPathFacade);
                        if (facadeBitmap != null) {
                            imageViewFacade.setImageBitmap(facadeBitmap);
                            photoUriFacade = Uri.fromFile(facadeFile);
                        }
                    }
                }

                if (photoPathInterieur != null && !photoPathInterieur.isEmpty()) {
                    File interieurFile = new File(photoPathInterieur);
                    if (interieurFile.exists()) {
                        Bitmap interieurBitmap = BitmapFactory.decodeFile(photoPathInterieur);
                        if (interieurBitmap != null) {
                            imageViewInterieur.setImageBitmap(interieurBitmap);
                            photoUriInterieur = Uri.fromFile(interieurFile);
                        }
                    }
                }
            }
        });
    }

    private File compressImage(File originalFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath());
            File compressedFile = new File(this.getCacheDir(), "compressed_" + originalFile.getName());

            FileOutputStream fos = new FileOutputStream(compressedFile);
            // Compresser avec une qualité de 70% (ajuster selon vos besoins)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
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

    private void loadCentreSante() {
        List<String> etablissements = referentielService.getAllEtablissements();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                etablissements
        );
        autoCompleteCentreSante.setAdapter(adapter);
        autoCompleteCentreSante.setThreshold(1);
    }

    private void setupListeners() {
        // Étape 2: Photo de profil
        /*btnPrendrePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent(REQUEST_IMAGE_CAPTURE);
            }
        });*/

        // Dans setupListeners()
        btnPrendrePhoto.setOnClickListener(v -> {
            Intent selfieIntent = new Intent(InscriptionStepperActivity.this, SelfieActivity.class);
            startActivityForResult(selfieIntent, REQUEST_SELFIE);
        });

        // Étape 3: Empreintes
        btnEnregistrerEmpreinte.setOnClickListener(v -> {
            // Afficher un spinner de chargement
            loadingDialog = new ProgressDialog(InscriptionStepperActivity.this);
            loadingDialog.setMessage("Chargement en cours...");
            loadingDialog.setCancelable(false);
            loadingDialog.show();

            Intent intent = new Intent(InscriptionStepperActivity.this, EnrollmentActivity.class);

            // Passer les données via l'intent
            intent.putExtra("NOM", nomUtilisateur);
            intent.putExtra("PRENOM", prenomUtilisateur);
            intent.putExtra("TELEPHONE", numeroUtilisateur);
            intent.putExtra("MATRICULE", matriculeUtilisateur);
            intent.putExtra("PHOTO_PATH", photoPath);

            startActivityForResult(intent, REQUEST_EMPREINTES);
        });

        // Étape 4: Centre de santé et photos de l'établissement
        /*btnCaptureFacade.setOnClickListener(v -> {
            currentPhotoRequest = REQUEST_IMAGE_CAPTURE_FACADE;
            if (checkCameraPermission() && checkLocationPermission()) {
                dispatchTakePictureIntent(REQUEST_IMAGE_CAPTURE_FACADE);
            }
        });

        btnCaptureInterieur.setOnClickListener(v -> {
            currentPhotoRequest = REQUEST_IMAGE_CAPTURE_INTERIEUR;
            if (checkCameraPermission() && checkLocationPermission()) {
                dispatchTakePictureIntent(REQUEST_IMAGE_CAPTURE_INTERIEUR);
            }
        });*/

        btnCaptureFacade.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                // Lancer CameraActivity pour la façade
                Intent intent = new Intent(this, CameraEtablissementActivity.class);
                intent.putExtra(CameraEtablissementActivity.EXTRA_PHOTO_TYPE, CameraEtablissementActivity.PHOTO_TYPE_FACADE);
                intent.putExtra("MATRICULE", matriculeUtilisateur);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_FACADE);
            }
        });

        btnCaptureInterieur.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                // Lancer CameraActivity pour l'intérieur
                Intent intent = new Intent(this, CameraEtablissementActivity.class);
                intent.putExtra(CameraEtablissementActivity.EXTRA_PHOTO_TYPE, CameraEtablissementActivity.PHOTO_TYPE_INTERIEUR);
                intent.putExtra("MATRICULE", matriculeUtilisateur);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_INTERIEUR);
            }
        });
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName;

        switch (currentPhotoRequest) {
            case REQUEST_IMAGE_CAPTURE:
                imageFileName = "photo_user_" + numeroUtilisateur + "_" + timeStamp;
                break;
            case REQUEST_IMAGE_CAPTURE_FACADE:
                imageFileName = "facade_" + timeStamp;
                break;
            case REQUEST_IMAGE_CAPTURE_INTERIEUR:
                imageFileName = "interieur_" + timeStamp;
                break;
            default:
                imageFileName = "photo_" + timeStamp;
        }

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.d(TAG, "Dossier de stockage: " + storageDir.getAbsolutePath());

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );

            // Enregistrer le chemin de l'image selon le type
            String currentPath = image.getAbsolutePath();
            switch (currentPhotoRequest) {
                case REQUEST_IMAGE_CAPTURE:
                    photoPath = currentPath;
                    break;
                case REQUEST_IMAGE_CAPTURE_FACADE:
                    photoPathFacade = currentPath;
                    break;
                case REQUEST_IMAGE_CAPTURE_INTERIEUR:
                    photoPathInterieur = currentPath;
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Erreur lors de la création du fichier image", e);
        }

        return image;
    }

    private void dispatchTakePictureIntent(int requestCode) {
        currentPhotoRequest = requestCode;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Spécifier l'utilisation de la caméra frontale (selfie)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1); // 1 = caméra frontale
            takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
            takePictureIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
        }
        File photoFile = createImageFile();

        if (photoFile != null) {
            Uri photoUriTemp = FileProvider.getUriForFile(
                    this,
                    "ci.technchange.prestationscmuym.fileprovider",  // Utiliser la même autorité que dans l'ancien code
                    photoFile
            );

            // Stocker l'URI selon le type de photo
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    photoUri = photoUriTemp;
                    break;
                case REQUEST_IMAGE_CAPTURE_FACADE:
                    photoUriFacade = photoUriTemp;
                    break;
                case REQUEST_IMAGE_CAPTURE_INTERIEUR:
                    photoUriInterieur = photoUriTemp;
                    break;
            }

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUriTemp);
            startActivityForResult(takePictureIntent, requestCode);
        } else {
            Toast.makeText(this, "Erreur lors de la création du fichier photo", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
            return false;
        }
        return true;
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_CAMERA:
                    // Si c'est pour les photos de l'établissement, vérifier aussi la permission de localisation
                    if (currentPhotoRequest == REQUEST_IMAGE_CAPTURE_FACADE ||
                            currentPhotoRequest == REQUEST_IMAGE_CAPTURE_INTERIEUR) {
                        if (checkLocationPermission()) {
                            dispatchTakePictureIntent(currentPhotoRequest);
                        }
                    } else {
                        dispatchTakePictureIntent(currentPhotoRequest);
                    }
                    break;
                case REQUEST_LOCATION_PERMISSION:
                    if (checkCameraPermission()) {
                        dispatchTakePictureIntent(currentPhotoRequest);
                    }
                    break;
            }
        } else {
            Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Fermer le dialog de chargement s'il est visible
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }

        if (resultCode == RESULT_OK) {
            switch (requestCode) {

                case REQUEST_SELFIE:
                    if (data != null) {
                        photoPath = data.getStringExtra("PHOTO_PATH");
                        // Définir currentPhotoRequest pour que processCameraResult sache quelle variable mettre à jour
                        currentPhotoRequest = REQUEST_IMAGE_CAPTURE;
                        processCameraResult(photoPath, imgUtilisateur, layoutPhotoButton);
                    }
                    break;
                case REQUEST_IMAGE_CAPTURE:
                    processCameraResult(photoPath, imgUtilisateur, layoutPhotoButton);
                    break;

                case REQUEST_EMPREINTES:
                    if (data != null) {
                        boolean empreintesValides = data.getBooleanExtra("EMPREINTES_VALIDES", false);
                        if (empreintesValides) {

                            // Récupérer l'identifiant temporaire
                            String tempId = data.getStringExtra("TEMP_ID");
                            int nbEmpreintes = data.getIntExtra("NB_EMPREINTES", 0);

                            Log.d(TAG, "onActivityResult11111111: "+ tempId);

                            // Récupérer les empreintes depuis la base de données temporaire
                            if (tempId != null) {
                                Log.d(TAG, "onActivityResult11111111: okkkkkk");
                                retrieveAndStoreFingerprints(tempId, matriculeUtilisateur);
                            }

                            // Masquer le bouton d'empreinte et afficher les cases à cocher
                            layoutEmpreinteButton.setVisibility(View.GONE);
                            layoutEmpreintesCapturees.setVisibility(View.VISIBLE);

                            // Mettre à jour l'état des cases à cocher
                            checkboxIndexDroit.setChecked(data.getBooleanExtra("INDEX_DROIT", false));
                            checkboxIndexGauche.setChecked(data.getBooleanExtra("INDEX_GAUCHE", false));
                            checkboxPouceDroit.setChecked(data.getBooleanExtra("POUCE_DROIT", false));
                            checkboxPouceGauche.setChecked(data.getBooleanExtra("POUCE_GAUCHE", false));

                            empreintesEnregistrees = true;
                            Toast.makeText(this, "Empreintes enregistrées avec succès", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;

                case REQUEST_IMAGE_CAPTURE_FACADE:
                    photoPathFacade = data.getStringExtra(CameraEtablissementActivity.EXTRA_PHOTO_PATH);
                    photoUriFacade = Uri.parse(data.getStringExtra(CameraEtablissementActivity.EXTRA_PHOTO_URI));

                    // Récupérer les coordonnées GPS de l'intent
                    latitudeFacade = data.getDoubleExtra("LATITUDE", 0);
                    longitudeFacade = data.getDoubleExtra("LONGITUDE", 0);

                    processEstablishmentPhoto(photoPathFacade, imageViewFacade, true);
                    break;

                case REQUEST_IMAGE_CAPTURE_INTERIEUR:
                    photoPathInterieur = data.getStringExtra(CameraEtablissementActivity.EXTRA_PHOTO_PATH);
                    photoUriInterieur = Uri.parse(data.getStringExtra(CameraEtablissementActivity.EXTRA_PHOTO_URI));

                    // Récupérer les coordonnées GPS de l'intent
                    latitudeInterieur = data.getDoubleExtra("LATITUDE", 0);
                    longitudeInterieur = data.getDoubleExtra("LONGITUDE", 0);

                    processEstablishmentPhoto(photoPathInterieur, imageViewInterieur, false);
                    break;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // S'assurer que le dialog est fermé si l'activité est arrêtée
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    // Méthode pour stocker les empreintes dans SQLite
    // Méthode pour récupérer et stocker définitivement les empreintes
    private void retrieveAndStoreFingerprints(String tempId, String matricule) {
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();

        try {
            // Créer la table finale si elle n'existe pas
            db.execSQL("CREATE TABLE IF NOT EXISTS empreintes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "matricule TEXT NOT NULL, " +
                    "main TEXT NOT NULL, " +
                    "doigt TEXT NOT NULL, " +
                    "template BLOB NOT NULL, " +
                    "date_enregistrement TEXT)");

            // Format de date actuel
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String date = dateFormat.format(new Date());

            // Supprimer les anciennes empreintes pour cet agent
            db.delete("empreintes", "matricule = ?", new String[]{matricule});
            Log.d(TAG, "onActivityResult11111111: 2222222");
            // Récupérer les empreintes temporaires
            Cursor cursor = db.query(
                    "temp_empreintes",
                    null,
                    "temp_id = ?",
                    new String[]{tempId},
                    null, null, null);

            int count = 0;
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "onActivityResult333333: iffff");
                do {
                    Log.d(TAG, "onActivityResult333333: start");
                    // Récupérer les indices des colonnes
                    int mainIndex = cursor.getColumnIndex("main");
                    int doigtIndex = cursor.getColumnIndex("doigt");
                    int templateIndex = cursor.getColumnIndex("template");
                    Log.d(TAG, "onActivityResult333333: "+ mainIndex);

                    // Récupérer les valeurs
                    if (mainIndex >= 0 && doigtIndex >= 0 && templateIndex >= 0) {
                        String main = cursor.getString(mainIndex);
                        String doigt = cursor.getString(doigtIndex);
                        byte[] template = cursor.getBlob(templateIndex);

                        // Insérer dans la table permanente
                        ContentValues values = new ContentValues();
                        values.put("matricule", matricule);
                        values.put("main", main);
                        values.put("doigt", doigt);
                        values.put("template", template);
                        values.put("date_enregistrement", date);

                        long id = db.insert("empreintes", null, values);
                        if (id != -1) {
                            count++;
                            //db.delete("temp_empreintes", "temp_id = ?", new String[]{tempId});
                            Log.d(TAG, "Empreinte définitive enregistrée: Main=" + main + ", Doigt=" + doigt);
                        }
                    }
                } while (cursor.moveToNext());

                cursor.close();
            }else{
                Log.d(TAG, "onActivityResult11111111: else");
            }

            // Supprimer les données temporaires
            db.delete("temp_empreintes", "temp_id = ?", new String[]{tempId});

            Log.d(TAG, count + " empreintes ont été transférées et stockées définitivement");
        } catch (Exception e) {
            Log.e("onActivityResult", "Erreur lors de la récupération/stockage des empreintes: " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    private void processCameraResult(String imagePath, ImageView imageView, View buttonLayout) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);

                if (buttonLayout != null) {
                    buttonLayout.setVisibility(View.GONE);
                }

                photoUtilisateur = bitmap;

                /*if (currentPhotoRequest == REQUEST_IMAGE_CAPTURE) {
                    photoUtilisateur = bitmap;
                }*/

                Toast.makeText(this, "Photo enregistrée", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processEstablishmentPhoto(String imagePath, ImageView imageView, boolean isFacade) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                Toast.makeText(this, "Photo de l'établissement enregistrée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*private void processEstablishmentPhoto(String imagePath, ImageView imageView, boolean isFacade) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);

                // Obtenir les coordonnées GPS
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    try {
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation == null) {
                            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }

                        if (lastKnownLocation != null) {
                            if (isFacade) {
                                latitudeFacade = lastKnownLocation.getLatitude();
                                longitudeFacade = lastKnownLocation.getLongitude();
                                Log.d(TAG, "Coordonnées façade: " + latitudeFacade + ", " + longitudeFacade);
                            } else {
                                latitudeInterieur = lastKnownLocation.getLatitude();
                                longitudeInterieur = lastKnownLocation.getLongitude();
                                Log.d(TAG, "Coordonnées intérieur: " + latitudeInterieur + ", " + longitudeInterieur);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de l'obtention des coordonnées", e);
                    }
                }

                Toast.makeText(this, "Photo de l'établissement enregistrée", Toast.LENGTH_SHORT).show();
            }
        }
    }*/

    private void completeInscription() {
        // Création d'un résumé des informations saisies
        if (saveDataToSQLite()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Inscription réussie!\n\n");
            sb.append("Informations personnelles:\n");
            sb.append("Nom: ").append(nomUtilisateur).append("\n");
            sb.append("Prénom: ").append(prenomUtilisateur).append("\n");
            sb.append("Numéro de téléphone: ").append(numeroUtilisateur).append("\n");
            sb.append("Matricule: ").append(matriculeUtilisateur).append("\n\n");

            sb.append("Centre de santé: ").append(centreSanteUtilisateur).append("\n\n");

            sb.append("Données enregistrées:\n");
            sb.append("Photo de profil: ").append(photoUtilisateur != null ? "Oui" : "Non").append("\n");
            sb.append("Empreintes: ").append(empreintesEnregistrees ? "Oui" : "Non").append("\n");
            sb.append("Photo façade: ").append(photoUriFacade != null ? "Oui" : "Non").append("\n");
            sb.append("Photo intérieur: ").append(photoUriInterieur != null ? "Oui" : "Non").append("\n");

            // Afficher le résumé et terminer l'inscription
            new AlertDialog.Builder(this)
                    .setTitle("Résumé d'inscription")
                    .setMessage(sb.toString())
                    .setPositiveButton("Terminer", (dialog, which) -> {
                        // Vous pouvez ajouter ici le code pour envoyer les données au serveur
                        // puis terminer l'activité
                        //prepareForFingerprintEnrollment();
                        sharedPrefManager.setagentName(nomUtilisateur,prenomUtilisateur);
                        sharedPrefManager.setCodeAgent(matriculeUtilisateur);
                        Intent intent = new Intent(this, ConnexionActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        }

    }

    private void prepareForFingerprintEnrollment() {
        // Déconnecter le service s'il est en cours d'exécution
        try {
            Context context = getApplicationContext();

            // Vous avez besoin d'une instance de BiometryServiceAccess
            // Si BiometryServiceAccess est accessible via le ViewModel
            // Vous pouvez faire comme ceci:
            EnrollmentViewModel viewModel = new ViewModelProvider(this).get(EnrollmentViewModel.class);

            // Utilisez les méthodes via le ViewModel
            viewModel.disconnectSensor();
            viewModel.disconnectService(context);

            // Si vous ne pouvez pas accéder au ViewModel, mais que BiometryServiceAccess
            // a une méthode getInstance() ou similaire:
            // BiometryServiceAccess biometryService = BiometryServiceAccess.getInstance();
            // biometryService.disconnectSensor();
            // biometryService.disconnectService(context);

            // Petit délai pour s'assurer que le service est bien arrêté
            Thread.sleep(500);
        } catch (Exception e) {
            Log.e("TAG", "Erreur lors de la déconnexion du service: " + e.getMessage());
        }
    }

    // Modifiez la méthode saveDataToSQLite pour tenir compte des données du centre existant
    private boolean saveDataToSQLite() {
        dbHelper helper = new dbHelper(this);
        DataSMSManager smsManager = new DataSMSManager();

        try {


            // Insérer un nouvel agent avec les informations combinées
            long newRowId = helper.insertAgentInscription(
                    nomUtilisateur,
                    prenomUtilisateur,
                    numeroUtilisateur,
                    matriculeUtilisateur,
                    centreSanteUtilisateur,  // Sera l'existingCentreSante si un agent existe déjà
                    photoPath,
                    empreintesEnregistrees,
                    photoPathFacade,         // Sera l'existingPhotoFacadePath si un agent existe déjà
                    photoPathInterieur,      // Sera l'existingPhotoInterieurPath si un agent existe déjà
                    latitudeFacade,          // Sera l'existingLatitudeFacade si un agent existe déjà
                    longitudeFacade,         // Sera l'existingLongitudeFacade si un agent existe déjà
                    latitudeInterieur,       // Sera l'existingLatitudeInterieur si un agent existe déjà
                    longitudeInterieur       // Sera l'existingLongitudeInterieur si un agent existe déjà
            );

            Map<String, Object> formData = new HashMap<>();
            formData.put("nom_agent", nomUtilisateur+" "+prenomUtilisateur);
            formData.put("contact", numeroUtilisateur);
            formData.put("nom_etablissement", centreSanteUtilisateur);
            formData.put("code_ets", sharedPrefManager.getCodeEts());
            formData.put("idFamoco", idFamoco);
            Log.d(TAG, "saveDataToSQLite: "+idFamoco);

            Map<String, Double> coordFacade = new HashMap<>();
            coordFacade.put("latitude", latitudeFacade);
            coordFacade.put("longitude", longitudeFacade);
            formData.put("coordonnees_facade", coordFacade);

            Map<String, Double> coordInterieur = new HashMap<>();
            coordInterieur.put("latitude", latitudeInterieur);
            coordInterieur.put("longitude", longitudeInterieur);
            formData.put("coordonnees_interieur", coordInterieur);

            if (newRowId != -1) {
                Log.d(TAG, "Inscription enregistrée avec ID: " + newRowId);

                // Récupérer le code de l'établissement
                String codeEts = referentielService.getCodeEtsForEtablissement(centreSanteUtilisateur);
                Log.d("CHOIXETABLISSEMENTACTIVITYSTEPPER", "setupButtonListener: codeEts"+codeEts);
                Log.d("CHOIXETABLISSEMENTACTIVITYSTEPPERCENTRE", "setupButtonListener: codeEts"+centreSanteUtilisateur);
                codeEts = sharedPrefManager.getCodeEts();
                Log.d("CHOIXETABLISSEMENTACTIVITYSTEPPERPREF", "setupButtonListener: codeEts"+codeEts);
                if (codeEts != null && !codeEts.isEmpty()) {
                    // Afficher un dialogue de progression
                    final AlertDialog progressDialog = new AlertDialog.Builder(this)
                            .setTitle("Synchronisation")
                            .setMessage("Synchronisation en cours patientez...")
                            .setCancelable(false)
                            .create();
                    //progressDialog.show();
                    if (isNetworkAvailable()) {

                        // Appeler l'API pour envoyer les données
                        ApiService apiService = new ApiService(this);
                        apiService.registerAgent(
                                nomUtilisateur,
                                prenomUtilisateur,
                                numeroUtilisateur,
                                codeEts,
                                matriculeUtilisateur,
                                photoPath,
                                idFamoco,
                                new ApiService.ApiCallback() {
                                    @Override
                                    public void onSuccess(JSONObject response) {
                                        runOnUiThread(() -> {
                                            progressDialog.dismiss();

                                            // Vérifier si un message existe
                                            String message = response.optString("message", "Données synchronisées avec succès");

                                            // Vérifier s'il y a un token ou un utilisateur
                                            boolean hasToken = response.has("token");
                                            boolean hasUser = response.has("user");

                                            if (hasToken || hasUser) {
                                                Toast.makeText(InscriptionStepperActivity.this,
                                                        "Synchronisation réussie: " + message,
                                                        Toast.LENGTH_LONG).show();

                                                // Marquer l'agent comme synchronisé dans la base de données locale
                                                markAgentAsSynchronized(matriculeUtilisateur);
                                                sendDataToApiCentre(formData, matriculeUtilisateur);

                                                // Sauvegarder le token si présent pour de futures authentifications
                                            /*if (hasToken) {
                                                String token = response.getString("token");
                                                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                                                prefs.edit().putString("auth_token", token).apply();
                                                Log.d(TAG, "Token sauvegardé: " + token);
                                            }*/
                                            } else {
                                                Toast.makeText(InscriptionStepperActivity.this,
                                                        "Réponse reçue, mais manque d'informations: " + message,
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(String message) {
                                        runOnUiThread(() -> {
                                            progressDialog.dismiss();
                                            Log.e(TAG, "Erreur API: " + message);

                                            handleSmsSending(smsManager, nomUtilisateur, prenomUtilisateur, matriculeUtilisateur, numeroUtilisateur, String.valueOf(latitudeFacade), String.valueOf(longitudeFacade), String.valueOf(latitudeInterieur), String.valueOf(longitudeInterieur));

                                            // Afficher une alerte avec le message d'erreur
                                            /*new AlertDialog.Builder(InscriptionStepperActivity.this)
                                                    .setTitle("Erreur de synchronisation")
                                                    .setMessage("Impossible d'envoyer les données au serveur: " + message +
                                                            "\n\nLes données sont sauvegardées localement et seront synchronisées automatiquement plus tard.")
                                                    .setPositiveButton("OK", null)
                                                    .show();*/
                                        });
                                    }
                                }
                        );

                    }else{
                        handleSmsSending(smsManager, nomUtilisateur, prenomUtilisateur, matriculeUtilisateur, numeroUtilisateur, String.valueOf(latitudeFacade), String.valueOf(longitudeFacade), String.valueOf(latitudeInterieur), String.valueOf(longitudeInterieur));
                    }

                } else {
                    Log.e(TAG, "Code établissement non trouvé pour: " + centreSanteUtilisateur);
                    Toast.makeText(this, "Code établissement non trouvé", Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(this, "Données enregistrées localement", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Log.e(TAG, "Erreur lors de l'enregistrement des données");
                Toast.makeText(this, "Erreur lors de l'enregistrement des données", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de l'enregistrement dans SQLite", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void handleSmsSending(DataSMSManager smsManager,  String nom,
                                  String prenoms,
                                  String matricule, String contact, String lat, String lng, String latInt, String lngInt) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                // Variable pour la date
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());

                // Variable pour l'heure
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH'h'mm'm'ss's'", Locale.getDefault());
                String currentTime = timeFormat.format(new Date());

                String code_ets = sharedPrefManager.getCodeEts();
                String downloadedFileName = sharedPrefManager.getDownloadedFileName();

                String regionEts = referentielService.getRegionomByEtablissement(nomEtablissement);
                //String idFamoco = utilsInfos.recupererIdAppareil();
                Log.d("Famoco", "ID famoco est : "+idFamoco);
                //int id = RegionUtils.getRegionid(downloadedFileName);
                int id = regionCoordUtils.getIdForRegion(regionEts);
                String idString = Integer.toString(id);
                String lettreCle = "i";
                //smsManager.sendAgentAndCentreViaSMS(lettreCle,nom, prenoms, matricule, code_ets, idFamoco, currentDate,currentTime, contact, idString, lat, lng, latInt, lngInt);

                smsManager.sendAgentAndCentreViaSMS(
                        this, lettreCle, nom, prenoms, matricule, code_ets,
                        idFamoco, currentDate, currentTime, contact, idString,
                        lat, lng, latInt, lngInt,
                        new DataSMSManager.SMSSendCallback() {
                            @Override
                            public void onSMSSendSuccess() {
                                Log.d("SMS_SYNC", "SMS envoyé avec succès pour l'agent: " + matricule);

                                // Marquer l'agent comme "envoyé par SMS" (is_synchronized = 2)
                                markAgentAsDoNotSendBySMS(matricule);

                                // Afficher une notification

                                Toast.makeText(InscriptionStepperActivity.this, "Données envoyées par SMS avec succès",
                                        Toast.LENGTH_SHORT).show();



                            }

                            @Override
                            public void onSMSSendFailure(String errorMessage) {
                                Log.e("SMS_SYNC", "Échec d'envoi SMS: " + errorMessage);

                                // Afficher une notification d'erreur

                                Toast.makeText(InscriptionStepperActivity.this, "Échec d'envoi SMS: " + errorMessage,
                                        Toast.LENGTH_SHORT).show();

                            }
                        }
                );
                //fseServiceDb.updateStatusProgres(numTrans);
                //showSuccessDialog();
            } catch (Exception e) {
                Toast.makeText(this, "Échec envoi SMS", Toast.LENGTH_SHORT).show();
                //btnSend.setVisibility(View.VISIBLE);
                //progressBar.setVisibility(View.GONE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    private void markAgentAsDoNotSendBySMS(String matricule) {
        dbHelper helper = new dbHelper(this);
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
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void sendDataToApiCentre(Map<String, Object> formData, String matriculeUtilisateur) {
        OkHttpClient client = new OkHttpClient();

        try {
            //File fileFacade = getFileFromUri(photoUriFacade);
            //File fileInterieur = getFileFromUri(photoUriInterieur);

            File fileFacade = compressImage(getFileFromUri(photoUriFacade));
            File fileInterieur = compressImage(getFileFromUri(photoUriInterieur));

            if (fileFacade == null || !fileFacade.exists() || fileInterieur == null || !fileInterieur.exists()) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Erreur: Fichiers image introuvables", Toast.LENGTH_LONG).show());
                return;
            }

            Log.d(TAG, "Fichier façade: " + fileFacade.getAbsolutePath() + " (" + fileFacade.length() + " bytes)");
            Log.d(TAG, "Fichier intérieur: " + fileInterieur.getAbsolutePath() + " (" + fileInterieur.length() + " bytes)");

            MediaType mediaType = MediaType.parse("image/jpeg");
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

            builder.addFormDataPart("nom_agent", (String) formData.get("nom_agent"));
            builder.addFormDataPart("contact", (String) formData.get("contact"));
            builder.addFormDataPart("code_ets", (String) formData.get("code_ets"));
            Log.e(TAG, "idFamoco"+formData.get("idFamoco"));
            builder.addFormDataPart("id_famoco", (String) formData.get("idFamoco"));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            // Obtenir la date et l'heure actuelles formatées
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
            Request request = new Request.Builder()
                    .url("http://57.128.30.4:8090/api/v1/addEtablissementLocalisation")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur réseau", e);
                    runOnUiThread(() -> Toast.makeText(InscriptionStepperActivity.this,
                            "Erreur réseau: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.d(TAG, "API Response: Code: " + response.code() + ", Body: " + responseBody);

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(InscriptionStepperActivity.this,
                                    "Données envoyées avec succès pour le centre !", Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(InscriptionStepperActivity.this,
                                    "Erreur: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation envoi", e);
            runOnUiThread(() -> Toast.makeText(this,
                    "Erreur préparation: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }


    /**
     * Convertit un URI en objet File
     * @param uri URI de l'image
     * @return Objet File correspondant ou null en cas d'erreur
     */
    /*private File getFileFromUri(Uri uri) {
        if (uri == null) {
            Log.e(TAG, "URI null passé à getFileFromUri");
            return null;
        }

        try {
            // Vérifier d'abord si c'est un URI de fichier direct
            if ("file".equals(uri.getScheme())) {
                return new File(uri.getPath());
            }

            // Sinon, essayez d'utiliser le ContentResolver
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(column_index);
                cursor.close();

                if (path != null) {
                    return new File(path);
                }
            }

            // Si tout a échoué, essayez de copier le fichier dans un fichier temporaire
            String fileName = getFileNameFromUri(uri);
            if (fileName == null) {
                fileName = "temp_" + System.currentTimeMillis() + ".jpg";
            }

            File outputFile = new File(getCacheDir(), fileName);
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            if (inputStream != null) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                outputStream.close();

                Log.d(TAG, "Fichier créé à partir de l'URI: " + outputFile.getAbsolutePath());
                return outputFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la conversion de l'URI en File", e);
        }

        return null;
    }*/

    private File getFileFromUri(Uri uri) {
        if (uri == null) {
            Log.e(TAG, "URI null passé à getFileFromUri");
            return null;
        }

        try {
            // Vérifier d'abord si c'est un URI de fichier direct
            if ("file".equals(uri.getScheme())) {
                return new File(uri.getPath());
            }

            // Pour les URIs de FileProvider ou autres ContentProviders
            // Créer directement un fichier temporaire et copier le contenu
            String fileName = getFileNameFromUri(uri);
            if (fileName == null) {
                fileName = "temp_" + System.currentTimeMillis() + ".jpg";
            }

            File outputFile = new File(getCacheDir(), fileName);
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                if (inputStream != null) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    Log.d(TAG, "Fichier créé à partir de l'URI: " + outputFile.getAbsolutePath());
                    return outputFile;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la conversion de l'URI en File", e);
        }

        return null;
    }

    /**
     * Récupère le nom de fichier à partir d'un URI
     */
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void create_table(){
        dbHelper helper = new dbHelper(this);
        // Créer la table si elle n'existe pas
        SQLiteDatabase db = helper.getWritableDatabase();
        String createTableQuery = "CREATE TABLE IF NOT EXISTS agents_inscription (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nom TEXT NOT NULL, " +
                "prenom TEXT NOT NULL, " +
                "telephone TEXT NOT NULL, " +
                "matricule TEXT NOT NULL, " +
                "centre_sante TEXT, " +
                "photo_path TEXT, " +
                "empreintes INTEGER, " +
                "photo_facade_path TEXT, " +
                "photo_interieur_path TEXT, " +
                "latitude_facade REAL, " +
                "longitude_facade REAL, " +
                "latitude_interieur REAL, " +
                "longitude_interieur REAL, " +
                "date_inscription TEXT)";
        db.execSQL(createTableQuery);
    }

    /**
     * Marque un agent comme synchronisé dans la base de données
     */
    private void markAgentAsSynchronized(String matricule) {
        dbHelper helper = new dbHelper(this);
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


    @Override
    protected void onResume() {
        super.onResume();

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
    }
}
