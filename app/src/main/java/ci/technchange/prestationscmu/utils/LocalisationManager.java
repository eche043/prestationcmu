package ci.technchange.prestationscmu.utils;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

/**
 * Gestionnaire centralisé pour toutes les fonctionnalités de localisation
 */
public class LocalisationManager {
    private static final String TAG = "LocalisationManager";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private LocationUpdateCallback callback;

    // Interface pour recevoir les mises à jour de localisation
    public interface LocationUpdateCallback {
        void onLocationUpdated(double latitude, double longitude);
        void onLocationError(String error);
        void onPermissionRequired();
    }

    public LocalisationManager(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        setupLocationListener();
    }

    /**
     * Configuration du listener de localisation
     */
    private void setupLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Log.d(TAG, "Nouvelle position - Lat: " + latitude + ", Long: " + longitude);

                // Notifier le callback si défini
                if (callback != null) {
                    callback.onLocationUpdated(latitude, longitude);
                }

                // Arrêter les mises à jour après avoir obtenu une position
                locationManager.removeUpdates(this);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG, "Status du provider " + provider + " changé: " + status);
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TAG, "Provider activé: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TAG, "Provider désactivé: " + provider);
            }
        };
    }

    /**
     * Définir le callback pour recevoir les mises à jour
     */
    public void setLocationUpdateCallback(LocationUpdateCallback callback) {
        this.callback = callback;
    }

    /**
     * Vérifier si les permissions de localisation sont accordées
     */
    public boolean hasLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Pour les versions antérieures à Android 6.0
    }

    /**
     * Vérifier si les fournisseurs de localisation sont activés
     */
    public boolean areLocationProvidersEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Demander les mises à jour de localisation
     */
    public void requestLocationUpdates() {
        // Vérifier les permissions
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Permissions de localisation non accordées");
            if (callback != null) {
                callback.onPermissionRequired();
            }
            return;
        }

        // Vérifier si les fournisseurs de localisation sont activés
        if (!areLocationProvidersEnabled()) {
            handleDisabledLocationProviders();
            return;
        }

        // Obtenir la dernière position connue
        getLastKnownLocation();

        // Demander des mises à jour régulières
        startLocationUpdates();
    }

    /**
     * Obtenir la dernière position connue
     */
    private void getLastKnownLocation() {
        try {
            Location lastKnownLocation = null;

            // Essayer d'abord avec le GPS (plus précis)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            // Si pas de localisation GPS, essayer avec le réseau
            if (lastKnownLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            // Utiliser la dernière position connue si disponible
            if (lastKnownLocation != null) {
                latitude = lastKnownLocation.getLatitude();
                longitude = lastKnownLocation.getLongitude();
                Log.d(TAG, "Dernière position connue - Lat: " + latitude + ", Long: " + longitude);

                if (callback != null) {
                    callback.onLocationUpdated(latitude, longitude);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Erreur de sécurité lors de la récupération de la dernière position", e);
            if (callback != null) {
                callback.onLocationError("Erreur de sécurité lors de la récupération de la position");
            }
        }
    }

    /**
     * Démarrer les mises à jour de localisation en temps réel
     */
    private void startLocationUpdates() {
        try {
            // Demander des mises à jour GPS
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,  // 5 secondes
                        10,    // 10 mètres
                        locationListener
                );
                Log.d(TAG, "Mises à jour GPS démarrées");
            }

            // Ajouter aussi les mises à jour réseau pour plus de fiabilité
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,  // 5 secondes
                        10,    // 10 mètres
                        locationListener
                );
                Log.d(TAG, "Mises à jour réseau démarrées");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Erreur de sécurité lors de la demande de mises à jour", e);
            if (callback != null) {
                callback.onLocationError("Erreur de sécurité lors de la demande de mises à jour");
            }
        }
    }

    /**
     * Gérer le cas où les fournisseurs de localisation sont désactivés
     */
    private void handleDisabledLocationProviders() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pour Android 9 et versions antérieures, essayer d'activer automatiquement
            try {
                Settings.Secure.putInt(context.getContentResolver(),
                        Settings.Secure.LOCATION_MODE,
                        Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);

                Log.d(TAG, "Fournisseurs de localisation activés automatiquement");

                // Réessayer après activation
                if (areLocationProvidersEnabled()) {
                    requestLocationUpdates();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Impossible d'activer automatiquement les fournisseurs", e);
                showEnableLocationDialog();
            }
        } else {
            // Pour Android 10+, rediriger vers les paramètres
            Log.w(TAG, "Activation automatique impossible sur Android 10+");
            showEnableLocationDialog();
        }
    }

    /**
     * Afficher une boîte de dialogue pour activer la localisation
     */
    public void showEnableLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Localisation désactivée")
                .setMessage("La localisation GPS est désactivée. Voulez-vous l'activer maintenant?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Rediriger vers les paramètres de localisation
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(intent);
                })
                .setNegativeButton("Non", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(context, "La localisation est nécessaire pour cette fonctionnalité", Toast.LENGTH_LONG).show();
                    if (callback != null) {
                        callback.onLocationError("Localisation désactivée par l'utilisateur");
                    }
                })
                .create()
                .show();
    }

    /**
     * Arrêter les mises à jour de localisation
     */
    public void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
                Log.d(TAG, "Mises à jour de localisation arrêtées");
            } catch (SecurityException e) {
                Log.e(TAG, "Erreur lors de l'arrêt des mises à jour", e);
            }
        }
    }

    /**
     * Obtenir la latitude actuelle
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Obtenir la longitude actuelle
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Vérifier si une position valide est disponible
     */
    public boolean hasValidLocation() {
        return latitude != 0.0 && longitude != 0.0;
    }

    /**
     * Obtenir la position sous forme de chaîne formatée
     */
    public String getLocationString() {
        if (hasValidLocation()) {
            return "Lat: " + latitude + ", Long: " + longitude;
        }
        return "Position non disponible";
    }

    /**
     * Définir manuellement une position (pour les tests ou cas spéciaux)
     */
    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        Log.d(TAG, "Position définie manuellement: " + getLocationString());

        if (callback != null) {
            callback.onLocationUpdated(latitude, longitude);
        }
    }

    /**
     * Réinitialiser la position
     */
    public void resetLocation() {
        this.latitude = 0.0;
        this.longitude = 0.0;
        Log.d(TAG, "Position réinitialisée");
    }

    /**
     * Nettoyer les ressources
     */
    public void cleanup() {
        stopLocationUpdates();
        callback = null;
        Log.d(TAG, "Ressources nettoyées");
    }
}
