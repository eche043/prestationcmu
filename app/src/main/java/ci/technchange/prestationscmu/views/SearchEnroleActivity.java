package ci.technchange.prestationscmu.views;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
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


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SearchEnroleActivity extends AppCompatActivity {
    final int MIN_NAME_LENGTH = 3;

    EditText firstNameField, lastNameField, birthdayField, phoneField, birthPlaceField;
    String lastName=" ";
    String firstName, birthday, phone, birthPlace, tableName;
    ImageView loadingSearchEnrole,imageViewPhoto;
    ListView resultList;
    private boolean isEditing = false; // Flag pour éviter la boucle infinie
    RelativeLayout noResultSection, beforeActionSection;
    ImageButton captureButton;
    private Bitmap imageBitmap;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri photoUri;
    private String dateString;
    private static String date_fin = "";
    private RequestQueue requestQueue;
    private static final String API_URL = "http://51.38.224.233:8080/api/v1/saveFSE";
    private ActivityTracker activityTracker;
    private UtilsInfosAppareil utilsInfos;
    private MetriqueServiceDb metriqueServiceDb;
    private SimpleDateFormat dateFormat;
    private boolean isBackPressed = false;

    LinearLayout filterBox;

    Button newEnrolementBtn;

    PatientAdapter adapter;

    SQLiteDatabase db;

    ArrayList<Patient> patients = new ArrayList<>();

    final int LIMIT = 20;

    int offset = 0;

    boolean hasMoreData = true;

    private SharedPrefManager sharedPrefManager;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_enrole);

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
        loadingSearchEnrole = findViewById(R.id.loadingSearchEnrole);
        firstNameField = findViewById(R.id.first_name_field);
        lastNameField = findViewById(R.id.last_name_field);
        birthdayField = findViewById(R.id.birthday_field);
        phoneField = findViewById(R.id.phone_field);
        ProgressBar externalSearchSpinner = findViewById(R.id.externalSearchSpinner);
        birthPlaceField = findViewById(R.id.birth_place_field);
        resultList = findViewById(R.id.result_list);
        noResultSection = findViewById(R.id.no_patient_found);
        beforeActionSection = findViewById(R.id.before_action_section);
        filterBox = findViewById(R.id.filter_box);
        newEnrolementBtn = findViewById(R.id.new_enrolement_btn);

        imageViewPhoto = findViewById(R.id.imageViewPhoto);
        captureButton = findViewById(R.id.fabCapture);
        captureButton.setOnClickListener(v -> openCamera());

        adapter = new PatientAdapter(this, patients);
        resultList.setAdapter(adapter);

        lastNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence seq, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence seq, int i, int i1, int i2) {
                patients.clear();
                hasMoreData = true;
                offset = 0;

                if (seq.length() >= MIN_NAME_LENGTH) {
                    Log.d("DEBUG", "LASTNAME: " + seq.toString());
                    lastName = removeSpecialCharsAndNumbers(seq.toString());
                    String tableNameBase = lastName.replace("'", "");
                    //tableName = "enrole_" + tableNameBase.substring(0, 3);
                    //tableName = "enrole_" + tableNameBase.substring(0, Math.min(3, tableNameBase.length()));
                    tableName = "enrole_" + lastName.replace("'", "").substring(0, Math.min(3, lastName.replace("'", "").length()));
                    handleSearch();
                } else {
                    lastName = " ";
                    tableName = null;
                    patients.clear();
                    resultList.setVisibility(GONE);
                    noResultSection.setVisibility(VISIBLE);
                    // Toast.makeText(SearchEnroleActivity.this, "Saisir au minimum 4 charctères", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        firstNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence seq, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence seq, int i, int i1, int i2) {
                firstName = seq.toString();
                if (lastName != " " && seq.length()>=3) {
                    offset = 0;
                    //patients.clear();
                    hasMoreData = true;
                    handleSearch();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
//noResultSection
        birthdayField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence seq, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence seq, int i, int i1, int i2) {
                String inputDate = seq.toString();
                birthday=null;
                /*
                // Empêcher la boucle infinie
                if (isEditing) return;
                isEditing = true; // Indiquer que l'on est en train de modifier le texte

                 //CODE DE LA FIONCTION formatDateInput pas encore parfait
                if (inputDate.length() <= 10) {
                    String formattedText = formatDateInput(inputDate, false);
                    // Appliquez la modification à l'EditText
                    birthdayField.setText(formattedText);
                    birthdayField.setSelection(formattedText.length());  // Garder le curseur à la fin
                }
                isEditing = false; // Réinitialiser le flag
                 */
                if (isValidDate(inputDate) && seq.length()==10) {
                    // La date est valide et inférieure à la date actuelle
                    birthdayField.setBackgroundColor(Color.WHITE); // Fond blanc
                    birthday = inputDate;
                    if (lastName != " ") {
                        offset = 0;
                        //patients.clear();
                        hasMoreData = true;
                        handleSearch();
                    }
                } else {
                    if(seq.length()==0){
                        birthdayField.setBackgroundColor(Color.WHITE); // Fond blanc
                    }else {
                        // La date est invalide ou trop récente
                        birthdayField.setBackgroundColor(Color.RED); // Fond rouge
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        // Ajout d'un KeyListener pour capturer les touches physiques (notamment backspace)
        /*
        // CODE DE LA FONCTION formatDateInput pas en core parfait
        birthdayField.setOnKeyListener((view, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                // Lorsque la touche de suppression est appuyée
                String currentText = birthdayField.getText().toString();
                String updatedText = formatDateInput(currentText, true);  // Passer "true" pour l'effacement
                birthdayField.setText(updatedText);
                birthdayField.setSelection(updatedText.length()); // Garder le curseur à la fin
                return true; // Retourner true pour indiquer que l'événement est consommé
            }
            return false; // Retourner false si ce n'est pas la touche effacer
        });*/

        phoneField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence seq, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence seq, int i, int i1, int i2) {
                phone = seq.toString();
                if (lastName != " " && seq.length()>=3) {
                    offset = 0;
                    patients.clear();
                    hasMoreData = true;
                    handleSearch();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        birthPlaceField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence seq, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence seq, int i, int i1, int i2) {
                birthPlace = seq.toString();
                offset = 0;
                patients.clear();
                hasMoreData = true;
                handleSearch();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        resultList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount >= totalItemCount && totalItemCount != 0) {
                    paginate();
                }
            }
        });

        newEnrolementBtn.setOnClickListener(v -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("sparta.realm.apps.idcapture.cnamv2_demo");
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                // L'application n'est pas installée, gérez l'erreur ici
                Toast.makeText(this, "Application d'enrôlement non-installée", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onBackPressed() {
        isBackPressed = true;
        saveMetrique();
        super.onBackPressed();
    }
    private void openCamera() {
        /*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }*/
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
            /*Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageViewPhoto.setImageBitmap(imageBitmap);
            processImage();*/
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
                    Toast.makeText(SearchEnroleActivity.this,"Erreur OCR : " + e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
                });
    }

    private void extractData(Text text) {
        String extractedText = text.getText();
        System.out.println("TEXTE ON CARD: "+extractedText);
        firstNameField.setText(ExtractData.extractPrenoms2(extractedText));
        lastNameField.setText(ExtractData.extractNom2(extractedText));
        birthdayField.setText(ExtractData.extractBirthDate2(extractedText));
        Toast.makeText(SearchEnroleActivity.this,"Fin de lecture de la carte présentée ", Toast.LENGTH_SHORT);
    }


    private String formatDateInput(String input, boolean isBackspace) {
        // Enlever tous les caractères non numériques
        Log.i("Formatted 1: ",input);
        String inputinit=input;
        input = input.replaceAll("[^\\d]", "");
        Log.i("Formatted 2: ",input);
        // Si le texte est vide, retourner un texte vide
        if (input.isEmpty()) {
            return "";
        }
        // Si la touche effacer a été pressée, nous devons ajuster l'input avant de formater
        if (isBackspace && input.length() > 0) {
            input = input.substring(0, input.length() - 1); // Supprimer le dernier caractère
            Log.i("Formatted 3: ",input);
        }
        Log.i("Formatted 4: ", String.valueOf(input.length()));
        // Appliquer le format "JJ/MM/AAAA"
        StringBuilder formattedDate = new StringBuilder(input);
        // Ajouter le séparateur '/' après 2 caractères pour le jour
        if (formattedDate.length() == 2) {
            formattedDate.insert(2, "/");
        }
        // Ajouter le séparateur '/' après 5 caractères pour le mois
        if (formattedDate.length() == 5) {
            formattedDate.insert(5, "/");
        }
        // Retourner le texte formaté
        return formattedDate.toString();
    }

    private boolean isValidDate(String inputDate) {
        // Format de la date attendu
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false); // Désactive la tolérance pour accepter des dates incorrectes
        try {
            // Essaie de parser la date saisie
            Date parsedDate = sdf.parse(inputDate);
            // Vérification si la date est avant la date actuelle
            Date currentDate = new Date();
            return parsedDate != null && parsedDate.before(currentDate);
        } catch (ParseException e) {
            // Si le format de la date est incorrect
            return false;
        }
    }

    @SuppressLint("Range")
    Patient createPatientFromCursor(Cursor cursor) {
        return new Patient(
                cursor.getInt(cursor.getColumnIndex("id")),
                _getStringFromCursor(cursor, "nom"),
                _getStringFromCursor(cursor, "prenoms"),
                _getStringFromCursor(cursor, "date_naissance"),
                _getStringFromCursor(cursor, "telephone"),
                _getStringFromCursor(cursor, "lieu_naissance"),
                _getStringFromCursor(cursor, "sexe"),
                _getStringFromCursor(cursor, "csp"),
                _getStringFromCursor(cursor, "cmr"),
                _getStringFromCursor(cursor, "num_secu"),
                _getStringFromCursor(cursor, "guid"),
                _getStringFromCursor(cursor, "nomjeunefille")
        );
    }

    private String _getStringFromCursor(Cursor cursor, String colName) {
        int colIdx = cursor.getColumnIndex(colName);
        return colIdx != -1 ? cursor.getString(colIdx) : "";
    }

    private String removeSpecialCharsAndNumbers(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("[^a-zA-Z]");
        return pattern.matcher(normalized).replaceAll("");
    }

    public static String convertDateFormat(String inputDate) {
        if(inputDate!="") {
            // Définir le format d'entrée : jj/mm/aaaa
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
            // Définir le format de sortie : aaaa-mm-jj
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                // Parse la date d'entrée en un objet Date
                Date date = inputFormat.parse(inputDate);
                // Convertir la date en format de sortie
                return outputFormat.format(date);
            } catch (ParseException e) {
                // Si la date n'est pas valide, afficher une erreur
                e.printStackTrace();
                return null;
            }
        }else{
            return null;
        }
    }

    @SuppressLint("Range")
    private void handleSearch() {
        String conditions = " ";
        Cursor cursor = null;

        if (lastName != " ") {
            loadingSearchEnrole.setVisibility(VISIBLE);
            Log.i("HANDLESEARCH: ", "affichage du loadinfsearch");
            String newName = lastName.replace("'","''");
            conditions += "nom LIKE '" + newName + "%'";

            if (firstName != null) {
                String newFirstName = firstName.replace(" ","%");
                newFirstName = newFirstName.replace("'","''");
                conditions += " AND prenoms LIKE '%" + newFirstName + "%'";
            }
            if (birthday != null) {
                conditions += " AND date_naissance LIKE '%" + birthday + "%'";
            }
            if (phone != null) {
                conditions += " AND telephone LIKE '%" + phone + "%'";
            }
            if (birthPlace != null) {
                String newBirthPlace = birthPlace.replace("'", "''");
                newBirthPlace = newBirthPlace.replace(" ", "%").replace("-", "%");
                conditions += " AND REPLACE(REPLACE(lieu_naissance, '-', ' '), ' ', '%') LIKE '%" + newBirthPlace + "%'";
            }

            String sql = "SELECT * FROM " + tableName + " WHERE " + conditions + " LIMIT " + LIMIT + " OFFSET " + offset;
            Log.d("Requette ", sql);

            String downloadedFileName = sharedPrefManager.getDownloadedFileName();
            File dbFile = new File(getFilesDir(), "encryptedbd/newcnambd1.db");
            if (dbFile.exists() && !"newcnambd1.db".equals(downloadedFileName) && !"".equals(downloadedFileName)) {
                if (GlobalClass.getInstance().cnxDbEnrole == null) {
                    GlobalClass.getInstance().initDatabase("enrole");
                }
                db = GlobalClass.getInstance().cnxDbEnrole;
                cursor = db.rawQuery(sql, new String[0]);
            }

            Log.i("HANDLESEARCH: ", "SQL: " + sql);
            if (cursor != null) {
                Log.i("HANDLESEARCH: ", "NBRE trouvé: " + cursor.getCount());
                patients.clear();
                int resultCount = 0;
                while (cursor.moveToNext()) {
                    patients.add(createPatientFromCursor(cursor));
                    resultCount++;
                }
                hasMoreData = resultCount < LIMIT ? false : true;
            }
            beforeActionSection.setVisibility(GONE);
            Log.i("HANDLESEARCH: ", "CACHE : beforeActionSection");
            if (!patients.isEmpty()) {
                Log.i("HANDLESEARCH: ", "AFFICHAGE DE resultList");

                resultList.setVisibility(VISIBLE);
                noResultSection.setVisibility(GONE);
                filterBox.setVisibility(VISIBLE);

                date_fin = activityTracker.enregistrerDateFin();

            } else {
                Log.i("HANDLESEARCH: ", "CACHER resultList et Afficher NoResultSection");


                if (offset == 0) {
                    if (isInternetAvailable()) {

                        searchExternalAPI();
                    } else {
                        loadingSearchEnrole.setVisibility(GONE);
                        resultList.setVisibility(GONE);
                        noResultSection.setVisibility(VISIBLE);
                    }
                } else {

                    resultList.setVisibility(GONE);
                    noResultSection.setVisibility(VISIBLE);
                    loadingSearchEnrole.setVisibility(GONE);
                }
            }

            adapter.notifyDataSetChanged();
        } else {
            loadingSearchEnrole.setVisibility(GONE);
            return;
        }
    }

    /*@SuppressLint("Range")
    private void handleSearch() {
        String conditions = " ";
        Cursor cursor = null;

        if (lastName != " ") {
            loadingSearchEnrole.setVisibility(VISIBLE);
            Log.i("HANDLESEARCH: ", "affichage du loadinfsearch");
            String newName = lastName.replace("'","''");
            conditions += "nom LIKE '" + newName + "%'";

            if (firstName != null) {
                String newFirstName = firstName.replace(" ","%");
                newFirstName = newFirstName.replace("'","''");
                conditions += " AND prenoms LIKE '%" + newFirstName + "%'";
            }
            if (birthday != null) {
                //conditions += " AND date_naissance LIKE '%" + convertDateFormat(birthday) + "%'";
                conditions += " AND date_naissance LIKE '%" + birthday + "%'";
            }
            if (phone != null) {
                conditions += " AND telephone LIKE '%" + phone + "%'";
            }
            /*if (birthPlace != null) {
                String newBirthPlace = birthPlace.replace("'","''");
                newBirthPlace = newBirthPlace.replace(" ","%");
                conditions += " AND lieu_naissance LIKE '%" + newBirthPlace + "%'";
            }**/
            /*if (birthPlace != null) {
                String newBirthPlace = birthPlace.replace("'", "''");
                newBirthPlace = newBirthPlace.replace(" ", "%").replace("-", "%");
                conditions += " AND REPLACE(REPLACE(lieu_naissance, '-', ' '), ' ', '%') LIKE '%" + newBirthPlace + "%'";
            }

            String sql = "SELECT * FROM " + tableName + " WHERE " + conditions + " LIMIT " + LIMIT + " OFFSET " + offset;
            Log.d("Requette ", sql);

            String downloadedFileName = sharedPrefManager.getDownloadedFileName();
            // Vérification immédiate de l'existence de la base de données
            File dbFile = new File(getFilesDir(), "encryptedbd/newcnambd1.db");
            if (dbFile.exists() && !"newcnambd1.db".equals(downloadedFileName) && !"".equals(downloadedFileName)) {
                if (GlobalClass.getInstance().cnxDbEnrole == null) {
                    GlobalClass.getInstance().initDatabase("enrole");
                }
                db = GlobalClass.getInstance().cnxDbEnrole;
                cursor = db.rawQuery(sql, new String[0]);
            }


            Log.i("HANDLESEARCH: ", "SQL: " + sql);
            if (cursor != null) {
                Log.i("HANDLESEARCH: ", "NBRE trouvé: " + cursor.getCount());
                patients.clear();
                int resultCount = 0;
                while (cursor.moveToNext()) {
                    patients.add(createPatientFromCursor(cursor));
                    resultCount++;
                }
                hasMoreData = resultCount < LIMIT ? false : true;
            }
            beforeActionSection.setVisibility(GONE);
            Log.i("HANDLESEARCH: ", "CACHE : beforeActionSection");
            if (!patients.isEmpty()) {
                Log.i("HANDLESEARCH: ", "AFFICHAGE DE resultList");
                //searchExternalAPI();

                resultList.setVisibility(VISIBLE);
                date_fin = activityTracker.enregistrerDateFin();
                saveMetrique();
                noResultSection.setVisibility(GONE);
                filterBox.setVisibility(VISIBLE);
            } else {
                Log.i("HANDLESEARCH: ", "CACHER resultList et Afficher NoResultSection");

                // Vérifiez si c'est la première recherche (offset = 0)
                if (offset == 0) {


                    if (isInternetAvailable()) {
                        // Si aucun résultat local n'est trouvé et que la connexion Internet est disponible,
                        // lancez la recherche externe
                        searchExternalAPI();
                    } else {
                        loadingSearchEnrole.setVisibility(GONE);
                        resultList.setVisibility(GONE);
                        noResultSection.setVisibility(VISIBLE);
                    }
                } else {
                    // Sinon, c'est juste qu'il n'y a plus de résultats pour la pagination
                    resultList.setVisibility(GONE);
                    noResultSection.setVisibility(VISIBLE);
                    loadingSearchEnrole.setVisibility(GONE);
                }
            }

            adapter.notifyDataSetChanged();
        } else {
            loadingSearchEnrole.setVisibility(GONE);
            return;
        }
    }**/


    public void paginate() {
        if (hasMoreData) {
            offset += LIMIT;
            handleSearch();
        }
    }


    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

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
    // Ajoutez cette méthode dans votre classe SearchEnroleActivity
    private void searchExternalAPI() {

        // Vérifiez si vous avez au moins le nom ou prénom saisi
        if (lastName == null || lastName.trim().equals(" ") || birthday == null || birthday.length() < 10 || firstName == null || firstName.trim().isEmpty()) {
            Toast.makeText(this, "Veuillez saisir la date de naissance", Toast.LENGTH_SHORT).show();

            return;
        }
        ProgressBar externalSearchSpinner = findViewById(R.id.externalSearchSpinner);

        // Affichage FORCÉ des loaders sur le thread UI
        runOnUiThread(() -> {
            Log.d("Loaders", "Avant: loadingSearchEnrole visibility = " + loadingSearchEnrole.getVisibility());
            Log.d("Loaders", "Avant: externalSearchSpinner visibility = " + externalSearchSpinner.getVisibility());

            loadingSearchEnrole.setVisibility(View.VISIBLE);
            externalSearchSpinner.setVisibility(View.VISIBLE);

            // Démarrer l'animation si nécessaire
            Drawable drawable = loadingSearchEnrole.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                animationDrawable.start();
            }

            // Forcer la mise à jour
            loadingSearchEnrole.invalidate();
            externalSearchSpinner.invalidate();

            Log.d("Loaders", "Après: loadingSearchEnrole visibility = " + loadingSearchEnrole.getVisibility());
            Log.d("Loaders", "Après: externalSearchSpinner visibility = " + externalSearchSpinner.getVisibility());
        });


        Toast.makeText(this, "Recherche dans la base externe...", Toast.LENGTH_SHORT).show();

        // Créer un thread pour l'appel réseau
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {

                // Construire l'URL avec les paramètres
                StringBuilder urlBuilder = new StringBuilder("http://57.128.30.4:8090/api/v1/recherche");

                // Ajouter les paramètres à l'URL
                boolean firstParam = true;

                if (lastName != null && !lastName.trim().isEmpty() && !lastName.equals(" ")) {
                    urlBuilder.append(firstParam ? "?" : "&");
                    urlBuilder.append("nom=").append(URLEncoder.encode(lastName.trim(), StandardCharsets.UTF_8.name()));
                    firstParam = false;
                }

                if (firstName != null && !firstName.trim().isEmpty()) {
                    urlBuilder.append(firstParam ? "?" : "&");
                    urlBuilder.append("prenoms=").append(URLEncoder.encode(firstName.trim(), StandardCharsets.UTF_8.name()));
                    firstParam = false;
                }

                if (birthday != null && !birthday.trim().isEmpty()) {
                    urlBuilder.append(firstParam ? "?" : "&");
                    urlBuilder.append("date_naissance=").append(URLEncoder.encode(birthday.trim(), StandardCharsets.UTF_8.name()));
                    firstParam = false;
                }

                /*if (birthPlace != null && !birthPlace.trim().isEmpty()) {
                    urlBuilder.append(firstParam ? "?" : "&");
                    urlBuilder.append("lieu_naissance=").append(URLEncoder.encode(birthPlace.trim(), StandardCharsets.UTF_8.name()));
                    firstParam = false;
                }*/

                /*if (phone != null && !phone.trim().isEmpty()) {
                    urlBuilder.append(firstParam ? "?" : "&");
                    urlBuilder.append("telephone=").append(URLEncoder.encode(phone.trim(), StandardCharsets.UTF_8.name()));
                    firstParam = false;
                }*/

                String finalUrl = urlBuilder.toString();
                Log.i("HANDLESEARCH: ", "URL de recherche: " + finalUrl);

                // Préparer la connexion
                URL url = new URL(finalUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET"); // Changé de POST à GET
                connection.setRequestProperty("Content-Type", "application/json");
                // Supprimer setDoOutput(true) car ce n'est plus nécessaire pour GET

                Log.i("HANDLESEARCH: ", "Envoi de la requête GET");

                // Obtenir la réponse
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Lire la réponse
                    Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name());
                    String responseBody = scanner.useDelimiter("\\A").next();
                    scanner.close();

                    Log.i("HANDLESEARCH: ", "Réponse reçue: " + responseBody);
                    Log.i("HANDLESEARCH: ", "avant processExternalAPIResponse");

                    // Traiter la réponse
                    processExternalAPIResponse(responseBody);
                } else {
                    // Gérer l'erreur
                    runOnUiThread(() -> {
                        Toast.makeText(SearchEnroleActivity.this,
                                "Aucun patient trouvé ",
                                Toast.LENGTH_LONG).show();

                        resultList.setVisibility(GONE);
                        noResultSection.setVisibility(VISIBLE);
                        loadingSearchEnrole.setVisibility(GONE);
                        externalSearchSpinner.setVisibility(GONE);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(SearchEnroleActivity.this,
                            "Erreur de connexion: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    loadingSearchEnrole.setVisibility(GONE);
                    externalSearchSpinner.setVisibility(GONE);
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }


    // Méthode pour traiter la réponse de l'API
    // Méthode pour traiter la réponse de l'API
    private void processExternalAPIResponse(String responseBody) {
        try {
            Log.i("HANDLESEARCH: ", "processExternalAPIResponse");
            // Analysez la réponse JSON et mettez à jour l'interface utilisateur
            JSONObject response = new JSONObject(responseBody);

            ProgressBar externalSearchSpinner = findViewById(R.id.externalSearchSpinner);

            // Vérifier si la réponse indique un succès et contient des données
            if (response.has("success") && response.getBoolean("success")
                    && response.has("data") && !response.isNull("data")) {

                JSONArray dataArray = response.getJSONArray("data");

                if (dataArray.length() > 0) {
                    runOnUiThread(() -> {
                        try {
                            patients.clear();

                            // Traiter chaque patient dans le tableau de données
                            for (int i = 0; i < dataArray.length(); i++) {
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

                                patients.add(externalPatient);
                            }

                            // Mettre à jour l'interface utilisateur
                            beforeActionSection.setVisibility(GONE);

                            // Enregistrer la métrique uniquement ici
                            date_fin = activityTracker.enregistrerDateFin();
                            saveMetrique();

                            resultList.setVisibility(VISIBLE);
                            noResultSection.setVisibility(GONE);
                            filterBox.setVisibility(VISIBLE);
                            adapter.notifyDataSetChanged();

                            // Message selon le nombre de patients trouvés
                            String message = dataArray.length() == 1
                                    ? "Patient trouvé dans la base externe"
                                    : dataArray.length() + " patients trouvés dans la base externe";

                            Toast.makeText(SearchEnroleActivity.this,
                                    message,
                                    Toast.LENGTH_SHORT).show();

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(SearchEnroleActivity.this,
                                    "Erreur lors du traitement des données: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            noResultSection.setVisibility(VISIBLE);
                        }
                        loadingSearchEnrole.setVisibility(GONE);
                        externalSearchSpinner.setVisibility(GONE);
                    });
                } else {
                    // Tableau de données vide
                    runOnUiThread(() -> {
                        Toast.makeText(SearchEnroleActivity.this,
                                "Aucun patient trouvé dans la base externe",
                                Toast.LENGTH_SHORT).show();
                        loadingSearchEnrole.setVisibility(GONE);
                        externalSearchSpinner.setVisibility(GONE);
                        noResultSection.setVisibility(VISIBLE);
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
                    Toast.makeText(SearchEnroleActivity.this,
                            finalErrorMessage,
                            Toast.LENGTH_SHORT).show();
                    loadingSearchEnrole.setVisibility(GONE);
                    externalSearchSpinner.setVisibility(GONE);
                    noResultSection.setVisibility(VISIBLE);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                ProgressBar externalSearchSpinner = findViewById(R.id.externalSearchSpinner);
                Toast.makeText(SearchEnroleActivity.this,
                        "Erreur lors de l'analyse de la réponse: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                loadingSearchEnrole.setVisibility(GONE);
                externalSearchSpinner.setVisibility(GONE);
                noResultSection.setVisibility(VISIBLE);
            });
        }
    }
    /*private void processExternalAPIResponse(String responseBody) {
        try {
            Log.i("HANDLESEARCH: ", "processExternalAPIResponse");
            // Analysez la réponse JSON et mettez à jour l'interface utilisateur
            JSONObject response = new JSONObject(responseBody);

            ProgressBar externalSearchSpinner = findViewById(R.id.externalSearchSpinner);

            // Vérifier si la réponse contient les données nécessaires
            // selon le format fourni: {"date_naissance": "09/05/1964", "nom_prenom": "MAO YAPO YVES", "numero_securite_sociale": "3847468984543"}
            if (response.has("nom_prenom") && !response.isNull("nom_prenom")) {
                runOnUiThread(() -> {
                    try {
                        patients.clear();

                        // Récupérer les données de la réponse
                        String nomPrenom = response.optString("nom_prenom", "");
                        String dateNaissance = response.optString("date_naissance", "");
                        String numSecu = response.optString("numero_securite_sociale", "");

                        // Séparer le nom et le prénom (en supposant que le premier mot est le nom)
                        String[] parts = nomPrenom.split(" ", 2);
                        String nom = parts.length > 0 ? parts[0] : "";
                        String prenoms = parts.length > 1 ? parts[1] : "";

                        // Créer un nouvel objet Patient avec les données disponibles
                        Patient externalPatient = new Patient(
                                0, // ID local (à définir selon votre logique)
                                nom,
                                prenoms,
                                dateNaissance,
                                phone != null ? phone : "", // Utiliser la valeur saisie par l'utilisateur
                                birthPlace != null ? birthPlace : "", // Utiliser la valeur saisie par l'utilisateur
                                "", // sexe (non disponible dans la réponse)
                                "", // csp (non disponible dans la réponse)
                                "", // cmr (non disponible dans la réponse)
                                numSecu, // numéro de sécurité sociale
                                "", // GUID (non disponible dans la réponse)
                                "" // nomjeunefille (non disponible dans la réponse)
                        );

                        patients.add(externalPatient);

                        // Mettre à jour l'interface utilisateur
                        beforeActionSection.setVisibility(GONE);
                        date_fin = activityTracker.enregistrerDateFin();
                        saveMetrique();
                        resultList.setVisibility(VISIBLE);
                        noResultSection.setVisibility(GONE);
                        filterBox.setVisibility(VISIBLE);
                        adapter.notifyDataSetChanged();

                        Toast.makeText(SearchEnroleActivity.this,
                                "Patient trouvé dans la base externe",
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(SearchEnroleActivity.this,
                                "Erreur lors du traitement des données: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        noResultSection.setVisibility(VISIBLE);
                    }
                    loadingSearchEnrole.setVisibility(GONE);
                    externalSearchSpinner.setVisibility(GONE);
                });
            } else {
                // Aucun résultat trouvé
                runOnUiThread(() -> {
                    Toast.makeText(SearchEnroleActivity.this,
                            "Aucun patient trouvé dans la base externe",
                            Toast.LENGTH_SHORT).show();
                    loadingSearchEnrole.setVisibility(GONE);
                    externalSearchSpinner.setVisibility(GONE);
                    noResultSection.setVisibility(VISIBLE);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                ProgressBar externalSearchSpinner = findViewById(R.id.externalSearchSpinner);
                Toast.makeText(SearchEnroleActivity.this,
                        "Erreur lors de l'analyse de la réponse: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                loadingSearchEnrole.setVisibility(GONE);
                externalSearchSpinner.setVisibility(GONE);
                noResultSection.setVisibility(VISIBLE);
            });
        }
    }**/

}