package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.AuctionAuditTrail;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuctionAuditTrailRepository extends MongoRepository<AuctionAuditTrail, String> {
    List<AuctionAuditTrail> findByAuctionIdOrderByTimestampDesc(String auctionId);
}
