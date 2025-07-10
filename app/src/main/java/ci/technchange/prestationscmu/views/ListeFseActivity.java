package ci.technchange.prestationscmu.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import ci.technchange.prestationscmu.R;

public class ListeFseActivity extends AppCompatActivity {

    Button btnFragment1, btnFragment2, btnFragment3 ,btnFragment4,btnFragment5;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liste_fse);

       btnFragment1 = findViewById(R.id.btnFragment1);
       btnFragment2 = findViewById(R.id.btnFragment2);
       btnFragment3 = findViewById(R.id.btnFragment3);
       btnFragment4 = findViewById(R.id.btnFragment4);
       btnFragment5 = findViewById(R.id.btnFragment5);


        // Charger le Fragment 1 par défaut
      //  btnFragment1.setSelected(true);
        btnFragment2.setSelected(false);
        btnFragment3.setSelected(false);
        btnFragment4.setSelected(false);
        btnFragment5.setSelected(false);
        // btnFragment1.setBackgroundColor(android.graphics.Color.parseColor("#000000"));
        // btnFragment1.setTextColor(Color.WHITE);
        loadFragment(new ListeAllFse());

        btnFragment1.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                btnFragment1.setSelected(true);
                btnFragment2.setSelected(false);
                btnFragment3.setSelected(false);
                btnFragment4.setSelected(false);
                btnFragment5.setSelected(false);
                loadFragment(new ListeAllFse());
            }
        });

        btnFragment2.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                btnFragment1.setSelected(false);
                btnFragment2.setSelected(true);
                btnFragment3.setSelected(false);
                btnFragment4.setSelected(false);
                btnFragment5.setSelected(false);
                loadFragment(new ListeFseFinalise());
            }
        });

        btnFragment3.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                btnFragment1.setSelected(false);
                btnFragment2.setSelected(false);
                btnFragment3.setSelected(true);
                btnFragment4.setSelected(false);
                btnFragment5.setSelected(false);
                loadFragment(new ListeFseNonFinalise());

            }
        });
        btnFragment4.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                btnFragment1.setSelected(false);
                btnFragment2.setSelected(false);
                btnFragment3.setSelected(false);
                btnFragment4.setSelected(true);
                loadFragment(new ListeEntentePrealable());
                btnFragment5.setSelected(false);

            }
        });
        btnFragment5.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                btnFragment1.setSelected(false);
                btnFragment2.setSelected(false);
                btnFragment3.setSelected(false);
                btnFragment4.setSelected(false);
                btnFragment5.setSelected(true);
                loadFragment(new AutreFspAffichage());
            }
        });


        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ListeFseActivity.this, MainActivity.class);
                startActivity(intent);
               // finish(); // Ferme l'activité actuelle si nécessaire
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment);
        fragmentTransaction.commit();
    }
}