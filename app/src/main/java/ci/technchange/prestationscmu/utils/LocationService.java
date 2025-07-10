package ci.technchange.prestationscmu.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import org.json.JSONObject;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;
import ci.technchange.prestationscmu.utils.SharedPrefManager;

public class LocationService extends Service implements LocationListener {
    private static final String TAG = "LocationService";
    private static final int LOCATION_INTERVAL = 15 * 60 * 1000; // 15 minutes
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "location_channel";

    private LocationManager locationManager;
    private Handler handler;
    private Runnable locationRunnable;
    private UtilsInfosAppareil utilsInfos;
    private SharedPrefManager sharedPrefManager;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        utilsInfos = new UtilsInfosAppareil(this);
        sharedPrefManager = new SharedPrefManager(this);

        createNotificationChannel();
        setupPeriodicLocationUpdates();

        Log.d(TAG, "LocationService créé");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Service de localisation",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Suivi de la position toutes les 15 minutes");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Suivi de localisation actif")
                .setContentText("Position envoyée toutes les 15 minutes")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Utilisez votre icône
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void setupPeriodicLocationUpdates() {
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                getCurrentLocationAndSend();
                // Programmer la prochaine exécution dans 15 minutes
                handler.postDelayed(this, LOCATION_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Démarrer en tant que service foreground
        startForeground(NOTIFICATION_ID, createNotification());

        // Commencer les mises à jour de localisation périodiques
        handler.post(locationRunnable);

        Log.d(TAG, "Service de localisation démarré");
        return START_STICKY; // Redémarrer automatiquement si tué par le système
    }

    private void getCurrentLocationAndSend() {
        if (!hasLocationPermissions()) {
            Log.e(TAG, "Permissions de localisation manquantes");
            return;
        }

        try {
            // Essayer d'obtenir la dernière position connue
            Location lastKnownLocation = getLastKnownLocation();

            if (lastKnownLocation != null) {
                latitude = lastKnownLocation.getLatitude();
                longitude = lastKnownLocation.getLongitude();
                Log.d(TAG, "Position trouvée - Lat: " + latitude + ", Long: " + longitude);
                sendLocationToAPI(lastKnownLocation);
            } else {
                // Si pas de position connue, demander une mise à jour
                requestSingleLocationUpdate();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Erreur de sécurité lors de l'accès à la localisation", e);
        }
    }

    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private Location getLastKnownLocation() {
        if (!hasLocationPermissions()) {
            return null;
        }

        Location bestLocation = null;

        // Essayer d'abord avec le GPS (plus précis)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gpsLocation != null) {
                bestLocation = gpsLocation;
            }
        }

        // Si pas de GPS, essayer avec le réseau
        if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        return bestLocation;
    }

    private void requestSingleLocationUpdate() {
        if (!hasLocationPermissions()) {
            return;
        }

        try {
            // Demander une mise à jour unique
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
            }

            Log.d(TAG, "Demande de mise à jour de localisation unique");
        } catch (SecurityException e) {
            Log.e(TAG, "Erreur lors de la demande de mise à jour", e);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        Log.d(TAG, "Nouvelle position reçue - Lat: " + latitude + ", Long: " + longitude);

        sendLocationToAPI(location);

        // Arrêter les mises à jour car on a obtenu une position
        if (hasLocationPermissions()) {
            locationManager.removeUpdates(this);
        }
    }

    private void sendLocationToAPI(Location location) {
        new Thread(() -> {
            try {
                // Remplacez par votre URL d'API
                String url = "https://votre-api.com/location";

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000); // 30 secondes timeout
                connection.setReadTimeout(30000);

                Log.d("LOCATIONSERVICE---",utilsInfos.recupererIdAppareil());
                Log.d("LOCATIONSERVICE---", String.valueOf(location.getLatitude()));
                Log.d("LOCATIONSERVICE---", String.valueOf(location.getLongitude()));

                // Créer le JSON avec les données
                JSONObject jsonData = new JSONObject();
                jsonData.put("latitude", location.getLatitude());
                jsonData.put("longitude", location.getLongitude());
                jsonData.put("timestamp", System.currentTimeMillis());
                jsonData.put("accuracy", location.getAccuracy());
                jsonData.put("device_id", utilsInfos.recupererIdAppareil());
                jsonData.put("code_ets", sharedPrefManager.getCodeEts());
                jsonData.put("agent_matricule", sharedPrefManager.getCodeAgent());

                // Envoyer les données
                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                    writer.write(jsonData.toString());
                    writer.flush();
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Position envoyée à l'API - Code de réponse: " + responseCode);

                if (responseCode >= 200 && responseCode < 300) {
                    Log.d(TAG, "Position envoyée avec succès");
                } else {
                    Log.w(TAG, "Réponse API non optimale: " + responseCode);
                }

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'envoi de la position à l'API", e);
            }
        }).start();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

        // Arrêter les mises à jour de localisation
        if (locationManager != null && hasLocationPermissions()) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException e) {
                Log.e(TAG, "Erreur lors de l'arrêt des mises à jour", e);
            }
        }

        // Arrêter le handler
        if (handler != null && locationRunnable != null) {
            handler.removeCallbacks(locationRunnable);
        }

        Log.d(TAG, "LocationService détruit");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service non lié
    }

    // Méthodes requises par LocationListener (pas utilisées dans ce contexte)
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Statut du fournisseur changé: " + provider + " - " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Fournisseur activé: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Fournisseur désactivé: " + provider);
    }
}