package com.example.guestme;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.guestme.utils.CountryUtils;
import com.example.guestme.utils.LocationData;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class VisitorHomeFragment extends Fragment {
    private Uri selectedImageUri;
    private String uploadedImageUrl;
    private CircleImageView profileImage;
    private EditText fullNameInput, phoneInput, descriptionInput, addressInput;
    private MaterialAutoCompleteTextView countrySpinner;
    private MaterialAutoCompleteTextView citySpinner;
    private TextInputLayout phoneInputLayout;
    private PhoneNumberUtil phoneNumberUtil;
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    
    private boolean isEditing = false;
    private boolean isProfileComplete = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        profileImage.setImageURI(selectedImageUri);
                        uploadPhotoToCloudinary();
                    }
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LocationData.initialize(requireContext(), success -> {
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        if (!success) {
                            Toast.makeText(requireContext(), 
                                "Failed to load countries. Please check your internet connection.", 
                                Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        
        if (getArguments() != null) {
            isEditing = getArguments().getBoolean("isEditing", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_visitor_home, container, false);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews(view);
        
        if (!isEditing) {
            checkProfileCompletion();
        } else {
            loadProfileData();
        }

        return view;
    }

    private void initializeViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        fullNameInput = view.findViewById(R.id.fullNameInput);
        addressInput = view.findViewById(R.id.addressInput);
        phoneInput = view.findViewById(R.id.phoneInput);
        descriptionInput = view.findViewById(R.id.visitorDescription);
        countrySpinner = view.findViewById(R.id.country_spinner);
        citySpinner = view.findViewById(R.id.city_spinner);
        phoneInputLayout = view.findViewById(R.id.phoneInputLayout);

        Button uploadPhotoButton = view.findViewById(R.id.uploadPhotoButton);
        Button saveProfileButton = view.findViewById(R.id.saveProfileButton);

        phoneNumberUtil = PhoneNumberUtil.createInstance(requireContext());

        uploadPhotoButton.setOnClickListener(v -> openImagePicker());
        saveProfileButton.setOnClickListener(v -> saveProfile());

        setupPhoneNumberFormatting();
        setupCountrySpinner(countrySpinner, citySpinner);
    }

    private void saveProfile() {
        String userId = auth.getCurrentUser().getUid();
        String fullName = fullNameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String country = countrySpinner.getText().toString();
        String city = citySpinner.getText().toString();

        if (fullName.isEmpty() || phone.isEmpty() || description.isEmpty() || 
            country.isEmpty() || city.isEmpty() || address.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validatePhoneNumber(phone)) {
            return;
        }

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("fullName", fullName);
        profileData.put("address", address);
        profileData.put("phone", phone);
        profileData.put("description", description);
        profileData.put("country", country);
        profileData.put("city", city);
        if (uploadedImageUrl != null) {
            profileData.put("photoUrl", uploadedImageUrl);
        }

        firestore.collection("users")
                .document(userId)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    isProfileComplete = true;

                    Intent intent = new Intent(getActivity(), VisitorProfileActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to update profile: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadPhotoToCloudinary() {
        if (selectedImageUri != null) {
            try {
                String filePath = getRealPathFromURI(selectedImageUri);
                if (filePath != null) {
                    File imageFile = new File(filePath);
                    CloudinaryUploader.uploadImage(imageFile, new okhttp3.Callback() {
                        @Override
                        public void onFailure(okhttp3.Call call, IOException e) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getActivity(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseBody = response.body().string();
                                uploadedImageUrl = extractImageUrlFromResponse(responseBody);
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getActivity(), "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();
                                    updateProfileImage(uploadedImageUrl);
                                });
                            } else {
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getActivity(), "Failed to upload image.", Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(profileImage);
        }
    }

    private String getRealPathFromURI(Uri uri) {
        if (uri == null) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    cursor.moveToFirst();
                    String fileName = cursor.getString(nameIndex);
                    File file = new File(getActivity().getFilesDir(), fileName);
                    try (InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                         OutputStream outputStream = new java.io.FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                    cursor.close();
                    return file.getAbsolutePath();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Failed to process file", Toast.LENGTH_SHORT).show();
            }
        } else {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(columnIndex);
                cursor.close();
                return path;
            }
        }
        return null;
    }

    private String extractImageUrlFromResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json.getString("secure_url");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void checkProfileCompletion() {
        String userId = auth.getCurrentUser().getUid();
        
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Check if all required fields are present
                            boolean isProfileComplete = document.contains("fullName") &&
                                    document.contains("phone") &&
                                    document.contains("description") &&
                                    document.contains("country") &&
                                    document.contains("city") &&
                                    document.contains("address");

                            if (isProfileComplete) {
                                // If profile is complete, redirect to VisitorProfileActivity
                                Intent intent = new Intent(getActivity(), VisitorProfileActivity.class);
                                startActivity(intent);
                                getActivity().finish();
                            } else {
                                // If profile is not complete, load existing data (if any)
                                loadProfileData();
                            }
                        } else {
                            // New user, just stay on the current screen to fill out profile
                            Log.d("VisitorHomeFragment", "New user, creating profile");
                        }
                    } else {
                        Log.e("VisitorHomeFragment", "Error checking profile completion", task.getException());
                        Toast.makeText(getActivity(), 
                            "Error checking profile. Please try again.", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProfileData() {
        String userId = auth.getCurrentUser().getUid();
        
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String fullName = document.getString("fullName");
                        String address = document.getString("address");
                        String phone = document.getString("phone");
                        String description = document.getString("description");
                        String country = document.getString("country");
                        String city = document.getString("city");
                        String photoUrl = document.getString("photoUrl");

                        if (fullName != null) fullNameInput.setText(fullName);
                        if (address != null) addressInput.setText(address);
                        if (phone != null) phoneInput.setText(phone);
                        if (description != null) descriptionInput.setText(description);
                        if (country != null) countrySpinner.setText(country, false);
                        if (city != null) citySpinner.setText(city, false);
                        
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            uploadedImageUrl = photoUrl;
                            Picasso.get()
                                    .load(photoUrl)
                                    .placeholder(R.drawable.profile)
                                    .error(R.drawable.profile)
                                    .into(profileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(getActivity(), 
                        "Error loading profile data: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show());
    }

    private void setupPhoneNumberFormatting() {
        phoneInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String phoneNumber = s.toString();
                if (!phoneNumber.isEmpty()) {
                    try {
                        // Try to parse the phone number
                        Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, null);
                        String regionCode = phoneNumberUtil.getRegionCodeForNumber(number);
                        
                        if (regionCode != null) {
                            // Get the country flag emoji
                            String flag = CountryUtils.getCountryFlag(regionCode);
                            
                            // Format the phone number
                            String formattedNumber = phoneNumberUtil.format(number, 
                                PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                            
                            // Update the hint with the flag
                            phoneInputLayout.setHint("Phone Number " + flag);
                        }
                    } catch (NumberParseException e) {
                        // If parsing fails, just keep the default hint
                        phoneInputLayout.setHint("Phone Number");
                    }
                } else {
                    phoneInputLayout.setHint("Phone Number");
                }
            }
        });
    }

    private boolean validatePhoneNumber(String phoneNumber) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, null);
            if (!phoneNumberUtil.isValidNumber(number)) {
                Toast.makeText(getActivity(), "Please enter a valid phone number", 
                    Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (NumberParseException e) {
            Toast.makeText(getActivity(), "Please enter a valid phone number", 
                Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void setupCountrySpinner(MaterialAutoCompleteTextView countrySpinner, 
            MaterialAutoCompleteTextView citySpinner) {
        // Get the list of countries
        List<String> countries = LocationData.getCountries();
        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                countries
        );
        countrySpinner.setAdapter(countryAdapter);

        // Setup city spinner to update when country is selected
        countrySpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCountry = countryAdapter.getItem(position);
            updateCitySpinner(selectedCountry, citySpinner);
        });
    }

    private void updateCitySpinner(String country, MaterialAutoCompleteTextView citySpinner) {
        List<String> cities = LocationData.getCitiesForCountry(country);
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                cities
        );
        citySpinner.setAdapter(cityAdapter);
        citySpinner.setText("", false); // Clear the current selection
    }
} 