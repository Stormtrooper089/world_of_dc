package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.service.WastePickupService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class WastePickupController {
    private final WastePickupService wastePickupService;

    public WastePickupController(WastePickupService wastePickupService) {
        this.wastePickupService = wastePickupService;
    }

    @PostMapping(value = "/api/waste-pickup/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WastePickupRequest>> createRequest(
            @RequestParam String citizenName,
            @RequestParam String mobileNumber,
            @RequestParam(required = false) Integer wardNumber,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) String landmark,
            @RequestParam(required = false) String fullAddress,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam WasteCategory wasteCategory,
            @RequestParam WasteQuantityEstimate estimatedQuantity,
            @RequestParam WasteUrgency urgency,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String preferredPickupSlot,
            @RequestParam(required = false) Boolean vehicleAccessAvailable,
            @RequestParam(required = false) String sensitiveNearbyLocation,
            @RequestParam(value = "beforePhotos", required = false) List<MultipartFile> beforePhotos
    ) {
        WastePickupRequest request = new WastePickupRequest();
        request.setCitizenName(citizenName);
        request.setCitizenMobile(mobileNumber);
        request.setWardNumber(wardNumber);
        request.setLocality(locality);
        request.setLandmark(landmark);
        request.setFullAddress(fullAddress);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setWasteCategory(wasteCategory);
        request.setEstimatedQuantity(estimatedQuantity);
        request.setUrgency(urgency);
        request.setDescription(description);
        request.setPreferredPickupSlot(preferredPickupSlot);
        request.setVehicleAccessAvailable(vehicleAccessAvailable);
        request.setSensitiveNearbyLocation(sensitiveNearbyLocation);
        return ResponseEntity.ok(ApiResponse.success("Waste pickup request submitted", wastePickupService.createRequest(request, beforePhotos)));
    }

    @GetMapping("/api/waste-pickup/track/{trackingId}")
    public ResponseEntity<ApiResponse<WastePickupRequest>> track(@PathVariable String trackingId) {
        return ResponseEntity.ok(ApiResponse.success("Waste pickup request retrieved", wastePickupService.track(trackingId)));
    }

    @GetMapping("/api/waste-pickup/track")
    public ResponseEntity<ApiResponse<List<WastePickupRequest>>> trackByMobile(@RequestParam String mobileNumber) {
        return ResponseEntity.ok(ApiResponse.success("Waste pickup requests retrieved", wastePickupService.trackByMobile(mobileNumber)));
    }

    @PostMapping("/api/waste-pickup/{trackingId}/feedback")
    public ResponseEntity<ApiResponse<WastePickupRequest>> feedback(
            @PathVariable String trackingId,
            @RequestBody Map<String, Object> body
    ) {
        Integer rating = body.get("rating") == null ? null : Integer.valueOf(body.get("rating").toString());
        String feedback = body.get("feedback") == null ? null : body.get("feedback").toString();
        return ResponseEntity.ok(ApiResponse.success("Feedback saved", wastePickupService.addFeedback(trackingId, rating, feedback)));
    }

    @PostMapping("/api/waste-pickup/{trackingId}/reopen")
    public ResponseEntity<ApiResponse<WastePickupRequest>> reopen(
            @PathVariable String trackingId,
            @RequestBody Map<String, Object> body
    ) {
        String reason = body.get("reason") == null ? null : body.get("reason").toString();
        return ResponseEntity.ok(ApiResponse.success("Waste pickup request reopened", wastePickupService.reopen(trackingId, reason)));
    }

    @GetMapping("/api/officer/waste-pickup")
    public ResponseEntity<ApiResponse<List<WastePickupRequest>>> list(
            @RequestParam(required = false) Integer wardNumber,
            @RequestParam(required = false) WastePickupStatus status,
            @RequestParam(required = false) WasteUrgency urgency,
            @RequestParam(required = false) WasteCategory category,
            @RequestParam(required = false) Boolean slaBreached,
            @RequestParam(required = false) String officerId,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        return ResponseEntity.ok(ApiResponse.success("Waste pickup requests retrieved", wastePickupService.search(wardNumber, status, urgency, category, slaBreached, officerId, locality, fromDate, toDate)));
    }

    @GetMapping("/api/officer/waste-pickup/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> officerDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Waste pickup dashboard retrieved", wastePickupService.dashboard(null, null, null, null, null, null, null, null, null)));
    }

    @GetMapping("/api/officer/waste-pickup/hotspots")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> hotspots() {
        return ResponseEntity.ok(ApiResponse.success("Waste hotspots retrieved", wastePickupService.hotspots()));
    }

    @GetMapping("/api/officer/waste-pickup/route-suggestions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> routeSuggestions(@RequestParam(required = false) Integer wardNumber) {
        return ResponseEntity.ok(ApiResponse.success("Route suggestions retrieved", wastePickupService.routeSuggestions(wardNumber)));
    }

    @GetMapping("/api/officer/waste-pickup/{requestId}")
    public ResponseEntity<ApiResponse<WastePickupRequest>> detail(@PathVariable String requestId) {
        return ResponseEntity.ok(ApiResponse.success("Waste pickup request retrieved", wastePickupService.getById(requestId)));
    }

    @PutMapping("/api/officer/waste-pickup/{requestId}/verify")
    public ResponseEntity<ApiResponse<WastePickupRequest>> verify(@PathVariable String requestId, @RequestBody Map<String, Object> body, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Request verified", wastePickupService.verify(requestId, string(body, "remarks"), actorId(authentication), actorRole(authentication), doubleValue(body, "latitude"), doubleValue(body, "longitude"))));
    }

    @PutMapping("/api/officer/waste-pickup/{requestId}/assign")
    public ResponseEntity<ApiResponse<WastePickupRequest>> assign(@PathVariable String requestId, @RequestBody Map<String, Object> body, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Request assigned", wastePickupService.assign(requestId, string(body, "assignedOfficerId"), string(body, "assignedSanitationStaffId"), string(body, "assignedVehicleNumber"), string(body, "sanitationTeamName"), string(body, "remarks"), actorId(authentication), actorRole(authentication))));
    }

    @PutMapping("/api/officer/waste-pickup/{requestId}/schedule")
    public ResponseEntity<ApiResponse<WastePickupRequest>> schedule(@PathVariable String requestId, @RequestBody Map<String, Object> body, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Pickup scheduled", wastePickupService.schedule(requestId, string(body, "preferredPickupSlot"), string(body, "remarks"), actorId(authentication), actorRole(authentication))));
    }

    @PutMapping("/api/officer/waste-pickup/{requestId}/status")
    public ResponseEntity<ApiResponse<WastePickupRequest>> updateStatus(@PathVariable String requestId, @RequestBody Map<String, Object> body, Authentication authentication) {
        WastePickupStatus status = WastePickupStatus.valueOf(string(body, "status"));
        return ResponseEntity.ok(ApiResponse.success("Status updated", wastePickupService.updateStatus(requestId, status, string(body, "remarks"), actorId(authentication), actorRole(authentication), doubleValue(body, "latitude"), doubleValue(body, "longitude"))));
    }

    @PutMapping(value = "/api/officer/waste-pickup/{requestId}/close", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WastePickupRequest>> close(
            @PathVariable String requestId,
            @RequestParam String closureRemarks,
            @RequestParam Double closureLatitude,
            @RequestParam Double closureLongitude,
            @RequestParam(required = false) WasteQuantityEstimate actualWasteQuantity,
            @RequestParam(required = false) Boolean siteFullyCleaned,
            @RequestParam(required = false) String assignedVehicleNumber,
            @RequestParam(required = false) String sanitationTeamName,
            @RequestParam(value = "afterPhotos", required = false) List<MultipartFile> afterPhotos,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Waste pickup request closed", wastePickupService.close(requestId, closureRemarks, closureLatitude, closureLongitude, actualWasteQuantity, siteFullyCleaned, assignedVehicleNumber, sanitationTeamName, afterPhotos, actorId(authentication), actorRole(authentication))));
    }

    @GetMapping("/api/dashboard/waste-pickup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @RequestParam(required = false) Integer wardNumber,
            @RequestParam(required = false) WastePickupStatus status,
            @RequestParam(required = false) WasteUrgency urgency,
            @RequestParam(required = false) WasteCategory category,
            @RequestParam(required = false) Boolean slaBreached,
            @RequestParam(required = false) String officerId,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        return ResponseEntity.ok(ApiResponse.success("Waste pickup dashboard retrieved", wastePickupService.dashboard(wardNumber, status, urgency, category, slaBreached, officerId, locality, fromDate, toDate)));
    }

    private String actorId(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private String actorRole(Authentication authentication) {
        return authentication == null || authentication.getAuthorities().isEmpty()
                ? "UNKNOWN"
                : authentication.getAuthorities().iterator().next().getAuthority();
    }

    private String string(Map<String, Object> body, String key) {
        return body.get(key) == null ? null : body.get(key).toString();
    }

    private Double doubleValue(Map<String, Object> body, String key) {
        return body.get(key) == null ? null : Double.valueOf(body.get(key).toString());
    }
}
