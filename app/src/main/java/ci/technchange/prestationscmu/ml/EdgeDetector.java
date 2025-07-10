/*package com.example.scancmu.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EdgeDetector {
    private static final String TAG = "EdgeDetector";
    private final Context context;

    public EdgeDetector(Context context) {
        this.context = context;
    }

    public File processImage(File inputFile) {
        try {
            // Charger l'image
            Bitmap originalBitmap = BitmapFactory.decodeFile(inputFile.getAbsolutePath());
            if (originalBitmap == null) {
                Log.e(TAG, "Échec du décodage de l'image");
                return inputFile;
            }

            // Dans une application réelle, vous utiliseriez OpenCV ou ML Kit ici
            // pour une détection de document précise
            // Cette implémentation ajoute simplement un cadre pour illustrer

            // Créer une nouvelle image avec le cadre
            Bitmap processedBitmap = addDocumentFrame(originalBitmap);

            // Enregistrer l'image traitée
            File outputFile = new File(context.getCacheDir(), "processed_" + System.currentTimeMillis() + ".jpg");
            saveBitmapToFile(processedBitmap, outputFile);

            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement de l'image", e);
            return inputFile; // Retourner le fichier original en cas d'erreur
        }
    }

    private Bitmap addDocumentFrame(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Créer un bitmap mutable pour pouvoir dessiner dessus
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        // Définir un peu de marge pour le cadre
        float marginX = width * 0.05f;
        float marginY = height * 0.05f;

        // Créer un chemin pour le cadre du document
        Path path = new Path();
        path.moveTo(marginX, marginY); // Coin supérieur gauche
        path.lineTo(width - marginX, marginY); // Coin supérieur droit
        path.lineTo(width - marginX, height - marginY); // Coin inférieur droit
        path.lineTo(marginX, height - marginY); // Coin inférieur gauche
        path.close();

        // Configurer le pinceau pour le cadre
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setAntiAlias(true);

        // Dessiner le cadre
        canvas.drawPath(path, paint);

        // Dessiner des cercles aux coins
        paint.setStyle(Paint.Style.FILL);
        float cornerRadius = 20;
        canvas.drawCircle(marginX, marginY, cornerRadius, paint); // Coin supérieur gauche
        canvas.drawCircle(width - marginX, marginY, cornerRadius, paint); // Coin supérieur droit
        canvas.drawCircle(width - marginX, height - marginY, cornerRadius, paint); // Coin inférieur droit
        canvas.drawCircle(marginX, height - marginY, cornerRadius, paint); // Coin inférieur gauche

        return result;
    }

    private void saveBitmapToFile(Bitmap bitmap, File outputFile) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // Compresser l'image au format JPEG avec une qualité de 90%
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        byte[] bitmapData = bos.toByteArray();

        // Écrire dans le fichier
        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(bitmapData);
        fos.flush();
        fos.close();

        Log.d(TAG, "Image enregistrée à: " + outputFile.getAbsolutePath());
    }
}*/
package ci.technchange.prestationscmu.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EdgeDetector {
    private static final String TAG = "EdgeDetector";
    private final Context context;

    public EdgeDetector(Context context) {
        this.context = context;
    }

    public File processImage(File inputFile) {
        try {
            // Charger l'image
            Bitmap originalBitmap = BitmapFactory.decodeFile(inputFile.getAbsolutePath());
            if (originalBitmap == null) {
                Log.e(TAG, "Échec du décodage de l'image");
                return inputFile;
            }

            // Déterminer les marges pour le recadrage
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();

            // Définir un peu de marge pour le recadrage (5% de chaque côté)
            int marginX = (int)(width * 0.05f);
            int marginY = (int)(height * 0.05f);

            // Recadrer l'image au lieu de simplement dessiner un cadre
            Bitmap croppedBitmap = Bitmap.createBitmap(
                    originalBitmap,
                    marginX,                 // X de départ
                    marginY,                 // Y de départ
                    width - 2 * marginX,     // Largeur
                    height - 2 * marginY     // Hauteur
            );

            // Enregistrer l'image recadrée
            File outputFile = new File(context.getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg");
            saveBitmapToFile(croppedBitmap, outputFile);

            // Libérer les ressources
            if (croppedBitmap != originalBitmap) {
                originalBitmap.recycle();
            }

            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement de l'image", e);
            return inputFile; // Retourner le fichier original en cas d'erreur
        }
    }

    private void saveBitmapToFile(Bitmap bitmap, File outputFile) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // Compresser l'image au format JPEG avec une qualité de 90%
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        byte[] bitmapData = bos.toByteArray();

        // Écrire dans le fichier
        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(bitmapData);
        fos.flush();
        fos.close();

        Log.d(TAG, "Image recadrée enregistrée à: " + outputFile.getAbsolutePath());
    }
}