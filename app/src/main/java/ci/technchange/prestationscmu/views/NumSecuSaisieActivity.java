package ci.technchange.prestationscmu.views;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

import androidx.core.content.FileProvider;

import com.android.volley.RequestQueue;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.google.android.material.textfield.TextInputEditText;
import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.core.dbHelper;
import ci.technchange.prestationscmu.models.Metrique;
import ci.technchange.prestationscmu.models.Patient;
import ci.technchange.prestationscmu.utils.ActivityTracker;
import ci.technchange.prestationscmu.utils.ExtractData;
import ci.technchange.prestationscmu.utils.MetriqueServiceDb;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONObject;

public class NumSecuSaisieActivity extends AppCompatActivity {

    private TextInputEditText champNumeroSecu;
    private TextInputEditText champNomEnrole;
    private TextInputEditText champMatriculePlanteur;
    private TextView textInfo;
    private dbHelper dbHelper;
    private SQLiteDatabase db;
    private final Handler mainHandler;
    private Patient currentPatient;
    private boolean hasFoundPatient;
    ListView listViewPatients;
    String lastName, numsecu, tableName;
    private String currentSearchMode = "numSecu"; // "numSecu" ou "matPlanteur"

    private List<Patient> patientList;
    private PatientAdapter patientAdapter;
    final int MIN_NAME_LENGTH = 4;
    private boolean isBackPressed = false;
    ImageButton captureButton;
    ImageView imageViewPhoto;
    private Bitmap imageBitmap;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri photoUri;

    private ProgressBar progressBarExternalSearch;
    private SharedPrefManager sharedPrefManager;
    private String dateString;
    private static String date_fin = "";
    private RequestQueue requestQueue;
    private static final String API_URL = "http://51.38.224.233:8080/api/v1/saveFSE";
    private ActivityTracker activityTracker;
    private UtilsInfosAppareil utilsInfos;
    private MetriqueServiceDb metriqueServiceDb;
    private SimpleDateFormat dateFormat;

    public NumSecuSaisieActivity() {
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_num_secu_saisie);

        sharedPrefManager = new SharedPrefManager(this);
        sharedPrefManager = new SharedPrefManager(this);
        metriqueServiceDb = MetriqueServiceDb.getInstance(this);
        activityTracker = new ActivityTracker(this);
        utilsInfos = new UtilsInfosAppareil(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateString = dateFormat.format(new Date());

        String downloadedFileName = sharedPrefManager.getDownloadedFileName();
        File dbFile = new File(getFilesDir(), "encryptedbd/newcnambd1.db");
        if (dbFile.exists() && !"newcnambd1.db".equals(downloadedFileName) && !"".equals(downloadedFileName)) {
            if (GlobalClass.getInstance().cnxDbEnrole == null) {
                GlobalClass.getInstance().initDatabase("enrole");
            }
        }

        db = GlobalClass.getInstance().cnxDbEnrole;
        champNumeroSecu = findViewById(R.id.champNumeroSecu);
        champMatriculePlanteur = findViewById(R.id.champMatriculePlanteur);

        progressBarExternalSearch = findViewById(R.id.progressBarExternalSearch);
        if (progressBarExternalSearch != null) {
            progressBarExternalSearch.setVisibility(View.GONE);
        }

        champNomEnrole = findViewById(R.id.champNomEnrole);
        textInfo = findViewById(R.id.textInfo);
        textInfo.setVisibility(GONE);
        listViewPatients = findViewById(R.id.listRechercheNumsecu);
        lastName = " ";
        captureButton = findViewById(R.id.fabNumSecCapture);
        imageViewPhoto = findViewById(R.id.imageViewPhotosecu);

        captureButton.setOnClickListener(v -> openCamera());

        patientList = new ArrayList<>();
        patientAdapter = new PatientAdapter(this, patientList);
        listViewPatients.setAdapter(patientAdapter);

        setupTextWatchers();
        setupMatriculePlanteurWatcher();

        // Gestion du changement de mode de recherche
        champNumeroSecu.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) currentSearchMode = "numSecu";
        });

        champMatriculePlanteur.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) currentSearchMode = "matPlanteur";
        });
    }

    public void onBackPressed() {
        isBackPressed = true;
        saveMetrique();
        super.onBackPressed();
    }
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "photocard.jpg");
        photoUri = FileProvider.getUriForFile(this, "ci.technchange.prestationscmuym.fileprovider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                imageViewPhoto.setImageBitmap(imageBitmap);
                processImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processImage() {
        if (imageBitmap == null) return;
        InputImage image = InputImage.fromBitmap(imageBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(this::extractData)
                .addOnFailureListener(e -> {
                    Toast.makeText(NumSecuSaisieActivity.this,"Erreur OCR : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void extractData(Text text) {
        String extractedText = text.getText();
        System.out.println("TEXTE ON CARD: "+extractedText);
        champNumeroSecu.setText(ExtractData.extractnumeroSecuNew(extractedText));
        champNomEnrole.setText(ExtractData.extractnomNew(extractedText));
        Toast.makeText(NumSecuSaisieActivity.this,"Fin de lecture de la carte présentée ", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("Range")
    private Patient createPatientFromCursor(Cursor cursor) {
        String[] columnNames = cursor.getColumnNames();
        Log.d("COLUMN_NAMES", "Colonnes disponibles: " + Arrays.toString(columnNames));

        return new Patient(
                cursor.getInt(cursor.getColumnIndex("id")),
                getStringFromCursor(cursor, "nom"),
                getStringFromCursor(cursor, "prenoms"),
                getStringFromCursor(cursor, "date_naissance"),
                getStringFromCursor(cursor, "telephone"),
                getStringFromCursor(cursor, "lieu_naissance"),
                getStringFromCursor(cursor, "sexe"),
                getStringFromCursor(cursor, "csp"),
                getStringFromCursor(cursor, "cmr"),
                getStringFromCursor(cursor, "num_secu"),
                getStringFromCursor(cursor, "guid"),
                getStringFromCursor(cursor, "nomjeunefille")
        );
    }

    private String getStringFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : "";
    }

    private void setupTextWatchers() {
        champNomEnrole.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String inputText = s.toString().trim();
                textInfo.setVisibility(GONE);
                textInfo.setText("");
                if (s.length() >= MIN_NAME_LENGTH) {
                    Log.d("DEBUG", "LASTNAME: " + s.toString());
                    lastName = removeSpecialCharsAndNumbers(s.toString());
                    tableName = "enrole_" + lastName.substring(0, 3);
                    handleNameChange();
                } else {
                    lastName = " ";
                    patientList.clear();
                    patientAdapter.notifyDataSetChanged();
                    listViewPatients.setVisibility(ListView.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        champNumeroSecu.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence seq, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence seq, int i, int i1, int i2) {
                numsecu = seq.toString();
                if (lastName != " " && seq.length()>=13) {
                    handleNameChange();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void setupMatriculePlanteurWatcher() {
        champMatriculePlanteur.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentSearchMode.equals("matPlanteur") && s.length() >= 3) {
                    String matricule = s.toString();
                    String nom = champNomEnrole.getText().toString().trim();

                    if (!nom.isEmpty() && nom.length() >= MIN_NAME_LENGTH) {
                        searchByMatriculeAndName(matricule, nom);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchByMatriculeAndName(String matricule, String nom) {
        new Thread(() -> {
            try {
                String normalizedName = removeSpecialCharsAndNumbers(nom);
                String tableName = "enrole_" + normalizedName.substring(0, 3);

                String query = "SELECT e.* FROM " + tableName + " e " +
                        "JOIN planteurs p ON e.num_secu = p.num_secu " +
                        "WHERE p.mat_planteur = ? AND e.nom LIKE ?";

                Cursor cursor = db.rawQuery(query, new String[]{matricule, nom + "%"});

                mainHandler.post(() -> {
                    try {
                        if (cursor != null && cursor.moveToFirst()) {
                            currentPatient = createPatientFromCursor(cursor);
                            hasFoundPatient = true;

                            // Remplir automatiquement le numéro de sécurité
                            champNumeroSecu.setText(currentPatient.getNumSecu());

                            patientList.clear();
                            patientList.add(currentPatient);
                            patientAdapter.notifyDataSetChanged();
                            listViewPatients.setVisibility(ListView.VISIBLE);
                            textInfo.setVisibility(GONE);
                        } else {
                            textInfo.setVisibility(VISIBLE);
                            textInfo.setText("Aucun enrolé trouvé avec ce matricule et ce nom");
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("MATRICULE_SEARCH", "Erreur recherche matricule: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(NumSecuSaisieActivity.this,
                            "Erreur lors de la recherche",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String removeSpecialCharsAndNumbers(String input) {
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("[^a-zA-Z]");
        return pattern.matcher(normalized).replaceAll("");
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    @SuppressLint("Range")
    private void handleNameChange() {
        if (!currentSearchMode.equals("numSecu")) return;

        currentPatient = null;
        hasFoundPatient = false;

        new Thread(() -> {
            try {
                Cursor cursor1 = null;
                String sql = "SELECT * FROM " + tableName + " WHERE num_secu = '"+numsecu+"' AND nom LIKE '"+lastName+"%'";
                Log.d("SQL_QUERY", "Requête: " + sql + ", NumSecu: " + numsecu + ", Name: " + lastName);
                File dbFile = new File(getFilesDir(), "encryptedbd/newcnambd1.db");
                String downloadedFileName = sharedPrefManager.getDownloadedFileName();
                if (dbFile.exists() && !"newcnambd1.db".equals(downloadedFileName) && !"".equals(downloadedFileName)) {
                    if (GlobalClass.getInstance().cnxDbEnrole == null) {
                        GlobalClass.getInstance().initDatabase("enrole");
                    }
                    db = GlobalClass.getInstance().cnxDbEnrole;

                    cursor1 = db.rawQuery(sql, new String[0]);
                }
                final Cursor cursor = cursor1;
                Log.d("CURSOR_COUNT", "Nombre de lignes: " + (cursor != null ? cursor.getCount() : "null"));

                mainHandler.post(() -> {
                    try {
                        if (cursor != null && cursor.moveToFirst()) {
                            Log.d("Inside_cursor", "bienvenue");
                            currentPatient = createPatientFromCursor(cursor);
                            hasFoundPatient = true;
                            Log.d("CURSOR_DATA", "Patient trouvé: " + currentPatient.getNom()+" csp: "+ currentPatient.getCsp());
                            patientList.clear();
                            patientList.add(currentPatient);
                            patientAdapter.notifyDataSetChanged();
                            activityTracker.enregistrerDateFin();
                            listViewPatients.setVisibility(ListView.VISIBLE);
                            textInfo.setVisibility(GONE);
                        } else {
                            Log.d("CURSOR_EMPTY", "Aucun patient trouvé localement.");

                            if (isInternetAvailable() && numsecu != null && numsecu.length() >= 13) {
                                Log.d("API_CALL", "Recherche externe avec numéro de sécurité: " + numsecu);
                                searchExternalAPIByNumSecu(numsecu);
                            } else {
                                mainHandler.post(() -> {
                                    listViewPatients.setVisibility(ListView.GONE);
                                    textInfo.setVisibility(VISIBLE);
                                    textInfo.setText("Aucun enrolé ne correspond à ces informations !");
                                });
                            }

                        }
                    } catch (Exception e) {
                        Log.e("CURSOR_ERROR", "Erreur lors de la lecture des données: " + e.getMessage());
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("SQL_ERROR", "Erreur SQL: " + e.getMessage());
            }
        }).start();
    }

    // Nouvelle méthode pour rechercher par numéro de sécurité via l'API
    private void searchExternalAPIByNumSecu(String numeroSecu) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // Afficher le spinner de chargement
                runOnUiThread(() -> {
                    if (progressBarExternalSearch != null) {
                        progressBarExternalSearch.setVisibility(VISIBLE);
                    }
                    textInfo.setVisibility(VISIBLE);
                    textInfo.setText("Recherche en cours dans la base externe...");
                });

                // Construire l'URL avec le paramètre num_secu
                StringBuilder urlBuilder = new StringBuilder("http://57.128.30.4:8090/api/v1/recherche");
                urlBuilder.append("?num_secu=").append(URLEncoder.encode(numeroSecu, StandardCharsets.UTF_8.name()));

                String finalUrl = urlBuilder.toString();
                Log.d("API_SEARCH", "URL de recherche: " + finalUrl);

                // Préparer la connexion
                URL url = new URL(finalUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");

                Log.d("API_SEARCH", "Envoi de la requête GET");

                // Obtenir la réponse
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Lire la réponse
                    Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name());
                    String responseBody = scanner.useDelimiter("\\A").next();
                    scanner.close();

                    Log.d("API_SEARCH", "Réponse reçue: " + responseBody);

                    // Traiter la réponse
                    processExternalAPIResponseNumSecu(responseBody);
                } else {
                    // Gérer l'erreur
                    runOnUiThread(() -> {
                        Toast.makeText(NumSecuSaisieActivity.this,
                                "Aucun patient trouvé dans la base externe",
                                Toast.LENGTH_LONG).show();

                        listViewPatients.setVisibility(ListView.GONE);
                        textInfo.setVisibility(VISIBLE);
                        textInfo.setText("Aucun enrolé ne correspond à ces informations !");
                        if (progressBarExternalSearch != null) {
                            progressBarExternalSearch.setVisibility(GONE);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(NumSecuSaisieActivity.this,
                            "Erreur de connexion: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    textInfo.setVisibility(VISIBLE);
                    textInfo.setText("Erreur de connexion à la base externe");
                    if (progressBarExternalSearch != null) {
                        progressBarExternalSearch.setVisibility(GONE);
                    }
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // Nouvelle méthode pour traiter la réponse de l'API externe
    private void processExternalAPIResponseNumSecu(String responseBody) {
        try {
            Log.d("API_RESPONSE", "Traitement de la réponse API");
            JSONObject response = new JSONObject(responseBody);

            // Vérifier si la réponse indique un succès et contient des données
            if (response.has("success") && response.getBoolean("success")
                    && response.has("data") && !response.isNull("data")) {

                JSONArray dataArray = response.getJSONArray("data");
                int arrayLength = dataArray.length();

                if (arrayLength > 0) {
                    runOnUiThread(() -> {
                        try {
                            patientList.clear();

                            // Traiter chaque patient dans le tableau de données
                            for (int i = 0; i < arrayLength; i++) {
                                JSONObject patientData = dataArray.getJSONObject(i);

                                // Récupérer les données de la réponse
                                int externalId = patientData.optInt("id", 0);
                                String guid = patientData.optString("guid", "");
                                String numSecu = patientData.optString("num_secu", "");
                                String nom = patientData.optString("nom", "");
                                String prenoms = patientData.optString("prenoms", "");
                                String dateNaissance = patientData.optString("date_naissance", "");
                                String lieuNaissance = patientData.optString("lieu_naissance", "");
                                String cmr = patientData.optString("cmr", "");
                                String sexe = patientData.optString("sexe", "");
                                String csp = patientData.optString("csp", "");
                                String telephone = patientData.optString("telephone", "");

                                // Créer un nouvel objet Patient avec toutes les données disponibles
                                Patient externalPatient = new Patient(
                                        externalId, // ID externe
                                        nom,
                                        prenoms,
                                        dateNaissance,
                                        telephone,
                                        lieuNaissance,
                                        sexe,
                                        csp,
                                        cmr,
                                        numSecu, // numéro de sécurité sociale
                                        guid, // GUID
                                        "" // nomjeunefille (non disponible dans la réponse)
                                );

                                patientList.add(externalPatient);
                            }

                            // Mettre à jour la référence du patient actuel
                            if (patientList.size() > 0) {
                                currentPatient = patientList.get(0);
                                hasFoundPatient = true;
                            }

                            // Mettre à jour l'interface utilisateur
                            listViewPatients.setVisibility(ListView.VISIBLE);
                            textInfo.setVisibility(GONE);
                            patientAdapter.notifyDataSetChanged();

                            // Message selon le nombre de patients trouvés
                            String message = arrayLength == 1
                                    ? "Patient trouvé dans la base externe"
                                    : arrayLength + " patients trouvés dans la base externe";

                            Toast.makeText(NumSecuSaisieActivity.this,
                                    message,
                                    Toast.LENGTH_SHORT).show();

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(NumSecuSaisieActivity.this,
                                    "Erreur lors du traitement des données: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            textInfo.setVisibility(VISIBLE);
                            textInfo.setText("Erreur lors du traitement des données externes");
                        }
                        if (progressBarExternalSearch != null) {
                            progressBarExternalSearch.setVisibility(GONE);
                        }
                    });
                } else {
                    // Tableau de données vide
                    runOnUiThread(() -> {
                        Toast.makeText(NumSecuSaisieActivity.this,
                                "Aucun patient trouvé dans la base externe",
                                Toast.LENGTH_SHORT).show();
                        textInfo.setVisibility(VISIBLE);
                        textInfo.setText("Aucun enrolé ne correspond à ces informations !");
                        if (progressBarExternalSearch != null) {
                            progressBarExternalSearch.setVisibility(GONE);
                        }
                    });
                }
            } else {
                // Succès = false ou pas de données
                String errorMessage = "Aucun patient trouvé dans la base externe";

                // Si la réponse contient un message d'erreur, l'utiliser
                if (response.has("message")) {
                    errorMessage = response.optString("message", errorMessage);
                }

                final String finalErrorMessage = errorMessage;
                runOnUiThread(() -> {
                    Toast.makeText(NumSecuSaisieActivity.this,
                            finalErrorMessage,
                            Toast.LENGTH_SHORT).show();
                    textInfo.setVisibility(VISIBLE);
                    textInfo.setText("Aucun enrolé ne correspond à ces informations !");
                    if (progressBarExternalSearch != null) {
                        progressBarExternalSearch.setVisibility(GONE);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(NumSecuSaisieActivity.this,
                        "Erreur lors de l'analyse de la réponse: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                textInfo.setVisibility(VISIBLE);
                textInfo.setText("Erreur de communication avec la base externe");
                if (progressBarExternalSearch != null) {
                    progressBarExternalSearch.setVisibility(GONE);
                }
            });
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

    public Patient getCurrentPatient() {
        return currentPatient;
    }

    public boolean hasFoundPatient() {
        return hasFoundPatient;
    }
}