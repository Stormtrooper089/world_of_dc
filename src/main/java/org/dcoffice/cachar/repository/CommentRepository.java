package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByComplaintIdOrderByCreatedAtAsc(String complaintId);

    List<Comment> findByCommenterIdOrderByCreatedAtDesc(String commenterId);

    long countByComplaintId(String complaintId);
}
