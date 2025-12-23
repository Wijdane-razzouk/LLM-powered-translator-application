package org.translate.com.dto;

public class ImageTranslationResponse {
    private String extractedText;
    private String translation;

    public ImageTranslationResponse() {
    }

    public ImageTranslationResponse(String extractedText, String translation) {
        this.extractedText = extractedText;
        this.translation = translation;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }
}
