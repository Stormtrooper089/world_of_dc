package org.dcoffice.cachar.dto;

import org.dcoffice.cachar.entity.PollingStation;

import java.util.List;
import java.util.Map;

public class ClusterResultDTO {

    // -------------------------
    // Core Identity
    // -------------------------
    private int clusterId;

    // -------------------------
    // Spatial Objects
    // -------------------------
    private PollingStation centroid;
    private List<PollingStation> stations;
    private List<PollingStation> routeOrder;

    // -------------------------
    // Distance & Routing
    // -------------------------
    /**
     * Key format:
     *  - "ISBT → PS X"
     *  - "PS X → PS Y"
     */
    private Map<String, Double> distanceMatrix;

    // -------------------------
    // Calculated Metrics
    // -------------------------
    private double isbtToFirstStationKm;
    private double totalRouteDistanceKm;
    private double estimatedTravelTimeHours;
    private double avgInterStationDistanceKm;
    private double maxInterStationDistanceKm;
    private int sameLocationCount;

    // =========================
    // Getters & Setters
    // =========================

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public PollingStation getCentroid() {
        return centroid;
    }

    public void setCentroid(PollingStation centroid) {
        this.centroid = centroid;
    }

    public List<PollingStation> getStations() {
        return stations;
    }

    public void setStations(List<PollingStation> stations) {
        this.stations = stations;
    }

    public List<PollingStation> getRouteOrder() {
        return routeOrder;
    }

    public void setRouteOrder(List<PollingStation> routeOrder) {
        this.routeOrder = routeOrder;
    }

    public Map<String, Double> getDistanceMatrix() {
        return distanceMatrix;
    }

    public void setDistanceMatrix(Map<String, Double> distanceMatrix) {
        this.distanceMatrix = distanceMatrix;
    }

    public double getIsbtToFirstStationKm() {
        return isbtToFirstStationKm;
    }

    public void setIsbtToFirstStationKm(double isbtToFirstStationKm) {
        this.isbtToFirstStationKm = isbtToFirstStationKm;
    }

    public double getTotalRouteDistanceKm() {
        return totalRouteDistanceKm;
    }

    public void setTotalRouteDistanceKm(double totalRouteDistanceKm) {
        this.totalRouteDistanceKm = totalRouteDistanceKm;
    }

    public double getEstimatedTravelTimeHours() {
        return estimatedTravelTimeHours;
    }

    public void setEstimatedTravelTimeHours(double estimatedTravelTimeHours) {
        this.estimatedTravelTimeHours = estimatedTravelTimeHours;
    }

    public double getAvgInterStationDistanceKm() {
        return avgInterStationDistanceKm;
    }

    public void setAvgInterStationDistanceKm(double avgInterStationDistanceKm) {
        this.avgInterStationDistanceKm = avgInterStationDistanceKm;
    }

    public double getMaxInterStationDistanceKm() {
        return maxInterStationDistanceKm;
    }

    public void setMaxInterStationDistanceKm(double maxInterStationDistanceKm) {
        this.maxInterStationDistanceKm = maxInterStationDistanceKm;
    }

    public int getSameLocationCount() {
        return sameLocationCount;
    }

    public void setSameLocationCount(int sameLocationCount) {
        this.sameLocationCount = sameLocationCount;
    }
}
