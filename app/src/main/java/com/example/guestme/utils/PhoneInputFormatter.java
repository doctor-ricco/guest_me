package com.example.guestme.utils;

import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import android.content.Context;

public class PhoneInputFormatter {
    private final PhoneNumberUtil phoneNumberUtil;
    private final TextInputLayout phoneInputLayout;
    private final TextInputEditText phoneInput;
    private String selectedCountryCode = "";

    public PhoneInputFormatter(Context context, TextInputLayout layout, TextInputEditText input) {
        this.phoneNumberUtil = PhoneNumberUtil.createInstance(context);
        this.phoneInputLayout = layout;
        this.phoneInput = input;
    }

    public void setCountryCode(String countryCode) {
        this.selectedCountryCode = countryCode;
        updateHint();
    }

    private void updateHint() {
        if (!selectedCountryCode.isEmpty()) {
            String flag = CountryUtils.getCountryFlag(selectedCountryCode);
            String prefix = getCountryPrefix(selectedCountryCode);
            phoneInput.setHint(prefix);
            phoneInputLayout.setPrefixText(flag);
        }
    }

    private String getCountryPrefix(String countryCode) {
        Phonenumber.PhoneNumber example = phoneNumberUtil.getExampleNumber(countryCode);
        if (example != null) {
            String fullNumber = phoneNumberUtil.format(example, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
            return fullNumber.split(" ")[0]; // Get just the prefix
        }
        return "";
    }

    public void setupPhoneNumberFormatting() {
        phoneInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String phoneNumber = s.toString();
                if (!phoneNumber.isEmpty() && !selectedCountryCode.isEmpty()) {
                    try {
                        // Try to parse the phone number
                        Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, selectedCountryCode);
                        String formattedNumber = phoneNumberUtil.format(number, 
                            PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                        
                        // Only update if the formatting is different
                        if (!phoneNumber.equals(formattedNumber)) {
                            phoneInput.removeTextChangedListener(this);
                            phoneInput.setText(formattedNumber);
                            phoneInput.setSelection(formattedNumber.length());
                            phoneInput.addTextChangedListener(this);
                        }
                    } catch (NumberParseException e) {
                        // Invalid number, but don't show error yet
                    }
                }
            }
        });
    }

    public String getFullPhoneNumber() {
        String phoneNumber = phoneInput.getText().toString();
        if (!phoneNumber.isEmpty() && !selectedCountryCode.isEmpty()) {
            try {
                Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, selectedCountryCode);
                return phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
            } catch (NumberParseException e) {
                return null;
            }
        }
        return null;
    }

    public boolean isValidPhoneNumber() {
        String phoneNumber = phoneInput.getText().toString();
        if (!phoneNumber.isEmpty() && !selectedCountryCode.isEmpty()) {
            try {
                Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, selectedCountryCode);
                return phoneNumberUtil.isValidNumber(number);
            } catch (NumberParseException e) {
                return false;
            }
        }
        return false;
    }
} 