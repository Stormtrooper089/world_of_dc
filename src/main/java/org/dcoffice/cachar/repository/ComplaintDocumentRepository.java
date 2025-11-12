package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.ComplaintDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintDocumentRepository extends MongoRepository<ComplaintDocument, String> {
    List<ComplaintDocument> findByComplaintId(String complaintId);
    List<ComplaintDocument> findByComplaintNumber(String complaintNumber);
}