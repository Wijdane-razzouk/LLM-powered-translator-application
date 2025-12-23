package org.translate.com.api;

import org.translate.com.dto.ReadAloudRequest;
import org.translate.com.dto.ReadAloudResponse;
import org.translate.com.dto.SpeechTranslationRequest;
import org.translate.com.dto.SpeechTranslationResponse;
import org.translate.com.services.WhisperBasedSpeechService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/translator")
public class SpeechResource {

    private final WhisperBasedSpeechService speechService = new WhisperBasedSpeechService();

    @POST
    @Path("/speech/translate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response translateSpeech(SpeechTranslationRequest request) {
        try {
            SpeechTranslationResponse translated = speechService.voiceToVoice(request);
            return Response.ok(translated).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ex.getMessage())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error while processing speech translation: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/read-aloud")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response readAloud(ReadAloudRequest request) {
        try {
            ReadAloudResponse audio = speechService.readAloud(request);
            return Response.ok(audio).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ex.getMessage())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error while generating audio: " + e.getMessage())
                    .build();
        }
    }
}
