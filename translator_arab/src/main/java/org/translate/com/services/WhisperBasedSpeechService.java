package org.translate.com.services;

import org.translate.com.dto.*;


import java.io.IOException;

public class WhisperBasedSpeechService {

    private final LlmService llmService;
    private final WhisperService whisperService;
    private final TTSService ttsService;

    public WhisperBasedSpeechService() {
        this.llmService = new LlmService();
        this.whisperService = new WhisperService();
        this.ttsService = new TTSService();
    }

    
    public SpeechTranslationResponse voiceToVoice(SpeechTranslationRequest request) {
        validateRequest(request);

        try {
            String transcript = transcribeWithWhisper(request);

            String translatedText = translateText(transcript, request);

            String audioBase64 = synthesizeSpeech(translatedText, request);

            return new SpeechTranslationResponse(
                    transcript,
                    translatedText,
                    audioBase64);

        } catch (Exception e) {
            handleProcessingError(e, request);
            throw new RuntimeException("Échec du traitement vocal: " + e.getMessage(), e);
        }
    }

   
    public ReadAloudResponse readAloud(ReadAloudRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw new IllegalArgumentException("Le texte est requis");
        }

        try {
            String audioBase64 = ttsService.synthesizeText(
                    request.getText(),
                    null, // No target language for read-aloud
                    request.getVoice());

            return new ReadAloudResponse(audioBase64);

        } catch (Exception e) {
            throw new RuntimeException("Échec de la synthèse vocale: " + e.getMessage(), e);
        }
    }

    
    private String transcribeWithWhisper(SpeechTranslationRequest request) {
        if (!whisperService.isAvailable()) {
            throw new IllegalStateException(
                    "Whisper API non configurée. Définissez OPENAI_API_KEY");
        }

        try {
            return whisperService.transcribeAudio(
                    request.getAudioBase64(),
                    request.getSourceLanguage(),
                    request.getAudioMimeType());
        } catch (IOException e) {
            throw new RuntimeException("Erreur de transcription Whisper: " + e.getMessage(), e);
        }
    }

    private String translateText(String text, SpeechTranslationRequest request) {
        try {
            return llmService.translate(
                    text,
                    request.getSourceLanguage(),
                    request.getTargetLanguage());
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.isBlank()) {
                message = e.getClass().getSimpleName();
            }
            throw new RuntimeException("Erreur de traduction: " + message, e);
        }
    }

    private String synthesizeSpeech(String text, SpeechTranslationRequest request) {
        return ttsService.synthesizeText(
                text,
                request.getTargetLanguage(),
                request.getVoice());
    }

    private void validateRequest(SpeechTranslationRequest request) {
        if (request == null || request.getAudioBase64() == null ||
                request.getAudioBase64().isBlank()) {
            throw new IllegalArgumentException("audioBase64 est requis");
        }

        if (!whisperService.isAvailable()) {
            throw new IllegalStateException(
                    "API Whisper non disponible. Vérifiez votre clé API OpenAI.");
        }
    }

    private void handleProcessingError(Exception e, SpeechTranslationRequest request) {
        System.err.println("Erreur de traitement vocal:");
        System.err.println("Requête: " + request);
        System.err.println("Erreur: " + e.getMessage());
        e.printStackTrace();
    }

  
    public void shutdown() {
        if (whisperService != null) {
            whisperService.shutdown();
        }
    }
}
