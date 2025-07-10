package ci.technchange.prestationscmu.views;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.adapter.CustomSpinnerAdapter;
import ci.technchange.prestationscmu.core.DownloadService;
import ci.technchange.prestationscmu.core.dbHelper;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;

public class ConnexionActivity extends AppCompatActivity {
    private SharedPrefManager sharedPrefManager;
    private TextView tvDatabaseInfoConnxeion, inscritpion;
    private Button btnConnexionEmpreinte;
    private Button btnConnexionAgent;

    private Spinner spinnerAgents;
    private ProgressDialog loadingDialog;
    private String selectedMatricule = ""; // Matricule sélectionné
    private List<String> agentsList = new ArrayList<>(); // Liste des agents
    private CustomSpinnerAdapter customAdapter; // Adaptateur personnalisé

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connexion);
        Button btnConnexionEmpreinte = findViewById(R.id.btnConnexionEmpreinte);
        Button btnConnexionAgent = findViewById(R.id.btnConnexionAgent);
        Button btnCreerCompte = findViewById(R.id.btnCreerCompte);
        spinnerAgents = findViewById(R.id.spinnerAgents);

        sharedPrefManager = new SharedPrefManager(this);
        tvDatabaseInfoConnxeion = findViewById(R.id.tvDatabaseInfoConnexion);

        SharedPrefManager sharedPrefManager = new SharedPrefManager(this);
        String downloadedFileName = sharedPrefManager.getDownloadedFileName();

        // Charger les matricules des agents
        loadAgentMatricules();

        spinnerAgents.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMatricule = (String) parent.getItemAtPosition(position);
                sharedPrefManager.setagentCodeAndName(selectedMatricule);
                Toast.makeText(ConnexionActivity.this, "Agent sélectionné: " + selectedMatricule, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMatricule = "";
            }
        });

        btnCreerCompte.setOnClickListener(v -> {
            //Intent intent = new Intent(ConnexionActivity.this, InscriptionActivity.class);
            Intent intent = new Intent(ConnexionActivity.this, InscriptionStepperActivity.class);

            startActivity(intent);
        });
        btnConnexionEmpreinte.setOnClickListener(v -> {
            // Afficher un spinner de chargement
            loadingDialog = new ProgressDialog(ConnexionActivity.this);
            loadingDialog.setMessage("Chargement en cours...");
            loadingDialog.setCancelable(false);
            loadingDialog.show();
            //Intent intent = new Intent(ConnexionActivity.this, LoginFingerprintActivity.class);
            Intent intent = new Intent(ConnexionActivity.this, EnrollmentVerificationFingerActivity.class);
            Log.d("SELECTED-MATRICULE", selectedMatricule);
            intent.putExtra("MATRICULE", extractMatricule(selectedMatricule));
            startActivity(intent);
        });

        btnConnexionAgent.setOnClickListener(v -> {
            Intent intent = new Intent(ConnexionActivity.this, MainActivity.class);
            startActivity(intent);
        });

        File dbFile = new File(getFilesDir(), "encryptedbd/newcnambd1.db");
        Log.d("TAGGGGGG", "onCreate: "+downloadedFileName);
        if (dbFile.exists()) {
            if (downloadedFileName != null && !downloadedFileName.isEmpty() && !"newcnambd1.db".equals(downloadedFileName)) {
                // Convertir le nom de fichier en nom de région
                String regionName = RegionUtils.getRegionNameFromFileName(downloadedFileName);
                //sharedPrefManager.saveRegionName(regionName);

                //Toast.makeText(this, "name2:"+regionName, Toast.LENGTH_LONG).show();

                tvDatabaseInfoConnxeion.setText("Région: " + regionName);
                tvDatabaseInfoConnxeion.setTextColor(getResources().getColor(android.R.color.black));
            } else {

                tvDatabaseInfoConnxeion.setText("Refaire la synchronisation");
                tvDatabaseInfoConnxeion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        } else {
            tvDatabaseInfoConnxeion.setText("Aucune base trouvée");
            tvDatabaseInfoConnxeion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Fermer le dialog de chargement s'il est visible
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
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

    // Méthode pour charger les matricules des agents
    private void loadAgentMatricules() {
        agentsList.clear(); // Vider la liste avant de la recharger

        try {
            dbHelper helper = new dbHelper(this);
            SQLiteDatabase db = helper.getReadableDatabase();

            // Requête pour récupérer les matricules avec l'ID
            Cursor cursor = db.query(
                    "agents_inscription",
                    new String[]{"id", "matricule", "nom", "prenom"},
                    null, null, null, null, "id DESC"  // Trier par ID décroissant pour avoir les plus récents d'abord
            );

            // Parcourir les résultats
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                    String matricule = cursor.getString(cursor.getColumnIndexOrThrow("matricule"));
                    String nom = cursor.getString(cursor.getColumnIndexOrThrow("nom"));
                    String prenom = cursor.getString(cursor.getColumnIndexOrThrow("prenom"));

                    // Ajouter au format "ID|MATRICULE - NOM Prénom" (l'ID est séparé par |)
                    agentsList.add(id + "|" + matricule + " - " + nom + " " + prenom);
                } while (cursor.moveToNext());

                cursor.close();
            }

            db.close();

            // Si aucun agent n'est trouvé
            if (agentsList.isEmpty()) {
                agentsList.add("Aucun agent inscrit");
            }

        } catch (Exception e) {
            Log.e("ConnexionActivity", "Erreur lors du chargement des matricules: " + e.getMessage());
            agentsList.add("Erreur de chargement");
        }

        // Créer l'adaptateur personnalisé et l'attacher au Spinner
        customAdapter = new CustomSpinnerAdapter(this, agentsList);
        spinnerAgents.setAdapter(customAdapter);
    }

    // Méthode publique pour recharger la liste (appelée après suppression)
    public void refreshAgentsList() {
        loadAgentMatricules();
    }

    // Extraire le matricule de la chaîne formatée
    private String extractMatricule(String formattedString) {
        // Le format est maintenant "ID|MATRICULE - NOM Prénom"
        if (formattedString != null && formattedString.contains("|")) {
            String withoutId = formattedString.split("\\|")[1]; // Enlever l'ID
            if (withoutId.contains(" - ")) {
                return withoutId.split(" - ")[0].trim();
            }
            return withoutId;
        } else if (formattedString != null && formattedString.contains(" - ")) {
            // Format ancien pour la compatibilité
            return formattedString.split(" - ")[0].trim();
        }
        return formattedString; // Retourne la chaîne d'origine si le format n'est pas reconnu
    }

    // Nouvelle méthode pour extraire l'ID
    private String extractId(String formattedString) {
        // Le format est "ID|MATRICULE - NOM Prénom"
        if (formattedString != null && formattedString.contains("|")) {
            return formattedString.split("\\|")[0].trim();
        }
        return null; // Retourne null si pas d'ID trouvé
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadService.PROGRESS_UPDATE_ACTION.equals(intent.getAction())) {
                // Récupérer la progression et l'afficher
                if (intent.hasExtra(DownloadService.PROGRESS_EXTRA)) {
                    int progress = intent.getIntExtra(DownloadService.PROGRESS_EXTRA, 0);
                    runOnUiThread(() -> {
                        tvDatabaseInfoConnxeion.setText("Téléchargement en cours : " + progress + "%");
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
                                        tvDatabaseInfoConnxeion.setText("Refaire la synchronisation");
                                    } else {
                                        String regionName = RegionUtils.getRegionNameFromFileName(originalFileName);
                                        tvDatabaseInfoConnxeion.setText("Base de données: " + regionName);
                                        tvDatabaseInfoConnxeion.setTextColor(getResources().getColor(android.R.color.black));
                                    }
                                });

                            } else {
                                runOnUiThread(() -> {
                                    tvDatabaseInfoConnxeion.setText("Erreur lors du téléchargement");
                                    tvDatabaseInfoConnxeion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                });
                                Toast.makeText(ConnexionActivity.this, "Erreur lors du téléchargement", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("DownloadReceiver", "Erreur : result ou originalFileName est null");
                            Toast.makeText(ConnexionActivity.this, "Erreur lors du téléchargement : données manquantes", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    };

    protected void onResume() {
        super.onResume();
        // Recharger les agents à chaque reprise de l'activité
        loadAgentMatricules();
        IntentFilter filters = new IntentFilter(DownloadService.PROGRESS_UPDATE_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadReceiver, filters);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadReceiver);
    }
}