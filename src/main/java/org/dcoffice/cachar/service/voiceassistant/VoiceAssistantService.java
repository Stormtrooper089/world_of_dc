package org.dcoffice.cachar.service.voiceassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.Complaint;
import org.dcoffice.cachar.entity.ComplaintCategory;
import org.dcoffice.cachar.entity.ComplaintHistory;
import org.dcoffice.cachar.entity.DistrictService;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.dcoffice.cachar.service.ComplaintHistoryService;
import org.dcoffice.cachar.service.ComplaintService;
import org.dcoffice.cachar.service.DistrictServiceRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
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
 * Conversation state is kept in a plain in-memory map keyed by sessionId.
 * That's fine for a single-instance dev/demo deployment; before this goes to
 * production behind more than one app instance, move sessions to Redis (or a
 * Mongo capped collection with a TTL index) so a session survives a restart
 * and works across replicas — the same operational note that applies to
 * Render's ephemeral filesystem for file uploads (see CLAUDE.md).
 */
@Service
public class VoiceAssistantService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceAssistantService.class);
    private static final int MAX_TOOL_ITERATIONS = 4;

    private static final String SYSTEM_PROMPT =
            "You are the Cachar District Office citizen voice assistant. Citizens speak to you instead of typing, "
            + "so keep replies short, plain-spoken, and free of markdown or lists — this text will be read aloud. "
            + "You can: (1) file a new complaint via the create_complaint tool, (2) check an existing complaint's "
            + "status via the track_complaint tool, (3) look up district services (property tax, trade license, "
            + "waste pickup, etc.) via the search_district_services tool. "
            + "Never invent a complaint number, status, or service detail — only state what a tool actually returned. "
            + "If create_complaint reports the citizen is not verified, tell them to complete OTP login on the portal "
            + "first, then come back — do not attempt to file the complaint anyway.";

    private final ChatToolCallingClient llmClient;
    private final ComplaintService complaintService;
    private final ComplaintHistoryService complaintHistoryService;
    private final DistrictServiceRegistryService serviceRegistryService;
    private final CitizenRepository citizenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, List<Map<String, Object>>> conversations = new ConcurrentHashMap<>();

    public VoiceAssistantService(ChatToolCallingClient llmClient,
                                  ComplaintService complaintService,
                                  ComplaintHistoryService complaintHistoryService,
                                  DistrictServiceRegistryService serviceRegistryService,
                                  CitizenRepository citizenRepository) {
        this.llmClient = llmClient;
        this.complaintService = complaintService;
        this.complaintHistoryService = complaintHistoryService;
        this.serviceRegistryService = serviceRegistryService;
        this.citizenRepository = citizenRepository;
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

    public VoiceReply handleTurn(String sessionId, String transcript) {
        List<Map<String, Object>> conversation = conversations.computeIfAbsent(sessionId, id -> new ArrayList<>());

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", transcript);
        conversation.add(userMessage);

        String[] actionTaken = new String[1];
        String[] complaintNumberHolder = new String[1];

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            ToolCallTurn turn = llmClient.nextTurn(conversation, toolDefinitions(), SYSTEM_PROMPT);

            if (turn.isFinal()) {
                Map<String, Object> assistantMessage = new LinkedHashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", turn.getFinalText());
                conversation.add(assistantMessage);
                return new VoiceReply(turn.getFinalText(), actionTaken[0], complaintNumberHolder[0]);
            }

            Map<String, String> resultsByCallId = new LinkedHashMap<>();
            for (ToolCall call : turn.getToolCalls()) {
                String resultJson = executeTool(call, actionTaken, complaintNumberHolder);
                resultsByCallId.put(call.getId(), resultJson);
            }
            llmClient.appendToolResults(conversation, turn, resultsByCallId);
        }

        logger.warn("Voice assistant hit MAX_TOOL_ITERATIONS for session {}", sessionId);
        return new VoiceReply(
                "Sorry, I'm having trouble completing that right now. Please try again, or use the web form.",
                actionTaken[0], complaintNumberHolder[0]);
    }

    private String executeTool(ToolCall call, String[] actionTaken, String[] complaintNumberHolder) {
        try {
            switch (call.getName()) {
                case "create_complaint":
                    return createComplaint(call.getInput(), actionTaken, complaintNumberHolder);
                case "track_complaint":
                    return trackComplaint(call.getInput(), actionTaken, complaintNumberHolder);
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

    private String createComplaint(Map<String, Object> input, String[] actionTaken, String[] complaintNumberHolder) {
        String mobileNumber = str(input.get("mobileNumber"));
        if (mobileNumber == null || !mobileNumber.matches("^[6-9]\\d{9}$")) {
            return toJson(Map.of("error", "A valid 10-digit mobile number is required before filing a complaint."));
        }

        // Security note: deliberately requires an ALREADY-VERIFIED citizen record rather
        // than auto-creating one (the officer-assisted complaint flow elsewhere in this
        // codebase fabricates an unverified "Test Citizen" in that situation — see the
        // architecture review's finding B5. We don't repeat that here: an unverified or
        // unknown mobile number is turned away with instructions to complete OTP login.)
        Optional<Citizen> citizenOpt = citizenRepository.findByMobileNumber(mobileNumber);
        if (citizenOpt.isEmpty() || !citizenOpt.get().isVerified()) {
            return toJson(Map.of(
                    "error", "NOT_VERIFIED",
                    "message", "This mobile number has not completed OTP verification on the citizen portal yet."));
        }

        String subject = str(input.get("subject"));
        String description = str(input.get("description"));
        String categoryRaw = str(input.get("category"));
        String location = str(input.get("location"));

        if (subject == null || description == null) {
            return toJson(Map.of("error", "Both a short subject and a fuller description are required."));
        }

        Complaint complaint = new Complaint();
        complaint.setCitizenId(citizenOpt.get().getId());
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

        // Map.of() throws on any null value, and several of these entity fields are
        // legitimately nullable (e.g. a brand-new complaint has no updatedAt yet), so
        // this is built with a plain LinkedHashMap rather than Map.of.
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("complaintNumber", complaint.getComplaintNumber());
        result.put("status", complaint.getStatus() == null ? "UNKNOWN" : complaint.getStatus().name());
        result.put("subject", nullToEmpty(complaint.getSubject()));
        result.put("lastUpdated", complaint.getUpdatedAt() == null ? "" : complaint.getUpdatedAt().toString());
        result.put("historyEntryCount", history.size());
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

    private List<Map<String, Object>> toolDefinitions() {
        return Arrays.asList(
                tool("create_complaint",
                        "File a new citizen complaint. Requires the citizen's mobile number to already be "
                        + "OTP-verified on the portal; if not verified, tell the citizen to verify first.",
                        Map.of(
                                "mobileNumber", schemaString("10-digit mobile number, digits only"),
                                "subject", schemaString("Short one-line summary of the issue"),
                                "description", schemaString("Fuller description of the issue as the citizen described it"),
                                "category", schemaString("Best-matching category, e.g. GARBAGE_NOT_COLLECTED, WATER_SUPPLY, ROAD_DAMAGE, STREET_LIGHT, DRAIN_BLOCKAGE, OTHER"),
                                "location", schemaString("Locality, ward, or landmark the citizen mentioned")),
                        List.of("mobileNumber", "subject", "description")),
                tool("track_complaint",
                        "Look up an existing complaint's current status by its complaint number.",
                        Map.of("complaintNumber", schemaString("The complaint number the citizen was given when they filed, e.g. CMP-2026-000123")),
                        List.of("complaintNumber")),
                tool("search_district_services",
                        "Search the district's service registry (property tax, trade license, waste pickup, etc.) "
                        + "to answer general questions about what services exist and how they work.",
                        Map.of("query", schemaString("Keywords from the citizen's question")),
                        List.of("query"))
        );
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
