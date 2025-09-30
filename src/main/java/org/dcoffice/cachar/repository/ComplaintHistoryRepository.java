package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.ComplaintHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintHistoryRepository extends MongoRepository<ComplaintHistory, Long> {
    List<ComplaintHistory> findByComplaintIdOrderByTimestampDesc(Long complaintId);
    List<ComplaintHistory> findByOfficerIdOrderByTimestampDesc(Long officerId);
}
