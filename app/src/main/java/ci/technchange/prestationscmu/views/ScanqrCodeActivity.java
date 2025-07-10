package ci.technchange.prestationscmu.views;

import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.core.dbHelper;
import ci.technchange.prestationscmu.models.Metrique;
import ci.technchange.prestationscmu.models.Patient;
import ci.technchange.prestationscmu.utils.ActivityTracker;
import ci.technchange.prestationscmu.utils.MetriqueServiceDb;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;

public class ScanqrCodeActivity  extends AppCompatActivity {
    private static final String TAG = "ScanQrCode";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private DecoratedBarcodeView barcodeView;
    private EditText champQrGuid,champNomQr;
    private ImageButton searchButton;
    private String lastScannedQR = "";
    private String lastEnteredName = "";
    private dbHelper dbHelper;
    private SQLiteDatabase db;
    private final Handler mainHandler;
    private Patient currentPatient;
    private boolean hasFoundPatient;
    ListView listViewPatients;
    private Button btnSuivantQrAcitvity;
    String lastName, guid,tableName;
    final int MIN_NAME_LENGTH = 4;

    private List<Patient> patientList;
    private PatientAdapter patientAdapter;

    private SharedPrefManager sharedPrefManager;
    private String dateString;
    private static String date_fin = "";
    private RequestQueue requestQueue;
    private static final String API_URL = "http://51.38.224.233:8080/api/v1/saveFSE";
    private ActivityTracker activityTracker;
    private UtilsInfosAppareil utilsInfos;
    private MetriqueServiceDb metriqueServiceDb;
    private SimpleDateFormat dateFormat;
    private boolean isBackPressed = false;

    public ScanqrCodeActivity(){this.mainHandler = new Handler(Looper.getMainLooper());}

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanqrcode);
        if (GlobalClass.getInstance().cnxDbEnrole == null) {
            GlobalClass.getInstance().initDatabase("enrole");
        }
        db = GlobalClass.getInstance().cnxDbEnrole;

        sharedPrefManager = new SharedPrefManager(this);
        metriqueServiceDb = MetriqueServiceDb.getInstance(this);
        activityTracker = new ActivityTracker(this);
        utilsInfos = new UtilsInfosAppareil(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateString = dateFormat.format(new Date());
        String downloadedFileName = sharedPrefManager.getDownloadedFileName();
        barcodeView = findViewById(R.id.barcode_scanner);
        champQrGuid = findViewById(R.id.champQrGuid);
        champNomQr = findViewById(R.id.champQrNom);
        searchButton = findViewById(R.id.searchButton);
        btnSuivantQrAcitvity = findViewById(R.id.btnSuivantQr);
        searchButton.setVisibility(View.GONE);
        listViewPatients = findViewById(R.id.listQrPatientTrouve);
        lastName= " ";

        patientList = new ArrayList<>();
        patientAdapter = new PatientAdapter(this, patientList);
        listViewPatients.setAdapter(patientAdapter);
        checkCameraPermission();

        setupTextWatchers();

        champNomQr.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                lastEnteredName = champNomQr.getText().toString();
                Log.d(TAG, "Nom enregistré: " + lastEnteredName);
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFields();
            }
        });

    }

    public void onBackPressed() {
        isBackPressed = true;
        saveMetrique();
        super.onBackPressed();
    }
    @SuppressLint("Range")
    private Patient createPatientFromCursor(Cursor cursor) {
        // Afficher les noms des colonnes disponibles
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

    private void setupTextWatchers() {
        champNomQr.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

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
            public void afterTextChanged(Editable s) {
            }
        });
        champQrGuid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence seq, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence seq, int i, int i1, int i2) {
                guid = seq.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private String getStringFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : "";
    }


    private String removeSpecialCharsAndNumbers(String input) {
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("[^a-zA-Z]");
        return pattern.matcher(normalized).replaceAll("");
    }

    private void handleNameChange() {
        currentPatient = null;
        hasFoundPatient = false;


        new Thread(() -> {
            try {

                String sql = "SELECT * FROM " + tableName + " WHERE GUID = '"+guid+"' AND nom LIKE '"+lastName+"%'";
                //String[] args = {guid, "%" + lastName + "%"};
                Log.d("SQL_QUERY", "Requête: " + sql + ", guid: " + guid + ", Name: " + lastName);
                if (GlobalClass.getInstance().cnxDbEnrole == null) {
                    GlobalClass.getInstance().initDatabase("enrole");
                }
                db = GlobalClass.getInstance().cnxDbEnrole;

                Cursor cursor = db.rawQuery(sql, new String[0]);
                Log.d("CURSOR_COUNT", "Nombre de lignes: " + (cursor != null ? cursor.getCount() : "null"));

                mainHandler.post(() -> {
                    try {
                        if (cursor != null && cursor.moveToFirst()) {
                            Log.d("Inside_cursor", "bienvenue");
                            currentPatient = createPatientFromCursor(cursor);
                            hasFoundPatient = true;
                            date_fin = activityTracker.enregistrerDateFin();
                            Log.d("CURSOR_DATA", "Patient trouvé: " + currentPatient.getNom()+" csp: "+ currentPatient.getCsp());
                            patientList.clear();
                            patientList.add(currentPatient);
                            patientAdapter.notifyDataSetChanged();
                            listViewPatients.setVisibility(ListView.VISIBLE);
                           /* btnSuivantQrAcitvity.setVisibility(View.VISIBLE);
                            btnSuivantQrAcitvity.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (currentPatient != null) {
                                        Intent intent = new Intent(ScanqrCodeActivity.this, FseEditActivity.class);
                                        intent.putExtra("PATIENT", currentPatient);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(ScanqrCodeActivity.this, "Aucun patient sélectionné", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });**/

                        } else {
                            Log.d("CURSOR_EMPTY", "Aucun patient trouvé.");
                            listViewPatients.setVisibility(ListView.GONE);
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
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            setupBarcodeScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupBarcodeScanner();
            } else {
                Toast.makeText(this, "La permission de la caméra est nécessaire pour scanner les QR codes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void setupBarcodeScanner() {
        barcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                lastScannedQR = result.getText();
                champQrGuid.setText(lastScannedQR);

                Log.d(TAG, "QR Code scanné: " + lastScannedQR);
                champQrGuid.setText(result.getText());
                champQrGuid.setVisibility(View.VISIBLE);
                barcodeView.setVisibility(View.GONE);
                barcodeView.pause();
                searchButton.setVisibility(View.VISIBLE);
                champNomQr.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resetFields() {
        lastScannedQR = "";
        lastEnteredName = "";

        champQrGuid.getText().clear();
        champNomQr.getText().clear();
        champQrGuid.getText().clear();
        barcodeView.resume();
        champQrGuid.setVisibility(View.VISIBLE);
        champNomQr.setVisibility(View.GONE);
        searchButton.setVisibility(View.GONE);
        barcodeView.setVisibility(View.VISIBLE);

        setupBarcodeScanner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    public String getLastScannedQR() {
        return lastScannedQR;
    }

    public String getLastEnteredName() {
        return lastEnteredName;
    }

}
