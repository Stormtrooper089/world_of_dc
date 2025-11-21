package org.dcoffice.cachar.dto;

/**
 * DTO for portal statistics displayed on citizen home page
 */
public class PortalStatisticsDto {
    private long grievancesFiled;
    private long resolved;
    private String avgResolutionTime; // e.g., "5.2 Days"
    private String satisfactionRate; // e.g., "92%"

    public PortalStatisticsDto() {
    }

    public PortalStatisticsDto(long grievancesFiled, long resolved, String avgResolutionTime, String satisfactionRate) {
        this.grievancesFiled = grievancesFiled;
        this.resolved = resolved;
        this.avgResolutionTime = avgResolutionTime;
        this.satisfactionRate = satisfactionRate;
    }

    public long getGrievancesFiled() {
        return grievancesFiled;
    }

    public void setGrievancesFiled(long grievancesFiled) {
        this.grievancesFiled = grievancesFiled;
    }

    public long getResolved() {
        return resolved;
    }

    public void setResolved(long resolved) {
        this.resolved = resolved;
    }

    public String getAvgResolutionTime() {
        return avgResolutionTime;
    }

    public void setAvgResolutionTime(String avgResolutionTime) {
        this.avgResolutionTime = avgResolutionTime;
    }

    public String getSatisfactionRate() {
        return satisfactionRate;
    }

    public void setSatisfactionRate(String satisfactionRate) {
        this.satisfactionRate = satisfactionRate;
    }
}

