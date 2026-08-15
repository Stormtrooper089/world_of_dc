package org.dcoffice.cachar.service.voiceassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around a cloud LLM's tool-calling ("function calling") API.
 *
 * Defaults to Anthropic's Messages API (tool_use content blocks) — the same
 * agentic-tool-calling pattern demonstrated in the AgenticAI training deck's
 * "Agentic Tool Calling" / "Talking to Databases" / "Fully Integrated Customer
 * Care Agent" sections. If you'd rather standardize on OpenAI's `tools` /
 * `tool_calls` shape or Gemini's `functionCall` shape instead, swap the two
 * private parse/build methods below — VoiceAssistantService only depends on
 * the ToolCallTurn/ToolCall contract, not on this class's HTTP details.
 *
 * Mirrors the existing UpyogClient pattern elsewhere in this codebase: with no
 * API key configured, it runs in "placeholder mode" with a small canned
 * router instead of failing, so this scaffold builds and is demoable before
 * you've wired in real credentials.
 */
@Component
public class ChatToolCallingClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${voiceassistant.llm.base-url:https://api.anthropic.com/v1/messages}")
    private String baseUrl;

    @Value("${voiceassistant.llm.api-key:}")
    private String apiKey;

    @Value("${voiceassistant.llm.model:claude-sonnet-4-5}")
    private String model;

    @Value("${voiceassistant.llm.anthropic-version:2023-06-01}")
    private String anthropicVersion;

    /**
     * One round-trip to the LLM: send the running conversation plus the tools
     * it's allowed to call. Returns either final text (conversation turn is
     * done) or a list of tool calls the caller must execute and feed back in
     * via appendToolResults() before calling nextTurn() again.
     */
    public ToolCallTurn nextTurn(List<Map<String, Object>> messages, List<Map<String, Object>> toolDefinitions, String systemPrompt) {
        if (isPlaceholderMode()) {
            return placeholderTurn(messages);
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.put("system", systemPrompt);
        body.set("messages", objectMapper.valueToTree(messages));
        body.set("tools", objectMapper.valueToTree(toolDefinitions));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", anthropicVersion);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl, new HttpEntity<>(body, headers), JsonNode.class);
        return parseAnthropicResponse(response.getBody());
    }

    /**
     * Appends the assistant's tool_use turn and the corresponding tool_result
     * turn to the conversation, in the shape Anthropic's API requires. Call
     * this after executing every ToolCall from a non-final ToolCallTurn, then
     * call nextTurn() again to let the model continue.
     */
    public void appendToolResults(List<Map<String, Object>> messages, ToolCallTurn turn, Map<String, String> toolCallIdToResultJson) {
        Map<String, Object> assistantMessage = new LinkedHashMap<>();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content", objectMapper.convertValue(turn.getRawAssistantMessage().get("content"), Object.class));
        messages.add(assistantMessage);

        List<Map<String, Object>> resultBlocks = new ArrayList<>();
        for (ToolCall toolCall : turn.getToolCalls()) {
            Map<String, Object> resultBlock = new LinkedHashMap<>();
            resultBlock.put("type", "tool_result");
            resultBlock.put("tool_use_id", toolCall.getId());
            resultBlock.put("content", toolCallIdToResultJson.getOrDefault(toolCall.getId(), "{\"error\":\"tool did not return a result\"}"));
            resultBlocks.add(resultBlock);
        }
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", resultBlocks);
        messages.add(userMessage);
    }

    private ToolCallTurn parseAnthropicResponse(JsonNode responseBody) {
        List<ToolCall> toolCalls = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();

        JsonNode contentBlocks = responseBody.path("content");
        for (JsonNode block : contentBlocks) {
            String type = block.path("type").asText();
            if ("tool_use".equals(type)) {
                Map<String, Object> input = objectMapper.convertValue(block.path("input"), Map.class);
                toolCalls.add(new ToolCall(block.path("id").asText(), block.path("name").asText(), input));
            } else if ("text".equals(type)) {
                textBuilder.append(block.path("text").asText());
            }
        }

        if (!toolCalls.isEmpty()) {
            return ToolCallTurn.toolUse(toolCalls, responseBody);
        }
        return ToolCallTurn.text(textBuilder.toString());
    }

    private boolean isPlaceholderMode() {
        return apiKey == null || apiKey.isBlank() || apiKey.startsWith("dummy-");
    }

    /**
     * Deterministic fallback so the widget is demoable end-to-end before a
     * real LLM key is configured. Deliberately dumb — replace by setting
     * voiceassistant.llm.api-key in application.properties (or the
     * corresponding env var), which routes real conversation through
     * VoiceAssistantService's actual tool definitions instead.
     */
    private ToolCallTurn placeholderTurn(List<Map<String, Object>> messages) {
        String lastUserText = lastUserMessageText(messages).toLowerCase();
        if (lastUserText.contains("status") || lastUserText.contains("track")) {
            return ToolCallTurn.text("Please tell me your complaint number so I can check its status. "
                    + "(Placeholder mode — set voiceassistant.llm.api-key to enable full conversation and voice complaint filing.)");
        }
        if (lastUserText.contains("complaint") || lastUserText.contains("problem") || lastUserText.contains("issue")) {
            return ToolCallTurn.text("I can help file that complaint. Please share your registered mobile number and a short "
                    + "description of the issue and its location. (Placeholder mode — set voiceassistant.llm.api-key for full tool-calling.)");
        }
        return ToolCallTurn.text("Namaskar! I can help you file a complaint, check a complaint's status, or find a district service. "
                + "What would you like to do? (Placeholder mode — set voiceassistant.llm.api-key in application.properties to enable the real assistant.)");
    }

    private String lastUserMessageText(List<Map<String, Object>> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> message = messages.get(i);
            if ("user".equals(message.get("role"))) {
                Object content = message.get("content");
                return content == null ? "" : content.toString();
            }
        }
        return "";
    }
}
