package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.CommentAttachment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentAttachmentRepository extends MongoRepository<CommentAttachment, String> {

    List<CommentAttachment> findByCommentId(String commentId);

    void deleteByCommentId(String commentId);
}
