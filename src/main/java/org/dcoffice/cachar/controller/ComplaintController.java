package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.ComplaintUpdateRequest;
import org.dcoffice.cachar.dto.ComplaintDepartmentAssignmentRequest;
import org.dcoffice.cachar.dto.ComplaintProgressUpdateRequest;
import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.service.CitizenService;
import org.dcoffice.cachar.service.ComplaintHistoryService;
import org.dcoffice.cachar.service.ComplaintService;
import org.dcoffice.cachar.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import javax.validation.Valid;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            @RequestParam(value = "priority", defaultValue = "MEDIUM") String priorityStr,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "department", required = false) String departmentStr,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {

        try {
            // Check if citizen exists, if not create one for testing
            Citizen citizen;
            if (!citizenService.isCitizenVerified(mobileNumber)) {
                // Create a citizen for testing purposes
                Citizen newCitizen = new Citizen();
                newCitizen.setMobileNumber(mobileNumber);
                newCitizen.setName("Test Citizen");
                newCitizen.setEmail("test@example.com");
                newCitizen.setVerified(true); // Mark as verified for testing
                citizen = citizenService.registerOrUpdateCitizen(newCitizen);
            } else {
                citizen = citizenService.getCitizenByMobileNumber(mobileNumber);
            }

            // Create complaint object
            Complaint complaint = new Complaint();
            complaint.setCitizenId(citizen.getMobileNumber());
            complaint.setSubject(subject);
            complaint.setDescription(description);
            
            // Convert priority string to enum
            Priority priority = Priority.MEDIUM; // Default
            if (priorityStr != null && !priorityStr.trim().isEmpty()) {
                try {
                    priority = Priority.valueOf(priorityStr);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid priority value: {}, using default MEDIUM", priorityStr);
                }
            }
            complaint.setPriority(priority);
            complaint.setLocation(location);
            
            // Convert department string to enum
            if (departmentStr != null && !departmentStr.trim().isEmpty()) {
                try {
                    Department department = Department.valueOf(departmentStr);
                    complaint.setAssignedDepartment(department);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid department value: {}, ignoring", departmentStr);
                    // Continue without department assignment
                }
            }
            
            // Set the officer who created the complaint
            if (authentication != null) {
                complaint.setCreatedById(authentication.getName());
            }

            Complaint savedComplaint = complaintService.createComplaint(complaint, files);

            Map<String, Object> response = new HashMap<>();
            response.put("complaintNumber", savedComplaint.getComplaintNumber());
            response.put("complaintId", savedComplaint.getId());
            response.put("status", savedComplaint.getStatus());

            return ResponseEntity.ok(ApiResponse.success("Complaint created successfully", response));

        } catch (Exception e) {
            logger.error("Failed to create complaint for {}: {}", mobileNumber, e.getMessage(), e);
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

            List<Complaint> complaints = complaintService.getComplaintsByCitizen(citizenOpt.get().getMobileNumber());
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

                List<ComplaintHistory> history = complaintHistoryService.getComplaintHistory(complaintNumber);
                List<ComplaintDocument> documents = fileStorageService.getComplaintDocumentsByNumber(complaint.getComplaintNumber());

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

    @GetMapping
    public ResponseEntity<ApiResponse<List<Complaint>>> getComplaints(
            @RequestParam(value = "officerId", required = false) String officerId,
            @RequestParam(value = "citizenId", required = false) String citizenId,
            @RequestParam(value = "status", required = false) ComplaintStatus status,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            Authentication authentication) {
        try {
            String currentOfficerId = authentication.getName();
            String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
            
            List<Complaint> complaints;
            
            // Authorization logic based on role
            if ("ROLE_DISTRICT_COMMISSIONER".equals(currentRole)) {
                // DC can see all complaints with any filter
                if (createdBy != null) {
                    complaints = complaintService.getComplaintsCreatedByOfficer(createdBy);
                } else if (officerId != null) {
                    complaints = complaintService.getComplaintsByOfficer(officerId);
                } else if (citizenId != null) {
                    complaints = complaintService.getComplaintsByCitizen(citizenId);
                } else if (status != null) {
                    complaints = complaintService.getComplaintsByStatus(status);
                } else {
                    complaints = complaintService.getAllComplaints();
                }
            } else {
                // Regular officers can only see complaints they created
                if (createdBy != null && createdBy.equals(currentOfficerId)) {
                    complaints = complaintService.getComplaintsCreatedByOfficer(createdBy);
                } else if (createdBy == null) {
                    // If no createdBy specified, default to current officer's created complaints
                    complaints = complaintService.getComplaintsCreatedByOfficer(currentOfficerId);
                } else {
                    // Officer trying to access other officer's complaints - not allowed
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Access denied: You can only view complaints you created"));
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success("Complaints retrieved successfully", complaints));
        } catch (Exception e) {
            logger.error("Failed to fetch complaints: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch complaints: " + e.getMessage()));
        }
    }

    /**
     * Update complaint details - only DC or complaint creator can update
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<Complaint>> updateComplaint(
            @Valid @RequestBody ComplaintUpdateRequest request,
            Authentication authentication) {
        try {
            String currentOfficerId = authentication.getName();
            String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
            
            Complaint updatedComplaint = complaintService.updateComplaint(request, currentOfficerId, currentRole);
            
            return ResponseEntity.ok(ApiResponse.success("Complaint updated successfully", updatedComplaint));
        } catch (SecurityException e) {
            logger.warn("Unauthorized complaint update attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update complaint: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update complaint: " + e.getMessage()));
        }
    }
    
    /**
     * Assign complaint to department - only DC can assign
     */
    @PutMapping("/assign-department")
    public ResponseEntity<ApiResponse<Complaint>> assignComplaintToDepartment(
            @Valid @RequestBody ComplaintDepartmentAssignmentRequest request,
            Authentication authentication) {
        try {
            String currentOfficerId = authentication.getName();
            String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
            
            Complaint updatedComplaint = complaintService.assignComplaintToDepartment(request, currentOfficerId, currentRole);
            
            return ResponseEntity.ok(ApiResponse.success("Complaint assigned to department successfully", updatedComplaint));
        } catch (SecurityException e) {
            logger.warn("Unauthorized department assignment attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to assign complaint to department: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to assign department: " + e.getMessage()));
        }
    }
    
    /**
     * Update complaint progress - only DC or complaint creator can update
     */
    @PutMapping("/update-progress")
    public ResponseEntity<ApiResponse<Complaint>> updateComplaintProgress(
            @Valid @RequestBody ComplaintProgressUpdateRequest request,
            Authentication authentication) {
        try {
            String currentOfficerId = authentication.getName();
            String currentRole = authentication.getAuthorities().iterator().next().getAuthority();
            
            Complaint updatedComplaint = complaintService.updateComplaintProgress(request, currentOfficerId, currentRole);
            
            return ResponseEntity.ok(ApiResponse.success("Complaint progress updated successfully", updatedComplaint));
        } catch (SecurityException e) {
            logger.warn("Unauthorized progress update attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update complaint progress: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update progress: " + e.getMessage()));
        }
    }

    private String generateComplaintNumber() {
        String prefix = "CCR"; // Cachar Complaint Registration
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int)(Math.random() * 10000));
        return prefix + timestamp + randomSuffix;
    }
}