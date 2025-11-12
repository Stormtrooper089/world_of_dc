package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.MeetingCreateRequest;
import org.dcoffice.cachar.entity.Meeting;
import org.dcoffice.cachar.service.MeetingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "*")
public class MeetingController {

    private static final Logger logger = LoggerFactory.getLogger(MeetingController.class);

    @Autowired
    private MeetingService meetingService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Meeting>> createMeeting(
            @Valid @RequestBody MeetingCreateRequest request,
            Authentication authentication) {
        try {
            String officerId = authentication.getName(); // Get officer ID from JWT token
            Meeting meeting = meetingService.createMeeting(request, officerId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Meeting created successfully", meeting));
        } catch (Exception e) {
            logger.error("Error creating meeting", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "Failed to create meeting: " + e.getMessage(), null));
        }
    }

    @GetMapping("/my-meetings")
    public ResponseEntity<ApiResponse<List<Meeting>>> getMyMeetings(Authentication authentication) {
        try {
            String officerId = authentication.getName();
            List<Meeting> meetings = meetingService.getMeetingsByOfficerId(officerId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Meetings retrieved successfully", meetings));
        } catch (Exception e) {
            logger.error("Error retrieving meetings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve meetings: " + e.getMessage(), null));
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Meeting>>> getUpcomingMeetings(Authentication authentication) {
        try {
            String officerId = authentication.getName();
            List<Meeting> meetings = meetingService.getUpcomingMeetingsByOfficerId(officerId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Upcoming meetings retrieved successfully", meetings));
        } catch (Exception e) {
            logger.error("Error retrieving upcoming meetings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve upcoming meetings: " + e.getMessage(), null));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Meeting>>> getAllMeetings(Authentication authentication) {
        try {
            List<Meeting> meetings = meetingService.getAllMeetings();
            return ResponseEntity.ok(new ApiResponse<>(true, "All meetings retrieved successfully", meetings));
        } catch (Exception e) {
            logger.error("Error retrieving all meetings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve meetings: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Meeting>> getMeetingById(@PathVariable String id) {
        try {
            Meeting meeting = meetingService.getMeetingById(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Meeting retrieved successfully", meeting));
        } catch (Exception e) {
            logger.error("Error retrieving meeting", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Meeting not found: " + e.getMessage(), null));
        }
    }
}

