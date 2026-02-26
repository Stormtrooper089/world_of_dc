package org.dcoffice.cachar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dcoffice.cachar.entity.PollingStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class RoadDistanceService {

    private static final Logger logger =
            LoggerFactory.getLogger(RoadDistanceService.class);

    @Value("${ors.api.key:}")
    private String orsApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ORS_MATRIX_URL =
            "https://api.openrouteservice.org/v2/matrix/driving-car";

    /* =========================================================
       PUBLIC SAFE METHOD
       ========================================================= */

    public double[][] getDistanceMatrixSafe(List<PollingStation> stations) {

        if (stations == null || stations.isEmpty()) {
            logger.warn("Distance matrix requested for empty station list.");
            return new double[0][0];
        }

        try {
            if (orsApiKey == null || orsApiKey.isBlank()) {
                logger.warn("ORS API key not configured. Using Haversine fallback.");
                return computeHaversineMatrix(stations);
            }

            logger.info("Requesting ORS matrix for {} stations",
                    stations.size());

            return fetchFromORS(stations);

        } catch (HttpStatusCodeException ex) {

            logger.error("ORS HTTP error: {} | Response: {}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());

            logger.warn("Falling back to Haversine due to ORS HTTP error.");
            return computeHaversineMatrix(stations);

        } catch (Exception e) {

            logger.error("ORS failed: {}", e.getMessage(), e);
            logger.warn("Using Haversine fallback matrix.");

            return computeHaversineMatrix(stations);
        }
    }

    /* =========================================================
       ORS MATRIX CALL
       ========================================================= */

    private double[][] fetchFromORS(List<PollingStation> stations)
            throws Exception {

        List<List<Double>> locations = new ArrayList<>();

        for (PollingStation ps : stations) {
            locations.add(Arrays.asList(
                    ps.getLongitude(),  // IMPORTANT
                    ps.getLatitude()
            ));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("locations", locations);
        body.put("metrics", Collections.singletonList("distance"));
        body.put("units", "km");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", orsApiKey);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        ORS_MATRIX_URL,
                        entity,
                        String.class);

        JsonNode root =
                objectMapper.readTree(response.getBody());

        JsonNode distancesNode = root.get("distances");

        int size = stations.size();
        double[][] matrix = new double[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] =
                        distancesNode.get(i).get(j).asDouble();
            }
        }

        logger.info("ORS matrix fetched successfully.");
        return matrix;
    }

    /* =========================================================
       HAVERSINE FALLBACK
       ========================================================= */

    private double[][] computeHaversineMatrix(
            List<PollingStation> stations) {

        int n = stations.size();
        double[][] matrix = new double[n][n];

        logger.info("Computing fallback Haversine matrix.");

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {

                if (i == j) {
                    matrix[i][j] = 0;
                    continue;
                }

                matrix[i][j] =
                        haversineKm(
                                stations.get(i).getLatitude(),
                                stations.get(i).getLongitude(),
                                stations.get(j).getLatitude(),
                                stations.get(j).getLongitude()
                        );
            }
        }

        return matrix;
    }

    private double haversineKm(
            double lat1, double lon1,
            double lat2, double lon2) {

        final double R = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        return 2 * R * Math.asin(Math.sqrt(a));
    }
}
