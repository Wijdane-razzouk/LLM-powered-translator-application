package org.translate.com.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.translate.com.config.EnvConfig;
import org.translate.com.dto.ImageTranslationRequest;
import org.translate.com.dto.ImageTranslationResponse;

public class ImageService {

    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String DEFAULT_MIME_TYPE = "image/png";

    private final LlmService llmService = new LlmService();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey = EnvConfig.get("GEMINI_API_KEY");
    private final String visionModel = EnvConfig.getOrDefault("GEMINI_VISION_MODEL", DEFAULT_MODEL);
    private final String tesseractPath = EnvConfig.get("TESSERACT_PATH");
    private final String tesseractLang = EnvConfig.get("TESSERACT_LANG");

    public ImageTranslationResponse translate(ImageTranslationRequest request) throws Exception {
        if (request == null || request.getImageBase64() == null || request.getImageBase64().isBlank()) {
            throw new IllegalArgumentException("imageBase64 is required");
        }

        String base64 = normalizeBase64(request.getImageBase64());
        String mimeType = normalizeMimeType(request.getImageMimeType(), request.getImageBase64());
        String extractedText = extractText(base64, mimeType, request.getSourceLanguage());
        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalStateException("No text detected in image");
        }

        String translation = llmService.translate(
                extractedText,
                request.getSourceLanguage(),
                request.getTargetLanguage()
        );
        return new ImageTranslationResponse(extractedText, translation);
    }

    private String extractText(String imageBase64, String mimeType, String sourceLanguage) throws Exception {
        String tesseractText = null;
        Exception tesseractError = null;
        try {
            tesseractText = extractTextWithTesseract(imageBase64, mimeType, sourceLanguage);
        } catch (Exception ex) {
            tesseractError = ex;
        }

        if (tesseractText != null && !tesseractText.isBlank()) {
            return tesseractText;
        }

        if (apiKey == null || apiKey.isBlank()) {
            if (tesseractError != null) {
                throw new IllegalStateException(
                        "Tesseract OCR failed and GEMINI_API_KEY is not set: " + tesseractError.getMessage(),
                        tesseractError);
            }
            throw new IllegalStateException("No text detected by Tesseract and GEMINI_API_KEY is not set");
        }

        return extractTextWithGemini(imageBase64, mimeType, sourceLanguage);
    }

    private String extractTextWithTesseract(String imageBase64, String mimeType, String sourceLanguage)
            throws Exception {
        String command = resolveTesseractCommand();
        String language = resolveTesseractLanguage(sourceLanguage);
        String extension = resolveImageExtension(mimeType);

        Path imagePath = Files.createTempFile("ocr_image_", extension);
        Path outputBase = Files.createTempFile("ocr_text_", "");

        try {
            Files.write(imagePath, Base64.getDecoder().decode(imageBase64));
            Files.deleteIfExists(outputBase);

            List<String> args = new ArrayList<>();
            args.add(command);
            args.add(imagePath.toString());
            args.add(outputBase.toString());
            if (language != null && !language.isBlank()) {
                args.add("-l");
                args.add(language);
            }
            args.add("--psm");
            args.add("3");

            ProcessBuilder builder = new ProcessBuilder(args);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Tesseract failed (exit " + exitCode + "): " + output);
            }

            Path textPath = Path.of(outputBase.toString() + ".txt");
            if (!Files.exists(textPath)) {
                throw new IOException("Tesseract output file missing");
            }
            return Files.readString(textPath, StandardCharsets.UTF_8).trim();
        } finally {
            safeDelete(imagePath);
            safeDelete(Path.of(outputBase.toString() + ".txt"));
            safeDelete(outputBase);
        }
    }

    private String resolveTesseractCommand() {
        if (tesseractPath != null && !tesseractPath.isBlank()) {
            return tesseractPath;
        }
        return "tesseract";
    }

    private String resolveTesseractLanguage(String sourceLanguage) {
        if (tesseractLang != null && !tesseractLang.isBlank()) {
            return tesseractLang;
        }
        if (sourceLanguage == null || sourceLanguage.isBlank() || "auto".equalsIgnoreCase(sourceLanguage)) {
            return "eng";
        }
        String lang = sourceLanguage.trim().toLowerCase();
        if (lang.startsWith("en")) {
            return "eng";
        }
        if (lang.startsWith("fr")) {
            return "fra";
        }
        if (lang.startsWith("ar") || lang.startsWith("ary")) {
            return "ara";
        }
        if (lang.startsWith("es")) {
            return "spa";
        }
        return "eng";
    }

    private String resolveImageExtension(String mimeType) {
        String candidate = stripParameters(mimeType);
        if (candidate == null) {
            return ".png";
        }
        switch (candidate) {
            case "image/jpeg":
            case "image/jpg":
                return ".jpg";
            case "image/webp":
                return ".webp";
            case "image/bmp":
                return ".bmp";
            case "image/tiff":
                return ".tiff";
            default:
                return ".png";
        }
    }

    private void safeDelete(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // Best-effort cleanup.
        }
    }

    private String extractTextWithGemini(String imageBase64, String mimeType, String sourceLanguage)
            throws Exception {

        String prompt = buildOcrPrompt(sourceLanguage);

        JSONObject inlineData = new JSONObject()
                .put("mime_type", mimeType)
                .put("data", imageBase64);

        JSONArray parts = new JSONArray()
                .put(new JSONObject().put("text", prompt))
                .put(new JSONObject().put("inline_data", inlineData));

        JSONObject contents = new JSONObject().put("parts", parts);
        JSONObject body = new JSONObject().put("contents", new JSONArray().put(contents));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildGeminiVisionUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini Vision error: " + response.body());
        }

        return readTextFromResponse(response.body());
    }

    private URI buildGeminiVisionUri() throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + visionModel + ":generateContent?key=" + apiKey;
        return new URI(url);
    }

    private String readTextFromResponse(String responseBody) {
        JSONObject json = new JSONObject(responseBody);
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Gemini Vision returned no candidates");
        }

        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        if (content == null) {
            throw new RuntimeException("Gemini Vision returned no content");
        }

        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.isEmpty()) {
            throw new RuntimeException("Gemini Vision returned no text parts");
        }

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            String text = parts.getJSONObject(i).optString("text", "").trim();
            if (!text.isEmpty()) {
                if (output.length() > 0) {
                    output.append("\n");
                }
                output.append(text);
            }
        }

        return output.toString().trim();
    }

    private String buildOcrPrompt(String sourceLanguage) {
        String languageHint = "";
        if (sourceLanguage != null && !sourceLanguage.isBlank() && !"auto".equalsIgnoreCase(sourceLanguage)) {
            languageHint = " The text language is " + sourceLanguage + ".";
        }
        return "Extract all readable text from the image. Preserve line breaks."
                + languageHint
                + " Return only the extracted text.";
    }

    private String normalizeBase64(String imageBase64) {
        String trimmed = imageBase64.trim();
        String base64 = trimmed;
        if (trimmed.startsWith("data:")) {
            int commaIndex = trimmed.indexOf(',');
            if (commaIndex > -1 && commaIndex < trimmed.length() - 1) {
                base64 = trimmed.substring(commaIndex + 1).trim();
            }
        }

        try {
            Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("imageBase64 must be valid base64");
        }

        return base64;
    }

    private String normalizeMimeType(String imageMimeType, String imageBase64) {
        String candidate = stripParameters(imageMimeType);
        if (candidate != null && candidate.startsWith("image/")) {
            return candidate;
        }

        if (imageBase64 != null && imageBase64.trim().startsWith("data:")) {
            String dataHeader = imageBase64.trim();
            int colonIndex = dataHeader.indexOf(':');
            int semicolonIndex = dataHeader.indexOf(';');
            if (colonIndex > -1 && semicolonIndex > colonIndex) {
                String fromHeader = dataHeader.substring(colonIndex + 1, semicolonIndex).trim();
                if (fromHeader.startsWith("image/")) {
                    return fromHeader;
                }
            }
        }

        return DEFAULT_MIME_TYPE;
    }

    private String stripParameters(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return null;
        }
        return mimeType.split(";", 2)[0].trim().toLowerCase();
    }
}
