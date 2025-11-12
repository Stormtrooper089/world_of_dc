package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.AssignComplaintRequest;
import org.dcoffice.cachar.dto.OfficerLoginRequest;
import org.dcoffice.cachar.dto.OfficerApproveRequest;
import org.dcoffice.cachar.dto.OfficerUpdateRequest;
import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.service.ComplaintService;
import org.dcoffice.cachar.service.OfficerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/officer")
@CrossOrigin(origins = "*")
public class OfficerController {

    private static final Logger logger = LoggerFactory.getLogger(OfficerController.class);

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private OfficerService officerService;

    @Autowired
    private org.dcoffice.cachar.service.JwtService jwtService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private org.dcoffice.cachar.repository.OfficerRepository officerRepository;

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
    public ResponseEntity<ApiResponse<List<Complaint>>> getMyComplaints(@PathVariable String officerId) {
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
            String officerId = request.get("officerId").toString();

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(@PathVariable String officerId) {
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
            // Pending information state removed; keep key for API stability with 0 value
            stats.put("pendingInformation", 0L);

            return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", stats));
        } catch (Exception e) {
            logger.error("Failed to fetch dashboard stats for officer {}: {}", officerId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch stats: " + e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Officer>>> getAllActiveOfficers(
            @RequestParam(value = "search", required = false) String searchQuery) {
        try {
            List<Officer> officers;
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                officers = officerService.searchOfficersByName(searchQuery);
            } else {
                officers = officerService.findActiveOfficers();
            }
            return ResponseEntity.ok(ApiResponse.success("Active officers retrieved", officers));
        } catch (Exception e) {
            logger.error("Failed to fetch active officers: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch officers: " + e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signupOfficer(@Valid @RequestBody Officer officer) {
        try {
            Officer saved = officerService.signupOfficer(officer);
            return ResponseEntity.ok(ApiResponse.success("Officer signup initiated. Await admin approval.", saved.getId()));
        } catch (Exception e) {
            logger.error("Officer signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Signup failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> officerLogin(@Valid @RequestBody org.dcoffice.cachar.dto.OfficerLoginRequest request) {
        try {
            Officer officer = officerService.authenticateOfficer(request.getEmployeeId(), request.getPassword());
            String token = jwtService.generateTokenForOfficer(officer);
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("token", token);
            data.put("officerId", officer.getId());
            data.put("name", officer.getName());
            data.put("email", officer.getEmail());
            data.put("employeeId", officer.getEmployeeId());
            data.put("role", officer.getRole() != null ? officer.getRole().name() : "OFFICER");
            return ResponseEntity.ok(ApiResponse.success("Login successful", data));
        } catch (Exception e) {
            logger.warn("Officer login failed for {}: {}", request.getEmployeeId(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/approve/{officerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DISTRICT_COMMISSIONER')")
    public ResponseEntity<ApiResponse<Officer>> approveOfficer(@PathVariable String officerId, @RequestBody OfficerApproveRequest request) {
        try {
            Officer updated = officerService.approveOfficer(officerId, request.getApproverEmployeeId(), request.getRoleAsEnum());
            return ResponseEntity.ok(ApiResponse.success("Officer approved successfully", updated));
        } catch (Exception e) {
            logger.error("Failed to approve officer {}: {}", officerId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Approval failed: " + e.getMessage()));
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

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DISTRICT_COMMISSIONER')")
    public ResponseEntity<ApiResponse<List<Officer>>> getPendingApprovals() {
        try {
            List<Officer> pending = officerService.findPendingApprovals();
            return ResponseEntity.ok(ApiResponse.success("Pending approvals retrieved", pending));
        } catch (Exception e) {
            logger.error("Failed to fetch pending approvals: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch pending approvals: " + e.getMessage()));
        }
    }

    @PostMapping("/reject/{officerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DISTRICT_COMMISSIONER')")
    public ResponseEntity<ApiResponse<Void>> rejectOfficer(@PathVariable String officerId, @RequestBody OfficerApproveRequest request) {
        try {
            officerService.rejectOfficer(officerId, request.getApproverEmployeeId());
            return ResponseEntity.ok(ApiResponse.success("Officer rejected"));
        } catch (Exception e) {
            logger.error("Failed to reject officer {}: {}", officerId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Rejection failed: " + e.getMessage()));
        }
    }

    // Get current officer profile
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Officer>> getCurrentOfficerProfile(Authentication authentication) {
        try {
            String officerId = authentication.getName();
            Officer officer = officerService.getOfficerById(officerId);
            return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", officer));
        } catch (Exception e) {
            logger.error("Failed to get officer profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve profile: " + e.getMessage()));
        }
    }

    // Update officer profile
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Officer>> updateOfficerProfile(@RequestBody OfficerUpdateRequest request, Authentication authentication) {
        try {
            String officerId = authentication.getName();
            Officer existingOfficer = officerService.getOfficerById(officerId);
            
            // Update only the allowed fields
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                existingOfficer.setName(request.getName().trim());
            }
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                existingOfficer.setEmail(request.getEmail().trim());
            }
            if (request.getMobileNumber() != null && !request.getMobileNumber().trim().isEmpty()) {
                existingOfficer.setMobileNumber(request.getMobileNumber().trim());
            }
            if (request.getDesignation() != null && !request.getDesignation().trim().isEmpty()) {
                existingOfficer.setDesignation(request.getDesignation().trim());
            }
            if (request.getDepartment() != null && !request.getDepartment().trim().isEmpty()) {
                existingOfficer.setDepartment(request.getDepartment().trim());
            }
            
            Officer updatedOfficer = officerService.updateOfficer(existingOfficer);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedOfficer));
        } catch (Exception e) {
            logger.error("Failed to update officer profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Profile update failed: " + e.getMessage()));
        }
    }
}