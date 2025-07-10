package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.utils.ReferentielService;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FormulaireEtablissementActivity extends AppCompatActivity {

    private static final String TAG = "FormulaireEtablissement";
    private static final int REQUEST_IMAGE_CAPTURE_FACADE = 1;
    private static final int REQUEST_IMAGE_CAPTURE_INTERIEUR = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private static final int REQUEST_CAMERA_PERMISSION = 102;

    private ImageView imageViewFacade, imageViewInterieur;
    private ImageButton btnCaptureFacade, btnCaptureInterieur;
    private EditText nomEditText, prenomEditText, contactEditText, etablissementEditText;
    private Button submitButton;

    private Uri photoUriFacade, photoUriInterieur;
    private Uri currentPhotoUri;
    private int currentRequestCode;
    private SharedPrefManager sharedPrefManager;
    private ReferentielService referentielService;
    private double latitudeFacade, longitudeFacade;
    private double latitudeInterieur, longitudeInterieur;
    private String codeEts;
    private String etablissementBycode;
    private String idFamoco;
    private String currentPhotoPath;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private UtilsInfosAppareil utilsInfos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulaire_etablissement);

        imageViewFacade = findViewById(R.id.imageViewEtablisementFacade);
        imageViewInterieur = findViewById(R.id.imageViewEtablissementInterieur);
        btnCaptureFacade = findViewById(R.id.btnCaptureFacadeAvant);
        btnCaptureInterieur = findViewById(R.id.btnCaptureIntereieurEtablissement);
        nomEditText = findViewById(R.id.nomEditText);
        prenomEditText = findViewById(R.id.prenomEditText);
        contactEditText = findViewById(R.id.contactEditText);
        etablissementEditText = findViewById(R.id.etablissementEditText);
        submitButton = findViewById(R.id.submitButton);
        utilsInfos = new UtilsInfosAppareil(this);

        referentielService = new ReferentielService(this);
        sharedPrefManager = new SharedPrefManager(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (currentRequestCode == REQUEST_IMAGE_CAPTURE_FACADE) {
                    latitudeFacade = location.getLatitude();
                    longitudeFacade = location.getLongitude();
                    Log.d(TAG, "Facade - Lat: " + latitudeFacade + ", Long: " + longitudeFacade);
                } else if (currentRequestCode == REQUEST_IMAGE_CAPTURE_INTERIEUR) {
                    latitudeInterieur = location.getLatitude();
                    longitudeInterieur = location.getLongitude();
                    Log.d(TAG, "Interieur - Lat: " + latitudeInterieur + ", Long: " + longitudeInterieur);
                }
                locationManager.removeUpdates(this);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        codeEts = sharedPrefManager.getCodeEts();
        etablissementBycode = referentielService.getEtbalissementByCodeEts(codeEts);
        idFamoco = utilsInfos.recupererIdAppareil();

        if (etablissementBycode != null && !etablissementBycode.isEmpty()) {
            etablissementEditText.setText(etablissementBycode);
        }

        btnCaptureFacade.setOnClickListener(v -> {
            currentRequestCode = REQUEST_IMAGE_CAPTURE_FACADE;
            checkPermissionsAndOpenCamera();
        });

        btnCaptureInterieur.setOnClickListener(v -> {
            currentRequestCode = REQUEST_IMAGE_CAPTURE_INTERIEUR;
            checkPermissionsAndOpenCamera();
        });

        submitButton.setOnClickListener(v -> submitForm());
    }

    private void checkPermissionsAndOpenCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_CAMERA_PERMISSION || requestCode == REQUEST_LOCATION_PERMISSION) {
                checkPermissionsAndOpenCamera(); // Check if all permissions are granted now
            }
        } else {
            Toast.makeText(this, "Les permissions sont nécessaires", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "SCMU_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            boolean dirCreated = storageDir.mkdirs();
            if (!dirCreated) {
                Log.e(TAG, "Échec de création du répertoire Pictures");
            }
        }

        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = imageFile.getAbsolutePath();
        Log.d(TAG, "Fichier image créé à: " + currentPhotoPath);
        return imageFile;
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Erreur lors de la création du fichier image", ex);
                Toast.makeText(this, "Erreur lors de la création du fichier image", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                try {
                    String authority = getPackageName() + ".fileprovider";
                    currentPhotoUri = FileProvider.getUriForFile(this, authority, photoFile);

                    Log.d(TAG, "Authority: " + authority);
                    Log.d(TAG, "URI de la photo: " + currentPhotoUri);

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);


                    List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(
                            takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);

                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, currentPhotoUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                    startActivityForResult(takePictureIntent, currentRequestCode);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la préparation de l'URI", e);
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(this, "Aucune application de caméra disponible", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            try {
                File photoFile = new File(currentPhotoPath);

                if (photoFile.exists() && photoFile.length() > 0) {
                    Log.d(TAG, "Fichier trouvé. Taille: " + photoFile.length() + " bytes");

                    if (requestCode == REQUEST_IMAGE_CAPTURE_FACADE) {
                        photoUriFacade = currentPhotoUri;
                        imageViewFacade.setImageURI(null);
                        imageViewFacade.setImageURI(photoUriFacade);
                        getLastLocation(true);
                    } else if (requestCode == REQUEST_IMAGE_CAPTURE_INTERIEUR) {
                        photoUriInterieur = currentPhotoUri;
                        imageViewInterieur.setImageURI(null);
                        imageViewInterieur.setImageURI(photoUriInterieur);
                        getLastLocation(false);
                    }
                } else {
                    Log.e(TAG, "Fichier image non trouvé ou vide à: " + currentPhotoPath);
                    Toast.makeText(this, "Erreur: Image non trouvée ou vide", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur traitement image", e);
                Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {

            if (currentPhotoPath != null) {
                new File(currentPhotoPath).delete();
            }
        }
    }

    private void getLastLocation(final boolean isFacade) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);

            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation == null) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnownLocation != null) {
                if (isFacade) {
                    latitudeFacade = lastKnownLocation.getLatitude();
                    longitudeFacade = lastKnownLocation.getLongitude();
                    Log.d(TAG, "Last Known Facade - Lat: " + latitudeFacade + ", Long: " + longitudeFacade);
                } else {
                    latitudeInterieur = lastKnownLocation.getLatitude();
                    longitudeInterieur = lastKnownLocation.getLongitude();
                    Log.d(TAG, "Last Known Interieur - Lat: " + latitudeInterieur + ", Long: " + longitudeInterieur);
                }
            } else {
                Log.w(TAG, "Aucune dernière position connue disponible");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur de localisation", e);
            Toast.makeText(this, "Erreur de localisation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void submitForm() {
        String nom = nomEditText.getText().toString().trim();
        String prenom = prenomEditText.getText().toString().trim();
        String contact = contactEditText.getText().toString().trim();
        String etablissement = etablissementEditText.getText().toString().trim();
        String nom_complet = nom + " " + prenom;
        codeEts = sharedPrefManager.getCodeEts();

        if (nom.isEmpty() || prenom.isEmpty() || contact.isEmpty() || etablissement.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoUriFacade == null || photoUriInterieur == null) {
            Toast.makeText(this, "Veuillez prendre les deux photos", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> formData = new HashMap<>();
        formData.put("nom_agent", nom_complet);
        formData.put("contact", contact);
        formData.put("nom_etablissement", etablissement);
        formData.put("code_ets", codeEts);
        formData.put("idFamoco", idFamoco);

        Map<String, Double> coordFacade = new HashMap<>();
        coordFacade.put("latitude", latitudeFacade);
        coordFacade.put("longitude", longitudeFacade);
        formData.put("coordonnees_facade", coordFacade);

        Map<String, Double> coordInterieur = new HashMap<>();
        coordInterieur.put("latitude", latitudeInterieur);
        coordInterieur.put("longitude", longitudeInterieur);
        formData.put("coordonnees_interieur", coordInterieur);

        sendDataToApi(formData);
    }

    private void sendDataToApi(Map<String, Object> formData) {
        OkHttpClient client = new OkHttpClient();

        try {
            File fileFacade = getFileFromUri(photoUriFacade);
            File fileInterieur = getFileFromUri(photoUriInterieur);

            if (fileFacade == null || !fileFacade.exists() || fileInterieur == null || !fileInterieur.exists()) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Erreur: Fichiers image introuvables", Toast.LENGTH_LONG).show());
                return;
            }

            Log.d(TAG, "Fichier façade: " + fileFacade.getAbsolutePath() + " (" + fileFacade.length() + " bytes)");
            Log.d(TAG, "Fichier intérieur: " + fileInterieur.getAbsolutePath() + " (" + fileInterieur.length() + " bytes)");

            MediaType mediaType = MediaType.parse("image/jpeg");
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

            builder.addFormDataPart("nom_agent", (String) formData.get("nom_agent"));
            builder.addFormDataPart("contact", (String) formData.get("contact"));
            builder.addFormDataPart("code_ets", (String) formData.get("code_ets"));
            builder.addFormDataPart("id_famoco", (String) formData.get("idFamoco"));
            builder.addFormDataPart("id_agent", "inconnu");

            Map<String, Double> coordFacade = (Map<String, Double>) formData.get("coordonnees_facade");
            Map<String, Double> coordInterieur = (Map<String, Double>) formData.get("coordonnees_interieur");

            builder.addFormDataPart("latitude_font", String.valueOf(coordFacade.get("latitude")));
            builder.addFormDataPart("longitude_font", String.valueOf(coordFacade.get("longitude")));
            builder.addFormDataPart("latitude_in", String.valueOf(coordInterieur.get("latitude")));
            builder.addFormDataPart("longitude_in", String.valueOf(coordInterieur.get("longitude")));

            builder.addFormDataPart("image_font", fileFacade.getName(),
                    RequestBody.create(fileFacade, mediaType));
            builder.addFormDataPart("image_in", fileInterieur.getName(),
                    RequestBody.create(fileInterieur, mediaType));

            RequestBody requestBody = builder.build();
            Request request = new Request.Builder()
                    .url("http://57.128.30.4:8090/api/v1/addEtablissementLocalisation")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur réseau", e);
                    runOnUiThread(() -> Toast.makeText(FormulaireEtablissementActivity.this,
                            "Erreur réseau: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.d(TAG, "API Response: Code: " + response.code() + ", Body: " + responseBody);

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(FormulaireEtablissementActivity.this,
                                    "Données envoyées avec succès !", Toast.LENGTH_SHORT).show();
                            resetForm();
                        } else {
                            Toast.makeText(FormulaireEtablissementActivity.this,
                                    "Erreur: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation envoi", e);
            runOnUiThread(() -> Toast.makeText(this,
                    "Erreur préparation: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Nullable
    private File getFileFromUri(Uri uri) {
        if (uri == null) return null;


        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath());
        }

        if (currentPhotoPath != null && uri.equals(currentPhotoUri)) {
            File file = new File(currentPhotoPath);
            if (file.exists()) {
                return file;
            }
        }

        if ("content".equals(uri.getScheme())) {
            try {

                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                            String filePath = cursor.getString(columnIndex);
                            if (filePath != null) {
                                File file = new File(filePath);
                                if (file.exists()) {
                                    return file;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur extraction chemin du fichier", e);
                    } finally {
                        cursor.close();
                    }
                }

                InputStream is = null;
                try {
                    is = getContentResolver().openInputStream(uri);
                    if (is != null) {
                        File tempFile = File.createTempFile("temp_img", ".jpg", getCacheDir());
                        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                            byte[] buffer = new byte[4096];
                            int length;
                            while ((length = is.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                            fos.flush();
                        }
                        return tempFile;
                    }
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // Ignorer
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur conversion URI", e);
            }
        }

        return null;
    }

    private void resetForm() {
        nomEditText.setText("");
        prenomEditText.setText("");
        contactEditText.setText("");
        imageViewFacade.setImageResource(android.R.color.transparent);
        imageViewInterieur.setImageResource(android.R.color.transparent);
        photoUriFacade = null;
        photoUriInterieur = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}