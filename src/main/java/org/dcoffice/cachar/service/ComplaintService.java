// ===============================================
// ComplaintService.java
package org.dcoffice.cachar.service;

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

    public Complaint createComplaint(Complaint complaint, List<MultipartFile> files) {
        // Generate complaint number
        complaint.setComplaintNumber(generateComplaintNumber());

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
        complaintHistoryService.createHistoryEntry(savedComplaint, null,
                null, ComplaintStatus.SUBMITTED, "Complaint submitted by citizen");

        return savedComplaint;
    }

    public Complaint assignComplaint(Long complaintId, Long officerId, String remarks, Officer assignedBy) {
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

    public List<Complaint> getComplaintsByCitizen(Long citizenId) {
        return complaintRepository.findByCitizenId(String.valueOf(citizenId));
    }

    public List<Complaint> getComplaintsByOfficer(Long officerId) {
        return complaintRepository.findByAssignedToId(String.valueOf(officerId));
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

    public Optional<Complaint> findByComplaintNumber(String complaintNumber) {
        return complaintRepository.findByComplaintNumber(complaintNumber);
    }

    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(String.valueOf(id))
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found with ID: " + id));
    }

    public Complaint getComplaintByNumber(String complaintNumber) {
        return complaintRepository.findByComplaintNumber(complaintNumber)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found with number: " + complaintNumber));
    }

    public Long getTotalComplaints() {
        return complaintRepository.count();
    }

    public Long getActiveComplaintsByOfficer(Long officerId) {
        return complaintRepository.countActiveComplaintsByOfficer(String.valueOf(officerId));
    }

    public Long getComplaintsInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return complaintRepository.countComplaintsByDateRange(startDate, endDate);
    }

    private String generateComplaintNumber() {
        String prefix = "CCR"; // Cachar Complaint Registration
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int)(Math.random() * 10000));
        return prefix + timestamp + randomSuffix;
    }
}