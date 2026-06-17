package org.dcoffice.cachar.service;

import org.dcoffice.cachar.dto.GovernanceDashboardResponse;
import org.dcoffice.cachar.entity.Complaint;
import org.dcoffice.cachar.entity.ComplaintCategory;
import org.dcoffice.cachar.entity.ComplaintStatus;
import org.dcoffice.cachar.entity.Department;
import org.dcoffice.cachar.entity.Officer;
import org.dcoffice.cachar.entity.Priority;
import org.dcoffice.cachar.repository.ComplaintRepository;
import org.dcoffice.cachar.repository.OfficerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GovernanceDashboardService {
    private final ComplaintRepository complaintRepository;
    private final OfficerRepository officerRepository;

    public GovernanceDashboardService(ComplaintRepository complaintRepository, OfficerRepository officerRepository) {
        this.complaintRepository = complaintRepository;
        this.officerRepository = officerRepository;
    }

    public GovernanceDashboardResponse getDashboard(Integer pendingAgeDays) {
        LocalDateTime now = LocalDateTime.now();
        Integer safePendingAgeDays = pendingAgeDays == null ? null : Math.max(1, Math.min(pendingAgeDays, 3650));
        boolean slaFilterApplied = safePendingAgeDays != null;

        List<Complaint> allComplaints = complaintRepository.findAll();
        List<Complaint> complaints = allComplaints.stream()
                .filter(complaint -> !slaFilterApplied || isPendingBeyondAge(complaint, now, safePendingAgeDays))
                .collect(Collectors.toList());

        GovernanceDashboardResponse response = new GovernanceDashboardResponse();
        response.setSummary(buildSummary(complaints, allComplaints, now, safePendingAgeDays));
        response.setWardPerformance(buildWardPerformance(complaints, now));
        response.setDepartmentPerformance(buildDepartmentPerformance(complaints, now));
        response.setOfficerPerformance(buildOfficerPerformance(complaints, now));
        response.setCategoryPerformance(buildCategoryPerformance(complaints, now));
        response.setStatusBreakdown(buildStatusBreakdown(complaints));
        response.setPriorityItems(buildPriorityItems(complaints, now));
        response.setOldestPendingComplaints(buildOldestPendingComplaints(complaints, now));
        return response;
    }

    private GovernanceDashboardResponse.Summary buildSummary(
            List<Complaint> complaints,
            List<Complaint> allComplaints,
            LocalDateTime now,
            Integer selectedSlaDays
    ) {
        GovernanceDashboardResponse.Summary summary = new GovernanceDashboardResponse.Summary();
        long total = complaints.size();
        long open = complaints.stream().filter(this::isOpen).count();
        long resolved = complaints.stream().filter(this::isResolved).count();
        long rejected = complaints.stream().filter(complaint -> complaint.getStatus() == ComplaintStatus.REJECTED).count();
        long breached = complaints.stream().filter(complaint -> isSlaBreached(complaint, now)).count();
        long escalated = complaints.stream().filter(complaint -> isEscalated(complaint, now)).count();
        long dueToday = complaints.stream().filter(complaint -> isDueToday(complaint, now)).count();
        long geoTagged = complaints.stream().filter(this::hasGeoTag).count();
        double averageResolutionDays = complaints.stream()
                .filter(this::isResolved)
                .mapToLong(this::resolutionDays)
                .filter(days -> days >= 0)
                .average()
                .orElse(0);

        double averageRating = complaints.stream()
                .map(Complaint::getCitizenRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        summary.setSlaFilterApplied(selectedSlaDays != null);
        summary.setSelectedSlaDays(selectedSlaDays);
        summary.setTotalComplaints(total);
        summary.setOpenComplaints(open);
        summary.setPendingComplaints(open);
        summary.setResolvedComplaints(resolved);
        summary.setRejectedComplaints(rejected);
        summary.setEscalatedComplaints(escalated);
        summary.setSlaBreached(breached);
        summary.setDueToday(dueToday);
        summary.setGeoTaggedComplaints(geoTagged);
        summary.setResolutionRate(percent(resolved, total));
        long allPending = allComplaints.stream().filter(this::isOpen).count();
        summary.setSlaBreachPercentage(selectedSlaDays == null ? percent(breached, open) : percent(total, allPending));
        summary.setGeoTagCoverage(percent(geoTagged, total));
        summary.setAverageCitizenRating(round(averageRating));
        summary.setAverageResolutionDays(round(averageResolutionDays));
        return summary;
    }

    private List<GovernanceDashboardResponse.WardPerformance> buildWardPerformance(
            List<Complaint> complaints,
            LocalDateTime now
    ) {
        Map<Integer, List<Complaint>> byWard = complaints.stream()
                .collect(Collectors.groupingBy(complaint -> complaint.getWardNumber() == null ? 0 : complaint.getWardNumber()));

        return byWard.entrySet().stream()
                .map(entry -> {
                    List<Complaint> wardComplaints = entry.getValue();
                    Complaint first = wardComplaints.get(0);
                    long total = wardComplaints.size();
                    long open = wardComplaints.stream().filter(this::isOpen).count();
                    long resolved = wardComplaints.stream().filter(this::isResolved).count();

                    GovernanceDashboardResponse.WardPerformance ward = new GovernanceDashboardResponse.WardPerformance();
                    ward.setWardNumber(entry.getKey() == 0 ? null : entry.getKey());
                    ward.setWardName(first.getWardName() != null ? first.getWardName() : (entry.getKey() == 0 ? "Unmapped" : "Ward " + entry.getKey()));
                    ward.setZone(first.getZone());
                    ward.setTotalComplaints(total);
                    ward.setOpenComplaints(open);
                    ward.setResolvedComplaints(resolved);
                    ward.setSlaBreached(wardComplaints.stream().filter(complaint -> isSlaBreached(complaint, now)).count());
                    ward.setResolutionRate(percent(resolved, total));
                    ward.setTopCategory(findTopCategoryLabel(wardComplaints));
                    return ward;
                })
                .sorted((left, right) -> {
                    int slaCompare = Long.compare(right.getSlaBreached(), left.getSlaBreached());
                    if (slaCompare != 0) return slaCompare;
                    return Long.compare(right.getOpenComplaints(), left.getOpenComplaints());
                })
                .limit(12)
                .collect(Collectors.toList());
    }

    private List<GovernanceDashboardResponse.CategoryPerformance> buildCategoryPerformance(
            List<Complaint> complaints,
            LocalDateTime now
    ) {
        Map<ComplaintCategory, List<Complaint>> byCategory = complaints.stream()
                .collect(Collectors.groupingBy(
                        complaint -> complaint.getCategory() == null ? ComplaintCategory.OTHER : complaint.getCategory(),
                        () -> new EnumMap<>(ComplaintCategory.class),
                        Collectors.toList()
                ));

        return byCategory.entrySet().stream()
                .map(entry -> {
                    List<Complaint> categoryComplaints = entry.getValue();
                    long total = categoryComplaints.size();
                    long resolved = categoryComplaints.stream().filter(this::isResolved).count();

                    GovernanceDashboardResponse.CategoryPerformance category = new GovernanceDashboardResponse.CategoryPerformance();
                    category.setCategory(entry.getKey().name());
                    category.setLabel(entry.getKey().getDisplayName());
                    category.setTotalComplaints(total);
                    category.setOpenComplaints(categoryComplaints.stream().filter(this::isOpen).count());
                    category.setSlaBreached(categoryComplaints.stream().filter(complaint -> isSlaBreached(complaint, now)).count());
                    category.setResolutionRate(percent(resolved, total));
                    return category;
                })
                .sorted((left, right) -> {
                    int openCompare = Long.compare(right.getOpenComplaints(), left.getOpenComplaints());
                    if (openCompare != 0) return openCompare;
                    return Long.compare(right.getSlaBreached(), left.getSlaBreached());
                })
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<GovernanceDashboardResponse.DepartmentPerformance> buildDepartmentPerformance(
            List<Complaint> complaints,
            LocalDateTime now
    ) {
        Map<Department, List<Complaint>> byDepartment = complaints.stream()
                .collect(Collectors.groupingBy(
                        complaint -> complaint.getAssignedDepartment() == null ? Department.UNASSIGNED : complaint.getAssignedDepartment(),
                        () -> new EnumMap<>(Department.class),
                        Collectors.toList()
                ));

        return byDepartment.entrySet().stream()
                .map(entry -> {
                    List<Complaint> departmentComplaints = entry.getValue();
                    long total = departmentComplaints.size();
                    long resolved = departmentComplaints.stream().filter(this::isResolved).count();

                    GovernanceDashboardResponse.DepartmentPerformance department = new GovernanceDashboardResponse.DepartmentPerformance();
                    department.setDepartment(entry.getKey().name());
                    department.setLabel(entry.getKey().getDisplayName());
                    department.setTotalComplaints(total);
                    department.setOpenComplaints(departmentComplaints.stream().filter(this::isOpen).count());
                    department.setSlaBreached(departmentComplaints.stream().filter(complaint -> isSlaBreached(complaint, now)).count());
                    department.setResolutionRate(percent(resolved, total));
                    return department;
                })
                .sorted((left, right) -> {
                    int openCompare = Long.compare(right.getOpenComplaints(), left.getOpenComplaints());
                    if (openCompare != 0) return openCompare;
                    return Long.compare(right.getSlaBreached(), left.getSlaBreached());
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<GovernanceDashboardResponse.OfficerPerformance> buildOfficerPerformance(
            List<Complaint> complaints,
            LocalDateTime now
    ) {
        Map<String, List<Complaint>> byOfficer = complaints.stream()
                .collect(Collectors.groupingBy(complaint -> {
                    String officerId = complaint.getAssignedToId();
                    return officerId == null || officerId.trim().isEmpty() ? "UNASSIGNED" : officerId;
                }));
        Map<String, Officer> officersById = loadOfficersByAssignmentId(byOfficer.keySet());

        return byOfficer.entrySet().stream()
                .map(entry -> {
                    List<Complaint> officerComplaints = entry.getValue();

                    GovernanceDashboardResponse.OfficerPerformance officer = new GovernanceDashboardResponse.OfficerPerformance();
                    officer.setOfficerId("UNASSIGNED".equals(entry.getKey()) ? null : entry.getKey());
                    officer.setLabel(resolveOfficerLabel(entry.getKey(), officersById));
                    officer.setTotalComplaints(officerComplaints.size());
                    officer.setOpenComplaints(officerComplaints.stream().filter(this::isOpen).count());
                    officer.setSlaBreached(officerComplaints.stream().filter(complaint -> isSlaBreached(complaint, now)).count());
                    return officer;
                })
                .sorted((left, right) -> {
                    int openCompare = Long.compare(right.getOpenComplaints(), left.getOpenComplaints());
                    if (openCompare != 0) return openCompare;
                    return Long.compare(right.getSlaBreached(), left.getSlaBreached());
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    private Map<String, Officer> loadOfficersByAssignmentId(Set<String> assignmentIds) {
        Set<String> concreteIds = assignmentIds.stream()
                .filter(id -> id != null && !"UNASSIGNED".equals(id))
                .collect(Collectors.toCollection(HashSet::new));

        Map<String, Officer> officersById = officerRepository.findAllById(concreteIds).stream()
                .collect(Collectors.toMap(Officer::getId, Function.identity()));

        concreteIds.stream()
                .filter(id -> !officersById.containsKey(id))
                .forEach(id -> officerRepository.findByEmployeeId(id)
                        .ifPresent(officer -> officersById.put(id, officer)));

        return officersById;
    }

    private String resolveOfficerLabel(String assignmentId, Map<String, Officer> officersById) {
        if ("UNASSIGNED".equals(assignmentId)) {
            return "Unassigned";
        }

        Officer officer = officersById.get(assignmentId);
        if (officer == null) {
            return assignmentId;
        }

        String name = officer.getName();
        String employeeId = officer.getEmployeeId();
        if (name == null || name.trim().isEmpty()) {
            return employeeId == null || employeeId.trim().isEmpty() ? assignmentId : employeeId;
        }

        return employeeId == null || employeeId.trim().isEmpty()
                ? name
                : name + " (" + employeeId + ")";
    }

    private List<GovernanceDashboardResponse.StatusBreakdown> buildStatusBreakdown(List<Complaint> complaints) {
        Map<ComplaintStatus, Long> counts = complaints.stream()
                .filter(complaint -> complaint.getStatus() != null)
                .collect(Collectors.groupingBy(Complaint::getStatus, () -> new EnumMap<>(ComplaintStatus.class), Collectors.counting()));

        return counts.entrySet().stream()
                .map(entry -> {
                    GovernanceDashboardResponse.StatusBreakdown status = new GovernanceDashboardResponse.StatusBreakdown();
                    status.setStatus(entry.getKey().name());
                    status.setLabel(entry.getKey().getDisplayName());
                    status.setCount(entry.getValue());
                    return status;
                })
                .sorted(Comparator.comparing(GovernanceDashboardResponse.StatusBreakdown::getStatus))
                .collect(Collectors.toList());
    }

    private List<GovernanceDashboardResponse.PriorityItem> buildPriorityItems(List<Complaint> complaints, LocalDateTime now) {
        return complaints.stream()
                .filter(this::isOpen)
                .filter(complaint -> isSlaBreached(complaint, now) || complaint.getPriority() == Priority.HIGH || complaint.getPriority() == Priority.URGENT)
                .sorted(Comparator
                        .comparing((Complaint complaint) -> !isSlaBreached(complaint, now))
                        .thenComparing(Complaint::getSlaDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Complaint::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .map(complaint -> toPriorityItem(complaint, now))
                .collect(Collectors.toList());
    }

    private List<GovernanceDashboardResponse.PriorityItem> buildOldestPendingComplaints(
            List<Complaint> complaints,
            LocalDateTime now
    ) {
        return complaints.stream()
                .filter(this::isOpen)
                .sorted(Comparator.comparing(Complaint::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .map(complaint -> toPriorityItem(complaint, now))
                .collect(Collectors.toList());
    }

    private GovernanceDashboardResponse.PriorityItem toPriorityItem(Complaint complaint, LocalDateTime now) {
        GovernanceDashboardResponse.PriorityItem item = new GovernanceDashboardResponse.PriorityItem();
        item.setId(complaint.getId());
        item.setComplaintNumber(complaint.getComplaintNumber());
        item.setSubject(complaint.getSubject());
        item.setCategory(complaint.getCategory() != null ? complaint.getCategory().getDisplayName() : "Other");
        item.setPriority(complaint.getPriority() != null ? complaint.getPriority().name() : Priority.MEDIUM.name());
        item.setStatus(complaint.getStatus() != null ? complaint.getStatus().getDisplayName() : "Created");
        item.setWardNumber(complaint.getWardNumber());
        item.setWardName(complaint.getWardName());
        item.setLocation(complaint.getLocation());
        item.setSlaDueAt(complaint.getSlaDueAt() != null ? complaint.getSlaDueAt().toString() : null);
        item.setSlaBreached(isSlaBreached(complaint, now));
        item.setAgeDays(complaintAgeDays(complaint, now));
        return item;
    }

    private String findTopCategoryLabel(List<Complaint> complaints) {
        Map<ComplaintCategory, Long> counts = new HashMap<>();
        for (Complaint complaint : complaints) {
            ComplaintCategory category = complaint.getCategory() == null ? ComplaintCategory.OTHER : complaint.getCategory();
            counts.put(category, counts.getOrDefault(category, 0L) + 1);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().getDisplayName())
                .orElse("No data");
    }

    private boolean isOpen(Complaint complaint) {
        ComplaintStatus status = complaint.getStatus();
        return status == null || !(status.isResolved() || status.isFinal());
    }

    private boolean isResolved(Complaint complaint) {
        return complaint.getStatus() != null && complaint.getStatus().isResolved();
    }

    private boolean isSlaBreached(Complaint complaint, LocalDateTime now) {
        return isOpen(complaint) && complaint.getSlaDueAt() != null && complaint.getSlaDueAt().isBefore(now);
    }

    private boolean isEscalated(Complaint complaint, LocalDateTime now) {
        return isOpen(complaint) && (complaint.getStatus() == ComplaintStatus.BLOCKED || isSlaBreached(complaint, now));
    }

    private boolean isPendingBeyondAge(Complaint complaint, LocalDateTime now, int selectedDays) {
        return isOpen(complaint) && complaintAgeDays(complaint, now) >= selectedDays;
    }

    private long complaintAgeDays(Complaint complaint, LocalDateTime now) {
        if (complaint.getCreatedAt() == null) return 0;
        return Math.max(0, ChronoUnit.DAYS.between(complaint.getCreatedAt(), now));
    }

    private long resolutionDays(Complaint complaint) {
        if (complaint.getCreatedAt() == null) return -1;
        LocalDateTime resolvedAt = complaint.getClosedAt() != null ? complaint.getClosedAt() : complaint.getUpdatedAt();
        if (resolvedAt == null) return -1;
        return Math.max(0, ChronoUnit.DAYS.between(complaint.getCreatedAt(), resolvedAt));
    }

    private boolean isDueToday(Complaint complaint, LocalDateTime now) {
        if (!isOpen(complaint) || complaint.getSlaDueAt() == null) return false;
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        return !complaint.getSlaDueAt().isBefore(now) && !complaint.getSlaDueAt().isAfter(endOfDay);
    }

    private boolean hasGeoTag(Complaint complaint) {
        return complaint.getLatitude() != null && complaint.getLongitude() != null;
    }

    private double percent(long numerator, long denominator) {
        if (denominator == 0) return 0;
        return round((numerator * 100.0) / denominator);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
