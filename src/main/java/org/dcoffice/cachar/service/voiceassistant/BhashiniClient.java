package org.dcoffice.cachar.service.voiceassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcoffice.cachar.exception.BhashiniException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cloud speech-to-text / text-to-speech via Bhashini (MeitY's ULCA API) —
 * the Phase 2 upgrade over VoiceAssistantWidget's browser-only Web Speech API,
 * needed because browser STT/TTS barely supports Assamese and is patchy for
 * Hindi/Bengali (see that widget's file header). Free for this kind of
 * government citizen-service use, unlike Google/Azure/AWS speech APIs.
 *
 * Two-step flow per the ULCA API contract: first resolve a "pipeline config"
 * per language (which service to call, and the auth token for it) via
 * getModelsPipeline, then POST the actual audio/text to the callback URL that
 * call returns. The config rarely changes, so it's cached per language for
 * this instance's lifetime — same "good enough for one instance" tradeoff
 * VoiceAssistantService already makes for its own session map.
 *
 * Mirrors ChatToolCallingClient's placeholder-mode pattern: with no
 * BHASHINI_USER_ID / BHASHINI_API_KEY configured, every call throws a
 * BhashiniException up to the citizen-facing error path (the widget falls
 * back to typed input / browser speechSynthesis) rather than failing to build
 * or silently no-op'ing.
 */
@Component
public class BhashiniClient {

    private static final Logger logger = LoggerFactory.getLogger(BhashiniClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, LanguagePipeline> pipelineCache = new ConcurrentHashMap<>();

    @Value("${bhashini.user-id:}")
    private String userId;

    @Value("${bhashini.api-key:}")
    private String apiKey;

    @Value("${bhashini.pipeline-id:64392f96daac500b55c543cd}")
    private String pipelineId;

    @Value("${bhashini.config-url:https://meity-auth.ulcacontrib.org/ulca/apis/v0/model/getModelsPipeline}")
    private String configUrl;

    /** Resolved per-language routing info from the pipeline config call. */
    private static class LanguagePipeline {
        final String callbackUrl;
        final String inferenceApiKeyName;
        final String inferenceApiKeyValue;
        final String asrServiceId;
        final String ttsServiceId;

        LanguagePipeline(String callbackUrl, String inferenceApiKeyName, String inferenceApiKeyValue,
                          String asrServiceId, String ttsServiceId) {
            this.callbackUrl = callbackUrl;
            this.inferenceApiKeyName = inferenceApiKeyName;
            this.inferenceApiKeyValue = inferenceApiKeyValue;
            this.asrServiceId = asrServiceId;
            this.ttsServiceId = ttsServiceId;
        }
    }

    public String transcribe(String audioBase64Wav, String language) {
        requireConfigured();
        LanguagePipeline pipeline = resolvePipeline(language);

        ObjectNode taskConfig = objectMapper.createObjectNode();
        taskConfig.putObject("language").put("sourceLanguage", language);
        taskConfig.put("serviceId", pipeline.asrServiceId);
        taskConfig.put("audioFormat", "wav");
        taskConfig.put("samplingRate", 16000);

        ObjectNode task = objectMapper.createObjectNode();
        task.put("taskType", "asr");
        task.set("config", taskConfig);

        ObjectNode audioEntry = objectMapper.createObjectNode();
        audioEntry.put("audioContent", audioBase64Wav);

        ObjectNode inputData = objectMapper.createObjectNode();
        inputData.putArray("audio").add(audioEntry);

        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("pipelineTasks").add(task);
        body.set("inputData", inputData);

        JsonNode response = callInferenceEndpoint(pipeline, body);
        JsonNode source = response.path("pipelineResponse").path(0).path("output").path(0).path("source");
        if (!source.isTextual()) {
            logger.error("Bhashini ASR response missing output[0].source: {}", response);
            throw new BhashiniException("Speech-to-text didn't return any text. Please try again or type your message.");
        }
        return source.asText();
    }

    public String synthesize(String text, String language) {
        requireConfigured();
        LanguagePipeline pipeline = resolvePipeline(language);

        ObjectNode taskConfig = objectMapper.createObjectNode();
        taskConfig.putObject("language").put("sourceLanguage", language);
        taskConfig.put("serviceId", pipeline.ttsServiceId);
        taskConfig.put("gender", "female");

        ObjectNode task = objectMapper.createObjectNode();
        task.put("taskType", "tts");
        task.set("config", taskConfig);

        ObjectNode inputEntry = objectMapper.createObjectNode();
        inputEntry.put("source", text);

        ObjectNode inputData = objectMapper.createObjectNode();
        inputData.putArray("input").add(inputEntry);

        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("pipelineTasks").add(task);
        body.set("inputData", inputData);

        JsonNode response = callInferenceEndpoint(pipeline, body);
        JsonNode audioContent = response.path("pipelineResponse").path(0).path("audio").path(0).path("audioContent");
        if (!audioContent.isTextual()) {
            logger.error("Bhashini TTS response missing audio[0].audioContent: {}", response);
            throw new BhashiniException("Text-to-speech didn't return audio.");
        }
        return audioContent.asText();
    }

    private JsonNode callInferenceEndpoint(LanguagePipeline pipeline, ObjectNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(pipeline.inferenceApiKeyName, pipeline.inferenceApiKeyValue);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    pipeline.callbackUrl, new HttpEntity<>(body, headers), JsonNode.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Bhashini inference call failed", e);
            throw new BhashiniException("Couldn't reach the speech service. Please try again in a moment.", e);
        }
    }

    /** computeIfAbsent won't cache a thrown exception, so a transient failure just retries next call. */
    private LanguagePipeline resolvePipeline(String language) {
        LanguagePipeline cached = pipelineCache.get(language);
        if (cached != null) {
            return cached;
        }
        LanguagePipeline fetched = fetchPipelineConfig(language);
        pipelineCache.put(language, fetched);
        return fetched;
    }

    private LanguagePipeline fetchPipelineConfig(String language) {
        ObjectNode languageNode = objectMapper.createObjectNode().put("sourceLanguage", language);

        ObjectNode asrTaskConfig = objectMapper.createObjectNode();
        asrTaskConfig.set("language", languageNode.deepCopy());
        ObjectNode asrTask = objectMapper.createObjectNode();
        asrTask.put("taskType", "asr");
        asrTask.set("config", asrTaskConfig);

        ObjectNode ttsTaskConfig = objectMapper.createObjectNode();
        ttsTaskConfig.set("language", languageNode.deepCopy());
        ObjectNode ttsTask = objectMapper.createObjectNode();
        ttsTask.put("taskType", "tts");
        ttsTask.set("config", ttsTaskConfig);

        ObjectNode pipelineRequestConfig = objectMapper.createObjectNode();
        pipelineRequestConfig.put("pipelineId", pipelineId);

        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("pipelineTasks").add(asrTask).add(ttsTask);
        body.set("pipelineRequestConfig", pipelineRequestConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("userID", userId);
        headers.set("ulcaApiKey", apiKey);

        JsonNode response;
        try {
            ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(
                    configUrl, new HttpEntity<>(body, headers), JsonNode.class);
            response = responseEntity.getBody();
        } catch (Exception e) {
            logger.error("Bhashini pipeline config call failed for language '{}'", language, e);
            throw new BhashiniException("Couldn't set up the speech service for this language.", e);
        }

        String asrServiceId = findServiceId(response, "asr");
        String ttsServiceId = findServiceId(response, "tts");
        JsonNode endpoint = response.path("pipelineInferenceAPIEndPoint");
        String callbackUrl = endpoint.path("callbackUrl").asText(null);
        String inferenceApiKeyName = endpoint.path("inferenceApiKey").path("name").asText(null);
        String inferenceApiKeyValue = endpoint.path("inferenceApiKey").path("value").asText(null);

        if (asrServiceId == null || ttsServiceId == null || callbackUrl == null || inferenceApiKeyValue == null) {
            logger.error("Bhashini pipeline config response missing expected fields for language '{}': {}", language, response);
            throw new BhashiniException("Speech service isn't available for this language right now.");
        }
        return new LanguagePipeline(callbackUrl, inferenceApiKeyName, inferenceApiKeyValue, asrServiceId, ttsServiceId);
    }

    private String findServiceId(JsonNode configResponse, String taskType) {
        for (JsonNode taskResponse : configResponse.path("pipelineResponseConfig")) {
            if (taskType.equals(taskResponse.path("taskType").asText())) {
                return taskResponse.path("config").path(0).path("serviceId").asText(null);
            }
        }
        return null;
    }

    private void requireConfigured() {
        if (userId == null || userId.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new BhashiniException(
                    "Voice input/output isn't set up yet — please type your message instead.");
        }
    }
}
