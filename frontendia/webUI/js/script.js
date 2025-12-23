const defaultConfig = {
  baseUrl: "http://localhost:8080/api/translator",
  user: "translator",
  pass: "translator",
};

const defaultLanguages = {
  source: "en",
  target: "ary",
};

let languageState = { ...defaultLanguages };

function readConfigValue(id, fallback) {
  const el = document.getElementById(id);
  if (!el) return fallback;
  const value = (el.value || "").trim();
  return value || fallback;
}

function normalizeLang(value, fallback) {
  if (!value) return fallback;
  const lang = value.toString().trim().toLowerCase();
  if (!lang) return fallback;
  if (lang === "english" || lang.startsWith("en")) return "en";
  if (lang === "darija" || lang === "ary" || lang.startsWith("ary")) return "ary";
  return lang;
}

function labelForLang(lang) {
  if (lang === "en") return "English";
  if (lang === "ary") return "Darija";
  return lang ? lang.toUpperCase() : "";
}

function saveConfig() {
  const base = readConfigValue("apiBase", defaultConfig.baseUrl);
  const user = readConfigValue("apiUser", defaultConfig.user);
  const pass = readConfigValue("apiPass", defaultConfig.pass);
  localStorage.setItem("translatorApiBase", base);
  localStorage.setItem("translatorApiUser", user);
  localStorage.setItem("translatorApiPass", pass);
  updateBaseLabel(base);
}

function loadConfig() {
  const base = localStorage.getItem("translatorApiBase") || defaultConfig.baseUrl;
  const user = localStorage.getItem("translatorApiUser") || defaultConfig.user;
  const pass = localStorage.getItem("translatorApiPass") || defaultConfig.pass;

  const baseInput = document.getElementById("apiBase");
  const userInput = document.getElementById("apiUser");
  const passInput = document.getElementById("apiPass");

  if (baseInput) baseInput.value = base;
  if (userInput) userInput.value = user;
  if (passInput) passInput.value = pass;

  updateBaseLabel(base);
}

function saveLanguageState() {
  localStorage.setItem("translatorSourceLang", languageState.source);
  localStorage.setItem("translatorTargetLang", languageState.target);
}

function loadLanguageState() {
  const source = normalizeLang(localStorage.getItem("translatorSourceLang"), defaultLanguages.source);
  const target = normalizeLang(localStorage.getItem("translatorTargetLang"), defaultLanguages.target);
  languageState = { source, target };
  updateLanguageToggle();
}

function updateLanguageToggle() {
  const sourceEl = document.getElementById("sourceLang");
  const targetEl = document.getElementById("targetLang");
  if (!sourceEl || !targetEl) return;
  sourceEl.textContent = labelForLang(languageState.source).toUpperCase();
  targetEl.textContent = labelForLang(languageState.target).toUpperCase();
  sourceEl.classList.add("active");
  targetEl.classList.remove("active");
}

function swapLanguages() {
  languageState = {
    source: languageState.target,
    target: languageState.source,
  };
  saveLanguageState();
  updateLanguageToggle();
}

function updateBaseLabel(value) {
  const label = document.getElementById("apiBaseLabel");
  if (label) {
    label.textContent = value;
  }
}

function getApiBase() {
  const base = readConfigValue("apiBase", localStorage.getItem("translatorApiBase") || defaultConfig.baseUrl);
  return base.replace(/\/+$/, "");
}

function getAuthHeader() {
  const user = defaultConfig.user;
  const pass = defaultConfig.pass;
  if (!user || !pass) {
    return null;
  }
  return "Basic " + btoa(user + ":" + pass);
}

function buildHeaders(extra = {}) {
  const headers = { ...extra };
  const auth = getAuthHeader();
  if (auth) {
    headers.Authorization = auth;
  }
  return headers;
}

function setVariant(el, variant) {
  if (!el) return;
  ["success", "danger", "info", "muted"].forEach((v) => el.classList.remove(v));
  if (variant) {
    el.classList.add(variant);
  }
}

function setResult(message, variant = "muted") {
  const resultBox = document.getElementById("result");
  if (!resultBox) return;
  resultBox.textContent = message;
  setVariant(resultBox, variant);
}

async function doTranslate() {
  const input = (document.getElementById("textInput")?.value || "").trim();
  if (!input) {
    setResult("Merci d'ecrire quelque chose avant de traduire.", "danger");
    return;
  }

  setResult("Traduction en cours...", "info");

  try {
    const response = await fetch(getApiBase() + "/translate", {
      method: "POST",
      headers: buildHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({
        text: input,
        sourceLanguage: languageState.source,
        targetLanguage: languageState.target,
      }),
    });

    if (!response.ok) {
      throw new Error("HTTP error " + response.status);
    }

    const data = await response.json();
    setResult(data.translation || "Aucune traduction retournee.", "success");
  } catch (e) {
    setResult("Erreur serveur : " + e.message, "danger");
  }
}

async function doReadAloud() {
  const input = (document.getElementById("textInput")?.value || "").trim();
  const playback = document.getElementById("playback");

  if (!input) {
    setResult("Merci d'ecrire quelque chose avant de lancer la lecture.", "danger");
    return;
  }

  setResult("Generation audio via le backend...", "info");

  try {
    const response = await fetch(getApiBase() + "/read-aloud", {
      method: "POST",
      headers: buildHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ text: input, voice: "standard" }),
    });

    if (!response.ok) {
      throw new Error("HTTP error " + response.status);
    }

    const data = await response.json();
    if (data.audioBase64 && playback) {
      playback.style.display = "block";
      playback.src = "data:audio/wav;base64," + data.audioBase64;
      playback.play().catch(() => {});
      setResult("Audio pret (backend TTS).", "success");
    } else {
      setResult("Aucun flux audio recu.", "danger");
    }
  } catch (e) {
    setResult("Erreur serveur : " + e.message, "danger");
  }
}

function clearText() {
  const input = document.getElementById("textInput");
  if (input) {
    input.value = "";
  }
  setResult("Aucun resultat pour le moment.", "muted");
}

function handleFileSelection(file) {
  if (!file) return;
  if (!file.type.startsWith("image/")) {
    setUploadStatus("Format non pris en charge. Utilisez une image.", "danger");
    return;
  }

  const maxBytes = 10 * 1024 * 1024;
  if (file.size > maxBytes) {
    setUploadStatus("Fichier trop volumineux (10 Mo max).", "danger");
    return;
  }

  const reader = new FileReader();
  reader.onload = () => {
    const result = reader.result || "";
    const base64 = result.toString().split(",")[1] || "";
    translateImageBase64(base64, file.name, file.type);
  };
  reader.readAsDataURL(file);
}

function initUploadZone() {
  const dropZone = document.getElementById("uploadZone");
  const fileInput = document.getElementById("fileInput");
  if (!dropZone || !fileInput) return;

  const stopDefaults = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  ["dragenter", "dragover", "dragleave", "drop"].forEach((eventName) => {
    dropZone.addEventListener(eventName, stopDefaults);
  });

  dropZone.addEventListener("dragover", () => dropZone.classList.add("dragging"));
  dropZone.addEventListener("dragleave", () => dropZone.classList.remove("dragging"));
  dropZone.addEventListener("drop", (e) => {
    dropZone.classList.remove("dragging");
    const files = e.dataTransfer?.files;
    if (files && files.length > 0) {
      handleFileSelection(files[0]);
    }
  });

  dropZone.addEventListener("click", () => fileInput.click());
  fileInput.addEventListener("change", (e) => {
    const files = e.target?.files;
    if (files && files.length > 0) {
      handleFileSelection(files[0]);
    }
  });
}

function setUploadStatus(message, variant = "muted") {
  const el = document.getElementById("uploadStatus");
  if (!el) return;
  el.textContent = message;
  setVariant(el, variant);
}

function updateImageResult(text, translation) {
  const textEl = document.getElementById("imageText");
  const transEl = document.getElementById("imageTranslation");
  if (textEl) textEl.textContent = text || "--";
  if (transEl) transEl.textContent = translation || "--";
}

async function translateImageBase64(imageBase64, filename, mimeType) {
  setUploadStatus("Analyse de l'image en cours...", "info");
  updateImageResult("", "");

  try {
    const response = await fetch(getApiBase() + "/image/translate", {
      method: "POST",
      headers: buildHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({
        imageBase64,
        imageMimeType: mimeType || "",
        sourceLanguage: languageState.source || "auto",
        targetLanguage: languageState.target || "ary",
      }),
    });

    if (!response.ok) {
      throw new Error("HTTP error " + response.status);
    }

    const data = await response.json();
    updateImageResult(data.extractedText || data.text || "", data.translation || data.translatedText || "");
    const label = filename ? " pour " + filename : "";
    setUploadStatus("Traduction terminee" + label + ".", "success");
  } catch (err) {
    setUploadStatus("Echec de la traduction d'image : " + err.message, "danger");
  }
}

function bindConfigInputs() {
  ["apiBase", "apiUser", "apiPass"].forEach((id) => {
    const el = document.getElementById(id);
    if (el) {
      el.addEventListener("change", saveConfig);
      el.addEventListener("blur", saveConfig);
    }
  });
}

function bindButtons() {
  const readAloudBtn = document.getElementById("readAloudBtn");
  const translateBtn = document.getElementById("translateBtn");
  const clearBtn = document.getElementById("clearText");
  const saveBtn = document.getElementById("saveCfg");

  if (readAloudBtn) {
    readAloudBtn.addEventListener("click", doReadAloud);
  }
  if (translateBtn) {
    translateBtn.addEventListener("click", doTranslate);
  }
  if (clearBtn) {
    clearBtn.addEventListener("click", clearText);
  }
  if (saveBtn) {
    saveBtn.addEventListener("click", () => saveConfig());
  }
}

document.addEventListener("DOMContentLoaded", () => {
  loadConfig();
  loadLanguageState();
  bindConfigInputs();
  bindButtons();
  initUploadZone();
  const swapBtn = document.getElementById("swapLang");
  if (swapBtn) {
    swapBtn.addEventListener("click", swapLanguages);
  }
});

// Expose helpers for other scripts (audio.js)
window.getApiBase = getApiBase;
window.buildHeaders = buildHeaders;
window.getLanguageConfig = () => ({ ...defaultLanguages });
