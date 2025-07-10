package ci.technchange.prestationscmu.views;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.models.Affection;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.models.Patient;
import ci.technchange.prestationscmu.printing.generatePdfForFSE;
import ci.technchange.prestationscmu.utils.FseServiceDb;
import ci.technchange.prestationscmu.utils.ReferentielService;

public class FseHospitalisationActivity extends AppCompatActivity {
    private FseServiceDb fseServiceDb;
    private  ReferentielService referentielService;
    private Patient currentPatient;
    private CheckBox checkboxAutre, checkboxCMR, checkboxEloignement, checkboxUrgent, checkboxReference;
    private EditText champAutreLayout;
    private Button enregisterFse, retourHome;
    private EditText champTransaction, champNumSecu, champGuid, champNomComplet,
            champDateNaiss, champNomEtablissement , champNbJour , champMotifFH;
    private Spinner champSexe ;
    private AutoCompleteTextView champCodeAff ;
    private Random random;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fse_hospitalisation);

        fseServiceDb = FseServiceDb.getInstance(this);
        referentielService =  ReferentielService.getInstance(this);
        random = new Random();

        initializeViews();

        currentPatient = (Patient) getIntent().getSerializableExtra("PATIENT");

        if (currentPatient != null) {
            populatePatientData();
        } else {
            Toast.makeText(this, "Aucun patient sélectionné", Toast.LENGTH_SHORT).show();
            finish();
        }
        champTransaction.setText(generateTransactionNumber());
        champTransaction.setEnabled(false);

        setupListeners();

        fetchEtablissementName();
    }

    @SuppressLint("LongLogTag")
    private void fetchEtablissementName() {
        Cursor cursor = null;
        try {
            if (GlobalClass.getInstance().cnxDbReferentiel == null) {
                GlobalClass.getInstance().initDatabase("referentiel");
                db = GlobalClass.getInstance().cnxDbReferentiel;
                if (db == null) {
                    Log.e("fetchEtablissementName DB_ERROR", "La connexion à la base de données (cnxDbReferentiel) est toujours null après l'initialisation.");
                    return;
                } else {
                    Log.d("fetchEtablissementName DB_SUCCESS", "Connexion à la base de données (cnxDbReferentiel) initialisée avec succès.");
                }
            } else {
                db = GlobalClass.getInstance().cnxDbReferentiel;
                Log.d("fetchEtablissementName DB_SUCCESS", "Connexion à la base de données (cnxDbReferentiel) déjà initialisée.");
            }

            String query = "SELECT etablissement FROM etablissements WHERE code_ets = '000119061'";
            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") String etablissement = cursor.getString(cursor.getColumnIndex("etablissement"));
                champNomEtablissement.setText(etablissement);
            } else {
                Toast.makeText(this, "Aucun établissement trouvé pour ce code", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("fetchEtablissementName DB_ERROR", "Erreur lors de l'initialisation de la base de données (cnxDbReferentiel): " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors de la récupération du nom de l'établissement", Toast.LENGTH_SHORT).show();
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


    private void showdModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Fiche éditée avec succès")
                .setMessage("Voulez-vous imprimer la feuille de soins ?")
                .setNegativeButton("Fermer", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Intent intent = new Intent(FseHospitalisationActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setPositiveButton("Imprimer", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        impression();

                        Intent intent = new Intent(FseHospitalisationActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }

    private void initializeViews() {
        retourHome = findViewById(R.id.retourHomeFH);
        enregisterFse = findViewById(R.id.enregistrerFicheFH);

        champTransaction = findViewById(R.id.champTransactionFH);
        champNumSecu = findViewById(R.id.champNumSecuFH);
        champGuid = findViewById(R.id.champGuidFH);
        champNomComplet = findViewById(R.id.champNomCompletFH);
        champDateNaiss = findViewById(R.id.champDateNaissFH);
        champNomEtablissement = findViewById(R.id.champNomEtablissementFH);
        champAutreLayout = findViewById(R.id.champAutreFH);

        checkboxCMR = findViewById(R.id.checkboxCMRFH);
        checkboxEloignement = findViewById(R.id.checkboxEloignementFH);
        checkboxUrgent = findViewById(R.id.checkboxUrgentFH);
        checkboxAutre = findViewById(R.id.checkboxAutreFH);
        checkboxReference = findViewById(R.id.checkboxReferenceFH);


        champSexe = findViewById(R.id.champSexeFH);
        String[] sexes = new String[]{"M", "F"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sexes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        champSexe.setAdapter(adapter);

        champCodeAff = findViewById(R.id.champCodeAff);
        List<Affection> affectionList = referentielService.getAllAffectations();
        List<String>  affectionLabels = new ArrayList<>();
        Log.d("la taille de ",affectionList.get(0).getLibelle());

        // Extraire uniquement les noms (libelle) pour l'affichage dans le Spinner
        for (Affection a : affectionList) {
            affectionLabels.add(a.getCodeAffection());
        }
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                affectionLabels
        );
     //   adapter2.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        champCodeAff.setAdapter(adapter2);
        champCodeAff.setThreshold(1);
        champCodeAff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                champCodeAff.showDropDown();
            }

        });
        //champNbJour
        champNbJour = findViewById(R.id.champNbJour);
        champMotifFH = findViewById(R.id.champMotifFH);




    }

    private void impression() {
        try {

            String numTrans = champTransaction.getText().toString();
            FseAmbulatoire fse = fseServiceDb.getFseAmbulatoireByNumTrans(numTrans);
            Map<String, Object> data = new HashMap<>();
            data.put("numTransaction", champTransaction.getText().toString());
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

    private void setupListeners() {
        retourHome.setOnClickListener(v -> finish());

        checkboxAutre.setOnCheckedChangeListener((buttonView, isChecked) -> {
            champAutreLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        enregisterFse.setOnClickListener(v -> {
            if (validateForm()) {
                if (saveFse()) {
                    showdModal();
                }
            }
        });
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
            checkboxCMR.setChecked(currentPatient.getCmr().equals("1") ||
                    currentPatient.getCmr().equalsIgnoreCase("true"));
        } else {
            checkboxCMR.setChecked(false);
            checkboxCMR.setEnabled(false);
            checkboxCMR.setAlpha(0.5f);
        }

        champNumSecu.setEnabled(false);
        champGuid.setEnabled(false);
        champNomComplet.setEnabled(false);
        champDateNaiss.setEnabled(false);
        champSexe.setEnabled(false);
    }

    private boolean validateForm() {
        if (champNomEtablissement.getText().toString().trim().isEmpty()) {
            champNomEtablissement.setError("Le nom de l'établissement est requis");
            return false;
        }
        return true;
    }

    private boolean saveFse() {
        try {
            FseAmbulatoire fseAmbulatoire = new FseAmbulatoire();

            fseAmbulatoire.setNumTrans(champTransaction.getText().toString());
            fseAmbulatoire.setNumSecu(champNumSecu.getText().toString());
            fseAmbulatoire.setNumGuid(champGuid.getText().toString());
            fseAmbulatoire.setNomComplet(champNomComplet.getText().toString());
            fseAmbulatoire.setSexe(champSexe.getSelectedItem().toString());
            fseAmbulatoire.setDateNaissance(champDateNaiss.getText().toString());
            fseAmbulatoire.setNomEtablissement(champNomEtablissement.getText().toString());
            fseAmbulatoire.setCodeEts("000119061");
            fseAmbulatoire.setEtablissementCmr(checkboxCMR.isChecked());
            fseAmbulatoire.setEtablissementUrgent(checkboxUrgent.isChecked());
            fseAmbulatoire.setEtablissementRef(checkboxReference.isChecked());
            fseAmbulatoire.setEtablissementEloignement(checkboxEloignement.isChecked());

            if (checkboxAutre.isChecked()) {
                fseAmbulatoire.setInfoAutre(true);
                fseAmbulatoire.setEtablissementPrecision(champAutreLayout.getText().toString());
            }

            fseAmbulatoire.setStatusProgres(2);
            fseAmbulatoire.setPreInscription(false);
            fseAmbulatoire.setMotif(champMotifFH.getText().toString());
            fseAmbulatoire.setNombre_jour(Integer.parseInt(champNbJour.getText().toString()));
            fseAmbulatoire.setCodeAffection( champCodeAff.getText().toString());
            fseAmbulatoire.setType_fse("Hospitalisation et Soins");
            Log.d("FseHospi", fseAmbulatoire.toString());

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
}
