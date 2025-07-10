package ci.technchange.prestationscmu.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.utils.ApiService;
import ci.technchange.prestationscmu.utils.RSAUtils;
import ci.technchange.prestationscmu.views.SplashscreenActivity;

public class DownloadService extends Service {
    private static final String CHANNEL_ID = "DownloadServiceChannel";
    public static final String PROGRESS_UPDATE_ACTION = "PROGRESS_UPDATE_ACTION";
    public static final String PROGRESS_EXTRA = "PROGRESS_EXTRA";

    private KeyPair keyPair;
    private int downloadCount = 0;

    private Intent originalIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        try {
            keyPair = RSAUtils.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String fileUrl = intent.getStringExtra("fileUrl");
        String apiUpdateDb = intent.getStringExtra("apiUpdateDb");
        this.originalIntent = intent;
        startForeground(1, createNotification());

        if (fileUrl != null) {
            // Télécharger newcnambd1.db
            //new DownloadFileTask("newcnambd1.db").execute( fileUrl);
            new DownloadFileTask("newcnambd1.db").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fileUrl);
        } else if (apiUpdateDb != null) {
            // Télécharger apiUpdateDb
            //new DownloadFileTask("enroles_sql_files.zip").execute( apiUpdateDb);
            new DownloadFileTask("enroles_sql_files.zip").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, apiUpdateDb);
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, SplashscreenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Téléchargement en cours")
                .setContentText("Votre fichier est en cours de téléchargement")
                .setSmallIcon(R.drawable.ic_download)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Download Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private class DownloadFileTask extends AsyncTask<String, Integer, String> {
        private String fileName;

        public DownloadFileTask(String fileName) {
            this.fileName = fileName;
        }

        @Override
        protected String doInBackground(String... params) {
            String fileUrl = params[0];
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            String originalFileName = "newcnambd1.db"; // Nom par défaut

            try {
                URL url = new URL(fileUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Public-Key", Base64.getEncoder().encodeToString(publicKey.getEncoded()));
                urlConnection.setConnectTimeout(15000); // 15 secondes de timeout
                urlConnection.setReadTimeout(15000);
                urlConnection.connect();

                // Vérifier le code de réponse HTTP
                if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Erreur HTTP : " + urlConnection.getResponseCode();
                }

                // Extraire le nom du fichier des en-têtes HTTP
                String contentDisposition = urlConnection.getHeaderField("Content-Disposition");
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    originalFileName = contentDisposition.substring(contentDisposition.indexOf("filename=") + 9).replace("\"", "");
                    System.out.println("Nom original du fichier téléchargé: " + originalFileName);
                } else {
                    // Si l'en-tête n'existe pas, on utilise le chemin d'URL
                    String path = url.getPath();
                    int lastSlashIndex = path.lastIndexOf('/');
                    if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
                        String urlFileName = path.substring(lastSlashIndex + 1);
                        if (!urlFileName.equals("bdregion.php")) {
                            originalFileName = urlFileName;
                            System.out.println("Nom de fichier extrait de l'URL: " + originalFileName);
                        }
                    }
                    System.out.println("URL de téléchargement: " + fileUrl);
                    System.out.println("Nom par défaut utilisé: " + originalFileName);
                }

                // Télécharger le fichier
                File externalDir = new File(getFilesDir(), "encryptedbd");
                if (!externalDir.exists()) externalDir.mkdirs();

                File file = new File(externalDir, fileName);
                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                FileOutputStream outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[8192];
                long total = 0;
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    total += length;
                    int progress = (int) (total * 100 / urlConnection.getContentLength());
                    publishProgress(progress);
                    outputStream.write(buffer, 0, length);
                }
                outputStream.close();
                inputStream.close();

                // Retourner le résultat avec le chemin du fichier et le nom original
                return "Fichier téléchargé avec succès : " + file.getAbsolutePath() + "|" + originalFileName;
            } catch (Exception e) {
                Log.e("DownloadFileTask", "Erreur lors du téléchargement du fichier", e);
                return "Erreur lors du téléchargement du fichier : " + e.getMessage();
            }
        }

        /*protected String doInBackground(String... params) {
            String fileUrl = params[0];
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            String originalFileName = "newcnambd1.db"; // Nom par défaut

            try {
                URL url = new URL(fileUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Public-Key", Base64.getEncoder().encodeToString(publicKey.getEncoded()));
                urlConnection.connect();

                // Essayer d'extraire le nom du fichier des en-têtes HTTP
                String contentDisposition = urlConnection.getHeaderField("Content-Disposition");
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    originalFileName = contentDisposition.substring(contentDisposition.indexOf("filename=") + 9).replace("\"", "");
                    System.out.println("Nom original du fichier téléchargé: " + originalFileName);
                } else {
                    // Si l'en-tête n'existe pas, on utilise le chemin d'URL
                    String path = url.getPath();
                    int lastSlashIndex = path.lastIndexOf('/');
                    if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
                        String urlFileName = path.substring(lastSlashIndex + 1);
                        if (!urlFileName.equals("bdregion.php")) {
                            originalFileName = urlFileName;
                            System.out.println("Nom de fichier extrait de l'URL: " + originalFileName);
                        }
                    }
                    System.out.println("URL de téléchargement: " + fileUrl);
                    System.out.println("Nom par défaut utilisé: " + originalFileName);
                }
                int fileLength = urlConnection.getContentLength();

                File externalDir = new File(getFilesDir(), "encryptedbd");
                if (!externalDir.exists()) externalDir.mkdirs();

                File file = new File(externalDir, fileName);
                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                FileOutputStream outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[8192];
                long total = 0;
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    total += length;
                    int progress = (int) (total * 100 / fileLength);
                    publishProgress(progress);
                    outputStream.write(buffer, 0, length);
                }
                outputStream.close();
                inputStream.close();

                return "Fichier téléchargé avec succès : " + file.getAbsolutePath() + "|" + originalFileName;
            } catch (Exception e) {
                Log.e("DownloadFileTask", "Erreur lors du téléchargement du fichier", e);
                return "Erreur lors du téléchargement du fichier : " + fileUrl;
            }
        }**/

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            Intent intent = new Intent(PROGRESS_UPDATE_ACTION);
            intent.putExtra(PROGRESS_EXTRA, progress[0]);
            LocalBroadcastManager.getInstance(DownloadService.this).sendBroadcast(intent);
            Log.d("DownloadFileTask", "Progression : " + progress[0] + "%");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("DownloadFileTask", result);

            String downloadResult = result;
            String originalFileName = fileName; // par défaut

            if (result.contains("|")) {
                String[] parts = result.split("\\|");
                downloadResult = parts[0];
                originalFileName = parts.length > 1 ? parts[1] : fileName;
            }
            if (result.startsWith("Erreur")) {

                File externalDir = new File(getFilesDir(), "encryptedbd");
                File file = new File(externalDir, fileName);
                if (file.exists()) {
                    file.delete();
                }
            }else{
                Log.d("DownloadService", "Telechargement OK");
                ApiService apiService = new ApiService(DownloadService.this);

                // Récupérer les données de l'Intent
                Intent originalIntent = DownloadService.this.originalIntent;
                if (originalIntent != null) {
                    String idRegion = originalIntent.getStringExtra("id_region");
                    String idFamoco = originalIntent.getStringExtra("id_famoco");
                    String codeEts = originalIntent.getStringExtra("code_ets");
                    String codeAgac = originalIntent.getStringExtra("code_agac");
                    String dateRemontee = originalIntent.getStringExtra("date_remontee");
                    String heureDebut = originalIntent.getStringExtra("heure_debut");

                    Log.d("DownloadService","id Famaco"+idFamoco);
                    Log.d("DownloadService","id region"+idRegion);

                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                    Date FinHeure = new Date();

                    apiService.storeBdDownload(
                            idRegion,           // id_region
                            idFamoco,         // id_famoco
                            codeEts,      // code_ets
                            codeAgac,       // code_agac
                            dateRemontee,   // date_remontee
                            heureDebut,     // heure_debut
                            timeFormat.format(FinHeure),     // heure_fin
                            new ApiService.ApiCallback() {
                                @Override
                                public void onSuccess(JSONObject result) {
                                    // Traitement en cas de succès
                                    Log.d("TAG", "Statistiques enregistrées avec succès");
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    // Traitement en cas d'erreur
                                    Log.e("TAG", "Erreur: " + errorMessage);
                                }
                            }
                    );
                }else{
                    Log.d("DownloadService", "Intent null");
                }
                // Exemple d'utilisation de la fonction


            }
            Intent intent = new Intent(PROGRESS_UPDATE_ACTION);
            intent.putExtra("result", downloadResult);
            intent.putExtra("originalFileName", originalFileName);
            if (fileName.equals("enroles_sql_files.zip")) {
                intent.putExtra("fileType", "enroles_sql_files.zip");
            }
            LocalBroadcastManager.getInstance(DownloadService.this).sendBroadcast(intent);

            downloadCount++;
            if (downloadCount == 2) {
                stopForeground(true);
                stopSelf();
            }
        }
    }
}