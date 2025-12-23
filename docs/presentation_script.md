# Presentation Script and Slide Outline

## Slide Outline
1) Title + problem statement
2) Architecture overview (clients, backend, services)
3) Text translation pipeline
4) Voice pipeline (Whisper + TTS)
5) Image pipeline (Tesseract OCR)
6) Key implementation choices + wrap-up

## Narration Script (3-5 minutes)

Slide 1 - Title + problem statement
Hello everyone. This project is a Darija translator that supports text, voice, and image inputs. The main goal is to make English-to-Darija translation accessible through a simple web UI and a Chrome extension, while keeping the backend modular so we can swap models and services as needed.

Slide 2 - Architecture overview
Here is the high-level architecture. Users interact with either the web app or the Chrome extension. Both send requests to the same REST API backend. The backend orchestrates three optional services: OCR for images, speech-to-text for voice, and text-to-speech for audio output. At the center is the translation LLM layer, which can use Gemini or a local model such as Mistral. This modular design lets us upgrade one piece without breaking the rest.

Slide 3 - Text translation pipeline
For text input, the flow is direct. The UI sends the text to the backend endpoint. The backend validates the request, then calls the translation service. The LLM returns a Darija translation in Arabic script, and the API returns it to the client. This path is the simplest and fastest, and it is the primary baseline for quality testing.

Slide 4 - Voice pipeline
For voice input, the client records audio and sends a base64 payload to the backend. The backend uses Whisper for speech-to-text. The transcript is then passed to the translation LLM. Finally, the translated Darija text can be converted to audio via the TTS service. This gives a full voice-to-voice experience, but it is optional: if Whisper is not running, the text and image features still work.

Slide 5 - Image pipeline
For images, the backend first runs OCR. We use local Tesseract as the primary OCR engine, with a fallback to Gemini Vision if needed. The extracted text is then translated using the same LLM service. This keeps the OCR and translation steps separated, which improves debugging and lets us tune each step independently.

Slide 6 - Key implementation choices + wrap-up
There are a few key choices worth highlighting. First, the backend is the single source of truth, so the UI is thin and easy to swap or update. Second, we prioritize Gemini for translation when a key is available, and fall back to a local LLM for offline use. Third, OCR and Whisper are optional services, so the system degrades gracefully. Finally, we keep the API stateless and simple for easy deployment.

In summary, this project delivers a practical, modular pipeline for English-to-Darija translation across text, voice, and images. It is ready for local use today and can be deployed for broader access with minimal changes.
