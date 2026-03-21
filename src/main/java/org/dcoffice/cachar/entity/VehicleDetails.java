package org.dcoffice.cachar.entity;

import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.annotation.Id;

@Document(collection = "vehicle_details")
public class VehicleDetails {

    @Id
    private String id;

    private String acNo;
    private String psNo;
    private String psName;

    private String vehicleNo;
    private String driverName;
    private String driverMobile;
    private String vehicleType;
    private Integer capacity;

    private String route;
    private String remarks;

    // 📍 Parking Location (Geo)
    @GeoSpatialIndexed
    private GeoJsonPoint location;

    // 🅿️ Parking Address (human readable)
    private String parkingAddress;

    // 📝 Live status
    private String statusComment;

    private Long uploadTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAcNo() { return acNo; }
    public void setAcNo(String acNo) { this.acNo = acNo; }

    public String getPsNo() { return psNo; }
    public void setPsNo(String psNo) { this.psNo = psNo; }

    public String getPsName() { return psName; }
    public void setPsName(String psName) { this.psName = psName; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverMobile() { return driverMobile; }
    public void setDriverMobile(String driverMobile) { this.driverMobile = driverMobile; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public GeoJsonPoint getLocation() { return location; }
    public void setLocation(GeoJsonPoint location) { this.location = location; }

    public String getParkingAddress() { return parkingAddress; }
    public void setParkingAddress(String parkingAddress) { this.parkingAddress = parkingAddress; }

    public String getStatusComment() { return statusComment; }
    public void setStatusComment(String statusComment) { this.statusComment = statusComment; }

    public Long getUploadTime() { return uploadTime; }
    public void setUploadTime(Long uploadTime) { this.uploadTime = uploadTime; }
}