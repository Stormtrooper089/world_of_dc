package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.AuctionBid;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AuctionBidRepository extends MongoRepository<AuctionBid, String> {
    Optional<AuctionBid> findByBidId(String bidId);
    List<AuctionBid> findByAuctionIdOrderByBidAmountDesc(String auctionId);
    List<AuctionBid> findByBidderCitizenIdOrderBySubmittedAtDesc(String bidderCitizenId);
    long countByAuctionId(String auctionId);
}
