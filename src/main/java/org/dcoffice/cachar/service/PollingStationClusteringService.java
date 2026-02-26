package org.dcoffice.cachar.service;

import com.uber.h3core.H3Core;
import org.dcoffice.cachar.dto.PollingStationCluster;
import org.dcoffice.cachar.entity.PollingStation;
import org.dcoffice.cachar.repository.PollingStationRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PollingStationClusteringService {

    private final PollingStationRepository repository;
    private final H3Core h3;

    public PollingStationClusteringService(PollingStationRepository repository) throws IOException {
        this.repository = repository;
        this.h3 = H3Core.newInstance();
    }

    public List<PollingStationCluster> createClusters(
            int lacNo,
            int clusterSize,
            int kRing
    ) {

        List<PollingStation> stations =
                repository.findByLacNo(lacNo);

        Map<String, List<PollingStation>> byH3 =
                stations.stream()
                        .collect(Collectors.groupingBy(PollingStation::getH3Index));

        Set<String> usedStations = new HashSet<>();
        List<PollingStationCluster> clusters = new ArrayList<>();

        int clusterSeq = 1;

        for (PollingStation anchor : stations) {

            if (usedStations.contains(anchor.getId())) {
                continue;
            }

            List<PollingStation> clusterMembers = new ArrayList<>();
            String anchorH3 = anchor.getH3Index();

            // Expand using H3 neighbors
            List<String> neighbors = h3.gridDisk(anchorH3, kRing);

            for (String h3Cell : neighbors) {
                List<PollingStation> cellStations =
                        byH3.getOrDefault(h3Cell, Collections.emptyList());

                for (PollingStation ps : cellStations) {
                    if (usedStations.contains(ps.getId())) continue;

                    clusterMembers.add(ps);
                    usedStations.add(ps.getId());

                    if (clusterMembers.size() >= clusterSize) {
                        break;
                    }
                }
                if (clusterMembers.size() >= clusterSize) {
                    break;
                }
            }

            if (!clusterMembers.isEmpty()) {
                PollingStationCluster cluster = new PollingStationCluster();
                cluster.setClusterId(
                        "L" + lacNo + "-C" + String.format("%03d", clusterSeq++)
                );
                cluster.setLacNo(lacNo);
                cluster.setAnchorH3(anchorH3);
                cluster.setStations(clusterMembers);

                clusters.add(cluster);
            }
        }

        return clusters;
    }
}
