package com.pumpkiiings.pklogin.translator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeminiProvider implements TranslationProvider {

    private final String apiKey;
    private final String model;
    private final OkHttpClient client;
    private final Gson gson;

    public GeminiProvider(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) throws Exception {
        String prompt = String.format("Translate the following text from %s to %s. " +
                "Do NOT translate or modify any [M1], [M2] etc markers. " +
                "Only return the translated text without any quotes, markdown formatting, or explanations.\n\nText: %s",
                sourceLanguage, targetLanguage, text);

        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject part = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject textObj = new JsonObject();
        textObj.addProperty("text", prompt);
        parts.add(textObj);
        part.add("parts", parts);
        contents.add(part);
        payload.add("contents", contents);

        RequestBody body = RequestBody.create(
                gson.toJson(payload),
                MediaType.parse("application/json; charset=utf-8")
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            try {
                String translatedText = jsonResponse
                        .getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
                
                return translatedText.trim();
            } catch (Exception e) {
                throw new Exception("Failed to parse Gemini response: " + responseBody, e);
            }
        }
    }
}
