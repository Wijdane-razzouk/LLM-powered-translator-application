package org.translate.com.services;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.translate.com.config.EnvConfig;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TTSService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String ttsApiUrl;

    public TTSService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        this.objectMapper = new ObjectMapper();

        // Optional: Local/Custom TTS Endpoint
        this.ttsApiUrl = EnvConfig.get("TTS_API_URL");
    }

    /**
     * Synthétiser du texte en audio avec Google TTS (gratuit)
     */
    public String synthesizeWithGoogleTTS(String text, String language, String voiceType) throws IOException {
        try {
            // Google TTS API (gratuit jusqu'à 1 million de caractères/mois)
            String googleTtsUrl = "https://texttospeech.googleapis.com/v1/text:synthesize";

            // Créer le payload JSON
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> input = new HashMap<>();
            input.put("text", text);

            Map<String, Object> voiceConfig = new HashMap<>();
            voiceConfig.put("languageCode", mapLanguageCode(language));
            voiceConfig.put("name", getGoogleVoice(language, voiceType));
            voiceConfig.put("ssmlGender", getGender(voiceType));

            Map<String, Object> audioConfig = new HashMap<>();
            audioConfig.put("audioEncoding", "MP3");
            audioConfig.put("speakingRate", 1.0);
            audioConfig.put("pitch", 0.0);

            payload.put("input", input);
            payload.put("voice", voiceConfig);
            payload.put("audioConfig", audioConfig);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Note: Google TTS nécessite une API key
            // Pour une solution gratuite sans API key, voir la méthode alternative
            // ci-dessous

            RequestBody body = RequestBody.create(
                    jsonPayload,
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(googleTtsUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Google TTS API error: " + response.code());
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                return jsonNode.get("audioContent").asText();
            }

        } catch (Exception e) {
            // Fallback à une méthode locale
            throw new IOException("Google TTS failure", e);
        }
    }

    /**
     * Alternative: Utiliser un service TTS local gratuit
     */
    private String synthesizeLocal(String text) {
        try {
            // Simulation simple - en production, utilisez FreeTTS ou autre
            byte[] audioBytes = generateSimpleAudio(text);
            return Base64.getEncoder().encodeToString(audioBytes);
        } catch (Exception e) {
            // Dernier recours: retourner un message d'erreur encodé
            return Base64.getEncoder().encodeToString(
                    ("Audio non disponible pour: " + text).getBytes());
        }
    }

    /**
     * Utiliser Edge TTS (Microsoft) - Gratuit
     */
    public String synthesizeWithEdgeTTS(String text, String language) throws IOException {
        try {
            // Edge TTS via un proxy public (exemple)
            String edgeTtsUrl = "https://edge-tts-proxy.vercel.app/api/tts";

            Map<String, String> params = new HashMap<>();
            params.put("text", text);
            params.put("language", language);
            params.put("voice", getEdgeVoice(language));

            HttpUrl.Builder urlBuilder = HttpUrl.parse(edgeTtsUrl).newBuilder();
            params.forEach(urlBuilder::addQueryParameter);

            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Edge TTS error: " + response.code());
                }

                // Edge TTS retourne directement l'audio
                byte[] audioBytes = response.body().bytes();
                return Base64.getEncoder().encodeToString(audioBytes);
            }

        } catch (Exception e) {
            System.err.println("Edge TTS failed: " + e.getMessage());
            throw new IOException("Edge TTS failed", e);
        }
    }

    public String synthesizeWithCustomEndpoint(String text, String language) throws IOException {
        if (ttsApiUrl == null || ttsApiUrl.isBlank()) {
            throw new IOException("TTS_API_URL not configured");
        }

        try {
            // Create a generic JSON payload.
            // Many local TTS servers (like OpenAI-compatible ones) expect "input" or "text"
            // and "voice".
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "tts-1");
            payload.put("input", text); // OpenAI format uses 'input'
            payload.put("voice", "alloy"); // Default voice, can be parameterized if needed

            // Some might use 'text' instead of 'input', we can add both or check the
            // specific API.
            // For a generic implementation, let's stick to OpenAI format as a baseline dev
            // standard.

            String jsonPayload = objectMapper.writeValueAsString(payload);

            RequestBody body = RequestBody.create(
                    jsonPayload,
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(ttsApiUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Custom TTS API error: " + response.code() + " - " + response.message());
                }

                // If the response is binary audio (standard), encode to Base64
                if (response.body() != null) {
                    byte[] audioBytes = response.body().bytes();
                    return Base64.getEncoder().encodeToString(audioBytes);
                }
                return "";
            }
        } catch (Exception e) {
            throw new IOException("Custom TTS failed: " + e.getMessage(), e);
        }
    }

    /**
     * Méthode principale de synthèse avec fallback
     */
    public String synthesizeText(String text, String language, String voiceType) {
        // 1. Try Custom/Local Endpoint first if configured
        if (ttsApiUrl != null && !ttsApiUrl.isBlank()) {
            try {
                return synthesizeWithCustomEndpoint(text, language);
            } catch (Exception e) {
                System.err.println("Custom TTS failed, falling back...");
            }
        }

        // 2. Try Edge TTS (Free)
        try {
            return synthesizeWithEdgeTTS(text, language);
        } catch (Exception e1) {
            try {
                // 3. Try Google TTS
                return synthesizeWithGoogleTTS(text, language, voiceType);
            } catch (Exception e2) {
                // 4. Fallback to local simulation
                return synthesizeLocal(text);
            }
        }
    }

    // Méthodes utilitaires
    private String mapLanguageCode(String lang) {
        if (lang == null)
            return "en-US";
        String lowerLang = lang.toLowerCase();
        switch (lowerLang) {
            case "fr":
            case "fra":
                return "fr-FR";
            case "en":
            case "eng":
                return "en-US";
            case "es":
            case "spa":
                return "es-ES";
            case "ar":
            case "ara":
            case "ary":
                return "ar-SA";
            default:
                return lang + "-" + lang.toUpperCase();
        }
    }

    private String getGoogleVoice(String language, String voiceType) {
        String code = mapLanguageCode(language);
        if ("male".equalsIgnoreCase(voiceType)) {
            return code.replace("-", "-Standard-B");
        }
        return code.replace("-", "-Standard-A");
    }

    private String getEdgeVoice(String language) {
        String lowerLang = language.toLowerCase();
        switch (lowerLang) {
            case "fr":
                return "fr-FR-DeniseNeural";
            case "en":
                return "en-US-JennyNeural";
            case "es":
                return "es-ES-ElviraNeural";
            case "ar":
                return "ar-SA-ZariyahNeural";
            default:
                return "en-US-JennyNeural";
        }
    }

    private String getGender(String voiceType) {
        if ("male".equalsIgnoreCase(voiceType)) {
            return "MALE";
        }
        return "FEMALE";
    }

    private byte[] generateSimpleAudio(String text) {
        // Générer un son simple (pour le fallback)
        String audioContent = "Audio simulé pour: " + text;
        return audioContent.getBytes();
    }

}
