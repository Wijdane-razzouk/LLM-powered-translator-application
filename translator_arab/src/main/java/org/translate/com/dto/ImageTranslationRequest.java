package org.translate.com.dto;

public class ImageTranslationRequest {
    private String imageBase64;
    private String imageMimeType;
    private String sourceLanguage = "auto";
    private String targetLanguage = "ary";

    public ImageTranslationRequest() {
    }

    public ImageTranslationRequest(String imageBase64, String sourceLanguage, String targetLanguage) {
        this.imageBase64 = imageBase64;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    public ImageTranslationRequest(String imageBase64, String imageMimeType, String sourceLanguage,
            String targetLanguage) {
        this.imageBase64 = imageBase64;
        this.imageMimeType = imageMimeType;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getImageMimeType() {
        return imageMimeType;
    }

    public void setImageMimeType(String imageMimeType) {
        this.imageMimeType = imageMimeType;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }
}
