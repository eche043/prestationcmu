package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.models.ActeMedical;
import ci.technchange.prestationscmu.models.Affection;
import ci.technchange.prestationscmu.models.Professionel;

public class ReferentielService {

    private static ReferentielService instance;
    private final Context context;
    SQLiteDatabase db;

    public ReferentielService(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized ReferentielService getInstance(Context context) {
        if (instance == null) {
            instance = new ReferentielService(context);
        }
        return instance;
    }

    public List<Affection> getAllAffectations() {
        List<Affection> affectations = new ArrayList<>();
        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        Cursor cursor = db.rawQuery("SELECT * FROM ref_affection", null);


        if (cursor.moveToFirst()) {
            do {
                Affection affectation = new Affection();
                affectation.setId(cursor.getInt(cursor.getColumnIndex("id")));
                affectation.setCodeAffection(cursor.getString(cursor.getColumnIndex("code_affectation")));
                affectation.setLibelle(cursor.getString(cursor.getColumnIndex("libelle")));
                affectations.add(affectation);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return affectations;
    }
    public List<Professionel> getAllProfessionnel() {
        List<Professionel> professionels = new ArrayList<>();
        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        Cursor cursor = db.rawQuery("SELECT * FROM professionels_sante", null);


        if (cursor.moveToFirst()) {
            do {
                Professionel professionel = new Professionel();
                String nomcomplet = cursor.getString(cursor.getColumnIndex("nom")) +" "
                        +cursor.getString(cursor.getColumnIndex("prenom"));
                professionel.setId(cursor.getInt(cursor.getColumnIndex("id")));
                professionel.setInp(cursor.getString(cursor.getColumnIndex("inp")));
                professionel.setNom(nomcomplet);
                professionel.setSpecialite(cursor.getString(cursor.getColumnIndex("specialite")));
                professionel.setCodeEts(cursor.getString(cursor.getColumnIndex("code_ets")));
                professionel.setEtablissement(cursor.getString(cursor.getColumnIndex("etablissement")));
                professionels.add(professionel);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return professionels;
    }
    public Professionel getProfessionnelById(int id) {
        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        Cursor cursor = db.rawQuery("SELECT * FROM professionels_sante where id = ?", new String[]{String.valueOf(id)});

        Professionel professionel = new Professionel();
        if (cursor.moveToFirst()) {


            String nomcomplet = cursor.getString(cursor.getColumnIndex("nom")) +" "
                    +cursor.getString(cursor.getColumnIndex("prenom"));
            professionel.setId(cursor.getInt(cursor.getColumnIndex("id")));
            professionel.setInp(cursor.getString(cursor.getColumnIndex("inp")));
            professionel.setNom(nomcomplet);
            professionel.setSpecialite(cursor.getString(cursor.getColumnIndex("specialite")));
            professionel.setCodeEts(cursor.getString(cursor.getColumnIndex("code_ets")));
            professionel.setEtablissement(cursor.getString(cursor.getColumnIndex("etablissement")));


        }
        cursor.close();

        return professionel;
    }



    public List<Professionel> getAllProfessionnelByCentre(String code) {
        List<Professionel> professionels = new ArrayList<>();
        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        //Cursor cursor = db.rawQuery("SELECT * FROM professionels_sante WHERE etablissement like ?", new String[]{"%" + etablissement + "%"});
        Cursor cursor = db.rawQuery("SELECT * FROM professionels_sante WHERE code_ets = ?", new String[]{code});


        if (cursor.moveToFirst()) {
            do {
                Professionel professionel = new Professionel();
                String nomcomplet = cursor.getString(cursor.getColumnIndex("nom")) +" "
                        +cursor.getString(cursor.getColumnIndex("prenom"));
                professionel.setId(cursor.getInt(cursor.getColumnIndex("id")));
                professionel.setInp(cursor.getString(cursor.getColumnIndex("inp")));
                professionel.setNom(nomcomplet);
                professionel.setSpecialite(cursor.getString(cursor.getColumnIndex("specialite")));
                professionel.setCodeEts(cursor.getString(cursor.getColumnIndex("code_ets")));
                professionel.setEtablissement(cursor.getString(cursor.getColumnIndex("etablissement")));
                professionels.add(professionel);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return professionels;
    }

    public List<Professionel> getAllProfessionnelByName(String name) {
        List<Professionel> professionels = new ArrayList<>();
        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        Cursor cursor = db.rawQuery("SELECT * FROM professionels_sante WHERE nom like ? or prenom like ?",new String[]{"%" + name + "%", "%" + name + "%"});


        if (cursor.moveToFirst()) {
            do {
                Professionel professionel = new Professionel();
                String nomcomplet = cursor.getString(cursor.getColumnIndex("nom")) +" "
                        +cursor.getString(cursor.getColumnIndex("prenom"));
                professionel.setId(cursor.getInt(cursor.getColumnIndex("id")));
                professionel.setInp(cursor.getString(cursor.getColumnIndex("inp")));
                professionel.setNom(nomcomplet);
                professionel.setSpecialite(cursor.getString(cursor.getColumnIndex("specialite")));
                professionel.setCodeEts(cursor.getString(cursor.getColumnIndex("code_ets")));
                professionel.setEtablissement(cursor.getString(cursor.getColumnIndex("etablissement")));
                professionels.add(professionel);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return professionels;
    }

    public List<ActeMedical> getAllActes() {
        List<ActeMedical> acteMedicals = new ArrayList<>();
        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        Cursor cursor = db.rawQuery("SELECT * FROM ref_actes_medicaux order by code asc", null);


        if (cursor.moveToFirst()) {
            do {
                ActeMedical acte = new ActeMedical();

                acte.setCode(cursor.getString(cursor.getColumnIndex("code")));
                acte.setLibelle(cursor.getString(cursor.getColumnIndex("libelle")));
                acte.setTarif(cursor.getDouble(cursor.getColumnIndex("tarif")));

                acteMedicals.add(acte);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return acteMedicals;
    }

    public String getRegionomByEtablissement(String etablissement) {
        String region = null;

        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;


        Cursor cursor = db.rawQuery(
                "SELECT region FROM etablissements WHERE etablissement = ? LIMIT 1",
                new String[]{etablissement}
        );

        if (cursor.moveToFirst()) {
            region = cursor.getString(cursor.getColumnIndex("region"));
        }
        cursor.close();

        return region;
    }


    /*public List<String> getEtablissementsByDepartement(String departement) {
        List<String> etablissements = new ArrayList<>();

        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        // Requête avec filtre sur le département
        String query = "SELECT DISTINCT etablissement FROM professionels_sante " +
                "WHERE departement = ? " +
                "ORDER BY etablissement ASC";

        Cursor cursor = db.rawQuery(query, new String[]{departement});

        if (cursor.moveToFirst()) {
            do {
                etablissements.add(cursor.getString(cursor.getColumnIndex("etablissement")));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return etablissements;
    }*/

    public List<String> getAllEtablissements() {
        List<String> etablissements = new ArrayList<>();

        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;
        Cursor cursor = db.rawQuery("SELECT DISTINCT etablissement FROM etablissements  ORDER BY etablissement ASC", null);
        if (cursor.moveToFirst()) {
            do {
                etablissements.add(cursor.getString(cursor.getColumnIndex("etablissement")));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return etablissements;
    }

    public String getEtbalissementByCodeEts(String code){
        String etablissement = null;
        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        Cursor cursor = db.rawQuery(
                "SELECT etablissement FROM etablissements WHERE code_ets = ? LIMIT 1",
                new String[]{code}
        );

        if (cursor.moveToFirst()){
            etablissement = cursor.getString(cursor.getColumnIndex("etablissement"));
        }
        cursor.close();

        return etablissement;


    }


    public String getCodeEtsForEtablissement(String etablissement) {
        String codeEts = null;

        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;


        Cursor cursor = db.rawQuery(
                "SELECT code_ets FROM etablissements WHERE etablissement = ? LIMIT 1",
                new String[]{etablissement}
        );

        if (cursor.moveToFirst()) {
            codeEts = cursor.getString(cursor.getColumnIndex("code_ets"));
        }
        cursor.close();

        return codeEts;
    }

    public ActeMedical getActeMedicalByCode(String code) {
        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        Log.d("DB_QUERY", "Recherche acte avec code: " + code);

        ActeMedical acte = null;
        Cursor cursor = db.rawQuery("SELECT * FROM ref_actes_medicaux WHERE code = ?", new String[]{code});

        if (cursor != null) {
            Log.d("DB_QUERY", "Nombre de résultats: " + cursor.getCount());
            if (cursor.moveToFirst()) {
                acte = new ActeMedical();
                acte.setCode(cursor.getString(cursor.getColumnIndex("code")));
                acte.setLibelle(cursor.getString(cursor.getColumnIndex("libelle")));
                acte.setTarif(cursor.getDouble(cursor.getColumnIndex("tarif")));
                acte.setCoeficient(cursor.getString(cursor.getColumnIndex("coefficient")));


                String lettreCle = cursor.getString(cursor.getColumnIndex("lettre_cle"));
                acte.setLettreCle(lettreCle != null ? lettreCle.trim() : null);

                Log.d("DB_RESULT", "Acte trouvé: " + acte.toString() +
                        ", LettreCle: '" + acte.getLettreCle() + "'");
            } else {
                Log.d("DB_RESULT", "Aucun acte trouvé pour ce code");
            }
            cursor.close();
        } else {
            Log.d("DB_RESULT", "Cursor est null");
        }

        return acte;
    }

    public double calculerMontantActe(String codeActe) {
        if (codeActe == null || codeActe.isEmpty()) {
            Log.d("MONTANT", "Code acte vide");
            return 0.0;
        }

        ActeMedical acte = getActeMedicalByCode(codeActe);
        if (acte == null) {
            Log.d("MONTANT", "Aucun acte trouvé pour code: " + codeActe);
            return 0.0;
        }

        Log.d("MONTANT", "Calcul montant pour: " + acte.toString());

        double coefficient = 0.0;
        try {
            coefficient = Double.parseDouble(acte.getCoeficient());
            Log.d("MONTANT", "Coefficient: " + coefficient);
        } catch (NumberFormatException e) {
            Log.e("MONTANT", "Erreur parsing coefficient: " + acte.getCoeficient());
            return 0.0;
        }

        Log.d("MONTANT", "Lettre clé avant calcul: '" + acte.getLettreCle() + "'");
        double tarifBase = getTarifForLettreCle(acte.getLettreCle());
        Log.d("MONTANT", "Tarif base: " + tarifBase);

        double montant = coefficient * tarifBase;
        Log.d("MONTANT", "Montant calculé: " + montant);

        return montant;
    }

    private double getTarifForLettreCle(String lettreCle) {
        if (lettreCle == null) {
            return 0.0;
        }


        String lettreCleTrimmed = lettreCle.trim().toUpperCase();
        Log.d("TARIF", "Lettre clé après trim: '" + lettreCleTrimmed + "'");

        switch (lettreCleTrimmed) {
            case "K":
                return 250.0;
            case "Z":
            case "R":
                return 300.0;
            case "B":
                return 100.0;
            case "D":
                return 200.0;
            case "URGENCES_HG":
                return 2000.0;
            case "URGENCES_CHR":
                return 2500.0;
            case "URGENCES_CHU":
                return 5000.0;
            case "REANIMATION_CHU":
            case "REANIMATION_CHR_HG":
                return 10000.0;
            case "AMI": // AMI
                return 100.0;
            case "AMK": // AMK/SEANCE
                return 1000.0;
            case "SFI": // SFI
                return 100.0;
            default:
                Log.d("TARIF", "Lettre clé non reconnue: '" + lettreCleTrimmed + "'");
                return 0.0;
        }
    }
    public List<String> getAllEtablissementsDistincts(String departement) {
        List<String> etablissements = new ArrayList<>();

        if (GlobalClass.getInstance().cnxDbReferentiel == null) {
            GlobalClass.getInstance().initDatabase("referentiel");
        }
        db = GlobalClass.getInstance().cnxDbReferentiel;

        String query;
        String[] selectionArgs;

        if (departement != null && !departement.isEmpty()) {
            query = "SELECT DISTINCT etablissement FROM professionels_sante " +
                    "WHERE departement = ? " +
                    "ORDER BY etablissement COLLATE NOCASE ASC";
            selectionArgs = new String[]{departement};
        } else {
            query = "SELECT DISTINCT etablissement FROM professionels_sante " +
                    "ORDER BY etablissement COLLATE NOCASE ASC";
            selectionArgs = null;
        }

        Cursor cursor = db.rawQuery(query, selectionArgs);

        if (cursor.moveToFirst()) {
            do {
                etablissements.add(cursor.getString(cursor.getColumnIndex("etablissement")));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return etablissements;
    }
}
