package com.example.guestme;

import org.json.JSONException;
import org.json.JSONObject;

public class CloudinaryUtils {
    private String extractImageUrlFromResponse(String responseBody) {
        try {
            JSONObject jsonResponse = null;
            try {
                jsonResponse = new JSONObject(responseBody);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return jsonResponse.getString("secure_url"); // Obtém a URL segura da imagem
        } catch (JSONException e) {
            e.printStackTrace();
            return null; // Retorna null se ocorrer um erro
        }
    }

}
