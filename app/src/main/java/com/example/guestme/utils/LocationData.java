package com.example.guestme.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationData {
    private static final Map<String, List<String>> CITIES_BY_COUNTRY = new HashMap<>();
    
    static {
        // United States
        List<String> usCities = new ArrayList<>();
        usCities.add("New York");
        usCities.add("Los Angeles");
        usCities.add("Chicago");
        usCities.add("Houston");
        usCities.add("Phoenix");
        CITIES_BY_COUNTRY.put("United States", usCities);
        
        // Brazil
        List<String> brCities = new ArrayList<>();
        brCities.add("São Paulo");
        brCities.add("Rio de Janeiro");
        brCities.add("Brasília");
        brCities.add("Salvador");
        brCities.add("Fortaleza");
        CITIES_BY_COUNTRY.put("Brazil", brCities);
        
        // Add more countries and cities as needed
    }
    
    public static List<String> getCountries() {
        return new ArrayList<>(CITIES_BY_COUNTRY.keySet());
    }
    
    public static List<String> getCitiesForCountry(String country) {
        return CITIES_BY_COUNTRY.getOrDefault(country, new ArrayList<>());
    }
} 