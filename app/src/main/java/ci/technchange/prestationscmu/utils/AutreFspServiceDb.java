package ci.technchange.prestationscmu.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.models.AutreFse;
import ci.technchange.prestationscmu.models.FseAmbulatoire;

public class AutreFspServiceDb {
    private static AutreFspServiceDb instance;
    private final Context context;
    SQLiteDatabase db;
    private static final String TABLE_FSP_AUTRE = "autre_fsp";

    public AutreFspServiceDb(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AutreFspServiceDb getInstance(Context context) {
        if (instance == null) {
            instance = new AutreFspServiceDb(context);
        }
        return instance;
    }

    public long insertAutreFsp(AutreFse fsp) {
        try {

            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;

            if (db == null){
                Log.e("FseServiceDb","La base de données n'a pas pu être initialisée.");
            }

            ContentValues values = new ContentValues();
            values.put("numTransaction", fsp.getNumTransaction());
            values.put("photo_url", fsp.getUrlPhoto());

            return db.insert(TABLE_FSP_AUTRE, null, values);
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de l'insertion du FSE ambulatoire", e);
            return -1;
        }
    }


    @SuppressLint("Range")
    public List<AutreFse> getAllAutreFsp() {
        List<AutreFse> fspList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {

            if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                GlobalClass.getInstance().initDatabase("app");
            }
            db = GlobalClass.getInstance().cnxDbAppPrestation;


            cursor = db.query(TABLE_FSP_AUTRE, null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    AutreFse fsp = new AutreFse();
                    fsp.setId(cursor.getInt(cursor.getColumnIndex("id")));
                    fsp.setNumTrans(cursor.getString(cursor.getColumnIndex("numTransaction")));
                    fsp.setUrlPhoto(cursor.getString(cursor.getColumnIndex("photo_url")));

                    fspList.add(fsp);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("FseServiceDb", "Erreur lors de la récupération des FSE ambulatoires", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fspList;
    }
}
