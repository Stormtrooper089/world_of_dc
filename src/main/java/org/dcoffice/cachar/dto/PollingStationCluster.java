package org.dcoffice.cachar.dto;

import org.dcoffice.cachar.entity.PollingStation;

import java.util.List;

public class PollingStationCluster {

    private String clusterId;
    private int lacNo;
    private String anchorH3;
    private List<PollingStation> stations;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public int getLacNo() {
        return lacNo;
    }

    public void setLacNo(int lacNo) {
        this.lacNo = lacNo;
    }

    public String getAnchorH3() {
        return anchorH3;
    }

    public void setAnchorH3(String anchorH3) {
        this.anchorH3 = anchorH3;
    }

    public List<PollingStation> getStations() {
        return stations;
    }

    public void setStations(List<PollingStation> stations) {
        this.stations = stations;
    }
}
