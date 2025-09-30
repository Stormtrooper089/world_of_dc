package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.AssignComplaintRequest;
import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.service.ComplaintService;
import org.dcoffice.cachar.service.OfficerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/officer")
@CrossOrigin(origins = "*")
public class OfficerController {

    private static final Logger logger = LoggerFactory.getLogger(OfficerController.class);

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private OfficerService officerService;

    @PostMapping("/assign-complaint")
    @PreAuthorize("hasRole('DISTRICT_COMMISSIONER') or hasRole('ADDITIONAL_DISTRICT_COMMISSIONER')")
    public ResponseEntity<ApiResponse<Complaint>> assignComplaint(@Valid @RequestBody AssignComplaintRequest request) {
        try {
            // In production, get current officer from security context
            // For demo, we'll use a dummy district commissioner
            Officer assignedBy = new Officer();
            assignedBy.setId(String.valueOf(1L));
            assignedBy.setName("District Commissioner");
            assignedBy.setRole(OfficerRole.DISTRICT_COMMISSIONER);

            Complaint updatedComplaint = complaintService.assignComplaint(
                    request.getComplaintId(),
                    request.getOfficerId(),
                    request.getRemarks(),
                    assignedBy
            );

            return ResponseEntity.ok(ApiResponse.success("Complaint assigned successfully", updatedComplaint));

        } catch (Exception e) {
            logger.error("Failed to assign complaint {}: {}", request.getComplaintId(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to assign complaint: " + e.getMessage()));
        }
    }

    @GetMapping("/my-complaints/{officerId}")
    public ResponseEntity<ApiResponse<List<Complaint>>> getMyComplaints(@PathVariable Long officerId) {
        try {
            List<Complaint> complaints = complaintService.getComplaintsByOfficer(officerId);
            return ResponseEntity.ok(ApiResponse.success("Officer complaints retrieved", complaints));
        } catch (Exception e) {
            logger.error("Failed to fetch complaints for officer {}: {}", officerId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch complaints: " + e.getMessage()));
        }
    }

    @PutMapping("/update-status")
    public ResponseEntity<ApiResponse<Complaint>> updateComplaintStatus(@RequestBody Map<String, Object> request) {
        try {
            Long complaintId = Long.valueOf(request.get("complaintId").toString());
            ComplaintStatus newStatus = ComplaintStatus.valueOf(request.get("status").toString());
            String remarks = request.get("remarks").toString();
            Long officerId = Long.valueOf(request.get("officerId").toString());

            Officer currentOfficer = officerService.getOfficerById(officerId);

            Complaint updatedComplaint = complaintService.updateComplaintStatus(
                    complaintId, newStatus, remarks, currentOfficer);

            return ResponseEntity.ok(ApiResponse.success("Complaint status updated successfully", updatedComplaint));

        } catch (Exception e) {
            logger.error("Failed to update complaint status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard-stats/{officerId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(@PathVariable Long officerId) {
        try {
            List<Complaint> allComplaints = complaintService.getComplaintsByOfficer(officerId);

            long totalAssigned = allComplaints.size();
            long active = allComplaints.stream()
                    .mapToLong(c -> (c.getStatus() == ComplaintStatus.ASSIGNED ||
                            c.getStatus() == ComplaintStatus.IN_PROGRESS) ? 1 : 0)
                    .sum();
            long resolved = allComplaints.stream()
                    .mapToLong(c -> c.getStatus() == ComplaintStatus.RESOLVED ? 1 : 0)
                    .sum();
            long closed = allComplaints.stream()
                    .mapToLong(c -> c.getStatus() == ComplaintStatus.CLOSED ? 1 : 0)
                    .sum();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAssigned", totalAssigned);
            stats.put("active", active);
            stats.put("resolved", resolved);
            stats.put("closed", closed);
            stats.put("pendingInformation", allComplaints.stream()
                    .mapToLong(c -> c.getStatus() == ComplaintStatus.PENDING_INFORMATION ? 1 : 0)
                    .sum());

            return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", stats));
        } catch (Exception e) {
            logger.error("Failed to fetch dashboard stats for officer {}: {}", officerId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch stats: " + e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Officer>>> getAllActiveOfficers() {
        try {
            List<Officer> officers = officerService.findActiveOfficers();
            return ResponseEntity.ok(ApiResponse.success("Active officers retrieved", officers));
        } catch (Exception e) {
            logger.error("Failed to fetch active officers: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch officers: " + e.getMessage()));
        }
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<Officer>>> getOfficersByRole(@PathVariable OfficerRole role) {
        try {
            List<Officer> officers = officerService.findOfficersByRole(role);
            return ResponseEntity.ok(ApiResponse.success("Officers by role retrieved", officers));
        } catch (Exception e) {
            logger.error("Failed to fetch officers by role {}: {}", role, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch officers by role: " + e.getMessage()));
        }
    }
}