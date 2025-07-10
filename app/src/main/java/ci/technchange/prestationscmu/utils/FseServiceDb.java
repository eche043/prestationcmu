package ci.technchange.prestationscmu.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.models.FseAmbulatoire;

public class FseServiceDb {
    private static FseServiceDb instance;
    private final Context context;
    SQLiteDatabase db;

    // Noms des tables
    private static final String TABLE_FSE_AMBULATOIRE = "fse_ambulatoire";
    private static final String TABLE_PRESTATION_AMBULATOIRE = "prestation_ambulatoire";
    private static final String TABLE_VERSION_DB = "version_bd";

    private FseServiceDb(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized FseServiceDb getInstance(Context context) {
        if (instance == null) {
            instance = new FseServiceDb(context);
        }
        return instance;
    }

    public int getVersionBD() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int version = 0;

        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;
            cursor = db.rawQuery("SELECT numero_version FROM version_bd WHERE id = 1", null);

            if (cursor != null && cursor.moveToFirst()) {
                version = cursor.getInt(cursor.getColumnIndex("numero_version"));
            }
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la récupération de la version de la base de données", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return version;
    }

    public int getVersionBDReferentiel() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int version = 0;

        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;
            cursor = db.rawQuery("SELECT numero_version_referentiel FROM version_bd WHERE id = 1", null);

            if (cursor != null && cursor.moveToFirst()) {
                version = cursor.getInt(cursor.getColumnIndex("numero_version_referentiel"));
            }
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la récupération de la version de la base de données", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return version;
    }


    public boolean updateVersionBD(int newVersion) {
        SQLiteDatabase db = null;
        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;
            ContentValues values = new ContentValues();
            values.put("numero_version", newVersion);
            int rowsAffected = db.update("version_bd", values, "id = ?", new String[]{"1"});

            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la mise à jour de la version de la base de données", e);
            return false;
        }
    }


    public boolean updateVersionBDReferentiel(int newVersion) {
        SQLiteDatabase db = null;
        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;
            ContentValues values = new ContentValues();
            values.put("numero_version_referentiel", newVersion);
            int rowsAffected = db.update("version_bd", values, "id = ?", new String[]{"1"});

            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la mise à jour de la version de la base de données", e);
            return false;
        }
    }


    public boolean updateEtablissementCheck(int newCheck){
        SQLiteDatabase db =null;
        try{
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;
            ContentValues values = new ContentValues();
            values.put("etablissement_check", newCheck);
            int rowsAffected = db.update("parametre_fammoco", values, "id = ?", new String[]{"1"});
            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la mise à jour du statut de l'établissement ", e);
            return false;
        }
    }

    public boolean updateFamocoCheck(int newFamocoCheck){
        SQLiteDatabase db = null;
        try{
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;
            ContentValues values = new ContentValues();
            values.put("famoco_check",newFamocoCheck);
            int rowsAffected  = db.update("parametre_famoco",values, "id=?", new String[]{"1"});
            return rowsAffected > 0;
        }catch (Exception e){
            Log.e("FseServiceDb", "Erreur lors de la mise à jour du statut du famoco ", e);
            return false;
        }
    }

    public boolean updatePhotoCheck(int newPhotoCheck){
        SQLiteDatabase db = null;
        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;
            ContentValues values = new ContentValues();
            values.put("photo_check",newPhotoCheck);
            int rowsAffected = db.update("parametre_famoco", values, "id = ?", new String[]{"1"});
            return rowsAffected > 0;
        }catch (Exception e){
            Log.e("FseServiceDb", "Erreur lors de la mise à jour du statut du photo ", e);
            return false;
        }
    }

    public long insertFseAmbulatoire(FseAmbulatoire fse) {
        try {

            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            if (db == null){
                Log.e("FseServiceDb","La base de données n'a pas pu être initialisée.");
            }

            ContentValues values = new ContentValues();
            values.put("num_fs_initial", fse.getNumFsInitial());
            values.put("num_trans", fse.getNumTrans());
            values.put("num_secu", fse.getNumSecu());
            values.put("num_guid", fse.getNumGuid());
            values.put("nom_complet", fse.getNomComplet());
            values.put("sexe", fse.getSexe());
            values.put("date_naissance", fse.getDateNaissance());
            values.put("nom_etablissement", fse.getNomEtablissement());
            values.put("code_ets", fse.getCodeEts());
            values.put("date_soins",fse.getDate_soins());
            values.put("etablissement_cmr", fse.isEtablissementCmr() ? 1 : 0);
            values.put("etablissement_urgent", fse.isEtablissementUrgent() ? 1 : 0);
            values.put("etablissement_ref", fse.isEtablissementRef() ? 1 : 0);
            values.put("etablissement_eloignement", fse.isEtablissementEloignement() ? 1 : 0);
            values.put("etablissement_precision", fse.getEtablissementPrecision());
            values.put("professionnel", fse.getProfessionnel());
            values.put("code_professionnel", fse.getCodeProfessionnel());
            values.put("spe_professionnel", fse.getSpeProfessionnel());
            values.put("code_aff1", fse.getCodeAff1());
            values.put("code_aff2", fse.getCodeAff2());
            values.put("info_maternite", fse.isInfoMaternite() ? 1 : 0);
            values.put("info_avp", fse.isInfoAVP() ? 1 : 0);
            values.put("info_atmp", fse.isInfoATMP() ? 1 : 0);
            values.put("info_autre", fse.isInfoAutre() ? 1 : 0);
            values.put("statusProgres", fse.iStatusProgres());
            values.put("preinscription", fse.isPreInscription() ? 1 : 0);
            values.put("motif",fse.getMotif());
            values.put("nombre_jour",fse.getNombre_jour());
            values.put("codeAffection",fse.getCodeAffection());
            values.put("type_fse",fse.getType_fse());
            values.put("code_acte1",fse.getCode_acte1());
            values.put("code_acte2",fse.getCode_acte2());
            values.put("code_acte3",fse.getCode_acte3());
            values.put("quantite_1",fse.getQuantite_1());
            values.put("quantite_2",fse.getQuantite_2());
            values.put("quantite_3",fse.getCode_acte3());
            values.put("montant_acte",fse.getMontant_acte());
            values.put("part_cmu", fse.getPart_cmu());
            values.put("part_assure",fse.getPart_assure());



            return db.insert(TABLE_FSE_AMBULATOIRE, null, values);
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de l'insertion du FSE ambulatoire", e);
            return -1;
        }
    }

    @SuppressLint("LongLogTag")
    public FseAmbulatoire getFseAmbulatoireByNumTrans(String numTrans) {
        FseAmbulatoire fse = null;
        Cursor cursor = null;

        try {
            // Initialisation de la base de données si nécessaire
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            // Vérification que la base de données est bien initialisée
            if (db == null) {
                Log.e("getFseAmbulatoireByNumTrans", "La connexion à la base de données est null.");
                return null;
            }

            // Exécution de la requête
            cursor = db.rawQuery("SELECT * FROM fse_ambulatoire WHERE num_trans = ?", new String[]{numTrans});

            // Vérification que le Cursor contient des données
            if (cursor != null && cursor.moveToFirst()) {
                fse = new FseAmbulatoire();

                // Récupération des données avec validation des colonnes
                fse.setId(getIntFromCursor(cursor, "id"));
                fse.setNumFsInitial(cursor.getString(cursor.getColumnIndex("num_fs_initial")));
                fse.setNumTrans(getStringFromCursor(cursor, "num_trans"));
                fse.setNumSecu(getStringFromCursor(cursor, "num_secu"));
                fse.setNumGuid(getStringFromCursor(cursor, "num_guid"));
                fse.setNomComplet(getStringFromCursor(cursor, "nom_complet"));
                fse.setSexe(getStringFromCursor(cursor, "sexe"));
                fse.setDateNaissance(getStringFromCursor(cursor, "date_naissance"));
                fse.setNomEtablissement(getStringFromCursor(cursor, "nom_etablissement"));
                fse.setCodeEts(getStringFromCursor(cursor, "code_ets"));
                fse.setEtablissementCmr(getBooleanFromCursor(cursor, "etablissement_cmr"));
                fse.setEtablissementUrgent(getBooleanFromCursor(cursor, "etablissement_urgent"));
                fse.setEtablissementRef(getBooleanFromCursor(cursor, "etablissement_ref"));
                fse.setEtablissementEloignement(getBooleanFromCursor(cursor, "etablissement_eloignement"));
                fse.setEtablissementPrecision(getStringFromCursor(cursor, "etablissement_precision"));
                fse.setProfessionnel(getStringFromCursor(cursor, "professionnel"));
                fse.setCodeProfessionnel(getStringFromCursor(cursor, "code_professionnel"));
                fse.setSpeProfessionnel(getStringFromCursor(cursor, "spe_professionnel"));
                fse.setCodeAff1(getStringFromCursor(cursor, "code_aff1"));
                fse.setCodeAff2(getStringFromCursor(cursor, "code_aff2"));
                fse.setInfoMaternite(getBooleanFromCursor(cursor, "info_maternite"));
                fse.setInfoAVP(getBooleanFromCursor(cursor, "info_avp"));
                fse.setInfoATMP(getBooleanFromCursor(cursor, "info_atmp"));
                fse.setInfoAutre(getBooleanFromCursor(cursor, "info_autre"));
                fse.setStatusProgres(getIntFromCursor(cursor, "status_progres"));
                fse.setPreInscription(getBooleanFromCursor(cursor, "pre_inscription"));
                fse.setType_fse(getStringFromCursor(cursor ,"type_fse"));
                fse.setMotif(getStringFromCursor(cursor ,"motif"));
                fse.setNombre_jour(getIntFromCursor(cursor ,"nombre_jour"));
                fse.setCodeAffection(getStringFromCursor(cursor ,"codeAffection"));
                fse.setCode_acte1(getStringFromCursor(cursor,"code_acte1"));
                fse.setCode_acte2(getStringFromCursor(cursor,"code_acte2"));
                fse.setCode_acte3(getStringFromCursor(cursor,"code_acte3"));
                fse.setQuantite_1(getStringFromCursor(cursor,"quantite_1"));
                fse.setQuantite_2(getStringFromCursor(cursor,"quantite_2"));
                fse.setQuantite_3(getStringFromCursor(cursor,"quantite_3"));
                fse.setMontant_acte(getStringFromCursor(cursor,"montant_acte"));
                fse.setPart_cmu(getStringFromCursor(cursor,"part_cmu"));
                fse.setPart_assure(getStringFromCursor(cursor, "part_assure"));

            } else {
                Log.d("getFseAmbulatoireByNumTrans", "Aucun résultat trouvé pour numTrans = " + numTrans);
            }
        } catch (Exception e) {
            Log.e("getFseAmbulatoireByNumTrans", "Erreur lors de la récupération de la FSE : " + e.getMessage(), e);
        } finally {
            // Fermeture du Cursor pour éviter les fuites de mémoire
            if (cursor != null) {
                cursor.close();
            }
        }

        return fse;
    }

    public List<String> getAllNumTrans() {
        List<String> numTransList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            // 1. Initialisation DB
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            if (db == null) {
                Log.e("getAllNumTrans", "Connexion DB null");
                return getDefaultMessage();
            }

            // 2. Requête directe (on suppose que statusProgres existe)
            cursor = db.rawQuery(
                    "SELECT num_trans FROM fse_ambulatoire WHERE statusProgres = 0",
                    null);

            // 3. Traitement résultats
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String numTrans = cursor.getString(cursor.getColumnIndex("num_trans"));
                    if (numTrans != null && !numTrans.isEmpty()) {
                        numTransList.add(numTrans);
                    }
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e("getAllNumTrans", "Erreur: " + e.getMessage());
            return getDefaultMessage();
        } finally {
            if (cursor != null) cursor.close();
        }

        return numTransList.isEmpty() ? getDefaultMessage() : numTransList;
    }

    public boolean updateStatusProgres(String numTrans) {
        SQLiteDatabase db = null;
        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            if (db == null) return false;

            db.execSQL(
                    "UPDATE " + TABLE_FSE_AMBULATOIRE +
                            " SET statusProgres = 1" +
                            " WHERE num_trans = ?",
                    new Object[]{numTrans});

            return true;
        } catch (Exception e) {
            Log.e("updateStatusProgres", "Erreur: " + e.getMessage());
            return false;
        }
    }

    private List<String> getDefaultMessage() {
        return Arrays.asList("Aucun numéro de transaction disponible");
    }

    private int getIntFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex != -1) ? cursor.getInt(columnIndex) : 0;
    }

    private String getStringFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex != -1) ? cursor.getString(columnIndex) : "";
    }

    private boolean getBooleanFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex != -1) && (cursor.getInt(columnIndex) == 1);
    }
    /*@SuppressLint("Range")
    public FseAmbulatoire getFseAmbulatoireByNumTrans(String numTrans) {

        if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
            GlobalClass.getInstance().initDatabase("app");
        }
        db = GlobalClass.getInstance().cnxDbAppPrestation;

        Cursor cursor = db.rawQuery("SELECT * FROM fse_ambulatoire WHERE num_trans = ?", new String[]{numTrans});

        FseAmbulatoire fse = null;
        if (cursor.moveToFirst()) {
            fse = new FseAmbulatoire();
            fse.setId(cursor.getInt(cursor.getColumnIndex("id")));
            fse.setNumTrans(cursor.getString(cursor.getColumnIndex("num_trans")));
            fse.setNumSecu(cursor.getString(cursor.getColumnIndex("num_secu")));
            fse.setNumGuid(cursor.getString(cursor.getColumnIndex("num_guid")));
            fse.setNomComplet(cursor.getString(cursor.getColumnIndex("nom_complet")));
            fse.setSexe(cursor.getString(cursor.getColumnIndex("sexe")));
            fse.setDateNaissance(cursor.getString(cursor.getColumnIndex("date_naissance")));
            fse.setNomEtablissement(cursor.getString(cursor.getColumnIndex("nom_etablissement")));
            fse.setCodeEts(cursor.getString(cursor.getColumnIndex("code_ets")));
            fse.setEtablissementCmr(cursor.getInt(cursor.getColumnIndex("etablissement_cmr")) == 1);
            fse.setEtablissementUrgent(cursor.getInt(cursor.getColumnIndex("etablissement_urgent")) == 1);
            fse.setEtablissementRef(cursor.getInt(cursor.getColumnIndex("etablissement_ref")) == 1);
            fse.setEtablissementEloignement(cursor.getInt(cursor.getColumnIndex("etablissement_eloignement")) == 1);
            fse.setEtablissementPrecision(cursor.getString(cursor.getColumnIndex("etablissement_precision")));
            fse.setProfessionnel(cursor.getString(cursor.getColumnIndex("professionnel")));
            fse.setCodeProfessionnel(cursor.getString(cursor.getColumnIndex("code_professionnel")));
            fse.setSpeProfessionnel(cursor.getString(cursor.getColumnIndex("spe_professionnel")));
            fse.setCodeAff1(cursor.getString(cursor.getColumnIndex("code_aff1")));
            fse.setCodeAff2(cursor.getString(cursor.getColumnIndex("code_aff2")));
            fse.setInfoMaternite(cursor.getInt(cursor.getColumnIndex("info_maternite")) == 1);
            fse.setInfoAVP(cursor.getInt(cursor.getColumnIndex("info_avp")) == 1);
            fse.setInfoATMP(cursor.getInt(cursor.getColumnIndex("info_atmp")) == 1);
            fse.setInfoAutre(cursor.getInt(cursor.getColumnIndex("info_autre")) == 1);
            fse.setStatusProgres(cursor.getInt(cursor.getColumnIndex("status_progres")));
            fse.setPreInscription(cursor.getInt(cursor.getColumnIndex("pre_inscription")) == 1);
        }


        return fse;
    }**/


    @SuppressLint("Range")
    public List<FseAmbulatoire> getFseAmbulatoireByNomComplet(String nomComplet) {
        List<FseAmbulatoire> fseList = new ArrayList<>();

        if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
            GlobalClass.getInstance().initDatabase("app");
        }
        db = GlobalClass.getInstance().cnxDbAppPrestation;

        // Exécuter la requête
        Cursor cursor = db.rawQuery("SELECT * FROM fse_ambulatoire WHERE nom_complet = ?", new String[]{nomComplet});

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    FseAmbulatoire fse = new FseAmbulatoire();
                    fse.setId(cursor.getInt(cursor.getColumnIndex("id")));
                    fse.setNumFsInitial(cursor.getString(cursor.getColumnIndex("num_fs_initial")));
                    fse.setNumTrans(cursor.getString(cursor.getColumnIndex("num_trans")));
                    fse.setNumSecu(cursor.getString(cursor.getColumnIndex("num_secu")));
                    fse.setNumGuid(cursor.getString(cursor.getColumnIndex("num_guid")));
                    fse.setNomComplet(cursor.getString(cursor.getColumnIndex("nom_complet")));
                    fse.setSexe(cursor.getString(cursor.getColumnIndex("sexe")));
                    fse.setDateNaissance(cursor.getString(cursor.getColumnIndex("date_naissance")));
                    fse.setNomEtablissement(cursor.getString(cursor.getColumnIndex("nom_etablissement")));
                    fse.setCodeEts(cursor.getString(cursor.getColumnIndex("code_ets")));
                    fse.setEtablissementCmr(cursor.getInt(cursor.getColumnIndex("etablissement_cmr")) == 1);
                    fse.setCode_acte1(getStringFromCursor(cursor,"code_acte1"));
                    fse.setCode_acte2(getStringFromCursor(cursor,"code_acte2"));
                    fse.setCode_acte3(getStringFromCursor(cursor,"code_acte3"));
                    fse.setQuantite_1(getStringFromCursor(cursor,"quantite_1"));
                    fse.setQuantite_2(getStringFromCursor(cursor,"quantite_2"));
                    fse.setQuantite_3(getStringFromCursor(cursor,"quantite_3"));
                    fse.setMontant_acte(getStringFromCursor(cursor,"montant_acte"));
                    fse.setPart_cmu(getStringFromCursor(cursor,"part_cmu"));
                    fse.setPart_assure(getStringFromCursor(cursor, "part_assure"));

                    // Ajouter l'objet à la liste
                    fseList.add(fse);
                } while (cursor.moveToNext()); // Passer à la ligne suivante
            } else {
                Log.d("Info", "Aucune donnée trouvée pour nom_complet : " + nomComplet);
            }
            cursor.close(); // Fermer le Cursor
        } else {
            Log.e("Error", "Le Cursor est null");
        }

        return fseList;
    }


    @SuppressLint("Range")
    public List<FseAmbulatoire> getAllFseAmbulatoire() {
        List<FseAmbulatoire> fseList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {

            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;


            cursor = db.query(TABLE_FSE_AMBULATOIRE, null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    FseAmbulatoire fse = new FseAmbulatoire();
                    fse.setId(cursor.getInt(cursor.getColumnIndex("id")));
                    fse.setNumFsInitial(cursor.getString(cursor.getColumnIndex("num_fs_initial")));
                    fse.setNumTrans(cursor.getString(cursor.getColumnIndex("num_trans")));
                    fse.setNumSecu(cursor.getString(cursor.getColumnIndex("num_secu")));
                    fse.setNumGuid(cursor.getString(cursor.getColumnIndex("num_guid")));
                    fse.setNomComplet(cursor.getString(cursor.getColumnIndex("nom_complet")));
                    fse.setSexe(cursor.getString(cursor.getColumnIndex("sexe")));
                    fse.setDateNaissance(cursor.getString(cursor.getColumnIndex("date_naissance")));
                    fse.setNomEtablissement(cursor.getString(cursor.getColumnIndex("nom_etablissement")));
                    fse.setCodeEts(cursor.getString(cursor.getColumnIndex("code_ets")));
                    fse.setDate_soins(cursor.getString(cursor.getColumnIndex("date_soins")));
                    fse.setEtablissementCmr(cursor.getInt(cursor.getColumnIndex("etablissement_cmr")) == 1);
                    fse.setEtablissementUrgent(cursor.getInt(cursor.getColumnIndex("etablissement_urgent")) == 1);
                    fse.setEtablissementRef(cursor.getInt(cursor.getColumnIndex("etablissement_ref")) == 1);
                    fse.setEtablissementEloignement(cursor.getInt(cursor.getColumnIndex("etablissement_eloignement")) == 1);
                    fse.setEtablissementPrecision(cursor.getString(cursor.getColumnIndex("etablissement_precision")));
                    fse.setProfessionnel(cursor.getString(cursor.getColumnIndex("professionnel")));
                    fse.setCodeProfessionnel(cursor.getString(cursor.getColumnIndex("code_professionnel")));
                    fse.setSpeProfessionnel(cursor.getString(cursor.getColumnIndex("spe_professionnel")));
                    fse.setCodeAff1(cursor.getString(cursor.getColumnIndex("code_aff1")));
                    fse.setCodeAff2(cursor.getString(cursor.getColumnIndex("code_aff2")));
                    fse.setInfoMaternite(cursor.getInt(cursor.getColumnIndex("info_maternite")) == 1);
                    fse.setInfoAVP(cursor.getInt(cursor.getColumnIndex("info_avp")) == 1);
                    fse.setInfoATMP(cursor.getInt(cursor.getColumnIndex("info_atmp")) == 1);
                    fse.setInfoAutre(cursor.getInt(cursor.getColumnIndex("info_autre")) == 1);
                    fse.setStatusProgres(cursor.getInt(cursor.getColumnIndex("statusProgres")));
                    fse.setPreInscription(cursor.getInt(cursor.getColumnIndex("preinscription")) == 1);
                    fse.setCode_acte1(getStringFromCursor(cursor,"code_acte1"));
                    fse.setCode_acte2(getStringFromCursor(cursor,"code_acte2"));
                    fse.setCode_acte3(getStringFromCursor(cursor,"code_acte3"));
                    fse.setQuantite_1(getStringFromCursor(cursor,"quantite_1"));
                    fse.setQuantite_2(getStringFromCursor(cursor,"quantite_2"));
                    fse.setQuantite_3(getStringFromCursor(cursor,"quantite_3"));
                    fse.setMontant_acte(getStringFromCursor(cursor,"montant_acte"));
                    fse.setPart_cmu(getStringFromCursor(cursor,"part_cmu"));
                    fse.setPart_assure(getStringFromCursor(cursor, "part_assure"));

                    fseList.add(fse);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la récupération des FSE ambulatoires", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fseList;
    }


    public int updateFseAmbulatoire(FseAmbulatoire fse) {
        SQLiteDatabase db = null;
        try {

            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            // Préparer les valeurs à mettre à jour
            ContentValues values = new ContentValues();
            values.put("num_trans", fse.getNumTrans());
            values.put("num_secu", fse.getNumSecu());
            values.put("num_guid", fse.getNumGuid());
            // TODO ajuster les valeurs à modifier.

            return db.update(TABLE_FSE_AMBULATOIRE, values, "id = ?", new String[]{String.valueOf(fse.getId())});
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la mise à jour du FSE ambulatoire", e);
            return -1;
        }
    }

    public int updateImageFseAmbulatoire(String guid ,String photo) {
        SQLiteDatabase db = null;
        try {

            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            // Préparer les valeurs à mettre à jour
            ContentValues values = new ContentValues();
            values.put("urlPhoto", photo);

            // TODO ajuster les valeurs à modifier.

            return db.update(TABLE_FSE_AMBULATOIRE, values, "num_guid = ?", new String[]{String.valueOf(guid)});
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la mise à jour du FSE ambulatoire", e);
            return -1;
        }
    }


    public int deleteFseAmbulatoire(int id) {
        SQLiteDatabase db = null;
        try {

            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;


            return db.delete(TABLE_FSE_AMBULATOIRE, "id = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la suppression du FSE ambulatoire", e);
            return -1;
        }
    }


    public List<FseAmbulatoire> getFseAmbulatoireByStatusProgres(int statusProgres) {
        List<FseAmbulatoire> fseList = new ArrayList<>();
        Cursor cursor = null;

        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            cursor = db.query(
                    TABLE_FSE_AMBULATOIRE,
                    null,
                    "statusProgres = ?",
                    new String[]{String.valueOf(statusProgres)},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    FseAmbulatoire fse = new FseAmbulatoire();
                    fse.setId(cursor.getInt(cursor.getColumnIndex("id")));
                    fse.setNumTrans(cursor.getString(cursor.getColumnIndex("num_trans")));
                    fse.setNumSecu(cursor.getString(cursor.getColumnIndex("num_secu")));
                    fse.setNumGuid(cursor.getString(cursor.getColumnIndex("num_guid")));
                    fse.setNomComplet(cursor.getString(cursor.getColumnIndex("nom_complet")));
                    fse.setSexe(cursor.getString(cursor.getColumnIndex("sexe")));
                    fse.setDateNaissance(cursor.getString(cursor.getColumnIndex("date_naissance")));
                    fse.setNomEtablissement(cursor.getString(cursor.getColumnIndex("nom_etablissement")));
                    fse.setDate_soins(cursor.getString(cursor.getColumnIndex("date_soins")));
                    fse.setCodeEts(cursor.getString(cursor.getColumnIndex("code_ets")));
                    fse.setEtablissementCmr(cursor.getInt(cursor.getColumnIndex("etablissement_cmr")) == 1);
                    fse.setEtablissementUrgent(cursor.getInt(cursor.getColumnIndex("etablissement_urgent")) == 1);
                    fse.setEtablissementRef(cursor.getInt(cursor.getColumnIndex("etablissement_ref")) == 1);
                    fse.setEtablissementEloignement(cursor.getInt(cursor.getColumnIndex("etablissement_eloignement")) == 1);
                    fse.setEtablissementPrecision(cursor.getString(cursor.getColumnIndex("etablissement_precision")));
                    fse.setProfessionnel(cursor.getString(cursor.getColumnIndex("professionnel")));
                    fse.setCodeProfessionnel(cursor.getString(cursor.getColumnIndex("code_professionnel")));
                    fse.setSpeProfessionnel(cursor.getString(cursor.getColumnIndex("spe_professionnel")));
                    fse.setCodeAff1(cursor.getString(cursor.getColumnIndex("code_aff1")));
                    fse.setCodeAff2(cursor.getString(cursor.getColumnIndex("code_aff2")));
                    fse.setInfoMaternite(cursor.getInt(cursor.getColumnIndex("info_maternite")) == 1);
                    fse.setInfoAVP(cursor.getInt(cursor.getColumnIndex("info_avp")) == 1);
                    fse.setInfoATMP(cursor.getInt(cursor.getColumnIndex("info_atmp")) == 1);
                    fse.setInfoAutre(cursor.getInt(cursor.getColumnIndex("info_autre")) == 1);
                    fse.setStatusProgres(cursor.getInt(cursor.getColumnIndex("statusProgres")));
                    fse.setPreInscription(cursor.getInt(cursor.getColumnIndex("preinscription")) == 1);
                    fse.setCode_acte1(getStringFromCursor(cursor,"code_acte1"));
                    fse.setCode_acte2(getStringFromCursor(cursor,"code_acte2"));
                    fse.setCode_acte3(getStringFromCursor(cursor,"code_acte3"));
                    fse.setQuantite_1(getStringFromCursor(cursor,"quantite_1"));
                    fse.setQuantite_2(getStringFromCursor(cursor,"quantite_2"));
                    fse.setQuantite_3(getStringFromCursor(cursor,"quantite_3"));
                    fse.setMontant_acte(getStringFromCursor(cursor,"montant_acte"));
                    fse.setPart_cmu(getStringFromCursor(cursor,"part_cmu"));
                    fse.setPart_assure(getStringFromCursor(cursor, "part_assure"));

                    fseList.add(fse);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la récupération des FSE ambulatoires avec statusProgres = " + statusProgres, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fseList;
    }

    public int countFseAmbulatoireProgresFalse() {
        Cursor cursor = null;
        int count = 0;

        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FSE_AMBULATOIRE + " WHERE statusProgres = ?",
                    new String[]{"0"});

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors du comptage des FSE ambulatoires avec statusProgres false", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    public int countFseAmbulatoireProgresTrue() {
        Cursor cursor = null;
        int count = 0;

        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FSE_AMBULATOIRE + " WHERE statusProgres = ?",
                    new String[]{"1"});

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors du comptage des FSE ambulatoires avec statusProgres false", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    public int countAllFseAmbulatoire() {
        Cursor cursor = null;
        int count = 0;

        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FSE_AMBULATOIRE, null);

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors du comptage de tous les FSE ambulatoires", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    public Map<String, Integer> countFseAmbulatoireByStatus() {
        Cursor cursor = null;
        Map<String, Integer> counts = new HashMap<>();
        counts.put("en_retard", 0);
        counts.put("en_attente", 0);

        try {
            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            cursor = db.rawQuery("SELECT date_soins FROM " + TABLE_FSE_AMBULATOIRE + " WHERE statusProgres = ?",
                    new String[]{"0"});

            if (cursor != null) {
                SimpleDateFormat sdfWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat sdfDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                String currentDate = sdfDateOnly.format(new Date());

                while (cursor.moveToNext()) {
                    @SuppressLint("Range") String dateSoinWithTime = cursor.getString(cursor.getColumnIndex("date_soins"));
                    if (dateSoinWithTime != null) {
                        try {
                            // Parse the full datetime
                            Date dateSoin = sdfWithTime.parse(dateSoinWithTime);
                            // Extract just the date part for comparison
                            String dateSoinDateOnly = sdfDateOnly.format(dateSoin);

                            if (dateSoinDateOnly.equals(currentDate)) {
                                counts.put("en_attente", counts.get("en_attente") + 1);
                            } else {
                                counts.put("en_retard", counts.get("en_retard") + 1);
                            }
                        } catch (ParseException e) {
                            Log.e("FseServiceDb", "Erreur de parsing de la date des soins", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors du comptage des FSE ambulatoires", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return counts;
    }


}