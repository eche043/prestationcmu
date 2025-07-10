package ci.technchange.prestationscmu.printing;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.GlobalClass;

public class TestSendFseToPrint extends AppCompatActivity {

    SQLiteDatabase db;
    private SQLiteDatabase database;
    /*
    private DatabaseHelper dbHelper; => db
    private SQLiteDatabase database; idem
     */

    public static String generateRandomString() {
        int length = 3;
        String characters = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder randomString = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }

        return randomString.toString();
    }

    // Méthode pour sélectionner une ligne aléatoire
    public Cursor getRandomRow(String tb) {
        Cursor cursor = database.rawQuery("SELECT * FROM enrole_"+tb+" ORDER BY RANDOM() LIMIT 1", null);
        return cursor;

    }

    protected Map getData(){
        String nomPrenomsAssure="Nom assure indefini";
        String num_secu="9x8x7x6x5x";
        String date_naissance="EstNeVers";
        String guid="le guid";
        String telephone="le telephone";
        Cursor cursor = null;
        if(GlobalClass.getInstance().cnxDbEnrole == null) {
            GlobalClass.getInstance().initDatabase("enrole");
        }
        db = GlobalClass.getInstance().cnxDbEnrole;
        try {
            cursor = getRandomRow(generateRandomString());
        } catch (Exception e) {
            System.out.println("Un execption est survenue ici là");
            System.out.println(e.getMessage());
            //e.printStackTrace();
        }

        //Cursor cursor = db.getRandomRow(generateRandomString());

        if (cursor != null && cursor.moveToFirst()) {
            int i = cursor.getColumnIndex("nom");
            nomPrenomsAssure = cursor.getString(i);
            i=cursor.getColumnIndex("prenoms");
            nomPrenomsAssure+= cursor.getString(i);
            i=cursor.getColumnIndex("num_secu");
            num_secu= cursor.getString(i);
            i=cursor.getColumnIndex("date_naissance");
            date_naissance= cursor.getString(i);
            i=cursor.getColumnIndex("GUID");
            guid= cursor.getString(i);
            i=cursor.getColumnIndex("telephone");
            telephone= cursor.getString(i);
            Log.d("MainActivity Test", "Random Row - Name: " + nomPrenomsAssure + ", num_secu: " + num_secu+ ", guid: " + guid+ ", telephone: " + telephone);
        }
        if (cursor != null) {
            cursor.close();
        }

        // Exemple de données
        Map<String, Object> data = new HashMap<>();
        data.put("dateSoins", "2025-02-08");
        data.put("OGD", "OGD Value");
        data.put("numFsInitiale", "123456");
        data.put("numTransaction", "789012");
        data.put("numEntentePrealable", "345678");
        data.put("numEntentePrealableAC", "901234");
        data.put("nomPrenomsAssure", nomPrenomsAssure);
        data.put("numSecu", (num_secu=="")?guid:num_secu);
        data.put("dateNaissance", date_naissance);
        data.put("genre", "M");
        data.put("numAssureAC", telephone);
        data.put("codeAC", "AC123");
        data.put("nomAC", "Nom AC");
        data.put("codeEtablissement", "ETB001");
        data.put("nomEtablissement", "Etablissement XYZ");
        data.put("typeFSE", "Type FSE");
        data.put("cocheCMR", true);
        data.put("cocheUrgence", false);
        data.put("cocheEloignement", true);
        data.put("cochereference", false);
        data.put("cocheAutre", true);
        data.put("precisionEtablissementAccueil", "Précision");
        data.put("codeProfessionnelSante", "PS123");
        data.put("nomProfessionnelSante", "Dr. John Doe");
        data.put("specialiteProfessionnelSante", "Généraliste");
        data.put("infoComp_Maternite", "Info Maternité");
        data.put("infoComp_AVP", "Info AVP");
        data.put("infoComp_ATMP", "Info ATMP");
        data.put("infoComp_AUTRE", "Info Autre");
        data.put("infoComp_PROGSPECIAL", "Programme Spécial");
        data.put("infoComp_CODE", "Code Info");
        data.put("infoComp_IMMVEH", "Imm Véhicule");
        data.put("infoComp_Observation", "Observation");
        data.put("codeAffection1", "Aff1");
        data.put("codeAffection2", "Aff2");

        List<Map<String, Object>> prestations = new ArrayList<>();
        Map<String, Object> prestation1 = new HashMap<>();
        prestation1.put("codeActe", "Acte001");
        prestation1.put("designation", "Désignation 1");
        prestation1.put("dateDebut", "2025-02-01");
        prestation1.put("dateFin", "2025-02-05");
        prestation1.put("numDent", "12");
        prestation1.put("quantite", 1);
        prestation1.put("montant", 100.0);
        prestation1.put("partCmu", 50.0);
        prestation1.put("partAC", 30.0);
        prestation1.put("partAssure", 20.0);
        prestations.add(prestation1);

        data.put("prestations", prestations);
        return data;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_test_envoie_printpdf);
        }catch (Exception e){

        }

        // Convertir les données en JSON
        Gson gson = new Gson();
        String jsonData = gson.toJson(getData());

        // Bouton pour envoyer les données
        Button sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(view -> {

            generatePdfForFSE fsp = new generatePdfForFSE(jsonData);

        });
    }
}