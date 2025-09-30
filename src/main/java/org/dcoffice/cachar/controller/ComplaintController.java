package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.service.CitizenService;
import org.dcoffice.cachar.service.ComplaintHistoryService;
import org.dcoffice.cachar.service.ComplaintService;
import org.dcoffice.cachar.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    private static final Logger logger = LoggerFactory.getLogger(ComplaintController.class);

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private CitizenService citizenService;

    @Autowired
    private ComplaintHistoryService complaintHistoryService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createComplaint(
            @RequestParam("mobileNumber") String mobileNumber,
            @RequestParam("subject") String subject,
            @RequestParam("description") String description,
            @RequestParam("category") ComplaintCategory category,
            @RequestParam(value = "priority", defaultValue = "MEDIUM") Priority priority,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        try {
            // Verify citizen exists and is verified
            if (!citizenService.isCitizenVerified(mobileNumber)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Citizen not found or not verified"));
            }

            Citizen citizen = citizenService.getCitizenByMobileNumber(mobileNumber);

            // Create complaint object
            Complaint complaint = new Complaint();
            complaint.setCitizenId(citizen.getId());
            complaint.setSubject(subject);
            complaint.setDescription(description);
            complaint.setCategory(category);
            complaint.setPriority(priority);
            complaint.setLocation(location);

            Complaint savedComplaint = complaintService.createComplaint(complaint, files);

            Map<String, Object> response = new HashMap<>();
            response.put("complaintNumber", savedComplaint.getComplaintNumber());
            response.put("complaintId", savedComplaint.getId());
            response.put("status", savedComplaint.getStatus());

            return ResponseEntity.ok(ApiResponse.success("Complaint created successfully", response));

        } catch (Exception e) {
            logger.error("Failed to create complaint for {}: {}", mobileNumber, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create complaint: " + e.getMessage()));
        }
    }

    @GetMapping("/citizen/{mobileNumber}")
    public ResponseEntity<ApiResponse<List<Complaint>>> getComplaintsByCitizen(@PathVariable String mobileNumber) {
        try {
            Optional<Citizen> citizenOpt = citizenService.findByMobileNumber(mobileNumber);
            if (!citizenOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            List<Complaint> complaints = complaintService.getComplaintsByCitizen(Long.valueOf(citizenOpt.get().getId()));
            return ResponseEntity.ok(ApiResponse.success("Complaints retrieved successfully", complaints));

        } catch (Exception e) {
            logger.error("Failed to fetch complaints for citizen {}: {}", mobileNumber, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch complaints: " + e.getMessage()));
        }
    }

    @GetMapping("/track/{complaintNumber}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trackComplaint(@PathVariable String complaintNumber) {
        try {
            Optional<Complaint> complaintOpt = complaintService.findByComplaintNumber(complaintNumber);

            if (complaintOpt.isPresent()) {
                Complaint complaint = complaintOpt.get();
                List<ComplaintHistory> history = complaintHistoryService.getComplaintHistory(Long.valueOf(complaint.getId()));
                List<ComplaintDocument> documents = fileStorageService.getComplaintDocuments(Long.valueOf(complaint.getId()));

                Map<String, Object> response = new HashMap<>();
                response.put("complaint", complaint);
                response.put("history", history);
                response.put("documents", documents);

                return ResponseEntity.ok(ApiResponse.success("Complaint details retrieved", response));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Failed to track complaint {}: {}", complaintNumber, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to track complaint: " + e.getMessage()));
        }
    }

    @GetMapping("/unassigned")
    public ResponseEntity<ApiResponse<List<Complaint>>> getUnassignedComplaints() {
        try {
            List<Complaint> complaints = complaintService.getUnassignedComplaints();
            return ResponseEntity.ok(ApiResponse.success("Unassigned complaints retrieved", complaints));
        } catch (Exception e) {
            logger.error("Failed to fetch unassigned complaints: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch unassigned complaints: " + e.getMessage()));
        }
    }

    @GetMapping("/recent/{days}")
    public ResponseEntity<ApiResponse<List<Complaint>>> getRecentComplaints(@PathVariable int days) {
        try {
            List<Complaint> complaints = complaintService.getRecentComplaints(days);
            return ResponseEntity.ok(ApiResponse.success("Recent complaints retrieved", complaints));
        } catch (Exception e) {
            logger.error("Failed to fetch recent complaints: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch recent complaints: " + e.getMessage()));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<Complaint>>> getComplaintsByCategory(@PathVariable ComplaintCategory category) {
        try {
            List<Complaint> complaints = complaintService.getComplaintsByCategory(category);
            return ResponseEntity.ok(ApiResponse.success("Complaints by category retrieved", complaints));
        } catch (Exception e) {
            logger.error("Failed to fetch complaints by category {}: {}", category, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch complaints by category: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Complaint>>> getComplaintsByStatus(@PathVariable ComplaintStatus status) {
        try {
            List<Complaint> complaints = complaintService.getComplaintsByStatus(status);
            return ResponseEntity.ok(ApiResponse.success("Complaints by status retrieved", complaints));
        } catch (Exception e) {
            logger.error("Failed to fetch complaints by status {}: {}", status, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch complaints by status: " + e.getMessage()));
        }
    }
}