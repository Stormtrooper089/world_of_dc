package org.dcoffice.cachar.service.voiceassistant;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;

/**
 * One turn back from the LLM: either it's done (finalText is set, toolCalls is
 * empty) or it wants one or more tools executed before it continues.
 *
 * rawAssistantMessage is the exact JSON the model returned for this turn. When
 * tool calls are present, the Anthropic Messages API requires the assistant's
 * tool_use turn to be replayed back verbatim in the next request's message
 * history (immediately followed by a user-role message containing the
 * tool_result blocks) — so VoiceAssistantService just stores this node rather
 * than re-deriving it.
 */
public class ToolCallTurn {

    private final String finalText;
    private final List<ToolCall> toolCalls;
    private final JsonNode rawAssistantMessage;

    private ToolCallTurn(String finalText, List<ToolCall> toolCalls, JsonNode rawAssistantMessage) {
        this.finalText = finalText;
        this.toolCalls = toolCalls;
        this.rawAssistantMessage = rawAssistantMessage;
    }

    public static ToolCallTurn text(String finalText) {
        return new ToolCallTurn(finalText, Collections.emptyList(), null);
    }

    public static ToolCallTurn toolUse(List<ToolCall> toolCalls, JsonNode rawAssistantMessage) {
        return new ToolCallTurn(null, toolCalls, rawAssistantMessage);
    }

    public boolean isFinal() {
        return toolCalls.isEmpty();
    }

    public String getFinalText() {
        return finalText;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public JsonNode getRawAssistantMessage() {
        return rawAssistantMessage;
    }
}
