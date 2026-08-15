package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

public class TextToSpeechRequest {

    @NotBlank(message = "Text is required")
    private String text;

    @NotBlank(message = "Language is required")
    private String language;

    public TextToSpeechRequest() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
