package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.ml.DocumentClassifier;
import ci.technchange.prestationscmu.ml.EdgeDetector;
import ci.technchange.prestationscmu.models.AutreFse;
import ci.technchange.prestationscmu.utils.AutreFspServiceDb;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final long PROCESSING_INTERVAL = 1000; // Milliseconds

    private ImageAnalysis imageAnalysis;
    private ProcessCameraProvider cameraProvider;

    private PreviewView previewView;
    private ScannerOverlayView scannerOverlay;
    private ImageView checkIcon;
    private Toolbar toolbar;

    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private DocumentClassifier documentClassifier;
    private EdgeDetector edgeDetector;
    private Camera camera;
    private AutreFspServiceDb autreFspServiceDb;

    private boolean canTakePhoto = false;
    private boolean isProcessing = false;
    private long lastProcessingTime = 0;
    String numTrans, numSecu, guid, affection1, affection2, acte1, acte2, acte3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Initialiser les vues
        previewView = findViewById(R.id.previewView);
        scannerOverlay = findViewById(R.id.scannerOverlay);
        checkIcon = findViewById(R.id.checkIcon);
        toolbar = findViewById(R.id.toolbar);
        autreFspServiceDb = AutreFspServiceDb.getInstance(this);

        // Récupérer l'intent
        Intent intent = getIntent();

        // Récupérer les données
        numTrans = intent.getStringExtra("NUM_TRANS");


        // Configurer la Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.scan_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Cacher l'icône de validation au démarrage
        checkIcon.setVisibility(View.GONE);

        // Vérifier les permissions de caméra
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }

        // Initialiser l'exécuteur pour les opérations de caméra
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialiser les classificateurs
        try {
            documentClassifier = new DocumentClassifier(this);
            edgeDetector = new EdgeDetector(this);
        } catch (Exception e) {
            Log.e(TAG, "Erreur d'initialisation des modèles ML: " + e.getMessage());
            Toast.makeText(this, "Erreur d'initialisation des modèles ML", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permission de caméra requise", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Erreur lors du démarrage de la caméra: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /*private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        // Configuration de la prévisualisation
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Configuration de la capture d'image
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        // Configuration de l'analyse d'image
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        // Sélectionner la caméra arrière
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Débinder toutes les utilisations précédentes
        cameraProvider.unbindAll();

        // Lier les cas d'utilisation à la caméra
        try {
            camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Échec du lien de cas d'utilisation: " + e.getMessage());
        }
    }*/
    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        // Stockage pour accès ultérieur
        this.cameraProvider = cameraProvider;

        // Configuration de la prévisualisation
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Configuration de la capture d'image
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        // Configuration de l'analyse d'image
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // S'assurer que l'analyzer est défini avant de lier
        if (imageAnalysis != null) {
            imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
        }

        // Sélectionner la caméra arrière
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Débinder toutes les utilisations précédentes
        cameraProvider.unbindAll();

        // Vérifier que les use cases sont bien initialisés
        try {
            if (preview != null && imageCapture != null && imageAnalysis != null) {
                // Lier les cas d'utilisation à la caméra
                camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis);
            } else {
                Log.e(TAG, "Un ou plusieurs use cases n'ont pas été initialisés correctement");
            }
        } catch (Exception e) {
            Log.e(TAG, "Échec du lien de cas d'utilisation: " + e.getMessage(), e);
        }
    }


    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(ImageProxy imageProxy) {
        // Si nous avons déjà programmé une capture, ignorer cette image
        if (canTakePhoto) {
            imageProxy.close();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (isProcessing || (currentTime - lastProcessingTime) < PROCESSING_INTERVAL) {
            imageProxy.close();
            return;
        }

        isProcessing = true;
        lastProcessingTime = currentTime;

        try {
            // Si le classificateur n'est pas initialisé, on ne fait rien
            if (documentClassifier == null) {
                Log.w(TAG, "DocumentClassifier n'est pas initialisé");
                imageProxy.close();
                isProcessing = false;
                return;
            }

            // Classifier l'image pour détecter si c'est un document médical
            float confidence = documentClassifier.classifyImage(imageProxy);
            Log.d(TAG, "Confiance de document: " + confidence);

            // Vérifier que nous ne sommes pas déjà en train de capturer
            if (!canTakePhoto) {
                final boolean isDocumentValid = confidence >= 0.6f;

                // Mettre à jour l'interface utilisateur en fonction des résultats
                runOnUiThread(() -> {
                    // Mettre à jour la couleur du cadre
                    if (scannerOverlay != null) {
                        scannerOverlay.setFrameColor(isDocumentValid ? Color.GREEN : Color.RED);
                    }

                    // Si c'est un document valide, préparer la capture
                    if (isDocumentValid && !canTakePhoto) {
                        // Marquer que nous allons prendre une photo pour éviter les appels multiples
                        canTakePhoto = true;

                        showCheckIcon();

                        // Attendre 2 secondes avant de capturer automatiquement
                        new Handler(Looper.getMainLooper()).postDelayed(
                                this::captureImage,
                                2000);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur d'analyse d'image: " + e.getMessage(), e);
        } finally {
            isProcessing = false;
            imageProxy.close();
        }
    }
    /*private void analyzeImage(ImageProxy imageProxy) {
        long currentTime = System.currentTimeMillis();
        if (isProcessing || (currentTime - lastProcessingTime) < PROCESSING_INTERVAL) {
            imageProxy.close();
            return;
        }

        isProcessing = true;
        lastProcessingTime = currentTime;

        try {
            // Classifier l'image pour détecter si c'est un document médical
            float confidence = documentClassifier.classifyImage(imageProxy);
            Log.d(TAG, "Confiance de document: " + confidence);

            // Mettre à jour l'interface utilisateur en fonction des résultats
            runOnUiThread(() -> {
                // Mettre à jour la couleur du cadre
                scannerOverlay.setFrameColor(confidence >= 0.6f ? Color.GREEN : Color.RED);
                // Mettre à jour l'état de capture
                canTakePhoto = confidence >= 0.6f;

                // Si nous pouvons prendre une photo, montrer l'icône de validation et
                // attendre un peu avant de capturer automatiquement
                if (canTakePhoto) {
                    showCheckIcon();

                    // Attendre 2 secondes avant de capturer automatiquement
                    new Handler(Looper.getMainLooper()).postDelayed(
                            this::captureImage,
                            2000);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur d'analyse d'image: " + e.getMessage());
        } finally {
            isProcessing = false;
            imageProxy.close();
        }
    }*/

    private void showCheckIcon() {
        // Afficher l'icône de validation brièvement
        checkIcon.setVisibility(View.VISIBLE);

        // Masquer l'icône après un délai
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> checkIcon.setVisibility(View.GONE),
                1000);
    }

    /*private void captureImage() {
        if (!canTakePhoto) return;

        // Créer un fichier temporaire pour l'image
        File photoFile = createTempImageFile();
        if (photoFile == null) {
            Log.e(TAG, "Impossible de créer un fichier temporaire");
            return;
        }

        // Configurer la sortie pour l'image capturée
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Capturer l'image
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d(TAG, "Image capturée: " + photoFile.getAbsolutePath());

                        // Traiter l'image et passer à l'écran de prévisualisation
                        processImageAndNavigate(photoFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Erreur de capture d'image: " + exception.getMessage());
                        Toast.makeText(CameraActivity.this,
                                "Impossible de capturer l'image",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }*/
    private void captureImage() {
        // Si la capture n'est pas possible ou déjà réalisée, ne rien faire
        if (!canTakePhoto || imageCapture == null) {
            return;
        }

        // Empêcher d'autres tentatives de capture
        canTakePhoto = false;

        // Créer un fichier temporaire pour l'image
        File photoFile = createTempImageFile();
        if (photoFile == null) {
            Log.e(TAG, "Impossible de créer un fichier temporaire");
            return;
        }

        // Configurer la sortie pour l'image capturée
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Capturer l'image
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d(TAG, "Image capturée: " + photoFile.getAbsolutePath());

                        // Traiter l'image et passer à l'écran de prévisualisation
                        processImageAndNavigate(photoFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Erreur de capture d'image: " + exception.getMessage());
                        Toast.makeText(CameraActivity.this,
                                "Impossible de capturer l'image",
                                Toast.LENGTH_SHORT).show();

                        // Réinitialiser l'état pour permettre une nouvelle tentative
                        canTakePhoto = false;
                    }
                }
        );
    }

    private File createTempImageFile() {
        try {
            // Créer un nom de fichier unique
            String timeStamp = String.valueOf(System.currentTimeMillis());
            String fileName = "JPEG_" + timeStamp + "_";

            // Obtenir le répertoire de cache externe
            File storageDir = getCacheDir();

            // Créer le fichier temporaire
            return File.createTempFile(
                    fileName,
                    ".jpg",
                    storageDir
            );
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la création du fichier: " + e.getMessage());
            return null;
        }
    }

    /*private void processImageAndNavigate(File photoFile) {
        try {
            // Utiliser le détecteur de bords pour traiter l'image
            File processedImage = edgeDetector.processImage(photoFile);

            // Passer à l'écran de prévisualisation
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.putExtra("imagePath", processedImage.getAbsolutePath());
            startActivity(intent);
            finish(); // Fermer cette activité

        } catch (Exception e) {
            Log.e(TAG, "Erreur de traitement d'image: " + e.getMessage());

            // En cas d'erreur, passer quand même l'image originale
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.putExtra("imagePath", photoFile.getAbsolutePath());
            startActivity(intent);
            finish();
        }
    }*/

    // Dans processImageAndNavigate() de CameraActivity.java
    private void processImageAndNavigate(File photoFile) {
        try {
            boolean isSaved = saveFse(numTrans, photoFile.getAbsolutePath());
            if (isSaved) {
                Log.d(TAG, "Autre fsp enregistreé avec succés");
            }else {
                Log.d(TAG, "Autre fsp non enregistreé ");
            }
            // Passer à l'écran de recadrage au lieu de prévisualisation directe
            Intent intent = new Intent(this, CropActivity.class);
            intent.putExtra("imagePath", photoFile.getAbsolutePath());
            intent.putExtra("NUM_TRANS", numTrans);

            startActivity(intent);
            finish(); // Fermer cette activité
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement de l'image: " + e.getMessage());

            boolean isSaved = saveFse(numTrans, photoFile.getAbsolutePath());
            if (isSaved) {
                Log.d(TAG, "Autre fsp enregistreé avec succés");
            }else {
                Log.d(TAG, "Autre fsp non enregistreé ");
            }
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.putExtra("imagePath", photoFile.getAbsolutePath());
            intent.putExtra("NUM_TRANS", numTrans);
            startActivity(intent);
            finish();
        }
    }

    private boolean saveFse(String numTransaction, String photoUrl) {
        try {
            AutreFse autreFsp = new AutreFse();

            // Définition des valeurs à partir des paramètres
            autreFsp.setNumTrans(numTransaction);
            autreFsp.setUrlPhoto(photoUrl);

            long result = autreFspServiceDb.insertAutreFsp(autreFsp);

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

    // Utilitaire pour convertir un ByteBuffer en Bitmap
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy plane = imageProxy.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // Rotation de l'image si nécessaire
        Matrix matrix = new Matrix();
        matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer les ressources
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        // Fermer les modèles ML
        if (documentClassifier != null) {
            documentClassifier.close();
        }
    }
}