package com.example.guestme.utils;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocationData {
    // Using a different endpoint that's more reliable
    private static final String COUNTRIES_API = "https://restcountries.com/v2/all";
    private static Map<String, List<String>> CITIES_BY_COUNTRY = new TreeMap<>();
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();
    private static boolean isInitialized = false;

    public static void initialize(Context context, LocationDataCallback callback) {
        if (isInitialized) {
            callback.onInitialized(true);
            return;
        }

        // Fallback to static data if API fails
        initializeStaticData();
        
        // Keep a weak reference to the callback
        final WeakReference<LocationDataCallback> weakCallback = new WeakReference<>(callback);

        Request request = new Request.Builder()
                .url(COUNTRIES_API)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("LocationData", "Failed to fetch countries", e);
                LocationDataCallback cb = weakCallback.get();
                if (cb != null) {
                    // Use static data as fallback
                    cb.onInitialized(true);
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                LocationDataCallback cb = weakCallback.get();
                if (cb == null) return;

                if (!response.isSuccessful()) {
                    cb.onInitialized(true);
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray countries = new JSONArray(responseData);

                    // Don't clear existing cities, just add countries that don't have cities yet
                    for (int i = 0; i < countries.length(); i++) {
                        JSONObject country = countries.getJSONObject(i);
                        String countryName = country.getString("name");
                        if (!CITIES_BY_COUNTRY.containsKey(countryName)) {
                            // Only add countries that don't have cities defined
                            CITIES_BY_COUNTRY.put(countryName, new ArrayList<>());
                        }
                    }

                    isInitialized = true;
                    cb.onInitialized(true);

                } catch (JSONException e) {
                    Log.e("LocationData", "Error parsing countries JSON", e);
                    cb.onInitialized(true);
                }
            }
        });
    }

    private static void initializeStaticData() {
        // Initialize with static data as fallback
        CITIES_BY_COUNTRY.clear(); // Clear existing data first
        
        // Add more countries and cities
        addCountry("United States",
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
            "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose",
            "Austin", "Boston", "Seattle", "Denver", "Washington DC"
        );
        
        addCountry("Brazil",
            "São Paulo", "Rio de Janeiro", "Brasília", "Salvador",
            "Fortaleza", "Belo Horizonte", "Manaus", "Curitiba",
            "Recife", "Porto Alegre", "Belém", "Goiânia", "Guarulhos"
        );
        
        addCountry("United Kingdom",
            "London", "Manchester", "Birmingham", "Glasgow",
            "Liverpool", "Edinburgh", "Leeds", "Bristol",
            "Sheffield", "Newcastle", "Nottingham", "Cardiff"
        );

        addCountry("Canada",
            "Toronto", "Montreal", "Vancouver", "Calgary",
            "Ottawa", "Edmonton", "Quebec City", "Winnipeg",
            "Hamilton", "Halifax", "Victoria", "London"
        );

        addCountry("Australia",
            "Sydney", "Melbourne", "Brisbane", "Perth",
            "Adelaide", "Gold Coast", "Newcastle", "Canberra",
            "Wollongong", "Hobart", "Darwin", "Cairns"
        );

        addCountry("France",
            "Paris", "Marseille", "Lyon", "Toulouse", "Nice",
            "Nantes", "Strasbourg", "Montpellier", "Bordeaux", "Lille"
        );

        addCountry("Germany",
            "Berlin", "Hamburg", "Munich", "Cologne", "Frankfurt",
            "Stuttgart", "Düsseldorf", "Leipzig", "Dortmund", "Dresden"
        );

        addCountry("Italy",
            "Rome", "Milan", "Naples", "Turin", "Palermo",
            "Genoa", "Bologna", "Florence", "Venice", "Verona"
        );

        addCountry("Spain",
            "Madrid", "Barcelona", "Valencia", "Seville", "Zaragoza",
            "Málaga", "Murcia", "Palma", "Bilbao", "Alicante"
        );

        addCountry("Portugal",
            "Lisbon", "Porto", "Vila Nova de Gaia", "Amadora",
            "Braga", "Setúbal", "Coimbra", "Funchal", "Évora", "Faro"
        );

        isInitialized = true;
    }

    private static void addCountry(String country, String... cities) {
        CITIES_BY_COUNTRY.put(country, Arrays.asList(cities));
    }

    public static List<String> getCountries() {
        if (!isInitialized) {
            initializeStaticData(); // Initialize if not already done
        }
        return new ArrayList<>(CITIES_BY_COUNTRY.keySet());
    }

    public static List<String> getCitiesForCountry(String country) {
        if (!isInitialized) {
            initializeStaticData(); // Initialize if not already done
        }
        List<String> cities = CITIES_BY_COUNTRY.getOrDefault(country, new ArrayList<>());
        Collections.sort(cities); // Sort cities alphabetically
        return cities;
    }

    public interface LocationDataCallback {
        void onInitialized(boolean success);
    }
} 