package org.translate.com.dto;

public class SpeechTranslationRequest {
    private String audioBase64;
    private String audioMimeType;
    private String sourceLanguage = "auto";
    private String targetLanguage = "ary"; // Darija locale code
    private String voice = "standard";

    public SpeechTranslationRequest() {
    }

    public SpeechTranslationRequest(String audioBase64, String sourceLanguage, String targetLanguage, String voice) {
        this.audioBase64 = audioBase64;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.voice = voice;
    }

    public SpeechTranslationRequest(String audioBase64, String audioMimeType, String sourceLanguage, String targetLanguage,
            String voice) {
        this.audioBase64 = audioBase64;
        this.audioMimeType = audioMimeType;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.voice = voice;
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }

    public String getAudioMimeType() {
        return audioMimeType;
    }

    public void setAudioMimeType(String audioMimeType) {
        this.audioMimeType = audioMimeType;
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

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }
}
