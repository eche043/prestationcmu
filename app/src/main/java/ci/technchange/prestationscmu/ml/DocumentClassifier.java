package ci.technchange.prestationscmu.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DocumentClassifier {
    private static final String TAG = "DocumentClassifier";
    private static final String MODEL_FILE = "model_unquant.tflite";
    private static final int INPUT_SIZE = 224;
    private static final int PIXEL_SIZE = 3;
    private static final int BATCH_SIZE = 1;
    private static final int FLOAT_TYPE_SIZE = 4;

    private final Interpreter interpreter;

    public DocumentClassifier(Context context) throws IOException {
        try {
            // Vérifier si le fichier existe avant de le charger
            String[] assets = context.getAssets().list("");
            boolean modelExists = false;
            for (String asset : assets) {
                if (asset.equals(MODEL_FILE)) {
                    modelExists = true;
                    break;
                }
            }

            if (!modelExists) {
                Log.e(TAG, "Le modèle " + MODEL_FILE + " n'existe pas dans les assets");
                throw new IOException("Modèle non trouvé dans les assets");
            }

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            interpreter = new Interpreter(FileUtil.loadMappedFile(context, MODEL_FILE), options);
            Log.d(TAG, "Modèle TensorFlow Lite chargé avec succès");
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors du chargement du modèle", e);
            throw e;
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    public float classifyImage(ImageProxy imageProxy) {
        try {
            // Convertir l'imageProxy en Bitmap
            Bitmap bitmap = imageToBitmap(imageProxy);
            if (bitmap == null) return 0.0f;

            // Redimensionner l'image à la taille d'entrée du modèle
            bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

            // Préparer le ByteBuffer pour l'entrée du modèle
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(
                    BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE * FLOAT_TYPE_SIZE);
            inputBuffer.order(ByteOrder.nativeOrder());

            // Normaliser et charger l'image dans le ByteBuffer
            preprocessImage(bitmap, inputBuffer);

            // Préparer le buffer de sortie - Modifier ici pour 2 sorties au lieu de 1
            float[][] outputBuffer = new float[1][2]; // Modification ici [1, 1] -> [1, 2]

            // Exécuter l'inférence
            interpreter.run(inputBuffer, outputBuffer);

            // Obtenir le score pour la classe 'feuilleExp' (index 0 comme dans votre code Flutter)
            return outputBuffer[0][0];
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la classification", e);
            return 0.0f;
        }
    }

    private void preprocessImage(Bitmap bitmap, ByteBuffer buffer) {
        buffer.rewind();

        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Normaliser les valeurs des pixels
        for (int i = 0; i < intValues.length; i++) {
            final int val = intValues[i];

            // Extraire les composantes RGB
            buffer.putFloat(((val >> 16) & 0xFF) / 255.0f * 2 - 1);
            buffer.putFloat(((val >> 8) & 0xFF) / 255.0f * 2 - 1);
            buffer.putFloat((val & 0xFF) / 255.0f * 2 - 1);
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private Bitmap imageToBitmap(ImageProxy imageProxy) {
        try {
            Image image = imageProxy.getImage();
            if (image == null) return null;

            // Créer un bitmap de taille appropriée
            Bitmap bitmap = Bitmap.createBitmap(
                    INPUT_SIZE,
                    INPUT_SIZE,
                    Bitmap.Config.ARGB_8888);

            // Tableau pour stocker les pixels
            int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];

            // Obtenir les plans YUV
            Image.Plane yPlane = image.getPlanes()[0];
            Image.Plane uPlane = image.getPlanes()[1];
            Image.Plane vPlane = image.getPlanes()[2];

            ByteBuffer yBuffer = yPlane.getBuffer();
            ByteBuffer uBuffer = uPlane.getBuffer();
            ByteBuffer vBuffer = vPlane.getBuffer();

            int yRowStride = yPlane.getRowStride();
            int uvRowStride = uPlane.getRowStride();
            int uvPixelStride = uPlane.getPixelStride();

            // Convertir les pixels YUV en RGB
            for (int y = 0; y < INPUT_SIZE; y++) {
                for (int x = 0; x < INPUT_SIZE; x++) {
                    // Calculer les coordonnées source
                    int sourceX = (int)(x * (image.getWidth() / (float)INPUT_SIZE));
                    int sourceY = (int)(y * (image.getHeight() / (float)INPUT_SIZE));

                    // Récupérer les valeurs YUV
                    int yIndex = sourceY * yRowStride + sourceX;
                    int uvIndex = (sourceY / 2) * uvRowStride + (sourceX / 2) * uvPixelStride;

                    int yValue = yBuffer.get(yIndex) & 0xFF;
                    int uValue = uBuffer.get(uvIndex) & 0xFF;
                    int vValue = vBuffer.get(uvIndex) & 0xFF;

                    // Convertir YUV en RGB
                    int r = yValue + (int)(1.402f * (vValue - 128));
                    int g = yValue - (int)(0.344f * (uValue - 128)) - (int)(0.714f * (vValue - 128));
                    int b = yValue + (int)(1.772f * (uValue - 128));

                    // Clamping
                    r = r < 0 ? 0 : (r > 255 ? 255 : r);
                    g = g < 0 ? 0 : (g > 255 ? 255 : g);
                    b = b < 0 ? 0 : (b > 255 ? 255 : b);

                    // Stocker le pixel RGB
                    pixels[y * INPUT_SIZE + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }

            // Remplir le bitmap avec les pixels transformés
            bitmap.setPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la conversion de l'image", e);
            return null;
        }
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}