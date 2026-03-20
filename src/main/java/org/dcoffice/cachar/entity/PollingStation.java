package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "pollingStations")
public class PollingStation {

    @Id
    private String id;              // MongoDB ObjectId

    private int sl;                 // Serial number

    @Indexed
    private int lacNo;              // LAC number

    private String lacName;         // LAC name
    private int psNo;               // Polling Station number
    private String stationName;     // Name of Polling Station
    private String bloName;         // Booth Level Officer name
    private String mobile;          // BLO mobile number
    private double latitude;        // Latitude
    private double longitude;       // Longitude

    @Indexed
    private String h3Index;         // H3 hex index for clustering

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSl() {
        return sl;
    }

    public void setSl(int sl) {
        this.sl = sl;
    }

    public int getLacNo() {
        return lacNo;
    }

    public void setLacNo(int lacNo) {
        this.lacNo = lacNo;
    }

    public String getLacName() {
        return lacName;
    }

    public void setLacName(String lacName) {
        this.lacName = lacName;
    }

    public int getPsNo() {
        return psNo;
    }

    public void setPsNo(int psNo) {
        this.psNo = psNo;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getBloName() {
        return bloName;
    }

    public void setBloName(String bloName) {
        this.bloName = bloName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getH3Index() {
        return h3Index;
    }

    public void setH3Index(String h3Index) {
        this.h3Index = h3Index;
    }
}