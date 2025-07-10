package ci.technchange.prestationscmu.views;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import ci.technchange.prestationscmu.R;

public class RecupererEmpreinteMainActivity extends AppCompatActivity {

    private ImageView imageIndexDroit, imageIndexGauche, imagePouceDroit, imagePouceGauche;
    private Button btnEnregistrer, btnRetour;
    private boolean[] empreintesCaptures = new boolean[4]; // [indexDroit, indexGauche, pouceDroit, pouceGauche]
    private static final int NOMBRE_MIN_EMPREINTES = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperer_empreinte_main);

        // Initialisation des vues
       /* imageIndexDroit = findViewById(R.id.imageIndexDroit);
        imageIndexGauche = findViewById(R.id.imageIndexGauche);
        imagePouceDroit = findViewById(R.id.imagePouceDroit);
        imagePouceGauche = findViewById(R.id.imagePouceGauche);*/
        btnEnregistrer = findViewById(R.id.enregistrerEmpreinte);
        btnRetour = findViewById(R.id.retourHome);

        // Récupération des données de l'inscription
        Intent intent = getIntent();
        String nom = intent.getStringExtra("NOM");
        String prenom = intent.getStringExtra("PRENOM");
        String telephone = intent.getStringExtra("TELEPHONE");
        String centreSante = intent.getStringExtra("CENTRE_SANTE");
        String photoPath = intent.getStringExtra("PHOTO_PATH");

        // Configuration des écouteurs
        setupClickListeners();
    }

    private void setupClickListeners() {

        btnRetour.setOnClickListener(v -> finish());

        /*imageIndexDroit.setOnClickListener(v -> capturerEmpreinte(imageIndexDroit, 0));
        imageIndexGauche.setOnClickListener(v -> capturerEmpreinte(imageIndexGauche, 1));
        imagePouceDroit.setOnClickListener(v -> capturerEmpreinte(imagePouceDroit, 2));
        imagePouceGauche.setOnClickListener(v -> capturerEmpreinte(imagePouceGauche, 3));

        findViewById(R.id.effacerIndexDroit).setOnClickListener(v -> effacerEmpreinte(imageIndexDroit, 0));
        findViewById(R.id.effacerIndexGauche).setOnClickListener(v -> effacerEmpreinte(imageIndexGauche, 1));
        findViewById(R.id.effacerPouceDroit).setOnClickListener(v -> effacerEmpreinte(imagePouceDroit, 2));
        findViewById(R.id.effacerPouceGauche).setOnClickListener(v -> effacerEmpreinte(imagePouceGauche, 3));*/

        btnEnregistrer.setOnClickListener(v -> {
            if (nombreEmpreintesCapturees() >= NOMBRE_MIN_EMPREINTES) {
                Intent resultIntent = new Intent();

                // Transmettre le statut de chaque empreinte
                resultIntent.putExtra("EMPREINTES_VALIDES", true);
                resultIntent.putExtra("INDEX_DROIT", empreintesCaptures[0]);
                resultIntent.putExtra("INDEX_GAUCHE", empreintesCaptures[1]);
                resultIntent.putExtra("POUCE_DROIT", empreintesCaptures[2]);
                resultIntent.putExtra("POUCE_GAUCHE", empreintesCaptures[3]);

                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Veuillez capturer au moins " + NOMBRE_MIN_EMPREINTES + " empreintes", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void capturerEmpreinte(ImageView imageView, int index) {

        empreintesCaptures[index] = true;
        imageView.setImageResource(R.drawable.empreinte_index_droit);


        CardView cardView = (CardView) imageView.getParent();
        cardView.setBackground(ContextCompat.getDrawable(this, R.drawable.card_background_selected));

        Toast.makeText(this, "Empreinte capturée", Toast.LENGTH_SHORT).show();
        verifierEmpreintesCompletes();
    }

    private void effacerEmpreinte(ImageView imageView, int index) {

        imageView.setImageResource(R.drawable.logo_empreinte);
        empreintesCaptures[index] = false;


        CardView cardView = (CardView) imageView.getParent();
        cardView.setBackground(ContextCompat.getDrawable(this, R.drawable.card_background_normal));

        verifierEmpreintesCompletes();
    }

    /*private void capturerEmpreinte(ImageView imageView, int index) {
        imageView.setSelected(true);
        empreintesCaptures[index] = true;
        imageView.setImageResource(R.drawable.empreinte_index_droit);

        Toast.makeText(this, "Empreinte capturée", Toast.LENGTH_SHORT).show();
        verifierEmpreintesCompletes();
    }**/

    /*private void effacerEmpreinte(ImageView imageView, int index) {
        imageView.setSelected(false);
        imageView.setImageResource(R.drawable.logo_empreinte);
        empreintesCaptures[index] = false;
        verifierEmpreintesCompletes();
    }**/

    private int nombreEmpreintesCapturees() {
        int count = 0;
        for (boolean captured : empreintesCaptures) {
            if (captured) count++;
        }
        return count;
    }

    private void verifierEmpreintesCompletes() {
        if (nombreEmpreintesCapturees() >= NOMBRE_MIN_EMPREINTES) {
            btnEnregistrer.setEnabled(true);
            btnEnregistrer.setBackgroundColor(Color.GREEN);
        } else {
            btnEnregistrer.setEnabled(false);
            btnEnregistrer.setBackgroundColor(Color.LTGRAY);
        }
    }
}