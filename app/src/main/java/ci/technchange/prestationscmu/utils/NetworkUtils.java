package ci.technchange.prestationscmu.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import ci.technchange.prestationscmu.views.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    //private static final String SERVER_URL = "http://51.38.224.233:8081/cmu-soins/api/feuilles-soin.php";
    private static final String SERVER_URL = "http://57.128.30.4:8085/cmu-soins/api/upload_handler.php";
    //private static final String SERVER_URL = "http://192.168.192.12:8087/cmu-soins/api/feuilles-soin.php";

    // Timeouts réseau (en secondes)
    private static final int CONNECT_TIMEOUT = 15;
    private static final int READ_TIMEOUT = 30;
    private static final int WRITE_TIMEOUT = 30;

    // Client OkHttp réutilisable
    private static OkHttpClient client = null;

    // Interface de callback pour les opérations d'upload
    public interface UploadCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Convertit une image en PNG en conservant la haute qualité
     * @param sourceFile Fichier image source
     * @return Fichier PNG converti
     */
    private static File convertToPng(File sourceFile) throws IOException {
        // Lire l'orientation originale de l'image
        ExifInterface exif = new ExifInterface(sourceFile.getAbsolutePath());
        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
        );

        // Options de décodage pour maintenir la qualité
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Meilleure qualité

        // Décoder le bitmap
        Bitmap originalBitmap = BitmapFactory.decodeFile(sourceFile.getAbsolutePath(), options);

        // Gérer la rotation si nécessaire
        Bitmap rotatedBitmap = originalBitmap;
        if (orientation != ExifInterface.ORIENTATION_NORMAL) {
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(270);
                    break;
            }

            rotatedBitmap = Bitmap.createBitmap(
                    originalBitmap, 0, 0,
                    originalBitmap.getWidth(), originalBitmap.getHeight(),
                    matrix, true
            );

            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle();
            }
        }

        // Créer un fichier temporaire PNG
        File pngFile = File.createTempFile("converted_", ".png", sourceFile.getParentFile());

        // Compresser en PNG
        try (FileOutputStream out = new FileOutputStream(pngFile)) {
            rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }

        // Libérer le bitmap
        rotatedBitmap.recycle();

        return pngFile;
    }

    // Ajoutez cette méthode à NetworkUtils
    public static void showNoConnectionDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Pas de connexion Internet")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {
                    // Retourner à l'écran d'accueil
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);

                    // Si le contexte est une activité, la fermer
                    if (context instanceof Activity) {
                        ((Activity) context).finish();
                    }
                });

        // Afficher le dialogue sur le thread UI
        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            builder.create().show();
        }
    }

    /**
     * Récupère une instance optimisée du client OkHttp
     */
    private static synchronized OkHttpClient getClient() {
        if (client == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .addInterceptor(logging)
                    .build();
        }
        return client;
    }

    /**
     * Télécharge une image sur le serveur
     * @param context Contexte de l'application
     * @param imageFile Fichier image à envoyer
     * @param callback Callback pour le résultat
     */
    /*public static void uploadImage(Context context, File imageFile, final UploadCallback callback) {
        Thread uploadThread = new Thread(() -> {
            File pngFile = null;
            try {
                // Convertir l'image en PNG
                pngFile = convertToPng(imageFile);

                // Vérifier que le fichier est valide
                if (pngFile == null || !pngFile.exists() || pngFile.length() == 0) {
                    Log.e(TAG, "Le fichier PNG n'a pas été créé correctement");
                    callback.onError("Erreur lors de la création du fichier PNG");
                    return;
                }

                // Créer le corps de la requête
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                                "image",
                                pngFile.getName(),
                                RequestBody.create(MediaType.parse("image/png"), pngFile)
                        )
                        .build();

                // Construire la requête
                Request request = new Request.Builder()
                        .url(SERVER_URL)
                        .header("Connection", "keep-alive")
                        .header("Content-Length", String.valueOf(pngFile.length()))
                        .post(requestBody)
                        .build();

                // Exécuter la requête
                Response response = getClient().newCall(request).execute();

                // Gérer la réponse
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String errorMsg = "Erreur serveur: " + response.code();
                    try {
                        String responseBody = response.body().string();
                        Log.e(TAG, "Réponse du serveur: " + responseBody);
                        errorMsg += " - " + responseBody;
                    } catch (IOException e) {
                        Log.e(TAG, "Erreur lors de la lecture de la réponse", e);
                    }
                    callback.onError(errorMsg);
                }
            } catch (IOException e) {
                Log.e(TAG, "Erreur upload", e);
                callback.onError("Erreur réseau: " + e.getMessage());
            } finally {
                // Supprimer le fichier temporaire PNG
                if (pngFile != null && pngFile.exists()) {
                    pngFile.delete();
                }
            }
        });

        uploadThread.setPriority(Thread.MAX_PRIORITY);
        uploadThread.start();
    }*/
    // Rendre cette méthode package-private pour qu'elle ne soit utilisée que par UploadQueueManager
    static void uploadImage(Context context, File imageFile, String numTrans, final UploadCallback callback) {
        Log.d("IMAGE_PATH",""+imageFile);
        Thread uploadThread = new Thread(() -> {
            File pngFile = null;
            try {
                // Convertir l'image en PNG
                pngFile = convertToPng(imageFile);

                // Vérifier que le fichier est valide
                if (pngFile == null || !pngFile.exists() || pngFile.length() == 0) {
                    Log.e(TAG, "Le fichier PNG n'a pas été créé correctement");
                    callback.onError("Erreur lors de la création du fichier PNG");
                    return;
                }

                Log.d("pngFile", ""+pngFile.getName());

                // Créer le corps de la requête avec num_trans
                MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                                "image",
                                pngFile.getName(),
                                RequestBody.create(MediaType.parse("image/png"), pngFile)
                        );

                // Ajouter num_trans s'il n'est pas null ou vide
                if (numTrans != null && !numTrans.isEmpty()) {
                    requestBodyBuilder.addFormDataPart("num_trans", numTrans);
                    Log.d(TAG, "Ajout du numéro de transaction: " + numTrans);
                }

                RequestBody requestBody = requestBodyBuilder.build();
                Log.d("png Size", ""+String.valueOf(pngFile.length()));
                // Construire la requête
                Request request = new Request.Builder()
                        .url(SERVER_URL)
                        .header("Connection", "keep-alive")
                        .header("Content-Length", String.valueOf(pngFile.length()))
                        .post(requestBody)
                        .build();

                // Exécuter la requête
                Response response = getClient().newCall(request).execute();

                // Gérer la réponse
                if (response.isSuccessful()) {
                    String responseBody = "";
                    try {
                        responseBody = response.body().string();
                        Log.d(TAG, "Réponse du serveur: " + responseBody);

                        // Si besoin, on peut analyser la réponse JSON pour récupérer des informations
                        // JSONObject jsonResponse = new JSONObject(responseBody);
                        // String jobId = jsonResponse.getString("job_id");

                        callback.onSuccess();
                    } catch (IOException e) {
                        Log.e(TAG, "Erreur lors de la lecture de la réponse", e);
                        callback.onSuccess(); // Considère quand même comme un succès
                    }
                } else {
                    String errorMsg = "Erreur serveur: " + response.code();
                    try {
                        String responseBody = response.body().string();
                        Log.e(TAG, "Réponse du serveur en erreur: " + responseBody);
                        errorMsg += " - " + responseBody;
                    } catch (IOException e) {
                        Log.e(TAG, "Erreur lors de la lecture de la réponse", e);
                    }
                    callback.onError(errorMsg);
                }
            } catch (IOException e) {
                Log.e(TAG, "Erreur upload", e);
                callback.onError("Erreur réseau: " + e.getMessage());
            } finally {
                // Supprimer le fichier temporaire PNG
                if (pngFile != null && pngFile.exists()) {
                    pngFile.delete();
                }
            }
        });

        uploadThread.setPriority(Thread.MAX_PRIORITY);
        uploadThread.start();
    }

    /*static void uploadImage(Context context, File imageFile, String numTrans,final UploadCallback callback) {
        Thread uploadThread = new Thread(() -> {
            File pngFile = null;
            try {
                // Convertir l'image en PNG
                pngFile = convertToPng(imageFile);

                // Vérifier que le fichier est valide
                if (pngFile == null || !pngFile.exists() || pngFile.length() == 0) {
                    Log.e(TAG, "Le fichier PNG n'a pas été créé correctement");
                    callback.onError("Erreur lors de la création du fichier PNG");
                    return;
                }

                // Créer le corps de la requête
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                                "image",
                                pngFile.getName(),
                                RequestBody.create(MediaType.parse("image/png"), pngFile)
                        )
                        .build();

                // Construire la requête
                Request request = new Request.Builder()
                        .url(SERVER_URL)
                        .header("Connection", "keep-alive")
                        .header("Content-Length", String.valueOf(pngFile.length()))
                        .post(requestBody)
                        .build();

                // Exécuter la requête
                Response response = getClient().newCall(request).execute();

                // Gérer la réponse
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String errorMsg = "Erreur serveur: " + response.code();
                    try {
                        String responseBody = response.body().string();
                        Log.e(TAG, "Réponse du serveur: " + responseBody);
                        errorMsg += " - " + responseBody;
                    } catch (IOException e) {
                        Log.e(TAG, "Erreur lors de la lecture de la réponse", e);
                    }
                    callback.onError(errorMsg);
                }
            } catch (IOException e) {
                Log.e(TAG, "Erreur upload", e);
                callback.onError("Erreur réseau: " + e.getMessage());
            } finally {
                // Supprimer le fichier temporaire PNG
                if (pngFile != null && pngFile.exists()) {
                    pngFile.delete();
                }
            }
        });

        uploadThread.setPriority(Thread.MAX_PRIORITY);
        uploadThread.start();
    }**/


    // Ajouter cette méthode à votre classe NetworkUtils
    /*public static void uploadImageWithRetry(Context context, File imageFile, final UploadCallback callback) {
        // Ne plus vérifier la connexion ici, toujours mettre dans la file d'attente
        Log.d(TAG, "Ajout de la feuille de soins à la file d'attente pour traitement en arrière-plan");
        // Vérifier d'abord l'état du réseau
        Log.d(TAG, "il est danns retry ");
        if (!UploadQueueManager.getInstance(context).isNetworkGood()) {
            // Si le réseau n'est pas bon, ajouter à la file d'attente
            Log.d(TAG, "Réseau indisponible, ajout à la file d'attente");
            UploadQueueManager.getInstance(context).addToQueue(imageFile);

            // Informer l'utilisateur
            callback.onError("Pas de connexion internet. L'image sera envoyée ultérieurement.");
            // Afficher un dialogue plutôt qu'un simple message d'erreur
            showNoConnectionDialog(context, "Votre feuille de soins a été sauvegardée et sera envoyée automatiquement dès qu'une connexion Internet sera disponible.");
            return;
        }

        // Si le réseau est bon, procéder à l'upload normal
        uploadImage(context, imageFile, new UploadCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
                Log.d(TAG, "On a un success " );
            }

            @Override
            public void onError(String error) {
                // En cas d'erreur malgré un réseau apparemment bon, ajouter à la file
                Log.d(TAG, "Échec d'upload, ajout à la file d'attente: " + error);
                UploadQueueManager.getInstance(context).addToQueue(imageFile);
                callback.onError("Erreur d'envoi. L'image sera réessayée plus tard.");
            }
        });
    }*/

    public static void uploadImageWithRetry(Context context, File imageFile,String numTrans, final UploadCallback callback) {
        // Ne plus vérifier la connexion ici, toujours mettre dans la file d'attente
        Log.d(TAG, "Ajout de la feuille de soins à la file d'attente pour traitement en arrière-plan");

        // Ajouter à la file d'attente
        boolean added = UploadQueueManager.getInstance(context).addToQueue(imageFile,numTrans);

        if (added) {
            // Informer l'utilisateur que la feuille a été enregistrée
            Log.d(TAG, "Feuille de soins ajoutée avec succès à la file d'attente");

            // Démarrer la synchronisation en arrière-plan
            UploadQueueManager.getInstance(context).startRetryScan();

            // Notifier le succès de l'enregistrement
            callback.onSuccess();
        } else {
            // En cas d'erreur lors de l'ajout à la file
            Log.e(TAG, "Erreur lors de l'ajout à la file d'attente");
            callback.onError("Erreur lors de l'enregistrement de la feuille de soins.");
        }
    }

    /**
     * Affiche un message d'erreur sous forme de toast
     */
    public static void showErrorToast(Context context, String error) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
    }
}