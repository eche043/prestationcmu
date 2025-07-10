package ci.technchange.prestationscmu.views;

import static com.tom_roush.fontbox.ttf.IndexToLocationTable.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.adapter.ProfessionnelAdapter;
import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.models.ActeMedical;
import ci.technchange.prestationscmu.models.Affection;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.models.Metrique;
import ci.technchange.prestationscmu.models.Professionel;
import ci.technchange.prestationscmu.utils.ActivityTracker;
import ci.technchange.prestationscmu.utils.CodeActeAdapter;
import ci.technchange.prestationscmu.utils.CodeActeAutoCompleteAdapter;
import ci.technchange.prestationscmu.utils.DataSMSManager;
import ci.technchange.prestationscmu.utils.FseServiceDb;
import ci.technchange.prestationscmu.utils.LocalisationManager;
import ci.technchange.prestationscmu.utils.MetriqueServiceDb;
import ci.technchange.prestationscmu.utils.NetworkUtils;
import ci.technchange.prestationscmu.utils.ReferentielService;

import ci.technchange.prestationscmu.models.Professionel;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;

public class FseFiniliastionSoinsAmbulatoire extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 123;

    private Dialog successDialog;
    private AutoCompleteTextView autoCompleteCodeActe, autoCompleteProfSante, autoCompleteAffection1, autoCompleteAffection2;

    private Spinner  spinnerCodeAffection1,spinnerCodeAffection2, spinnerCodeActe ;

    private String codeActe, codeaffection1, codeaffection2, codeprofessionnel,libelleprofessionnel, libelleActe, titreActe;
    private ImageButton btnAddActe;
    private ListView listViewActe;
    private ActePrestationAdapter adapterActe;
    private List<ActeMedical> prestationList;
    int compteurActe=0;
    double tarifActe;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView selectedImageView;
    private Uri photoUri;
    private Button enregisterFse;

    private FseServiceDb fseServiceDb;

    private String capturedImagePath;
    private ProgressBar progressBar;
    private DataSMSManager smsManager;
    private RequestQueue requestQueue;
    private ActivityTracker activityTracker;
    private MetriqueServiceDb metriqueServiceDb;
    private UtilsInfosAppareil utilsInfos;
    String numTrans, numSecu, guid, affection1, affection2, acte1, acte2, acte3;
    private SharedPrefManager sharedPrefManager;
    private static final String API_URL = "http://57.128.30.4:8090/api/v1/saveFSE";
    String idFse,TransactionFse;
    private LocalisationManager localisationManager;

    private String nomEtablissement;

// Dans la classe FseFiniliastionSoinsAmbulatoire, ajoutez ces variables de classe:

    private LinearLayout acteContainer;
    private int currentActeCount = 1; // Commence à 1 car la première ligne existe déjà
    private static final int MAX_ACTES = 3; // Limite max de 3 actes
    private List<String> selectedActeCodes = new ArrayList<>();
    private List<Double> selectedActeTarifs = new ArrayList<>();
    private List<String> selectedActeLibelles = new ArrayList<>();
    private List<String> selectedActeTitres = new ArrayList<>();

    private RadioGroup radioGroupInfosComplementaires;
    private RadioButton radioMaternite, radioATMP, radioAVP, radioProSpec, radioAucune;
    private String infoComplementaire = "AUCUNE"; // Valeur par défaut

    private EditText quantiteActe;
    private double latitude;
    private double longit;
    private List<Integer> selectedActeQuantites = new ArrayList<>();
    List<String> preexistingActeCodes, preexistingActeQuantites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fse_finiliastion_soins_ambulatoire);
        // Initialisation des listes AVANT de les utiliser
        selectedActeCodes = new ArrayList<>();
        selectedActeTarifs = new ArrayList<>();
        selectedActeLibelles = new ArrayList<>();
        selectedActeTitres = new ArrayList<>();
        activityTracker = new ActivityTracker(getApplicationContext());

        // Liste pour stocker les actes préexistants (ceux venant de FseEditActivity)
        preexistingActeCodes = new ArrayList<>();
        preexistingActeQuantites = new ArrayList<>();

        fseServiceDb = FseServiceDb.getInstance(this);
        localisationManager = new LocalisationManager(this);
        enregisterFse = findViewById(R.id.btnEnregistrerFseFinalisee);
        activityTracker.trackActivity("fse_finalise");
        enregisterFse.setEnabled(false);
        ImageView imageViewRecto = findViewById(R.id.imageViewFse1);
        //ImageView imageViewVerso = findViewById(R.id.imageViewFse2);
        idFse = (String) getIntent().getSerializableExtra("ITEM_ID").toString();
        TransactionFse = (String) getIntent().getSerializableExtra("NUM_TRANSACTION").toString();

        System.out.println("TransactionFse-----");
        System.out.println(TransactionFse);

        // Charger la FSE avec les actes déjà enregistrés
        FseAmbulatoire fseExistante = fseServiceDb.getFseAmbulatoireByNumTrans(TransactionFse);

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
                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this, "Erreur: " + error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPermissionRequired() {
                // Permissions nécessaires
                Log.w("MainActivity", "Permissions de localisation requises");
                //demanderPermissions();
            }
        });

        localisationManager.requestLocationUpdates();

        System.out.println(fseExistante.toString());
        if (fseExistante != null) {
            if (fseExistante.getCode_acte1() != null && !fseExistante.getCode_acte1().isEmpty()) {
                currentActeCount++;
            }
            if (fseExistante.getCode_acte2() != null && !fseExistante.getCode_acte2().isEmpty()) {
                currentActeCount++;
            }
            if (fseExistante.getCode_acte3() != null && !fseExistante.getCode_acte3().isEmpty()) {
                currentActeCount++;
            }
            // Récupérer les actes préexistants
            loadPreexistingActes(fseExistante);
        }
        // Initialiser le container pour les lignes d'actes
        acteContainer = findViewById(R.id.acteContainer);
        imageViewRecto.setOnClickListener(v -> openCamera(imageViewRecto));
        //imageViewVerso.setOnClickListener(v -> openCamera(imageViewVerso));




        autoCompleteProfSante = findViewById(R.id.autoCompleteProfSante);
        // Remplacer les références aux spinners par les AutoCompleteTextView
        autoCompleteAffection1 = findViewById(R.id.autoCompleteAffection1);
        autoCompleteAffection2 = findViewById(R.id.autoCompleteAffection2);
        //spinnerCodeActe = findViewById(R.id.champCodeActe);
        // Trouver l'AutoCompleteTextView pour la première ligne
        autoCompleteCodeActe = findViewById(R.id.autoCompleteCodeActe);
        btnAddActe = findViewById(R.id.btnAddActe);
        listViewActe = findViewById(R.id.listPrestation);
        prestationList = new ArrayList<>();

        // Ajouter cette ligne
        fseServiceDb = FseServiceDb.getInstance(this);

        progressBar = findViewById(R.id.progressBar); // Assurez-vous d'ajouter ce ProgressBar au layout
        requestQueue = Volley.newRequestQueue(this);
        sharedPrefManager = new SharedPrefManager(this);
        utilsInfos = new UtilsInfosAppareil(this);
        activityTracker = new ActivityTracker(this);
        metriqueServiceDb = MetriqueServiceDb.getInstance(this);
        smsManager = new DataSMSManager();
        nomEtablissement = fetchEtablissementName();

        String code_ets = sharedPrefManager.getCodeEts();
        // Charger les données depuis la base SQLite
        ReferentielService data = new ReferentielService(FseFiniliastionSoinsAmbulatoire.this);
        //List<Professionel> itemsProfessionnelSante = data.getAllProfessionnel();
        List<Professionel> itemsProfessionnelSante = data.getAllProfessionnelByCentre(code_ets);

        // Créer un adaptateur personnalisé pour l'AutoCompleteTextView des professionnels
        ProfessionnelAdapter profAdapter = new ProfessionnelAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                itemsProfessionnelSante);


        autoCompleteProfSante.setAdapter(profAdapter);
        autoCompleteProfSante.setThreshold(1); // Afficher suggestions après 1 caractère


        autoCompleteProfSante.setOnClickListener(v -> {
            autoCompleteProfSante.showDropDown();
        });

        // Gérer la sélection d'un élément dans l'AutoCompleteTextView
        autoCompleteProfSante.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Professionel selectedProfessionnel = (Professionel) parent.getItemAtPosition(position);
                codeprofessionnel = selectedProfessionnel.getInp();
                libelleprofessionnel = selectedProfessionnel.getNom(); // Assurez-vous que cette méthode existe

                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                        "Professionnel sélectionné : " + libelleprofessionnel, Toast.LENGTH_SHORT).show();
            }
        });

        // Initialiser le RadioGroup et les RadioButton
        radioGroupInfosComplementaires = findViewById(R.id.radioGroupInfosComplementaires);
        radioMaternite = findViewById(R.id.radioMaternite);
        radioATMP = findViewById(R.id.radioATMP);
        radioAVP = findViewById(R.id.radioAVP);
        radioProSpec = findViewById(R.id.radioProSpec);
        radioAucune = findViewById(R.id.radioAucune);

        // Gérer la sélection d'un RadioButton
        radioGroupInfosComplementaires.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioMaternite) {
                    infoComplementaire = "MATERNITE";
                } else if (checkedId == R.id.radioATMP) {
                    infoComplementaire = "ATMP";
                } else if (checkedId == R.id.radioAVP) {
                    infoComplementaire = "AVP";
                } else if (checkedId == R.id.radioProSpec) {
                    infoComplementaire = "PROGSPECIAL";
                } else if (checkedId == R.id.radioAucune) {
                    infoComplementaire = "AUCUNE";
                }

                Log.d("INFO_COMP", "Information complémentaire sélectionnée: " + infoComplementaire);
            }
        });

        List<Affection> affections = data.getAllAffectations();
        List<String> codeAffections = new ArrayList<>();

        // Map pour retrouver l'affection complète à partir du code
        final Map<String, Affection> codeToAffection = new HashMap<>();

        for (Affection affection : affections) {
            String code = affection.getCodeAffection();
            codeAffections.add(code);
            codeToAffection.put(code, affection);
        }
        // Créer des adaptateurs standard pour les codes d'affection
        ArrayAdapter<String> affection1Adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, codeAffections);
        ArrayAdapter<String> affection2Adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, codeAffections);

        // Configurer les AutoCompleteTextView
        autoCompleteAffection1.setAdapter(affection1Adapter);
        autoCompleteAffection1.setThreshold(1); // Commence à suggérer après 1 caractère

        autoCompleteAffection2.setAdapter(affection2Adapter);
        autoCompleteAffection2.setThreshold(1);

        autoCompleteAffection1.setOnClickListener(v -> {
            autoCompleteAffection1.showDropDown();
        });

        autoCompleteAffection2.setOnClickListener(v -> {
            autoCompleteAffection2.showDropDown();
        });
        // Gérer la sélection d'un code dans le premier champ
        autoCompleteAffection1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                codeaffection1 = (String) parent.getItemAtPosition(position);
                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                        "Affection 1 sélectionnée : " + codeaffection1, Toast.LENGTH_SHORT).show();
            }
        });

        // Gérer la sélection d'un code dans le deuxième champ
        autoCompleteAffection2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                codeaffection2 = (String) parent.getItemAtPosition(position);
                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                        "Affection 2 sélectionnée : " + codeaffection2, Toast.LENGTH_SHORT).show();
            }
        });
        enregisterFse.setOnClickListener(v -> {
            saveFse();
        });

        // Trouver la référence au champ de quantité
        quantiteActe = findViewById(R.id.quantiteActe);

        // Ajouter un écouteur pour le champ de quantité
        quantiteActe.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Mettre à jour la quantité dans la liste
                if (!selectedActeCodes.isEmpty()) {
                    try {
                        int quantite = s.toString().isEmpty() ? 1 : Integer.parseInt(s.toString());
                        if (selectedActeQuantites.size() > 0) {
                            selectedActeQuantites.set(0, quantite);
                        } else {
                            selectedActeQuantites.add(quantite);
                        }
                        updateMontantTotal();
                    } catch (NumberFormatException e) {
                        quantiteActe.setText("1");
                    }
                }
            }
        });


        List<ActeMedical> itemsCodeActes = data.getAllActes();
        // Remplir le Spinner avec les données récupérées
        //ArrayAdapter<ActeMedical> adapter4 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsCodeActes);
        // Utiliser l'adaptateur personnalisé au lieu de l'adaptateur standard
        CodeActeAdapter adapter4 = new CodeActeAdapter(this, itemsCodeActes);
        // Configurer l'AutoCompleteTextView avec un adaptateur spécifique
        CodeActeAutoCompleteAdapter autoCompleteAdapter = new CodeActeAutoCompleteAdapter(this, itemsCodeActes);
        autoCompleteCodeActe.setAdapter(autoCompleteAdapter);
        autoCompleteCodeActe.setThreshold(1); // Afficher suggestions après 1 caractère
        //spinnerCodeActe.setAdapter(adapter4);

        final EditText champDesignationEditText = findViewById(R.id.champDesignation);

        adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //spinnerCodeActe.setAdapter(adapter4);
        // Ajouter une méthode pour gérer la déselection
        autoCompleteCodeActe.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                autoCompleteCodeActe.setText("");
            }
        });
        autoCompleteCodeActe.setOnClickListener(v -> {
            // Vérifier s'il y a un acte actuellement sélectionné
            if (codeActe != null && !codeActe.isEmpty()) {
                // Trouver et supprimer le dernier acte ajouté
                int indexToRemove = selectedActeCodes.lastIndexOf(codeActe);
                if (indexToRemove != -1) {
                    selectedActeCodes.remove(indexToRemove);
                    selectedActeTarifs.remove(indexToRemove);
                    selectedActeLibelles.remove(indexToRemove);
                    selectedActeTitres.remove(indexToRemove);

                    // Mettre à jour le montant total
                    updateMontantTotal();
                }
            }

            // Réinitialiser les champs
            autoCompleteCodeActe.setText("");
            codeActe = null;
            libelleActe = null;
            titreActe = null;
            tarifActe = 0.0;

            // Réinitialiser le champ de désignation

            if (champDesignationEditText != null) {
                champDesignationEditText.setText("");
            }
        });
        autoCompleteCodeActe.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ActeMedical selectedActe = (ActeMedical) parent.getItemAtPosition(position);
                codeActe = selectedActe.getCode();
                autoCompleteCodeActe.setText(codeActe);
                libelleActe = selectedActe.getLibelle();
                titreActe = selectedActe.getTitre();

                // Remplir automatiquement le champ de désignation
                EditText champDesignationEditText = findViewById(R.id.champDesignation);
                if (champDesignationEditText != null) {
                    champDesignationEditText.setText(libelleActe);
                }

                // Calculer le tarif
                ReferentielService data = new ReferentielService(FseFiniliastionSoinsAmbulatoire.this);
                tarifActe = data.calculerMontantActe(codeActe);

                // Récupérer la quantité actuelle
                int quantite = 1;
                try {
                    String qteText = quantiteActe.getText().toString();
                    if (!qteText.isEmpty()) {
                        quantite = Integer.parseInt(qteText);
                    }
                } catch (NumberFormatException e) {
                    quantite = 1;
                    quantiteActe.setText("1");
                }

                // Ajouter le tarif à la liste existante
                selectedActeCodes.add(codeActe);
                selectedActeTarifs.add(tarifActe);
                selectedActeLibelles.add(libelleActe);
                selectedActeTitres.add(titreActe);
                selectedActeQuantites.add(quantite);  // Ajouter cette ligne

                // Mettre à jour le montant total
                updateMontantTotal();

                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                        "Sélectionné : " + codeActe + " - Tarif: " + tarifActe,
                        Toast.LENGTH_SHORT).show();

                /*ActeMedical selectedActe = (ActeMedical) parent.getItemAtPosition(position);
                codeActe = selectedActe.getCode();
                autoCompleteCodeActe.setText(codeActe);
                libelleActe = selectedActe.getLibelle();
                titreActe = selectedActe.getTitre();

                // Remplir automatiquement le champ de désignation
                EditText champDesignationEditText = findViewById(R.id.champDesignation);
                if (champDesignationEditText != null) {
                    champDesignationEditText.setText(libelleActe);
                }

                // Calculer le tarif
                ReferentielService data = new ReferentielService(FseFiniliastionSoinsAmbulatoire.this);
                tarifActe = data.calculerMontantActe(codeActe);

                // Ajouter le tarif à la liste existante
                selectedActeCodes.add(codeActe);
                selectedActeTarifs.add(tarifActe);
                selectedActeLibelles.add(libelleActe);
                selectedActeTitres.add(titreActe);

                // Mettre à jour le montant total
                updateMontantTotal();

                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                        "Sélectionné : " + codeActe + " - Tarif: " + tarifActe,
                        Toast.LENGTH_SHORT).show();*/
            }
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                ActeMedical selectedActe = (ActeMedical) parent.getItemAtPosition(position);
//                codeActe = selectedActe.getCode();
//                autoCompleteCodeActe.setText(codeActe);
//                libelleActe = selectedActe.getLibelle();
//                titreActe = selectedActe.getTitre();
//
//                // Calculer le tarif
//                ReferentielService data = new ReferentielService(FseFiniliastionSoinsAmbulatoire.this);
//                tarifActe = data.calculerMontantActe(codeActe);
//
//                // Ajouter le tarif à la liste existante
//                selectedActeCodes.add(codeActe);
//                selectedActeTarifs.add(tarifActe);
//                selectedActeLibelles.add(libelleActe);
//                selectedActeTitres.add(titreActe);
//
//                // Mettre à jour le montant total
//                updateMontantTotal();
//
//                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
//                        "Sélectionné : " + codeActe + " - Tarif: " + tarifActe,
//                        Toast.LENGTH_SHORT).show();
//            }
        });

//        autoCompleteCodeActe.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                ActeMedical selectedActe = (ActeMedical) parent.getItemAtPosition(position);
//                codeActe = selectedActe.getCode();
//                autoCompleteCodeActe.setText(codeActe);
//                libelleActe = selectedActe.getLibelle();
//                titreActe = selectedActe.getTitre();
//
//                // Remplir automatiquement le champ de désignation
//                EditText champDesignationEditText = findViewById(R.id.champDesignation);
//                if (champDesignationEditText != null) {
//                    champDesignationEditText.setText(libelleActe);
//                }
//
//                // Calculer le tarif
//                ReferentielService data = new ReferentielService(FseFiniliastionSoinsAmbulatoire.this);
//                tarifActe = data.calculerMontantActe(codeActe);
//
//                // Réinitialiser et ajouter les listes pour le premier acte
//                selectedActeCodes.clear();
//                selectedActeTarifs.clear();
//                selectedActeLibelles.clear();
//                selectedActeTitres.clear();
//
//                // Ajouter le premier acte
//                selectedActeCodes.add(codeActe);
//                selectedActeTarifs.add(tarifActe);
//                selectedActeLibelles.add(libelleActe);
//                selectedActeTitres.add(titreActe);
//
//                // Mettre à jour le montant total avec le nouveau tarif
//                updateMontantTotal(tarifActe);
//
//                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
//                        "Sélectionné : " + codeActe + " - Tarif: " + tarifActe,
//                        Toast.LENGTH_SHORT).show();
//            }
//        });

//        autoCompleteCodeActe.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                ActeMedical selectedActe = (ActeMedical) parent.getItemAtPosition(position);
//                codeActe = selectedActe.getCode();
//                autoCompleteCodeActe.setText(codeActe);
//                libelleActe = selectedActe.getLibelle();
//                titreActe = selectedActe.getTitre();
//
//                // Remplir automatiquement le champ de désignation
//                if (champDesignationEditText != null) {
//                    champDesignationEditText.setText(libelleActe);
//                } else {
//                    Log.e("ERROR", "champDesignation est null - ID introuvable dans le layout");
//                }
//
//                // Utiliser la même méthode que dans FseEditActivity pour calculer le tarif
//                ReferentielService data = new ReferentielService(FseFiniliastionSoinsAmbulatoire.this);
//                tarifActe = data.calculerMontantActe(codeActe);
//
//                Log.d("DEBUG_TARIF", "Code: " + codeActe + ", Tarif calculé: " + tarifActe);
//
//                // Si c'est le premier acte, on l'ajoute directement à la liste
//                if (currentActeCount == 1) {
//                    // Vider la liste si besoin pour éviter les ajouts multiples
//                    if (!selectedActeCodes.isEmpty() && selectedActeCodes.size() >= 1) {
//                        selectedActeCodes.set(0, codeActe);
//                        selectedActeTarifs.set(0, tarifActe);
//                        selectedActeLibelles.set(0, libelleActe);
//                        selectedActeTitres.set(0, titreActe);
//                    } else {
//                        selectedActeCodes.add(codeActe);
//                        selectedActeTarifs.add(tarifActe);
//                        selectedActeLibelles.add(libelleActe);
//                        selectedActeTitres.add(titreActe);
//                    }
//
//                    // Mise à jour immédiate des montants
//                    updateMontantTotal();
//                }
//
//                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this, "Sélectionné : " + codeActe + " - Tarif: " + tarifActe, Toast.LENGTH_SHORT).show();
//            }
//        });

        // Gérer la sélection d'un élément dans le Spinner
//        spinnerCodeActe.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                ActeMedical selectedActes = (ActeMedical) parent.getItemAtPosition(position);
//                codeActe = selectedActes.getCode();
//                libelleActe = selectedActes.getLibelle();
//                titreActe = selectedActes.getTitre();
//
//                // Remplir automatiquement le champ de désignation
//                if (champDesignationEditText != null) {
//                    champDesignationEditText.setText(libelleActe);
//                } else {
//                    Log.e("ERROR", "champDesignation est null - ID introuvable dans le layout");
//                }
//
//                // Utiliser la même méthode que dans FseEditActivity pour calculer le tarif
//                ReferentielService data = new ReferentielService(FseFiniliastionSoinsAmbulatoire.this);
//                tarifActe = data.calculerMontantActe(codeActe);
//
//                Log.d("DEBUG_TARIF", "Code: " + codeActe + ", Tarif calculé: " + tarifActe);
//
//                // Si c'est le premier acte, on l'ajoute directement à la liste
//                if (currentActeCount == 1) {
//                    // Vider la liste si besoin pour éviter les ajouts multiples
//                    if (!selectedActeCodes.isEmpty() && selectedActeCodes.size() >= 1) {
//                        selectedActeCodes.set(0, codeActe);
//                        selectedActeTarifs.set(0, tarifActe);
//                        selectedActeLibelles.set(0, libelleActe);
//                        selectedActeTitres.set(0, titreActe);
//                    } else {
//                        selectedActeCodes.add(codeActe);
//                        selectedActeTarifs.add(tarifActe);
//                        selectedActeLibelles.add(libelleActe);
//                        selectedActeTitres.add(titreActe);
//                    }
//
//                    // Mise à jour immédiate des montants
//                    updateMontantTotal();
//                }
//
//                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this, "Sélectionné : " + codeActe + " - Tarif: " + tarifActe, Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Ne rien faire
//            }
//        });

        adapterActe = new ActePrestationAdapter(this, prestationList);

        listViewActe.setAdapter(adapterActe);

        // Dans la méthode onClick du bouton btnAddActe, mettez à jour la vérification du code d'acte
        btnAddActe.setOnClickListener(v -> {
            Log.d("DEBUG_BTN", "Bouton d'ajout d'acte cliqué");
            String codeActeText = autoCompleteCodeActe.getText().toString().trim();

            // Vérifier si on a atteint la limite
            if (currentActeCount >= MAX_ACTES) {
                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                        "Maximum de " + MAX_ACTES + " actes atteint", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérifier si un code d'acte est sélectionné
            if (codeActeText.isEmpty() || codeActe == null || codeActe.isEmpty()) {
                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                        "Veuillez sélectionner un code d'acte", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("DEBUG_BTN", "Acte valide - Code: " + codeActe + ", Tarif: " + tarifActe);

            // Ajouter l'acte courant à la prestation si ce n'est pas déjà fait
            boolean acteDejaAjoute = false;
            for (ActeMedical acte : prestationList) {
                if (acte.getCode().equals(codeActe)) {
                    acteDejaAjoute = true;
                    break;
                }
            }

            if (!acteDejaAjoute) {
                Log.d("DEBUG_BTN", "Ajout de l'acte aux prestations");
                compteurActe++;
                ajouterNouvellePrestation(compteurActe);
            }

            // Ajouter une nouvelle ligne d'acte dynamiquement
            Log.d("DEBUG_BTN", "Ajout d'une nouvelle ligne d'acte");
            addNewActeRow();

            // Mettre à jour les variables pour l'API
            updateApiActeVariables();

            // Mise à jour forcée des montants
            Log.d("DEBUG_BTN", "Mise à jour des montants");
            updateMontantTotal();

            // Réinitialiser le champ d'autocomplétion
            //autoCompleteCodeActe.setText("");
        });

//        btnAddActe.setOnClickListener(v -> {
//            Log.d("DEBUG_BTN", "Bouton d'ajout d'acte cliqué");
//
//            // Vérifier si on a atteint la limite
//            if (currentActeCount >= MAX_ACTES) {
//                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
//                        "Maximum de " + MAX_ACTES + " actes atteint", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Vérifier si un code d'acte est sélectionné
//            if (codeActe == null || codeActe.isEmpty()) {
//                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
//                        "Veuillez sélectionner un code d'acte", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            Log.d("DEBUG_BTN", "Acte valide - Code: " + codeActe + ", Tarif: " + tarifActe);
//
//            // Ajouter l'acte courant à la prestation si ce n'est pas déjà fait
//            boolean acteDejaAjoute = false;
//            for (ActeMedical acte : prestationList) {
//                if (acte.getCode().equals(codeActe)) {
//                    acteDejaAjoute = true;
//                    break;
//                }
//            }
//
//            if (!acteDejaAjoute) {
//                Log.d("DEBUG_BTN", "Ajout de l'acte aux prestations");
//                compteurActe++;
//                ajouterNouvellePrestation(compteurActe);
//            }
//
//            // Ajouter une nouvelle ligne d'acte dynamiquement
//            Log.d("DEBUG_BTN", "Ajout d'une nouvelle ligne d'acte");
//            addNewActeRow();
//
//            // Mettre à jour les variables pour l'API
//            updateApiActeVariables();
//
//            // Mise à jour forcée des montants après ajout
//            Log.d("DEBUG_BTN", "Mise à jour des montants après ajout");
//            updateMontantTotal();
//        });
    }

    private void loadPreexistingActes(FseAmbulatoire fse) {
        // Créer un layout pour afficher les actes préexistants
        LinearLayout preexistingActesContainer = findViewById(R.id.preexistingActesContainer);
        // Assurez-vous d'ajouter ce container dans votre layout XML

        // Effacer tout contenu précédent
        preexistingActesContainer.removeAllViews();

        ReferentielService data = new ReferentielService(this);

        // Récupérer les actes et quantités
        String[] acteCodes = new String[3];
        String[] acteQuantites = new String[3];

        // Récupérer les actes s'ils existent
        acteCodes[0] = fse.getCode_acte1();
        acteCodes[1] = fse.getCode_acte2();
        acteCodes[2] = fse.getCode_acte3();

        // Récupérer les quantités
        acteQuantites[0] = fse.getQuantite_1();
        acteQuantites[1] = fse.getQuantite_2();
        acteQuantites[2] = fse.getQuantite_3();

        // Vérifier si les actes contiennent plusieurs codes (séparés par des ;)
        // Dans ce cas, nous devrons les traiter différemment
        if (acteCodes[0] != null && acteCodes[0].contains(";")) {
            String[] splitCodes = acteCodes[0].split(";");
            String[] splitQuantities = acteQuantites[0] != null ? acteQuantites[0].split(";") : new String[]{"1"};

            // Réinitialiser les tableaux
            acteCodes = new String[splitCodes.length];
            acteQuantites = new String[splitCodes.length];

            // Remplir avec les valeurs séparées
            for (int i = 0; i < splitCodes.length; i++) {
                acteCodes[i] = splitCodes[i];
                acteQuantites[i] = i < splitQuantities.length ? splitQuantities[i] : "1";
            }
        }

        // Parcourir les actes et les ajouter à l'interface
        for (int i = 0; i < acteCodes.length; i++) {

            if (acteCodes[i] != null && !acteCodes[i].isEmpty()) {
                ActeMedical acte = data.getActeMedicalByCode(acteCodes[i]);
                if (acte != null) {
                    // Créer une vue pour cet acte
                    View acteView = getLayoutInflater().inflate(R.layout.item_preexisting_acte, null);

                    // Récupérer les éléments de la vue
                    TextView tvCodeActe = acteView.findViewById(R.id.tvCodeActe);
                    TextView tvDesignation = acteView.findViewById(R.id.tvDesignation);
                    TextView tvQuantite = acteView.findViewById(R.id.tvQuantite);

                    // Remplir les informations
                    tvCodeActe.setText(acteCodes[i]);
                    tvDesignation.setText(acte.getLibelle());
                    tvQuantite.setText(acteQuantites[i] != null ? acteQuantites[i] : "1");

                    // Ajouter la vue au container
                    preexistingActesContainer.addView(acteView);

                    // Ajouter aux listes de préexistants pour les exclure des calculs
                    preexistingActeCodes.add(acteCodes[i]);
                }
            }
        }

        // Afficher un message s'il n'y a pas d'actes préexistants
        if (preexistingActesContainer.getChildCount() == 0) {
            TextView tvNoActes = new TextView(this);
            tvNoActes.setText("Aucun acte précédemment enregistré");
            tvNoActes.setPadding(16, 16, 16, 16);
            preexistingActesContainer.addView(tvNoActes);
        } else {
            // Ajouter un titre pour cette section
            TextView tvTitle = new TextView(this);
            tvTitle.setText("Actes déjà enregistrés :");
            tvTitle.setTextSize(16);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setPadding(16, 16, 16, 8);
            preexistingActesContainer.addView(tvTitle, 0); // Ajouter en premier
        }
    }

    private void addNewActeRow() {
        currentActeCount++;

        // Inflater la nouvelle ligne depuis le layout
        View newActeRow = getLayoutInflater().inflate(R.layout.acte_row_layout, null);

        // Récupérer les vues dans cette nouvelle ligne
        AutoCompleteTextView autoCompleteCodeActe = newActeRow.findViewById(R.id.champCodeActeAdditional);
        EditText editDesignation = newActeRow.findViewById(R.id.champDesignationAdditional);
        EditText editQuantite = newActeRow.findViewById(R.id.champQuantiteAdditional);  // Ajout de cette ligne
        ImageButton btnRemove = newActeRow.findViewById(R.id.btnRemoveActe);

        /*if (codeActe != null && !codeActe.isEmpty()) {
            autoCompleteCodeActe.setText(codeActe);
        }

        // Conserver la désignation
        if (libelleActe != null && !libelleActe.isEmpty()) {
            editDesignation.setText(libelleActe);
        }*/

        // Au lieu de cela, laissez les champs vides pour que l'utilisateur puisse saisir un nouvel acte
        autoCompleteCodeActe.setText("");
        editDesignation.setText("");
        editQuantite.setText("1");

        // Générer un ID unique pour l'autocomplétion
        final int autoCompleteId = View.generateViewId();
        autoCompleteCodeActe.setId(autoCompleteId);

        // Remplir avec la liste d'actes
        ReferentielService data = new ReferentielService(FseFiniliastionSoinsAmbulatoire.this);
        List<ActeMedical> itemsCodeActes = data.getAllActes();

        // Adapter personnalisé
        CodeActeAutoCompleteAdapter adapter = new CodeActeAutoCompleteAdapter(this, itemsCodeActes);
        autoCompleteCodeActe.setAdapter(adapter);
        autoCompleteCodeActe.setThreshold(1);

        // Configurer le listener pour la quantité (NOUVEAU)
        final int rowIndex = currentActeCount - 1;
        editQuantite.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (rowIndex < selectedActeQuantites.size()) {
                    try {
                        int quantite = s.toString().isEmpty() ? 1 : Integer.parseInt(s.toString());
                        selectedActeQuantites.set(rowIndex, quantite);
                    } catch (NumberFormatException e) {
                        selectedActeQuantites.set(rowIndex, 1);
                        editQuantite.setText("1");
                    }
                    updateMontantTotal();
                }
            }
        });

        // Configurer le listener pour la sélection d'un acte
        autoCompleteCodeActe.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ActeMedical selectedActe = (ActeMedical) parent.getItemAtPosition(position);
                String newCodeActe = selectedActe.getCode();
                String newLibelleActe = selectedActe.getLibelle();
                String newTitreActe = selectedActe.getTitre();

                // Afficher le CODE dans le champ de code acte, pas la désignation
                autoCompleteCodeActe.setText(newCodeActe);

                // Remplir automatiquement le champ de désignation
                editDesignation.setText(newLibelleActe);

                // Calculer le tarif
                double newTarifActe = data.calculerMontantActe(newCodeActe);

                // Récupérer la quantité
                int quantite = 1;
                try {
                    String qteText = editQuantite.getText().toString();
                    if (!qteText.isEmpty()) {
                        quantite = Integer.parseInt(qteText);
                    }
                } catch (NumberFormatException e) {
                    quantite = 1;
                    editQuantite.setText("1");
                }

                // Mettre à jour ou ajouter à la liste
                if (rowIndex < selectedActeCodes.size()) {
                    selectedActeCodes.set(rowIndex, newCodeActe);
                    selectedActeLibelles.set(rowIndex, newLibelleActe);
                    selectedActeTitres.set(rowIndex, newTitreActe);
                    selectedActeTarifs.set(rowIndex, newTarifActe);
                    selectedActeQuantites.set(rowIndex, quantite);  // Ajouter cette ligne
                } else {
                    selectedActeCodes.add(newCodeActe);
                    selectedActeLibelles.add(newLibelleActe);
                    selectedActeTitres.add(newTitreActe);
                    selectedActeTarifs.add(newTarifActe);
                    selectedActeQuantites.add(quantite);  // Ajouter cette ligne
                }

                // Mettre à jour l'acte dans la prestationList avec la quantité
                compteurActe++;
                ActeMedical nouvellePrestation = new ActeMedical(
                        compteurActe, "", newCodeActe, newLibelleActe, newTitreActe, newTarifActe * quantite
                );

                if (rowIndex < prestationList.size()) {
                    prestationList.set(rowIndex, nouvellePrestation);
                } else {
                    prestationList.add(nouvellePrestation);
                }

                // Mettre à jour l'affichage et les calculs
                adapterActe.notifyDataSetChanged();
                updateMontantTotal();
                updateApiActeVariables();

                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                        "Acte " + newCodeActe + " ajouté - Tarif: " + newTarifActe,
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Configurer le bouton de suppression pour également supprimer de la liste des quantités
        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Supprimer la vue
                acteContainer.removeView(newActeRow);

                // Mettre à jour le compteur
                currentActeCount--;

                // Supprimer les données correspondantes
                if (rowIndex < selectedActeCodes.size()) {
                    selectedActeCodes.remove(rowIndex);
                    selectedActeLibelles.remove(rowIndex);
                    selectedActeTitres.remove(rowIndex);
                    selectedActeTarifs.remove(rowIndex);
                    selectedActeQuantites.remove(rowIndex);  // Ajouter cette ligne
                }

                // Supprimer l'acte de la prestationList
                if (rowIndex < prestationList.size()) {
                    prestationList.remove(rowIndex);
                    adapterActe.notifyDataSetChanged();
                }

                // Mettre à jour le montant total
                updateMontantTotal();
                // Mettre à jour les variables API
                updateApiActeVariables();
            }
        });

        // Ajouter la nouvelle ligne au container
        acteContainer.addView(newActeRow);
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


//    private void addNewActeRow() {
//        currentActeCount++;
//
//        // Inflater la nouvelle ligne depuis le layout
//        View newActeRow = getLayoutInflater().inflate(R.layout.acte_row_layout, null);
//
//        // Récupérer les vues dans cette nouvelle ligne
//        Spinner spinnerNewActe = newActeRow.findViewById(R.id.champCodeActeAdditional);
//        EditText editDesignation = newActeRow.findViewById(R.id.champDesignationAdditional);
//        ImageButton btnRemove = newActeRow.findViewById(R.id.btnRemoveActe);
//
//        // Générer un ID unique pour ce spinner
//        final int spinnerId = View.generateViewId();
//        spinnerNewActe.setId(spinnerId);
//
//        // Remplir le spinner avec la même liste d'actes que le premier spinner
//        ReferentielService data = new ReferentielService(FseFiniliastionSoinsAmbulatoire.this);
//        List<ActeMedical> itemsCodeActes = data.getAllActes();
//
//        // Utiliser l'adaptateur personnalisé qui n'affiche que les codes
//        CodeActeAdapter adapter = new CodeActeAdapter(this, itemsCodeActes);
//        spinnerNewActe.setAdapter(adapter);
//
//        // Configurer le listener du nouveau spinner
//        final int rowIndex = currentActeCount - 1; // Index de cette ligne (0-based)
//        spinnerNewActe.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                ActeMedical selectedActe = (ActeMedical) parent.getItemAtPosition(position);
//                String newCodeActe = selectedActe.getCode();
//                String newLibelleActe = selectedActe.getLibelle();
//                String newTitreActe = selectedActe.getTitre();
//
//                // Remplir automatiquement le champ de désignation
//                editDesignation.setText(newLibelleActe);
//
//                // Utiliser la même méthode de calcul du tarif que dans FseEditActivity
//                double newTarifActe = data.calculerMontantActe(newCodeActe);
//
//                Log.d("DEBUG_TARIF", "Nouvel acte - Code: " + newCodeActe + ", Tarif calculé: " + newTarifActe);
//
//                // Mettre à jour ou ajouter à la liste
//                if (rowIndex < selectedActeCodes.size()) {
//                    selectedActeCodes.set(rowIndex, newCodeActe);
//                    selectedActeLibelles.set(rowIndex, newLibelleActe);
//                    selectedActeTitres.set(rowIndex, newTitreActe);
//                    selectedActeTarifs.set(rowIndex, newTarifActe);
//                } else {
//                    selectedActeCodes.add(newCodeActe);
//                    selectedActeLibelles.add(newLibelleActe);
//                    selectedActeTitres.add(newTitreActe);
//                    selectedActeTarifs.add(newTarifActe);
//                }
//
//                // Ajouter ou mettre à jour l'acte dans la prestationList
//                compteurActe++;
//                ActeMedical nouvellePrestation = new ActeMedical(compteurActe, "", newCodeActe,
//                        newLibelleActe, newTitreActe, newTarifActe);
//
//                // Si la position existe déjà dans la liste, la mettre à jour
//                if (rowIndex < prestationList.size()) {
//                    prestationList.set(rowIndex, nouvellePrestation);
//                } else {
//                    prestationList.add(nouvellePrestation);
//                }
//
//                // Mettre à jour l'adaptateur
//                adapterActe.notifyDataSetChanged();
//
//                // Mettre à jour les montants - IMPORTANT
//                updateMontantTotal();
//
//                // Mettre à jour les variables API
//                updateApiActeVariables();
//
//                // Feedback
//                Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
//                        "Acte " + newCodeActe + " ajouté - Tarif: " + newTarifActe, Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Ne rien faire
//            }
//        });
//
//        // Configurer le bouton de suppression (reste inchangé)
//        btnRemove.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Supprimer la vue
//                acteContainer.removeView(newActeRow);
//
//                // Mettre à jour le compteur
//                currentActeCount--;
//
//                // Supprimer les données correspondantes
//                if (rowIndex < selectedActeCodes.size()) {
//                    selectedActeCodes.remove(rowIndex);
//                    selectedActeLibelles.remove(rowIndex);
//                    selectedActeTitres.remove(rowIndex);
//                    selectedActeTarifs.remove(rowIndex);
//                }
//
//                // Supprimer l'acte de la prestationList
//                if (rowIndex < prestationList.size()) {
//                    prestationList.remove(rowIndex);
//                    adapterActe.notifyDataSetChanged();
//                }
//
//                // Mettre à jour les montants - IMPORTANT
//                updateMontantTotal();
//
//                // Mettre à jour les variables API
//                updateApiActeVariables();
//            }
//        });
//
//        // Ajouter la nouvelle ligne au container
//        acteContainer.addView(newActeRow);
//    }

    private void updateApiActeVariables() {
        acte1 = selectedActeCodes.size() >= 1 ? selectedActeCodes.get(0) : null;
        acte2 = selectedActeCodes.size() >= 2 ? selectedActeCodes.get(1) : null;
        acte3 = selectedActeCodes.size() >= 3 ? selectedActeCodes.get(2) : null;

        // On peut aussi mettre à jour les variables affection si nécessaire
        affection1 = codeaffection1;
        affection2 = codeaffection2;

        // Log pour le débogage
        Log.d("API_ACTES", "acte1=" + acte1 + ", acte2=" + acte2 + ", acte3=" + acte3);
    }

    // Dans la classe FseFiniliastionSoinsAmbulatoire, remplacer la méthode updateMontantTotal() existante

    // Amélioration de updateMontantTotal avec plus de logs
    private void updateMontantTotal() {
        // Calculer le total en tenant compte des quantités
        double totalMontant = 0.0;
        for (int i = 0; i < selectedActeTarifs.size(); i++) {
            if (selectedActeTarifs.get(i) != null) {
                int quantite = 1;
                if (i < selectedActeQuantites.size() && selectedActeQuantites.get(i) != null) {
                    quantite = selectedActeQuantites.get(i);
                }
                totalMontant += selectedActeTarifs.get(i) * quantite;
            }
        }

        // Calculer les parts
        double partCMU = Math.round(totalMontant * 0.7) ;
        double partAssure = Math.round(totalMontant * 0.3);

        // Mettre à jour les TextView
        TextView montantTextView = findViewById(R.id.montant);
        TextView cmuPartTextView = findViewById(R.id.cmupart);
        TextView assurePartTextView = findViewById(R.id.assurepart);

        montantTextView.setText(String.format(Locale.FRANCE, "%s FCFA", totalMontant));
        cmuPartTextView.setText(String.format(Locale.FRANCE, "%s FCFA", partCMU));
        assurePartTextView.setText(String.format(Locale.FRANCE, "%s FCFA", partAssure));
    }
//    private void updateMontantTotal() {
//        Log.d("DEBUG_MONTANT", "Méthode updateMontantTotal appelée");
//
//        // Vérifier si la liste est initialisée
//        if (selectedActeTarifs == null) {
//            Log.e("DEBUG_MONTANT", "ERREUR: selectedActeTarifs est null!");
//            return;
//        }
//
//        // Debug - afficher le contenu de la liste
//        Log.d("DEBUG_MONTANT", "Nombre d'actes: " + selectedActeTarifs.size());
//        for (int i = 0; i < selectedActeTarifs.size(); i++) {
//            Log.d("DEBUG_MONTANT", "Acte " + i + " - Tarif: " + selectedActeTarifs.get(i));
//        }
//
//        // Calcul du montant total
//        double totalMontant = 0.0;
//
//        // Utiliser les tarifs des actes sélectionnés
//        for (Double tarif : selectedActeTarifs) {
//            if (tarif != null) {
//                totalMontant += tarif;
//                Log.d("DEBUG_MONTANT", "Ajout du tarif: " + tarif + " - Total actuel: " + totalMontant);
//            } else {
//                Log.e("DEBUG_MONTANT", "ERREUR: tarif null détecté!");
//            }
//        }
//
//        // Appliquer la formule de calcul:
//        double partCMU = totalMontant * 0.7;    // 70% du total
//        double partAssure = totalMontant * 0.3;  // 30% du total
//
//        Log.d("DEBUG_MONTANT", "Montant total calculé: " + totalMontant);
//        Log.d("DEBUG_MONTANT", "Part CMU calculée: " + partCMU);
//        Log.d("DEBUG_MONTANT", "Part assure calculée: " + partAssure);
//
//        // Formater les montants avec 2 décimales maximum
//        java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");
//        df.setMaximumFractionDigits(2);
//
//        // Trouver les TextView
//        TextView montantTextView = findViewById(R.id.montant);
//        TextView cmuPartTextView = findViewById(R.id.cmupart);
//        TextView assurePartTextView = findViewById(R.id.assurepart);
//
//        // Vérifier que les vues existent
//        if (montantTextView == null || cmuPartTextView == null || assurePartTextView == null) {
//            Log.e("DEBUG_MONTANT", "ERREUR: Un ou plusieurs TextView sont null!");
//            return;
//        }
//
//        // Afficher les montants
//        String montantFormatted = df.format(totalMontant);
//        String cmuFormatted = df.format(partCMU);
//        String assureFormatted = df.format(partAssure);
//
//        montantTextView.setText(String.format(Locale.FRANCE, "%s FCFA", montantFormatted));
//        cmuPartTextView.setText(String.format(Locale.FRANCE, "%s FCFA", cmuFormatted));
//        assurePartTextView.setText(String.format(Locale.FRANCE, "%s FCFA", assureFormatted));
//
//        Log.d("DEBUG_MONTANT", "TextViews mis à jour - Total: " + montantFormatted + " FCFA, CMU: " + cmuFormatted + " FCFA, Assuré: " + assureFormatted + " FCFA");
//    }

    /*private boolean sendDataToApi() {
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
            FseAmbulatoire fse = fseServiceDb.getFseAmbulatoireByNumTrans(TransactionFse);
            if (fse == null) {
                Toast.makeText(this, "Transaction introuvable", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Compléter les champs requis
            jsonBody.put("type_bon", "");
            jsonBody.put("date_soins", currentDate);
            jsonBody.put("numTrans", fse.getNumTrans());
            jsonBody.put("numOgd", ""); // Valeur par défaut
            jsonBody.put("numSecu", fse.getNumSecu());
            jsonBody.put("numGuid", fse.getNumGuid());
            jsonBody.put("nomComplet", fse.getNomComplet());
            jsonBody.put("affection1",affection1);
            jsonBody.put("affection2",affection2);
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
                            Toast.makeText(FseFiniliastionSoinsAmbulatoire.this, "FSE envoyée avec succès à l'API", Toast.LENGTH_SHORT).show();

                            // Enregistrer localement aussi
                            //saveFse();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("API_ERROR", "Error: " + error.toString());
                            Toast.makeText(FseFiniliastionSoinsAmbulatoire.this, "Erreur lors de l'envoi à l'API: " + error.getMessage(), Toast.LENGTH_LONG).show();

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
    }*/
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
            Log.d("Famoco", "ID famoco est : " + idFamoco);
            int id = RegionUtils.getRegionid(downloadedFileName);
            FseAmbulatoire fse = fseServiceDb.getFseAmbulatoireByNumTrans(TransactionFse);
            if (fse == null) {
                Toast.makeText(this, "Transaction introuvable", Toast.LENGTH_SHORT).show();
                return false;
            }

            // IMPORTANT: Ajouter code_acte au niveau racine pour résoudre l'erreur API
            if (acte1 != null && !acte1.isEmpty()) {
                jsonBody.put("code_acte", acte1);
                Log.d("API_DEBUG", "Code acte racine ajouté: " + acte1);
            }

            Log.d("API_DEBUG", "type_bon: " + fse.getType_fse());
            // Compléter les champs requis
            jsonBody.put("type_bon", fse.getType_fse());
            jsonBody.put("date_soins", currentDate);
            jsonBody.put("numTrans", fse.getNumTrans());
            jsonBody.put("numOgd", ""); // Valeur par défaut
            jsonBody.put("numSecu", fse.getNumSecu());
            jsonBody.put("numGuid", fse.getNumGuid());
            jsonBody.put("nomComplet", fse.getNomComplet());
            jsonBody.put("affection1", codeaffection1 != null ? codeaffection1 : "");
            jsonBody.put("affection2", codeaffection2 != null ? codeaffection2 : "");
            jsonBody.put("latitude",latitude);
            jsonBody.put("longitude",longit);

            try {
                String inputDateStr = fse.getDateNaissance();
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date birthDate = inputFormat.parse(inputDateStr);
                String formattedBirthDate = outputFormat.format(birthDate);
                jsonBody.put("dateNaissance", formattedBirthDate);
            } catch (Exception e) {
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
            jsonBody.put("code_professionnel", codeprofessionnel != null ? codeprofessionnel : "");
            jsonBody.put("nom_professionnel", libelleprofessionnel != null ? libelleprofessionnel : "");
            jsonBody.put("specialite", "");

            // Informations complémentaires
            JSONObject infoComplementaireJson = new JSONObject();
            infoComplementaireJson.put("MATERNITE", infoComplementaire.equals("MATERNITE") ? 1 : 0);
            infoComplementaireJson.put("AVP", infoComplementaire.equals("AVP") ? 1 : 0);
            infoComplementaireJson.put("AT", infoComplementaire.equals("ATMP") ? 1 : 0);
            infoComplementaireJson.put("AUTRE", infoComplementaire.equals("PROGSPECIAL") ? 1 : 0);
            jsonBody.put("information_complementaire", infoComplementaireJson);

            // Observations
            String observations = "ras";
            jsonBody.put("observations", observations);

            // Champs vides pour respecter la structure
            jsonBody.put("nom_vehicule", "");
            jsonBody.put("num_assurance_vehicule", "");
            jsonBody.put("nom_societe", "");
            jsonBody.put("num_societe", "");

            // NOUVELLE APPROCHE POUR LES PRESTATIONS
            JSONArray prestations = new JSONArray();

            // Parcourir les actes sélectionnés et construire les prestations correctement
            for (int i = 0; i < selectedActeCodes.size(); i++) {
                String codeActe = selectedActeCodes.get(i);
                if (codeActe != null && !codeActe.isEmpty()) {
                    JSONObject prestation = new JSONObject();

                    // Inclure uniquement les champs absolument essentiels
                    prestation.put("code_acte", codeActe);

                    // Ajouter des détails seulement s'ils sont disponibles
                    if (i < selectedActeLibelles.size() && selectedActeLibelles.get(i) != null) {
                        prestation.put("designation", selectedActeLibelles.get(i));
                    } else {
                        prestation.put("designation", "");
                    }

                    // Ajouter des valeurs pour les champs obligatoires
                    prestation.put("date_debut", currentDate);
                    prestation.put("date_fin", currentDate);
                    prestation.put("quantite", 1);

                    if (i < selectedActeTarifs.size() && selectedActeTarifs.get(i) != null) {
                        double tarif = selectedActeTarifs.get(i);
                        prestation.put("montant", tarif);
                        prestation.put("part_cmu", tarif * 0.7);
                        prestation.put("part_assure", tarif * 0.3);
                    } else {
                        prestation.put("montant", 0);
                        prestation.put("part_cmu", 0);
                        prestation.put("part_assure", 0);
                    }

                    prestation.put("part_ac", 0);

                    prestations.put(prestation);
                    Log.d("API_DEBUG", "Ajout prestation: code_acte=" + codeActe);
                }
            }

            // Structure alternative si la première ne fonctionne pas
            if (prestations.length() == 0) {
                // Fallback en cas d'aucun acte valide
                if (acte1 != null && !acte1.isEmpty()) {
                    JSONObject simplePrestation = new JSONObject();
                    simplePrestation.put("code_acte", acte1);
                    simplePrestation.put("designation", "");
                    simplePrestation.put("date_debut", currentDate);
                    simplePrestation.put("date_fin", currentDate);
                    simplePrestation.put("quantite", 1);
                    simplePrestation.put("montant", 0);
                    simplePrestation.put("part_cmu", 0);
                    simplePrestation.put("part_ac", 0);
                    simplePrestation.put("part_assure", 0);
                    prestations.put(simplePrestation);
                    Log.d("API_DEBUG", "Fallback - ajout prestation simple: code_acte=" + acte1);
                } else if (acte2 != null && !acte2.isEmpty()) {
                    JSONObject simplePrestation = new JSONObject();
                    simplePrestation.put("code_acte", acte2);
                    prestations.put(simplePrestation);
                    Log.d("API_DEBUG", "Fallback - ajout prestation simple: code_acte=" + acte2);
                } else if (acte3 != null && !acte3.isEmpty()) {
                    JSONObject simplePrestation = new JSONObject();
                    simplePrestation.put("code_acte", acte3);
                    prestations.put(simplePrestation);
                    Log.d("API_DEBUG", "Fallback - ajout prestation simple: code_acte=" + acte3);
                }
            }

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

            // Prescriptions
            JSONArray prescriptions = new JSONArray();
            jsonBody.put("prescription_medicale", prescriptions);

            // Log détaillé de la structure JSON - utile pour le débogage
            try {
                Log.d("API_DEBUG", "Structure JSON complète:");
                Log.d("API_DEBUG", "code_acte au niveau racine: " + jsonBody.optString("code_acte", "non défini"));

                JSONArray prestationsArray = jsonBody.optJSONArray("prestations");
                if (prestationsArray != null && prestationsArray.length() > 0) {
                    Log.d("API_DEBUG", "Nombre de prestations: " + prestationsArray.length());
                    for (int i = 0; i < prestationsArray.length(); i++) {
                        JSONObject prestation = prestationsArray.optJSONObject(i);
                        if (prestation != null) {
                            Log.d("API_DEBUG", "Prestation " + i + " - code_acte: " +
                                    prestation.optString("code_acte", "non défini"));
                        }
                    }
                } else {
                    Log.d("API_DEBUG", "Aucune prestation définie!");
                }
            } catch (Exception e) {
                Log.e("API_DEBUG", "Erreur lors du logging: " + e.getMessage());
            }

            // Afficher la requête dans les logs pour debug
            Log.d("API_REQUEST", "Sending to API: " + jsonBody.toString());

            // Création de la requête
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, API_URL, jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("API_RESPONSE", "Réponse brute: " + response.toString());

                            // Vérifier explicitement si la réponse contient un message d'erreur
                            boolean hasError = false;
                            String errorMessage = "";

                            try {
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
                                    Log.e("API_ERROR", "Erreur API: " + errorMessage);
                                    Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                                            "Erreur API: " + errorMessage, Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.d("API_RESPONSE", "Success: " + response.toString());
                                    Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                                            "FSE envoyée avec succès à l'API", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e("API_ERROR", "Erreur traitement réponse: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("API_ERROR", "Error: " + error.toString());
                            Toast.makeText(FseFiniliastionSoinsAmbulatoire.this,
                                    "Erreur lors de l'envoi à l'API: " +
                                            (error.getMessage() != null ? error.getMessage() : "Erreur inconnue"),
                                    Toast.LENGTH_LONG).show();
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
    /*private boolean sendDataToApi() {
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
            //FseAmbulatoire fse = fseServiceDb.getFseAmbulatoireByNumTrans(numTrans);
            Log.d("FSE_DEBUG", "Transaction utilisée pour la recherche: " + TransactionFse);
            FseAmbulatoire fse = fseServiceDb.getFseAmbulatoireByNumTrans(TransactionFse);
            if (fse == null) {
                Toast.makeText(this, "Transaction introuvable", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Compléter les champs requis
            jsonBody.put("type_bon", "");
            jsonBody.put("date_soins", currentDate);
            jsonBody.put("numTrans", fse.getNumTrans());
            jsonBody.put("numOgd", ""); // Valeur par défaut
            jsonBody.put("numSecu", fse.getNumSecu());
            jsonBody.put("numGuid", fse.getNumGuid());
            jsonBody.put("nomComplet", fse.getNomComplet());
            jsonBody.put("affection1",affection1);
            jsonBody.put("affection2",affection2);
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
                            Toast.makeText(FseFiniliastionSoinsAmbulatoire.this, "FSE envoyée avec succès à l'API", Toast.LENGTH_SHORT).show();

                            // Enregistrer localement aussi
                            //saveFse();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("API_ERROR", "Error: " + error.toString());
                            Toast.makeText(FseFiniliastionSoinsAmbulatoire.this, "Erreur lors de l'envoi à l'API: " + error.getMessage(), Toast.LENGTH_LONG).show();

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
    }*/
    private void showSuccessDialog() {
        successDialog = new Dialog(this);
        successDialog.setContentView(R.layout.dialog_success);
        successDialog.setCancelable(false);

        Button btnOk = successDialog.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(v -> {
            successDialog.dismiss();
            successDialog = null;

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        successDialog.show();
    }
    //    private void uploadImage(String imagePath) {
//
//        File imageFile = new File(imagePath);
//
//        // Comprehensive file checks
//        if (!imageFile.exists()) {
//            Log.e(TAG, "Fichier introuvable: " + imagePath);
//            return;
//        }
//
//        if (!imageFile.canRead()) {
//            Log.e(TAG, "Impossible de lire le fichier: " + imagePath);
//            return;
//        }
//
//        Log.d(TAG, "Détails fichier - Chemin: " + imageFile.getAbsolutePath());
//        Log.d(TAG, "Taille fichier: " + imageFile.length() + " octets");
//
//        // Vérifier les permissions et le stockage
//        if (!imageFile.getParentFile().canWrite()) {
//            Log.e(TAG, "Droits d'écriture insuffisants");
//        }
//        if (imagePath == null || imagePath.isEmpty()) {
//            Log.e("FSE_DEBUG", "Chemin d'image vide");
//            Toast.makeText(this, "Erreur: chemin d'image invalide", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        Log.d("FSE_DEBUG", "Chemin de l'image à envoyer: " + imagePath);
//        Log.d("FSE_DEBUG", "Numéro de transaction: " + TransactionFse);
//        System.out.println("IMAGE PATH");
//        System.out.println(imagePath);
//        enregisterFse.setVisibility(View.INVISIBLE);
//        //progressBar.setVisibility(View.VISIBLE);
//        // Ajouter cette vérification
//        if (progressBar != null) {
//            progressBar.setVisibility(View.VISIBLE);
//        }
//        String date_fin = activityTracker.enregistrerDateFin();
//        String identifiant = (numSecu == null || numSecu.isEmpty()) ? "guid" : numSecu; // Ajustez selon les variables disponibles
//        saveMetrique();
//
//        if (isNetworkAvailable()) {
//            if (sendDataToApi()) {
//                NetworkUtils.uploadImageWithRetry(this, new File(imagePath), TransactionFse, new NetworkUtils.UploadCallback() {
//                    @Override
//                    public void onSuccess() {
//                        runOnUiThread(() -> {
//                            // Mettre à jour le statut dans la base de données
//                            SQLiteDatabase db = GlobalClass.getInstance().cnxDbAppPrestation;
//                            db.execSQL("UPDATE fse_ambulatoire SET statusProgres=1 WHERE id=" + idFse);
//
//                            progressBar.setVisibility(View.GONE);
//                            showSuccessDialog();
//                        });
//                    }
//
//                    @Override
//                    public void onError(String error) {
//                        runOnUiThread(() -> {
//                            progressBar.setVisibility(View.GONE);
//                            enregisterFse.setVisibility(View.VISIBLE);
//                            NetworkUtils.showErrorToast(FseFiniliastionSoinsAmbulatoire.this, error);
//                        });
//                    }
//                });
//            } else {
//                handleSmsSending(smsManager, identifiant);
//            }
//        } else {
//            handleSmsSending(smsManager, identifiant);
//        }
//    }
    private void uploadImage(String imagePath) {

        File imageFile = new File(imagePath);

        // Comprehensive file checks
        if (!imageFile.exists()) {
            Log.e(TAG, "Fichier introuvable: " + imagePath);
            return;
        }

        if (!imageFile.canRead()) {
            Log.e(TAG, "Impossible de lire le fichier: " + imagePath);
            return;
        }

        Log.d(TAG, "Détails fichier - Chemin: " + imageFile.getAbsolutePath());
        Log.d(TAG, "Taille fichier: " + imageFile.length() + " octets");

        // Vérifier les permissions et le stockage
        if (!imageFile.getParentFile().canWrite()) {
            Log.e(TAG, "Droits d'écriture insuffisants");
        }
        if (imagePath == null || imagePath.isEmpty()) {
            Log.e("FSE_DEBUG", "Chemin d'image vide");
            Toast.makeText(this, "Erreur: chemin d'image invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("FSE_DEBUG", "Chemin de l'image à envoyer: " + imagePath);
        Log.d("FSE_DEBUG", "Numéro de transaction: " + TransactionFse);
        System.out.println("IMAGE PATH");
        System.out.println(imagePath);
        enregisterFse.setVisibility(View.INVISIBLE);
        //progressBar.setVisibility(View.VISIBLE);
        // Ajouter cette vérification
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        FseAmbulatoire fse = fseServiceDb.getFseAmbulatoireByNumTrans(TransactionFse);
        String date_fin = activityTracker.enregistrerDateFin();
        String identifiant = (fse.getNumSecu() == null || fse.getNumSecu().isEmpty()) ? fse.getNumGuid() : fse.getNumSecu(); // Ajustez selon les variables disponibles
        saveMetrique();

        // AJOUTER CETTE LIGNE - Initialiser les variables avant l'envoi
        initializeVariablesForSms();

        if (isNetworkAvailable()) {
            if (sendDataToApi()) {
                NetworkUtils.uploadImageWithRetry(this, new File(imagePath), TransactionFse, new NetworkUtils.UploadCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Log.d("uploadImageWithRetry", "OK");
                            // Mettre à jour le statut dans la base de données
                            SQLiteDatabase db = GlobalClass.getInstance().cnxDbAppPrestation;
                            db.execSQL("UPDATE fse_ambulatoire SET statusProgres=1 WHERE id=" + idFse);

                            progressBar.setVisibility(View.GONE);
                            showSuccessDialog();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Log.d("uploadImageWithRetry", "NOK");
                            progressBar.setVisibility(View.GONE);
                            enregisterFse.setVisibility(View.VISIBLE);
                            NetworkUtils.showErrorToast(FseFiniliastionSoinsAmbulatoire.this, error);
                        });
                    }
                });
            } else {
                handleSmsSending(smsManager, identifiant,fse.getNumTrans(), fse.getType_fse());
            }
        } else {
            handleSmsSending(smsManager, identifiant,fse.getNumTrans(), fse.getType_fse());
        }
    }

    // Ajoutez les méthodes auxiliaires de PreviewActivity
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

    private void handleSmsSending(DataSMSManager smsManager, String identifiant, String numTrans, String type_bon) {
        System.out.print("SMS=>>>>>>>>>>>>>");
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
                Log.d("SMS_FINAL", "===============================");
                smsManager.sendDataViaSMS(lettreCle,identifiant, numTrans, affection1, affection2, acte1, acte2, acte3,currentDate,code_ets,idString,idFamoco,code_agac, heure,lat,longitude,type_bon);
                fseServiceDb.updateStatusProgres(numTrans);
                showSuccessDialog();
            } catch (Exception e) {
                Toast.makeText(this, "Échec envoi SMS", Toast.LENGTH_SHORT).show();
                enregisterFse.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    // 1. AJOUTER cette méthode pour initialiser les variables avant l'envoi SMS
    private void initializeVariablesForSms() {
        Log.d("SMS_DEBUG", "Initialisation des variables pour SMS");

        // Initialiser les affections
        affection1 = codeaffection1 != null ? codeaffection1 : "";
        affection2 = codeaffection2 != null ? codeaffection2 : "";

        Log.d("SMS_DEBUG", "Affection1: " + affection1);
        Log.d("SMS_DEBUG", "Affection2: " + affection2);

        // Récupérer la FSE existante pour les actes
        FseAmbulatoire fseExistante = fseServiceDb.getFseAmbulatoireByNumTrans(TransactionFse);

        if (fseExistante != null) {
            // Priorité aux actes existants dans la base
            String existingActe1 = fseExistante.getCode_acte1();
            String existingActe2 = fseExistante.getCode_acte2();
            String existingActe3 = fseExistante.getCode_acte3();

            // Combiner actes existants et nouveaux actes sélectionnés
            List<String> allActes = new ArrayList<>();

            // Ajouter les actes existants
            if (existingActe1 != null && !existingActe1.isEmpty()) {
                allActes.add(existingActe1);
            }
            if (existingActe2 != null && !existingActe2.isEmpty()) {
                allActes.add(existingActe2);
            }
            if (existingActe3 != null && !existingActe3.isEmpty()) {
                allActes.add(existingActe3);
            }

            // Ajouter les nouveaux actes sélectionnés (s'ils ne sont pas déjà présents)
            for (String selectedActe : selectedActeCodes) {
                if (selectedActe != null && !selectedActe.isEmpty() && !allActes.contains(selectedActe)) {
                    allActes.add(selectedActe);
                }
            }

            // Assigner aux variables finales
            acte1 = allActes.size() > 0 ? allActes.get(0) : "";
            acte2 = allActes.size() > 1 ? allActes.get(1) : "";
            acte3 = allActes.size() > 2 ? allActes.get(2) : "";

        } else {
            // Si pas de FSE existante, utiliser uniquement les actes sélectionnés
            acte1 = selectedActeCodes.size() > 0 ? selectedActeCodes.get(0) : "";
            acte2 = selectedActeCodes.size() > 1 ? selectedActeCodes.get(1) : "";
            acte3 = selectedActeCodes.size() > 2 ? selectedActeCodes.get(2) : "";
        }

        Log.d("SMS_DEBUG", "Acte1: " + acte1);
        Log.d("SMS_DEBUG", "Acte2: " + acte2);
        Log.d("SMS_DEBUG", "Acte3: " + acte3);
    }

// Ajoutez ici les méthodes sendDasendDataTtaToApi(), saveMetrique(), handleSmsSending() et showSuccessDialog() depuis PreviewActivity

    private void saveFse() {

        if (capturedImagePath == null || capturedImagePath.isEmpty()) {
            Toast.makeText(this, "Veuillez prendre une photo de la feuille de soins avant d'enregistrer", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d("FSE_DEBUG", "saveFse - capturedImagePath: " + (capturedImagePath != null ? capturedImagePath : "null"));
        System.out.println("CURRENT PATH");
        uploadImage(capturedImagePath);
        System.out.println(capturedImagePath);
        if (capturedImagePath != null) {
            //uploadImage(capturedImagePath);
            SQLiteDatabase db = GlobalClass.getInstance().cnxDbAppPrestation;
            try {
                db.execSQL("UPDATE fse_ambulatoire SET statusProgres=1 WHERE id=" + idFse);
                Toast.makeText(this, "FSE finalisée avec succès", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Erreur lors de l'enregistrement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Erreur lors de l'enregistrement: " + "Vérifier les informations", Toast.LENGTH_SHORT).show();
        }
    }

    /*private void saveFse() {
        SQLiteDatabase db = GlobalClass.getInstance().cnxDbAppPrestation;
        try {
            db.execSQL("UPDATE fse_ambulatoire SET statusProgres=1 WHERE id=" + idFse);
            Toast.makeText(this, "FSE finalisée avec succès", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors de l'enregistrement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }*/
        /*private void saveFse(){
            SQLiteDatabase db = GlobalClass.getInstance().cnxDbAppPrestation;
            db.execSQL("UPDATE fse_ambulatoire set statusProgres=1 where id=" + idFse);
        }**/
    /*private void openCamera(ImageView imageView) {
        selectedImageView = imageView;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, "ci.technchange.prestationscmuym.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }*/
    // Replace the openCamera method with this:
    private void openCamera(ImageView imageView) {
        selectedImageView = imageView;

        Intent cameraIntent = new Intent(this, CameraActivity2.class);
        // Pass necessary data to CameraActivity
        cameraIntent.putExtra("NUM_TRANSACTION", TransactionFse);
        cameraIntent.putExtra("ITEM_ID", idFse);
        cameraIntent.putExtra("NUM_SECU", ""); // Add this if available in your context
        cameraIntent.putExtra("GUID", ""); // Add this if available
        cameraIntent.putExtra("AFFECTION1", codeaffection1);
        cameraIntent.putExtra("AFFECTION2", codeaffection2);

        // Add acte information if needed
        if (!prestationList.isEmpty() && prestationList.size() >= 1) {
            cameraIntent.putExtra("ACTE1", prestationList.get(0).getCode());
        }
        if (!prestationList.isEmpty() && prestationList.size() >= 2) {
            cameraIntent.putExtra("ACTE2", prestationList.get(1).getCode());
        }
        if (!prestationList.isEmpty() && prestationList.size() >= 3) {
            cameraIntent.putExtra("ACTE3", prestationList.get(2).getCode());
        }

        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
    }
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return new File(storageDir, imageFileName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String ensurePermanentImageStorage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        File sourceFile = new File(imagePath);
        if (!sourceFile.exists()) {
            Log.e("FSE_DEBUG", "Image source n'existe pas: " + imagePath);
            return null;
        }

        try {
            // Créer le répertoire de destination si nécessaire
            File permanentDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "permanent_uploads");
            if (!permanentDir.exists()) {
                permanentDir.mkdirs();
            }

            // Créer un nom de fichier définitif lié à la transaction
            File destFile = new File(permanentDir, "FSE_" + TransactionFse + ".jpg");

            // Copier le fichier
            try (java.io.FileInputStream fis = new java.io.FileInputStream(sourceFile);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(destFile)) {

                byte[] buffer = new byte[8192];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.flush();
            }

            Log.d("FSE_DEBUG", "Image copiée vers stockage permanent: " + destFile.getAbsolutePath());
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("FSE_DEBUG", "Erreur lors de la copie vers stockage permanent: " + e.getMessage());
            return imagePath; // En cas d'échec, on retourne le chemin original
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("FSE_DEBUG", "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String imagePath = data.getStringExtra("imagePath");
                Log.d("FSE_DEBUG", "Image path reçu: " + (imagePath != null ? imagePath : "null"));

                if (imagePath != null && !imagePath.isEmpty()) {
                    capturedImagePath = imagePath;
                    photoUri = Uri.parse(imagePath);

                    // Vérifier que le fichier existe
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        Log.d("FSE_DEBUG", "Le fichier image existe: " + imageFile.length() + " bytes");
                        selectedImageView.setImageURI(photoUri);
                        // Activer le bouton d'enregistrement car une photo a été prise
                        enregisterFse.setEnabled(true);
                    } else {
                        Log.e("FSE_DEBUG", "Le fichier image n'existe pas: " + imagePath);
                    }
                }
            } else {
                Log.e("FSE_DEBUG", "Intent data est null");
            }
        }
    }

    private void processImage() {
        try {
            InputImage image = InputImage.fromFilePath(this, photoUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(this::extractData)
                    .addOnFailureListener(Throwable::printStackTrace);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void extractData(Text text) {
        String extractedText = text.getText();
        Toast.makeText(FseFiniliastionSoinsAmbulatoire.this, extractedText, Toast.LENGTH_SHORT).show();

        /*textView.setText("N°: " + idNumber + "\nNom: " + name + "\nPrénom(s): " + surname +
                "\nDate de Naissance: " + birthDate + "\nN° Sécurité Sociale: " + socialSecurityNumber);*/
    }

    // Méthode pour supprimer un acte de la liste
    public void supprimerActe(int position) {
        if (position >= 0 && position < prestationList.size()) {
            // Supprimer l'acte de la liste
            prestationList.remove(position);
            adapterActe.notifyDataSetChanged();

            // Mettre à jour les listes de sélection
            selectedActeCodes.remove(position);
            selectedActeTarifs.remove(position);
            selectedActeLibelles.remove(position);
            selectedActeTitres.remove(position);

            // Mettre à jour le montant total
            updateMontantTotal();

            // Mettre à jour les variables API
            updateApiActeVariables();

            // Cacher la liste si vide
            if (prestationList.isEmpty()) {
                listViewActe.setVisibility(View.GONE);
            }
        }
    }
    // Amélioration de la méthode ajouterNouvellePrestation
    private void ajouterNouvellePrestation(int cpt) {
        Log.d("DEBUG_PRESTA", "Ajout prestation - Code: " + codeActe + ", Tarif: " + tarifActe);

        // Vérifier si la prestation existe déjà
        boolean exists = false;
        for (ActeMedical acte : prestationList) {
            if (acte.getCode().equals(codeActe)) {
                exists = true;
                Log.d("DEBUG_PRESTA", "Acte déjà présent dans la liste");
                break;
            }
        }

        if (!exists) {
            // Récupérer les valeurs sélectionnées
            ActeMedical nouvellePrestation = new ActeMedical(cpt, "", codeActe, libelleActe, titreActe, tarifActe);
            prestationList.add(nouvellePrestation);

            Log.d("DEBUG_PRESTA", "Acte ajouté à prestationList, total: " + prestationList.size());

            // Notifier l'adaptateur des changements
            adapterActe.notifyDataSetChanged();

            // Rendre la ListView visible si elle ne l'est pas déjà
            /*if (listViewActe.getVisibility() != View.VISIBLE) {
                listViewActe.setVisibility(View.VISIBLE);
            }*/

            // Assurer que l'acte est aussi dans les listes de sélection
            if (!selectedActeCodes.contains(codeActe)) {
                selectedActeCodes.add(codeActe);
                selectedActeTarifs.add(tarifActe);
                selectedActeLibelles.add(libelleActe);
                selectedActeTitres.add(titreActe);
                Log.d("DEBUG_PRESTA", "Acte ajouté aux listes selected, total: " + selectedActeCodes.size());
            }

            // Mettre à jour le montant total avec le nouveau tarif
            updateMontantTotal();

            // Feedback utilisateur
            Toast.makeText(this, "Acte " + codeActe + " ajouté", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Cet acte est déjà dans la liste", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (successDialog != null && successDialog.isShowing()) {
            successDialog.dismiss();
            successDialog = null;
        }
        localisationManager.cleanup();
    }
}