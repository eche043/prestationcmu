package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.core.dbHelper;
import ci.technchange.prestationscmu.utils.ApiService;
import ci.technchange.prestationscmu.utils.ReferentielService;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterCenterActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "RegisterCenterActivity";
    private static final int REQUEST_IMAGE_CAPTURE_FACADE = 1;
    private static final int REQUEST_IMAGE_CAPTURE_INTERIEUR = 2;
    private static final int PERMISSION_CAMERA = 101;
    private static final int REQUEST_LOCATION_PERMISSION = 102;

    // Vues
    private TextView tvTitle;
    private TextView tvMatricule;
    private AutoCompleteTextView autoCompleteCentreSante;
    private ImageView imageViewFacade;
    private ImageView imageViewInterieur;
    private ImageButton btnCaptureFacade;
    private ImageButton btnCaptureInterieur;
    private Button btnSave;
    private Button btnCancel;

    // Données
    private String matricule;
    String nom,prenom,numero;
    private String centreSante;
    private String photoPathFacade;
    private String photoPathInterieur;
    private Uri photoUriFacade;
    private Uri photoUriInterieur;
    private double latitudeFacade, longitudeFacade;
    private double latitudeInterieur, longitudeInterieur;
    private int currentPhotoRequest;

    // Services
    private ReferentielService referentielService;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String idFamoco;
    private UtilsInfosAppareil utilsInfos;
    private SharedPrefManager sharedPrefManager;
    Map<String, Object> agentInfo;

    String nomEtablissement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_center);

        utilsInfos = new UtilsInfosAppareil(this);
        utilsInfos.obtenirInformationsSysteme(this);
        sharedPrefManager = new SharedPrefManager(this);
        nomEtablissement = fetchEtablissementName();

        idFamoco = utilsInfos.recupererIdAppareil();


        // Récupérer le matricule
        /*if (getIntent().hasExtra("MATRICULE")) {
            matricule = getIntent().getStringExtra("MATRICULE");
        } else {
            // Si pas de matricule, on ne peut pas continuer
            Toast.makeText(this, "Erreur: Matricule non spécifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

         nom = getIntent().getStringExtra("NOM");
         prenom = getIntent().getStringExtra("PRENOM");
         numero = getIntent().getStringExtra("NUMERO");*/
        agentInfo = getFirstAgentInfo();

        // Initialiser les services
        referentielService = new ReferentielService(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Initialiser les vues
        initViews();

        // Configurer les listeners
        setupListeners();

        // Charger la liste des centres de santé
        loadCentreSante();
        matricule = (String) agentInfo.get("matricule");
        System.out.println("------------"+nomEtablissement+"-----------");

        // Afficher le matricule
        tvMatricule.setText("Matricule : " + matricule);
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

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvMatricule = findViewById(R.id.tvMatricule);
        autoCompleteCentreSante = findViewById(R.id.autoCompleteCentreSante);
        imageViewFacade = findViewById(R.id.imageViewFacade);
        imageViewInterieur = findViewById(R.id.imageViewInterieur);
        btnCaptureFacade = findViewById(R.id.btnCaptureFacade);
        btnCaptureInterieur = findViewById(R.id.btnCaptureInterieur);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // Préremplir avec le nom de l'établissement récupéré et désactiver la modification
        if (nomEtablissement != null && !nomEtablissement.isEmpty()) {
            autoCompleteCentreSante.setText(nomEtablissement);
            autoCompleteCentreSante.setEnabled(false);  // Désactiver les modifications
            autoCompleteCentreSante.setFocusable(false);  // Empêcher le focus
            autoCompleteCentreSante.setClickable(false);  // Empêcher les clics

            // Option supplémentaire: changer l'apparence pour montrer qu'il n'est pas modifiable
            //autoCompleteCentreSante.setBackgroundResource(android.R.drawable.edit_text_normal);
        }
    }

    private void setupListeners() {
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
                // Lancer l'activité CameraActivity pour la façade
                Intent intent = new Intent(this, CameraEtablissementActivity.class);
                intent.putExtra(CameraEtablissementActivity.EXTRA_PHOTO_TYPE, CameraEtablissementActivity.PHOTO_TYPE_FACADE);
                intent.putExtra("MATRICULE", matricule);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_FACADE);
            }
        });

        btnCaptureInterieur.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                // Lancer l'activité CameraActivity pour l'intérieur
                Intent intent = new Intent(this, CameraEtablissementActivity.class);
                intent.putExtra(CameraEtablissementActivity.EXTRA_PHOTO_TYPE, CameraEtablissementActivity.PHOTO_TYPE_INTERIEUR);
                intent.putExtra("MATRICULE", matricule);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_INTERIEUR);
            }
        });

        btnSave.setOnClickListener(v -> {
            if (validateForm()) {
                saveCenterInfo();
            }
        });

        btnCancel.setOnClickListener(v -> {
            // Retourner à la page précédente
            finish();
        });
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

    private boolean validateForm() {
        //centreSante = autoCompleteCentreSante.getText().toString().trim();
        centreSante = nomEtablissement;

        if (TextUtils.isEmpty(centreSante)) {
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
    }

    private void saveCenterInfo() {
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();
        System.out.println("saveCenterInfo: "+agentInfo.toString());
        if (agentInfo != null){

            matricule = (String) agentInfo.get("matricule");
            nom = (String) agentInfo.get("nom");
            prenom = (String) agentInfo.get("prenom");
            numero = (String) agentInfo.get("telephone");
            String photo_path = (String) agentInfo.get("photo_path");
            try {
                // Vérifier si l'agent existe
                Cursor cursor = db.query(
                        "agents_inscription",
                        null,
                        "matricule = ?",
                        new String[]{matricule},
                        null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    // Mettre à jour les informations du centre
                    ContentValues values = new ContentValues();
                    values.put("centre_sante", centreSante);
                    values.put("photo_facade_path", photoPathFacade);
                    values.put("photo_interieur_path", photoPathInterieur);
                    values.put("latitude_facade", latitudeFacade);
                    values.put("longitude_facade", longitudeFacade);
                    values.put("latitude_interieur", latitudeInterieur);
                    values.put("longitude_interieur", longitudeInterieur);

                    Map<String, Object> formData = new HashMap<>();
                    formData.put("nom_agent", nom+" "+prenom);
                    formData.put("contact", numero);
                    formData.put("nom_etablissement", centreSante);
                    formData.put("code_ets", sharedPrefManager.getCodeEts());
                    formData.put("idFamoco", idFamoco);
                    Log.d(TAG, "saveDataToSQLite: 111"+idFamoco);


                    Map<String, Double> coordFacade = new HashMap<>();
                    coordFacade.put("latitude", latitudeFacade);
                    coordFacade.put("longitude", longitudeFacade);
                    formData.put("coordonnees_facade", coordFacade);

                    Map<String, Double> coordInterieur = new HashMap<>();
                    coordInterieur.put("latitude", latitudeInterieur);
                    coordInterieur.put("longitude", longitudeInterieur);
                    formData.put("coordonnees_interieur", coordInterieur);
                    Log.d(TAG, "saveDataToSQLite: 222"+formData.toString());

                    int rowsUpdated = db.update(
                            "agents_inscription",
                            values,
                            "matricule = ?",
                            new String[]{matricule});

                    if (rowsUpdated > 0) {
                        // Afficher un message de succès
                        new AlertDialog.Builder(this)
                                .setTitle("Enregistrement réussi")
                                .setMessage("Les informations du centre ont été enregistrées avec succès.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    // Rediriger vers la page principale
                                    ApiService apiService = new ApiService(this);
                                    apiService.registerAgent(
                                            nom,
                                            prenom,
                                            numero,
                                            sharedPrefManager.getCodeEts(),
                                            matricule,
                                            photo_path,
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
                                                            Toast.makeText(RegisterCenterActivity.this,
                                                                    "Synchronisation réussie: " + message,
                                                                    Toast.LENGTH_LONG).show();

                                                            // Marquer l'agent comme synchronisé dans la base de données locale
                                                            markAgentAsSynchronized(matricule);

                                                            sendDataToApiCentre(formData, matricule);

                                                            // Sauvegarder le token si présent pour de futures authentifications
                                            /*if (hasToken) {
                                                String token = response.getString("token");
                                                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                                                prefs.edit().putString("auth_token", token).apply();
                                                Log.d(TAG, "Token sauvegardé: " + token);
                                            }*/
                                                        } else {
                                                            Toast.makeText(RegisterCenterActivity.this,
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
                                                        new AlertDialog.Builder(RegisterCenterActivity.this)
                                                                .setTitle("Erreur de synchronisation")
                                                                .setMessage("Impossible d'envoyer les données au serveur: " + message +
                                                                        "\n\nLes données sont sauvegardées localement et seront synchronisées automatiquement plus tard.")
                                                                .setPositiveButton("OK", null)
                                                                .show();
                                                    });
                                                }
                                            }
                                    );

                                    Intent intent = new Intent(RegisterCenterActivity.this, ConnexionActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        Toast.makeText(this, "Erreur lors de la mise à jour des données", Toast.LENGTH_SHORT).show();
                    }

                    cursor.close();
                } else {
                    Toast.makeText(this, "Erreur : Agent non trouvé avec ce matricule", Toast.LENGTH_SHORT).show();
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'enregistrement : " + e.getMessage());
                Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            } finally {
                db.close();
            }
        }else{

        }


    }

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

    /**
     * Récupère toutes les informations du premier agent présent dans la base de données
     * @return Un Map contenant toutes les informations de l'agent ou null si aucun agent n'est trouvé
     */
    private Map<String, Object> getFirstAgentInfo() {
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        Map<String, Object> agentInfo = null;

        try {
            // Récupérer le premier agent de la table (sans condition de matricule)
            Cursor cursor = db.query(
                    "agents_inscription",
                    null, // Sélectionner toutes les colonnes
                    null, // Pas de clause WHERE
                    null, // Pas d'arguments pour WHERE
                    null, // Pas de GROUP BY
                    null, // Pas de HAVING
                    "id ASC", // Ordre ascendant par ID pour prendre le premier enregistré
                    "1");  // Limiter à 1 résultat

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
                Log.d(TAG, "Informations du premier agent récupérées : " + agentInfo.toString());
                cursor.close();
            } else {
                Log.d(TAG, "Aucun agent trouvé dans la base de données");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des informations de l'agent", e);
        } finally {
            db.close();
        }

        return agentInfo;
    }

    private void sendDataToApiCentre(Map<String, Object> formData, String matriculeUtilisateur) {
        OkHttpClient client = new OkHttpClient();

        try {
            File fileFacade = getFileFromUri(photoUriFacade);
            File fileInterieur = getFileFromUri(photoUriInterieur);

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
                    runOnUiThread(() -> Toast.makeText(RegisterCenterActivity.this,
                            "Erreur réseau: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.d(TAG, "API Response: Code: " + response.code() + ", Body: " + responseBody);

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(RegisterCenterActivity.this,
                                    "Données envoyées avec succès pour le centre !", Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(RegisterCenterActivity.this,
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

    private File createImageFile(int requestCode) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName;

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE_FACADE:
                imageFileName = "facade_" + matricule + "_" + timeStamp;
                break;
            case REQUEST_IMAGE_CAPTURE_INTERIEUR:
                imageFileName = "interieur_" + matricule + "_" + timeStamp;
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
            switch (requestCode) {
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
        File photoFile = createImageFile(requestCode);

        if (photoFile != null) {
            Uri photoUriTemp = FileProvider.getUriForFile(
                    this,
                    "ci.technchange.prestationscmuym.fileprovider",
                    photoFile
            );

            // Stocker l'URI selon le type de photo
            switch (requestCode) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE_FACADE:
                    photoPathFacade = data.getStringExtra(CameraEtablissementActivity.EXTRA_PHOTO_PATH);
                    photoUriFacade = Uri.parse(data.getStringExtra(CameraEtablissementActivity.EXTRA_PHOTO_URI));
                    processEstablishmentPhoto(photoPathFacade, imageViewFacade, true);
                    break;
                case REQUEST_IMAGE_CAPTURE_INTERIEUR:
                    photoPathInterieur = data.getStringExtra(CameraEtablissementActivity.EXTRA_PHOTO_PATH);
                    photoUriInterieur = Uri.parse(data.getStringExtra(CameraEtablissementActivity.EXTRA_PHOTO_URI));
                    processEstablishmentPhoto(photoPathInterieur, imageViewInterieur, false);
                    break;
            }
        }
    }

    private void processEstablishmentPhoto(String imagePath, ImageView imageView, boolean isFacade) {
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
                        } else {
                            // Si pas de localisation, demander une mise à jour
                            locationManager.requestSingleUpdate(
                                    LocationManager.GPS_PROVIDER,
                                    this,
                                    null
                            );
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de l'obtention des coordonnées", e);
                    }
                }

                Toast.makeText(this, "Photo prise avec succès", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
            }
        }
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
    public void onLocationChanged(Location location) {
        if (currentPhotoRequest == REQUEST_IMAGE_CAPTURE_FACADE) {
            latitudeFacade = location.getLatitude();
            longitudeFacade = location.getLongitude();
            Log.d(TAG, "Localisation mise à jour - Facade - Lat: " + latitudeFacade + ", Long: " + longitudeFacade);
        } else if (currentPhotoRequest == REQUEST_IMAGE_CAPTURE_INTERIEUR) {
            latitudeInterieur = location.getLatitude();
            longitudeInterieur = location.getLongitude();
            Log.d(TAG, "Localisation mise à jour - Intérieur - Lat: " + latitudeInterieur + ", Long: " + longitudeInterieur);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}