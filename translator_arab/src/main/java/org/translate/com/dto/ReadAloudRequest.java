package org.translate.com.dto;

public class ReadAloudRequest {
    private String text;
    private String voice = "standard";

    public ReadAloudRequest() {
    }

    public ReadAloudRequest(String text, String voice) {
        this.text = text;
        this.voice = voice;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }
}
