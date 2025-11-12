package org.dcoffice.cachar.config;

import org.dcoffice.cachar.entity.Comment;
import org.dcoffice.cachar.entity.Officer;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.service.OfficerService;
import org.dcoffice.cachar.service.CitizenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-time migration to populate commenterName field for existing comments.
 * Safe to run multiple times (idempotent updates).
 */
@Component
@Order(6)
@Profile({"default", "dev", "prod"})
public class CommentMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CommentMigrationRunner.class);

    private final MongoTemplate mongoTemplate;
    private final OfficerService officerService;
    private final CitizenService citizenService;

    public CommentMigrationRunner(MongoTemplate mongoTemplate, OfficerService officerService, CitizenService citizenService) {
        this.mongoTemplate = mongoTemplate;
        this.officerService = officerService;
        this.citizenService = citizenService;
    }

    @Override
    public void run(String... args) {
        try {
            // Find comments that don't have commenterName set
            Query query = new Query(Criteria.where("commenterName").exists(false));
            List<Comment> commentsWithoutName = mongoTemplate.find(query, Comment.class);

            if (commentsWithoutName.isEmpty()) {
                logger.info("No comments found without commenterName. Migration complete.");
                return;
            }

            int updatedCount = 0;
            for (Comment comment : commentsWithoutName) {
                try {
                    String commenterName = null;

                    // Try to get name based on role
                    if ("OFFICER".equals(comment.getCommenterRole()) ||
                        "DISTRICT_COMMISSIONER".equals(comment.getCommenterRole())) {
                        // For officers, get name from officer service
                        try {
                            Officer officer = officerService.getOfficerById(comment.getCommenterId());
                            if (officer != null) {
                                commenterName = officer.getName();
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to get officer name for commenter {}: {}", comment.getCommenterId(), e.getMessage());
                        }
                    } else if ("CITIZEN".equals(comment.getCommenterRole())) {
                        // For citizens, get name from citizen service
                        try {
                            Citizen citizen = citizenService.getCitizenByMobileNumber(comment.getCommenterId());
                            if (citizen != null) {
                                commenterName = citizen.getName();
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to get citizen name for commenter {}: {}", comment.getCommenterId(), e.getMessage());
                        }
                    }

                    // Update the comment if we found a name
                    if (commenterName != null && !commenterName.trim().isEmpty()) {
                        Query updateQuery = new Query(Criteria.where("_id").is(comment.getId()));
                        Update update = new Update().set("commenterName", commenterName);
                        mongoTemplate.updateFirst(updateQuery, update, Comment.class);
                        updatedCount++;
                    }

                } catch (Exception e) {
                    logger.warn("Failed to migrate comment {}: {}", comment.getId(), e.getMessage());
                }
            }

            if (updatedCount > 0) {
                logger.info("Comment migration completed. Updated {} comments with commenterName.", updatedCount);
            }

        } catch (Exception e) {
            logger.warn("Comment migration encountered an error: {}", e.getMessage());
        }
    }
}
