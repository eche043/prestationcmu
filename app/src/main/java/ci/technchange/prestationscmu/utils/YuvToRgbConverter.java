package ci.technchange.prestationscmu.utils;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Utilitaire pour convertir une image au format YUV en bitmap RGB
 */
public class YuvToRgbConverter {
    private static final String TAG = "YuvToRgbConverter";
    private final int rotationDegrees;

    public YuvToRgbConverter(int rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public void yuvToRgb(Image image, Bitmap bitmap) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            Log.e(TAG, "Format d'image non supporté: " + image.getFormat());
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Ne convertissez que si les dimensions correspondent
        if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
            Log.e(TAG, "Dimensions de bitmap incompatibles");
            return;
        }

        // Récupérer les plans YUV
        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        // Paramètres des plans
        int yRowStride = yPlane.getRowStride();
        int uvRowStride = uPlane.getRowStride();
        int uvPixelStride = uPlane.getPixelStride();

        // Tableau pour stocker les pixels
        int[] rgbData = new int[width * height];

        // Convertir les pixels YUV en RGB
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int yIndex = y * yRowStride + x;
                int uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride;

                int yValue = yBuffer.get(yIndex) & 0xFF;
                int uValue = uBuffer.get(uvIndex) & 0xFF;
                int vValue = vBuffer.get(uvIndex) & 0xFF;

                // Conversion YUV vers RGB
                int r = (int) (yValue + 1.402f * (vValue - 128));
                int g = (int) (yValue - 0.344f * (uValue - 128) - 0.714f * (vValue - 128));
                int b = (int) (yValue + 1.772f * (uValue - 128));

                // Clamping pour rester dans 0-255
                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                // Créer le pixel RGB
                rgbData[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        // Mettre à jour le bitmap avec les données RGB
        bitmap.setPixels(rgbData, 0, width, 0, 0, width, height);
    }
}