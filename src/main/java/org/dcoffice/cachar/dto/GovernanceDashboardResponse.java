package org.dcoffice.cachar.dto;

import java.util.ArrayList;
import java.util.List;

public class GovernanceDashboardResponse {
    private Summary summary = new Summary();
    private List<WardPerformance> wardPerformance = new ArrayList<>();
    private List<CategoryPerformance> categoryPerformance = new ArrayList<>();
    private List<DepartmentPerformance> departmentPerformance = new ArrayList<>();
    private List<OfficerPerformance> officerPerformance = new ArrayList<>();
    private List<StatusBreakdown> statusBreakdown = new ArrayList<>();
    private List<PriorityItem> priorityItems = new ArrayList<>();
    private List<PriorityItem> oldestPendingComplaints = new ArrayList<>();

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }

    public List<WardPerformance> getWardPerformance() { return wardPerformance; }
    public void setWardPerformance(List<WardPerformance> wardPerformance) { this.wardPerformance = wardPerformance; }

    public List<CategoryPerformance> getCategoryPerformance() { return categoryPerformance; }
    public void setCategoryPerformance(List<CategoryPerformance> categoryPerformance) { this.categoryPerformance = categoryPerformance; }

    public List<DepartmentPerformance> getDepartmentPerformance() { return departmentPerformance; }
    public void setDepartmentPerformance(List<DepartmentPerformance> departmentPerformance) { this.departmentPerformance = departmentPerformance; }

    public List<OfficerPerformance> getOfficerPerformance() { return officerPerformance; }
    public void setOfficerPerformance(List<OfficerPerformance> officerPerformance) { this.officerPerformance = officerPerformance; }

    public List<StatusBreakdown> getStatusBreakdown() { return statusBreakdown; }
    public void setStatusBreakdown(List<StatusBreakdown> statusBreakdown) { this.statusBreakdown = statusBreakdown; }

    public List<PriorityItem> getPriorityItems() { return priorityItems; }
    public void setPriorityItems(List<PriorityItem> priorityItems) { this.priorityItems = priorityItems; }

    public List<PriorityItem> getOldestPendingComplaints() { return oldestPendingComplaints; }
    public void setOldestPendingComplaints(List<PriorityItem> oldestPendingComplaints) { this.oldestPendingComplaints = oldestPendingComplaints; }

    public static class Summary {
        private boolean slaFilterApplied;
        private Integer selectedSlaDays;
        private long totalComplaints;
        private long openComplaints;
        private long pendingComplaints;
        private long resolvedComplaints;
        private long rejectedComplaints;
        private long escalatedComplaints;
        private long slaBreached;
        private long dueToday;
        private long geoTaggedComplaints;
        private double resolutionRate;
        private double slaBreachPercentage;
        private double geoTagCoverage;
        private double averageCitizenRating;
        private double averageResolutionDays;

        public boolean isSlaFilterApplied() { return slaFilterApplied; }
        public void setSlaFilterApplied(boolean slaFilterApplied) { this.slaFilterApplied = slaFilterApplied; }

        public Integer getSelectedSlaDays() { return selectedSlaDays; }
        public void setSelectedSlaDays(Integer selectedSlaDays) { this.selectedSlaDays = selectedSlaDays; }

        public long getTotalComplaints() { return totalComplaints; }
        public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }

        public long getOpenComplaints() { return openComplaints; }
        public void setOpenComplaints(long openComplaints) { this.openComplaints = openComplaints; }

        public long getPendingComplaints() { return pendingComplaints; }
        public void setPendingComplaints(long pendingComplaints) { this.pendingComplaints = pendingComplaints; }

        public long getResolvedComplaints() { return resolvedComplaints; }
        public void setResolvedComplaints(long resolvedComplaints) { this.resolvedComplaints = resolvedComplaints; }

        public long getRejectedComplaints() { return rejectedComplaints; }
        public void setRejectedComplaints(long rejectedComplaints) { this.rejectedComplaints = rejectedComplaints; }

        public long getEscalatedComplaints() { return escalatedComplaints; }
        public void setEscalatedComplaints(long escalatedComplaints) { this.escalatedComplaints = escalatedComplaints; }

        public long getSlaBreached() { return slaBreached; }
        public void setSlaBreached(long slaBreached) { this.slaBreached = slaBreached; }

        public long getDueToday() { return dueToday; }
        public void setDueToday(long dueToday) { this.dueToday = dueToday; }

        public long getGeoTaggedComplaints() { return geoTaggedComplaints; }
        public void setGeoTaggedComplaints(long geoTaggedComplaints) { this.geoTaggedComplaints = geoTaggedComplaints; }

        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }

        public double getSlaBreachPercentage() { return slaBreachPercentage; }
        public void setSlaBreachPercentage(double slaBreachPercentage) { this.slaBreachPercentage = slaBreachPercentage; }

        public double getGeoTagCoverage() { return geoTagCoverage; }
        public void setGeoTagCoverage(double geoTagCoverage) { this.geoTagCoverage = geoTagCoverage; }

        public double getAverageCitizenRating() { return averageCitizenRating; }
        public void setAverageCitizenRating(double averageCitizenRating) { this.averageCitizenRating = averageCitizenRating; }

        public double getAverageResolutionDays() { return averageResolutionDays; }
        public void setAverageResolutionDays(double averageResolutionDays) { this.averageResolutionDays = averageResolutionDays; }
    }

    public static class WardPerformance {
        private Integer wardNumber;
        private String wardName;
        private String zone;
        private long totalComplaints;
        private long openComplaints;
        private long resolvedComplaints;
        private long slaBreached;
        private double resolutionRate;
        private String topCategory;

        public Integer getWardNumber() { return wardNumber; }
        public void setWardNumber(Integer wardNumber) { this.wardNumber = wardNumber; }

        public String getWardName() { return wardName; }
        public void setWardName(String wardName) { this.wardName = wardName; }

        public String getZone() { return zone; }
        public void setZone(String zone) { this.zone = zone; }

        public long getTotalComplaints() { return totalComplaints; }
        public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }

        public long getOpenComplaints() { return openComplaints; }
        public void setOpenComplaints(long openComplaints) { this.openComplaints = openComplaints; }

        public long getResolvedComplaints() { return resolvedComplaints; }
        public void setResolvedComplaints(long resolvedComplaints) { this.resolvedComplaints = resolvedComplaints; }

        public long getSlaBreached() { return slaBreached; }
        public void setSlaBreached(long slaBreached) { this.slaBreached = slaBreached; }

        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }

        public String getTopCategory() { return topCategory; }
        public void setTopCategory(String topCategory) { this.topCategory = topCategory; }
    }

    public static class CategoryPerformance {
        private String category;
        private String label;
        private long totalComplaints;
        private long openComplaints;
        private long slaBreached;
        private double resolutionRate;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public long getTotalComplaints() { return totalComplaints; }
        public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }

        public long getOpenComplaints() { return openComplaints; }
        public void setOpenComplaints(long openComplaints) { this.openComplaints = openComplaints; }

        public long getSlaBreached() { return slaBreached; }
        public void setSlaBreached(long slaBreached) { this.slaBreached = slaBreached; }

        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
    }

    public static class DepartmentPerformance {
        private String department;
        private String label;
        private long totalComplaints;
        private long openComplaints;
        private long slaBreached;
        private double resolutionRate;

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public long getTotalComplaints() { return totalComplaints; }
        public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }

        public long getOpenComplaints() { return openComplaints; }
        public void setOpenComplaints(long openComplaints) { this.openComplaints = openComplaints; }

        public long getSlaBreached() { return slaBreached; }
        public void setSlaBreached(long slaBreached) { this.slaBreached = slaBreached; }

        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
    }

    public static class OfficerPerformance {
        private String officerId;
        private String label;
        private long totalComplaints;
        private long openComplaints;
        private long slaBreached;

        public String getOfficerId() { return officerId; }
        public void setOfficerId(String officerId) { this.officerId = officerId; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public long getTotalComplaints() { return totalComplaints; }
        public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }

        public long getOpenComplaints() { return openComplaints; }
        public void setOpenComplaints(long openComplaints) { this.openComplaints = openComplaints; }

        public long getSlaBreached() { return slaBreached; }
        public void setSlaBreached(long slaBreached) { this.slaBreached = slaBreached; }
    }

    public static class StatusBreakdown {
        private String status;
        private String label;
        private long count;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class PriorityItem {
        private String id;
        private String complaintNumber;
        private String subject;
        private String category;
        private String priority;
        private String status;
        private Integer wardNumber;
        private String wardName;
        private String location;
        private String slaDueAt;
        private boolean slaBreached;
        private long ageDays;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getComplaintNumber() { return complaintNumber; }
        public void setComplaintNumber(String complaintNumber) { this.complaintNumber = complaintNumber; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getWardNumber() { return wardNumber; }
        public void setWardNumber(Integer wardNumber) { this.wardNumber = wardNumber; }

        public String getWardName() { return wardName; }
        public void setWardName(String wardName) { this.wardName = wardName; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getSlaDueAt() { return slaDueAt; }
        public void setSlaDueAt(String slaDueAt) { this.slaDueAt = slaDueAt; }

        public boolean isSlaBreached() { return slaBreached; }
        public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }

        public long getAgeDays() { return ageDays; }
        public void setAgeDays(long ageDays) { this.ageDays = ageDays; }
    }
}
