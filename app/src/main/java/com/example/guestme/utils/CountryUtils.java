package com.example.guestme.utils;

public class CountryUtils {
    public static String getCountryFlag(String countryCode) {
        // Convert country code to uppercase to get flag emoji
        // Flag emoji is created by converting regional indicator symbols
        int firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
    }
} 