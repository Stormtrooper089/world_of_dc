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
 * Targets Groq's free-tier, OpenAI-compatible chat completions API — the same
 * agentic-tool-calling pattern demonstrated in the AgenticAI training deck's
 * "Agentic Tool Calling" / "Talking to Databases" / "Fully Integrated Customer
 * Care Agent" sections, just against an OpenAI-shaped `tools`/`tool_calls`
 * wire format instead of Anthropic's `tool_use` content blocks. If you'd
 * rather standardize on Anthropic's or Gemini's `functionCall` shape instead,
 * swap the parse/build methods below — VoiceAssistantService only depends on
 * the ToolCallTurn/ToolCall contract, not on this class's HTTP details.
 *
 * NOTE on the model default: Groq's lineup of hosted open models changes over
 * time (they retire/add models fairly often). "llama-3.3-70b-versatile" is
 * current as of this writing and is on Groq's free tier, but it's worth
 * checking https://console.groq.com/docs/models before relying on it in
 * production — override via voiceassistant.llm.model if it's been retired.
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

    @Value("${voiceassistant.llm.base-url:https://api.groq.com/openai/v1/chat/completions}")
    private String baseUrl;

    @Value("${voiceassistant.llm.api-key:}")
    private String apiKey;

    @Value("${voiceassistant.llm.model:llama-3.3-70b-versatile}")
    private String model;

    /**
     * One round-trip to the LLM: send the running conversation plus the tools
     * it's allowed to call. Returns either final text (conversation turn is
     * done) or a list of tool calls the caller must execute and feed back in
     * via appendToolResults() before calling nextTurn() again.
     */
    public ToolCallTurn nextTurn(List<Map<String, Object>> messages, List<Map<String, Object>> toolDefinitions, String systemPrompt) {
        if (isPlaceholderMode()) {
            return placeholderTurn(messages, systemPrompt);
        }

        // Groq's API is OpenAI-shaped: no top-level "system" field — the
        // system prompt is just the first message in the array.
        List<Map<String, Object>> fullMessages = new ArrayList<>();
        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        fullMessages.add(systemMessage);
        fullMessages.addAll(messages);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.set("messages", objectMapper.valueToTree(fullMessages));
        body.set("tools", objectMapper.valueToTree(wrapToolsForOpenAiShape(toolDefinitions)));
        body.put("tool_choice", "auto");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl, new HttpEntity<>(body, headers), JsonNode.class);
        return parseOpenAiCompatibleResponse(response.getBody());
    }

    /**
     * VoiceAssistantService hands us tool definitions in the intermediate
     * {name, description, input_schema} shape. OpenAI/Groq want each tool
     * wrapped as {"type":"function","function":{name, description, parameters}}.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> wrapToolsForOpenAiShape(List<Map<String, Object>> toolDefinitions) {
        List<Map<String, Object>> wrapped = new ArrayList<>();
        for (Map<String, Object> toolDef : toolDefinitions) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", toolDef.get("name"));
            function.put("description", toolDef.get("description"));
            function.put("parameters", toolDef.get("input_schema"));

            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("type", "function");
            wrapper.put("function", function);
            wrapped.add(wrapper);
        }
        return wrapped;
    }

    /**
     * Appends the assistant's tool-call turn and the corresponding tool
     * result messages to the conversation, in the shape Groq/OpenAI require:
     * the assistant message (with its own "tool_calls" array) followed by one
     * separate {"role":"tool", "tool_call_id":..., "content":...} message per
     * call — NOT a single combined message like Anthropic's format. Call this
     * after executing every ToolCall from a non-final ToolCallTurn, then call
     * nextTurn() again to let the model continue.
     */
    @SuppressWarnings("unchecked")
    public void appendToolResults(List<Map<String, Object>> messages, ToolCallTurn turn, Map<String, String> toolCallIdToResultJson) {
        Map<String, Object> assistantMessage = objectMapper.convertValue(turn.getRawAssistantMessage(), Map.class);
        messages.add(assistantMessage);

        for (ToolCall toolCall : turn.getToolCalls()) {
            Map<String, Object> toolResultMessage = new LinkedHashMap<>();
            toolResultMessage.put("role", "tool");
            toolResultMessage.put("tool_call_id", toolCall.getId());
            toolResultMessage.put("content", toolCallIdToResultJson.getOrDefault(toolCall.getId(), "{\"error\":\"tool did not return a result\"}"));
            messages.add(toolResultMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private ToolCallTurn parseOpenAiCompatibleResponse(JsonNode responseBody) {
        JsonNode message = responseBody.path("choices").path(0).path("message");
        JsonNode toolCallsNode = message.path("tool_calls");

        if (toolCallsNode.isArray() && toolCallsNode.size() > 0) {
            List<ToolCall> toolCalls = new ArrayList<>();
            for (JsonNode toolCallNode : toolCallsNode) {
                String id = toolCallNode.path("id").asText();
                JsonNode function = toolCallNode.path("function");
                String name = function.path("name").asText();
                // Unlike Anthropic's structured "input" object, OpenAI/Groq
                // send tool-call arguments as a JSON-encoded STRING that has
                // to be parsed separately.
                String argumentsJson = function.path("arguments").asText("{}");
                Map<String, Object> input;
                try {
                    input = objectMapper.readValue(argumentsJson, Map.class);
                } catch (Exception e) {
                    input = new LinkedHashMap<>();
                }
                toolCalls.add(new ToolCall(id, name, input));
            }
            return ToolCallTurn.toolUse(toolCalls, message);
        }

        return ToolCallTurn.text(message.path("content").asText(""));
    }

    private boolean isPlaceholderMode() {
        return apiKey == null || apiKey.isBlank() || apiKey.startsWith("dummy-");
    }

    private volatile boolean placeholderWarningLogged = false;

    /**
     * Deterministic fallback so the widget is demoable end-to-end before a
     * real LLM key is configured. Deliberately dumb — replace by setting
     * voiceassistant.llm.api-key in application.properties (or the
     * corresponding env var), which routes real conversation through
     * VoiceAssistantService's actual tool definitions instead.
     *
     * Citizen-facing text intentionally says nothing about "placeholder mode"
     * or config keys — that's an operator concern, not something to read
     * aloud to a citizen. It's logged once server-side instead, so whoever's
     * watching the logs knows a real LLM key still needs to be configured.
     */
    private ToolCallTurn placeholderTurn(List<Map<String, Object>> messages, String systemPrompt) {
        if (!placeholderWarningLogged) {
            placeholderWarningLogged = true;
            org.slf4j.LoggerFactory.getLogger(ChatToolCallingClient.class).warn(
                    "Voice assistant is running in PLACEHOLDER mode (no voiceassistant.llm.api-key configured) — "
                    + "citizens are getting canned responses, not real LLM-driven conversation.");
        }

        boolean identityKnown = systemPrompt != null && systemPrompt.contains("already logged in and verified");
        String lastUserText = lastUserMessageText(messages).toLowerCase();

        if (lastUserText.contains("status") || lastUserText.contains("track") || lastUserText.contains("my complaint")) {
            return ToolCallTurn.text(identityKnown
                    ? "I can check that for you — do you have the complaint number, or would you like me to look up your recent complaints?"
                    : "Please tell me your complaint number so I can check its status.");
        }
        if (lastUserText.contains("complaint") || lastUserText.contains("problem") || lastUserText.contains("issue")) {
            return ToolCallTurn.text(identityKnown
                    ? "I can help file that. Please tell me a short description of the issue and where it's happening."
                    : "I can help file that complaint. Please share your registered mobile number and a short description of the issue and its location.");
        }
        return ToolCallTurn.text(identityKnown
                ? "Namaskar! I can file a complaint, check on one of your complaints, or tell you about a district service. What would you like to do?"
                : "Namaskar! I can help you file a complaint, check a complaint's status, or find a district service. What would you like to do?");
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
