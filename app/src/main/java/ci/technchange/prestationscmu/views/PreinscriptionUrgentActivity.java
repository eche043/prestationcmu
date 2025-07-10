package ci.technchange.prestationscmu.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.Malade;

public class PreinscriptionUrgentActivity extends AppCompatActivity {

    private EditText nomMalade, prenomMalade, telMalade, dateNaisMalade,numeroPieceMalade;
    private Spinner sexeMalade, typePieceMalde;
    private Button suivantPreinscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preinscription_urgent);

        nomMalade = findViewById(R.id.champNomMalade);
        prenomMalade = findViewById(R.id.champPrenomMalade);
        telMalade = findViewById(R.id.champTelMalade);
        dateNaisMalade = findViewById(R.id.champDateNaissanceMalade);
        numeroPieceMalade = findViewById(R.id.champNumeroPiece);

        sexeMalade = findViewById(R.id.champSexeMalade);
        String[] sexes = new String[]{"M", "F"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sexes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sexeMalade.setAdapter(adapter);

        typePieceMalde = findViewById(R.id.champTypeIdentiteMalade);
        String[] typeIdentie = new String[]{"CNI", "Passeport", "Extrait de naissance"};
        ArrayAdapter<String> adapterType = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                typeIdentie
        );
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typePieceMalde.setAdapter(adapterType);
        suivantPreinscription = findViewById(R.id.btnSuivantPreinscription);

        suivantPreinscription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Récupérer les valeurs saisies
                String nom = nomMalade.getText().toString();
                String prenom = prenomMalade.getText().toString();
                String numero = telMalade.getText().toString();
                String dateNaissance = dateNaisMalade.getText().toString();
                String sexe = sexeMalade.getSelectedItem().toString();
                String typePiece = typePieceMalde.getSelectedItem().toString();
                String numeroPiece = numeroPieceMalade.getText().toString();


                Malade malade = new Malade(nom, prenom, numero, dateNaissance, sexe, typePiece, numeroPiece);


                Intent intent = new Intent(PreinscriptionUrgentActivity.this, photoPreinscriptionActivity.class);
                intent.putExtra("malade", malade);
                startActivity(intent);
            }
        });




    }
}