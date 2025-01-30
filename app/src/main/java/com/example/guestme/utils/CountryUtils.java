package com.example.guestme.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class CountryUtils {
    private static final Map<String, String> countryCodeMap = new HashMap<>();

    static {
        // Initialize the map with country names and their ISO codes
        for (String countryCode : Locale.getISOCountries()) {
            Locale locale = new Locale("", countryCode);
            countryCodeMap.put(locale.getDisplayCountry(), countryCode);
        }
    }

    public static String getCountryFlag(String countryCode) {
        // Convert country code to flag emoji
        int firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
    }

    public static String getCountryCode(String countryName) {
        // Get the ISO 3166-1 alpha-2 country code from country name
        return countryCodeMap.get(countryName);
    }
} 