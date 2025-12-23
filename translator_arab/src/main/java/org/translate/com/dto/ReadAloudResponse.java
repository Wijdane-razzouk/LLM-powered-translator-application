package org.translate.com.dto;

public class ReadAloudResponse {
    private String audioBase64;

    public ReadAloudResponse() {
    }

    public ReadAloudResponse(String audioBase64) {
        this.audioBase64 = audioBase64;
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }
}
