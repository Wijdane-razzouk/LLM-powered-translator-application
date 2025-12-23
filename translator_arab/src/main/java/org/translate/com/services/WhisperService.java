package org.translate.com.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.translate.com.config.EnvConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class WhisperService {

    // Default to OpenAI if not set, but allow override via env var
    private static final String DEFAULT_WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String WHISPER_MODEL = "whisper-1";

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String whisperApiUrl;
    private final ObjectMapper objectMapper;

    public WhisperService(String apiKey) {
        this.apiKey = resolveApiKey(apiKey);
        this.objectMapper = new ObjectMapper();

        // Check for local URL override
        String envUrl = EnvConfig.get("WHISPER_API_URL");
        this.whisperApiUrl = (envUrl != null && !envUrl.isBlank()) ? envUrl : DEFAULT_WHISPER_API_URL;

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // Increased timeout for local processing
                .readTimeout(120, TimeUnit.SECONDS) // Increased timeout for local processing
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public WhisperService() {
        this(EnvConfig.get("OPENAI_API_KEY"));
    }

    public String transcribeAudio(String audioBase64, String sourceLanguage) throws IOException {
        return transcribeAudio(audioBase64, sourceLanguage, null);
    }

    public String transcribeAudio(String audioBase64, String sourceLanguage, String audioMimeType) throws IOException {
        boolean isLocal = !DEFAULT_WHISPER_API_URL.equals(whisperApiUrl);
        if (!isLocal && (apiKey == null || apiKey.isBlank())) {
            throw new IOException("OPENAI_API_KEY is missing and no local WHISPER_API_URL is configured.");
        }

        byte[] audioBytes = Base64.getDecoder().decode(audioBase64);
        String suffix = resolveExtensionFromMimeType(audioMimeType);
        File tempFile = File.createTempFile("whisper_audio_", suffix);
        try {
            Files.write(tempFile.toPath(), audioBytes);
            return transcribeAudio(tempFile, sourceLanguage);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public String transcribeAudio(File audioFile, String sourceLanguage) throws IOException {
        boolean isLocal = !DEFAULT_WHISPER_API_URL.equals(whisperApiUrl);
        if (!isLocal && (apiKey == null || apiKey.isBlank())) {
            throw new IOException("OPENAI_API_KEY is missing and no local WHISPER_API_URL is configured.");
        }

        if (!audioFile.exists()) {
            throw new IOException("Audio file not found: " + audioFile.getPath());
        }

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", WHISPER_MODEL)
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, MediaType.parse(resolveMediaType(audioFile))))
                .addFormDataPart("response_format", "json")
                .addFormDataPart("temperature", "0.0");

        String language = mapToWhisperLanguage(sourceLanguage);
        if (language != null && !language.isBlank()) {
            builder.addFormDataPart("language", language);
        }

        RequestBody requestBody = builder.build();

        Request.Builder requestBuilder = new Request.Builder()
                .url(whisperApiUrl)
                .post(requestBody);

        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No body";
                throw new IOException(
                        "Whisper API error: " + response.code() + " - " + response.message() + " \nBody: " + errorBody);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode textNode = jsonNode.get("text");
            return textNode == null ? "" : textNode.asText().trim();
        }
    }

    private String resolveMediaType(File audioFile) {
        String name = audioFile.getName().toLowerCase();
        if (name.endsWith(".wav")) {
            return "audio/wav";
        }
        if (name.endsWith(".mp3") || name.endsWith(".mpeg")) {
            return "audio/mpeg";
        }
        if (name.endsWith(".webm")) {
            return "audio/webm";
        }
        if (name.endsWith(".ogg")) {
            return "audio/ogg";
        }
        if (name.endsWith(".m4a")) {
            return "audio/mp4";
        }
        return "application/octet-stream";
    }

    private String resolveExtensionFromMimeType(String audioMimeType) {
        if (audioMimeType == null || audioMimeType.isBlank()) {
            return ".wav";
        }
        String baseType = audioMimeType.split(";", 2)[0].trim().toLowerCase();
        switch (baseType) {
            case "audio/webm":
                return ".webm";
            case "audio/ogg":
            case "audio/opus":
                return ".ogg";
            case "audio/mpeg":
            case "audio/mp3":
                return ".mp3";
            case "audio/mp4":
            case "audio/m4a":
            case "audio/x-m4a":
                return ".m4a";
            case "audio/wav":
            case "audio/wave":
            case "audio/x-wav":
                return ".wav";
            default:
                return ".wav";
        }
    }

    private String mapToWhisperLanguage(String languageCode) {
        if (languageCode == null || "auto".equalsIgnoreCase(languageCode)) {
            return null;
        }

        String lowerCode = languageCode.toLowerCase();
        if (lowerCode.equals("fr") || lowerCode.equals("fra") || lowerCode.equals("fre")) {
            return "fr";
        } else if (lowerCode.equals("en") || lowerCode.equals("eng")) {
            return "en";
        } else if (lowerCode.equals("es") || lowerCode.equals("spa")) {
            return "es";
        } else if (lowerCode.equals("de") || lowerCode.equals("deu") || lowerCode.equals("ger")) {
            return "de";
        } else if (lowerCode.equals("it") || lowerCode.equals("ita")) {
            return "it";
        } else if (lowerCode.equals("pt") || lowerCode.equals("por")) {
            return "pt";
        } else if (lowerCode.equals("nl") || lowerCode.equals("nld") || lowerCode.equals("dut")) {
            return "nl";
        } else if (lowerCode.equals("ru") || lowerCode.equals("rus")) {
            return "ru";
        } else if (lowerCode.equals("ja") || lowerCode.equals("jpn")) {
            return "ja";
        } else if (lowerCode.equals("ko") || lowerCode.equals("kor")) {
            return "ko";
        } else if (lowerCode.equals("zh") || lowerCode.equals("chi") || lowerCode.equals("zho")) {
            return "zh";
        } else if (lowerCode.equals("ar") || lowerCode.equals("ara")) {
            return "ar";
        } else if (lowerCode.equals("ary") || lowerCode.equals("ar-ma")) {
            return "ar";
        } else {
            return languageCode.length() > 2 ? languageCode.substring(0, 2) : languageCode;
        }
    }

    private String resolveApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return apiKey;
    }

    public boolean isAvailable() {
        boolean isLocal = !DEFAULT_WHISPER_API_URL.equals(whisperApiUrl);
        return isLocal || (apiKey != null && !apiKey.isBlank());
    }

    public void shutdown() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}
