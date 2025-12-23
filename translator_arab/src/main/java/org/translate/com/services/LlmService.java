package org.translate.com.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.translate.com.config.EnvConfig;

public class LlmService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;
    private final String model;

    public LlmService() {
        this.apiKey = EnvConfig.get("MISTRAL_API_KEY");
        this.model = EnvConfig.getOrDefault("MISTRAL_MODEL", "mistral-large-latest");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MISTRAL_API_KEY is missing");
        }
    }

    /* ============================
       PUBLIC API
       ============================ */

    public String translate(String text) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        return callMistral(text);
    }

    public String translate(String text, String sourceLanguage, String targetLanguage) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        if (!isEnglish(sourceLanguage)) {
            throw new IllegalArgumentException("Only English source is supported");
        }
        if (!isDarija(targetLanguage)) {
            throw new IllegalArgumentException("Only Darija target is supported");
        }
        return callMistral(text);
    }

    /* ============================
       MISTRAL AI CLOUD
       ============================ */

    private String callMistral(String text) throws Exception {

        String prompt = buildDarijaPrompt(text);

        JSONObject body = new JSONObject()
            .put("model", model)
            .put("temperature", 0.2)
            .put("messages", new JSONArray()
                .put(new JSONObject()
                    .put("role", "user")
                    .put("content", prompt)
                )
            );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("https://api.mistral.ai/v1/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Mistral call failed: " + e, e);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Mistral API error: " + response.body());
        }

        JSONObject json = new JSONObject(response.body());

        return json
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim();
    }

    /* ============================
       PROMPT (DARJA OPTIMIZED)
       ============================ */

private String buildDarijaPrompt(String text) {
    return "You are a native Moroccan speaker.\n\n"+
        "Translate the following English text into Moroccan Arabic (Darija). " +
                "Return ONLY the translation, no explanations.\n\n" +
    text ;
       
}

    private boolean isEnglish(String lang) {
        if (lang == null || lang.isBlank()) return true;
        String v = lang.trim().toLowerCase(Locale.ROOT);
        return v.equals("en") || v.startsWith("en") || v.equals("english");
    }

    private boolean isDarija(String lang) {
        if (lang == null || lang.isBlank()) return true;
        String v = lang.trim().toLowerCase(Locale.ROOT);
        return v.equals("ary") || v.equals("darija") || v.equals("moroccan");
    }
}
