package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.CreateTrackingMemberRequest;
import org.dcoffice.cachar.dto.CreateTrackingSquadRequest;
import org.dcoffice.cachar.dto.TrackingDashboardResponseDto;
import org.dcoffice.cachar.dto.UpdateTrackingLocationRequest;
import org.dcoffice.cachar.entity.TrackingActivity;
import org.dcoffice.cachar.entity.TrackingMember;
import org.dcoffice.cachar.entity.TrackingSquad;
import org.dcoffice.cachar.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "*")
public class TrackingController {

    private static final Logger logger = LoggerFactory.getLogger(TrackingController.class);

    @Autowired
    private TrackingService trackingService;

    @PostMapping("/test/seed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedTestData() {
        try {
            Map<String, Object> result = trackingService.seedTestData();
            return ResponseEntity.ok(ApiResponse.success("Test data seeded successfully", result));
        } catch (Exception e) {
            logger.error("Failed to seed test data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to seed test data: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<TrackingDashboardResponseDto>> getDashboard(
            @RequestParam(value = "squadId", required = false) String squadId,
            @RequestParam(value = "activityLimit", required = false) Integer activityLimit,
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo) {
        try {
            TrackingDashboardResponseDto dashboard = trackingService.getDashboard(squadId, activityLimit, dateFrom, dateTo);
            return ResponseEntity.ok(ApiResponse.success("Tracking dashboard fetched successfully", dashboard));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch tracking dashboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch tracking dashboard: " + e.getMessage()));
        }
    }

    @PostMapping("/squads")
    public ResponseEntity<ApiResponse<TrackingSquad>> createSquad(@Valid @RequestBody CreateTrackingSquadRequest request) {
        try {
            TrackingSquad created = trackingService.createSquad(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tracking squad created successfully", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create squad: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create squad: " + e.getMessage()));
        }
    }

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<TrackingMember>> createMember(@Valid @RequestBody CreateTrackingMemberRequest request) {
        try {
            TrackingMember created = trackingService.createMember(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tracking member created successfully", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create member: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create member: " + e.getMessage()));
        }
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<TrackingMember>>> getAllMembers() {
        try {
            List<TrackingMember> members = trackingService.getAllMembers();
            return ResponseEntity.ok(ApiResponse.success("Tracking members fetched successfully", members));
        } catch (Exception e) {
            logger.error("Failed to fetch members: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch members: " + e.getMessage()));
        }
    }

    @PutMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<TrackingMember>> updateMember(
            @PathVariable String memberId,
            @RequestBody CreateTrackingMemberRequest request) {
        try {
            TrackingMember updated = trackingService.updateMember(memberId, request);
            return ResponseEntity.ok(ApiResponse.success("Member updated successfully", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update member {}: {}", memberId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update member: " + e.getMessage()));
        }
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable String memberId) {
        try {
            trackingService.deleteMember(memberId);
            return ResponseEntity.ok(ApiResponse.success("Member deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete member {}: {}", memberId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete member: " + e.getMessage()));
        }
    }

    @PutMapping("/members/{memberId}/location")
    public ResponseEntity<ApiResponse<TrackingMember>> updateMemberLocation(
            @PathVariable String memberId,
            @Valid @RequestBody UpdateTrackingLocationRequest request) {
        try {
            TrackingMember updated = trackingService.updateMemberLocation(memberId, request);
            return ResponseEntity.ok(ApiResponse.success("Member location shared successfully", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update location for member {}: {}", memberId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update member location: " + e.getMessage()));
        }
    }

    @GetMapping("/squads")
    public ResponseEntity<ApiResponse<List<TrackingSquad>>> getSquads() {
        try {
            List<TrackingSquad> squads = trackingService.getAllSquads();
            return ResponseEntity.ok(ApiResponse.success("Tracking squads fetched successfully", squads));
        } catch (Exception e) {
            logger.error("Failed to fetch squads: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch squads: " + e.getMessage()));
        }
    }

    @GetMapping("/squads/{squadId}/members")
    public ResponseEntity<ApiResponse<List<TrackingMember>>> getSquadMembers(@PathVariable String squadId) {
        try {
            List<TrackingMember> members = trackingService.getMembersBySquadId(squadId);
            return ResponseEntity.ok(ApiResponse.success("Tracking squad members fetched successfully", members));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch members for squad {}: {}", squadId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch members: " + e.getMessage()));
        }
    }

    @GetMapping("/squads/{squadId}/activities")
    public ResponseEntity<ApiResponse<List<TrackingActivity>>> getSquadActivity(
            @PathVariable String squadId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            List<TrackingActivity> activities = trackingService.getActivityBySquadId(squadId, limit);
            return ResponseEntity.ok(ApiResponse.success("Tracking squad activity logs fetched successfully", activities));
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().startsWith("limit")
                    ? HttpStatus.BAD_REQUEST : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch activity logs for squad {}: {}", squadId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch squad activity logs: " + e.getMessage()));
        }
    }

    @GetMapping("/squads/{squadId}/members/{memberId}/activities")
    public ResponseEntity<ApiResponse<List<TrackingActivity>>> getSquadMemberActivity(
            @PathVariable String squadId,
            @PathVariable String memberId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            List<TrackingActivity> activities = trackingService.getActivityBySquadAndMember(squadId, memberId, limit);
            return ResponseEntity.ok(ApiResponse.success("Tracking member activity logs fetched successfully", activities));
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().startsWith("limit")
                    ? HttpStatus.BAD_REQUEST : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch activity logs for squad {} member {}: {}", squadId, memberId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch member activity logs: " + e.getMessage()));
        }
    }

    @GetMapping("/activities/{activityId}")
    public ResponseEntity<ApiResponse<TrackingActivity>> getActivity(@PathVariable String activityId) {
        try {
            TrackingActivity activity = trackingService.getActivity(activityId);
            return ResponseEntity.ok(ApiResponse.success("Activity fetched successfully", activity));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch activity {}: {}", activityId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch activity: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/activities/{activityId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TrackingActivity>> uploadActivityAttachments(
            @PathVariable String activityId,
            @RequestPart("files") List<MultipartFile> files) {
        try {
            TrackingActivity activity = trackingService.uploadActivityAttachments(activityId, files);
            return ResponseEntity.ok(ApiResponse.success("Attachments uploaded successfully", activity));
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to upload attachments for activity {}: {}", activityId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload attachments: " + e.getMessage()));
        }
    }

    @DeleteMapping("/activities/{activityId}/attachments")
    public ResponseEntity<ApiResponse<TrackingActivity>> deleteActivityAttachment(
            @PathVariable String activityId,
            @RequestParam("filePath") String filePath) {
        try {
            TrackingActivity activity = trackingService.deleteActivityAttachment(activityId, filePath);
            return ResponseEntity.ok(ApiResponse.success("Attachment removed successfully", activity));
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete attachment for activity {}: {}", activityId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to remove attachment: " + e.getMessage()));
        }
    }
}
