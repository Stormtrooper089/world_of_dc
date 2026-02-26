package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "distance_matrix_cache")
public class DistanceMatrixCache {

    @Id
    private String id;

    /** SHA-256 hash of sorted lat,lon list */
    private String cacheKey;

    /** "PS 1 → PS 2" -> distance in KM */
    private Map<String, Double> distanceMatrix;

    private int stationCount;

    private Instant createdAt;

    public String getId() {
        return id;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public Map<String, Double> getDistanceMatrix() {
        return distanceMatrix;
    }

    public void setDistanceMatrix(Map<String, Double> distanceMatrix) {
        this.distanceMatrix = distanceMatrix;
    }

    public int getStationCount() {
        return stationCount;
    }

    public void setStationCount(int stationCount) {
        this.stationCount = stationCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
