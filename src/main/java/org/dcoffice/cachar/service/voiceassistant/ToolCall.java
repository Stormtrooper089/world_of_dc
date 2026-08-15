package org.dcoffice.cachar.service.voiceassistant;

import java.util.Map;

/** One tool the LLM asked to invoke, plus the arguments it chose. */
public class ToolCall {
    private final String id;
    private final String name;
    private final Map<String, Object> input;

    public ToolCall(String id, String name, Map<String, Object> input) {
        this.id = id;
        this.name = name;
        this.input = input;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getInput() {
        return input;
    }
}
