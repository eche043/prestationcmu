package ci.technchange.prestationscmu.core;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;

public class GlobalClass extends Application {
    private static GlobalClass mInstance;

    public SQLiteDatabase cnxDbEnrole;
    public SQLiteDatabase cnxDbReferentiel;
    public SQLiteDatabase cnxDbAppPrestation;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public void initDatabase(String mode) {
        System.out.println("Tentative de connexion au bases de données");
        dbHelper databaseHelper = new dbHelper(this);

        try {
            if(mode=="all") {
                this.cnxDbEnrole = databaseHelper.openDataBase(GlobalClass.mInstance.getFilesDir().getPath() + "/encryptedbd/", "newcnambd1.db", SQLiteDatabase.OPEN_READONLY);
                this.cnxDbAppPrestation = databaseHelper.openDataBase(GlobalClass.mInstance.getFilesDir().getPath() + "/databases/", "prestations_fse.db", SQLiteDatabase.OPEN_READWRITE);
                this.cnxDbReferentiel = databaseHelper.openDataBase(GlobalClass.mInstance.getFilesDir().getPath() + "/databases/", "referentiels.db", SQLiteDatabase.OPEN_READONLY);
                System.out.println("Connexion effectiées avec succès");
            }else if(mode=="enrole"){
                this.cnxDbEnrole = databaseHelper.openDataBase(GlobalClass.mInstance.getFilesDir().getPath() + "/encryptedbd/", "newcnambd1.db", SQLiteDatabase.OPEN_READWRITE);
            }else if(mode=="referentiel"){
                this.cnxDbReferentiel = databaseHelper.openDataBase(GlobalClass.mInstance.getFilesDir().getPath() + "/databases/", "referentiels.db", SQLiteDatabase.OPEN_READWRITE);
            }else if(mode=="app"){
                this.cnxDbAppPrestation = databaseHelper.openDataBase(GlobalClass.mInstance.getFilesDir().getPath() + "/databases/", "prestations_fse.db", SQLiteDatabase.OPEN_READWRITE);
            }
        } catch (SQLiteException e) {
            System.out.println("DB error: " + e.getMessage());
        } catch (Exception e){
            System.out.println("Tentative de connexion au bases de données: ERREUR CAPTUREE");
            e.printStackTrace();
        }
    }

    public static synchronized GlobalClass getInstance(){
        return mInstance;
    }

}
