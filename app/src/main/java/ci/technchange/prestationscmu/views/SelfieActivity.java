package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ci.technchange.prestationscmu.R;

public class SelfieActivity extends AppCompatActivity {
    private static final String TAG = "SelfieActivity";

    private TextureView textureView;
    private Button captureButton;

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    private File outputFile;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie);

        textureView = findViewById(R.id.texture_view);
        captureButton = findViewById(R.id.btn_take_picture);

        textureView.setSurfaceTextureListener(textureListener);
        captureButton.setOnClickListener(v -> takePicture());
        //setupMask();
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // Rechercher la caméra frontale
            for (String camId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(camId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = camId;
                    break;
                }
            }

            // Si pas de caméra frontale, utiliser la première disponible
            if (cameraId == null && manager.getCameraIdList().length > 0) {
                cameraId = manager.getCameraIdList()[0];
            }

            // Configurer la taille de l'image
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                imageDimension = getOptimalSize(map.getOutputSizes(SurfaceTexture.class));
                imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getHeight(),
                        ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            }

            // Vérifier les permissions
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
                return;
            }

            // Ouvrir la caméra
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getOptimalSize(Size[] sizes) {
        // Par défaut, prendre une résolution moyenne (ni trop grande ni trop petite)
        if (sizes.length > 0) {
            // Trier les tailles par aire (largeur * hauteur) et prendre celle du milieu
            Arrays.sort(sizes, (o1, o2) -> Long.compare(
                    (long) o1.getWidth() * o1.getHeight(),
                    (long) o2.getWidth() * o2.getHeight())
            );
            return sizes[sizes.length / 2]; // Taille médiane
        }
        return new Size(640, 480); // Taille par défaut
    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;

            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) return;

                            cameraCaptureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(SelfieActivity.this, "Configuration changée",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) return;

        try {
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        if (cameraDevice == null) {
            Toast.makeText(this, "Caméra non disponible, veuillez réessayer", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Créer le fichier de sortie
            createPhotoFile();

            // Configurer la capture
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Obtenir l'orientation de l'appareil
            int rotation = getWindowManager().getDefaultDisplay().getRotation();

            // Obtenir les caractéristiques de la caméra
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            // Définir l'orientation JPEG correcte
            int jpegOrientation = getJpegOrientation(characteristics, rotation * 90);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);

            // Lancer la capture
            final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    runOnUiThread(() -> {
                        try {
                            if (cameraDevice != null) {
                                createCameraPreview();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de la recréation de l'aperçu", e);
                        }
                    });
                }
            };

            if (cameraCaptureSession != null) {
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.capture(captureBuilder.build(), captureCallback, null);
            } else {
                Toast.makeText(this, "Session caméra non disponible", Toast.LENGTH_SHORT).show();
            }

        } catch (CameraAccessException e) {
            Log.e(TAG, "Erreur d'accès à la caméra", e);
            Toast.makeText(this, "Erreur d'accès à la caméra", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la prise de photo", e);
            Toast.makeText(this, "Erreur lors de la prise de photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void rotateImageIfRequired(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                // Charger l'image
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

                // Créer une matrice de rotation
                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                }

                // Appliquer la rotation
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                // Sauvegarder l'image rotée
                try (FileOutputStream out = new FileOutputStream(imagePath)) {
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                }

                // Libérer la mémoire
                bitmap.recycle();
                rotatedBitmap.recycle();
            }
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la rotation de l'image", e);
        }
    }

    private void createPhotoFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "SELFIE_" + timeStamp;

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        try {
            outputFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireLatestImage()) {
                if (image != null && outputFile != null) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    try (FileOutputStream output = new FileOutputStream(outputFile)) {
                        output.write(bytes);

                        // Corriger l'orientation si nécessaire
                        //rotateImageIfRequired(outputFile.getAbsolutePath());

                        // Retourner le résultat
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("PHOTO_PATH", outputFile.getAbsolutePath());
                        setResult(RESULT_OK, resultIntent);

                        // Afficher un message et terminer l'activité
                        runOnUiThread(() -> {
                            Toast.makeText(SelfieActivity.this, "Photo capturée avec succès",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permission caméra requise", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    private int getJpegOrientation(CameraCharacteristics characteristics, int deviceOrientation) {
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Vérifier si c'est une caméra frontale
        boolean isFrontCamera = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT;

        // Pour un appareil Famoco spécifique, nous devons ajuster l'orientation
        // Forcer une rotation de 180 degrés pour corriger l'inversion
        if (isFrontCamera) {
            return 270; // Rotation fixe pour résoudre le problème spécifique
        } else {
            // Pour la caméra arrière, calculer normalement
            deviceOrientation = (deviceOrientation + 45) / 90 * 90;
            return (sensorOrientation - deviceOrientation + 360) % 360;
        }
    }


    /*private void setupMask() {
        View ovalMask = findViewById(R.id.oval_mask);

        // Nous utilisons un ViewTreeObserver pour obtenir les dimensions après le rendu
        ovalMask.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Supprimer le listener pour éviter les appels multiples
                ovalMask.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Obtenir les dimensions du masque ovale
                int width = ovalMask.getWidth();
                int height = ovalMask.getHeight();
                int x = (int)ovalMask.getX();
                int y = (int)ovalMask.getY();

                // Obtenir les dimensions de l'écran
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                // Calculer les coordonnées du centre
                int centerX = metrics.widthPixels / 2;
                int centerY = metrics.heightPixels / 2 - 40; // Ajuster pour le décalage vertical

                // Positionner le masque au centre
                ovalMask.setX(centerX - width / 2);
                ovalMask.setY(centerY - height / 2);
            }
        });
    }*/

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        closeCamera();
        if (mBackgroundThread != null) stopBackgroundThread();
        super.onDestroy();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }
}