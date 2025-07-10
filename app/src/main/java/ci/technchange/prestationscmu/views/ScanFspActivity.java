package ci.technchange.prestationscmu.views;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;


import android.graphics.Bitmap;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import android.view.View;
import android.widget.Button;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.utils.FseServiceDb;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;


import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ScanFspActivity extends AppCompatActivity {

    private FseServiceDb fseServiceDb;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private ImageView imageView;
    private ImageView selectedImageView;
    private Uri photoUri;
    private Button btnCapture ,btnValider;
    private TextView mesinfos;

    private String photoPath , numguid ,extractedText;
    //private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_fsp);
        fseServiceDb = FseServiceDb.getInstance(this);
        imageView = findViewById(R.id.imageView);
        btnCapture = findViewById(R.id.btnCaptureS);
        btnValider = findViewById(R.id.btnvaliderphoto);
        mesinfos = findViewById(R.id.mesinfo);
        openCamera(imageView);

        ImageButton backButton = findViewById(R.id.backButtonHome);
        TextView textView = findViewById(R.id.textViewScanner);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera(imageView);
            }
        });
        btnValider.setOnClickListener(v -> {
            if (photoPath != null && !photoPath.isEmpty()) {
              //  saveImageToDatabase(photoPath); // Enregistre l'image dans SQLite

                Toast.makeText(this, "Photo enregistrée avec succès", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Veuillez d'abord capturer une photo", Toast.LENGTH_SHORT).show();
            }
        });

// Vérifier si ImageView a une image
        if (imageView.getDrawable() != null) {
            textView.setVisibility(View.GONE); // Masquer le texte
        } else {
            textView.setVisibility(View.VISIBLE); // Afficher le texte
        }
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScanFspActivity.this, MainActivity.class);
                startActivity(intent);
                // finish(); // Ferme l'activité actuelle si nécessaire
            }
        });
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public void saveImageToDatabase(String imagePath) {
        if (isConnected()) {
            // Si connecté, envoyer à l'API
            uploadImageToServer(imagePath , "","");
        } else {
            // Sinon, stocker l'image en local (SQLite)
            saveImageLocally(imagePath);
            Toast.makeText(this, "Image enregistrée localement. Elle sera envoyée quand la connexion reviendra.", Toast.LENGTH_LONG).show();
        }

    }

    private void saveImageLocally(String imagePath) {
    fseServiceDb.updateImageFseAmbulatoire(numguid,imagePath);

    }
    private void uploadImageToServer(String imagePath, String guid, String numsecu) {
        File imageFile = new File(imagePath);
       /* RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestBody);

        // Ajouter `guid` et `numsecu` en tant que RequestBody
        RequestBody guidPart = RequestBody.create(MediaType.parse("text/plain"), guid);
        RequestBody numsecuPart = RequestBody.create(MediaType.parse("text/plain"), numsecu);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ton-api.com/") // Remplace par l'URL de ton API
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<ResponseBody> call = apiService.uploadImage(body, guidPart, numsecuPart);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Image envoyée avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Échec d'envoi, sauvegarde en local", Toast.LENGTH_SHORT).show();
                    saveImageLocally(imagePath, guid, numsecu);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Erreur réseau, sauvegarde en local", Toast.LENGTH_SHORT).show();
                saveImageLocally(imagePath, guid, numsecu);
            }
        });*/
    }



    // Méthode pour ouvrir l'appareil photo
    private void openCamera(ImageView imageView) {
        selectedImageView = imageView;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoPath = photoFile.getAbsolutePath();
                photoUri = FileProvider.getUriForFile(this, "ci.technchange.prestationscmuym.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return new File(storageDir, imageFileName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            File file = new File(photoPath);
            if (file.exists()) {
                selectedImageView.setImageURI(photoUri);
                processImage();
            }else {
              //  Toast.makeText(this, "Erreur : la photo n'a pas été enregistrée.", Toast.LENGTH_SHORT).show();
                photoPath = null; // Remettre à null si la photo n'existe pas
            }
        }else {
           // Toast.makeText(this, "Capture annulée.", Toast.LENGTH_SHORT).show();
            photoPath = null; // Réinitialise photoPath si l'utilisateur annule
        }
    }

    private void processImage() {
        try {
            InputImage image = InputImage.fromFilePath(this, photoUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(this::extractData)
                    .addOnFailureListener(Throwable::printStackTrace);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void extractData(Text text) {
         extractedText = text.getText();
        mesinfos.setText(extractedText);
        Toast.makeText(ScanFspActivity.this, extractedText, Toast.LENGTH_SHORT).show();

        /*textView.setText("N°: " + idNumber + "\nNom: " + name + "\nPrénom(s): " + surname +
                "\nDate de Naissance: " + birthDate + "\nN° Sécurité Sociale: " + socialSecurityNumber);*/
    }


}