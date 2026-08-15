package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.VoiceChatRequest;
import org.dcoffice.cachar.dto.VoiceChatResponse;
import org.dcoffice.cachar.service.voiceassistant.VoiceAssistantService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Public, anonymous-friendly endpoint for the citizen voice assistant widget.
 *
 * Intentionally public (like /api/complaints/track/** and /api/citizen/register)
 * because a citizen speaking to this widget hasn't logged in — but the
 * create_complaint tool inside VoiceAssistantService still requires the
 * citizen's mobile number to already be OTP-verified before it will file
 * anything, so this doesn't reopen the kind of unauthenticated-write gap
 * flagged elsewhere in the platform review (e.g. the public tracking API's
 * admin-flag escalation).
 *
 * IMPORTANT — before shipping this beyond a demo: add a matcher for this path
 * to SecurityConfig's PUBLIC ENDPOINTS block (it currently only recognizes
 * routes it already lists, and everything else falls through to
 * .anyRequest().authenticated()):
 *
 *   .antMatchers(HttpMethod.POST, "/api/citizen/voice-assistant/**").permitAll()
 *
 * Also add basic per-IP or per-session rate limiting here (e.g. bucket4j) —
 * every turn is a billed LLM call, and this endpoint has no auth to slow down
 * abuse the way a login-gated endpoint would.
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
    public ApiResponse<VoiceChatResponse> chat(@Valid @RequestBody VoiceChatRequest request) {
        VoiceAssistantService.VoiceReply reply = voiceAssistantService.handleTurn(request.getSessionId(), request.getTranscript());
        VoiceChatResponse response = new VoiceChatResponse(request.getSessionId(), reply.text, reply.actionTaken, reply.complaintNumber);
        return ApiResponse.success("ok", response);
    }
}
