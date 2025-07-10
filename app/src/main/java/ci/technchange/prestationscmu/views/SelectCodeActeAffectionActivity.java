package ci.technchange.prestationscmu.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.utils.FseServiceDb;

public class SelectCodeActeAffectionActivity extends AppCompatActivity {
    private FseServiceDb fseServiceDb;
    private EditText editNumTrans;
    private Button btnSuivant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_code_acte_affection);

        fseServiceDb = FseServiceDb.getInstance(this);
        initViews();

        btnSuivant.setOnClickListener(v -> {
            String selectedNumTrans = editNumTrans.getText().toString().trim();

            if (selectedNumTrans.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer un numéro de transaction", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, ScannerFseActivity.class);
            intent.putExtra("NUM_TRANS", selectedNumTrans);

            startActivity(intent);
        });
    }

    private void initViews() {
        editNumTrans = findViewById(R.id.editNumTrans);
        btnSuivant = findViewById(R.id.btnSuivant);
    }
}