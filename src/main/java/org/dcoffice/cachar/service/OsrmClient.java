package org.dcoffice.cachar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dcoffice.cachar.entity.PollingStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class OsrmClient {

    private static final Logger logger =
            LoggerFactory.getLogger(OsrmClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Double> fetchDistanceMapSafe(
            List<PollingStation> stations) {

        if (stations.size() < 2) {
            return Collections.emptyMap();
        }

        try {
            logger.info("Calling OSRM TABLE API for {} stations",
                    stations.size());
            return callOsrm(stations);
        } catch (Exception ex) {
            logger.error("OSRM failed: {}", ex.getMessage());
            return fallbackZeroMatrix(stations);
        }
    }

    private Map<String, Double> callOsrm(
            List<PollingStation> stations) throws Exception {

        String coordinates = stations.stream()
                .map(ps -> ps.getLongitude() + "," + ps.getLatitude())
                .reduce((a, b) -> a + ";" + b)
                .orElseThrow();

        String url =
                "https://router.project-osrm.org/table/v1/driving/"
                        + coordinates + "?annotations=distance";

        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(response);
        JsonNode distances = root.path("distances");

        Map<String, Double> map = new LinkedHashMap<>();

        for (int i = 0; i < stations.size(); i++) {
            for (int j = 0; j < stations.size(); j++) {
                if (i == j) continue;

                double km =
                        distances.get(i).get(j).asDouble() / 1000.0;

                map.put(
                        "PS " + stations.get(i).getPsNo()
                                + " → PS " + stations.get(j).getPsNo(),
                        round(km, 2)
                );
            }
        }
        return map;
    }

    private Map<String, Double> fallbackZeroMatrix(
            List<PollingStation> stations) {

        Map<String, Double> map = new LinkedHashMap<>();

        for (PollingStation a : stations) {
            for (PollingStation b : stations) {
                if (a == b) continue;
                map.put(
                        "PS " + a.getPsNo() + " → PS " + b.getPsNo(),
                        0.0
                );
            }
        }
        return map;
    }

    private double round(double v, int p) {
        double s = Math.pow(10, p);
        return Math.round(v * s) / s;
    }
}
