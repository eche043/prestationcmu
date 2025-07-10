package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.models.Metrique;
import ci.technchange.prestationscmu.utils.ActivityTracker;
import ci.technchange.prestationscmu.utils.DataSMSManager;
import ci.technchange.prestationscmu.utils.FseServiceDb;
import ci.technchange.prestationscmu.utils.LocalisationManager;
import ci.technchange.prestationscmu.utils.MetriqueServiceDb;
import ci.technchange.prestationscmu.utils.NetworkUtils;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;

public class PreviewActivity extends AppCompatActivity {
    private static final String TAG = "PreviewActivity";
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 123;

    private FseServiceDb fseServiceDb;

    private ImageView imagePreview;
    private Button btnSend;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private String imagePath;
    String numTrans, numSecu, guid, affection1, affection2, acte1, acte2, acte3;

    private RequestQueue requestQueue;
    private static final String API_URL = "http://57.128.30.4:8090/api/v1/saveFSE";
    private SharedPrefManager sharedPrefManager;
    private ActivityTracker activityTracker;
    private static String date_fin = "";
    private MetriqueServiceDb metriqueServiceDb;
    private UtilsInfosAppareil utilsInfos ;
    private LocalisationManager localisationManager;
    private double latitude;
    private double longit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        requestQueue = Volley.newRequestQueue(this);
        sharedPrefManager = new SharedPrefManager(this);
        utilsInfos = new UtilsInfosAppareil(this);
        activityTracker = new ActivityTracker(this);
        metriqueServiceDb = MetriqueServiceDb.getInstance(this);
        localisationManager = new LocalisationManager(this);
        // Récupérer le chemin de l'image
        imagePath = getIntent().getStringExtra("imagePath");
        if (imagePath == null) {
            finish();
            return;
        }
        // Récupérer l'intent
        Intent intent = getIntent();

        // Récupérer les données
        numTrans = intent.getStringExtra("NUM_TRANS");
        numSecu = intent.getStringExtra("NUM_SECU");
        guid = intent.getStringExtra("GUID");
        affection1 = intent.getStringExtra("AFFECTION1");
        affection2 = intent.getStringExtra("AFFECTION2");
        acte1 = intent.getStringExtra("ACTE1");
        acte2 = intent.getStringExtra("ACTE2");
        acte3 = intent.getStringExtra("ACTE3");

        fseServiceDb = FseServiceDb.getInstance(this);

        // Initialiser les composants UI
        imagePreview = findViewById(R.id.imagePreview);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);

        // Configurer l'ActionBar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.preview_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        localisationManager.setLocationUpdateCallback(new LocalisationManager.LocationUpdateCallback() {

            @Override
            public void onLocationUpdated(double lat, double lng) {
                // Coordonnées reçues avec succès
                Log.d("MainActivity", "Coordonnées reçues: " + lat + ", " + lng);

                latitude = lat;
                longit = lng;
                // Utiliser les coordonnées selon vos besoins
                //utiliserCoordonnees(latitude, longitude);
            }

            @Override
            public void onLocationError(String error) {
                // Erreur lors de la récupération
                Log.e("MainActivity", "Erreur de localisation: " + error);
                Toast.makeText(PreviewActivity.this, "Erreur: " + error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPermissionRequired() {
                // Permissions nécessaires
                Log.w("MainActivity", "Permissions de localisation requises");
                //demanderPermissions();
            }
        });

        localisationManager.requestLocationUpdates();
        // Afficher l'image
        displayImage();

        // Configurer le bouton d'envoi
        btnSend.setOnClickListener(v -> uploadImage());
    }

    private void displayImage() {
        try {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imagePreview.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying image: " + e.getMessage());
        }
    }



    /*private void uploadImage() {
        // Afficher l'indicateur de chargement
        btnSend.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        /*NetworkUtils.uploadImage(this, new File(imagePath), new NetworkUtils.UploadCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    // Masquer l'indicateur de chargement
                    progressBar.setVisibility(View.GONE);

                    // Afficher le dialogue de succès
                    showSuccessDialog();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Masquer l'indicateur de chargement
                    progressBar.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);

                    // Afficher le message d'erreur
                    NetworkUtils.showErrorToast(PreviewActivity.this, error);
                });
            }
        });*/
        /*NetworkUtils.uploadImageWithRetry(this, new File(imagePath), new NetworkUtils.UploadCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    // Masquer l'indicateur de chargement
                    progressBar.setVisibility(View.GONE);

                    // Afficher le dialogue de succès
                    showSuccessDialog();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Masquer l'indicateur de chargement
                    progressBar.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);

                    // Afficher le message d'erreur
                    NetworkUtils.showErrorToast(PreviewActivity.this, error);
                });
            }
        });
    }*/

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private boolean sendDataToApi() {
        try {
            // Créer l'objet JSON principal
            JSONObject jsonBody = new JSONObject();

            // Date actuelle formattée
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());
            String code_ets = sharedPrefManager.getCodeEts();
            String downloadedFileName = sharedPrefManager.getDownloadedFileName();
            String idFamoco = utilsInfos.recupererIdAppareil();
            Log.d("Famoco", "ID famoco est : "+idFamoco);
            int id = RegionUtils.getRegionid(downloadedFileName);
            FseAmbulatoire fse = fseServiceDb.getFseAmbulatoireByNumTrans(numTrans);
            if (fse == null) {
                Toast.makeText(this, "Transaction introuvable", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Compléter les champs requis
            jsonBody.put("type_bon", fse.getType_fse());
            jsonBody.put("date_soins", currentDate);
            jsonBody.put("numTrans", fse.getNumTrans());
            jsonBody.put("numOgd", ""); // Valeur par défaut
            jsonBody.put("numSecu", fse.getNumSecu());
            jsonBody.put("numGuid", fse.getNumGuid());
            jsonBody.put("nomComplet", fse.getNomComplet());
            jsonBody.put("affection1",affection1);
            jsonBody.put("affection2",affection2);
            jsonBody.put("latitude",latitude);
            jsonBody.put("longitude",longit);
            //jsonBody.put("dateNaissance", champDateNaiss.getText().toString());
            try {
                String inputDateStr = fse.getDateNaissance();
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date birthDate = inputFormat.parse(inputDateStr);
                String formattedBirthDate = outputFormat.format(birthDate);
                jsonBody.put("dateNaissance", formattedBirthDate);
            } catch (Exception e) {
                // En cas d'erreur de format, utiliser la date telle quelle
                Log.e("DATE_FORMAT", "Erreur de conversion de la date: " + e.getMessage());
                jsonBody.put("dateNaissance", "");
            }
            jsonBody.put("nomEtablissement", fse.getNomEtablissement());
            jsonBody.put("code_etablissement", code_ets);
            jsonBody.put("sexe", fse.getSexe());


            // Construire l'objet statut_etablissement
            JSONObject statutEtablissement = new JSONObject();
            statutEtablissement.put("CMR", fse.isEtablissementCmr() ? 1 : 0);
            statutEtablissement.put("URGENCE", fse.isEtablissementUrgent() ? 1 : 0);
            statutEtablissement.put("ELOIGNEMENT", fse.isEtablissementEloignement() ? 1 : 0);
            statutEtablissement.put("REFERENCE", fse.isEtablissementEloignement() ? 1 : 0);
            statutEtablissement.put("AUTRE", fse.isInfoAutre() ? 1 : 0);
            jsonBody.put("statut_etablissement", statutEtablissement);
            Date maintenant = new Date();
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            String code_agac = sharedPrefManager.getCodeAgent();

            String heure = format.format(maintenant);
            jsonBody.put("heure_edition", heure);
            jsonBody.put("code_agac", code_agac);

            // Mettre des valeurs par défaut pour les champs obligatoires restants
            jsonBody.put("code_professionnel", "");
            jsonBody.put("nom_professionnel", "");
            jsonBody.put("specialite", "");

            // Informations complémentaires
            JSONObject infoComplementaire = new JSONObject();
            infoComplementaire.put("MATERNITE", 0);
            infoComplementaire.put("AVP", 0);
            infoComplementaire.put("AT", 0);
            infoComplementaire.put("AUTRE", 0);
            jsonBody.put("information_complementaire", infoComplementaire);

            // Observations (texte de précision si "Autre" est coché)
            //String observations = checkboxAutre.isChecked() ? champAutreLayout.getText().toString() : "ras";
            String observations = "ras";
            jsonBody.put("observations", observations);

            // Champs vides pour respecter la structure
            jsonBody.put("nom_vehicule", "");
            jsonBody.put("num_assurance_vehicule", "");
            jsonBody.put("nom_societe", "");
            jsonBody.put("num_societe", "");

            // Prestations (tableau vide pour respecter la structure)
            JSONArray prestations = new JSONArray();

            JSONObject infoPrestation1 = new JSONObject();
            infoPrestation1.put("code_acte", acte1);
            infoPrestation1.put("designation","");
            infoPrestation1.put("date_debut","");
            infoPrestation1.put("date_fin","");
            infoPrestation1.put("quantite",0);
            infoPrestation1.put("montant",0);
            infoPrestation1.put("part_cmu",0);
            infoPrestation1.put("part_ac",0);
            infoPrestation1.put("part_assure",0);

            JSONObject infoPrestation2 = new JSONObject();
            infoPrestation2.put("code_acte", acte2);
            infoPrestation2.put("designation","");
            infoPrestation2.put("date_debut","");
            infoPrestation2.put("date_fin","");
            infoPrestation2.put("quantite",0);
            infoPrestation2.put("montant",0);
            infoPrestation2.put("part_cmu",0);
            infoPrestation2.put("part_ac",0);
            infoPrestation2.put("part_assure",0);

            JSONObject infoPrestation3 = new JSONObject();
            infoPrestation3.put("code_acte", acte3);
            infoPrestation3.put("designation","");
            infoPrestation3.put("date_debut","");
            infoPrestation3.put("date_fin","");
            infoPrestation3.put("quantite",0);
            infoPrestation3.put("montant",0);
            infoPrestation3.put("part_cmu",0);
            infoPrestation3.put("part_ac",0);
            infoPrestation3.put("part_assure",0);

            prestations.put(infoPrestation1);
            prestations.put(infoPrestation2);
            prestations.put(infoPrestation3);

            jsonBody.put("prestations", prestations);

            // Type de prestation
            JSONObject typePresta = new JSONObject();
            typePresta.put("examen", 1);
            typePresta.put("exeat", 0);
            typePresta.put("refere", 0);
            typePresta.put("hospitalisation", 0);
            typePresta.put("deces", 0);
            jsonBody.put("type_presta", typePresta);
            jsonBody.put("id_region", id);
            jsonBody.put("id_famoco", idFamoco);

            // Prescriptions (tableau vide pour respecter la structure)
            JSONArray prescriptions = new JSONArray();
            jsonBody.put("prescription_medicale", prescriptions);

            // Afficher la requête dans les logs pour debug
            Log.d("API_REQUEST", "Sending to API: " + jsonBody.toString());

            // Création de la requête
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, API_URL, jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("API_RESPONSE", "Success: " + response.toString());
                            Toast.makeText(PreviewActivity.this, "FSE envoyée avec succès à l'API", Toast.LENGTH_SHORT).show();

                            // Enregistrer localement aussi
                            //saveFse();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("API_ERROR", "Error: " + error.toString());
                            Toast.makeText(PreviewActivity.this, "Erreur lors de l'envoi à l'API: " + error.getMessage(), Toast.LENGTH_LONG).show();

                            // Enregistrer localement en cas d'échec
                            //saveFse();
                        }
                    });

            // Ajouter la requête à la file d'attente
            requestQueue.add(jsonObjectRequest);
            return true;

        } catch (Exception e) {
            Log.e("API_ERROR", "Exception: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de la préparation des données: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void uploadImage() {
        btnSend.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        date_fin = activityTracker.enregistrerDateFin();
        DataSMSManager smsManager = new DataSMSManager();
        FseAmbulatoire fse = fseServiceDb.getFseAmbulatoireByNumTrans(numTrans);
        String identifiant = (numSecu == null || numSecu.isEmpty()) ? guid : numSecu;
        saveMetrique();
        if (isNetworkAvailable()) {
            if (sendDataToApi()) {
                NetworkUtils.uploadImageWithRetry(this, new File(imagePath),numTrans, new NetworkUtils.UploadCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            fseServiceDb.updateStatusProgres(numTrans);
                            progressBar.setVisibility(View.GONE);
                            showSuccessDialog();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnSend.setVisibility(View.VISIBLE);
                            NetworkUtils.showErrorToast(PreviewActivity.this, error);
                        });
                    }
                });
            } else {
                handleSmsSending(smsManager, identifiant, fse.getType_fse());
            }
        } else {
            handleSmsSending(smsManager, identifiant, fse.getType_fse());
        }
    }
    private boolean saveMetrique() {
        try {
            Metrique metrique = new Metrique();
            String downloadedFileName = sharedPrefManager.getDownloadedFileName();
            String idFamoco = utilsInfos.recupererIdAppareil();
            Log.d("Famoco", "ID famoco est : "+idFamoco);
            int id = RegionUtils.getRegionid(downloadedFileName);
            String activite = activityTracker.getLastActivity();
            String date_debut = activityTracker.getDateDebut();
            String date_fin =activityTracker.getDateFin();
            metrique.setActivite(activite);
            metrique.setDateDebut(date_debut);
            metrique.setDateFin(date_fin);
            metrique.setIdRegion(id);
            metrique.setIdFamoco(idFamoco);
            metrique.setStatusSynchro(0);


            Log.d("Metrique", metrique.toString());

            long result = metriqueServiceDb.insertMetrique(metrique);

            if (result != -1) {
                Log.d("metrique_info", "La metrique pour l'activité "+activite+" est enregistré");
                return true;
            } else {
                Log.d("metrique_info", "La metrique pour l'activité "+activite+"n'a pas pu être enregistrer");
                return false;
            }
        } catch (Exception e) {
            Log.e("metrique_info", "Erreur :"+e.getMessage());
            //Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }
    }

    private void handleSmsSending(DataSMSManager smsManager, String identifiant, String type_bon) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String currentDate = sdf.format(new Date());
                String code_ets = sharedPrefManager.getCodeEts();
                String downloadedFileName = sharedPrefManager.getDownloadedFileName();
                String idFamoco = utilsInfos.recupererIdAppareil();
                Log.d("Famoco", "ID famoco est : "+idFamoco);
                int id = RegionUtils.getRegionid(downloadedFileName);
                String idString = Integer.toString(id);
                String lettreCle = "p";
                Date maintenant = new Date();
                String lat = String.valueOf(latitude);
                String longitude = String.valueOf(longit);
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                String code_agac = sharedPrefManager.getCodeAgent();

                String heure = format.format(maintenant);
                smsManager.sendDataViaSMS(lettreCle,identifiant, numTrans, affection1, affection2, acte1, acte2, acte3,currentDate,code_ets,idString,idFamoco,code_agac,heure,lat,longitude, type_bon);
                fseServiceDb.updateStatusProgres(numTrans);
                showSuccessDialog();
            } catch (Exception e) {
                Toast.makeText(this, "Échec envoi SMS", Toast.LENGTH_SHORT).show();
                btnSend.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }
    /*private void uploadImage() {
        // Afficher l'indicateur de chargement
        btnSend.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        DataSMSManager smsManager = new DataSMSManager();

        String identifiant = (numSecu == null || numSecu.isEmpty()) ? guid : numSecu;


        if (isNetworkAvailable()) {

            if (sendDataToApi()) {
                NetworkUtils.uploadImageWithRetry(this, new File(imagePath), new NetworkUtils.UploadCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            // Masquer l'indicateur de chargement
                            progressBar.setVisibility(View.GONE);

                            // Afficher le dialogue de succès
                            showSuccessDialog();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            // Masquer l'indicateur de chargement
                            progressBar.setVisibility(View.GONE);
                            btnSend.setVisibility(View.VISIBLE);

                            // Afficher le message d'erreur
                            NetworkUtils.showErrorToast(PreviewActivity.this, error);
                        });
                    }
                });
            }else{
                Log.d(TAG, "SendSMS: ");
                System.out.println(identifiant);
                smsManager.sendDataViaSMS(identifiant, numTrans, affection1, affection2,acte1,acte2,acte3);
                showSuccessDialog();
            }

        }else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Demander la permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            } else {
                Log.d("TAGSMS", "setupListeners: IN");
                // Permission déjà accordée, envoyer le SMS
                smsManager.sendDataViaSMS(identifiant, numTrans, affection1, affection2,acte1,acte2,acte3);
                showSuccessDialog();
            }
        }


    }**/

    private void showSuccessDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_success);
        dialog.setCancelable(false);

        Button btnOk = dialog.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();

            // Retourner à l'écran d'accueil
            Intent intent = new Intent(PreviewActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}