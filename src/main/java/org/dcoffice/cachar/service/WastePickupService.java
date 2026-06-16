package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.*;
import org.dcoffice.cachar.repository.WastePickupRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WastePickupService {
    private final WastePickupRequestRepository repository;
    private final FileStorageService fileStorageService;

    public WastePickupService(WastePickupRequestRepository repository, FileStorageService fileStorageService) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
    }

    public WastePickupRequest createRequest(WastePickupRequest request, List<MultipartFile> beforePhotos) {
        request.setTrackingId(generateTrackingId());
        request.setStatus(WastePickupStatus.SUBMITTED);
        request.setSubmittedAt(LocalDateTime.now());
        request.setSlaHours(request.getUrgency() != null ? request.getUrgency().getSlaHours() : WasteUrgency.NORMAL.getSlaHours());
        request.setBeforePhotos(storeFiles(beforePhotos, "waste-pickup/before"));
        updateSla(request);
        addAudit(request, "REQUEST_SUBMITTED", null, WastePickupStatus.SUBMITTED, "Waste pickup request submitted", null, "CITIZEN", request.getLatitude(), request.getLongitude(), request.getBeforePhotos());
        return repository.save(request);
    }

    public WastePickupRequest track(String trackingId) {
        WastePickupRequest request = repository.findByTrackingId(trackingId)
                .orElseThrow(() -> new IllegalArgumentException("Waste pickup request not found"));
        updateSla(request);
        return repository.save(request);
    }

    public List<WastePickupRequest> trackByMobile(String mobileNumber) {
        List<WastePickupRequest> requests = repository.findByCitizenMobileOrderBySubmittedAtDesc(mobileNumber);
        requests.forEach(this::updateSla);
        return repository.saveAll(requests);
    }

    public WastePickupRequest addFeedback(String trackingId, Integer rating, String feedback) {
        WastePickupRequest request = track(trackingId);
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        request.setCitizenRating(rating);
        request.setCitizenFeedback(feedback);
        addAudit(request, "CITIZEN_FEEDBACK", request.getStatus(), request.getStatus(), feedback, null, "CITIZEN", null, null, Collections.emptyList());
        return repository.save(request);
    }

    public WastePickupRequest reopen(String trackingId, String reason) {
        WastePickupRequest request = track(trackingId);
        if (request.getClosedAt() == null || Duration.between(request.getClosedAt(), LocalDateTime.now()).toHours() > 48) {
            throw new IllegalArgumentException("Request can be reopened only within 48 hours of closure");
        }
        WastePickupStatus previous = request.getStatus();
        request.setStatus(WastePickupStatus.REOPENED);
        request.setReopenedReason(reason);
        request.setClosedAt(null);
        addAudit(request, "REQUEST_REOPENED", previous, WastePickupStatus.REOPENED, reason, null, "CITIZEN", null, null, Collections.emptyList());
        updateSla(request);
        return repository.save(request);
    }

    public List<WastePickupRequest> search(Integer wardNumber, WastePickupStatus status, WasteUrgency urgency,
                                           WasteCategory category, Boolean slaBreached, String officerId,
                                           String locality, LocalDateTime fromDate, LocalDateTime toDate) {
        List<WastePickupRequest> requests = repository.findAll();
        requests.forEach(this::updateSla);
        List<WastePickupRequest> filtered = requests.stream()
                .filter(request -> wardNumber == null || wardNumber.equals(request.getWardNumber()))
                .filter(request -> status == null || status == request.getStatus())
                .filter(request -> urgency == null || urgency == request.getUrgency())
                .filter(request -> category == null || category == request.getWasteCategory())
                .filter(request -> slaBreached == null || slaBreached.equals(request.getSlaBreached()))
                .filter(request -> !StringUtils.hasText(officerId) || officerId.equals(request.getAssignedOfficerId()))
                .filter(request -> !StringUtils.hasText(locality) || (request.getLocality() != null && request.getLocality().toLowerCase().contains(locality.toLowerCase())))
                .filter(request -> fromDate == null || !request.getSubmittedAt().isBefore(fromDate))
                .filter(request -> toDate == null || !request.getSubmittedAt().isAfter(toDate))
                .sorted(Comparator.comparing(WastePickupRequest::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        repository.saveAll(requests);
        return filtered;
    }

    public WastePickupRequest getById(String requestId) {
        WastePickupRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Waste pickup request not found"));
        updateSla(request);
        return repository.save(request);
    }

    public WastePickupRequest verify(String requestId, String remarks, String actorId, String actorRole, Double latitude, Double longitude) {
        WastePickupRequest request = getById(requestId);
        WastePickupStatus previous = request.getStatus();
        request.setStatus(WastePickupStatus.VERIFIED);
        request.setVerifiedAt(LocalDateTime.now());
        addAudit(request, "REQUEST_VERIFIED", previous, WastePickupStatus.VERIFIED, remarks, actorId, actorRole, latitude, longitude, Collections.emptyList());
        updateSla(request);
        return repository.save(request);
    }

    public WastePickupRequest assign(String requestId, String officerId, String staffId, String vehicleNumber,
                                     String teamName, String remarks, String actorId, String actorRole) {
        WastePickupRequest request = getById(requestId);
        WastePickupStatus previous = request.getStatus();
        request.setStatus(WastePickupStatus.ASSIGNED);
        request.setAssignedOfficerId(officerId);
        request.setAssignedSanitationStaffId(staffId);
        request.setAssignedVehicleNumber(vehicleNumber);
        request.setSanitationTeamName(teamName);
        request.setAssignedAt(LocalDateTime.now());
        addAudit(request, "REQUEST_ASSIGNED", previous, WastePickupStatus.ASSIGNED, remarks, actorId, actorRole, null, null, Collections.emptyList());
        updateSla(request);
        return repository.save(request);
    }

    public WastePickupRequest schedule(String requestId, String preferredPickupSlot, String remarks, String actorId, String actorRole) {
        WastePickupRequest request = getById(requestId);
        WastePickupStatus previous = request.getStatus();
        request.setStatus(WastePickupStatus.PICKUP_SCHEDULED);
        request.setPreferredPickupSlot(preferredPickupSlot);
        request.setScheduledAt(LocalDateTime.now());
        addAudit(request, "PICKUP_SCHEDULED", previous, WastePickupStatus.PICKUP_SCHEDULED, remarks, actorId, actorRole, null, null, Collections.emptyList());
        updateSla(request);
        return repository.save(request);
    }

    public WastePickupRequest updateStatus(String requestId, WastePickupStatus status, String remarks,
                                           String actorId, String actorRole, Double latitude, Double longitude) {
        WastePickupRequest request = getById(requestId);
        if (status == WastePickupStatus.CLOSED || status == WastePickupStatus.PICKED_UP) {
            throw new IllegalArgumentException("Use close endpoint to mark a request picked up or closed with mandatory proof");
        }
        WastePickupStatus previous = request.getStatus();
        request.setStatus(status);
        addAudit(request, "STATUS_UPDATED", previous, status, remarks, actorId, actorRole, latitude, longitude, Collections.emptyList());
        updateSla(request);
        return repository.save(request);
    }

    public WastePickupRequest close(String requestId, String closureRemarks, Double closureLatitude, Double closureLongitude,
                                    WasteQuantityEstimate actualQuantity, Boolean siteFullyCleaned, String vehicleNumber,
                                    String teamName, List<MultipartFile> afterPhotos, String actorId, String actorRole) {
        if (!StringUtils.hasText(closureRemarks)) {
            throw new IllegalArgumentException("Closure remarks are required");
        }
        if (closureLatitude == null || closureLongitude == null) {
            throw new IllegalArgumentException("Closure GPS location is required");
        }
        List<String> photoPaths = storeFiles(afterPhotos, "waste-pickup/after");
        if (photoPaths.isEmpty()) {
            throw new IllegalArgumentException("At least one after-pickup photo is required");
        }

        WastePickupRequest request = getById(requestId);
        WastePickupStatus previous = request.getStatus();
        request.setStatus(WastePickupStatus.CLOSED);
        request.setPickedUpAt(LocalDateTime.now());
        request.setClosedAt(LocalDateTime.now());
        request.setClosureRemarks(closureRemarks);
        request.setClosureLatitude(closureLatitude);
        request.setClosureLongitude(closureLongitude);
        request.setActualWasteQuantity(actualQuantity);
        request.setSiteFullyCleaned(siteFullyCleaned);
        request.setAssignedVehicleNumber(vehicleNumber != null ? vehicleNumber : request.getAssignedVehicleNumber());
        request.setSanitationTeamName(teamName != null ? teamName : request.getSanitationTeamName());
        request.getAfterPhotos().addAll(photoPaths);
        addAudit(request, "REQUEST_CLOSED", previous, WastePickupStatus.CLOSED, closureRemarks, actorId, actorRole, closureLatitude, closureLongitude, photoPaths);
        updateSla(request);
        return repository.save(request);
    }

    public Map<String, Object> dashboard(Integer wardNumber, WastePickupStatus status, WasteUrgency urgency,
                                         WasteCategory category, Boolean slaBreached, String officerId,
                                         String locality, LocalDateTime fromDate, LocalDateTime toDate) {
        List<WastePickupRequest> requests = search(wardNumber, status, urgency, category, slaBreached, officerId, locality, fromDate, toDate);
        Map<String, Object> dashboard = new LinkedHashMap<>();
        long pending = requests.stream().filter(request -> !request.getStatus().isClosedState()).count();
        long assigned = requests.stream().filter(request -> request.getStatus() == WastePickupStatus.ASSIGNED).count();
        long picked = requests.stream().filter(request -> request.getStatus() == WastePickupStatus.PICKED_UP || request.getStatus() == WastePickupStatus.CLOSED).count();
        long closed = requests.stream().filter(request -> request.getStatus() == WastePickupStatus.CLOSED).count();
        long reopened = requests.stream().filter(request -> request.getStatus() == WastePickupStatus.REOPENED).count();
        long breached = requests.stream().filter(request -> Boolean.TRUE.equals(request.getSlaBreached())).count();

        dashboard.put("totalRequests", requests.size());
        dashboard.put("pendingRequests", pending);
        dashboard.put("assignedRequests", assigned);
        dashboard.put("pickedUpRequests", picked);
        dashboard.put("closedRequests", closed);
        dashboard.put("reopenedRequests", reopened);
        dashboard.put("slaBreachedRequests", breached);
        dashboard.put("averagePickupTimeHours", averagePickupTimeHours(requests));
        dashboard.put("wardWiseData", groupCount(requests, request -> request.getWardNumber() == null ? "Unmapped" : "Ward " + request.getWardNumber()));
        dashboard.put("categoryWiseData", groupCount(requests, request -> request.getWasteCategory() == null ? "Other" : request.getWasteCategory().getDisplayName()));
        dashboard.put("urgencyWiseData", groupCount(requests, request -> request.getUrgency() == null ? "Normal" : request.getUrgency().getDisplayName()));
        dashboard.put("officerWisePendency", groupCount(
                requests.stream().filter(request -> !request.getStatus().isClosedState()).collect(Collectors.toList()),
                request -> request.getAssignedOfficerId() == null ? "Unassigned" : request.getAssignedOfficerId()
        ));
        dashboard.put("repeatHotspots", hotspots());
        dashboard.put("oldestPendingRequests", requests.stream()
                .filter(request -> !request.getStatus().isClosedState())
                .sorted(Comparator.comparing(WastePickupRequest::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .collect(Collectors.toList()));
        return dashboard;
    }

    public List<Map<String, Object>> hotspots() {
        LocalDateTime since = LocalDateTime.now().minusDays(15);
        List<WastePickupRequest> recent = repository.findByStatusNotInAndSubmittedAtAfter(
                Arrays.asList(WastePickupStatus.CLOSED, WastePickupStatus.REJECTED),
                since
        ).stream()
                .filter(request -> request.getLatitude() != null && request.getLongitude() != null)
                .collect(Collectors.toList());

        List<Map<String, Object>> hotspots = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (WastePickupRequest request : recent) {
            if (visited.contains(request.getId())) continue;
            List<WastePickupRequest> nearby = recent.stream()
                    .filter(candidate -> distanceMeters(request.getLatitude(), request.getLongitude(), candidate.getLatitude(), candidate.getLongitude()) <= 100)
                    .collect(Collectors.toList());
            if (nearby.size() >= 3) {
                nearby.forEach(item -> visited.add(item.getId()));
                Map<String, Object> hotspot = new LinkedHashMap<>();
                hotspot.put("label", "Repeated Waste Hotspot");
                hotspot.put("count", nearby.size());
                hotspot.put("wardNumber", request.getWardNumber());
                hotspot.put("locality", request.getLocality());
                hotspot.put("latitude", request.getLatitude());
                hotspot.put("longitude", request.getLongitude());
                hotspot.put("requests", nearby.stream().map(WastePickupRequest::getTrackingId).collect(Collectors.toList()));
                hotspots.add(hotspot);
            }
        }
        return hotspots;
    }

    public List<Map<String, Object>> routeSuggestions(Integer wardNumber) {
        List<WastePickupRequest> pending = search(wardNumber, null, null, null, null, null, null, null, null)
                .stream()
                .filter(request -> !request.getStatus().isClosedState())
                .sorted(Comparator
                        .comparing(WastePickupRequest::getWardNumber, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(WastePickupRequest::getLocality, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());

        return pending.stream()
                .collect(Collectors.groupingBy(request -> request.getWardNumber() == null ? "Unmapped" : "Ward " + request.getWardNumber(), LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    Map<String, Object> route = new LinkedHashMap<>();
                    route.put("routeName", entry.getKey() + " sanitation pickup route");
                    route.put("requestCount", entry.getValue().size());
                    route.put("requests", entry.getValue());
                    return route;
                })
                .collect(Collectors.toList());
    }

    private void updateSla(WastePickupRequest request) {
        if (request.getSubmittedAt() == null || request.getStatus().isClosedState()) {
            request.setSlaBreached(false);
            return;
        }
        LocalDateTime dueAt = request.getSubmittedAt().plusHours(request.getSlaHours() == null ? 48 : request.getSlaHours());
        request.setSlaBreached(LocalDateTime.now().isAfter(dueAt));
        if (Boolean.TRUE.equals(request.getSlaBreached())) {
            request.setEscalationLevel(Math.max(request.getEscalationLevel() == null ? 0 : request.getEscalationLevel(), 1));
        }
    }

    private List<String> storeFiles(List<MultipartFile> files, String directory) {
        if (files == null) return new ArrayList<>();
        List<String> paths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            try {
                paths.add(fileStorageService.storeFile(file, directory));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to store photo: " + e.getMessage(), e);
            }
        }
        return paths;
    }

    private String generateTrackingId() {
        return "SMC-WP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void addAudit(WastePickupRequest request, String action, WastePickupStatus previousStatus, WastePickupStatus newStatus,
                          String remarks, String actorId, String actorRole, Double latitude, Double longitude, List<String> photos) {
        WastePickupAuditTrail audit = new WastePickupAuditTrail();
        audit.setAction(action);
        audit.setPreviousStatus(previousStatus);
        audit.setNewStatus(newStatus);
        audit.setRemarks(remarks);
        audit.setActorId(actorId);
        audit.setActorRole(actorRole);
        audit.setLatitude(latitude);
        audit.setLongitude(longitude);
        audit.setPhotos(photos == null ? new ArrayList<>() : photos);
        request.getAuditTrail().add(audit);
    }

    private double averagePickupTimeHours(List<WastePickupRequest> requests) {
        return Math.round(requests.stream()
                .filter(request -> request.getSubmittedAt() != null && request.getClosedAt() != null)
                .mapToLong(request -> Duration.between(request.getSubmittedAt(), request.getClosedAt()).toHours())
                .average()
                .orElse(0) * 10.0) / 10.0;
    }

    private Map<String, Long> groupCount(List<WastePickupRequest> requests, java.util.function.Function<WastePickupRequest, String> classifier) {
        return requests.stream().collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.counting()));
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
