package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.repository.ComplaintHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        history.setComplaintId(complaint.getComplaintId()); // use complaintId instead of Mongo _id
        history.setComplaintNumber(complaint.getComplaintNumber());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setRemarks(remarks);
        history.setTimestamp(LocalDateTime.now());

        if (officer != null) {
            history.setOfficerId(officer.getId());
            history.setActorName(officer.getName());
        } else {
            history.setOfficerId(null);
            history.setActorName("Citizen");
        }

        ComplaintHistory saved = complaintHistoryRepository.save(history);
        logger.debug("Created history entry for complaint: {} with status: {} by {}",
                complaint.getComplaintNumber(), newStatus, history.getActorName());

        return saved;
    }



    public List<ComplaintHistory> getComplaintHistory(String complaintNumber) {
        return complaintHistoryRepository.findByComplaintIdOrderByTimestampDesc(complaintNumber);
    }

    public List<ComplaintHistory> getOfficerHistory(String officerId) {
        return complaintHistoryRepository.findByOfficerIdOrderByTimestampDesc(officerId);
    }

}