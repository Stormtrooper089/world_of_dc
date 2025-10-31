package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.Complaint;
import org.dcoffice.cachar.entity.ComplaintStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends MongoRepository<Complaint, String> {

    Optional<Complaint> findByComplaintNumber(String complaintNumber);

    Optional<Complaint> findByComplaintId(Long complaintId);

    List<Complaint> findByCitizenId(String citizenId);

    List<Complaint> findByAssignedToId(String officerId);

    List<Complaint> findByCreatedById(String officerId);

    List<Complaint> findByStatus(ComplaintStatus status);

    @Query("{ 'assignedToId': null, 'status': 'CREATED' }")
    List<Complaint> findUnassignedComplaints();

    @Query(value = "{ 'assignedToId': ?0, 'status': { $in: ['ASSIGNED', 'IN_PROGRESS'] } }", count = true)
    Long countActiveComplaintsByOfficer(String officerId);

    @Query(value = "{ 'createdAt': { $gte: ?0, $lte: ?1 } }", count = true)
    Long countComplaintsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    @Query("{ 'createdAt': { $gte: ?0 } }")
    List<Complaint> findRecentComplaints(LocalDateTime date);

    @Query("{ 'status': { $in: ?0 } }")
    List<Complaint> findByStatusIn(List<ComplaintStatus> statuses);

    @Query("{ 'priority': ?0, 'status': { $nin: ['CLOSED', 'RESOLVED'] } }")
    List<Complaint> findOpenComplaintsByPriority(String priority);

    // For pagination and sorting
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<Complaint> findByAssignedToIdOrderByPriorityDescCreatedAtDesc(String officerId);
}
