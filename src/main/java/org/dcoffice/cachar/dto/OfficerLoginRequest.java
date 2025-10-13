package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

public class OfficerLoginRequest {
    @NotBlank
    private String employeeId;
    @NotBlank
    private String password;

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
