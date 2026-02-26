package org.dcoffice.cachar.service;

import com.uber.h3core.H3Core;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dcoffice.cachar.dto.ClusterResultDTO;
import org.dcoffice.cachar.entity.PollingStation;
import org.dcoffice.cachar.repository.PollingStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PollingStationService {

    private static final Logger logger =
            LoggerFactory.getLogger(PollingStationService.class);

    private final PollingStationRepository repository;
    private final RoadDistanceService roadDistanceService;
    private final H3Core h3;

    // 🚌 ISBT DEPOT
    public static final String ISBT_NAME = "ISBT";
    public static final double ISBT_LAT = 24.83;
    public static final double ISBT_LON = 92.75;

    public PollingStationService(PollingStationRepository repository,
                                 RoadDistanceService roadDistanceService)
            throws IOException {
        this.repository = repository;
        this.roadDistanceService = roadDistanceService;
        this.h3 = H3Core.newInstance();
    }

    /* =====================================================
       ✅ SAFE EXCEL INGESTION (FIXED)
       ===================================================== */

    public void processExcelFile(String filePath, int resolution)
            throws IOException {

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<PollingStation> stations = new ArrayList<>();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                PollingStation ps = new PollingStation();

                ps.setSl((int) row.getCell(0).getNumericCellValue());
                ps.setLacNo((int) row.getCell(1).getNumericCellValue());
                ps.setLacName(getCellAsString(row.getCell(2)));
                ps.setPsNo((int) row.getCell(3).getNumericCellValue());
                ps.setStationName(getCellAsString(row.getCell(4)));
                ps.setBloName(getCellAsString(row.getCell(5)));
                ps.setMobile(getCellAsString(row.getCell(6)));
                ps.setLatitude(row.getCell(7).getNumericCellValue());
                ps.setLongitude(row.getCell(8).getNumericCellValue());

                long h3Index =
                        h3.latLngToCell(
                                ps.getLatitude(),
                                ps.getLongitude(),
                                resolution);

                ps.setH3Index(h3.h3ToString(h3Index));
                stations.add(ps);
            }

            repository.saveAll(stations);
            logger.info("Uploaded {} polling stations", stations.size());
        }
    }

    /* =====================================================
       READ APIs
       ===================================================== */

    public List<PollingStation> getStationsByLac(int lacNo) {
        return repository.findByLacNo(lacNo);
    }

    public List<PollingStation> getStationsByHex(String h3Index) {
        return repository.findByH3Index(h3Index);
    }

    public Map<Integer, Long> getStationCountByLac() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        PollingStation::getLacNo,
                        Collectors.counting()));
    }

    public Map<String, Long> getStationCountByHex() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        PollingStation::getH3Index,
                        Collectors.counting()));
    }

    /* =====================================================
       CLUSTER + ROUTING
       ===================================================== */

    public List<ClusterResultDTO> clusterPollingStations(
            Integer lacNo,
            int clusterSize) {

        List<PollingStation> all =
                lacNo == null
                        ? repository.findAll()
                        : repository.findByLacNo(lacNo);

        List<List<PollingStation>> clusters = new ArrayList<>();
        List<PollingStation> buffer = new ArrayList<>();

        for (PollingStation ps : all) {
            buffer.add(ps);
            if (buffer.size() == clusterSize) {
                clusters.add(new ArrayList<>(buffer));
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) clusters.add(buffer);

        List<ClusterResultDTO> result = new ArrayList<>();
        int clusterId = 1;

        for (List<PollingStation> group : clusters) {

            PollingStation centroid = findCentroid(group);

            double[][] matrix =
                    roadDistanceService.getDistanceMatrixSafe(group);

            Map<String, Double> distanceMap =
                    buildDistanceMap(group, matrix);

            List<PollingStation> route =
                    buildRoute(group, matrix, centroid);

            double isbtKm = haversineKm(
                    ISBT_LAT, ISBT_LON,
                    route.get(0).getLatitude(),
                    route.get(0).getLongitude());

            double routeKm =
                    computeRouteDistance(route, distanceMap);

            ClusterResultDTO dto = new ClusterResultDTO();
            dto.setClusterId(clusterId++);
            dto.setCentroid(centroid);
            dto.setStations(group);
            dto.setRouteOrder(route);
            dto.setDistanceMatrix(distanceMap);

            dto.setIsbtToFirstStationKm(round(isbtKm, 2));
            dto.setTotalRouteDistanceKm(
                    round(isbtKm + routeKm, 2));
            dto.setEstimatedTravelTimeHours(
                    round((isbtKm + routeKm) / 25.0, 2));

            result.add(dto);
        }
        return result;
    }

    /* =====================================================
       HELPERS
       ===================================================== */

    private String getCellAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCachedFormulaResultType() == CellType.NUMERIC
                        ? String.valueOf((long) cell.getNumericCellValue())
                        : cell.getStringCellValue();
            default:
                return "";
        }
    }

    private Map<String, Double> buildDistanceMap(
            List<PollingStation> stations,
            double[][] matrix) {

        Map<String, Double> map = new LinkedHashMap<>();

        for (PollingStation ps : stations) {
            double km = haversineKm(
                    ISBT_LAT, ISBT_LON,
                    ps.getLatitude(), ps.getLongitude());
            map.put("ISBT → PS " + ps.getPsNo(), round(km, 2));
        }

        for (int i = 0; i < stations.size(); i++) {
            for (int j = 0; j < stations.size(); j++) {
                if (i == j) continue;
                map.put(
                        "PS " + stations.get(i).getPsNo()
                                + " → PS " + stations.get(j).getPsNo(),
                        round(matrix[i][j], 2));
            }
        }
        return map;
    }

    private double computeRouteDistance(
            List<PollingStation> route,
            Map<String, Double> map) {

        double total = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            String key =
                    "PS " + route.get(i).getPsNo()
                            + " → PS " + route.get(i + 1).getPsNo();
            total += map.getOrDefault(key, 0.0);
        }
        return total;
    }

    private PollingStation findCentroid(List<PollingStation> g) {
        double lat = 0, lon = 0;
        for (PollingStation p : g) {
            lat += p.getLatitude();
            lon += p.getLongitude();
        }
        PollingStation c = new PollingStation();
        c.setLatitude(lat / g.size());
        c.setLongitude(lon / g.size());
        c.setStationName("CLUSTER CENTROID");
        return c;
    }

    private List<PollingStation> buildRoute(
            List<PollingStation> s,
            double[][] m,
            PollingStation c) {

        boolean[] v = new boolean[s.size()];
        List<PollingStation> r = new ArrayList<>();

        int cur = 0;
        v[cur] = true;
        r.add(s.get(cur));

        for (int k = 1; k < s.size(); k++) {
            int next = -1;
            double best = Double.MAX_VALUE;
            for (int j = 0; j < s.size(); j++) {
                if (!v[j] && m[cur][j] < best) {
                    best = m[cur][j];
                    next = j;
                }
            }
            if (next == -1) break;
            v[next] = true;
            r.add(s.get(next));
            cur = next;
        }
        return r;
    }

    private double haversineKm(double a, double b, double c, double d) {
        double R = 6371;
        double dLat = Math.toRadians(c - a);
        double dLon = Math.toRadians(d - b);
        double x =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(a)) *
                                Math.cos(Math.toRadians(c)) *
                                Math.sin(dLon / 2) *
                                Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.sqrt(x));
    }

    private double round(double v, int p) {
        double s = Math.pow(10, p);
        return Math.round(v * s) / s;
    }
}
