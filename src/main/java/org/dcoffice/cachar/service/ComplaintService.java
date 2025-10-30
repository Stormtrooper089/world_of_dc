// ===============================================
// ComplaintService.java
package org.dcoffice.cachar.service;

import org.dcoffice.cachar.dto.ComplaintUpdateRequest;
import org.dcoffice.cachar.dto.ComplaintDepartmentAssignmentRequest;
import org.dcoffice.cachar.dto.ComplaintProgressUpdateRequest;
import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.exception.ComplaintNotFoundException;
import org.dcoffice.cachar.repository.ComplaintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ComplaintService {

    private static final Logger logger = LoggerFactory.getLogger(ComplaintService.class);

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ComplaintHistoryService complaintHistoryService;

    @Autowired
    private OfficerService officerService;

    @Autowired
    private CounterService counterService;

    public Complaint createComplaint(Complaint complaint, List<MultipartFile> files) {
        // Generate complaint number
        complaint.setComplaintNumber(generateComplaintNumber());
        complaint.setComplaintId(counterService.getNextSequence("complaintId"));
        // Save complaint first
        Complaint savedComplaint = complaintRepository.save(complaint);
        logger.info("Created complaint: {} for citizen: {}", savedComplaint.getComplaintNumber(),
                savedComplaint.getCitizenId());

        // Handle file uploads
        if (files != null && !files.isEmpty()) {
            try {
                fileStorageService.storeFiles(savedComplaint, files);
                logger.info("Stored {} files for complaint: {}", files.size(), savedComplaint.getComplaintNumber());
            } catch (Exception e) {
                logger.error("Failed to store files for complaint: {}", savedComplaint.getComplaintNumber(), e);
                // Don't fail the entire operation, just log the error
            }
        }

        // Create history entry
        complaintHistoryService.createHistoryEntry(
                savedComplaint,
                null, // actor is null because it's submitted by citizen
                null, // no previous status
                ComplaintStatus.CREATED,
                "Complaint submitted by citizen"
        );

        return savedComplaint;
    }

    public Complaint assignComplaint(Long complaintId, String officerId, String remarks, Officer assignedBy) {
        Complaint complaint = getComplaintById(complaintId);
        Officer officer = officerService.getOfficerById(officerId);

        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setAssignedToId(officer.getId());
        complaint.setAssignedById(assignedBy.getId());
        complaint.setAssignmentRemarks(remarks);
        complaint.setAssignedAt(LocalDateTime.now());
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        Complaint saved = complaintRepository.save(complaint);

        // Create history entry
        complaintHistoryService.createHistoryEntry(saved, assignedBy,
                previousStatus, ComplaintStatus.ASSIGNED,
                String.format("Complaint assigned to %s (%s): %s",
                        officer.getName(), officer.getDesignation(), remarks));

        logger.info("Assigned complaint {} to officer {} by {}",
                complaint.getComplaintNumber(), officer.getName(), assignedBy.getName());

        return saved;
    }

    public Complaint updateComplaintStatus(Long complaintId, ComplaintStatus newStatus,
                                           String remarks, Officer officer) {
        Complaint complaint = getComplaintById(complaintId);
        ComplaintStatus previousStatus = complaint.getStatus();

        complaint.setStatus(newStatus);
        Complaint saved = complaintRepository.save(complaint);

        // Create history entry
        complaintHistoryService.createHistoryEntry(saved, officer,
                previousStatus, newStatus, remarks);

        logger.info("Updated complaint {} status from {} to {} by officer {}",
                complaint.getComplaintNumber(), previousStatus, newStatus, officer.getName());

        return saved;
    }

    public List<Complaint> getComplaintsByCitizen(String citizenMobile) {
        return complaintRepository.findByCitizenId(citizenMobile);
    }

    public List<Complaint> getComplaintsByOfficer(String officerId) {
        return complaintRepository.findByAssignedToId(officerId);
    }

    public List<Complaint> getComplaintsCreatedByOfficer(String officerId) {
        return complaintRepository.findByCreatedById(officerId);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public List<Complaint> getUnassignedComplaints() {
        return complaintRepository.findUnassignedComplaints();
    }

    public List<Complaint> getComplaintsByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatus(status);
    }

    public List<Complaint> getComplaintsByCategory(ComplaintCategory category) {
        return complaintRepository.findByCategory(category);
    }

    public List<Complaint> getRecentComplaints(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return complaintRepository.findRecentComplaints(fromDate);
    }

    public Optional<Complaint> findByComplaintId(Long complaintId) {
        return complaintRepository.findByComplaintId(complaintId);
    }

    public Optional<Complaint> findByComplaintNumber(String complaintNumber) {
        return complaintRepository.findByComplaintNumber(complaintNumber);
    }

    public Complaint getComplaintById(Long id) {
        return complaintRepository.findByComplaintId(id)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found with ID: " + id));
    }

    public Complaint getComplaintByNumber(String complaintNumber) {
        return complaintRepository.findByComplaintNumber(complaintNumber)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found with number: " + complaintNumber));
    }

    public Long getTotalComplaints() {
        return complaintRepository.count();
    }

    public Long getActiveComplaintsByOfficer(String officerId) {
        return complaintRepository.countActiveComplaintsByOfficer(officerId);
    }

    public Long getComplaintsInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return complaintRepository.countComplaintsByDateRange(startDate, endDate);
    }

    /**
     * Update complaint details - only DC or complaint creator can update
     */
    public Complaint updateComplaint(ComplaintUpdateRequest request, String currentOfficerId, String currentRole) {
        Complaint complaint = getComplaintById(request.getComplaintId());
        
        // Check authorization - only DC or complaint creator can update
        if (!"ROLE_DISTRICT_COMMISSIONER".equals(currentRole) && 
            !complaint.getCreatedById().equals(currentOfficerId)) {
            throw new SecurityException("Access denied: Only District Commissioner or complaint creator can update complaints");
        }
        
        // Update fields if provided
        if (request.getSubject() != null && !request.getSubject().trim().isEmpty()) {
            complaint.setSubject(request.getSubject().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            complaint.setDescription(request.getDescription().trim());
        }
        if (request.getLocation() != null) {
            complaint.setLocation(request.getLocation());
        }
        if (request.getPriority() != null) {
            complaint.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            ComplaintStatus previous = complaint.getStatus();
            ComplaintStatus next = request.getStatus();
            if (!ComplaintStatus.isValidTransition(previous, next)) {
                throw new IllegalArgumentException("Invalid status transition: " + previous + " -> " + next);
            }
            complaint.setStatus(next);
        }
        if (request.getAssignedDepartment() != null) {
            complaint.setAssignedDepartment(request.getAssignedDepartment());
        }
        if (request.getDepartmentRemarks() != null) {
            complaint.setDepartmentRemarks(request.getDepartmentRemarks());
        }
        if (request.getAssignmentRemarks() != null) {
            complaint.setAssignmentRemarks(request.getAssignmentRemarks());
        }
        if (request.getProgressNotes() != null) {
            complaint.setProgressNotes(request.getProgressNotes());
        }
        if (request.getProgressPercentage() != null) {
            complaint.setProgressPercentage(request.getProgressPercentage());
        }
        
        // Add to history
        String historyMessage = "Complaint updated";
        if (request.getAssignedDepartment() != null) {
            historyMessage += " and assigned to " + request.getAssignedDepartment().getDisplayName();
        }
        addToHistory(complaint, historyMessage, request.getUpdateRemarks(), currentOfficerId);
        
        return complaintRepository.save(complaint);
    }
    
    /**
     * Assign complaint to department - only DC can assign
     */
    public Complaint assignComplaintToDepartment(ComplaintDepartmentAssignmentRequest request, String currentOfficerId, String currentRole) {
        if (!"ROLE_DISTRICT_COMMISSIONER".equals(currentRole)) {
            throw new SecurityException("Access denied: Only District Commissioner can assign departments");
        }
        
        Complaint complaint = getComplaintById(request.getComplaintId());
        complaint.setAssignedDepartment(request.getDepartment());
        complaint.setDepartmentRemarks(request.getAssignmentRemarks());
        
        // Add to history
        addToHistory(complaint, "Department assigned to " + request.getDepartment().getDisplayName(), 
                    request.getAssignmentRemarks(), currentOfficerId);
        
        return complaintRepository.save(complaint);
    }
    
    /**
     * Update complaint progress - only DC or complaint creator can update
     */
    public Complaint updateComplaintProgress(ComplaintProgressUpdateRequest request, String currentOfficerId, String currentRole) {
        Complaint complaint = getComplaintById(request.getComplaintId());
        
        // Check authorization - only DC or complaint creator can update
        if (!"ROLE_DISTRICT_COMMISSIONER".equals(currentRole) && 
            !complaint.getCreatedById().equals(currentOfficerId)) {
            throw new SecurityException("Access denied: Only District Commissioner or complaint creator can update progress");
        }
        
        complaint.setProgressPercentage(request.getProgressPercentage());
        complaint.setProgressNotes(request.getProgressNotes());
        
        // Add to history
        addToHistory(complaint, "Progress updated to " + request.getProgressPercentage() + "%", 
                    request.getUpdateRemarks(), currentOfficerId);
        
        return complaintRepository.save(complaint);
    }
    
    /**
     * Add entry to complaint history
     */
    private void addToHistory(Complaint complaint, String action, String remarks, String updatedBy) {
        if (complaint.getHistory() == null) {
            complaint.setHistory(new java.util.ArrayList<>());
        }
        
        ComplaintHistory historyEntry = new ComplaintHistory();
        historyEntry.setComplaintNumber(complaint.getComplaintNumber());
        // Capture previous status if possible (last history entry), else null
        ComplaintStatus previous = null;
        java.util.List<ComplaintHistory> h = complaint.getHistory();
        if (!h.isEmpty()) {
            previous = h.get(h.size() - 1).getNewStatus();
        } else {
            previous = null;
        }
        historyEntry.setPreviousStatus(previous);
        historyEntry.setNewStatus(complaint.getStatus());
        historyEntry.setRemarks(action + ": " + remarks);
        historyEntry.setOfficerId(updatedBy);
        historyEntry.setTimestamp(LocalDateTime.now());
        
        complaint.getHistory().add(historyEntry);
    }

    private String generateComplaintNumber() {
        String prefix = "CCR"; // Cachar Complaint Registration
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int)(Math.random() * 10000));
        return prefix + timestamp + randomSuffix;
    }
}