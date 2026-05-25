package org.dcoffice.cachar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkerUserDto {
    private String id;
    private String mobile;
    private String name;
    private String address;
    private String createdAt;
    private boolean admin;
    private String squadId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @JsonProperty("isAdmin")
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public String getSquadId() { return squadId; }
    public void setSquadId(String squadId) { this.squadId = squadId; }
}
