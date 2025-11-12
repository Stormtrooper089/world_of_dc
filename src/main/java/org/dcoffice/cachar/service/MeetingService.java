package org.dcoffice.cachar.service;

import org.dcoffice.cachar.dto.MeetingCreateRequest;
import org.dcoffice.cachar.entity.Meeting;
import org.dcoffice.cachar.exception.OfficerNotFoundException;
import org.dcoffice.cachar.repository.MeetingRepository;
import org.dcoffice.cachar.repository.OfficerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MeetingService {

    private static final Logger logger = LoggerFactory.getLogger(MeetingService.class);

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private OfficerRepository officerRepository;

    public Meeting createMeeting(MeetingCreateRequest request, String createdById) {
        // Validate that the creator exists
        if (!officerRepository.existsById(createdById)) {
            throw new OfficerNotFoundException("Creator officer not found: " + createdById);
        }

        // Validate that all invited officers exist
        if (request.getInvitedOfficerIds() != null && !request.getInvitedOfficerIds().isEmpty()) {
            for (String officerId : request.getInvitedOfficerIds()) {
                if (!officerRepository.existsById(officerId)) {
                    throw new OfficerNotFoundException("Invited officer not found: " + officerId);
                }
            }
        }

        Meeting meeting = new Meeting();
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setStartDateTime(request.getStartDateTime());
        meeting.setDurationMinutes(request.getDurationMinutes());
        meeting.setCreatedById(createdById);
        meeting.setInvitedOfficerIds(request.getInvitedOfficerIds());

        Meeting savedMeeting = meetingRepository.save(meeting);
        logger.info("Created meeting: {} by officer: {}", savedMeeting.getTitle(), createdById);
        return savedMeeting;
    }

    public List<Meeting> getMeetingsByOfficerId(String officerId) {
        return meetingRepository.findMeetingsByOfficerIdOrderByStartDateTimeDesc(officerId);
    }

    public List<Meeting> getUpcomingMeetingsByOfficerId(String officerId) {
        return meetingRepository.findUpcomingMeetingsByOfficerId(officerId, LocalDateTime.now());
    }

    public List<Meeting> getPastMeetingsByOfficerId(String officerId) {
        return meetingRepository.findPastMeetingsByOfficerId(officerId, LocalDateTime.now());
    }

    public List<Meeting> getAllMeetings() {
        return meetingRepository.findAllByOrderByStartDateTimeDesc();
    }

    public Meeting getMeetingById(String meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found: " + meetingId));
    }
}

