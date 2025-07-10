package ci.technchange.prestationscmu.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

//import ci.technchange.prestationscmu.BuildConfig;

public class dbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_ENROLES_NAME = "newcnambd1.db";
    private static final String DB_REFERENTIELS_NAME = "referentiels.db";
    private static final String DB_APP_NAME = "prestations_fse.db";

    private static final int DATABASE_ENROLES_VERSION = 1;
    //private static final int DATABASE_APP_REFERENTIELS_VERSION = 2;//Version intégrant les codes de création de tables utilisateur et empreintes
    private static final int DATABASE_APP_REFERENTIELS_VERSION = 5;//Version intégrant les codes de création de tables etablissements

    private static String DB_ENROLE_PATH = "";
    //private static final String DB_APP_REFERENTIEL_PATH = "/data/data/" + BuildConfig.APPLICATION_ID + "/databases/";
    private static final String DB_APP_REFERENTIEL_PATH = GlobalClass.getInstance().getFilesDir().getPath() + "/databases/";

    private SQLiteDatabase myDataBase;
    private static Context myContext;


    // Table pour les agents d'inscription (à ajouter après les autres déclarations de tables)
    public static final String TABLE_AGENTS_INSCRIPTION = "agents_inscription";
    public static final String TABLE_CENTRE_SANTE = "centre_sante";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NOM = "nom";
    public static final String COLUMN_PRENOM = "prenom";
    public static final String COLUMN_TELEPHONE = "telephone";
    public static final String COLUMN_MATRICULE = "matricule";
    public static final String COLUMN_CENTRE_SANTE = "centre_sante";
    public static final String COLUMN_PHOTO_PATH = "photo_path";
    public static final String COLUMN_EMPREINTES = "empreintes";
    public static final String COLUMN_PHOTO_FACADE_PATH = "photo_facade_path";
    public static final String COLUMN_PHOTO_INTERIEUR_PATH = "photo_interieur_path";
    public static final String COLUMN_LATITUDE_FACADE = "latitude_facade";
    public static final String COLUMN_LONGITUDE_FACADE = "longitude_facade";
    public static final String COLUMN_LATITUDE_INTERIEUR = "latitude_interieur";
    public static final String COLUMN_LONGITUDE_INTERIEUR = "longitude_interieur";
    public static final String COLUMN_DATE_INSCRIPTION = "date_inscription";

    public static final String TABLE_ETABLISSEMENT = "etablissements";

    String tables_empreintes = "CREATE TABLE IF NOT EXISTS empreintes ( " +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "matricule TEXT NOT NULL, " +
            "main TEXT NOT NULL, " +
            "user_id INTEGER, " +
            "doigt TEXT NOT NULL, " +
            "template BLOB NOT NULL, " +
            "date_enregistrement TEXT, " +
            "rawTemplate BLOB)";

    String table_etablissements = "CREATE TABLE IF NOT EXISTS " + TABLE_ETABLISSEMENT + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "code_ets TEXT, " +
            "etablissement TEXT, " +
            "id_region TEXT, " +
            "ville TEXT, " +
            "region TEXT, " +
            "district_sanitaire TEXT)";
    String table_fse_ambulatoire = "CREATE TABLE IF NOT EXISTS fse_ambulatoire (" +
            "        id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "        num_fs_initial TEXT ," +
            "        num_trans TEXT NOT NULL," +
            "        num_secu TEXT NOT NULL," +
            "        num_guid TEXT," +
            "        nom_complet TEXT NOT NULL," +
            "        sexe TEXT NOT NULL," +
            "        date_naissance TEXT NOT NULL," +
            "        nom_etablissement TEXT NOT NULL," +
            "        code_ets TEXT," +
            "        etablissement_cmr INTEGER NOT NULL DEFAULT 0," +
            "        etablissement_urgent INTEGER NOT NULL DEFAULT 0," +
            "        etablissement_ref INTEGER NOT NULL DEFAULT 0," +
            "        etablissement_eloignement INTEGER NOT NULL DEFAULT 0," +
            "        etablissement_precision TEXT," +
            "        professionnel TEXT," +
            "        code_acte1 TEXT,"+
            "        code_acte2 TEXT,"+
            "        code_acte3 TEXT,"+
            "        quantite_1 TEXT,"+
            "        quantite_2 TEXT,"+
            "        quantite_3 TEXT,"+
            "        montant_acte TEXT,"+
            "        part_cmu TEXT,"+
            "        part_assure TEXT,"+
            "        code_professionnel TEXT," +
            "        spe_professionnel TEXT," +
            "        date_soins TEXT NOT NULL,"+
            "        code_aff1 TEXT," +
            "        code_aff2 TEXT," +
            "        type_fse TEXT," +
            "        statusEnvoie INTEGER NOT NULL DEFAULT 0," +
            "        date_synchro TEXT," +
            "        codeAffection TEXT," +
            "        motif TEXT," +
            "        nombre_jour INTEGER NOT NULL DEFAULT 0," +
            "        info_maternite INTEGER NOT NULL DEFAULT 0," +
            "        info_avp INTEGER NOT NULL DEFAULT 0," +
            "        info_atmp INTEGER NOT NULL DEFAULT 0," +
            "        info_autre INTEGER NOT NULL DEFAULT 0," +
            "        statusProgres INTEGER NOT NULL DEFAULT 0," +
            "        preinscription INTEGER NOT NULL DEFAULT 0," +
            "        urlPhoto TEXT" +

            ");";
    String table_prestation_ambulatoire = "CREATE TABLE IF NOT EXISTS prestation_ambulatoire ("+
            "        id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "        code TEXT NOT NULL,"+
            "        libelle TEXT NOT NULL,"+
            "        titre TEXT NOT NULL,"+
            "        tarif INTEGER NOT NULL,"+
            "        fse_id INTEGER NOT NULL,"+
            "        FOREIGN KEY (fse_id) REFERENCES fse_ambulatoire (id)"+
            ");";
    String table_fse_hospi = "CREATE TABLE IF NOT EXISTS fse_hospi("+
            "        id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "        num_trans TEXT NOT NULL,"+
            "        num_secu TEXT NOT NULL,"+
            "        num_guid TEXT,"+
            "        nom_complet TEXT NOT NULL,"+
            "        sexe TEXT NOT NULL,"+
            "        date_naissance TEXT NOT NULL,"+
            "        nom_etablissement TEXT NOT NULL,"+
            "        code_ets TEXT,"+
            "        etablissement_cmr INTEGER NOT NULL DEFAULT 0,"+
            "        etablissement_urgent INTEGER NOT NULL DEFAULT 0,"+
            "        etablissement_ref INTEGER NOT NULL DEFAULT 0,"+
            "        etablissement_eloignement INTEGER NOT NULL DEFAULT 0,"+
            "       etablissement_precision TEXT,"+
            "      professionnel TEXT,"+
            "        code_professionnel TEXT,"+
            "        spe_professionnel TEXT,"+
            "        info_maternite INTEGER NOT NULL DEFAULT 0,"+
            "        info_avp INTEGER NOT NULL DEFAULT 0,"+
            "        info_atmp INTEGER NOT NULL DEFAULT 0,"+
            "        info_autre INTEGER NOT NULL DEFAULT 0,"+
            "        code_affection TEXT NOT NULL,"+
            "        nombre_jour INTEGER NOT NULL,"+
            "        motif TEXT,"+
            "        statusProgres INTEGER NOT NULL DEFAULT 0"+
            ");";
    String table_prestation_hospi = "CREATE TABLE IF NOT EXISTS prestation_hospi ("+
            "        id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "        code TEXT NOT NULL,"+
            "        libelle TEXT NOT NULL,"+
            "        titre TEXT NOT NULL,"+
            "        tarif INTEGER NOT NULL,"+
            "        fse_id INTEGER NOT NULL,"+
            "        FOREIGN KEY (fse_id) REFERENCES fse_hospi (id)"+
            ");";
    String table_fse_prothese_dentaire = "CREATE TABLE IF NOT EXISTS fse_prothese_dentaire("+
            "        id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "       num_trans TEXT NOT NULL,"+
            "num_secu TEXT NOT NULL,"+
            "num_guid TEXT,"+
            "nom_complet TEXT NOT NULL,"+
            "sexe TEXT NOT NULL,"+
            "date_naissance TEXT NOT NULL,"+
            "nom_etablissement TEXT NOT NULL,"+
            "professionnel TEXT,"+
            "code_professionnel TEXT,"+
            "spe_professionnel TEXT,"+
            "code_aff1 TEXT,"+
            "code_aff2 TEXT,"+
            " info_maternite INTEGER NOT NULL DEFAULT 0,"+
            "info_avp INTEGER NOT NULL DEFAULT 0,"+
            "info_atmp INTEGER NOT NULL DEFAULT 0,"+
            "info_autre INTEGER NOT NULL DEFAULT 0,"+
            "statusProgres INTEGER NOT NULL DEFAULT 0"+
            ");";
    String table_prestation_prothese_dentaire = "CREATE TABLE IF NOT EXISTS table_prestation_prothese_dentaire ("+
            "        id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "        code TEXT NOT NULL,"+
            "        libelle TEXT NOT NULL,"+
            "        titre TEXT NOT NULL,"+
            "        tarif INTEGER NOT NULL,"+
            "        fse_id INTEGER NOT NULL,"+
            "        FOREIGN KEY (fse_id) REFERENCES fse_prothese_dentaire (id)"+
            ");";

    String table_fse_biologie_imagerie = "CREATE TABLE IF NOT EXISTS biologie_imagerie("+
            "        id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "        num_trans TEXT NOT NULL,"+
            "        num_ogd TEXT NOT NULL,"+
            "        num_secu TEXT NOT NULL,"+
            "        num_guid TEXT,"+
            "        nom_complet TEXT NOT NULL,"+
            " sexe TEXT NOT NULL,"+
            "date_naissance TEXT NOT NULL,"+
            "nom_etablissement TEXT NOT NULL,"+
            "code_ets TEXT,"+
            "professionnel TEXT,"+
            "code_professionnel TEXT,"+
            "spe_professionnel TEXT,"+
            "code_affection TEXT NOT NULL,"+
            "statusProgres INTEGER NOT NULL DEFAULT 0"+
            ");";
    String table_prestation_biologie_imagerie = "CREATE TABLE IF NOT EXISTS prestation_biologie_imagerie ("+
            "        id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "        code TEXT NOT NULL,"+
            "        libelle TEXT NOT NULL,"+
            "        titre TEXT NOT NULL,"+
            "        tarif INTEGER NOT NULL,"+
            "        fse_id INTEGER NOT NULL,"+
            "        FOREIGN KEY (fse_id) REFERENCES biologie_imagerie (id)"+
            ");";

    String table_version_db = "CREATE TABLE IF NOT EXISTS version_bd ("+
            "        id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "        numero_version NOT NULL DEFAULT 0,"+
            "        numero_version_referentiel NOT NULL DEFAULT 0"+
            ");";


    String table_utilisateurs = "CREATE TABLE IF NOT EXISTS utilisateurs (" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    phoneNumber TEXT," +
            "    nom TEXT NOT NULL," +
            "    prenom TEXT NOT NULL," +
            "    date_naissance TEXT NOT NULL," +
            "    photo BLOB," +
            "    empreinte_digitale BLOB," +
            "    centre_sante_code TEXT," +
            "    date_inscription TEXT NOT NULL," +
            "    statut INTEGER DEFAULT 1" + // 1 pour actif, 0 pour inactif
            ");";
    String table_metrique = "CREATE TABLE IF NOT EXISTS metrique_utilisation("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "activite TEXT NOT NULL,"+
            "date_debut TEXT NOT NULL,"+
            "date_fin TEXT NOT NULL,"+
            "id_famoco TEXT NOT NULL,"+
            "id_region INTEGER NOT NULL,"+
            "status_synchro INTERGER DEFAULT 0"+
            ");";

    String table_metrique_connexion = "CREATE TABLE IF NOT EXISTS metrique_connexion("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "code_ets TEXT NOT NULL,"+
            "code_agac TEXT NOT NULL,"+
            "nom_complet TEXT NOT NULL,"+
            "date_connexion TEXT NOT NULL,"+
            "heure_connexion TEXT NOT NULL,"+
            "id_region INTEGER NOT NULL,"+
            "status_synchro INTERGER DEFAULT 0"+
            ");";

    String table_parametre = "CREATE TABLE IF NOT EXISTS parametre_fammoco("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "etablissement_check INTERGER DEFAULT 0,"+
            "famoco_check INTERGER DEFAULT 0,"+
            "photo_check INTERGER DEFAULT 0"+
            ");";

    String table_user_connecter = "CREATE TABLE IF NOT EXISTS user_connecter("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "code_agac TEXT,"+
            "nom_complet TEXT"+
            ");";
    String table_autre_fsp= "CREATE TABLE IF NOT EXISTS autre_fsp("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "numTransaction TEXT,"+
            "photo_url TEXT"+
            ");";
    // Dans la méthode createDataBaseApp(), ajoutez ceci avec les autres créations de tables
    String table_agents_inscription = "CREATE TABLE IF NOT EXISTS " + TABLE_AGENTS_INSCRIPTION + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NOM + " TEXT NOT NULL, " +
            COLUMN_PRENOM + " TEXT NOT NULL, " +
            COLUMN_TELEPHONE + " TEXT NOT NULL, " +
            COLUMN_MATRICULE + " TEXT NOT NULL, " +
            COLUMN_CENTRE_SANTE + " TEXT NOT NULL, " +
            COLUMN_PHOTO_PATH + " TEXT, " +
            COLUMN_EMPREINTES + " INTEGER, " +
            COLUMN_PHOTO_FACADE_PATH + " TEXT, " +
            COLUMN_PHOTO_INTERIEUR_PATH + " TEXT, " +
            COLUMN_LATITUDE_FACADE + " REAL, " +
            COLUMN_LONGITUDE_FACADE + " REAL, " +
            COLUMN_LATITUDE_INTERIEUR + " REAL, " +
            COLUMN_LONGITUDE_INTERIEUR + " REAL, " +
            COLUMN_DATE_INSCRIPTION + " TEXT)";

    String table_centre_sante = "CREATE TABLE IF NOT EXISTS " + TABLE_CENTRE_SANTE + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_CENTRE_SANTE + " TEXT NOT NULL, " +
            COLUMN_PHOTO_FACADE_PATH + " TEXT, " +
            COLUMN_PHOTO_INTERIEUR_PATH + " TEXT, " +
            COLUMN_LATITUDE_FACADE + " REAL, " +
            COLUMN_LONGITUDE_FACADE + " REAL, " +
            COLUMN_LATITUDE_INTERIEUR + " REAL, " +
            COLUMN_LONGITUDE_INTERIEUR + " REAL, " +
            COLUMN_DATE_INSCRIPTION + " TEXT)";

    public dbHelper(Context context) {
        super(context, DB_APP_NAME, null, DATABASE_APP_REFERENTIELS_VERSION);
        dbHelper.myContext = context;
        //DB_PATH = myContext.getDatabasePath(DATABASE_NAME).toString();
        //File externalDir = new File(context.getFilesDir(),"encryptedbd");
        File externalDir = new File(GlobalClass.getInstance().getFilesDir().getPath(),"encryptedbd");
        File file = new File(externalDir, "newcnambd1.db");
        //deleteDatabaseApp();
        DB_ENROLE_PATH = file.getPath();
        //DB_PATH = file.getAbsolutePath();
    }

    @Override
    public synchronized void close() {
        if (myDataBase != null) {
            myDataBase.close();
        }
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // La table a déjà été créée dans l'autre application
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Liste des tables et des colonnes à vérifier
        List<String> tableQueries = new ArrayList<>();
        tableQueries.add(table_fse_ambulatoire);
        tableQueries.add(table_prestation_ambulatoire);
        tableQueries.add(table_fse_hospi);
        tableQueries.add(table_prestation_hospi);
        tableQueries.add(table_fse_prothese_dentaire);
        tableQueries.add(table_prestation_prothese_dentaire);
        tableQueries.add(table_fse_biologie_imagerie);
        tableQueries.add(table_prestation_biologie_imagerie);
        tableQueries.add(table_version_db);
        tableQueries.add(table_utilisateurs);
        tableQueries.add(table_metrique);
        tableQueries.add(table_metrique_connexion);
        tableQueries.add(table_parametre);
        tableQueries.add(table_user_connecter);
        tableQueries.add(table_autre_fsp);
        tableQueries.add(table_agents_inscription);
        tableQueries.add(table_centre_sante);
        tableQueries.add(tables_empreintes);


        // Vérification de chaque table et création si nécessaire
        for (String tableQuery : tableQueries) {
            try {
                // Vérification si la table existe
                String checkTableQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
                Cursor cursor = db.rawQuery(checkTableQuery, new String[] { getTableName(tableQuery) });
                if (!cursor.moveToFirst()) {
                    // La table n'existe pas, on la crée
                    db.execSQL(tableQuery);
                }
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //BAse de données referentiel
        SQLiteDatabase db1 = SQLiteDatabase.openDatabase(DB_APP_REFERENTIEL_PATH + DB_REFERENTIELS_NAME, null, SQLiteDatabase.OPEN_READWRITE);
        try {
            // Vérification si la table existe
            String checkTableQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
            Cursor cursor = db1.rawQuery(checkTableQuery, new String[] { getTableName(table_etablissements) });
            if (!cursor.moveToFirst()) {
                // La table n'existe pas, on la crée
                db1.execSQL(table_etablissements);
                executeSqlFile(db1, "liste_etablissements_202505061405.sql", myContext);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        checkAndAddColumns(db, "empreintes", new String[]{
                "id", "matricule", "main", "user_id",
                "doigt", "template", "date_enregistrement", "rawTemplate"
        });

        checkAndAddColumns(db, "fse_ambulatoire", new String[]{
                "id", "num_fs_initial", "num_trans", "num_secu",
                "num_guid", "nom_complet", "sexe", "date_naissance",
                "nom_etablissement", "code_ets", "etablissement_cmr",
                "etablissement_urgent", "etablissement_ref", "etablissement_eloignement",
                "etablissement_precision", "professionnel", "code_acte1",
                "code_acte2", "code_acte3", "quantite_1", "quantite_2",
                "quantite_3", "montant_acte", "part_cmu", "part_assure",
                "code_professionnel", "spe_professionnel", "date_soins",
                "code_aff1", "code_aff2", "type_fse", "statusEnvoie",
                "date_synchro", "codeAffection", "motif", "nombre_jour",
                "info_maternite", "info_avp", "info_atmp", "info_autre",
                "statusProgres", "preinscription", "urlPhoto"
        });

        checkAndAddColumns(db, "prestation_ambulatoire", new String[]{
                "id", "code", "libelle", "titre", "tarif", "fse_id"
        });

        checkAndAddColumns(db, "fse_hospi", new String[]{
                "id", "num_trans", "num_secu", "num_guid",
                "nom_complet", "sexe", "date_naissance", "nom_etablissement",
                "code_ets", "etablissement_cmr", "etablissement_urgent",
                "etablissement_ref", "etablissement_eloignement", "etablissement_precision",
                "professionnel", "code_professionnel", "spe_professionnel",
                "info_maternite", "info_avp", "info_atmp", "info_autre",
                "code_affection", "nombre_jour", "motif", "statusProgres"
        });

        checkAndAddColumns(db, "prestation_hospi", new String[]{
                "id", "code", "libelle", "titre", "tarif", "fse_id"
        });

        checkAndAddColumns(db, "fse_prothese_dentaire", new String[]{
                "id", "num_trans", "num_secu", "num_guid",
                "nom_complet", "sexe", "date_naissance", "nom_etablissement",
                "professionnel", "code_professionnel", "spe_professionnel",
                "code_aff1", "code_aff2", "info_maternite", "info_avp",
                "info_atmp", "info_autre", "statusProgres"
        });

        checkAndAddColumns(db, "table_prestation_prothese_dentaire", new String[]{
                "id", "code", "libelle", "titre", "tarif", "fse_id"
        });

        checkAndAddColumns(db, "biologie_imagerie", new String[]{
                "id", "num_trans", "num_ogd", "num_secu", "num_guid",
                "nom_complet", "sexe", "date_naissance", "nom_etablissement",
                "code_ets", "professionnel", "code_professionnel",
                "spe_professionnel", "code_affection", "statusProgres"
        });

        checkAndAddColumns(db, "prestation_biologie_imagerie", new String[]{
                "id", "code", "libelle", "titre", "tarif", "fse_id"
        });

        checkAndAddColumns(db, "version_bd", new String[]{
                "id", "numero_version_referentiel", "numero_version"
        });

        checkAndAddColumns(db, "utilisateurs", new String[]{
                "id", "phoneNumber", "nom", "prenom", "date_naissance",
                "photo", "empreinte_digitale", "centre_sante_code",
                "date_inscription", "statut"
        });

        checkAndAddColumns(db, "metrique_utilisation", new String[]{
                "id", "activite", "date_debut", "date_fin",
                "id_famoco", "id_region", "status_synchro"
        });
        checkAndAddColumns(db, "metrique_connexion", new String[]{
                "id", "code_ets", "code_agac", "nom_complet",
                "date_connexion", "heure_connexion", "id_region", "status_synchro"
        });

        checkAndAddColumns(db, "parametre_fammoco", new String[]{
                "id", "etablissement_check", "famoco_check", "photo_check"
        });

        checkAndAddColumns(db, "user_connecter", new String[]{
                "id", "code_agac", "nom_complet"
        });

        checkAndAddColumns(db, "autre_fsp", new String[]{
                "id", "numTransaction", "photo_url"
        });

        checkAndAddColumns(db, "agents_inscription", new String[]{
                "id", "nom", "prenom", "telephone", "matricule",
                "centre_sante", "photo_path", "empreintes", "photo_facade_path",
                "photo_interieur_path", "latitude_facade", "longitude_facade",
                "latitude_interieur", "longitude_interieur", "date_inscription"
        });

        checkAndAddColumns(db, "centre_sante", new String[]{
                "id", "centre_sante", "photo_facade_path", "photo_interieur_path",
                "latitude_facade", "longitude_facade", "latitude_interieur",
                "longitude_interieur", "date_inscription"
        });

        // Vérification des colonnes pour chaque table
        //checkAndAddColumns(db, "fse_ambulatoire", new String[]{"colonne1", "colonne2"});
        //checkAndAddColumns(db, "prestation_ambulatoire", new String[]{"colonne1", "colonne2"});



        checkAndAddColumns(db1, "etablissements", new String[]{
                "id", "code_ets", "etablissement", "id_region",
                "ville", "region", "district_sanitaire"
        });

        // Ajoutez la même logique pour les autres tables

    }

    private void checkAndAddColumns(SQLiteDatabase db, String tableName, String[] columns) {
        // Vérifier la structure des colonnes actuelles de la table
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        Set<String> existingColumns = new HashSet<>();
        while (cursor.moveToNext()) {
            existingColumns.add(cursor.getString(cursor.getColumnIndex("name")));
        }
        cursor.close();

        // Ajouter les colonnes manquantes
        for (String column : columns) {
            if (!existingColumns.contains(column)) {
                // Syntaxe pour ajouter une nouvelle colonne
                String addColumnQuery = "ALTER TABLE " + tableName + " ADD COLUMN " + column + " TEXT"; // ou INTEGER, REAL en fonction du type
                db.execSQL(addColumnQuery);
            }
        }
    }
    // Fonction utilitaire pour extraire le nom de la table depuis la requête de création
    private String getTableName(String createTableQuery) {
        String[] parts = createTableQuery.split(" ");
        return parts[2]; // Assurez-vous que la table est à la position correcte dans votre requête
    }

    private void executeSqlFile(SQLiteDatabase db, String filename, Context context) throws IOException {
        // Lire le fichier SQL à partir du dossier assets
        BufferedReader reader = null;
        try {
            // Ouvrir le fichier
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));
            String line;
            StringBuilder sqlScript = new StringBuilder();

            // Lire chaque ligne du fichier et accumuler les commandes SQL
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignorer les lignes vides ou les commentaires
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }

                // Ajouter la ligne à la commande SQL
                sqlScript.append(line).append(" ");

                // Si la ligne se termine par un point-virgule, c'est une requête complète
                if (line.endsWith(";")) {
                    // Exécuter la requête SQL
                    db.execSQL(sqlScript.toString());
                    sqlScript.setLength(0);  // Réinitialiser pour la prochaine requête
                }
            }
        } catch (SQLException e) {
            throw new IOException("Erreur d'exécution des requêtes SQL dans le fichier", e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public boolean deleteDatabaseApp(){
        File fichier = new File(GlobalClass.getInstance().getFilesDir().getPath(), "databases/"+DB_APP_NAME);
        if (fichier.exists()) {
            return fichier.delete(); // Supprime le fichier
        }
        return false;

    }
    public boolean checkDataBasesReferentiel() {
        SQLiteDatabase checkDB1=null,checkDB2 = null;
        boolean ok1=false, ok2=false;
        try {
            File dbFile = new File(DB_APP_REFERENTIEL_PATH + DB_REFERENTIELS_NAME);
            // Vérifiez si la base de données existe déjà, si oui, ne pas la copier à nouveau
            if (!dbFile.exists()) {
                System.out.println("la base de données n'existe pas; tentative de copie...");
                ok2 = copyDataBaseReferentiel();
            }
            if(dbFile.exists()) {// Donc meme si ca n'existait pas, ca existe après la copie effectuée par l'appel de la ligne précédente
                System.out.println("la base de données "+DB_APP_REFERENTIEL_PATH + DB_REFERENTIELS_NAME+" existe; tentative de connexion...");
                checkDB2 = SQLiteDatabase.openDatabase(DB_APP_REFERENTIEL_PATH + DB_REFERENTIELS_NAME, null, SQLiteDatabase.OPEN_READONLY);
                if (checkDB2 != null) {
                    System.out.println("test de connexion à la base de données "+DB_APP_REFERENTIEL_PATH + DB_REFERENTIELS_NAME+"  ->>>>>OK");
                    checkDB2.close();
                    ok2=true;
                }
            }
            if(ok2==true) return true;
            return false;
        } catch (SQLiteException e) {
            // La base de données n'existe pas encore
            System.out.println("la base de données n'existe pas");
            return false;
        }
    }
    public boolean checkDataBasesApp() {
        boolean ok=false;
        //checkDB = SQLiteDatabase.openDatabase(DB_APP_REFERENTIEL_PATH+DB_APP_NAME, null, SQLiteDatabase.OPEN_READONLY);
        SQLiteDatabase checkDB=null;
        try {
            File dbFile = new File(DB_APP_REFERENTIEL_PATH + DB_APP_NAME);
            // Vérifiez si la base de données existe déjà, si oui, ne pas la copier à nouveau
            if (!dbFile.exists()) {
                System.out.println("la base de données n'existe pas; tentative de copie...");
                ok = createDataBaseApp();
            }
            if(dbFile.exists()) {// Donc meme si ca n'existait pas, ca existe après la copie effectuée par l'appel de la ligne précédente
                System.out.println("la base de données "+DB_APP_NAME+" existe; tentative de connexion...");
                checkDB = SQLiteDatabase.openDatabase(DB_APP_REFERENTIEL_PATH + DB_APP_NAME, null, SQLiteDatabase.OPEN_READWRITE);
                if (checkDB != null) {
                    System.out.println("test de connexion à la base de données "+DB_APP_REFERENTIEL_PATH + DB_APP_NAME+"  ->>>>>OK");
                    checkDB.close();
                    ok=true;
                }
            }
            if(ok==true) return true;
            return false;
        } catch (SQLiteException e) {
            // La base de données n'existe pas encore
            System.out.println("la base de données n'existe pas");
            return false;
        }
    }

    public boolean copyDataBaseReferentiel() {
        System.out.println("Dans la fonction de copy de base de donnée");
        File dbFile = new File(DB_APP_REFERENTIEL_PATH + DB_REFERENTIELS_NAME);
        // Vérifiez si la base de données existe déjà, si oui, ne pas la copier à nouveau
        if (!dbFile.exists()) {
            try {
                File dir = new File(DB_APP_REFERENTIEL_PATH);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                InputStream inputStream = this.myContext.getAssets().open(DB_REFERENTIELS_NAME);
                OutputStream outputStream = new FileOutputStream(dbFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();
                Log.d("Database", "Base de données"+DB_REFERENTIELS_NAME+" copiée avec succès !");
                return true;
            } catch (IOException e) {
                Log.e("Database", "Erreur lors de la copie de la base de données "+DB_REFERENTIELS_NAME, e);
                return false;
            }
        } else {
            Log.d("Database", "La base de données existe déjà");
            return true;
        }
    }

    public boolean copyDataBaseApp() {
        System.out.println("Dans la fonction de copy de base de donnée");
        File dbFile = new File(DB_APP_REFERENTIEL_PATH + DB_APP_NAME);
        // Vérifiez si la base de données existe déjà, si oui, ne pas la copier à nouveau
        if (!dbFile.exists()) {
            try {
                File dir = new File(DB_APP_REFERENTIEL_PATH);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                InputStream inputStream = this.myContext.getAssets().open(DB_APP_NAME);
                OutputStream outputStream = new FileOutputStream(dbFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();
                Log.d("Database", "Base de données"+DB_APP_NAME+" copiée avec succès !");
                return true;
            } catch (IOException e) {
                Log.e("Database", "Erreur lors de la copie de la base de données "+DB_APP_NAME, e);
                return false;
            }
        } else {
            Log.d("Database", "La base de données existe déjà");
            return true;
        }
    }

    public SQLiteDatabase openDataBase(String path, String name, int mode) throws SQLiteException {
        //if(checkDataBase()) {
        // myDataBase = SQLiteDatabase.openDatabase(DB_ENROLE_PATH, null, SQLiteDatabase.OPEN_READWRITE);
        myDataBase = SQLiteDatabase.openDatabase(path + name, null, mode);
        //}
        return myDataBase;
    }

    public boolean createDataBaseApp()  {
        try {
            copyDataBaseApp();
            SQLiteDatabase db = SQLiteDatabase.openDatabase(DB_APP_REFERENTIEL_PATH + DB_APP_NAME, null, SQLiteDatabase.OPEN_READWRITE);
            SQLiteDatabase db1 = SQLiteDatabase.openDatabase(DB_APP_REFERENTIEL_PATH + DB_REFERENTIELS_NAME, null, SQLiteDatabase.OPEN_READWRITE);



            System.out.println("La base de données n'existe pas");
            this.getReadableDatabase();
            this.close();

            System.out.println("Creation des tables de la base de données de l'application");
            db.execSQL(table_fse_ambulatoire);
            db.execSQL(table_prestation_ambulatoire);
            db.execSQL(table_fse_hospi);
            db.execSQL(table_prestation_hospi);
            db.execSQL(table_fse_prothese_dentaire);
            db.execSQL(table_prestation_prothese_dentaire);
            db.execSQL(table_fse_biologie_imagerie);
            db.execSQL(table_prestation_biologie_imagerie);
            db.execSQL(table_version_db);
            db.execSQL(table_utilisateurs);
            db.execSQL(table_metrique);
            db.execSQL(table_metrique_connexion);
            db.execSQL(table_parametre);
            db.execSQL(table_user_connecter);
            db.execSQL(table_autre_fsp);
            db.execSQL(table_agents_inscription);
            db.execSQL(table_centre_sante);
            db1.execSQL(table_etablissements);
            db1.execSQL(tables_empreintes);


            String insertVersion = "INSERT INTO version_bd (numero_version,numero_version_referentiel) VALUES (0,0);";
            String insertParametreInitial = "INSERT INTO parametre_fammoco (etablissement_check, famoco_check,photo_check) VALUES (0,0,0);";
            db.execSQL(insertVersion);
            db.execSQL(insertParametreInitial);
            db.execSQL(table_agents_inscription);

            try {
                executeSqlFile(db1, "liste_etablissements_202505061405.sql", myContext);
            } catch (IOException e) {
                Log.e("SQLite", "Erreur lors de l'exécution du fichier SQL", e);
            }
            System.out.println("Fin de Tentative de copy");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Méthode pour insérer un nouvel agent d'inscription
    public long insertAgentInscription(String nom, String prenom, String telephone, String matricule,
                                       String centreSante, String photoPath, boolean empreintes,
                                       String photoFacadePath, String photoInterieurPath,
                                       double latitudeFacade, double longitudeFacade,
                                       double latitudeInterieur, double longitudeInterieur) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NOM, nom);
        values.put(COLUMN_PRENOM, prenom);
        values.put(COLUMN_TELEPHONE, telephone);
        values.put(COLUMN_MATRICULE, matricule);
        values.put(COLUMN_CENTRE_SANTE, centreSante);
        values.put(COLUMN_PHOTO_PATH, photoPath);
        values.put(COLUMN_EMPREINTES, empreintes ? 1 : 0);
        values.put(COLUMN_PHOTO_FACADE_PATH, photoFacadePath);
        values.put(COLUMN_PHOTO_INTERIEUR_PATH, photoInterieurPath);
        values.put(COLUMN_LATITUDE_FACADE, latitudeFacade);
        values.put(COLUMN_LONGITUDE_FACADE, longitudeFacade);
        values.put(COLUMN_LATITUDE_INTERIEUR, latitudeInterieur);
        values.put(COLUMN_LONGITUDE_INTERIEUR, longitudeInterieur);

        // Ajouter la date d'inscription
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());
        values.put(COLUMN_DATE_INSCRIPTION, currentDateTime);

        // Insérer la ligne dans la base de données
        return db.insert(TABLE_AGENTS_INSCRIPTION, null, values);
    }

    // Méthode pour obtenir tous les agents d'inscription
    public Cursor getAllAgentsInscription() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_AGENTS_INSCRIPTION, null, null, null, null, null, COLUMN_DATE_INSCRIPTION + " DESC");
    }

    // Méthode pour obtenir un agent par son ID
    public Cursor getAgentInscriptionById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_AGENTS_INSCRIPTION, null, COLUMN_ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
    }

    // Méthode pour supprimer un agent d'inscription
    public int deleteAgentInscription(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_AGENTS_INSCRIPTION, COLUMN_ID + "=?", new String[] { String.valueOf(id) });
    }
}
