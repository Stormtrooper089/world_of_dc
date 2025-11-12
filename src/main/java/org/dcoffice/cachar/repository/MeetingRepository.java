package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.Meeting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends MongoRepository<Meeting, String> {

    // Find meetings created by a specific officer
    List<Meeting> findByCreatedByIdOrderByStartDateTimeDesc(String createdById);

    // Find meetings where an officer is invited (either as creator or in invited list)
    @Query("{ $or: [ { 'createdById': ?0 }, { 'invitedOfficerIds': ?0 } ] }")
    List<Meeting> findMeetingsByOfficerId(String officerId);

    // Find meetings where an officer is invited (either as creator or in invited list), ordered by start date
    @Query("{ $or: [ { 'createdById': ?0 }, { 'invitedOfficerIds': ?0 } ] }")
    List<Meeting> findMeetingsByOfficerIdOrderByStartDateTimeDesc(String officerId);

    // Find upcoming meetings for an officer
    @Query("{ $or: [ { 'createdById': ?0 }, { 'invitedOfficerIds': ?0 } ], 'startDateTime': { $gte: ?1 } }")
    List<Meeting> findUpcomingMeetingsByOfficerId(String officerId, LocalDateTime now);

    // Find past meetings for an officer
    @Query("{ $or: [ { 'createdById': ?0 }, { 'invitedOfficerIds': ?0 } ], 'startDateTime': { $lt: ?1 } }")
    List<Meeting> findPastMeetingsByOfficerId(String officerId, LocalDateTime now);

    // Find all meetings ordered by start date
    List<Meeting> findAllByOrderByStartDateTimeDesc();
}

