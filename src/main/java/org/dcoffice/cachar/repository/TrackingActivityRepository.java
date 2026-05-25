package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.TrackingActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TrackingActivityRepository extends MongoRepository<TrackingActivity, String> {

    Page<TrackingActivity> findBySquadId(String squadId, Pageable pageable);

    Page<TrackingActivity> findBySquadIdAndTimestampBetween(String squadId, Instant start, Instant end, Pageable pageable);

    Page<TrackingActivity> findBySquadIdAndMemberId(String squadId, String memberId, Pageable pageable);

    Page<TrackingActivity> findBySquadIdAndMemberIdAndTimestampBetween(String squadId, String memberId, Instant start, Instant end, Pageable pageable);

    // Worker attendance and photo queries
    List<TrackingActivity> findByMemberIdAndTypeInAndTimestampBetweenOrderByTimestampDesc(
            String memberId, List<String> types, Instant start, Instant end);

    List<TrackingActivity> findByMemberIdAndTypeAndTimestampBetweenOrderByTimestampDesc(
            String memberId, String type, Instant start, Instant end);

    List<TrackingActivity> findByMemberIdAndTypeInOrderByTimestampDesc(
            String memberId, List<String> types);

    List<TrackingActivity> findByTypeInAndTimestampBetween(
            List<String> types, Instant start, Instant end);

    List<TrackingActivity> findBySquadIdAndTypeInAndTimestampBetween(
            String squadId, List<String> types, Instant start, Instant end);
}
