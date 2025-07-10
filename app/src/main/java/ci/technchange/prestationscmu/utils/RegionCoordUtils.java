package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RegionCoordUtils {
    private static final String TAG = "RegionCoordUtils";
    private final Map<String, RegionData> regionDataMap = new HashMap<>();

    // Classe interne pour stocker les données d'une région
    private static class RegionData {
        double lat;
        double lng;
        int id;

        RegionData(double lat, double lng, int id) {
            this.lat = lat;
            this.lng = lng;
            this.id = id;
        }
    }

    public RegionCoordUtils(Context context) {
        loadRegionCoordinates(context);
    }

    private void loadRegionCoordinates(Context context) {
        try {
            String jsonContent = readJsonFile(context, "region_coord.json");
            JSONArray jsonArray = new JSONArray(jsonContent);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject region = jsonArray.getJSONObject(i);
                String regionName = region.getString("region");
                double lat = region.getDouble("lat");
                double lng = region.getDouble("lng");
                int id = region.getInt("id");

                // Stocker dans la map avec la nouvelle structure
                regionDataMap.put(regionName, new RegionData(lat, lng, id));
                Log.d(TAG, "Région chargée: " + regionName + " [" + lat + ", " + lng + "], ID: " + id);
            }

            Log.d(TAG, "Chargement terminé: " + regionDataMap.size() + " régions chargées");
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Erreur lors du chargement des coordonnées de région", e);
        }
    }

    private String readJsonFile(Context context, String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream is = context.getAssets().open(filename);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        br.close();
        return sb.toString();
    }

    /**
     * Récupère les coordonnées d'une région spécifique
     * @param regionName Nom de la région
     * @return tableau de coordonnées [latitude, longitude] ou null si la région n'existe pas
     */
    public double[] getCoordinatesForRegion(String regionName) {
        RegionData data = findRegionData(regionName);
        if (data != null) {
            return new double[]{data.lat, data.lng};
        }
        return null;
    }

    /**
     * Récupère l'ID d'une région spécifique
     * @param regionName Nom de la région
     * @return ID de la région ou -1 si la région n'existe pas
     */
    public int getIdForRegion(String regionName) {
        RegionData data = findRegionData(regionName);
        if (data != null) {
            return data.id;
        }
        return -1; // Retourne -1 si la région n'est pas trouvée
    }

    /**
     * Méthode privée pour rechercher une région par son nom
     * @param regionName Nom de la région
     * @return Objet RegionData ou null si non trouvé
     */
    private RegionData findRegionData(String regionName) {
        if (regionName == null || regionName.isEmpty()) {
            Log.w(TAG, "Nom de région vide ou null");
            return null;
        }

        // Normaliser le nom de la région (supprimer les espaces supplémentaires, majuscules, etc.)
        String normalizedName = regionName.trim().toUpperCase();

        // Vérifier si la région existe telle quelle
        if (regionDataMap.containsKey(normalizedName)) {
            Log.d(TAG, "Données trouvées pour " + normalizedName);
            return regionDataMap.get(normalizedName);
        }

        // Si la région n'est pas trouvée exactement, essayer de trouver une correspondance partielle
        for (Map.Entry<String, RegionData> entry : regionDataMap.entrySet()) {
            String key = entry.getKey().toUpperCase();
            if (key.contains(normalizedName) || normalizedName.contains(key)) {
                Log.d(TAG, "Correspondance partielle trouvée pour " + regionName + " -> " + entry.getKey());
                return entry.getValue();
            }
        }

        Log.w(TAG, "Aucune donnée trouvée pour la région: " + regionName);
        return null;
    }
}
