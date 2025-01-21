package com.example.guestme;

//Classe para fazer o upload das imagens no Cloudinary

import okhttp3.*;

import java.io.File;
import java.io.IOException;

public class CloudinaryUploader {

    private static final String UPLOAD_URL = "https://api.cloudinary.com/v1_1/devugi42e/image/upload";
    private static final String UPLOAD_PRESET = "default_preset";

    public static void uploadImage(File imageFile, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        // Cria o corpo da requisição com o arquivo e o preset
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(), RequestBody.create(imageFile, MediaType.parse("image/*")))
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build();

        // Cria a requisição HTTP
        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build();

        // Envia a requisição
        client.newCall(request).enqueue(callback);
    }
}
