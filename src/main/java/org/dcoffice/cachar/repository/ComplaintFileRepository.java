package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.ComplaintFile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ComplaintFileRepository extends MongoRepository<ComplaintFile, String> {
    List<ComplaintFile> findByComplaintId(String complaintId);
}