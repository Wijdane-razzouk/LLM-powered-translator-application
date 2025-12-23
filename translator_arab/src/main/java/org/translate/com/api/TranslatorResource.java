package org.translate.com.api;

import org.translate.com.dto.TranslationRequest;
import org.translate.com.dto.TranslationResponse;
import org.translate.com.services.LlmService;
import org.translate.com.dto.ImageTranslationRequest;
import org.translate.com.dto.ImageTranslationResponse;
import org.translate.com.services.ImageService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/translator")
public class TranslatorResource {

	private final LlmService llmService = new LlmService();
	private final ImageService imageService = new ImageService();

	@POST
	@Path("/translate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response translate(TranslationRequest request) {

		if (request == null || request.getText() == null || request.getText().isBlank()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(new TranslationResponse("Error: 'text' is required"))
					.build();
		}

		try {
			String darija = llmService.translate(
					request.getText(),
					request.getSourceLanguage(),
					request.getTargetLanguage());
			return Response.ok(new TranslationResponse(darija)).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(new TranslationResponse("Error while calling LLM: " + e.getMessage()))
					.build();
		}

	}

	@POST
	@Path("/image/translate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response translateImage(ImageTranslationRequest request) {
		try {
			ImageTranslationResponse translated = imageService.translate(request);
			return Response.ok(translated).build();
		} catch (IllegalArgumentException ex) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(ex.getMessage())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error while processing image translation: " + e.getMessage())
					.build();
		}
	}

	@GET
	@Path("/ping")
	public Response ping() {
		return Response.ok("OK").build();
	}

}
