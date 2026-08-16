package org.dcoffice.cachar.service.voiceassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.Complaint;
import org.dcoffice.cachar.entity.ComplaintCategory;
import org.dcoffice.cachar.entity.ComplaintHistory;
import org.dcoffice.cachar.entity.DistrictService;
import org.dcoffice.cachar.entity.PropertyTaxAccount;
import org.dcoffice.cachar.entity.WasteCategory;
import org.dcoffice.cachar.entity.WastePickupRequest;
import org.dcoffice.cachar.entity.WasteQuantityEstimate;
import org.dcoffice.cachar.entity.WasteUrgency;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.dcoffice.cachar.repository.ComplaintRepository;
import org.dcoffice.cachar.service.ComplaintHistoryService;
import org.dcoffice.cachar.service.ComplaintService;
import org.dcoffice.cachar.service.DistrictServiceRegistryService;
import org.dcoffice.cachar.service.PropertyTaxService;
import org.dcoffice.cachar.service.WastePickupService;
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
    private final PropertyTaxService propertyTaxService;
    private final WastePickupService wastePickupService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, List<Map<String, Object>>> conversations = new ConcurrentHashMap<>();

    public VoiceAssistantService(ChatToolCallingClient llmClient,
                                  ComplaintService complaintService,
                                  ComplaintHistoryService complaintHistoryService,
                                  DistrictServiceRegistryService serviceRegistryService,
                                  CitizenRepository citizenRepository,
                                  ComplaintRepository complaintRepository,
                                  PropertyTaxService propertyTaxService,
                                  WastePickupService wastePickupService) {
        this.llmClient = llmClient;
        this.complaintService = complaintService;
        this.complaintHistoryService = complaintHistoryService;
        this.serviceRegistryService = serviceRegistryService;
        this.citizenRepository = citizenRepository;
        this.complaintRepository = complaintRepository;
        this.propertyTaxService = propertyTaxService;
        this.wastePickupService = wastePickupService;
    }

    public static class VoiceReply {
        public final String text;
        public final String actionTaken;
        public final String complaintNumber;
        public final String trackingNumber;

        VoiceReply(String text, String actionTaken, String complaintNumber, String trackingNumber) {
            this.text = text;
            this.actionTaken = actionTaken;
            this.complaintNumber = complaintNumber;
            this.trackingNumber = trackingNumber;
        }
    }

    /**
     * Per-turn identity (and geo) context, threaded through tool execution.
     * Never trust anything but this — in particular, latitude/longitude come
     * from the widget's own navigator.geolocation call, never from the LLM's
     * create_complaint arguments, so the model can't invent coordinates.
     */
    private static class Identity {
        final String citizenId;
        final String mobileNumber;
        final String name;
        final Double latitude;
        final Double longitude;

        Identity(String citizenId, String mobileNumber, String name, Double latitude, Double longitude) {
            this.citizenId = citizenId;
            this.mobileNumber = mobileNumber;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        boolean isKnown() {
            return citizenId != null;
        }
    }

    public VoiceReply handleTurn(String sessionId, String transcript, String citizenId, String citizenMobileNumber,
                                  String citizenName, Double latitude, Double longitude) {
        Identity identity = new Identity(citizenId, citizenMobileNumber, citizenName, latitude, longitude);
        List<Map<String, Object>> conversation = conversations.computeIfAbsent(sessionId, id -> new ArrayList<>());

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", transcript);
        conversation.add(userMessage);

        String[] actionTaken = new String[1];
        String[] complaintNumberHolder = new String[1];
        String[] trackingNumberHolder = new String[1];
        String systemPrompt = buildSystemPrompt(identity);
        List<Map<String, Object>> tools = toolDefinitions(identity);

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            ToolCallTurn turn = llmClient.nextTurn(conversation, tools, systemPrompt);

            if (turn.isFinal()) {
                Map<String, Object> assistantMessage = new LinkedHashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", turn.getFinalText());
                conversation.add(assistantMessage);
                return new VoiceReply(turn.getFinalText(), actionTaken[0], complaintNumberHolder[0], trackingNumberHolder[0]);
            }

            Map<String, String> resultsByCallId = new LinkedHashMap<>();
            for (ToolCall call : turn.getToolCalls()) {
                String resultJson = executeTool(call, identity, actionTaken, complaintNumberHolder, trackingNumberHolder);
                resultsByCallId.put(call.getId(), resultJson);
            }
            llmClient.appendToolResults(conversation, turn, resultsByCallId);
        }

        logger.warn("Voice assistant hit MAX_TOOL_ITERATIONS for session {}", sessionId);
        return new VoiceReply(
                "Sorry, I'm having trouble completing that right now. Please try again, or use the web form.",
                actionTaken[0], complaintNumberHolder[0], trackingNumberHolder[0]);
    }

    private String buildSystemPrompt(Identity identity) {
        StringBuilder prompt = new StringBuilder(
                "You are the Cachar District Office citizen voice assistant. Citizens speak to you instead of "
                + "typing, so keep replies short, plain-spoken, and free of markdown or lists — this text will be "
                + "read aloud. You can help with exactly these things: (1) file a new complaint via create_complaint, "
                + "(2) check an existing complaint's status via track_complaint, (3) request a waste/garbage pickup "
                + "via create_waste_pickup_request, (4) look up district services (property tax, trade license, "
                + "etc.) via search_district_services"
                + (identity.isKnown() ? ", (5) check this citizen's own property tax dues via check_property_tax_due." : "."));

        prompt.append(
                " CRITICAL — before doing anything else: if the citizen has not clearly and specifically told you "
                + "which of the above they want, do NOT call any tool and do NOT guess. A vague message like "
                + "\"I have a problem\", \"hello\", \"can you help me\", or just a greeting is NOT enough to infer "
                + "intent — in that case, briefly restate the short list of things you can help with and ask them "
                + "to pick one, then stop and wait for their answer. Never call create_complaint, "
                + "create_waste_pickup_request, or any other tool in response to the citizen's very first message "
                + "in a conversation unless that exact message already clearly and specifically names one of these "
                + "actions with real detail (e.g. \"I want to report a broken streetlight on MG Road\" is enough; "
                + "\"I want to file a complaint\" alone is not — that only tells you WHICH action they want, not "
                + "what the complaint itself is)."
                + " Once they've clearly chosen to file a complaint OR request a waste pickup, gather it one "
                + "question at a time — ask only ONE thing per reply and wait for their answer before asking the "
                + "next; never guess, invent, or leave a field blank just to finish faster."
                + " For a complaint, ask in this order: (a) what the problem is, in their own words — this becomes "
                + "the subject and description; (b) where it's happening — a locality, ward, or landmark; (c) which "
                + "category best fits, reading a few short options aloud (garbage collection, water supply, road "
                + "damage, street light, drain blockage, or other) unless already obvious from what they said. Do "
                + "not call create_complaint until you have real, specific answers for all of subject, description, "
                + "location, and category."
                + " For a waste pickup request, ask in this order: (a) what waste issue it is, in their own words "
                + "— this becomes the description; (b) where the pickup should happen — a locality, ward, or "
                + "landmark; (c) which category best fits, reading a few short options aloud (household waste not "
                + "collected, bulk waste, construction or demolition debris, a dead animal, drain silt, market "
                + "waste, festival waste, or other); (d) roughly how much waste — small, medium, large, or whether "
                + "a truck is needed; (e) how urgent it is — normal, urgent, or a public health risk. Do not call "
                + "create_waste_pickup_request until you have real answers for all of description, location, "
                + "category, quantity, and urgency."
                + " Never ask for GPS coordinates or a precise map location for either — their device's location "
                + "is captured automatically. Never invent a complaint number, tracking number, status, service "
                + "detail, or tax amount — only state what a tool actually returned.");

        if (identity.isKnown()) {
            prompt.append(" IDENTITY: this citizen is already logged in and verified")
                    .append(identity.name != null ? " (name: " + identity.name + ")" : "")
                    .append(". Their name and mobile number are already on file — do NOT ask for either, and do "
                            + "NOT read the mobile number back unless they ask. Prefer list_my_complaints over "
                            + "asking for a complaint number when they say things like \"my complaint\" or \"my "
                            + "last complaint\". check_property_tax_due takes no arguments — call it directly once "
                            + "they've asked about their property tax dues, don't ask them for a holding number "
                            + "first.");
        } else {
            prompt.append(" IDENTITY: this citizen is not logged in. If they want to file a complaint, ask for "
                    + "their registered mobile number first — create_complaint will tell you if it's not yet "
                    + "OTP-verified, in which case tell them to verify on the portal first. A waste pickup request "
                    + "doesn't need login or OTP verification, but does need their name and a contact mobile "
                    + "number — ask for both as part of gathering the request. Status lookups by complaint number "
                    + "and service questions don't require login. Checking property tax dues does require login "
                    + "— if they ask about that, tell them to log in on the portal first.");
        }
        return prompt.toString();
    }

    private String executeTool(ToolCall call, Identity identity, String[] actionTaken, String[] complaintNumberHolder, String[] trackingNumberHolder) {
        try {
            switch (call.getName()) {
                case "create_complaint":
                    return createComplaint(call.getInput(), identity, actionTaken, complaintNumberHolder);
                case "track_complaint":
                    return trackComplaint(call.getInput(), actionTaken, complaintNumberHolder);
                case "list_my_complaints":
                    return listMyComplaints(identity);
                case "check_property_tax_due":
                    return checkPropertyTaxDue(identity);
                case "create_waste_pickup_request":
                    return createWastePickupRequest(call.getInput(), identity, actionTaken, trackingNumberHolder);
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
        String resolvedMobileNumber;

        if (identity.isKnown()) {
            // Logged-in path: identity already verified by the JWT filter, no extra lookup needed.
            resolvedCitizenId = identity.citizenId;
            resolvedMobileNumber = identity.mobileNumber;
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
            resolvedMobileNumber = citizenOpt.get().getMobileNumber();
        }

        String subject = str(input.get("subject"));
        String description = str(input.get("description"));
        String categoryRaw = str(input.get("category"));
        String location = str(input.get("location"));

        // Blank (not just null/missing) check matters here: an eager tool call can
        // pass subject="" / description="" instead of actually asking the citizen,
        // which would otherwise sail past a null-only check and save a blank
        // complaint. This is the only guard against that — ComplaintService.
        // createComplaint() does no field validation of its own. location and
        // category get the same treatment so the assistant can't skip straight
        // to filing without having actually asked about them.
        if (subject == null || subject.isBlank() || description == null || description.isBlank()) {
            return toJson(Map.of("error",
                    "subject and description are still missing or empty. Ask the citizen to describe the issue "
                    + "in their own words, then call create_complaint again with their actual answer — "
                    + "never call it with placeholder or guessed text."));
        }
        if (location == null || location.isBlank()) {
            return toJson(Map.of("error",
                    "location is still missing. Ask the citizen where the issue is happening (locality, ward, or "
                    + "landmark) before calling create_complaint again."));
        }
        if (categoryRaw == null || categoryRaw.isBlank()) {
            return toJson(Map.of("error",
                    "category is still missing. Ask the citizen which category best fits — garbage collection, "
                    + "water supply, road damage, street light, drain blockage, or other — before calling "
                    + "create_complaint again."));
        }

        Complaint complaint = new Complaint();
        complaint.setCitizenId(resolvedCitizenId);
        complaint.setMobileNumber(resolvedMobileNumber);
        complaint.setSubject(subject);
        complaint.setDescription(description);
        complaint.setCategory(parseCategory(categoryRaw));
        complaint.setLocation(location);
        // GPS comes from Identity (the widget's own navigator.geolocation call, threaded
        // through the trusted request path), never from the LLM's tool-call input.
        complaint.setLatitude(identity.latitude);
        complaint.setLongitude(identity.longitude);

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

    /** Only reachable when identity.isKnown() — see toolDefinitions(). Reuses the same service the /api/property-tax/account REST endpoint calls. */
    @SuppressWarnings("unchecked")
    private String checkPropertyTaxDue(Identity identity) {
        if (!identity.isKnown()) {
            return toJson(Map.of("error", "Must be logged in to check property tax dues."));
        }

        Map<String, Object> account = propertyTaxService.getCitizenAccount(identity.citizenId);
        Object linkedPropertiesRaw = account.get("linkedProperties");
        List<PropertyTaxAccount> linkedProperties = linkedPropertiesRaw instanceof List
                ? (List<PropertyTaxAccount>) linkedPropertiesRaw
                : List.of();

        if (linkedProperties.isEmpty()) {
            return toJson(Map.of(
                    "linkedPropertyCount", 0,
                    "message", "No property is linked to this citizen's account yet. They need to link their "
                            + "holding number on the portal's property tax page before dues can be checked."));
        }

        List<Map<String, Object>> summarized = new ArrayList<>();
        for (PropertyTaxAccount property : linkedProperties) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("holdingNumber", nullToEmpty(property.getHoldingNumber()));
            row.put("amountDue", property.getAmountDue());
            row.put("status", nullToEmpty(property.getStatus()));
            row.put("financialYear", nullToEmpty(property.getFinancialYear()));
            summarized.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDue", account.get("totalDue"));
        result.put("linkedPropertyCount", linkedProperties.size());
        result.put("properties", summarized);
        return toJson(result);
    }

    /**
     * Available to everyone — /api/waste-pickup/request is fully anonymous (no
     * citizen login or OTP verification required, unlike create_complaint). When
     * the citizen IS logged in, their name/mobile come from Identity and are
     * never asked for or exposed in the tool schema — see createWastePickupTool().
     */
    private String createWastePickupRequest(Map<String, Object> input, Identity identity, String[] actionTaken, String[] trackingNumberHolder) {
        String citizenName;
        String citizenMobile;

        if (identity.isKnown()) {
            citizenName = identity.name;
            citizenMobile = identity.mobileNumber;
        } else {
            citizenName = str(input.get("citizenName"));
            citizenMobile = str(input.get("mobileNumber"));
            if (citizenName == null || citizenName.isBlank()) {
                return toJson(Map.of("error",
                        "citizenName is still missing. Ask the citizen their name before calling "
                        + "create_waste_pickup_request again."));
            }
            if (citizenMobile == null || citizenMobile.isBlank()) {
                return toJson(Map.of("error",
                        "mobileNumber is still missing. Ask the citizen for a contact mobile number before "
                        + "calling create_waste_pickup_request again."));
            }
        }

        String description = str(input.get("description"));
        String location = str(input.get("location"));
        String categoryRaw = str(input.get("category"));
        String quantityRaw = str(input.get("quantity"));
        String urgencyRaw = str(input.get("urgency"));

        // Same blank-check philosophy as create_complaint: an eager tool call could
        // pass empty strings instead of actually asking, which a null-only check
        // wouldn't catch. WastePickupService.createRequest() does no validation of
        // its own beyond what the (bypassed, multipart) REST controller enforces.
        if (description == null || description.isBlank()) {
            return toJson(Map.of("error",
                    "description is still missing or empty. Ask the citizen what the waste issue actually is, "
                    + "then call create_waste_pickup_request again with their actual answer."));
        }
        if (location == null || location.isBlank()) {
            return toJson(Map.of("error",
                    "location is still missing. Ask the citizen where the pickup should happen (locality, ward, "
                    + "or landmark) before calling create_waste_pickup_request again."));
        }
        if (categoryRaw == null || categoryRaw.isBlank()) {
            return toJson(Map.of("error",
                    "category is still missing. Ask the citizen which category best fits — household waste not "
                    + "collected, bulk waste, construction or demolition debris, dead animal, drain silt, market "
                    + "waste, festival waste, or other — before calling create_waste_pickup_request again."));
        }
        if (quantityRaw == null || quantityRaw.isBlank()) {
            return toJson(Map.of("error",
                    "quantity is still missing. Ask the citizen roughly how much waste — small, medium, large, "
                    + "or whether a truck is needed — before calling create_waste_pickup_request again."));
        }
        if (urgencyRaw == null || urgencyRaw.isBlank()) {
            return toJson(Map.of("error",
                    "urgency is still missing. Ask the citizen how urgent it is — normal, urgent, or a public "
                    + "health risk — before calling create_waste_pickup_request again."));
        }

        WastePickupRequest request = new WastePickupRequest();
        request.setCitizenName(citizenName);
        request.setCitizenMobile(citizenMobile);
        request.setDescription(description);
        request.setLocality(location);
        request.setFullAddress(location);
        request.setWasteCategory(parseWasteCategory(categoryRaw));
        request.setEstimatedQuantity(parseWasteQuantity(quantityRaw));
        request.setUrgency(parseWasteUrgency(urgencyRaw));
        // GPS comes from Identity (the widget's own navigator.geolocation call, threaded
        // through the trusted request path), never from the LLM's tool-call input.
        request.setLatitude(identity.latitude);
        request.setLongitude(identity.longitude);

        WastePickupRequest saved = wastePickupService.createRequest(request, null);

        actionTaken[0] = "WASTE_PICKUP_REQUESTED";
        trackingNumberHolder[0] = saved.getTrackingId();

        return toJson(Map.of(
                "trackingId", saved.getTrackingId(),
                "status", saved.getStatus().name(),
                "slaHours", saved.getSlaHours(),
                "message", "Waste pickup request submitted successfully."));
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
        tools.add(createWastePickupTool(identity));

        if (identity.isKnown()) {
            tools.add(tool("list_my_complaints",
                    "List the logged-in citizen's own recent complaints (no arguments needed) — use this instead "
                    + "of asking for a complaint number when they refer to \"my complaint\" or \"my last complaint\".",
                    Map.of(), List.of()));
            tools.add(tool("check_property_tax_due",
                    "Check the logged-in citizen's own property tax dues for any property linked to their account "
                    + "(no arguments needed).",
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
                    "File a new citizen complaint for the already-logged-in citizen. Only call this after the "
                    + "citizen has told you what the issue actually is — never call it with empty or made-up "
                    + "subject/description just because they said they want to file a complaint.",
                    Map.of(
                            "subject", schemaString("Short one-line summary of the issue"),
                            "description", schemaString("Fuller description of the issue as the citizen described it"),
                            "category", schemaString("Best-matching category, e.g. GARBAGE_NOT_COLLECTED, WATER_SUPPLY, ROAD_DAMAGE, STREET_LIGHT, DRAIN_BLOCKAGE, OTHER"),
                            "location", schemaString("Locality, ward, or landmark the citizen mentioned")),
                    List.of("subject", "description", "location", "category"));
        }
        return tool("create_complaint",
                "File a new citizen complaint. Requires the citizen's mobile number to already be "
                + "OTP-verified on the portal; if not verified, tell the citizen to verify first. Only call this "
                + "after the citizen has told you what the issue actually is — never call it with empty or "
                + "made-up subject/description just because they said they want to file a complaint.",
                Map.of(
                        "mobileNumber", schemaString("10-digit mobile number, digits only"),
                        "subject", schemaString("Short one-line summary of the issue"),
                        "description", schemaString("Fuller description of the issue as the citizen described it"),
                        "category", schemaString("Best-matching category, e.g. GARBAGE_NOT_COLLECTED, WATER_SUPPLY, ROAD_DAMAGE, STREET_LIGHT, DRAIN_BLOCKAGE, OTHER"),
                        "location", schemaString("Locality, ward, or landmark the citizen mentioned")),
                List.of("mobileNumber", "subject", "description", "location", "category"));
    }

    private Map<String, Object> createWastePickupTool(Identity identity) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("description", schemaString("What waste issue it is, in the citizen's own words"));
        properties.put("location", schemaString("Locality, ward, or landmark where the pickup should happen"));
        properties.put("category", schemaString("Best-matching category, e.g. HOUSEHOLD_WASTE_NOT_COLLECTED, "
                + "BULK_WASTE, CONSTRUCTION_DEMOLITION_WASTE, DEAD_ANIMAL, DRAIN_SILT_GARBAGE, MARKET_WASTE, "
                + "FESTIVAL_EVENT_WASTE, OTHER"));
        properties.put("quantity", schemaString("Estimated amount, e.g. SMALL, MEDIUM, LARGE, TRUCK_REQUIRED"));
        properties.put("urgency", schemaString("How urgent, e.g. NORMAL, URGENT, PUBLIC_HEALTH_RISK"));

        if (identity.isKnown()) {
            // No citizenName/mobileNumber properties at all — the LLM has nothing to ask for.
            return tool("create_waste_pickup_request",
                    "Request a waste/garbage pickup for the already-logged-in citizen. Only call this after the "
                    + "citizen has described the waste issue, its location, category, quantity, and urgency — "
                    + "never call it with empty or made-up values.",
                    properties,
                    List.of("description", "location", "category", "quantity", "urgency"));
        }

        Map<String, Object> anonymousProperties = new LinkedHashMap<>(properties);
        anonymousProperties.put("citizenName", schemaString("The citizen's name"));
        anonymousProperties.put("mobileNumber", schemaString("A contact mobile number for this pickup"));
        return tool("create_waste_pickup_request",
                "Request a waste/garbage pickup. Does not require login or OTP verification, but does need the "
                + "citizen's name and a contact mobile number. Only call this after the citizen has described the "
                + "waste issue, its location, category, quantity, and urgency — never call it with empty or "
                + "made-up values.",
                anonymousProperties,
                List.of("citizenName", "mobileNumber", "description", "location", "category", "quantity", "urgency"));
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

    private WasteCategory parseWasteCategory(String raw) {
        if (raw == null) {
            return WasteCategory.OTHER;
        }
        try {
            return WasteCategory.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return WasteCategory.OTHER;
        }
    }

    private WasteQuantityEstimate parseWasteQuantity(String raw) {
        if (raw == null) {
            return WasteQuantityEstimate.SMALL;
        }
        try {
            return WasteQuantityEstimate.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return WasteQuantityEstimate.SMALL;
        }
    }

    private WasteUrgency parseWasteUrgency(String raw) {
        if (raw == null) {
            return WasteUrgency.NORMAL;
        }
        try {
            return WasteUrgency.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return WasteUrgency.NORMAL;
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
