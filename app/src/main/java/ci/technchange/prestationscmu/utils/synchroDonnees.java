package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ci.technchange.prestationscmu.core.GlobalClass;

public class synchroDonnees {
    private static final String PREFS_NAME = "SyncPrefs";
    private static final String FOLDER_COUNT_KEY = "folderCount";
    private Context context;
    private RequestQueue requestQueue;
    private GlobalClass globalClass;
    private int processedFiles = 0;

    public synchroDonnees(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
        this.globalClass = GlobalClass.getInstance();
    }

    public void telechargerEtDezipperDossierZip(String url, final String fichierZip, final DownloadCallback callback) {
        InputStreamRequest inputStreamRequest = new InputStreamRequest(
                Request.Method.GET,
                url,
                new Response.Listener<InputStream>() {
                    @Override
                    public void onResponse(InputStream response) {
                        try {
                            saveToFile(response, fichierZip);
                            callback.onDownloadComplete(true, null);
                        } catch (IOException e) {
                            Log.e("Unzip Error", "Échec lors du dézippage du fichier", e);
                            callback.onDownloadComplete(false, e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = error.getMessage();

                        if (error.networkResponse != null &&
                                error.networkResponse.statusCode == 500) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                if (responseBody.contains("Le dossier racine n'existe pas")) {
                                    errorMessage = "La base de données est à jour";
                                }
                            } catch (UnsupportedEncodingException e) {
                                Log.e("API Error", "Erreur lors de la lecture du message d'erreur", e);
                            }
                        }

                        Log.e("API Error", "Échec du téléchargement du fichier", error);
                        callback.onDownloadComplete(false, errorMessage);
                    }
                });

        requestQueue.add(inputStreamRequest);
    }

    public int countFoldersInZip(File zipFile) throws IOException {
        int folderCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    String name = entry.getName();
                    if (!name.equals("/") && !name.isEmpty()) {
                        folderCount++;
                    }
                }
                zis.closeEntry();
            }
        }

        saveFolderCount(folderCount);

        return folderCount;
    }

    private void saveFolderCount(int count) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(FOLDER_COUNT_KEY, count)
                .apply();
    }

    public int getSavedFolderCount() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(FOLDER_COUNT_KEY, 0);
    }

    public void unzipDossierFile(File zipFile) throws IOException {
        if (!zipFile.exists() || zipFile.length() == 0) {
            Log.e("Unzip Error", "File does not exist or is empty");
            throw new IOException("Fichier ZIP invalide");
        }

        // Compter les dossiers avant extraction
        int totalFolders = countFoldersInZip(zipFile);
        Log.d("Zip Structure", "Nombre de dossiers dans le ZIP: " + totalFolders);

        FileInputStream fis = new FileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry zipEntry;

        File outputDir = new File(context.getFilesDir(), "unzipped");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        int processedFolders = 0;

        try {
            while ((zipEntry = zis.getNextEntry()) != null) {
                String entryName = zipEntry.getName();
                File outputFile = new File(outputDir, entryName);

                if (zipEntry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                    processedFolders++;
                    Log.d("Folder Progress", "Dossier " + processedFolders + "/" + totalFolders + " traité: " + entryName);
                    continue;
                }

                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }

                if (entryName.toLowerCase().endsWith(".sql")) {
                    Log.d("SQL Processing", "Traitement du fichier: " + entryName);
                    executeSqlFile(outputFile);
                }
                zis.closeEntry();
            }
        } finally {
            zis.close();
            fis.close();
        }
    }

    private void executeSqlFile(File sqlFile) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sqlFile)))) {
            globalClass.initDatabase("enrole");
            globalClass.cnxDbEnrole.beginTransaction();

            String line;
            int lineCount = 0;
            int successCount = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                lineCount++;

                if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*")) {
                    continue;
                }

                try {
                    globalClass.cnxDbEnrole.execSQL(line);
                    successCount++;
                } catch (Exception e) {
                    Log.e("SQL Error", "Erreur ligne " + lineCount + " dans " + sqlFile.getName() + ": " + line, e);
                }
            }

            globalClass.cnxDbEnrole.setTransactionSuccessful();
            Log.d("SQL Execution", "Succès: " + successCount + "/" + lineCount + " lignes exécutées");
        } catch (Exception e) {
            Log.e("SQL Error", "Erreur fatale dans " + sqlFile.getName(), e);
        } finally {
            globalClass.cnxDbEnrole.endTransaction();
        }
    }

    private void saveToFile(InputStream inputStream, String fileName) throws IOException {
        File file = new File(context.getFilesDir(), fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
        inputStream.close();
        Log.d("File Saved", "Taille du fichier: " + file.length() + " octets");
    }

    public void cleanup(File zipFile, File outputDir) {
        if (zipFile.exists() && !zipFile.delete()) {
            Log.e("Cleanup", "Échec de la suppression du fichier zip");
        }

        if (outputDir.exists() && outputDir.isDirectory()) {
            deleteDirectory(outputDir);
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else if (!file.delete()) {
                    Log.e("Cleanup", "Échec de la suppression du fichier: " + file.getName());
                }
            }
        }

        if (!directory.delete()) {
            Log.e("Cleanup", "Échec de la suppression du dossier: " + directory.getName());
        }
    }

    public interface DownloadCallback {
        void onDownloadComplete(boolean success, String errorMessage);
    }
}