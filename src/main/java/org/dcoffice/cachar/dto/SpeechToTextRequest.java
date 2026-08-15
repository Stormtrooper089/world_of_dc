package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

/**
 * Inbound audio for the voice assistant's cloud speech-to-text path (Bhashini).
 * audioBase64 must already be 16kHz mono 16-bit PCM WAV — the browser's
 * MediaRecorder output (webm/opus) is converted client-side before this is
 * sent, since Bhashini's ASR models expect PCM WAV, not opus.
 */
public class SpeechToTextRequest {

    @NotBlank(message = "Audio data is required")
    private String audioBase64;

    @NotBlank(message = "Language is required")
    private String language;

    public SpeechToTextRequest() {
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
