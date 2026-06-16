package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.WastePickupRequest;
import org.dcoffice.cachar.entity.WastePickupStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WastePickupRequestRepository extends MongoRepository<WastePickupRequest, String> {
    Optional<WastePickupRequest> findByTrackingId(String trackingId);
    List<WastePickupRequest> findByCitizenMobileOrderBySubmittedAtDesc(String citizenMobile);
    List<WastePickupRequest> findByStatusNotInAndSubmittedAtAfter(List<WastePickupStatus> statuses, LocalDateTime submittedAfter);
}
