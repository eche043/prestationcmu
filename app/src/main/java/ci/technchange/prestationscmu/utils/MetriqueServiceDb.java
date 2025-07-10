package ci.technchange.prestationscmu.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ci.technchange.prestationscmu.core.GlobalClass;
import ci.technchange.prestationscmu.models.Metrique;

public class MetriqueServiceDb {
    private static MetriqueServiceDb instance;
    private final Context context;
    private SQLiteDatabase db;
    private static final String TAG = "MetriqueServiceDb";
    private static final String TABLE_METRIQUE = "metrique_utilisation";
    private final Object dbLock = new Object();

    private MetriqueServiceDb(Context context) {
        this.context = context.getApplicationContext();
        initializeDatabase();
    }

    public static synchronized MetriqueServiceDb getInstance(Context context) {
        if (instance == null) {
            instance = new MetriqueServiceDb(context);
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

    public long insertMetrique(Metrique metrique) {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour l'insertion");
            return -1;
        }

        synchronized (dbLock) {
            try {
                ContentValues values = new ContentValues();
                values.put("activite", metrique.getActivite());
                values.put("date_debut", metrique.getDateDebut());
                values.put("date_fin", metrique.getDateFin());
                values.put("id_famoco", metrique.getIdFamoco());
                values.put("id_region", metrique.getIdRegion());
                values.put("status_synchro", metrique.getStatusSynchro());

                long id = db.insert(TABLE_METRIQUE, null, values);
                Log.d(TAG, "Métrique insérée avec ID: " + id);
                return id;
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'insertion de la métrique", e);
                return -1;
            }
        }
    }

    public boolean metriqueExists(String activite, String idFamoco, String dateDebut) {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour la vérification");
            return false;
        }

        Cursor cursor = null;
        synchronized (dbLock) {
            try {
                String query = "SELECT COUNT(*) FROM " + TABLE_METRIQUE +
                        " WHERE activite = ? AND id_famoco = ? AND date_debut = ?";
                cursor = db.rawQuery(query, new String[]{activite, idFamoco, dateDebut});

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

    public List<Metrique> getMetriqueNonSynchro() {
        List<Metrique> metriques = new ArrayList<>();
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour la lecture");
            return metriques;
        }

        Cursor cursor = null;
        synchronized (dbLock) {
            try {
                cursor = db.query(
                        TABLE_METRIQUE,
                        null,
                        "status_synchro = ?",
                        new String[]{"0"},
                        null, null, null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        Metrique metrique = new Metrique();
                        metrique.setId(cursor.getInt(cursor.getColumnIndex("id")));
                        metrique.setActivite(cursor.getString(cursor.getColumnIndex("activite")));
                        metrique.setDateDebut(cursor.getString(cursor.getColumnIndex("date_debut")));
                        metrique.setDateFin(cursor.getString(cursor.getColumnIndex("date_fin")));
                        metrique.setIdFamoco(cursor.getString(cursor.getColumnIndex("id_famoco")));
                        metrique.setIdRegion(cursor.getInt(cursor.getColumnIndex("id_region")));
                        metriques.add(metrique);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la récupération des métriques", e);
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
                        TABLE_METRIQUE,
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
                cursor = db.rawQuery("PRAGMA table_info(" + TABLE_METRIQUE + ")", null);
                Log.d(TAG, "Structure de la table " + TABLE_METRIQUE + ":");
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

    public String countAllMetriques() {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour le comptage");
            return "0"; // Retourne "0" sous forme de chaîne en cas d'erreur
        }

        Cursor cursor = null;
        synchronized (dbLock) {
            try {
                String query = "SELECT COUNT(*) FROM " + TABLE_METRIQUE;
                cursor = db.rawQuery(query, null);

                if (cursor != null && cursor.moveToFirst()) {
                    return String.valueOf(cursor.getInt(0)); // Convertit le résultat en String
                }
                return "0"; // Retourne "0" si le curseur est vide
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du comptage des métriques", e);
                return "0"; // Retourne "0" en cas d'exception
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public String countActivitesSpecifiques() {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour le comptage");
            return "0";
        }

        Cursor cursor = null;
        synchronized (dbLock) {
            try {
                // Requête pour compter les activités spécifiques
                String query = "SELECT COUNT(*) FROM " + TABLE_METRIQUE +
                        " WHERE activite IN ('rech_bio', 'scan_qr', 'saisir_num_secu')";
                cursor = db.rawQuery(query, null);

                if (cursor != null && cursor.moveToFirst()) {
                    return String.valueOf(cursor.getInt(0));
                }
                return "0";
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du comptage des activités spécifiques", e);
                return "0";
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public int countActiviteFseEdit() {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            Log.e(TAG, "La base de données n'est pas disponible pour le comptage");
            return 0;
        }

        Cursor cursor = null;
        synchronized (dbLock) {
            try {
                String query = "SELECT COUNT(*) FROM " + TABLE_METRIQUE +
                        " WHERE activite = 'fse_edit'";
                cursor = db.rawQuery(query, null);

                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
                return 0;
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du comptage des activités fse_edit", e);
                return 0;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }
}