package org.translate.com.dto;

public class SpeechTranslationResponse {
    private String transcript;
    private String translatedText;
    private String translatedAudioBase64;

    public SpeechTranslationResponse() {
    }

    public SpeechTranslationResponse(String transcript, String translatedText, String translatedAudioBase64) {
        this.transcript = transcript;
        this.translatedText = translatedText;
        this.translatedAudioBase64 = translatedAudioBase64;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getTranslatedAudioBase64() {
        return translatedAudioBase64;
    }

    public void setTranslatedAudioBase64(String translatedAudioBase64) {
        this.translatedAudioBase64 = translatedAudioBase64;
    }
}
