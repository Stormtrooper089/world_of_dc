package org.dcoffice.cachar.service.voiceassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.Complaint;
import org.dcoffice.cachar.entity.ComplaintCategory;
import org.dcoffice.cachar.entity.ComplaintHistory;
import org.dcoffice.cachar.entity.DistrictService;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.dcoffice.cachar.repository.ComplaintRepository;
import org.dcoffice.cachar.service.ComplaintHistoryService;
import org.dcoffice.cachar.service.ComplaintService;
import org.dcoffice.cachar.service.DistrictServiceRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates one turn of the citizen voice assistant: runs the LLM's
 * agentic tool-calling loop (see the AgenticAI deck's "Agentic Tool Calling"
 * / "Talking to Databases" sections) against this codebase's *real* services
 * — ComplaintService, ComplaintHistoryService, DistrictServiceRegistryService
 * — instead of a toy demo database.
 *
 * IDENTITY: when the citizen is logged in, VoiceAssistantController passes
 * down their already-verified citizenId/mobileNumber/name (derived from the
 * JWT, not from anything the client claims in the request body). This method
 * folds that into the system prompt and the tool schemas so the LLM never
 * asks "what's your mobile number" — it already knows, the same way a human
 * call-center agent looking at caller ID would. Anonymous callers (no token)
 * can still use status lookup and service Q&A; filing a new complaint without
 * being logged in falls back to the slower "state and verify your mobile
 * number" path.
 *
 * Conversation state is kept in a plain in-memory map keyed by sessionId.
 * That's fine for a single-instance dev/demo deployment; before this goes to
 * production behind more than one app instance, move sessions to Redis (or a
 * Mongo capped collection with a TTL index) so a session survives a restart
 * and works across replicas.
 */
@Service
public class VoiceAssistantService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceAssistantService.class);
    private static final int MAX_TOOL_ITERATIONS = 4;
    private static final int MAX_RECENT_COMPLAINTS = 5;

    private final ChatToolCallingClient llmClient;
    private final ComplaintService complaintService;
    private final ComplaintHistoryService complaintHistoryService;
    private final DistrictServiceRegistryService serviceRegistryService;
    private final CitizenRepository citizenRepository;
    private final ComplaintRepository complaintRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, List<Map<String, Object>>> conversations = new ConcurrentHashMap<>();

    public VoiceAssistantService(ChatToolCallingClient llmClient,
                                  ComplaintService complaintService,
                                  ComplaintHistoryService complaintHistoryService,
                                  DistrictServiceRegistryService serviceRegistryService,
                                  CitizenRepository citizenRepository,
                                  ComplaintRepository complaintRepository) {
        this.llmClient = llmClient;
        this.complaintService = complaintService;
        this.complaintHistoryService = complaintHistoryService;
        this.serviceRegistryService = serviceRegistryService;
        this.citizenRepository = citizenRepository;
        this.complaintRepository = complaintRepository;
    }

    public static class VoiceReply {
        public final String text;
        public final String actionTaken;
        public final String complaintNumber;

        VoiceReply(String text, String actionTaken, String complaintNumber) {
            this.text = text;
            this.actionTaken = actionTaken;
            this.complaintNumber = complaintNumber;
        }
    }

    /** Per-turn identity context, threaded through tool execution. Never trust anything but this. */
    private static class Identity {
        final String citizenId;
        final String mobileNumber;
        final String name;

        Identity(String citizenId, String mobileNumber, String name) {
            this.citizenId = citizenId;
            this.mobileNumber = mobileNumber;
            this.name = name;
        }

        boolean isKnown() {
            return citizenId != null;
        }
    }

    public VoiceReply handleTurn(String sessionId, String transcript, String citizenId, String citizenMobileNumber, String citizenName) {
        Identity identity = new Identity(citizenId, citizenMobileNumber, citizenName);
        List<Map<String, Object>> conversation = conversations.computeIfAbsent(sessionId, id -> new ArrayList<>());

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", transcript);
        conversation.add(userMessage);

        String[] actionTaken = new String[1];
        String[] complaintNumberHolder = new String[1];
        String systemPrompt = buildSystemPrompt(identity);
        List<Map<String, Object>> tools = toolDefinitions(identity);

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            ToolCallTurn turn = llmClient.nextTurn(conversation, tools, systemPrompt);

            if (turn.isFinal()) {
                Map<String, Object> assistantMessage = new LinkedHashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", turn.getFinalText());
                conversation.add(assistantMessage);
                return new VoiceReply(turn.getFinalText(), actionTaken[0], complaintNumberHolder[0]);
            }

            Map<String, String> resultsByCallId = new LinkedHashMap<>();
            for (ToolCall call : turn.getToolCalls()) {
                String resultJson = executeTool(call, identity, actionTaken, complaintNumberHolder);
                resultsByCallId.put(call.getId(), resultJson);
            }
            llmClient.appendToolResults(conversation, turn, resultsByCallId);
        }

        logger.warn("Voice assistant hit MAX_TOOL_ITERATIONS for session {}", sessionId);
        return new VoiceReply(
                "Sorry, I'm having trouble completing that right now. Please try again, or use the web form.",
                actionTaken[0], complaintNumberHolder[0]);
    }

    private String buildSystemPrompt(Identity identity) {
        StringBuilder prompt = new StringBuilder(
                "You are the Cachar District Office citizen voice assistant. Citizens speak to you instead of "
                + "typing, so keep replies short, plain-spoken, and free of markdown or lists — this text will be "
                + "read aloud. You can: (1) file a new complaint via the create_complaint tool, (2) check an "
                + "existing complaint's status via the track_complaint tool, (3) look up district services "
                + "(property tax, trade license, waste pickup, etc.) via the search_district_services tool. "
                + "Never invent a complaint number, status, or service detail — only state what a tool actually returned.");

        if (identity.isKnown()) {
            prompt.append(" IDENTITY: this citizen is already logged in and verified")
                    .append(identity.name != null ? " (name: " + identity.name + ")" : "")
                    .append(". Their mobile number is already on file — do NOT ask for it, and do NOT read it back "
                            + "unless they ask. When filing a complaint, only ask about the subject, description, "
                            + "location, and category. You also have a list_my_complaints tool to look up this "
                            + "citizen's own recent complaints without needing a complaint number — prefer it when "
                            + "they say things like \"my complaint\" or \"my last complaint\" rather than asking "
                            + "them to recite a number they may not remember.");
        } else {
            prompt.append(" IDENTITY: this citizen is not logged in. If they want to file a complaint, ask for "
                    + "their registered mobile number first — create_complaint will tell you if it's not yet "
                    + "OTP-verified, in which case tell them to verify on the portal first. Status lookups by "
                    + "complaint number and service questions don't require login.");
        }
        return prompt.toString();
    }

    private String executeTool(ToolCall call, Identity identity, String[] actionTaken, String[] complaintNumberHolder) {
        try {
            switch (call.getName()) {
                case "create_complaint":
                    return createComplaint(call.getInput(), identity, actionTaken, complaintNumberHolder);
                case "track_complaint":
                    return trackComplaint(call.getInput(), actionTaken, complaintNumberHolder);
                case "list_my_complaints":
                    return listMyComplaints(identity);
                case "search_district_services":
                    return searchDistrictServices(call.getInput());
                default:
                    return toJson(Map.of("error", "Unknown tool: " + call.getName()));
            }
        } catch (Exception e) {
            logger.error("Voice assistant tool '{}' failed", call.getName(), e);
            return toJson(Map.of("error", "Tool failed: " + e.getMessage()));
        }
    }

    // ---- Tool implementations, each calling the SAME services the REST API uses ----

    private String createComplaint(Map<String, Object> input, Identity identity, String[] actionTaken, String[] complaintNumberHolder) {
        String resolvedCitizenId;

        if (identity.isKnown()) {
            // Logged-in path: identity already verified by the JWT filter, no extra lookup needed.
            resolvedCitizenId = identity.citizenId;
        } else {
            // Anonymous fallback: same "must already be OTP-verified" gate as before, deliberately NOT
            // auto-creating an unverified citizen the way the officer-assisted flow elsewhere does.
            String mobileNumber = str(input.get("mobileNumber"));
            if (mobileNumber == null || !mobileNumber.matches("^[6-9]\\d{9}$")) {
                return toJson(Map.of("error", "A valid 10-digit mobile number is required before filing a complaint."));
            }
            Optional<Citizen> citizenOpt = citizenRepository.findByMobileNumber(mobileNumber);
            if (citizenOpt.isEmpty() || !citizenOpt.get().isVerified()) {
                return toJson(Map.of(
                        "error", "NOT_VERIFIED",
                        "message", "This mobile number has not completed OTP verification on the citizen portal yet."));
            }
            resolvedCitizenId = citizenOpt.get().getId();
        }

        String subject = str(input.get("subject"));
        String description = str(input.get("description"));
        String categoryRaw = str(input.get("category"));
        String location = str(input.get("location"));

        if (subject == null || description == null) {
            return toJson(Map.of("error", "Both a short subject and a fuller description are required."));
        }

        Complaint complaint = new Complaint();
        complaint.setCitizenId(resolvedCitizenId);
        complaint.setSubject(subject);
        complaint.setDescription(description);
        complaint.setCategory(parseCategory(categoryRaw));
        complaint.setLocation(location);

        Complaint saved = complaintService.createComplaint(complaint, null);

        actionTaken[0] = "COMPLAINT_CREATED";
        complaintNumberHolder[0] = saved.getComplaintNumber();

        return toJson(Map.of(
                "complaintNumber", saved.getComplaintNumber(),
                "status", saved.getStatus().name(),
                "message", "Complaint filed successfully."));
    }

    private String trackComplaint(Map<String, Object> input, String[] actionTaken, String[] complaintNumberHolder) {
        String complaintNumber = str(input.get("complaintNumber"));
        if (complaintNumber == null) {
            return toJson(Map.of("error", "A complaint number is required to check status."));
        }

        Optional<Complaint> complaintOpt = complaintService.findByComplaintNumber(complaintNumber);
        if (complaintOpt.isEmpty()) {
            return toJson(Map.of("error", "No complaint found with number " + complaintNumber + "."));
        }

        Complaint complaint = complaintOpt.get();
        List<ComplaintHistory> history = complaintHistoryService.getComplaintHistory(complaintNumber);

        actionTaken[0] = "COMPLAINT_STATUS";
        complaintNumberHolder[0] = complaintNumber;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("complaintNumber", complaint.getComplaintNumber());
        result.put("status", complaint.getStatus() == null ? "UNKNOWN" : complaint.getStatus().name());
        result.put("subject", nullToEmpty(complaint.getSubject()));
        result.put("lastUpdated", complaint.getUpdatedAt() == null ? "" : complaint.getUpdatedAt().toString());
        result.put("historyEntryCount", history.size());
        return toJson(result);
    }

    /** Only reachable when identity.isKnown() — see toolDefinitions(). */
    private String listMyComplaints(Identity identity) {
        if (!identity.isKnown()) {
            return toJson(Map.of("error", "Must be logged in to list your own complaints."));
        }
        List<Complaint> complaints = complaintRepository.findByCitizenId(identity.citizenId);
        complaints.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        List<Map<String, Object>> summarized = new ArrayList<>();
        for (Complaint complaint : complaints.subList(0, Math.min(complaints.size(), MAX_RECENT_COMPLAINTS))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("complaintNumber", nullToEmpty(complaint.getComplaintNumber()));
            row.put("subject", nullToEmpty(complaint.getSubject()));
            row.put("status", complaint.getStatus() == null ? "UNKNOWN" : complaint.getStatus().name());
            summarized.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", complaints.size());
        result.put("complaints", summarized);
        return toJson(result);
    }

    private String searchDistrictServices(Map<String, Object> input) {
        String query = str(input.get("query"));
        List<DistrictService> services = serviceRegistryService.publicServices(query, null, null);

        List<Map<String, Object>> summarized = new ArrayList<>();
        for (DistrictService service : services.subList(0, Math.min(services.size(), 5))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serviceName", nullToEmpty(service.getServiceName()));
            row.put("department", nullToEmpty(service.getDepartment()));
            row.put("description", nullToEmpty(service.getDescription()));
            summarized.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("results", summarized);
        return toJson(result);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    // ---- Tool schemas the LLM sees (Anthropic input_schema / JSON Schema format) ----
    // Built per-request so a logged-in citizen's create_complaint tool doesn't even
    // expose a mobileNumber field for the LLM to (mis)ask about.

    private List<Map<String, Object>> toolDefinitions(Identity identity) {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(createComplaintTool(identity));
        tools.add(tool("track_complaint",
                "Look up an existing complaint's current status by its complaint number.",
                Map.of("complaintNumber", schemaString("The complaint number the citizen was given when they filed, e.g. CMP-2026-000123")),
                List.of("complaintNumber")));

        if (identity.isKnown()) {
            tools.add(tool("list_my_complaints",
                    "List the logged-in citizen's own recent complaints (no arguments needed) — use this instead "
                    + "of asking for a complaint number when they refer to \"my complaint\" or \"my last complaint\".",
                    Map.of(), List.of()));
        }

        tools.add(tool("search_district_services",
                "Search the district's service registry (property tax, trade license, waste pickup, etc.) "
                + "to answer general questions about what services exist and how they work.",
                Map.of("query", schemaString("Keywords from the citizen's question")),
                List.of("query")));
        return tools;
    }

    private Map<String, Object> createComplaintTool(Identity identity) {
        if (identity.isKnown()) {
            // No mobileNumber property at all — the LLM has nothing to ask for.
            return tool("create_complaint",
                    "File a new citizen complaint for the already-logged-in citizen.",
                    Map.of(
                            "subject", schemaString("Short one-line summary of the issue"),
                            "description", schemaString("Fuller description of the issue as the citizen described it"),
                            "category", schemaString("Best-matching category, e.g. GARBAGE_NOT_COLLECTED, WATER_SUPPLY, ROAD_DAMAGE, STREET_LIGHT, DRAIN_BLOCKAGE, OTHER"),
                            "location", schemaString("Locality, ward, or landmark the citizen mentioned")),
                    List.of("subject", "description"));
        }
        return tool("create_complaint",
                "File a new citizen complaint. Requires the citizen's mobile number to already be "
                + "OTP-verified on the portal; if not verified, tell the citizen to verify first.",
                Map.of(
                        "mobileNumber", schemaString("10-digit mobile number, digits only"),
                        "subject", schemaString("Short one-line summary of the issue"),
                        "description", schemaString("Fuller description of the issue as the citizen described it"),
                        "category", schemaString("Best-matching category, e.g. GARBAGE_NOT_COLLECTED, WATER_SUPPLY, ROAD_DAMAGE, STREET_LIGHT, DRAIN_BLOCKAGE, OTHER"),
                        "location", schemaString("Locality, ward, or landmark the citizen mentioned")),
                List.of("mobileNumber", "subject", "description"));
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);

        Map<String, Object> toolDef = new LinkedHashMap<>();
        toolDef.put("name", name);
        toolDef.put("description", description);
        toolDef.put("input_schema", schema);
        return toolDef;
    }

    private Map<String, Object> schemaString(String description) {
        return Map.of("type", "string", "description", description);
    }

    private ComplaintCategory parseCategory(String raw) {
        if (raw == null) {
            return ComplaintCategory.OTHER;
        }
        try {
            return ComplaintCategory.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return ComplaintCategory.OTHER;
        }
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{\"error\":\"failed to serialize tool result\"}";
        }
    }
}
