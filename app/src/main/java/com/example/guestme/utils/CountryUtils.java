package com.example.guestme.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

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
        Map<String, String> countryCodes = new HashMap<>();
        // Add more comprehensive list of country codes
        countryCodes.put("United States", "+1");
        countryCodes.put("Brazil", "+55");
        countryCodes.put("United Kingdom", "+44");
        countryCodes.put("France", "+33");
        countryCodes.put("Germany", "+49");
        countryCodes.put("Italy", "+39");
        countryCodes.put("Spain", "+34");
        countryCodes.put("Portugal", "+351");
        countryCodes.put("Canada", "+1");
        countryCodes.put("Mexico", "+52");
        countryCodes.put("Argentina", "+54");
        countryCodes.put("China", "+86");
        countryCodes.put("Japan", "+81");
        countryCodes.put("South Korea", "+82");
        countryCodes.put("India", "+91");
        countryCodes.put("Australia", "+61");
        countryCodes.put("Russia", "+7");
        
        return countryCodes.get(countryName);
    }

    public static String formatPhoneNumber(String phoneNumber, String countryName, PhoneNumberUtil phoneNumberUtil) {
        try {
            // If number already has country code, use it directly
            if (phoneNumber.startsWith("+")) {
                return phoneNumber;
            }
            
            // Get country code for selected country
            String countryCode = getCountryCode(countryName);
            if (countryCode == null) {
                return phoneNumber; // Return original if country code not found
            }
            
            // Remove any existing + or country code if present
            phoneNumber = phoneNumber.replaceAll("^\\+?\\d{1,3}", "");
            
            // Combine country code and phone number
            String fullNumber = countryCode + phoneNumber;
            
            // Parse and format the number
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(fullNumber, null);
            if (phoneNumberUtil.isValidNumber(number)) {
                return phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
            }
            
            return fullNumber;
        } catch (NumberParseException e) {
            return phoneNumber; // Return original if parsing fails
        }
    }
} 