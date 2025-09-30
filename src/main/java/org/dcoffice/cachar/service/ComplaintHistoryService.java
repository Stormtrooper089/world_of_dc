package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.repository.ComplaintHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(ComplaintHistoryService.class);

    @Autowired
    private ComplaintHistoryRepository complaintHistoryRepository;

    public ComplaintHistory createHistoryEntry(Complaint complaint, Officer officer,
                                               ComplaintStatus previousStatus, ComplaintStatus newStatus,
                                               String remarks) {
        ComplaintHistory history = new ComplaintHistory();
        history.setId(complaint.getId());
        history.setOfficerId(officer.getId());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setRemarks(remarks);

        ComplaintHistory saved = complaintHistoryRepository.save(history);
        logger.debug("Created history entry for complaint: {} with status: {}",
                complaint.getComplaintNumber(), newStatus);

        return saved;
    }

    public List<ComplaintHistory> getComplaintHistory(Long complaintId) {
        return complaintHistoryRepository.findByComplaintIdOrderByTimestampDesc(complaintId);
    }

    public List<ComplaintHistory> getOfficerHistory(Long officerId) {
        return complaintHistoryRepository.findByOfficerIdOrderByTimestampDesc(officerId);
    }
}