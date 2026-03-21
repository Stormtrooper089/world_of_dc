package org.dcoffice.cachar.entity;

import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.annotation.Id;
import lombok.Data;

@Data
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
}