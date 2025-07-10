package ci.technchange.prestationscmu.views;

import static androidx.constraintlayout.motion.widget.Debug.getLocation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import ci.technchange.prestationscmu.R;

public class CameraEtablissementActivity extends AppCompatActivity {

    private static final int PERMISSION_CAMERA = 101;
    public static final String EXTRA_PHOTO_PATH = "extra_photo_path";
    public static final String EXTRA_PHOTO_TYPE = "extra_photo_type";
    public static final String EXTRA_PHOTO_URI = "extra_photo_uri";
    public static final int PHOTO_TYPE_FACADE = 1;
    public static final int PHOTO_TYPE_INTERIEUR = 2;

    private PreviewView previewView;
    private Button btnCapture;
    private ImageCapture imageCapture;
    private int photoType;
    private String matricule;

    private static final int PERMISSION_LOCATION = 102;
    private LocationManager locationManager;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_etablissement);

        // Récupérer les extras
        if (getIntent().hasExtra(EXTRA_PHOTO_TYPE)) {
            photoType = getIntent().getIntExtra(EXTRA_PHOTO_TYPE, PHOTO_TYPE_FACADE);
        } else {
            finish();
            return;
        }

        // Add this to your onCreate method
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (checkCameraPermission() && checkLocationPermission()) {
            startCamera();
            getLocation();
        }

        matricule = getIntent().getStringExtra("MATRICULE");
        if (matricule == null) {
            matricule = "default";
        }

        // Initialiser les vues
        previewView = findViewById(R.id.previewView);
        btnCapture = findViewById(R.id.btnCapture);

        // Vérifier et demander les permissions
        if (checkCameraPermission()) {
            startCamera();
        }

        btnCapture.setOnClickListener(v -> takePhoto());
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
            return false;
        }
        return true;
    }

    // Add this method to check location permission
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_LOCATION);
            return false;
        }
        return true;
    }

    // Add this method to get location
    private void getLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                // Try to get last known location first
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (lastKnownLocation != null) {
                    latitude = lastKnownLocation.getLatitude();
                    longitude = lastKnownLocation.getLongitude();
                }

                // Also request location updates for more accuracy
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0, // minimum time interval between updates (milliseconds)
                        0, // minimum distance between updates (meters)
                        new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {}
                            @Override
                            public void onProviderEnabled(String provider) {}
                            @Override
                            public void onProviderDisabled(String provider) {}
                        }
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_CAMERA) {
                if (checkLocationPermission()) {
                    startCamera();
                    getLocation();
                }
            } else if (requestCode == PERMISSION_LOCATION) {
                startCamera();
                getLocation();
            }
        } else {
            Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        // Créer un fichier pour stocker l'image
        File photoFile = createImageFile();
        if (photoFile == null) {
            Toast.makeText(this, "Erreur lors de la création du fichier", Toast.LENGTH_SHORT).show();
            return;
        }

        // Créer les options de sortie
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build();

        // Capturer l'image
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri photoUri = FileProvider.getUriForFile(
                                CameraEtablissementActivity.this,
                                "ci.technchange.prestationscmuym.fileprovider",
                                photoFile);

                        // Retourner le résultat
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(EXTRA_PHOTO_PATH, photoFile.getAbsolutePath());
                        resultIntent.putExtra(EXTRA_PHOTO_URI, photoUri.toString());
                        resultIntent.putExtra("LATITUDE", latitude);  // Add latitude
                        resultIntent.putExtra("LONGITUDE", longitude);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(CameraEtablissementActivity.this,
                                "Erreur lors de la capture: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName;

        switch (photoType) {
            case PHOTO_TYPE_FACADE:
                imageFileName = "facade_" + matricule + "_" + timeStamp;
                break;
            case PHOTO_TYPE_INTERIEUR:
                imageFileName = "interieur_" + matricule + "_" + timeStamp;
                break;
            default:
                imageFileName = "photo_" + timeStamp;
        }

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;
    }
}