package org.translate.com.dto;

public class TranslationRequest {
	  private String text;
      private String sourceLanguage = "en";
      private String targetLanguage = "ary";

	    public TranslationRequest() {
	    }

	    public TranslationRequest(String text) {
	        this.text = text;
	    }

	    public String getText() {
	        return text;
	    }

	    public void setText(String text) {
	        this.text = text;
	    }

        public String getSourceLanguage() {
            return sourceLanguage;
        }

        public void setSourceLanguage(String sourceLanguage) {
            this.sourceLanguage = sourceLanguage;
        }

        public String getTargetLanguage() {
            return targetLanguage;
        }

        public void setTargetLanguage(String targetLanguage) {
            this.targetLanguage = targetLanguage;
        }
}
