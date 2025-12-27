# Darija Translator (Web App + Chrome Extension)

## Overview
This project provides an English to Darija translator with three input modes:
- Text input
- Voice input (speech to text + translation + optional TTS)
- Image input (OCR + translation)

The UI is available as a web app and as a Chrome side panel extension.
The backend exposes a REST API at `http://localhost:8080/api/translator`.

## Architecture (high level)
- Web UI or Chrome Extension -> REST API
- Text: Mistral LLM translation
- Voice: Whisper STT -> Mistral LLM -> TTS
- Image: Tesseract OCR -> Mistral LLM (Gemini Vision OCR fallback if needed)
![alt text](<Untitled diagram-2025-12-27-124413.png>)
## Architecture (detailed)

### Flow
- Text: Mistral LLM translation
- Voice: Whisper STT -> Mistral LLM -> TTS (base64 audio)
- Image: Tesseract OCR -> Mistral LLM (Gemini Vision OCR fallback if needed)

### Components

#### Frontend
- `frontendia/webUI`: static web app
- `frontendia/extension`: Chrome side panel extension
- Both call the backend API at `http://localhost:8080/api/translator`

#### Backend (Java)
- `translator_arab/src/main/java/org/translate/com/config`
  - `TranslatorApplication`: `/api` base path, Jersey + Jackson wiring
  - `CorsFilter`: CORS handling (preflight before auth)
  - `EnvConfig`: loads `.env` and system env vars
- `translator_arab/src/main/java/org/translate/com/api`
  - `TranslatorResource`: text + image translation endpoints
  - `SpeechResource`: voice translation endpoints
- `translator_arab/src/main/java/org/translate/com/services`
  - `LlmService`: Mistral API translation
  - `WhisperService`: Whisper STT (OpenAI or local server)
  - `WhisperBasedSpeechService`: STT -> translate -> TTS pipeline
  - `ImageService`: Tesseract OCR with Gemini Vision fallback
  - `TTSService`: custom/Edge/Google fallback TTS
- `translator_arab/src/main/java/org/translate/com/security`
  - `BasicAuthFilter`: optional Basic auth

### API surface (summary)
- `POST /api/translator/translate` for text translation
- `POST /api/translator/image/translate` for image OCR + translation
- `POST /api/translator/speech/translate` for voice translation
- `POST /api/translator/read-aloud` for TTS only
- `GET /api/translator/ping` for health checks

### Data contracts (DTOs)
- `TranslationRequest`: `text`, `sourceLanguage`, `targetLanguage`
- `ImageTranslationRequest`: `imageBase64`, `imageMimeType`, `sourceLanguage`, `targetLanguage`
- `SpeechTranslationRequest`: `audioBase64`, `audioMimeType`, `sourceLanguage`, `targetLanguage`, `voice`
- `ReadAloudRequest`: `text`, `voice`

### Implementation choices (rationale)
- Jersey + JAX-RS keeps the API simple and deployable as a WAR or via the embedded server.
- Mistral cloud LLM gives strong Darija translations with a focused prompt.
- Tesseract handles OCR locally; Gemini Vision is used only when configured and needed.
- Whisper STT supports both OpenAI and a local Whisper server for flexibility.
- TTS uses a fallback chain to keep voice output available across setups.
- Separate UI targets (web app and extension) share the same backend API.

### Diagram
![Darija Translator Pipeline](docs/architecture.png)

## Implementation details (key behavior)

### Configuration loading
- `EnvConfig` reads from system environment first, then `.env` and `translator_arab/.env` once per process.

### Translation behavior
- `LlmService` calls `https://api.mistral.ai/v1/chat/completions` with a Darija-focused prompt.
- `MISTRAL_API_KEY` is required; `MISTRAL_MODEL` defaults to `mistral-large-latest`.
- Source language defaults to English (`en`) and target defaults to Darija (`ary`).

### OCR behavior
- Accepts raw base64 or data URLs; MIME type is inferred when missing.
- Tesseract uses `TESSERACT_PATH` if set and falls back to `tesseract` on PATH.
- OCR language defaults to `eng`, with simple mapping for `fr`, `ar/ary`, `es`.
- If no text is found and `GEMINI_API_KEY` is set, Gemini Vision OCR is used.

### Speech and TTS behavior
- Whisper STT uses OpenAI by default and switches to a local server when `WHISPER_API_URL` is set.
- `OPENAI_API_KEY` is required unless a local Whisper URL is configured.
- TTS fallback order: `TTS_API_URL` -> Edge TTS proxy -> Google TTS -> local fallback.

### Security and errors
- Basic auth is enabled only when `TRANSLATOR_USER` and `TRANSLATOR_PASSWORD` are set.
- CORS preflight runs before auth to allow browser requests.
- API returns 400 for missing inputs and 500 for upstream/service failures.

## Requirements
- Java 11+
- Maven (if you run without a prebuilt jar)
- (Required) Mistral API key for text translation
- (Optional) OpenAI API key or local Whisper server for voice
- (Optional) Tesseract for OCR
- (Optional) Gemini API key for Vision OCR fallback
- (Optional) TTS endpoint (or rely on Edge/Google fallback)

## Install backend dependencies (pom.xml)
From project root:
```
cd translator_arab
```

Download all Maven dependencies defined in `pom.xml`:
```
mvn -q dependency:go-offline
```

Or download dependencies as part of the build:
```
mvn -q -DskipTests package
```

Notes:
- Maven caches artifacts under your local repository (usually `~/.m2/repository`).
- If you change versions in `pom.xml`, rerun one of the commands above.

## Configuration (.env)
File: `translator_arab/.env`

Example (do not commit real keys):
```
MISTRAL_API_KEY=YOUR_KEY_HERE
MISTRAL_MODEL=mistral-large-latest
GEMINI_API_KEY=YOUR_KEY_HERE
GEMINI_VISION_MODEL=gemini-1.5-flash
OPENAI_API_KEY=YOUR_KEY_HERE
WHISPER_API_URL=http://127.0.0.1:9000/inference
TTS_API_URL=http://127.0.0.1:8000/v1/audio/speech
TESSERACT_PATH=C:\Program Files\Tesseract-OCR\tesseract.exe
TESSERACT_LANG=eng
TRANSLATOR_USER=translator
TRANSLATOR_PASSWORD=translator
```

Notes:
- `MISTRAL_API_KEY` is required for text translation.
- If `WHISPER_API_URL` is set, `OPENAI_API_KEY` is optional.
- If `GEMINI_API_KEY` is not set, image OCR only uses Tesseract.

## Run the backend
From project root:
```
cd translator_arab
```

If you have a jar:
```
java -jar target\translator_arab.jar
```

Or via Maven:
```
mvn -q exec:java -Dexec.mainClass=org.translate.com.EmbeddedServer
```

You can also use:
```
start_all.bat
```

## Run Whisper server (voice)
You must run your Whisper server separately if you want voice translation.
Example command is already included in `start_all.bat`.

Make sure this matches your `.env`:
```
WHISPER_API_URL=http://127.0.0.1:9000/inference
```

## Run the web app
Open this file in a browser:
```
frontendia\webUI\index.html
```

For best results, serve it with a local server (e.g. Live Server).

## Use the Chrome extension
1) Open `chrome://extensions`
2) Enable Developer mode
3) Click "Load unpacked"
4) Select `frontendia\extension`

Then open the side panel from the extension menu.

## Authentication
If you set these in `.env`, the backend requires Basic auth:
```
TRANSLATOR_USER=translator
TRANSLATOR_PASSWORD=translator
```

The UI currently sends `translator/translator` by default.
If you change backend credentials, update:
`frontendia/webUI/js/script.js`
and the extension copy under
`frontendia/extension/js/script.js`.

## Troubleshooting
### 401 Unauthorized
- Backend expects different credentials.
- Check OS env vars `TRANSLATOR_USER` and `TRANSLATOR_PASSWORD`.
- Restart backend after changes.

### 500 Internal Server Error (speech)
- Whisper not running or wrong `WHISPER_API_URL`.
- LLM translation failed (check backend logs).

### OCR returns no text
- Check `TESSERACT_PATH` and `TESSERACT_LANG`.
- Ensure Tesseract is installed and accessible.
