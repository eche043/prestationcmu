package ci.technchange.prestationscmu.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.DownloadService;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;

public class ReminderActivity extends AppCompatActivity {

    private TextView tvMatricule;
    private TextView tvMessage;
    private Button btnContinue;
    private Button btnRegisterNow;
    private ImageView ivRefresh;

    private String matricule;
    private String nom;
    private String prenom;
    private String numero;
    private TextView tvDatabaseInfoConnxeion;
    private SharedPrefManager sharedPrefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        sharedPrefManager = new SharedPrefManager(this);
        tvDatabaseInfoConnxeion = findViewById(R.id.tvDatabaseInfoConnexion2);

        ivRefresh = findViewById(R.id.ivRefresh);

        SharedPrefManager sharedPrefManager = new SharedPrefManager(this);
        String downloadedFileName = sharedPrefManager.getDownloadedFileName();

        // Récupérer le matricule passé en paramètre
        if (getIntent().hasExtra("MATRICULE")) {
            matricule = getIntent().getStringExtra("MATRICULE");
        }

        // Récupérer les informations additionnelles
        if (getIntent().hasExtra("NOM")) {
            nom = getIntent().getStringExtra("NOM");
        }

        if (getIntent().hasExtra("PRENOM")) {
            prenom = getIntent().getStringExtra("PRENOM");
        }

        if (getIntent().hasExtra("NUMERO")) {
            numero = getIntent().getStringExtra("NUMERO");
        }

        // Initialiser les vues
        initViews();

        // Configurer les listeners
        setupListeners();

        // Afficher le matricule
        if (matricule != null && !matricule.isEmpty()) {
            tvMatricule.setText("Matricule : " + matricule);
        }

        File dbFile = new File(getFilesDir(), "encryptedbd/newcnambd1.db");
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
                ivRefresh.setVisibility(View.VISIBLE);
            }
        } else {
            tvDatabaseInfoConnxeion.setText("Aucune base trouvée");
            tvDatabaseInfoConnxeion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            ivRefresh.setVisibility(View.VISIBLE);
        }


    }

    private void initViews() {
        tvMatricule = findViewById(R.id.tvMatricule);
        tvMessage = findViewById(R.id.tvMessage);
        btnContinue = findViewById(R.id.btnContinue);
        btnRegisterNow = findViewById(R.id.btnRegisterNow);

        // Définir le message
        tvMessage.setText("Votre inscription est incomplète. Vous devez vous rendre physiquement au centre de santé pour compléter les informations manquantes (nom du centre, photos, coordonnées GPS).");
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rediriger vers la page principale ou de connexion
                Intent intent = new Intent(ReminderActivity.this, ReminderActivity.class);
                if (nom != null) {
                    intent.putExtra("NOM", nom);
                }

                if (prenom != null) {
                    intent.putExtra("PRENOM", prenom);
                }

                if (numero != null) {
                    intent.putExtra("NUMERO", numero);
                }
                startActivity(intent);
                finish();
            }
        });

        btnRegisterNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rediriger vers la page d'enregistrement du centre
                Intent intent = new Intent(ReminderActivity.this, RegisterCenterActivity.class);
                intent.putExtra("MATRICULE", matricule);
                startActivity(intent);
                finish();
            }
        });

        // Ajouter un listener sur l'icône de rechargement
        ivRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Vous pouvez rediriger vers l'activité de synchronisation ici
                Toast.makeText(ReminderActivity.this, "Lancement de la synchronisation...", Toast.LENGTH_SHORT).show();
                downloadInitialDatabase();
                // Intent syncIntent = new Intent(ReminderActivity.this, SyncActivity.class);
                // startActivity(syncIntent);
            }
        });
    }

    private void downloadInitialDatabase() {
        String fileUrl = "http://57.128.30.4:8089/bdregion.php?latitude=" +
                sharedPrefManager.getLatitude() + "&longitude=" + sharedPrefManager.getLongitude();
        //String fileUrl = "http://57.128.30.4:8089/bdregion.php?latitude=9.410786&longitude=-7.513318";
        //String fileUrl ="http://57.128.30.4:8089/bdall.php";
        Log.d("Lien ", fileUrl);

        // Afficher l'état initial
        tvDatabaseInfoConnxeion.setText("Préparation du téléchargement...");
        Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.putExtra("fileUrl", fileUrl);
        ContextCompat.startForegroundService(this, serviceIntent);
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
                                Toast.makeText(ReminderActivity.this, "Erreur lors du téléchargement", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("DownloadReceiver", "Erreur : result ou originalFileName est null");
                            Toast.makeText(ReminderActivity.this, "Erreur lors du téléchargement : données manquantes", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filters = new IntentFilter(DownloadService.PROGRESS_UPDATE_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadReceiver, filters);
    }
}