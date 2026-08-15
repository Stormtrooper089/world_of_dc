package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.VoiceChatRequest;
import org.dcoffice.cachar.dto.VoiceChatResponse;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.service.voiceassistant.VoiceAssistantService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Endpoint for the citizen voice assistant widget. Reachable whether or not
 * the citizen is logged in (like /api/complaints/track/**), but when they ARE
 * logged in, Spring Security hands us an already-verified Authentication —
 * JwtAuthenticationFilter runs on this path (it's not in that filter's
 * shouldNotFilter list) and, for a CITIZEN-role token, stashes the loaded
 * Citizen entity in Authentication.getDetails(). We use that to tell
 * VoiceAssistantService who's asking, so the assistant already knows the
 * citizen's mobile number instead of asking for it — see the "IDENTITY"
 * handling in VoiceAssistantService for how that's used.
 *
 * IMPORTANT — before shipping this beyond a demo: add basic per-IP or
 * per-session rate limiting here (e.g. bucket4j) — every turn is a billed
 * LLM call, and the anonymous path has no login to slow down abuse the way
 * a login-gated endpoint would.
 */
@RestController
@RequestMapping("/api/citizen/voice-assistant")
@CrossOrigin(origins = "*")
public class VoiceAssistantController {

    private final VoiceAssistantService voiceAssistantService;

    public VoiceAssistantController(VoiceAssistantService voiceAssistantService) {
        this.voiceAssistantService = voiceAssistantService;
    }

    @PostMapping("/chat")
    public ApiResponse<VoiceChatResponse> chat(@Valid @RequestBody VoiceChatRequest request, Authentication authentication) {
        String citizenId = null;
        String citizenMobileNumber = null;
        String citizenName = null;

        if (authentication != null && authentication.isAuthenticated() && isCitizen(authentication)) {
            citizenId = authentication.getName(); // JWT subject == Citizen Mongo id, see JwtService.generateTokenForCitizen
            Object details = authentication.getDetails();
            if (details instanceof Citizen) {
                Citizen citizen = (Citizen) details;
                citizenMobileNumber = citizen.getMobileNumber();
                citizenName = citizen.getName();
            }
        }

        VoiceAssistantService.VoiceReply reply = voiceAssistantService.handleTurn(
                request.getSessionId(), request.getTranscript(), citizenId, citizenMobileNumber, citizenName);
        VoiceChatResponse response = new VoiceChatResponse(request.getSessionId(), reply.text, reply.actionTaken, reply.complaintNumber);
        return ApiResponse.success("ok", response);
    }

    private boolean isCitizen(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_CITIZEN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
