package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.ComplaintUpdateRequest;
import org.dcoffice.cachar.dto.ComplaintDepartmentAssignmentRequest;
import org.dcoffice.cachar.dto.CommentCreateRequest;
import org.dcoffice.cachar.dto.CommentUpdateRequest;
import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.service.CitizenService;
import org.dcoffice.cachar.service.ComplaintHistoryService;
import org.dcoffice.cachar.service.ComplaintService;
import org.dcoffice.cachar.service.FileStorageService;
import org.dcoffice.cachar.service.OfficerService;
import org.dcoffice.cachar.repository.CommentRepository;
import org.dcoffice.cachar.repository.CommentAttachmentRepository;
import org.dcoffice.cachar.repository.ComplaintDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;

import javax.validation.Valid;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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

    @Autowired
    private OfficerService officerService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentAttachmentRepository commentAttachmentRepository;

    @Autowired
    private ComplaintDocumentRepository complaintDocumentRepository;

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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Complaint>> getComplaintById(@PathVariable String id) {
        try {
            // Try to parse as numeric ID first, then fall back to MongoDB ObjectId
            Complaint complaint;
            try {
                Long complaintId = Long.parseLong(id);
                complaint = complaintService.getComplaintWithComments(complaintId);
            } catch (NumberFormatException e) {
                // If it's not a number, treat it as MongoDB ObjectId
                complaint = complaintService.getComplaintByIdString(id);
                if (complaint != null) {
                    // Load comments for this complaint and populate attachments and commenterName
                    List<Comment> comments = commentRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId());
                    for (Comment comment : comments) {
                        List<CommentAttachment> attachments = commentAttachmentRepository.findByCommentId(comment.getId());
                        comment.setAttachments(attachments);

                        // Populate commenterName if not set
                        if (comment.getCommenterName() == null || comment.getCommenterName().trim().isEmpty()) {
                            try {
                                if ("OFFICER".equals(comment.getCommenterRole()) ||
                                    "DISTRICT_COMMISSIONER".equals(comment.getCommenterRole())) {
                                    Officer officer = officerService.getOfficerById(comment.getCommenterId());
                                    if (officer != null) {
                                        comment.setCommenterName(officer.getName());
                                        commentRepository.save(comment);
                                    }
                                } else if ("CITIZEN".equals(comment.getCommenterRole())) {
                                    Citizen citizen = citizenService.getCitizenByMobileNumber(comment.getCommenterId());
                                    if (citizen != null) {
                                        comment.setCommenterName(citizen.getName());
                                        commentRepository.save(comment);
                                    }
                                }
                            } catch (Exception commentException) {
                                logger.warn("Failed to populate commenterName for comment {}: {}", comment.getId(), commentException.getMessage());
                            }
                        }
                    }
                    complaint.setComments(comments);

                    // Load documents for this complaint
                    List<ComplaintDocument> documents = complaintDocumentRepository.findByComplaintId(complaint.getId());
                    complaint.setDocuments(documents);
                }
            }

            if (complaint == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(ApiResponse.success("Complaint retrieved successfully", complaint));
        } catch (Exception e) {
            logger.error("Failed to get complaint {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get complaint: " + e.getMessage()));
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
                // Regular officers can see complaints they created OR are assigned to them
                if (createdBy != null && createdBy.equals(currentOfficerId)) {
                    complaints = complaintService.getComplaintsCreatedByOfficer(createdBy);
                } else if (createdBy == null) {
                    // If no createdBy specified, show complaints created by OR assigned to current officer
                    List<Complaint> createdComplaints = complaintService.getComplaintsCreatedByOfficer(currentOfficerId);
                    List<Complaint> assignedComplaints = complaintService.getComplaintsByOfficer(currentOfficerId);

                    // Combine and deduplicate complaints
                    Set<String> complaintIds = new HashSet<>();
                    complaints = new ArrayList<>();

                    // Add created complaints first
                    for (Complaint c : createdComplaints) {
                        if (complaintIds.add(c.getId())) {
                            complaints.add(c);
                        }
                    }

                    // Add assigned complaints (avoiding duplicates)
                    for (Complaint c : assignedComplaints) {
                        if (complaintIds.add(c.getId())) {
                            complaints.add(c);
                        }
                    }
                } else {
                    // Officer trying to access other officer's complaints - not allowed
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Access denied: You can only view complaints you created or are assigned to"));
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

    // Comment endpoints
    @PostMapping(value = "/{complaintId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Comment>> addComment(
            @PathVariable String complaintId,
            @RequestParam("text") String text,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {
        try {
            String currentUserId = authentication.getName();
            String currentUserRole = authentication.getAuthorities().iterator().next().getAuthority();

            // Validate text is not empty
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Comment text is required"));
            }

            Comment comment = new Comment();
            comment.setComplaintId(complaintId);
            comment.setCommenterId(currentUserId);
            comment.setText(text.trim());

            // Set commenter name and role based on user type
            if ("ROLE_OFFICER".equals(currentUserRole) || "ROLE_DISTRICT_COMMISSIONER".equals(currentUserRole)) {
                // For officers, get their name from officer service
                comment.setCommenterRole(currentUserRole.replace("ROLE_", ""));
                try {
                    Officer officer = officerService.getOfficerById(currentUserId);
                    if (officer != null) {
                        comment.setCommenterName(officer.getName());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to get officer name for commenter {}: {}", currentUserId, e.getMessage());
                }
            } else {
                // For citizens, get their name from citizen service
                comment.setCommenterRole("CITIZEN");
                try {
                    Citizen citizen = citizenService.getCitizenByMobileNumber(currentUserId);
                    if (citizen != null) {
                        comment.setCommenterName(citizen.getName());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to get citizen name for commenter {}: {}", currentUserId, e.getMessage());
                }
            }

            // Handle file attachments if provided
            List<CommentAttachment> attachments = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        try {
                            String filePath = fileStorageService.storeFile(file, "comment-attachments");

                            CommentAttachment attachment = new CommentAttachment();
                            attachment.setCommentId(comment.getId()); // Will be set after comment is saved
                            attachment.setFileName(file.getOriginalFilename());
                            attachment.setFilePath(filePath);
                            attachment.setFileSize(file.getSize());
                            attachment.setMimeType(file.getContentType());

                            // Determine attachment type
                            String mimeType = file.getContentType();
                            if (mimeType != null) {
                                if (mimeType.startsWith("image/")) {
                                    attachment.setAttachmentType("image");
                                } else if (mimeType.startsWith("video/")) {
                                    attachment.setAttachmentType("video");
                                } else {
                                    attachment.setAttachmentType("document");
                                }
                            }

                            attachments.add(attachment);
                        } catch (Exception e) {
                            logger.error("Failed to upload file {}: {}", file.getOriginalFilename(), e.getMessage());
                            // Continue with other files, don't fail the whole comment
                        }
                    }
                }
            }

            Comment savedComment = commentRepository.save(comment);

            // Save attachments with the comment ID
            if (!attachments.isEmpty()) {
                for (CommentAttachment attachment : attachments) {
                    attachment.setCommentId(savedComment.getId());
                    commentAttachmentRepository.save(attachment);
                }

                // Update comment with attachments
                savedComment.setAttachments(attachments);
            }

            return ResponseEntity.ok(ApiResponse.success("Comment added successfully", savedComment));
        } catch (Exception e) {
            logger.error("Failed to add comment to complaint {}: {}", complaintId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to add comment: " + e.getMessage()));
        }
    }

    @GetMapping("/{complaintId}/comments")
    public ResponseEntity<ApiResponse<List<Comment>>> getComments(@PathVariable String complaintId) {
        try {
            List<Comment> comments = commentRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);

            // Populate attachments and commenterName for each comment
            for (Comment comment : comments) {
                List<CommentAttachment> attachments = commentAttachmentRepository.findByCommentId(comment.getId());
                comment.setAttachments(attachments);

                // Populate commenterName if not set
                if (comment.getCommenterName() == null || comment.getCommenterName().trim().isEmpty()) {
                    try {
                        if ("OFFICER".equals(comment.getCommenterRole()) ||
                            "DISTRICT_COMMISSIONER".equals(comment.getCommenterRole())) {
                            Officer officer = officerService.getOfficerById(comment.getCommenterId());
                            if (officer != null) {
                                comment.setCommenterName(officer.getName());
                                // Optionally save the updated comment
                                commentRepository.save(comment);
                            }
                        } else if ("CITIZEN".equals(comment.getCommenterRole())) {
                            Citizen citizen = citizenService.getCitizenByMobileNumber(comment.getCommenterId());
                            if (citizen != null) {
                                comment.setCommenterName(citizen.getName());
                                // Optionally save the updated comment
                                commentRepository.save(comment);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to populate commenterName for comment {}: {}", comment.getId(), e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", comments));
        } catch (Exception e) {
            logger.error("Failed to get comments for complaint {}: {}", complaintId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get comments: " + e.getMessage()));
        }
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Comment>> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            Authentication authentication) {
        try {
            String currentUserId = authentication.getName();

            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (!commentOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Comment comment = commentOpt.get();

            // Only comment author can update
            if (!comment.getCommenterId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only update your own comments"));
            }

            comment.setText(request.getText());
            comment.setUpdatedAt(LocalDateTime.now());

            Comment updatedComment = commentRepository.save(comment);

            return ResponseEntity.ok(ApiResponse.success("Comment updated successfully", updatedComment));
        } catch (Exception e) {
            logger.error("Failed to update comment {}: {}", commentId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update comment: " + e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String commentId,
            Authentication authentication) {
        try {
            String currentUserId = authentication.getName();

            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (!commentOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Comment comment = commentOpt.get();

            // Only comment author can delete
            if (!comment.getCommenterId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only delete your own comments"));
            }

            // Delete comment attachments first
            commentAttachmentRepository.deleteByCommentId(commentId);

            // Delete comment
            commentRepository.deleteById(commentId);

            return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
        } catch (Exception e) {
            logger.error("Failed to delete comment {}: {}", commentId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete comment: " + e.getMessage()));
        }
    }

    private String generateComplaintNumber() {
        String prefix = "CCR"; // Cachar Complaint Registration
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int)(Math.random() * 10000));
        return prefix + timestamp + randomSuffix;
    }
}