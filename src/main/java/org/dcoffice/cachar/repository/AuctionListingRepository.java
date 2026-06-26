package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.AuctionListing;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AuctionListingRepository extends MongoRepository<AuctionListing, String> {
    Optional<AuctionListing> findByAuctionId(String auctionId);
    List<AuctionListing> findByStatusIn(List<String> statuses);
    List<AuctionListing> findByStatus(String status);
    long countByStatus(String status);
}
