package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}