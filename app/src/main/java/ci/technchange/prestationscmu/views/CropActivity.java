package ci.technchange.prestationscmu.views;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.AutreFse;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.utils.AutreFspServiceDb;

public class CropActivity extends AppCompatActivity {
    private static final String TAG = "CropActivity";

    private CropImageView cropImageView;
    private Button btnCrop;
    private Toolbar toolbar;

    private String imagePath;
    String numTrans, numSecu, guid, affection1, affection2, acte1, acte2, acte3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        // Récupérer le chemin de l'image
        imagePath = getIntent().getStringExtra("imagePath");
        if (imagePath == null) {
            Toast.makeText(this, "Erreur: chemin d'image non fourni", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Récupérer l'intent
        Intent intent = getIntent();

        // Récupérer les données
        numTrans = intent.getStringExtra("NUM_TRANS");

        // Initialiser les vues
        cropImageView = findViewById(R.id.cropImageView);
        btnCrop = findViewById(R.id.btnCrop);
        toolbar = findViewById(R.id.toolbar);

        // Configurer la toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.crop_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Charger l'image
        loadImage();

        // Configurer le bouton de recadrage
        btnCrop.setOnClickListener(v -> cropImage());
    }

    private void loadImage() {
        try {
            // Charger l'image depuis le chemin
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) {
                Toast.makeText(this, "Impossible de charger l'image", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Afficher l'image dans la vue de recadrage
            cropImageView.setImageBitmap(bitmap);

            // Initialiser les coins du cadre par défaut (marge de 5%)
            float marginX = bitmap.getWidth() * 0.05f;
            float marginY = bitmap.getHeight() * 0.05f;
            RectF cropRect = new RectF(
                    marginX,
                    marginY,
                    bitmap.getWidth() - marginX,
                    bitmap.getHeight() - marginY
            );
            cropImageView.setCropRect(cropRect);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement de l'image", e);
            Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void cropImage() {
        try {
            // Obtenir le rectangle de recadrage
            RectF cropRect = cropImageView.getCropRect();

            // Charger à nouveau l'image originale
            Bitmap originalBitmap = BitmapFactory.decodeFile(imagePath);

            // Recadrer l'image
            Bitmap croppedBitmap = Bitmap.createBitmap(
                    originalBitmap,
                    Math.round(cropRect.left),
                    Math.round(cropRect.top),
                    Math.round(cropRect.width()),
                    Math.round(cropRect.height())
            );

            // Enregistrer l'image recadrée
            File croppedFile = saveBitmapToFile(croppedBitmap);

            // Libérer les ressources
            if (croppedBitmap != originalBitmap) {
                originalBitmap.recycle();
            }

            // Passer à l'écran de prévisualisation
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.putExtra("imagePath", croppedFile.getAbsolutePath());
            intent.putExtra("NUM_TRANS", numTrans);

            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du recadrage de l'image", e);
            Toast.makeText(this, "Erreur lors du recadrage", Toast.LENGTH_SHORT).show();
        }
    }

    private File saveBitmapToFile(Bitmap bitmap) throws IOException {
        // Créer un fichier temporaire
        File outputFile = new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg");

        // Enregistrer le bitmap
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        byte[] bitmapData = bos.toByteArray();

        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(bitmapData);
        fos.flush();
        fos.close();

        return outputFile;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}