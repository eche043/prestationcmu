package ci.technchange.prestationscmu.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.models.ActeMedical;
import ci.technchange.prestationscmu.models.Affection;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.models.FseItem;
import ci.technchange.prestationscmu.models.Metrique;
import ci.technchange.prestationscmu.models.Patient;
import ci.technchange.prestationscmu.printing.generatePdfForFSE;
import ci.technchange.prestationscmu.utils.ActivityTracerFseEdit;
import ci.technchange.prestationscmu.utils.ActivityTracker;
import ci.technchange.prestationscmu.utils.FseServiceDb;
import ci.technchange.prestationscmu.utils.MetriqueServiceDb;
import ci.technchange.prestationscmu.utils.ReferentielService;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;

public class FseEditActivity extends AppCompatActivity {

    private FseServiceDb fseServiceDb;
    private ReferentielService referentielService;
    private Patient currentPatient;
    private String numTrans;

    private AutoCompleteTextView autoCompleteActe1, designationActe1;
    private EditText  quantiteActe1;
    private LinearLayout containerAdditionalActes;
    private Button btnAddActe;
    private RadioGroup radioGroupOptions;
    private RadioButton radioCMR, radioEloignement, radioUrgent, radioReference, radioAutre;
    private int acteCount = 1;
    private List<ActeMedical> allActes;
    private Map<String, EditText> acteDesignationMap = new HashMap<>();

    private String typeFse;
    private FseItem item;
    private EditText champAutreLayout;
    private EditText champTotal, champPartCMU, champPartAssure;

    private Button enregisterFse, retourHome;
    private EditText champTransaction, champNumSecu, champGuid, champNomComplet,
            champDateNaiss, champNomEtablissement, champFseInitial;
    private Spinner champSexe;
    private String dateString;
    private String code_ets;
    private Random random;
    SQLiteDatabase db;
    private SharedPrefManager sharedPrefManager;
    private RequestQueue requestQueue;
    private static final String API_URL = "http://51.38.224.233:8080/api/v1/saveFSE";
    private ActivityTracker activityTracker;
    private ActivityTracerFseEdit activityTracerFseEdit;
    private static String date_fin = "";
    private UtilsInfosAppareil utilsInfos;
    private MetriqueServiceDb metriqueServiceDb;
    private String nomActivityFseEdit= "";
    private String date_debut_fseedit="";
    private String date_fin_fseedit="";
    private SimpleDateFormat dateFormat;
    private List<LinearLayout> addedActesList = new ArrayList<>(); // Pour suivre les actes ajoutés
    private int maxActes = 3;

    private TextWatcher codeWatcher;
    private TextWatcher designationWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fse_edit);

        fseServiceDb = FseServiceDb.getInstance(this);
        metriqueServiceDb = MetriqueServiceDb.getInstance(this);
        referentielService = ReferentielService.getInstance(this);
        random = new Random();
        sharedPrefManager = new SharedPrefManager(this);
        requestQueue = Volley.newRequestQueue(this);
        activityTracker = new ActivityTracker(this);
        activityTracerFseEdit = new ActivityTracerFseEdit(this);
        utilsInfos = new UtilsInfosAppareil(this);

        initializeViews();
        activityTracerFseEdit.trackActivityFseEdit("fse_edit");
        activityTracerFseEdit.enregistrerDateDebutFseEdit();


        allActes = referentielService.getAllActes();

        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateString = dateFormat.format(new Date());

        code_ets = sharedPrefManager.getCodeEts();

        numTrans = getIntent().getStringExtra("num_trans");
        typeFse = getIntent().getStringExtra("TYPE_FSE");
        currentPatient = (Patient) getIntent().getSerializableExtra("PATIENT");
        item = getIntent().getParcelableExtra("PATIENT_OLD");

        if (currentPatient != null) {
            populatePatientData();
        } else if (item != null) {
            populateFromFseItem(item);
        } else {
            Toast.makeText(this, "Aucun patient sélectionné", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (numTrans != null && !numTrans.isEmpty()) {
            champFseInitial.setText(numTrans);
            champTransaction.setText(generateTransactionNumber());
        } else {
            champTransaction.setText(generateTransactionNumber());
        }

        champTransaction.setEnabled(false);

        setupListeners();
        fetchEtablissementName();
    }

    private void initializeViews() {
        retourHome = findViewById(R.id.retourHome);
        enregisterFse = findViewById(R.id.enregistrerFiche);

        champTransaction = findViewById(R.id.champTransaction);
        champNumSecu = findViewById(R.id.champNumSecu);
        champGuid = findViewById(R.id.champGuid);
        champNomComplet = findViewById(R.id.champNomComplet);
        champDateNaiss = findViewById(R.id.champDateNaiss);
        champNomEtablissement = findViewById(R.id.champNomEtablissement);
        champAutreLayout = findViewById(R.id.champAutre);
        champFseInitial = findViewById(R.id.champFseInitial);
        quantiteActe1 = findViewById(R.id.quantiteActe1);
        champTotal = findViewById(R.id.champTotal);
        champPartCMU = findViewById(R.id.champPartCMU);
        champPartAssure = findViewById(R.id.champPartAssure);

        autoCompleteActe1 = findViewById(R.id.autoCompleteCodeActe1);
        designationActe1 = findViewById(R.id.designationActe1);
        containerAdditionalActes = findViewById(R.id.containerAdditionalActes);
        btnAddActe = findViewById(R.id.btnAddActe);

        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        radioCMR = findViewById(R.id.radioCMR);
        radioEloignement = findViewById(R.id.radioEloignement);
        radioUrgent = findViewById(R.id.radioUrgent);
        radioReference = findViewById(R.id.radioReference);
        radioAutre = findViewById(R.id.radioAutre);

        champSexe = findViewById(R.id.champSexe);
        String[] sexes = new String[]{"M", "F"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sexes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        champSexe.setAdapter(adapter);
    }

    private void setupListeners() {
        retourHome.setOnClickListener(v -> finish());


        setupActeListener(autoCompleteActe1, designationActe1, quantiteActe1);


        btnAddActe.setOnClickListener(v -> addActeField());


        radioGroupOptions.setOnCheckedChangeListener((group, checkedId) -> {
            champAutreLayout.setVisibility(checkedId == R.id.radioAutre ? View.VISIBLE : View.GONE);
        });

        enregisterFse.setOnClickListener(v -> {
            activityTracerFseEdit.enregistrerDateFinFseEdit();
            if (validateForm()) {
                if (saveFse()) {
                    showdModal();
                }
            }
        });
    }
    private void setupActeListener(AutoCompleteTextView codeActeView, AutoCompleteTextView designationView, EditText quantiteView) {

        ArrayAdapter<String> codeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                getActeCodes());

        ArrayAdapter<String> designationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                getActeDesignations());

        codeActeView.setAdapter(codeAdapter);
        designationView.setAdapter(designationAdapter);
        codeActeView.setThreshold(1);
        designationView.setThreshold(1);


        final Map<String, String> codeToDesignation = new HashMap<>();
        final Map<String, String> designationToCode = new HashMap<>();
        for (ActeMedical acte : allActes) {
            codeToDesignation.put(acte.getCode(), acte.getLibelle());
            designationToCode.put(acte.getLibelle(), acte.getCode());
        }

        codeWatcher = new TextWatcher() {
            private boolean isInternalUpdate = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isInternalUpdate) return;

                String code = s.toString().trim();
                if (!code.isEmpty()) {
                    isInternalUpdate = true;
                    String designation = codeToDesignation.get(code);
                    designationView.setText(designation != null ? designation : "");
                    isInternalUpdate = false;

                    codeActeView.setBackgroundResource(designation != null ?
                            android.R.drawable.editbox_background : R.drawable.error_background);
                }
                updateMontants();
            }
        };

        designationWatcher = new TextWatcher() {
            private boolean isInternalUpdate = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isInternalUpdate) return;

                String designation = s.toString().trim();
                if (!designation.isEmpty()) {
                    isInternalUpdate = true;
                    String code = designationToCode.get(designation);
                    codeActeView.setText(code != null ? code : "");
                    isInternalUpdate = false;

                    designationView.setBackgroundResource(code != null ?
                            android.R.drawable.editbox_background : R.drawable.error_background);
                }
                updateMontants();
            }
        };

        codeActeView.addTextChangedListener(codeWatcher);
        designationView.addTextChangedListener(designationWatcher);


        codeActeView.addTextChangedListener(codeWatcher);
        designationView.addTextChangedListener(designationWatcher);


        quantiteView.addTextChangedListener(new SimpleTextWatcher(this::updateMontants));
    }


    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable action;

        SimpleTextWatcher(Runnable action) {
            this.action = action;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { action.run(); }
    }

    /*private void setupActeListener(AutoCompleteTextView codeActeView, AutoCompleteTextView designationView, EditText quantiteView) {
        // Adapter pour les codes
        ArrayAdapter<String> codeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                getActeCodes());
        codeActeView.setAdapter(codeAdapter);
        codeActeView.setThreshold(1);

        // Adapter pour les désignations
        ArrayAdapter<String> designationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                getActeDesignations());
        designationView.setAdapter(designationAdapter);
        designationView.setThreshold(1);

        // Map pour faire la correspondance code <-> désignation
        Map<String, String> codeToDesignation = new HashMap<>();
        Map<String, String> designationToCode = new HashMap<>();
        for (ActeMedical acte : allActes) {
            codeToDesignation.put(acte.getCode(), acte.getLibelle());
            designationToCode.put(acte.getLibelle(), acte.getCode());
        }

        // Déclaration des TextWatchers comme variables finales
        final TextWatcher[] codeWatcher = new TextWatcher[1];
        final TextWatcher[] designationWatcher = new TextWatcher[1];

        // Écouteur pour le code acte
        codeWatcher[0] = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String code = s.toString().trim();
                if (!code.isEmpty()) {
                    String designation = codeToDesignation.get(code);
                    if (designation != null) {
                        // Mettre à jour la désignation sans déclencher son écouteur
                        designationView.removeTextChangedListener(designationWatcher[0]);
                        designationView.setText(designation);
                        designationView.addTextChangedListener(designationWatcher[0]);

                        codeActeView.setBackgroundResource(android.R.drawable.editbox_background);
                    } else {
                        designationView.setText("");
                        codeActeView.setBackgroundColor(Color.RED);
                    }
                } else {
                    codeActeView.setBackgroundResource(android.R.drawable.editbox_background);
                    designationView.setText("");
                }
                updateMontants();
            }
        };
        codeActeView.addTextChangedListener(codeWatcher[0]);

        // Écouteur pour la désignation
        designationWatcher[0] = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String designation = s.toString().trim();
                if (!designation.isEmpty()) {
                    String code = designationToCode.get(designation);
                    if (code != null) {
                        // Mettre à jour le code sans déclencher son écouteur
                        codeActeView.removeTextChangedListener(codeWatcher[0]);
                        codeActeView.setText(code);
                        codeActeView.addTextChangedListener(codeWatcher[0]);

                        designationView.setBackgroundResource(android.R.drawable.editbox_background);
                    } else {
                        codeActeView.setText("");
                        designationView.setBackgroundColor(Color.RED);
                    }
                } else {
                    designationView.setBackgroundResource(android.R.drawable.editbox_background);
                    codeActeView.setText("");
                }
                updateMontants();
            }
        };
        designationView.addTextChangedListener(designationWatcher[0]);

        // Écouteur pour la quantité
        quantiteView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    updateMontants();
                }
            }
        });
    }**/


    /*private List<String> getActeDesignations() {
        List<String> designations = new ArrayList<>();
        for (ActeMedical acte : allActes) {
            designations.add(acte.getLibelle());
        }
        return designations;
    }**/

    /*private void setupActeListener(AutoCompleteTextView codeActeView, EditText designationView, EditText quantiteView) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                getActeCodes());
        codeActeView.setAdapter(adapter);
        codeActeView.setThreshold(1);

        codeActeView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String code = s.toString().trim();
                if (!code.isEmpty()) {
                    ActeMedical acte = referentielService.getActeMedicalByCode(code);
                    if (acte != null) {
                        designationView.setText(acte.getLibelle());
                        codeActeView.setBackgroundResource(android.R.drawable.editbox_background);

                        // Afficher les détails du calcul dans les logs
                        double montant = referentielService.calculerMontantActe(code);
                        Log.d("ACTE", "Acte valide: " + code +
                                "\nLibellé: " + acte.getLibelle() +
                                "\nCoefficient: " + acte.getCoeficient() +
                                "\nLettre clé: " + acte.getLettreCle() +
                                "\nMontant: " + montant);
                    } else {
                        designationView.setText("");
                        codeActeView.setBackgroundColor(Color.RED);
                        Log.d("ACTE", "Acte invalide: " + code);
                    }
                } else {
                    codeActeView.setBackgroundResource(android.R.drawable.editbox_background);
                    designationView.setText("");
                }
                updateMontants();
            }
        });

        quantiteView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    updateMontants();
                }
            }
        });
    }**/
    private void addActeField() {
        // Vérification du nombre maximum d'actes
        if (addedActesList.size() >= maxActes - 1) { // -1 car on a déjà 1 acte fixe
            Toast.makeText(this, "Maximum " + maxActes + " actes autorisés", Toast.LENGTH_SHORT).show();
            return;
        }

        // Création du conteneur pour l'acte
        LinearLayout acteLayout = new LinearLayout(this);
        acteLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        acteLayout.setOrientation(LinearLayout.HORIZONTAL);
        acteLayout.setWeightSum(4); // Pour 4 éléments (code, désignation, quantité, bouton)

        // 1. Champ Code Acte (AutoCompleteTextView)
        AutoCompleteTextView codeActe = new AutoCompleteTextView(this);
        codeActe.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.5f));
        codeActe.setHint("Code acte");
        codeActe.setPadding(12, 12, 12, 12);
        codeActe.setThreshold(1);
        codeActe.setBackgroundResource(android.R.drawable.editbox_background);

        // 2. Champ Désignation (AutoCompleteTextView)
        AutoCompleteTextView designation = new AutoCompleteTextView(this);
        designation.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.5f));
        designation.setHint("Désignation");
        designation.setPadding(12, 12, 12, 12);
        designation.setThreshold(1);
        designation.setBackgroundResource(android.R.drawable.editbox_background);

        // 3. Champ Quantité (EditText)
        EditText quantite = new EditText(this);
        quantite.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));
        quantite.setHint("Quantité");
        quantite.setPadding(12, 12, 12, 12);
        quantite.setInputType(InputType.TYPE_CLASS_NUMBER);
        quantite.setText("1"); // Valeur par défaut
        quantite.setBackgroundResource(android.R.drawable.editbox_background);

        // 4. Bouton Supprimer
        ImageButton btnRemove = new ImageButton(this);
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.5f);
        removeParams.gravity = Gravity.CENTER_VERTICAL;
        btnRemove.setLayoutParams(removeParams);
        btnRemove.setImageResource(android.R.drawable.ic_delete);
        btnRemove.setBackgroundResource(android.R.drawable.btn_default);
        btnRemove.setPadding(8, 8, 8, 8);
        btnRemove.setOnClickListener(v -> removeActe(acteLayout));

        // Configuration des adapters pour les AutoCompleteTextView
        ArrayAdapter<String> codeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                getActeCodes());
        codeActe.setAdapter(codeAdapter);

        ArrayAdapter<String> designationAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                getActeDesignations());
        designation.setAdapter(designationAdapter);


        setupActeListener(codeActe, designation, quantite);


        quantite.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateMontants(); }
        });

        // Ajout des vues au layout
        acteLayout.addView(codeActe);
        acteLayout.addView(designation);
        acteLayout.addView(quantite);
        acteLayout.addView(btnRemove);


        containerAdditionalActes.addView(acteLayout);
        addedActesList.add(acteLayout);
        updateMontants();
    }

    private List<String> getActeDesignations() {
        List<String> designations = new ArrayList<>();
        try {
            if (GlobalClass.getInstance().cnxDbReferentiel == null) {
                GlobalClass.getInstance().initDatabase("referentiel");
            }
            SQLiteDatabase db = GlobalClass.getInstance().cnxDbReferentiel;

            Cursor cursor = db.rawQuery("SELECT libelle FROM ref_actes_medicaux", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    designations.add(cursor.getString(0));
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Erreur lors de la récupération des désignations", e);
        }
        return designations;
    }
    /*private void addActeField() {
        // Vérification du nombre maximum d'actes
        if (addedActesList.size() >= maxActes - 1) { // -1 car on a déjà 1 acte fixe
            Toast.makeText(this, "Maximum " + maxActes + " actes autorisés", Toast.LENGTH_SHORT).show();
            return;
        }

        // Création du conteneur pour l'acte
        LinearLayout acteLayout = new LinearLayout(this);
        acteLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        acteLayout.setOrientation(LinearLayout.HORIZONTAL);
        acteLayout.setWeightSum(4); // Pour 4 éléments (code, désignation, quantité, bouton)

        // 1. Champ Code Acte
        AutoCompleteTextView codeActe = new AutoCompleteTextView(this);
        codeActe.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.5f));
        codeActe.setHint("Code acte");
        codeActe.setPadding(12, 12, 12, 12);
        codeActe.setThreshold(1);
        codeActe.setBackgroundResource(android.R.drawable.editbox_background);

        // 2. Champ Désignation
        EditText designation = new EditText(this);
        designation.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.5f));
        designation.setHint("Désignation");
        designation.setPadding(12, 12, 12, 12);
        designation.setEnabled(false);
        designation.setBackgroundResource(android.R.drawable.editbox_background);

        // 3. Champ Quantité
        EditText quantite = new EditText(this);
        quantite.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));
        quantite.setHint("Quantité");
        quantite.setPadding(12, 12, 12, 12);
        quantite.setInputType(InputType.TYPE_CLASS_NUMBER);
        quantite.setText("1"); // Valeur par défaut
        quantite.setBackgroundResource(android.R.drawable.editbox_background);

        // 4. Bouton Supprimer

        ImageButton btnRemove = new ImageButton(this);
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.5f);
        removeParams.gravity = Gravity.CENTER_VERTICAL;
        btnRemove.setLayoutParams(removeParams);
        btnRemove.setImageResource(android.R.drawable.ic_delete);
        btnRemove.setBackgroundResource(android.R.drawable.btn_default);
        btnRemove.setPadding(8, 8, 8, 8);
        btnRemove.setOnClickListener(v -> removeActe(acteLayout));

        // Configuration AutoComplete
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                getActeCodes());
        codeActe.setAdapter(adapter);

        // Écouteurs
        setupActeListener(codeActe, designation, quantite);
        quantite.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateMontants(); }
        });

        // Ajout des vues
        acteLayout.addView(codeActe);
        acteLayout.addView(designation);
        acteLayout.addView(quantite);
        acteLayout.addView(btnRemove);

        // Ajout au conteneur et à la liste
        containerAdditionalActes.addView(acteLayout);
        addedActesList.add(acteLayout);
        updateMontants();
    }**/

    private void removeActe(LinearLayout acteLayout) {
        if (acteLayout.getParent() != null) {
            containerAdditionalActes.removeView(acteLayout);
            addedActesList.remove(acteLayout);
            updateMontants();
        }
    }

    /*private List<String> getActeCodes() {
        List<String> codes = new ArrayList<>();
        for (ActeMedical acte : allActes) {
            codes.add(acte.getCode());
        }
        return codes;
    }**/

    private List<String> getActeCodes() {
        List<String> codes = new ArrayList<>();
        Cursor cursor = null;

        try {
            if (GlobalClass.getInstance().cnxDbReferentiel == null) {
                GlobalClass.getInstance().initDatabase("referentiel");
            }
            db = GlobalClass.getInstance().cnxDbReferentiel;

            cursor = db.rawQuery("SELECT code FROM ref_actes_medicaux", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    codes.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Erreur lors de la récupération des codes", e);
        } finally {
            if (cursor != null) cursor.close();
        }

        Log.d("ACTES", "Nombre de codes chargés: " + codes.size());
        return codes;
    }

    private boolean validateActes() {
        boolean isValid = true;


        String code1 = autoCompleteActe1.getText().toString().trim();
        if (!code1.isEmpty()) {
            boolean found = false;
            for (ActeMedical acte : allActes) {
                if (acte.getCode().equals(code1)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                autoCompleteActe1.setBackgroundColor(Color.RED);
                isValid = false;
            }
        }


        for (int i = 0; i < containerAdditionalActes.getChildCount(); i++) {
            View child = containerAdditionalActes.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) child;
                AutoCompleteTextView codeView = (AutoCompleteTextView) layout.getChildAt(0);
                String code = codeView.getText().toString().trim();

                if (!code.isEmpty()) {
                    boolean found = false;
                    for (ActeMedical acte : allActes) {
                        if (acte.getCode().equals(code)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        codeView.setBackgroundColor(Color.RED);
                        isValid = false;
                    }
                }
            }
        }

        if (!isValid) {
            Toast.makeText(this, "Un ou plusieurs codes acte sont invalides", Toast.LENGTH_LONG).show();
        }

        return isValid;
    }

    private boolean validateForm() {
        if (champNomEtablissement.getText().toString().trim().isEmpty()) {
            champNomEtablissement.setError("Le nom de l'établissement est requis");
            return false;
        }

        return validateActes();
    }
    private void updateMontants() {
        Log.d("MONTANTS", "Mise à jour des montants");
        final double total = calculateTotal();
        final double partCMU = total * 0.7;
        final double partAssure = total * 0.3;
        final DecimalFormat df = new DecimalFormat("#.##");
        df.setMaximumFractionDigits(2);

        runOnUiThread(() -> {
            champTotal.setText(df.format(total));
            champPartCMU.setText(df.format(partCMU));
            champPartAssure.setText(df.format(partAssure));
            Log.d("MONTANTS", "Affichage - Total: " + total + ", CMU: " + partCMU + ", Assuré: " + partAssure);
        });
    }

    /*private void updateMontants() {
        final double total = calculateTotal();
        final double partCMU = total * 0.7;
        final double partAssure = total * 0.3;
        final DecimalFormat df = new DecimalFormat("#.##");
        df.setMaximumFractionDigits(2);

        runOnUiThread(() -> {
            champTotal.setText(df.format(total));
            champPartCMU.setText(df.format(partCMU));
            champPartAssure.setText(df.format(partAssure));
        });
    }**/
    private void reorganiserActesAfterDeletion() {
        for (int i = 0; i < containerAdditionalActes.getChildCount(); i++) {
            View child = containerAdditionalActes.getChildAt(i);
            child.setTag("acte_layout_" + (i + 1));
        }
    }

    private double calculateTotal() {
        double total = 0;

        // Premier acte (toujours présent)
        String code1 = autoCompleteActe1.getText().toString().trim();
        if (!code1.isEmpty()) {
            ActeMedical acte = referentielService.getActeMedicalByCode(code1);
            if (acte != null) {
                int quantite = 1;
                try {
                    quantite = Integer.parseInt(quantiteActe1.getText().toString());
                } catch (NumberFormatException e) {
                    quantite = 1;
                }
                total += referentielService.calculerMontantActe(code1) * quantite;
            }
        }

        // Actes supplémentaires
        for (int i = 0; i < containerAdditionalActes.getChildCount(); i++) {
            View child = containerAdditionalActes.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) child;
                AutoCompleteTextView codeView = (AutoCompleteTextView) layout.getChildAt(0);
                EditText quantiteView = (EditText) layout.getChildAt(2);

                String code = codeView.getText().toString().trim();
                if (!code.isEmpty()) {
                    int quantite = 1;
                    try {
                        quantite = Integer.parseInt(quantiteView.getText().toString());
                    } catch (NumberFormatException e) {
                        quantite = 1;
                    }
                    total += referentielService.calculerMontantActe(code) * quantite;
                }
            }
        }

        return total;
    }

    /*private double calculateTotal() {
        double total = 0;
        Log.d("CALCUL", "Début du calcul du total");

        // Premier acte
        String code1 = autoCompleteActe1.getText().toString().trim();
        if (!code1.isEmpty()) {
            int quantite = 1;
            try {
                String qteText = quantiteActe1.getText().toString();
                if (!qteText.isEmpty()) {
                    quantite = Integer.parseInt(qteText);
                }
            } catch (NumberFormatException e) {
                quantite = 1;
            }

            double montantActe = referentielService.calculerMontantActe(code1) * quantite;
            total += montantActe;
            Log.d("CALCUL", "Acte " + code1 + ": " + quantite + " x " +
                    referentielService.calculerMontantActe(code1) + " = " + montantActe);
        }

        // Actes supplémentaires
        for (int i = 0; i < containerAdditionalActes.getChildCount(); i++) {
            View child = containerAdditionalActes.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) child;

                AutoCompleteTextView codeView = (AutoCompleteTextView) layout.getChildAt(0);
                EditText quantiteView = (EditText) layout.getChildAt(2);

                String code = codeView.getText().toString().trim();
                if (!code.isEmpty()) {
                    int quantite = 1;
                    try {
                        String qteText = quantiteView.getText().toString();
                        if (!qteText.isEmpty()) {
                            quantite = Integer.parseInt(qteText);
                        }
                    } catch (NumberFormatException e) {
                        quantite = 1;
                    }

                    double montantActe = referentielService.calculerMontantActe(code) * quantite;
                    total += montantActe;
                    Log.d("CALCUL", "Acte " + code + ": " + quantite + " x " +
                            referentielService.calculerMontantActe(code) + " = " + montantActe);
                }
            }
        }

        Log.d("CALCUL", "Total calculé: " + total);
        return total;
    }**/





    private boolean saveFse() {
        try {
            FseAmbulatoire fseAmbulatoire = new FseAmbulatoire();

            // Récupération des informations de base
            fseAmbulatoire.setNumTrans(champTransaction.getText().toString());
            fseAmbulatoire.setNumFsInitial(champFseInitial.getText().toString());
            fseAmbulatoire.setNumSecu(champNumSecu.getText().toString());
            fseAmbulatoire.setNumGuid(champGuid.getText().toString());
            fseAmbulatoire.setNomComplet(champNomComplet.getText().toString());
            fseAmbulatoire.setSexe(champSexe.getSelectedItem().toString());
            fseAmbulatoire.setDateNaissance(champDateNaiss.getText().toString());
            fseAmbulatoire.setNomEtablissement(champNomEtablissement.getText().toString());
            fseAmbulatoire.setType_fse(typeFse);
            fseAmbulatoire.setCodeEts(code_ets);
            fseAmbulatoire.setDate_soins(dateString);

            // Gestion des actes et quantités
            String code1 = autoCompleteActe1.getText().toString().trim();
            String quantite1 = quantiteActe1.getText().toString().trim();

            StringBuilder actesBuilder = new StringBuilder();
            StringBuilder quantitesBuilder = new StringBuilder();
            if (!code1.isEmpty()) {
                fseAmbulatoire.setCode_acte1(code1);
                fseAmbulatoire.setQuantite_1(!quantite1.isEmpty() ? quantite1 : "1");
            }

            /*if (!code1.isEmpty()) {
                actesBuilder.append(code1);
                quantitesBuilder.append(!quantite1.isEmpty() ? quantite1 : "1"); // Valeur par défaut 1

                // Parcourir les actes supplémentaires
                for (int i = 0; i < containerAdditionalActes.getChildCount(); i++) {
                    View child = containerAdditionalActes.getChildAt(i);
                    if (child instanceof LinearLayout) {
                        LinearLayout layout = (LinearLayout) child;
                        AutoCompleteTextView codeView = (AutoCompleteTextView) layout.getChildAt(0);
                        EditText quantiteView = (EditText) layout.getChildAt(2); // Index 2 pour la quantité

                        String code = codeView.getText().toString().trim();
                        String quantite = quantiteView.getText().toString().trim();

                        if (!code.isEmpty()) {
                            actesBuilder.append(";").append(code);
                            quantitesBuilder.append(";").append(!quantite.isEmpty() ? quantite : "1");
                        }
                    }
                }
            }*/

            // Traiter les actes supplémentaires - à mettre dans les colonnes appropriées
            if (containerAdditionalActes.getChildCount() > 0) {
                for (int i = 0; i < Math.min(containerAdditionalActes.getChildCount(), 2); i++) { // Limité à 2 actes supplémentaires
                    View child = containerAdditionalActes.getChildAt(i);
                    if (child instanceof LinearLayout) {
                        LinearLayout layout = (LinearLayout) child;
                        AutoCompleteTextView codeView = (AutoCompleteTextView) layout.getChildAt(0);
                        EditText quantiteView = (EditText) layout.getChildAt(2);

                        String code = codeView.getText().toString().trim();
                        String quantite = quantiteView.getText().toString().trim();

                        if (!code.isEmpty()) {
                            if (i == 0) { // Premier acte supplémentaire = deuxième acte
                                fseAmbulatoire.setCode_acte2(code);
                                fseAmbulatoire.setQuantite_2(!quantite.isEmpty() ? quantite : "1");
                            } else if (i == 1) { // Deuxième acte supplémentaire = troisième acte
                                fseAmbulatoire.setCode_acte3(code);
                                fseAmbulatoire.setQuantite_3(!quantite.isEmpty() ? quantite : "1");
                            }
                        }
                    }
                }
            }

            //fseAmbulatoire.setCode_acte1(actesBuilder.toString());
            //fseAmbulatoire.setQuantite_1(quantitesBuilder.toString());

            // Gestion des options d'établissement
            int selectedId = radioGroupOptions.getCheckedRadioButtonId();
            fseAmbulatoire.setEtablissementCmr(selectedId == R.id.radioCMR);
            fseAmbulatoire.setEtablissementEloignement(selectedId == R.id.radioEloignement);
            fseAmbulatoire.setEtablissementUrgent(selectedId == R.id.radioUrgent);
            fseAmbulatoire.setEtablissementRef(selectedId == R.id.radioReference);

            if (selectedId == R.id.radioAutre) {
                fseAmbulatoire.setInfoAutre(true);
                fseAmbulatoire.setEtablissementPrecision(champAutreLayout.getText().toString());
            }

            // Calcul des montants
            double total = calculateTotal();
            DecimalFormat df = new DecimalFormat("#.##");
            df.setMaximumFractionDigits(2);

            fseAmbulatoire.setMontant_acte(df.format(total));
            fseAmbulatoire.setPart_cmu(df.format(total * 0.7));
            fseAmbulatoire.setPart_assure(df.format(total * 0.3));

            // Statut et autres informations
            fseAmbulatoire.setStatusProgres(0);
            fseAmbulatoire.setPreInscription(false);
            fseAmbulatoire.setNombre_jour(0);

            // Insertion en base de données
            long result = fseServiceDb.insertFseAmbulatoire(fseAmbulatoire);

            if (result != -1) {
                Toast.makeText(this, "FSE enregistrée avec succès", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(this, "Erreur lors de l'enregistrement de la FSE", Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }
    }


    private void showdModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        View customView = getLayoutInflater().inflate(R.layout.custom_dialog_layout, null);
        builder.setView(customView); // Définir la vue personnalisée

        builder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        saveMetrique();
                        Intent intent = new Intent(FseEditActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setPositiveButton("Imprimer", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showdModal2();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();


        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }

        if (negativeButton != null) {
            negativeButton.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }



    private void showdModal2() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Créer une nouvelle mise en page pour notre dialogue
        LinearLayout customLayout = new LinearLayout(this);
        customLayout.setOrientation(LinearLayout.VERTICAL);
        customLayout.setPadding(30, 30, 30, 30);

        // Créer un TextView pour le message
        TextView messageTextView = new TextView(this);
        messageTextView.setText("Impression en cours...");
        messageTextView.setGravity(Gravity.CENTER);
        messageTextView.setTextSize(18);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.gravity = Gravity.CENTER;
        textParams.bottomMargin = 30;
        messageTextView.setLayoutParams(textParams);

        // Créer un ProgressBar
        ProgressBar spinner = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        params.topMargin = 20;
        params.bottomMargin = 20;
        spinner.setLayoutParams(params);

        // Ajouter les vues à notre mise en page
        customLayout.addView(messageTextView);
        customLayout.addView(spinner);

        // Utiliser notre mise en page personnalisée
        builder.setView(customLayout);

        // Pas de boutons
        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Traiter les actions dans un thread séparé
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Exécuter les opérations
                saveMetrique();

                // Exécuter l'impression sur le thread UI pour éviter l'erreur
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            impression();
                        } finally {
                            // Naviguer vers MainActivity après l'impression (même si une erreur survient)
                            dialog.dismiss();
                            Intent intent = new Intent(FseEditActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
            }
        }).start();
    }


    private void impression() {
        try {
            if (champTransaction == null || champNumSecu == null || champNomComplet == null ||
                    champDateNaiss == null || champNomEtablissement == null || champAutreLayout == null) {
                Log.e("impression", "Un ou plusieurs champs ne sont pas initialisés.");
                Toast.makeText(this, "Erreur : champs non initialisés", Toast.LENGTH_SHORT).show();
                return;
            }

            String numTrans = champTransaction.getText().toString();
            String numFsInitiale = champFseInitial.getText().toString();
            FseAmbulatoire fse = fseServiceDb.getFseAmbulatoireByNumTrans(numTrans);
            Map<String, Object> data = new HashMap<>();
            data.put("dateSoins", fse.getDate_soins());
            data.put("OGD", "OGD Value");
            data.put("numFsInitiale", numFsInitiale != null ? numFsInitiale : "");
            data.put("numTransaction", champTransaction.getText().toString());
            data.put("numEntentePrealable", "345678");
            data.put("numEntentePrealableAC", "901234");
            data.put("nomPrenomsAssure", champNomComplet.getText().toString());
            data.put("numSecu", champNumSecu.getText().toString());
            data.put("dateNaissance", champDateNaiss.getText().toString());
            data.put("genre", champSexe.getSelectedItem().toString());
            data.put("numAssureAC", "074852496241");
            data.put("codeAC", "AC123");
            data.put("nomAC", "Nom AC");
            data.put("codeEtablissement", code_ets);
            data.put("nomEtablissement", champNomEtablissement.getText().toString());
            data.put("typeFSE", "Type FSE");
            int selectedId = radioGroupOptions.getCheckedRadioButtonId();
            data.put("cocheCMR", selectedId == R.id.radioCMR);
            data.put("cocheUrgence", selectedId == R.id.radioUrgent);
            data.put("cocheEloignement", selectedId == R.id.radioEloignement);
            data.put("cochereference", selectedId == R.id.radioReference);
            data.put("cocheAutre", selectedId == R.id.radioAutre);
            data.put("precisionEtablissementAccueil", champAutreLayout.getText().toString());
            data.put("codeProfessionnelSante", "PS123");
            data.put("nomProfessionnelSante", "Dr. John Doe");
            data.put("specialiteProfessionnelSante", "Généraliste");
            data.put("infoComp_Maternite", "Info Maternité");
            data.put("infoComp_AVP", "Info AVP");
            data.put("infoComp_ATMP", "Info ATMP");
            data.put("infoComp_AUTRE", "Info Autre");
            data.put("infoComp_PROGSPECIAL", "Programme Spécial");
            data.put("infoComp_CODE", "Code Info");
            data.put("infoComp_IMMVEH", "Imm Véhicule");
            data.put("infoComp_Observation", "Observation");
            data.put("codeAffection1", "Aff1");
            data.put("codeAffection2", "Aff2");

            List<Map<String, Object>> prestations = new ArrayList<>();
            Map<String, Object> prestation1 = new HashMap<>();
            prestation1.put("codeActe", "Acte001");
            prestation1.put("designation", "Désignation 1");
            prestation1.put("dateDebut", "2025-02-01");
            prestation1.put("dateFin", "2025-02-05");
            prestation1.put("numDent", "12");
            prestation1.put("quantite", 1);
            prestation1.put("montant", champTotal.getText().toString());
            prestation1.put("partCmu", champPartCMU.getText().toString());
            prestation1.put("partAC", 30.0);
            prestation1.put("partAssure", champPartAssure.getText().toString());
            prestations.add(prestation1);

            data.put("prestations", prestations);
            /**
             * TODO: A complèter en s'inspirant du fichier TestSendFSeToPrint
             *
             */

            Gson gson = new Gson();
            String jsonData = gson.toJson(data);
            generatePdfForFSE fsp = new generatePdfForFSE(jsonData);

            if (fse != null) {
                Toast.makeText(this, "FSE imprimée avec succès : " + fse.getNumTrans(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Aucune FSE trouvée pour ce numéro de transaction", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors de l'impression de la FSE: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("tag","ok" +e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean sendDataToApi() {
        try {
            // Créer l'objet JSON principal
            JSONObject jsonBody = new JSONObject();

            // Date actuelle formattée
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());


            // Compléter les champs requis
            jsonBody.put("type_bon", "");
            jsonBody.put("date_soins", currentDate);
            jsonBody.put("numTrans", champTransaction.getText().toString());
            jsonBody.put("numOgd", ""); // Valeur par défaut
            jsonBody.put("numSecu", champNumSecu.getText().toString());
            jsonBody.put("numGuid", champGuid.getText().toString());
            jsonBody.put("nomComplet", champNomComplet.getText().toString());
            //jsonBody.put("dateNaissance", champDateNaiss.getText().toString());
            try {
                String inputDateStr = champDateNaiss.getText().toString();
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
            jsonBody.put("nomEtablissement", champNomEtablissement.getText().toString());
            jsonBody.put("codeEtablissement", "");
            jsonBody.put("sexe", champSexe.getSelectedItem().toString());

            // Construire l'objet statut_etablissement
            JSONObject statutEtablissement = new JSONObject();
            int selectedId = radioGroupOptions.getCheckedRadioButtonId();
            statutEtablissement.put("CMR", selectedId == R.id.radioCMR ? 1 : 0);
            statutEtablissement.put("URGENCE", selectedId == R.id.radioUrgent ? 1 : 0);
            statutEtablissement.put("ELOIGNEMENT", selectedId == R.id.radioEloignement ? 1 : 0);
            statutEtablissement.put("REFERENCE", selectedId == R.id.radioReference ? 1 : 0);
            statutEtablissement.put("AUTRE", selectedId == R.id.radioAutre ? 1 : 0);
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
           // jsonBody.put("observations", observations);

            // Champs vides pour respecter la structure
            jsonBody.put("nom_vehicule", "");
            jsonBody.put("num_assurance_vehicule", "");
            jsonBody.put("nom_societe", "");
            jsonBody.put("num_societe", "");

            // Prestations (tableau vide pour respecter la structure)
            JSONArray prestations = new JSONArray();
            jsonBody.put("prestations", prestations);

            // Type de prestation
            JSONObject typePresta = new JSONObject();
            typePresta.put("examen", 1);
            typePresta.put("exeat", 0);
            typePresta.put("refere", 0);
            typePresta.put("hospitalisation", 0);
            typePresta.put("deces", 0);
            jsonBody.put("type_presta", typePresta);

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
                            Toast.makeText(FseEditActivity.this, "FSE envoyée avec succès à l'API", Toast.LENGTH_SHORT).show();

                            // Enregistrer localement aussi
                            saveFse();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("API_ERROR", "Error: " + error.toString());
                            Toast.makeText(FseEditActivity.this, "Erreur lors de l'envoi à l'API: " + error.getMessage(), Toast.LENGTH_LONG).show();

                            // Enregistrer localement en cas d'échec
                            saveFse();
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



    private boolean saveMetrique() {
        try {
            Metrique metrique = new Metrique();
            String downloadedFileName = sharedPrefManager.getDownloadedFileName();
            String idFamoco = utilsInfos.recupererIdAppareil();
            Log.d("Famoco", "ID famoco est : "+idFamoco);
            int id = RegionUtils.getRegionid(downloadedFileName);
            String activite = activityTracerFseEdit.getLastActivityFseEdit();
            String date_debut = activityTracerFseEdit.getDateDebutFseEdit();
            String date_fin =activityTracerFseEdit.getDateFinFseEdit();
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

    private void populatePatientData() {
        champNumSecu.setText(currentPatient.getNumSecu());
        champGuid.setText(currentPatient.getGuid());

        String nomComplet = currentPatient.getNom();
        if (currentPatient.getPrenoms() != null && !currentPatient.getPrenoms().isEmpty()) {
            nomComplet += " " + currentPatient.getPrenoms();
        }
        champNomComplet.setText(nomComplet);

        champDateNaiss.setText(currentPatient.getDateNaissance());
        if ("M".equals(currentPatient.getSexe())) {
            champSexe.setSelection(0);
        } else if ("F".equals(currentPatient.getSexe())) {
            champSexe.setSelection(1);
        }

        if (currentPatient.getCmr() != null) {
            radioCMR.setChecked(currentPatient.getCmr().equals("1") ||
                    currentPatient.getCmr().equalsIgnoreCase("true"));
        } else {
            radioCMR.setEnabled(false);
            radioCMR.setAlpha(0.5f);
        }

        champNumSecu.setEnabled(false);
        champGuid.setEnabled(false);
        champNomComplet.setEnabled(false);
        champDateNaiss.setEnabled(false);
        champSexe.setEnabled(false);
        champFseInitial.setEnabled(false);

    }

    private void populateFromFseItem(FseItem item) {

        champFseInitial.setEnabled(false);
        champTransaction.setText(item.getTransactionNumber());
        champNumSecu.setText(item.getSecurityNumber());
        champGuid.setText(item.getGuid());
        champNomComplet.setText(item.getFullName());
        champDateNaiss.setText(item.getDateNaissance());

        if ("M".equals(item.getSexe())) {
            champSexe.setSelection(0);
        } else if ("F".equals(item.getSexe())) {
            champSexe.setSelection(1);
        }

        radioCMR.setChecked(item.isEtablissementCmr());


        champNumSecu.setEnabled(false);
        champGuid.setEnabled(false);
        champNomComplet.setEnabled(false);
        champDateNaiss.setEnabled(false);
        champSexe.setEnabled(false);
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }


    @SuppressLint("LongLogTag")
    private void fetchEtablissementName() {
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
                return;
            }

            // Récupération du code ETS
            String code_ets = sharedPrefManager.getCodeEts();
            if (code_ets == null || code_ets.isEmpty()) {
                Log.e("fetchEtablissementName", "Aucun code ETS trouvé dans les préférences");
                Toast.makeText(this, "Aucun établissement sélectionné", Toast.LENGTH_SHORT).show();
                return;
            }

            // Requête paramétrée (plus sécurisée contre les injections SQL)
            String query = "SELECT etablissement FROM etablissements WHERE code_ets = ?";
            cursor = db.rawQuery(query, new String[]{code_ets});

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("etablissement");
                if (columnIndex != -1) {
                    @SuppressLint("Range") String etablissement = cursor.getString(columnIndex);
                    if (champNomEtablissement != null) {
                        champNomEtablissement.setText(etablissement);
                    } else {
                        Log.e("fetchEtablissementName", "champNomEtablissement n'est pas initialisé");
                    }
                } else {
                    Log.e("fetchEtablissementName", "Colonne 'etablissement' non trouvée");
                    Toast.makeText(this, "Erreur: Structure de base de données invalide", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("fetchEtablissementName", "Aucun résultat pour code_ets: " + code_ets);
                Toast.makeText(this, "Aucun établissement trouvé pour ce code", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("fetchEtablissementName", "Erreur: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de la récupération", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Ne pas fermer db car elle est gérée par GlobalClass
        }
    }

    private String generateTransactionNumber() {
        String part1 = String.format("%04d", 1000 + random.nextInt(9000));
        StringBuilder part2 = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int asciiValue = random.nextInt(36);
            if (asciiValue < 10) {
                part2.append(asciiValue);
            } else {
                part2.append((char) (asciiValue + 55));
            }
        }
        String part3 = String.format("%03d", 100 + random.nextInt(900));
        return part1 + "-" + part2 + "-" + part3;
    }

    /*private class SimpleTextWatcher implements TextWatcher {
        private final Runnable onChanged;

        public SimpleTextWatcher(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            onChanged.run();
        }
    }**/


    @Override
    protected void onDestroy() {
        if (autoCompleteActe1 != null) {
            autoCompleteActe1.removeTextChangedListener(codeWatcher);
        }
        if (designationActe1 != null) {
            designationActe1.removeTextChangedListener(designationWatcher);
        }
        super.onDestroy();
    }
}


