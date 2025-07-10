package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.dbHelper;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.utils.AgentManager;
import ci.technchange.prestationscmu.utils.ApiService;
import ci.technchange.prestationscmu.utils.DataSMSManager;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;

public class ProfileEditActivity extends AppCompatActivity {

    private static final String TAG = "ProfileEditActivity";

    // Composants UI
    private Toolbar toolbar;
    private ImageView ivProfilePhoto;
    private TextView tvProfileName;
    private TextView tvProfileMatricule;
    private TextView tvCentreInfo;
    private TextView tvPhotoStatus;
    private TextView tvEmpreintesStatus;

    private EditText editNomProfile;
    private EditText editPrenomProfile;
    private EditText editMatriculeProfile;
    private EditText editTelephoneProfile;

    private Button btnCancel;
    private Button btnSave;

    // Services
    private SharedPrefManager sharedPrefManager;
    private AgentManager agentManager;

    // Données
    private String originalNom = "";
    private String originalPrenom = "";
    private String originalMatricule = "";
    private String originalTelephone = "";
    private String originalCentre = "";

    private DataSMSManager smsManager;

    private ProgressDialog progressDialog;

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        // Initialiser les services
        sharedPrefManager = new SharedPrefManager(this);
        agentManager = AgentManager.getInstance(this);
        smsManager = new DataSMSManager();

        // Initialiser les vues
        initializeViews();

        // Configurer la toolbar
        setupToolbar();

        // Charger les données
        loadProfileData();

        // Configurer les listeners
        setupListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        //ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        //tvProfileName = findViewById(R.id.tvProfileName);
        //tvProfileMatricule = findViewById(R.id.tvProfileMatricule);
        tvCentreInfo = findViewById(R.id.tvCentreInfo);
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus);
        tvEmpreintesStatus = findViewById(R.id.tvEmpreintesStatus);

        editNomProfile = findViewById(R.id.editNomProfile);
        editPrenomProfile = findViewById(R.id.editPrenomProfile);
        editMatriculeProfile = findViewById(R.id.editMatriculeProfile);
        editTelephoneProfile = findViewById(R.id.editTelephoneProfile);

        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void loadProfileData() {
        // Récupérer les données depuis l'Intent ou SharedPreferences
        //loadDataFromIntent();
        //loadDataFromSharedPreferences();
        loadDataFromDatabase();

        // Remplir les champs du formulaire
        fillFormFields();

        // Charger la photo de profil
        //loadProfilePhoto();

        // Mettre à jour les informations d'affichage
        //updateDisplayInfo();

        // Charger les informations du compte
        loadAccountInfo();
    }

    private void loadDataFromIntent() {
        // Récupérer les données passées depuis MainActivity
        Intent intent = getIntent();
        if (intent != null) {
            originalNom = intent.getStringExtra("AGENT_NOM") != null ? intent.getStringExtra("AGENT_NOM") : "";
            originalPrenom = intent.getStringExtra("AGENT_PRENOM") != null ? intent.getStringExtra("AGENT_PRENOM") : "";
            originalMatricule = intent.getStringExtra("AGENT_MATRICULE") != null ? intent.getStringExtra("AGENT_MATRICULE") : "";
            originalTelephone = intent.getStringExtra("AGENT_TELEPHONE") != null ? intent.getStringExtra("AGENT_TELEPHONE") : "";
            originalCentre = intent.getStringExtra("AGENT_CENTRE") != null ? intent.getStringExtra("AGENT_CENTRE") : "";
        }
    }

    private void loadDataFromSharedPreferences() {
        // Si les données de l'Intent sont vides, essayer SharedPreferences
        if (originalMatricule.isEmpty()) {
            originalMatricule = sharedPrefManager.getCodeAgent();
        }

        if (originalNom.isEmpty() || originalPrenom.isEmpty()) {
            String savedAgentInfo = sharedPrefManager.getCodeAndNomAgent();
            if (savedAgentInfo != null && !savedAgentInfo.isEmpty()) {
                // Parser le format "CODE - NOM PRENOM"
                String[] parts = savedAgentInfo.split(" - ");
                if (parts.length >= 2) {
                    String[] nameParts = parts[1].trim().split(" ");
                    if (nameParts.length >= 1) {
                        originalPrenom = nameParts[0];
                        if (nameParts.length >= 2) {
                            originalNom = nameParts[1];
                        }
                    }
                }
            }
        }
    }

    private void loadDataFromDatabase() {
        // Récupérer le matricule depuis SharedPreferences (comme identifiant de base)
        String matriculeFromPrefs = sharedPrefManager.getCodeAgent();

        if (matriculeFromPrefs != null && !matriculeFromPrefs.isEmpty()) {
            // Charger TOUTES les données depuis la base de données
            dbHelper helper = new dbHelper(this);
            SQLiteDatabase db = helper.getReadableDatabase();

            try {
                String query = "SELECT * FROM agents_inscription WHERE matricule = ?";
                android.database.Cursor cursor = db.rawQuery(query, new String[]{matriculeFromPrefs});

                if (cursor != null && cursor.moveToFirst()) {
                    // Récupérer tous les champs depuis la base
                    int nomIndex = cursor.getColumnIndex("nom");
                    int prenomIndex = cursor.getColumnIndex("prenom");
                    int matriculeIndex = cursor.getColumnIndex("matricule");
                    int telephoneIndex = cursor.getColumnIndex("telephone");
                    int centreSanteIndex = cursor.getColumnIndex("centre_sante");

                    if (nomIndex != -1) originalNom = cursor.getString(nomIndex) != null ? cursor.getString(nomIndex) : "";
                    if (prenomIndex != -1) originalPrenom = cursor.getString(prenomIndex) != null ? cursor.getString(prenomIndex) : "";
                    if (matriculeIndex != -1) originalMatricule = cursor.getString(matriculeIndex) != null ? cursor.getString(matriculeIndex) : "";
                    if (telephoneIndex != -1) originalTelephone = cursor.getString(telephoneIndex) != null ? cursor.getString(telephoneIndex) : "";
                    if (centreSanteIndex != -1) originalCentre = cursor.getString(centreSanteIndex) != null ? cursor.getString(centreSanteIndex) : "";

                    Log.d(TAG, "Données chargées depuis la base: " + originalNom + " " + originalPrenom + " (" + originalMatricule + ")");
                    cursor.close();
                } else {
                    Log.w(TAG, "Aucun agent trouvé avec le matricule: " + matriculeFromPrefs);
                    // Données par défaut vides
                    originalNom = "";
                    originalPrenom = "";
                    originalMatricule = matriculeFromPrefs;
                    originalTelephone = "";
                    originalCentre = "";
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du chargement depuis la base", e);
                // En cas d'erreur, utiliser le matricule des préférences
                originalMatricule = matriculeFromPrefs;
            } finally {
                if (db != null) {
                    db.close();
                }
            }
        } else {
            Log.w(TAG, "Aucun matricule trouvé dans SharedPreferences");
            // Valeurs par défaut
            originalNom = "";
            originalPrenom = "";
            originalMatricule = "";
            originalTelephone = "";
            originalCentre = "";
        }
    }


    private void fillFormFields() {
        editNomProfile.setText(originalNom);
        editPrenomProfile.setText(originalPrenom);
        editMatriculeProfile.setText(originalMatricule);
        editTelephoneProfile.setText(originalTelephone);
    }

    /*private void loadProfilePhoto() {
        // Charger la photo de profil avec AgentManager
        if (!originalMatricule.isEmpty()) {
            agentManager.applyCircularResizedAgentPhotoToImageView(
                    ivProfilePhoto,
                    originalMatricule,
                    200, 200,
                    true
            );
        }
    }*/

    /*private void updateDisplayInfo() {
        // Mettre à jour le nom affiché en haut
        String fullName = originalPrenom + " " + originalNom;
        tvProfileName.setText(fullName.trim());

        // Mettre à jour le matricule affiché
        tvProfileMatricule.setText("Matricule: " + originalMatricule);
    }*/

    private void loadAccountInfo() {
        // Charger les informations du compte (centre, photo, empreintes)

        // Centre de santé (vous pouvez adapter selon votre logique)
        if (!originalCentre.isEmpty()) {
            tvCentreInfo.setText(originalCentre);
        } else {
            tvCentreInfo.setText("Non renseigné");
        }

        // Statut de la photo
        boolean hasPhoto = agentManager.hasAgentPhoto(originalMatricule);
        if (hasPhoto) {
            tvPhotoStatus.setText("✓ Disponible");
            tvPhotoStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvPhotoStatus.setText("✗ Non disponible");
            tvPhotoStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        // Statut des empreintes (vous pouvez adapter selon votre logique)
        boolean hasFingerprints = checkFingerprintsInDatabase(originalMatricule);
        if (hasFingerprints) {
            tvEmpreintesStatus.setText("✓ Enregistrées");
            tvEmpreintesStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvEmpreintesStatus.setText("✗ Non enregistrées");
            tvEmpreintesStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private boolean checkFingerprintsInDatabase(String matricule) {
        // Vérifier si des empreintes existent pour cet agent
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();

        try {
            String query = "SELECT COUNT(*) FROM empreintes WHERE matricule = ?";
            android.database.Cursor cursor = db.rawQuery(query, new String[]{matricule});

            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                cursor.close();
                return count > 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification des empreintes", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }

        return false;
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> {
            // Fermer l'activité sans sauvegarder
            finish();
        });

        btnSave.setOnClickListener(v -> {

            // Afficher la dialog de chargement AVANT de commencer le processus
            showLoadingDialog();

            // Désactiver le bouton pour éviter les double-clics
            btnSave.setEnabled(false);
            if (validateAndSaveProfile()) {
                Toast.makeText(this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show();
                navigateToMainActivity("Données envoyées par SMS avec succès");
                //finish();
            }else {
                Toast.makeText(this, "echec", Toast.LENGTH_SHORT).show();
                // En cas d'échec de validation, cacher la dialog et réactiver le bouton
                hideLoadingDialog();
                btnSave.setEnabled(true);
            }
        });
    }

    // Méthode pour afficher la dialog de chargement
    private void showLoadingDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Sauvegarde en cours...");
            progressDialog.setCancelable(false); // Empêcher l'annulation
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        progressDialog.show();
    }

    // Méthode pour cacher la dialog de chargement
    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    // Méthode pour naviguer vers MainActivity
    private void navigateToMainActivity(String message) {
        runOnUiThread(() -> {
            hideLoadingDialog();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            // Naviguer vers MainActivity et vider la pile d'activités
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);
            finish();
        });
    }

    // Méthode pour gérer les erreurs
    private void handleError(String errorMessage) {
        runOnUiThread(() -> {
            hideLoadingDialog();
            btnSave.setEnabled(true);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    private boolean validateAndSaveProfile() {
        String nom = editNomProfile.getText().toString().trim();
        String prenom = editPrenomProfile.getText().toString().trim();
        String matricule = editMatriculeProfile.getText().toString().trim();
        String telephone = editTelephoneProfile.getText().toString().trim();


        Log.d(TAG, "=== DÉBUT VALIDATION ===");
        Log.d(TAG, "Matricule saisi: '" + matricule + "'");
        Log.d(TAG, "Matricule original: '" + originalMatricule + "'");
        Log.d(TAG, "Matricules sont égaux: " + matricule.equals(originalMatricule));
        // Validation des champs
        if (nom.isEmpty()) {
            editNomProfile.setError("Le nom est requis");
            editNomProfile.requestFocus();
            return false;
        }

        if (prenom.isEmpty()) {
            editPrenomProfile.setError("Le prénom est requis");
            editPrenomProfile.requestFocus();
            return false;
        }

        if (matricule.isEmpty()) {
            editMatriculeProfile.setError("Le matricule est requis");
            editMatriculeProfile.requestFocus();
            return false;
        }

        if (telephone.isEmpty()) {
            editTelephoneProfile.setError("Le numéro de téléphone est requis");
            editTelephoneProfile.requestFocus();
            return false;
        }

        // Validation du format du téléphone
        if (!isValidPhoneNumber(telephone)) {
            editTelephoneProfile.setError("Format de téléphone invalide");
            editTelephoneProfile.requestFocus();
            return false;
        }

        // Vérifier si le matricule a changé et s'il existe déjà
        if (!matricule.equals(originalMatricule)) {
            Log.d(TAG, "Le matricule a changé, vérification en cours...");

            boolean exists1 = agentManager.isMatriculeExists(matricule, originalMatricule);
            Log.d(TAG, "isMatriculeExists result: " + exists1);

            if (exists1) {
                Log.d(TAG, "BLOQUÉ par isMatriculeExists");
                editMatriculeProfile.setError("Ce matricule existe déjà");
                editMatriculeProfile.requestFocus();
                return false;
            }

            boolean exists2 = agentManager.isMatriculeExistsInscription(matricule);
            Log.d(TAG, "isMatriculeExistsInscription result: " + exists2);

            if (exists2) {
                Log.d(TAG, "BLOQUÉ par isMatriculeExistsInscription");
                editMatriculeProfile.setError("Ce matricule existe déjà");
                editMatriculeProfile.requestFocus();
                return false;
            }

            Log.d(TAG, "Matricule validé, pas de doublon trouvé");
        } else {
            Log.d(TAG, "Matricule inchangé, pas de vérification nécessaire");
        }

        Log.d(TAG, "=== VALIDATION RÉUSSIE ===");



        // Sauvegarder les données
        return saveProfileData(nom, prenom,matricule, telephone);
    }



    private boolean isMatriculeExistsInscription(String matricule) {
        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        Log.d("isMatriculeExistsInscription", matricule);

        try {
            // Requête pour compter les agents avec ce matricule,
            String query = "SELECT COUNT(*) FROM agents_inscription WHERE matricule = ?";
            android.database.Cursor cursor = db.rawQuery(query, new String[]{matricule});

            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, String.valueOf(count));
                cursor.close();
                return count > 0; // Si count > 0, le matricule existe déjà
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification du matricule", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }

        return false; // En cas d'erreur, considérer que le matricule n'existe pas
    }

    private boolean isValidPhoneNumber(String phone) {
        // Validation simple du numéro de téléphone
        return phone.matches("^[+\\d\\s\\-()]+$") && phone.length() >= 8;
    }

    private boolean saveProfileData(String nom, String prenom,String matricule, String telephone) {
        final boolean[] status_save = {true};
        try {
            if (isNetworkAvailable()){
                // Sauvegarder dans la base de données

                    sharedPrefManager.setagentName(nom, prenom);
                    sharedPrefManager.setCodeAgent(matricule);
                    ApiService apiService = new ApiService(this);

                    apiService.updateAgent(nom, prenom, telephone, matricule, originalMatricule, new ApiService.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            updateAgentInDatabase(nom, prenom,matricule, telephone);
                            status_save[0] = true;
                            runOnUiThread(() -> {
                                //navigateToMainActivity("Profil mis à jour avec succès");
                                Toast.makeText(ProfileEditActivity.this,"Modifications enrégistrées",Toast.LENGTH_SHORT).show();
                            });

                        }

                        @Override
                        public void onError(String message) {

                            boolean sms_status = handleSmsSending(smsManager,originalMatricule,nom,prenom,matricule,telephone);

                            if (sms_status == true){
                                updateAgentInDatabase(nom, prenom,matricule, telephone);
                                status_save[0] = sms_status;
                            }
                            //Toast.makeText(ProfileEditActivity.this,"Modifications echoué",Toast.LENGTH_SHORT).show();
                            Log.e("ProfileEditActivity",message);
                        }
                    });
                    Log.d(TAG, "Profil sauvegardé avec succès");
                    return status_save[0];

            }else{
                boolean sms_status = handleSmsSending(smsManager,originalMatricule,nom,prenom,matricule,telephone);
                if (sms_status == true){
                    updateAgentInDatabase(nom, prenom,matricule, telephone);
                    return true;
                }

                return sms_status;
            }


        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la sauvegarde du profil", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private boolean handleSmsSending(DataSMSManager smsManager,String ancien_code, String nom, String prenoms,String code_agac, String contact) {
        System.out.print("SMS=>>>>>>>>>>>>>");
        final boolean[] status = {true};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                /*SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String currentDate = sdf.format(new Date());
                String code_ets = sharedPrefManager.getCodeEts();
                String downloadedFileName = sharedPrefManager.getDownloadedFileName();
                String idFamoco = utilsInfos.recupererIdAppareil();
                Log.d("Famoco", "ID famoco est : "+idFamoco);
                int id = RegionUtils.getRegionid(downloadedFileName);
                String idString = Integer.toString(id);

                Date maintenant = new Date();
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                String code_agac = sharedPrefManager.getCodeAgent();
                String lat = String.valueOf(latitude);
                String longitude = String.valueOf(longit);
                FseAmbulatoire fseExistante = fseServiceDb.getFseAmbulatoireByNumTrans(numTrans);
                String heure = format.format(maintenant);

                // Log des valeurs avant envoi
                Log.d("SMS_FINAL", "=== VALEURS AVANT ENVOI SMS ===");
                Log.d("SMS_FINAL", "identifiant: " + identifiant);
                Log.d("SMS_FINAL", "numTrans: " + numTrans);
                Log.d("SMS_FINAL", "affection1: " + affection1);
                Log.d("SMS_FINAL", "affection2: " + affection2);
                Log.d("SMS_FINAL", "acte1: " + acte1);
                Log.d("SMS_FINAL", "acte2: " + acte2);
                Log.d("SMS_FINAL", "acte3: " + acte3);
                Log.d("SMS_FINAL", "===============================");*/

                String lettreCle = "up";
                smsManager.updateProfilSMS(this, lettreCle,ancien_code, nom, prenoms, code_agac, contact, new DataSMSManager.SMSSendCallback() {
                    @Override
                    public void onSMSSendSuccess() {
                        status[0] = true;
                        Toast.makeText(ProfileEditActivity.this, "Données envoyées avec succès",
                                Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onSMSSendFailure(String errorMessage) {
                        status[0] = false;
                    }
                });

            } catch (Exception e) {
                Toast.makeText(this, "Échec envoi SMS", Toast.LENGTH_SHORT).show();
                //enregisterFse.setVisibility(View.VISIBLE);
                //progressBar.setVisibility(View.GONE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
        return status[0];
    }

    private boolean updateAgentInDatabase(String nom, String prenom, String matricule, String telephone) {
        if (originalMatricule.isEmpty()) {
            Log.w(TAG, "Matricule original vide, impossible de mettre à jour la base de données");
            return false;
        }

        dbHelper helper = new dbHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();

        try {
            // Démarrer une transaction pour assurer la cohérence
            db.beginTransaction();

            // 1. D'abord récupérer le ROWID du premier agent correspondant
            Cursor cursor = db.rawQuery("SELECT ROWID FROM agents_inscription WHERE matricule = ? LIMIT 1",
                    new String[]{originalMatricule});

            if (!cursor.moveToFirst()) {
                Log.w(TAG, "Aucun agent trouvé avec le matricule: " + originalMatricule);
                cursor.close();
                return false;
            }

            long rowId = cursor.getLong(0);
            cursor.close();

            // 1. Mettre à jour la table agents_inscription
            ContentValues agentValues = new ContentValues();
            agentValues.put("nom", nom);
            agentValues.put("prenom", prenom);
            agentValues.put("matricule", matricule);
            agentValues.put("telephone", telephone);



            int agentRowsUpdated = db.update(
                    "agents_inscription",
                    agentValues,
                    "ROWID = ?",
                    new String[]{String.valueOf(rowId)}
            );

            if (agentRowsUpdated > 0) {
                Log.d(TAG, "Table agents_inscription mise à jour: " + agentRowsUpdated + " ligne(s)");

                // 2. Si le matricule a changé, mettre à jour aussi la table empreintes
                if (!matricule.equals(originalMatricule)) {
                    ContentValues empreintesValues = new ContentValues();
                    empreintesValues.put("matricule", matricule);

                    int empreintesRowsUpdated = db.update(
                            "empreintes",
                            empreintesValues,
                            "matricule = ?",
                            new String[]{originalMatricule}
                    );

                    Log.d(TAG, "Table empreintes mise à jour: " + empreintesRowsUpdated + " ligne(s)");
                }

                // Valider la transaction
                db.setTransactionSuccessful();
                return true;

            } else {
                Log.w(TAG, "Aucune ligne mise à jour dans agents_inscription pour le matricule: " + originalMatricule);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la mise à jour des tables", e);
            return false;
        } finally {
            // Terminer la transaction
            if (db != null) {
                try {
                    db.endTransaction();
                    db.close();
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la fermeture de la transaction", e);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Vérifier s'il y a des modifications non sauvegardées
        if (hasUnsavedChanges()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Modifications non sauvegardées")
                    .setMessage("Voulez-vous sauvegarder vos modifications avant de quitter ?")
                    .setPositiveButton("Sauvegarder", (dialog, which) -> {
                        if (validateAndSaveProfile()) {
                            navigateToMainActivity("Données envoyées par SMS avec succès");
                            //finish();
                        }
                    })
                    .setNegativeButton("Quitter sans sauvegarder", (dialog, which) -> finish())
                    .setNeutralButton("Annuler", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        String currentNom = editNomProfile.getText().toString().trim();
        String currentPrenom = editPrenomProfile.getText().toString().trim();
        String currentTelephone = editTelephoneProfile.getText().toString().trim();

        return !currentNom.equals(originalNom) ||
                !currentPrenom.equals(originalPrenom) ||
                !currentTelephone.equals(originalTelephone);
    }
}