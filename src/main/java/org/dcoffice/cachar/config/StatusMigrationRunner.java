package org.dcoffice.cachar.config;

import org.dcoffice.cachar.entity.Complaint;
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

/**
 * One-time migration to normalize legacy complaint statuses to the new simplified set.
 * Safe to run multiple times (idempotent updates).
 */
@Component
@Order(5)
@Profile({"default", "dev", "prod"})
public class StatusMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StatusMigrationRunner.class);

    private final MongoTemplate mongoTemplate;

    public StatusMigrationRunner(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            int totalUpdated = 0;
            totalUpdated += migrate("SUBMITTED", "CREATED");
            totalUpdated += migrate("UNDER_REVIEW", "CREATED");
            totalUpdated += migrate("REASSIGNED", "ASSIGNED");
            totalUpdated += migrate("WORKING", "IN_PROGRESS");
            totalUpdated += migrate("PENDING_INFORMATION", "BLOCKED");
            totalUpdated += migrate("PENDING_APPROVAL", "BLOCKED");
            totalUpdated += migrate("ON_HOLD", "BLOCKED");
            totalUpdated += migrate("PARTIALLY_RESOLVED", "RESOLVED");

            if (totalUpdated > 0) {
                logger.info("Status migration completed. Updated {} complaints.", totalUpdated);
            }
        } catch (Exception e) {
            logger.warn("Status migration encountered an error: {}", e.getMessage());
        }
    }

    private int migrate(String from, String to) {
        Query query = new Query(Criteria.where("status").is(from));
        Update update = new Update().set("status", to);
        var result = mongoTemplate.updateMulti(query, update, Complaint.class);
        int modified = result != null ? (int) result.getModifiedCount() : 0;
        if (modified > 0) {
            logger.info("Migrated {} complaints from {} to {}", modified, from, to);
        }
        return modified;
    }
}


