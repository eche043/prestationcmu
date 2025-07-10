package ci.technchange.prestationscmu.views;

import static androidx.camera.core.CameraXThreads.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.adapter.EtablissementAdapter;
import ci.technchange.prestationscmu.core.DownloadService;
import ci.technchange.prestationscmu.utils.ReferentielService;
import ci.technchange.prestationscmu.utils.RegionCoordUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;
import ci.technchange.prestationscmu.views.MainActivity;

public class ChoixEtablissementActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteEtablissement;
    private AutoCompleteTextView autoCompleteEtablissementCode;
    private Button btnSuivant;
    private String currentSelectedEtablissement;
    private String currentSelectedCode;
    private ReferentielService referentielService;
    private SharedPrefManager sharedPrefManager;
    private RegionCoordUtils regionCoordUtils;
    private List<String> etablissements;
    private List<String> codeEtablissements;
    private Map<String, String> codeToEtablissementMap;
    private Map<String, String> etablissementToCodeMap;
    private String idFamoco;
    private UtilsInfosAppareil utilsInfos;

    // Flag pour éviter les boucles infinies lors de la mise à jour des champs
    private boolean isUpdatingEtablissement = false;
    private boolean isUpdatingCode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choix_etablissement);

        autoCompleteEtablissement = findViewById(R.id.autoCompleteEtablissement);
        autoCompleteEtablissementCode = findViewById(R.id.autoCompleteEtablissementCode);
        btnSuivant = findViewById(R.id.btnSuivantEtablissemnt);

        referentielService = new ReferentielService(this);
        sharedPrefManager = new SharedPrefManager(this);
        regionCoordUtils = new RegionCoordUtils(this);

        utilsInfos = new UtilsInfosAppareil(this);
        idFamoco = utilsInfos.recupererIdAppareil();

        Log.d("ChoixEtablissementActivity", "IDFAMOCO: " + idFamoco);

        // Initialiser les maps pour la correspondance bidirectionnelle
        codeToEtablissementMap = new HashMap<>();
        etablissementToCodeMap = new HashMap<>();

        loadCentreSante();
        setupAutoCompleteListeners();
        setupButtonListener();

        // Afficher la liste au clic sur le champ
        autoCompleteEtablissement.setOnClickListener(v -> {
            autoCompleteEtablissement.showDropDown();
        });

        autoCompleteEtablissementCode.setOnClickListener(v -> {
            autoCompleteEtablissementCode.showDropDown();
        });

        // Désactiver le bouton Suivant par défaut
        btnSuivant.setEnabled(false);
    }

    private void loadCentreSante() {
        etablissements = referentielService.getAllEtablissements();
        codeEtablissements = new ArrayList<>();

        // Construire les maps de correspondance pour les établissements et leurs codes
        for (String etablissement : etablissements) {
            String codeEts = referentielService.getCodeEtsForEtablissement(etablissement);
            if (codeEts != null && !codeEts.isEmpty()) {
                codeEtablissements.add(codeEts);
                codeToEtablissementMap.put(codeEts, etablissement);
                etablissementToCodeMap.put(etablissement, codeEts);
            }
        }

        // Adaptateur pour l'AutoCompleteTextView des établissements
        EtablissementAdapter adapterEtablissement = new EtablissementAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                etablissements);

        // Adaptateur pour l'AutoCompleteTextView des codes
        ArrayAdapter<String> adapterCode = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                codeEtablissements);

        autoCompleteEtablissement.setAdapter(adapterEtablissement);
        autoCompleteEtablissement.setThreshold(1);

        autoCompleteEtablissementCode.setAdapter(adapterCode);
        autoCompleteEtablissementCode.setThreshold(1);
    }

    private void setupAutoCompleteListeners() {
        // Écouter les sélections d'établissements
        autoCompleteEtablissement.setOnItemClickListener((parent, view, position, id) -> {
            if (!isUpdatingEtablissement) {
                isUpdatingCode = true;

                currentSelectedEtablissement = (String) parent.getItemAtPosition(position);
                currentSelectedCode = etablissementToCodeMap.get(currentSelectedEtablissement);

                if (currentSelectedCode != null && !currentSelectedCode.isEmpty()) {
                    autoCompleteEtablissementCode.setText(currentSelectedCode);
                }

                updateButtonState();
                isUpdatingCode = false;
            }
        });

        // Écouter les sélections de codes
        autoCompleteEtablissementCode.setOnItemClickListener((parent, view, position, id) -> {
            if (!isUpdatingCode) {
                isUpdatingEtablissement = true;

                currentSelectedCode = (String) parent.getItemAtPosition(position);
                currentSelectedEtablissement = codeToEtablissementMap.get(currentSelectedCode);

                if (currentSelectedEtablissement != null && !currentSelectedEtablissement.isEmpty()) {
                    autoCompleteEtablissement.setText(currentSelectedEtablissement);
                }

                updateButtonState();
                isUpdatingEtablissement = false;
            }
        });

        // Surveiller les changements de texte pour le champ établissement
        autoCompleteEtablissement.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingEtablissement) {
                    String text = s.toString();
                    boolean isValid = etablissements.contains(text);

                    if (isValid) {
                        currentSelectedEtablissement = text;

                        isUpdatingCode = true;
                        String code = etablissementToCodeMap.get(text);
                        if (code != null) {
                            currentSelectedCode = code;
                            autoCompleteEtablissementCode.setText(code);
                        }
                        isUpdatingCode = false;
                    } else {
                        currentSelectedEtablissement = null;

                        if (!isUpdatingCode && !text.isEmpty()) {
                            isUpdatingCode = true;
                            autoCompleteEtablissementCode.setText("");
                            currentSelectedCode = null;
                            isUpdatingCode = false;
                        }
                    }

                    updateButtonState();
                }
            }
        });

        // Surveiller les changements de texte pour le champ code
        autoCompleteEtablissementCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingCode) {
                    String text = s.toString();
                    boolean isValid = codeEtablissements.contains(text);

                    if (isValid) {
                        currentSelectedCode = text;

                        isUpdatingEtablissement = true;
                        String etablissement = codeToEtablissementMap.get(text);
                        if (etablissement != null) {
                            currentSelectedEtablissement = etablissement;
                            autoCompleteEtablissement.setText(etablissement);
                        }
                        isUpdatingEtablissement = false;
                    } else {
                        currentSelectedCode = null;

                        if (!isUpdatingEtablissement && !text.isEmpty()) {
                            isUpdatingEtablissement = true;
                            autoCompleteEtablissement.setText("");
                            currentSelectedEtablissement = null;
                            isUpdatingEtablissement = false;
                        }
                    }

                    updateButtonState();
                }
            }
        });
    }

    private void updateButtonState() {
        boolean isValidSelection = (currentSelectedEtablissement != null && !currentSelectedEtablissement.isEmpty())
                && (currentSelectedCode != null && !currentSelectedCode.isEmpty());
        btnSuivant.setEnabled(isValidSelection);
    }

    private void downloadInitialDatabase(String idRegion) {
        String fileUrl = "http://57.128.30.4:8089/bdregion.php?latitude=" +
                sharedPrefManager.getLatitude() + "&longitude=" + sharedPrefManager.getLongitude();
        Log.d("Lien ", fileUrl);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Date nowHeure = new Date();

        Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.putExtra("fileUrl", fileUrl);
        serviceIntent.putExtra("id_region", idRegion);
        serviceIntent.putExtra("id_famoco", idFamoco);
        serviceIntent.putExtra("code_ets", sharedPrefManager.getCodeEts());
        serviceIntent.putExtra("code_agac", sharedPrefManager.getCodeAgent());
        serviceIntent.putExtra("date_remontee", dateFormat.format(now));
        serviceIntent.putExtra("heure_debut", timeFormat.format(nowHeure));
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void setupButtonListener() {
        btnSuivant.setOnClickListener(v -> {
            if (currentSelectedEtablissement != null && currentSelectedCode != null) {
                String codeEts = currentSelectedCode;
                String etablissement = currentSelectedEtablissement;

                if (codeEts != null && !codeEts.isEmpty()) {
                    Log.d("ChoixEtablissement", "Code ETS récupéré: " + codeEts + " centre de sante :" + etablissement);
                    sharedPrefManager.saveCodeEts(codeEts);

                    String regionEts = referentielService.getRegionomByEtablissement(etablissement);
                    Log.d("CHOIXETABLISSEMENTACTIVITY", "setupButtonListener: regionEts" + regionEts);

                    if (regionEts != null && !regionEts.isEmpty()) {
                        double[] coordinates = regionCoordUtils.getCoordinatesForRegion(regionEts);

                        if (coordinates != null) {
                            double latitude = coordinates[0];
                            double longitude = coordinates[1];

                            Log.d("ChoixEtablissement", "Coordonnées récupérées pour " + regionEts + ": lat=" +
                                    latitude + ", lng=" + longitude);

                            // Sauvegarder dans les préférences partagées
                            sharedPrefManager.saveLocation(latitude, longitude);

                            // Sauvegarder aussi le nom de la région
                            sharedPrefManager.saveRegionName(regionEts);

                            int id = regionCoordUtils.getIdForRegion(regionEts);
                            String idString = Integer.toString(id);

                            downloadInitialDatabase(idString);

                            Toast.makeText(this, "Région " + regionEts + " sélectionnée", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w("ChoixEtablissement", "Coordonnées non trouvées pour la région: " + regionEts);
                            // Utiliser des coordonnées par défaut ou gérer cette situation
                        }
                    }

                    Intent intent = new Intent(this, InscriptionStepperActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.e("ChoixEtablissement", "Aucun code ETS trouvé pour: " + etablissement);
                    Toast.makeText(this, "Erreur: Aucun code trouvé pour cet établissement", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}