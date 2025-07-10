package ci.technchange.prestationscmu.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.models.MetriqueConnexion;

public class MetriqueConnexionServiceDb {
    private static MetriqueConnexionServiceDb instance;
    private final Context context;
    private SQLiteDatabase db;
    private static final String TAG = "MetriqueConnexionServiceDb";
    private static final String TABLE_METRIQUE_CONNEXION = "metrique_connexion";
    private final Object dbLock = new Object();

    public MetriqueConnexionServiceDb(Context context) {
        this.context = context.getApplicationContext();
        initializeDatabase();
    }

    public static synchronized MetriqueConnexionServiceDb getInstance(Context context) {
        if (instance == null) {
            instance = new MetriqueConnexionServiceDb(context);
        }
        return instance;
    }

    private void initializeDatabase() {
        synchronized (dbLock) {
            try {
                if (GlobalClass.getInstance().cnxDbAppPrestation == null) {
                    GlobalClass.getInstance().initDatabase("app");
                }
                db = GlobalClass.getInstance().cnxDbAppPrestation;

                if (db == null) {
                    Log.e(TAG, "Échec de l'initialisation de la base de données");
                } else {
                    Log.d(TAG, "Base de données initialisée avec succès");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'initialisation de la base de données", e);
            }
        }
    }

    private SQLiteDatabase getDatabase() {
        synchronized (dbLock) {
            if (db == null || !db.isOpen()) {
                initializeDatabase();
            }
            return db;
        }
    }

    public long insertMetriqueConnexion(String codeEts,String codeAgac,String nomComplet,String dateConnexion,String heureConnexion,String idRegion,int statusSynchro) {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour l'insertion");
            return -1;
        }

        synchronized (dbLock) {
            try {
                ContentValues values = new ContentValues();
                values.put("code_ets", codeEts);
                values.put("code_agac", codeAgac);
                values.put("nom_complet", nomComplet);
                values.put("date_connexion", dateConnexion);
                values.put("heure_connexion", heureConnexion);
                values.put("id_region", idRegion);
                values.put("status_synchro", statusSynchro);

                System.out.println("METRIQUECONNEXION------");
                System.out.println(codeEts);
                System.out.println(codeAgac);
                System.out.println(nomComplet);
                System.out.println(dateConnexion);
                System.out.println(heureConnexion);
                System.out.println(idRegion);
                System.out.println(statusSynchro);

                long id = db.insert(TABLE_METRIQUE_CONNEXION, null, values);
                Log.d(TAG, "Métrique connexion insérée avec ID: " + id);
                return id;
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'insertion de la métrique connexion", e);
                return -1;
            }
        }
    }

    public boolean metriqueConnexionExists(String codeEts, String codeAgac, String dateConnexion, String heureConnexion) {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour la vérification");
            return false;
        }

        Cursor cursor = null;
        synchronized (dbLock) {
            try {
                String query = "SELECT COUNT(*) FROM " + TABLE_METRIQUE_CONNEXION +
                        " WHERE code_ets = ? AND code_agac = ? AND date_connexion = ? AND heure_connexion = ?";
                cursor = db.rawQuery(query, new String[]{codeEts, codeAgac, dateConnexion, heureConnexion});

                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getInt(0) > 0;
                }
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la vérification de l'existence", e);
                return false;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public List<MetriqueConnexion> getMetriquesConnexionNonSynchro() {
        List<MetriqueConnexion> metriques = new ArrayList<>();
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour la lecture");
            return metriques;
        }

        Cursor cursor = null;
        synchronized (dbLock) {
            try {
                cursor = db.query(
                        TABLE_METRIQUE_CONNEXION,
                        null,
                        "status_synchro = ?",
                        new String[]{"0"},
                        null, null, null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        MetriqueConnexion metrique = new MetriqueConnexion();
                        metrique.setId(cursor.getInt(cursor.getColumnIndex("id")));
                        metrique.setCodeEts(cursor.getString(cursor.getColumnIndex("code_ets")));
                        metrique.setCodeAgac(cursor.getString(cursor.getColumnIndex("code_agac")));
                        metrique.setNomComplet(cursor.getString(cursor.getColumnIndex("nom_complet")));
                        metrique.setDateConnexion(cursor.getString(cursor.getColumnIndex("date_connexion")));
                        metrique.setHeureConnexion(cursor.getString(cursor.getColumnIndex("heure_connexion")));
                        metrique.setIdRegion(cursor.getInt(cursor.getColumnIndex("id_region")));
                        metrique.setStatusSynchro(cursor.getInt(cursor.getColumnIndex("status_synchro")));

                        metriques.add(metrique);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la récupération des métriques de connexion", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return metriques;
    }

    public boolean updateSyncStatus(long id, int status) {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour la mise à jour");
            return false;
        }

        synchronized (dbLock) {
            try {
                ContentValues values = new ContentValues();
                values.put("status_synchro", status);

                int rowsAffected = db.update(
                        TABLE_METRIQUE_CONNEXION,
                        values,
                        "id = ?",
                        new String[]{String.valueOf(id)}
                );

                Log.d(TAG, "Mise à jour du statut - ID: " + id +
                        ", Lignes affectées: " + rowsAffected);
                return rowsAffected > 0;
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la mise à jour du statut", e);
                return false;
            }
        }
    }

    public void logTableStructure() {
        SQLiteDatabase db = getDatabase();
        if (db == null) return;

        Cursor cursor = null;
        synchronized (dbLock) {
            try {
                cursor = db.rawQuery("PRAGMA table_info(" + TABLE_METRIQUE_CONNEXION + ")", null);
                Log.d(TAG, "Structure de la table " + TABLE_METRIQUE_CONNEXION + ":");
                while (cursor.moveToNext()) {
                    String name = cursor.getString(1);
                    String type = cursor.getString(2);
                    Log.d(TAG, "Colonne: " + name + " - Type: " + type);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la lecture de la structure", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public void closeDatabase() {
        synchronized (dbLock) {
            try {
                if (db != null && db.isOpen()) {
                    db.close();
                    db = null;
                    Log.d(TAG, "Base de données fermée");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la fermeture de la base", e);
            }
        }
    }
}