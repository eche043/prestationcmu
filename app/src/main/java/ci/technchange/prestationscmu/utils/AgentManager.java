package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.dbHelper;

public class AgentManager {

    private static final String TAG = "AgentManager";
    private static AgentManager instance;
    private Context context;

    // Chemin spécifique vers votre base de données
    private static final String DB_PATH = "/data/data/ci.technchange.prestationscmu/databases/prestations_fse";

    // Constructeur privé pour le pattern Singleton
    private AgentManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Obtenir l'instance unique de AgentManager
     * @param context Le contexte de l'application
     * @return L'instance de AgentManager
     */
    public static synchronized AgentManager getInstance(Context context) {
        if (instance == null) {
            instance = new AgentManager(context);
        }
        return instance;
    }

    /**
     * Ouvre la base de données spécifique
     * @return SQLiteDatabase ou null si erreur
     */
    private SQLiteDatabase openSpecificDatabase() {
        try {
            File dbFile = new File(DB_PATH);
            if (dbFile.exists()) {
                Log.d(TAG, "Ouverture de la base spécifique: " + DB_PATH);
                return SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY);
            } else {
                Log.w(TAG, "Base de données non trouvée à: " + DB_PATH);
                // Essayer avec le chemin relatif
                File dbFileRelative = context.getDatabasePath("prestations_fse");
                if (dbFileRelative.exists()) {
                    Log.d(TAG, "Ouverture de la base relative: " + dbFileRelative.getPath());
                    return SQLiteDatabase.openDatabase(dbFileRelative.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                } else {
                    Log.w(TAG, "Base de données non trouvée non plus à: " + dbFileRelative.getPath());
                    // Fallback vers la méthode originale
                    Log.d(TAG, "Fallback vers dbHelper");
                    dbHelper helper = new dbHelper(context);
                    return helper.getReadableDatabase();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ouverture de la base spécifique", e);
            // Fallback vers la méthode originale
            try {
                Log.d(TAG, "Fallback vers dbHelper après erreur");
                dbHelper helper = new dbHelper(context);
                return helper.getReadableDatabase();
            } catch (Exception ex) {
                Log.e(TAG, "Erreur également avec dbHelper", ex);
                return null;
            }
        }
    }

    /**
     * Vérifie quel fichier de base de données est utilisé
     * @return Le chemin de la base utilisée
     */
    public String getCurrentDatabasePath() {
        SQLiteDatabase db = null;
        try {
            db = openSpecificDatabase();
            if (db != null) {
                return db.getPath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification du chemin DB", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return "Base de données non accessible";
    }

    /**
     * Liste toutes les tables de la base de données pour vérification
     * @return Liste des tables
     */
    public String[] getTablesInDatabase() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        java.util.List<String> tables = new java.util.ArrayList<>();

        try {
            db = openSpecificDatabase();
            if (db != null) {
                cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des tables", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return tables.toArray(new String[0]);
    }

    /**
     * Méthode de débogage pour vérifier le contenu de la table
     * @return Informations de débogage
     */
    public String debugAgentsTable() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        StringBuilder debug = new StringBuilder();

        try {
            db = openSpecificDatabase();
            if (db == null) {
                return "Impossible d'ouvrir la base de données";
            }

            debug.append("Base utilisée: ").append(db.getPath()).append("\n");

            // Vérifier si la table existe
            cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='agents_inscription'", null);
            if (cursor.getCount() == 0) {
                debug.append("Table 'agents_inscription' non trouvée\n");
                cursor.close();

                // Lister toutes les tables
                cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
                debug.append("Tables disponibles: ");
                while (cursor.moveToNext()) {
                    debug.append(cursor.getString(0)).append(", ");
                }
                debug.append("\n");
            } else {
                debug.append("Table 'agents_inscription' trouvée\n");
                cursor.close();

                // Compter les enregistrements
                cursor = db.rawQuery("SELECT COUNT(*) FROM agents_inscription", null);
                if (cursor.moveToFirst()) {
                    debug.append("Nombre d'agents: ").append(cursor.getInt(0)).append("\n");
                }
                cursor.close();

                // Lister les colonnes
                cursor = db.rawQuery("PRAGMA table_info(agents_inscription)", null);
                debug.append("Colonnes: ");
                while (cursor.moveToNext()) {
                    debug.append(cursor.getString(1)).append(", ");
                }
                debug.append("\n");

                // Afficher quelques exemples de matricules
                cursor = db.rawQuery("SELECT matricule FROM agents_inscription LIMIT 5", null);
                debug.append("Exemples de matricules: ");
                while (cursor.moveToNext()) {
                    debug.append(cursor.getString(0)).append(", ");
                }
                debug.append("\n");
            }

        } catch (Exception e) {
            debug.append("Erreur: ").append(e.getMessage());
            Log.e(TAG, "Erreur lors du débogage", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return debug.toString();
    }

    /**
     * Récupère le chemin de la photo de l'agent depuis la base de données
     * @param matricule Le matricule de l'agent
     * @return Le chemin de la photo ou null si non trouvé
     */
    public String getAgentPhotoPath(String matricule) {
        if (matricule == null || matricule.isEmpty()) {
            Log.w(TAG, "Matricule vide ou null");
            return null;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        String photoPath = null;

        try {
            db = openSpecificDatabase();
            if (db == null) {
                Log.e(TAG, "Impossible d'ouvrir la base de données");
                return null;
            }

            // Log du chemin de la base utilisée
            Log.d(TAG, "Base de données utilisée: " + db.getPath());

            // Requête pour récupérer le photo_path basé sur le matricule
            String query = "SELECT photo_path FROM agents_inscription WHERE matricule = ?";
            cursor = db.rawQuery(query, new String[]{matricule});

            if (cursor != null && cursor.moveToFirst()) {
                int photoPathIndex = cursor.getColumnIndex("photo_path");
                if (photoPathIndex != -1) {
                    photoPath = cursor.getString(photoPathIndex);
                    Log.d(TAG, "Chemin photo trouvé pour matricule " + matricule + ": " + photoPath);
                } else {
                    Log.w(TAG, "Colonne photo_path non trouvée dans la table agents_inscription");
                }
            } else {
                Log.w(TAG, "Aucun agent trouvé avec le matricule: " + matricule);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération du chemin photo", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return photoPath;
    }

    /**
     * Charge la photo de l'agent depuis le chemin stocké en base
     * @param matricule Le matricule de l'agent
     * @return Bitmap de la photo ou null si non trouvée
     */
    public Bitmap loadAgentPhoto(String matricule) {
        String photoPath = getAgentPhotoPath(matricule);
        return loadPhotoFromPath(photoPath);
    }

    /**
     * Charge une photo depuis un chemin donné
     * @param photoPath Le chemin vers la photo
     * @return Bitmap de la photo ou null si non trouvée
     */
    public Bitmap loadPhotoFromPath(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            Log.d(TAG, "Aucun chemin photo fourni");
            return null;
        }

        try {
            File photoFile = new File(photoPath);
            if (photoFile.exists() && photoFile.canRead()) {
                Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                if (bitmap != null) {
                    Log.d(TAG, "Photo chargée avec succès depuis: " + photoPath);
                    return bitmap;
                } else {
                    Log.w(TAG, "Impossible de décoder la photo: " + photoPath);
                }
            } else {
                Log.w(TAG, "Fichier photo inexistant ou non lisible: " + photoPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement de la photo: " + photoPath, e);
        }

        return null;
    }

    /**
     * Redimensionne une image pour optimiser la mémoire
     * @param original L'image originale
     * @param maxWidth Largeur maximale
     * @param maxHeight Hauteur maximale
     * @return Image redimensionnée
     */
    public Bitmap resizeBitmap(Bitmap original, int maxWidth, int maxHeight) {
        if (original == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;

        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        return Bitmap.createScaledBitmap(original, finalWidth, finalHeight, true);
    }

    /**
     * Applique la photo de l'agent à une ImageView
     * @param imageView L'ImageView à modifier
     * @param matricule Le matricule de l'agent
     * @param useDefaultIfNotFound Si true, utilise l'icône par défaut si pas de photo
     */
    public void applyAgentPhotoToImageView(ImageView imageView, String matricule, boolean useDefaultIfNotFound) {
        if (imageView == null) {
            Log.e(TAG, "ImageView est null");
            return;
        }

        Bitmap agentPhoto = loadAgentPhoto(matricule);

        if (agentPhoto != null) {
            imageView.setImageBitmap(agentPhoto);
            Log.d(TAG, "Photo appliquée à l'ImageView pour matricule: " + matricule);
        } else if (useDefaultIfNotFound) {
            imageView.setImageResource(R.drawable.ic_person_default);
            Log.d(TAG, "Photo par défaut appliquée pour matricule: " + matricule);
        }
    }

    /**
     * Applique la photo redimensionnée de l'agent à une ImageView
     * @param imageView L'ImageView à modifier
     * @param matricule Le matricule de l'agent
     * @param maxWidth Largeur maximale
     * @param maxHeight Hauteur maximale
     * @param useDefaultIfNotFound Si true, utilise l'icône par défaut si pas de photo
     */
    public void applyResizedAgentPhotoToImageView(ImageView imageView, String matricule,
                                                  int maxWidth, int maxHeight, boolean useDefaultIfNotFound) {
        if (imageView == null) {
            Log.e(TAG, "ImageView est null");
            return;
        }

        Bitmap agentPhoto = loadAgentPhoto(matricule);

        if (agentPhoto != null) {
            Bitmap resizedPhoto = resizeBitmap(agentPhoto, maxWidth, maxHeight);
            imageView.setImageBitmap(resizedPhoto);
            Log.d(TAG, "Photo redimensionnée appliquée à l'ImageView pour matricule: " + matricule);
        } else if (useDefaultIfNotFound) {
            imageView.setImageResource(R.drawable.ic_person_default);
            Log.d(TAG, "Photo par défaut appliquée pour matricule: " + matricule);
        }
    }

    /**
     * Vérifie si un agent a une photo
     * @param matricule Le matricule de l'agent
     * @return true si l'agent a une photo, false sinon
     */
    public boolean hasAgentPhoto(String matricule) {
        String photoPath = getAgentPhotoPath(matricule);
        if (photoPath == null || photoPath.isEmpty()) {
            return false;
        }

        File photoFile = new File(photoPath);
        return photoFile.exists() && photoFile.canRead();
    }

    /**
     * Récupère les informations complètes d'un agent
     * @param matricule Le matricule de l'agent
     * @return AgentInfo ou null si non trouvé
     */
    public AgentInfo getAgentInfo(String matricule) {
        if (matricule == null || matricule.isEmpty()) {
            return null;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        AgentInfo agentInfo = null;

        try {
            db = openSpecificDatabase();
            if (db == null) {
                Log.e(TAG, "Impossible d'ouvrir la base de données");
                return null;
            }

            // Requête pour récupérer toutes les infos de l'agent
            String query = "SELECT * FROM agents_inscription WHERE matricule = ?";
            cursor = db.rawQuery(query, new String[]{matricule});

            if (cursor != null && cursor.moveToFirst()) {
                agentInfo = new AgentInfo();
                agentInfo.matricule = matricule;

                // Récupérer les autres champs selon votre structure de table
                int nomIndex = cursor.getColumnIndex("nom");
                int prenomIndex = cursor.getColumnIndex("prenom");
                int telephoneIndex = cursor.getColumnIndex("telephone");
                int photoPathIndex = cursor.getColumnIndex("photo_path");

                if (nomIndex != -1) agentInfo.nom = cursor.getString(nomIndex);
                if (prenomIndex != -1) agentInfo.prenom = cursor.getString(prenomIndex);
                if (telephoneIndex != -1) agentInfo.telephone = cursor.getString(telephoneIndex);
                if (photoPathIndex != -1) agentInfo.photoPath = cursor.getString(photoPathIndex);

                Log.d(TAG, "Informations agent récupérées pour matricule: " + matricule);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des infos agent", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return agentInfo;
    }

    /**
     * Vérifie si un matricule existe déjà dans la base de données
     * @param matricule Le matricule à vérifier
     * @return true si le matricule existe déjà, false sinon
     */
    public boolean isMatriculeExists(String matricule, String oldmatricule) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        Log.d(TAG, "Vérification matricule: " + matricule + " (ancien: " + oldmatricule + ")");

        try {
            db = openSpecificDatabase();
            if (db == null) {
                Log.e(TAG, "Impossible d'ouvrir la base de données");
                return false;
            }

            // Requête pour compter les agents avec ce matricule,
            // en excluant l'agent actuel (originalMatricule)
            String query = "SELECT COUNT(*) FROM agents_inscription WHERE matricule = ? AND matricule != ?";
            cursor = db.rawQuery(query, new String[]{matricule, oldmatricule});

            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Nombre de matricules trouvés (hors agent actuel): " + count);
                return count > 0; // Si count > 0, le matricule existe déjà
            } else {
                Log.w("warning", "Aucun résultat trouvé");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification du matricule", e);
        } finally {
            // Fermer le cursor dans tous les cas
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return false; // En cas d'erreur, considérer que le matricule n'existe pas
    }

    public boolean isMatriculeExistsInscription(String matricule) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        Log.d(TAG, "Vérification matricule: " + matricule);

        try {
            db = openSpecificDatabase();
            if (db == null) {
                Log.e(TAG, "Impossible d'ouvrir la base de données");
                return false;
            }

            // Requête pour compter les agents avec ce matricule
            String query = "SELECT COUNT(*) FROM agents_inscription WHERE matricule = ?";
            cursor = db.rawQuery(query, new String[]{matricule});

            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Nombre de matricules trouvés: " + count);
                return count > 0; // Si count > 0, le matricule existe déjà
            } else {
                Log.w("warning", "Aucun résultat trouvé");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification du matricule", e);
        } finally {
            // Fermer le cursor dans tous les cas
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return false; // En cas d'erreur, considérer que le matricule n'existe pas
    }

    /**
     * Vérifie la structure de la base de données
     * @return true si la structure est correcte
     */
    public boolean verifyDatabaseStructure() {
        SQLiteDatabase db = null;
        boolean isValid = false;

        try {
            db = openSpecificDatabase();
            if (db == null) {
                Log.e(TAG, "Impossible d'ouvrir la base de données");
                return false;
            }

            // Vérifier si la table existe
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='agents_inscription'", null);
            if (cursor.getCount() == 0) {
                Log.e(TAG, "Table agents_inscription non trouvée");
                cursor.close();
                return false;
            }
            cursor.close();

            // Vérifier les colonnes
            Cursor columnCursor = db.rawQuery("PRAGMA table_info(agents_inscription)", null);
            boolean hasMatricule = false;
            boolean hasPhotoPath = false;

            if (columnCursor != null) {
                int nameIndex = columnCursor.getColumnIndex("name");
                while (columnCursor.moveToNext()) {
                    if (nameIndex != -1) {
                        String columnName = columnCursor.getString(nameIndex);
                        if ("matricule".equals(columnName)) {
                            hasMatricule = true;
                        } else if ("photo_path".equals(columnName)) {
                            hasPhotoPath = true;
                        }
                    }
                }
                columnCursor.close();
            }

            isValid = hasMatricule && hasPhotoPath;
            Log.d(TAG, "Structure BD - Table: OK, Matricule: " + hasMatricule + ", PhotoPath: " + hasPhotoPath);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification de la structure BD", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }

        return isValid;
    }

    /**
     * Transforme un bitmap en bitmap circulaire
     * @param bitmap L'image originale
     * @return Bitmap circulaire
     */
    public Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        // Dessiner le cercle
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Appliquer le mode de fusion pour découper l'image
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // Dessiner l'image au centre
        Rect srcRect = new Rect(
                (width - size) / 2,
                (height - size) / 2,
                (width - size) / 2 + size,
                (height - size) / 2 + size
        );
        Rect destRect = new Rect(0, 0, size, size);

        canvas.drawBitmap(bitmap, srcRect, destRect, paint);

        return output;
    }

    /**
     * Applique la photo circulaire de l'agent à une ImageView
     * @param imageView L'ImageView à modifier
     * @param matricule Le matricule de l'agent
     * @param useDefaultIfNotFound Si true, utilise l'icône par défaut si pas de photo
     */
    public void applyCircularAgentPhotoToImageView(ImageView imageView, String matricule, boolean useDefaultIfNotFound) {
        if (imageView == null) {
            Log.e(TAG, "ImageView est null");
            return;
        }

        Bitmap agentPhoto = loadAgentPhoto(matricule);

        if (agentPhoto != null) {
            // Transformer en bitmap circulaire
            Bitmap circularPhoto = getCircularBitmap(agentPhoto);
            imageView.setImageBitmap(circularPhoto);
            Log.d(TAG, "Photo circulaire appliquée à l'ImageView pour matricule: " + matricule);
        } else if (useDefaultIfNotFound) {
            imageView.setImageResource(R.drawable.ic_person_default);
            Log.d(TAG, "Photo par défaut appliquée pour matricule: " + matricule);
        }
    }

    /**
     * Applique la photo circulaire redimensionnée de l'agent à une ImageView
     * @param imageView L'ImageView à modifier
     * @param matricule Le matricule de l'agent
     * @param maxWidth Largeur maximale
     * @param maxHeight Hauteur maximale
     * @param useDefaultIfNotFound Si true, utilise l'icône par défaut si pas de photo
     */
    public void applyCircularResizedAgentPhotoToImageView(ImageView imageView, String matricule,
                                                          int maxWidth, int maxHeight, boolean useDefaultIfNotFound) {
        if (imageView == null) {
            Log.e(TAG, "ImageView est null");
            return;
        }

        Bitmap agentPhoto = loadAgentPhoto(matricule);

        if (agentPhoto != null) {
            // D'abord redimensionner puis transformer en circulaire
            Bitmap resizedPhoto = resizeBitmap(agentPhoto, maxWidth, maxHeight);
            Bitmap circularPhoto = getCircularBitmap(resizedPhoto);
            imageView.setImageBitmap(circularPhoto);
            Log.d(TAG, "Photo circulaire redimensionnée appliquée à l'ImageView pour matricule: " + matricule);
        } else if (useDefaultIfNotFound) {
            imageView.setImageResource(R.drawable.ic_person_default);
            Log.d(TAG, "Photo par défaut appliquée pour matricule: " + matricule);
        }
    }

    /**
     * Classe pour encapsuler les informations d'un agent
     */
    public static class AgentInfo {
        public String matricule;
        public String nom;
        public String prenom;
        public String telephone;
        public String photoPath;

        @Override
        public String toString() {
            return "AgentInfo{" +
                    "matricule='" + matricule + '\'' +
                    ", nom='" + nom + '\'' +
                    ", prenom='" + prenom + '\'' +
                    ", telephone='" + telephone + '\'' +
                    ", photoPath='" + photoPath + '\'' +
                    '}';
        }
    }
}