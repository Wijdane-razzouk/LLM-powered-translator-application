const recordBtn = document.getElementById("start");
const playbackEl = document.getElementById("voicePlayback") || document.getElementById("playback");
const txtInEl = document.getElementById("txt-in");
const txtOutEl = document.getElementById("txt-out");
const speakDarijaBtn = document.getElementById("speakDarija");

let recorder;
let chunks = [];
let translatedAudioBase64 = null;
let recordedMimeType = "";

function apiBase() {
  if (typeof getApiBase === "function") {
    return getApiBase();
  }
  return "http://localhost:8080/api/translator";
}

function headers(extra) {
  if (typeof buildHeaders === "function") {
    return buildHeaders(extra);
  }
  return extra || {};
}

async function authorizedFetch(url, options = {}) {
  if (typeof window.fetchWithAuthRetry === "function") {
    return window.fetchWithAuthRetry(url, options);
  }
  const authHeaders = headers(options.headers || {});
  return fetch(url, { ...options, headers: authHeaders });
}

function languageConfig() {
  if (typeof getLanguageConfig === "function") {
    return getLanguageConfig();
  }
  return { source: "en", target: "ary" };
}

function setupMedia() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    console.error("Media devices not available in this browser.");
    return;
  }

  navigator.mediaDevices
    .getUserMedia({ audio: true })
    .then((stream) => setupRecorder(stream))
    .catch((err) => console.error("Error accessing mic:", err));
}

function setupRecorder(stream) {
  recorder = new MediaRecorder(stream);
  recordedMimeType = recorder.mimeType || "";
  recorder.ondataavailable = (e) => {
    if (e.data && e.data.type) {
      recordedMimeType = e.data.type;
    }
    chunks.push(e.data);
  };
  recorder.onstop = handleRecordingStop;
}

async function handleRecordingStop() {
  const mimeType = recordedMimeType || (chunks[0] && chunks[0].type) || "audio/webm";
  const blob = new Blob(chunks, { type: mimeType });
  chunks = [];

  playbackEl.style.display = "block";
  playbackEl.src = URL.createObjectURL(blob);
  playbackEl.play().catch(() => {});

  let payload;
  try {
    payload = await convertToWavPayload(blob);
  } catch (err) {
    console.warn("WAV conversion failed, sending original audio:", err);
    payload = { base64: await blobToBase64(blob), mimeType };
  }
  sendSpeechToBackend(payload.base64, payload.mimeType);
}

function toggleRecord() {
  if (!recorder) return;

  if (recorder.state === "recording") {
    recorder.stop();
    recordBtn.textContent = "Micro / Stop";
    recordBtn.classList.remove("recording");
  } else {
    recorder.start();
    recordBtn.textContent = "Arreter l'enregistrement";
    recordBtn.classList.add("recording");
  }
}

function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const result = reader.result || "";
      const base64 = result.toString().split(",")[1];
      resolve(base64);
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}

async function convertToWavPayload(blob) {
  const audioContext = new (window.AudioContext || window.webkitAudioContext)();
  try {
    const arrayBuffer = await blob.arrayBuffer();
    const audioBuffer = await audioContext.decodeAudioData(arrayBuffer);
    const wavBuffer = audioBufferToWav(audioBuffer);
    const wavBlob = new Blob([wavBuffer], { type: "audio/wav" });
    const base64 = await blobToBase64(wavBlob);
    return { base64, mimeType: "audio/wav" };
  } finally {
    audioContext.close();
  }
}

function audioBufferToWav(buffer) {
  const numChannels = buffer.numberOfChannels;
  const sampleRate = buffer.sampleRate;
  const numFrames = buffer.length;
  const bytesPerSample = 2;
  const blockAlign = numChannels * bytesPerSample;
  const byteRate = sampleRate * blockAlign;
  const dataSize = numFrames * blockAlign;

  const arrayBuffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(arrayBuffer);

  writeString(view, 0, "RIFF");
  view.setUint32(4, 36 + dataSize, true);
  writeString(view, 8, "WAVE");
  writeString(view, 12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, numChannels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, byteRate, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, 16, true);
  writeString(view, 36, "data");
  view.setUint32(40, dataSize, true);

  let offset = 44;
  for (let i = 0; i < numFrames; i++) {
    for (let channel = 0; channel < numChannels; channel++) {
      const sample = buffer.getChannelData(channel)[i];
      const clamped = Math.max(-1, Math.min(1, sample));
      view.setInt16(offset, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true);
      offset += 2;
    }
  }

  return arrayBuffer;
}

function writeString(view, offset, value) {
  for (let i = 0; i < value.length; i++) {
    view.setUint8(offset + i, value.charCodeAt(i));
  }
}

async function sendSpeechToBackend(audioBase64, audioMimeType) {
  translatedAudioBase64 = null;
  const lang = languageConfig();
  const sourceLanguage = lang.source || "en";
  const targetLanguage = lang.target || "ary";

  try {
    const response = await authorizedFetch(apiBase() + "/speech/translate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        audioBase64,
        audioMimeType,
        sourceLanguage,
        targetLanguage,
        voice: "standard",
      }),
    });

    if (!response.ok) {
      throw new Error("HTTP error " + response.status);
    }

    const data = await response.json();
    txtInEl.textContent = data.transcript || "";
    txtOutEl.textContent = data.translatedText || "";
    translatedAudioBase64 = data.translatedAudioBase64 || null;

    if (translatedAudioBase64) {
      playBase64Audio(translatedAudioBase64);
    }
  } catch (err) {
    console.error("Speech translate error:", err);
    txtOutEl.textContent = "Speech translation failed: " + err.message;
  }
}

function playBase64Audio(encoded) {
  if (!encoded) return;
  const src = "data:audio/wav;base64," + encoded;
  playbackEl.style.display = "block";
  playbackEl.src = src;
  playbackEl.play().catch(() => {});
}

function bindAudioUi() {
  if (recordBtn) {
    recordBtn.addEventListener("click", toggleRecord);
  }
  if (speakDarijaBtn) {
    speakDarijaBtn.addEventListener("click", () => playBase64Audio(translatedAudioBase64));
  }
}

document.addEventListener("DOMContentLoaded", () => {
  setupMedia();
  bindAudioUi();
});

