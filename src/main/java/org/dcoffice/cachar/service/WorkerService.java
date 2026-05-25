package org.dcoffice.cachar.service;

import org.dcoffice.cachar.dto.AdminWorkerSummaryDto;
import org.dcoffice.cachar.dto.AttendanceRecordDto;
import org.dcoffice.cachar.dto.LocationCoordsDto;
import org.dcoffice.cachar.dto.WorkPhotoDto;
import org.dcoffice.cachar.dto.WorkerUserDto;
import org.dcoffice.cachar.entity.TrackingActivity;
import org.dcoffice.cachar.entity.TrackingMember;
import org.dcoffice.cachar.repository.TrackingActivityRepository;
import org.dcoffice.cachar.repository.TrackingMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkerService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerService.class);
    private static final List<String> ATTENDANCE_TYPES = Arrays.asList("LOGIN", "LOGOUT");

    // Static OTP — replace with SMS provider integration when ready
    private static final String STATIC_OTP = "24052026";

    @Autowired
    private TrackingMemberRepository trackingMemberRepository;

    @Autowired
    private TrackingActivityRepository trackingActivityRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Step 1: Send OTP to mobile.
     * Currently a no-op — OTP is hardcoded as STATIC_OTP.
     * Replace body with SMS provider call when ready.
     */
    public void sendOtp(String mobile) {
        logger.info("OTP requested for mobile: {} (static OTP in use)", mobile);
        // TODO: integrate SMS provider here
    }

    /**
     * Step 2: Verify OTP then log in.
     * Throws IllegalArgumentException if OTP is wrong.
     */
    public Map<String, Object> login(String mobile, String otp) {
        if (!STATIC_OTP.equals(otp)) {
            throw new IllegalArgumentException("INVALID_OTP");
        }
        TrackingMember member = trackingMemberRepository.findByPhone(mobile)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        String token = jwtService.generateTokenForWorker(member);
        Map<String, Object> result = new HashMap<>();
        result.put("user", toUserDto(member));
        result.put("token", token);
        return result;
    }

    public Map<String, Object> signup(String mobile, String name, String address) {
        if (trackingMemberRepository.findByPhone(mobile).isPresent()) {
            throw new IllegalStateException("Mobile number already registered");
        }
        TrackingMember member = new TrackingMember();
        member.setId("mem-" + UUID.randomUUID().toString().substring(0, 8));
        member.setPhone(mobile);
        member.setName(name != null ? name.trim() : "");
        member.setAddress(address != null ? address.trim() : null);
        member.setStatus("ACTIVE");
        member.setAdmin(false);
        member.setCreatedAt(Instant.now());
        TrackingMember saved = trackingMemberRepository.save(member);
        String token = jwtService.generateTokenForWorker(saved);
        Map<String, Object> result = new HashMap<>();
        result.put("user", toUserDto(saved));
        result.put("token", token);
        return result;
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    public AttendanceRecordDto markLogin(String userId, LocationCoordsDto location) {
        TrackingMember member = requireMember(userId);
        Instant[] range = todayRange();

        List<TrackingActivity> existing = trackingActivityRepository
                .findByMemberIdAndTypeInAndTimestampBetweenOrderByTimestampDesc(userId, ATTENDANCE_TYPES, range[0], range[1]);

        boolean hasLogin = existing.stream().anyMatch(a -> "LOGIN".equals(a.getType()));

        if (!hasLogin) {
            Instant now = Instant.now();
            TrackingActivity login = new TrackingActivity();
            login.setMemberId(userId);
            login.setMemberName(member.getName());
            login.setSquadId(member.getSquadId());
            login.setType("LOGIN");
            login.setStatus("ON_DUTY");
            login.setTimestamp(now);
            applyLocation(login, location);
            existing.add(trackingActivityRepository.save(login));

            member.setStatus("ON_DUTY");
            applyMemberLocation(member, location);
            member.setLastUpdate(now);
            trackingMemberRepository.save(member);
            logger.info("Marked LOGIN for member {}", userId);
        }

        return buildAttendanceRecord(userId, existing);
    }

    public AttendanceRecordDto markLogout(String userId, LocationCoordsDto location) {
        TrackingMember member = requireMember(userId);
        Instant[] range = todayRange();

        List<TrackingActivity> existing = trackingActivityRepository
                .findByMemberIdAndTypeInAndTimestampBetweenOrderByTimestampDesc(userId, ATTENDANCE_TYPES, range[0], range[1]);

        boolean hasLogin = existing.stream().anyMatch(a -> "LOGIN".equals(a.getType()));
        if (!hasLogin) {
            throw new IllegalStateException("No attendance login found for today. Please mark attendance first.");
        }

        boolean hasLogout = existing.stream().anyMatch(a -> "LOGOUT".equals(a.getType()));

        if (!hasLogout) {
            Instant now = Instant.now();
            TrackingActivity logout = new TrackingActivity();
            logout.setMemberId(userId);
            logout.setMemberName(member.getName());
            logout.setSquadId(member.getSquadId());
            logout.setType("LOGOUT");
            logout.setStatus("ACTIVE");
            logout.setTimestamp(now);
            applyLocation(logout, location);
            existing.add(trackingActivityRepository.save(logout));

            member.setStatus("ACTIVE");
            applyMemberLocation(member, location);
            member.setLastUpdate(now);
            trackingMemberRepository.save(member);
            logger.info("Marked LOGOUT for member {}", userId);
        }

        return buildAttendanceRecord(userId, existing);
    }

    public AttendanceRecordDto getTodayAttendance(String userId) {
        requireMember(userId);
        Instant[] range = todayRange();
        List<TrackingActivity> activities = trackingActivityRepository
                .findByMemberIdAndTypeInAndTimestampBetweenOrderByTimestampDesc(userId, ATTENDANCE_TYPES, range[0], range[1]);
        if (activities.isEmpty()) {
            return null;
        }
        return buildAttendanceRecord(userId, activities);
    }

    public List<AttendanceRecordDto> getAttendanceHistory(String userId) {
        requireMember(userId);
        List<TrackingActivity> all = trackingActivityRepository
                .findByMemberIdAndTypeInOrderByTimestampDesc(userId, ATTENDANCE_TYPES);

        // Group by date (UTC) preserving order (newest first)
        Map<String, List<TrackingActivity>> byDate = new LinkedHashMap<>();
        for (TrackingActivity a : all) {
            String date = a.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate().toString();
            byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(a);
        }

        return byDate.values().stream()
                .map(acts -> buildAttendanceRecord(userId, acts))
                .collect(Collectors.toList());
    }

    // ── Photos ────────────────────────────────────────────────────────────────

    public WorkPhotoDto uploadPhoto(String userId, MultipartFile file, String notes,
                                    Double latitude, Double longitude, Double accuracy) {
        TrackingMember member = requireMember(userId);
        String path;
        try {
            path = fileStorageService.storeFile(file, "worker-photos");
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to store photo: " + e.getMessage());
        }

        Instant now = Instant.now();
        TrackingActivity activity = new TrackingActivity();
        activity.setMemberId(userId);
        activity.setMemberName(member.getName());
        activity.setSquadId(member.getSquadId());
        activity.setType("PHOTO");
        activity.setStatus(member.getStatus());
        activity.setLatitude(latitude);
        activity.setLongitude(longitude);
        activity.setAccuracy(accuracy);
        activity.setNotes(notes);
        activity.setTimestamp(now);
        activity.setAttachments(Collections.singletonList(path));

        TrackingActivity saved = trackingActivityRepository.save(activity);
        return toWorkPhotoDto(saved);
    }

    public List<WorkPhotoDto> getTodayPhotos(String userId) {
        requireMember(userId);
        Instant[] range = todayRange();
        List<TrackingActivity> activities = trackingActivityRepository
                .findByMemberIdAndTypeAndTimestampBetweenOrderByTimestampDesc(userId, "PHOTO", range[0], range[1]);
        return activities.stream().map(this::toWorkPhotoDto).collect(Collectors.toList());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /**
     * Returns workers scoped to the supervisor's squad.
     * If the supervisor has no squadId, returns all workers (global fallback).
     */
    public List<WorkerUserDto> getWorkers(String supervisorId) {
        TrackingMember supervisor = requireMember(supervisorId);
        String squadId = supervisor.getSquadId();

        List<TrackingMember> members = (squadId != null && !squadId.isEmpty())
                ? trackingMemberRepository.findBySquadIdOrderByNameAsc(squadId)
                : trackingMemberRepository.findAllByOrderByNameAsc();

        return members.stream().map(this::toUserDto).collect(Collectors.toList());
    }

    /**
     * Returns today's attendance scoped to the supervisor's squad.
     * If the supervisor has no squadId, returns all workers (global fallback).
     */
    public List<AdminWorkerSummaryDto> getSquadTodayAttendance(String supervisorId) {
        TrackingMember supervisor = requireMember(supervisorId);
        String squadId = supervisor.getSquadId();

        Instant[] range = todayRange();
        List<TrackingActivity> activities = (squadId != null && !squadId.isEmpty())
                ? trackingActivityRepository.findBySquadIdAndTypeInAndTimestampBetween(squadId, ATTENDANCE_TYPES, range[0], range[1])
                : trackingActivityRepository.findByTypeInAndTimestampBetween(ATTENDANCE_TYPES, range[0], range[1]);

        Map<String, List<TrackingActivity>> byMember = activities.stream()
                .collect(Collectors.groupingBy(TrackingActivity::getMemberId));

        List<TrackingMember> squadMembers = (squadId != null && !squadId.isEmpty())
                ? trackingMemberRepository.findBySquadIdOrderByNameAsc(squadId)
                : trackingMemberRepository.findAllByOrderByNameAsc();

        List<AdminWorkerSummaryDto> result = new ArrayList<>();
        for (TrackingMember member : squadMembers) {
            List<TrackingActivity> acts = byMember.getOrDefault(member.getId(), Collections.emptyList());
            AttendanceRecordDto attendance = acts.isEmpty() ? null : buildAttendanceRecord(member.getId(), acts);
            AdminWorkerSummaryDto summary = new AdminWorkerSummaryDto();
            summary.setUser(toUserDto(member));
            summary.setTodayAttendance(attendance);
            result.add(summary);
        }
        return result;
    }

    /** Sets or clears the admin (supervisor) flag on a member. */
    public WorkerUserDto setSupervisor(String memberId, boolean isSupervisor) {
        TrackingMember member = requireMember(memberId);
        member.setAdmin(isSupervisor);
        return toUserDto(trackingMemberRepository.save(member));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private TrackingMember requireMember(String userId) {
        return trackingMemberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }

    private Instant[] todayRange() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return new Instant[]{
                today.atStartOfDay(ZoneOffset.UTC).toInstant(),
                today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        };
    }

    private AttendanceRecordDto buildAttendanceRecord(String userId, List<TrackingActivity> activities) {
        TrackingActivity login = activities.stream()
                .filter(a -> "LOGIN".equals(a.getType())).findFirst().orElse(null);
        TrackingActivity logout = activities.stream()
                .filter(a -> "LOGOUT".equals(a.getType())).findFirst().orElse(null);

        TrackingActivity reference = login != null ? login : logout;
        String date = reference.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate().toString();

        AttendanceRecordDto dto = new AttendanceRecordDto();
        dto.setId(userId + "_" + date);
        dto.setUserId(userId);
        dto.setDate(date);

        if (login != null) {
            dto.setLoginTime(login.getTimestamp().toString());
            if (login.getLatitude() != null) {
                dto.setLoginLocation(new LocationCoordsDto(login.getLatitude(), login.getLongitude(), login.getAccuracy()));
            }
        }
        if (logout != null) {
            dto.setLogoutTime(logout.getTimestamp().toString());
            if (logout.getLatitude() != null) {
                dto.setLogoutLocation(new LocationCoordsDto(logout.getLatitude(), logout.getLongitude(), logout.getAccuracy()));
            }
        }
        return dto;
    }

    private void applyLocation(TrackingActivity activity, LocationCoordsDto location) {
        if (location != null) {
            activity.setLatitude(location.getLatitude());
            activity.setLongitude(location.getLongitude());
            activity.setAccuracy(location.getAccuracy());
        }
    }

    private void applyMemberLocation(TrackingMember member, LocationCoordsDto location) {
        if (location != null && location.getLatitude() != null && location.getLongitude() != null) {
            member.setLocation(new GeoJsonPoint(location.getLongitude(), location.getLatitude()));
        }
    }

    private WorkerUserDto toUserDto(TrackingMember member) {
        WorkerUserDto dto = new WorkerUserDto();
        dto.setId(member.getId());
        dto.setMobile(member.getPhone());
        dto.setName(member.getName());
        dto.setAddress(member.getAddress());
        dto.setCreatedAt(member.getCreatedAt() != null ? member.getCreatedAt().toString() : null);
        dto.setAdmin(member.isAdmin());
        return dto;
    }

    private WorkPhotoDto toWorkPhotoDto(TrackingActivity activity) {
        WorkPhotoDto dto = new WorkPhotoDto();
        dto.setId(activity.getId());
        dto.setUserId(activity.getMemberId());
        dto.setNotes(activity.getNotes());
        dto.setUploadedAt(activity.getTimestamp() != null ? activity.getTimestamp().toString() : null);

        if (activity.getLatitude() != null) {
            dto.setLocation(new LocationCoordsDto(activity.getLatitude(), activity.getLongitude(), activity.getAccuracy()));
        }

        List<String> attachments = activity.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            dto.setImageUri(appBaseUrl + "/api/files/download/" + attachments.get(0));
        }
        return dto;
    }
}
