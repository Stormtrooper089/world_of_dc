package org.dcoffice.cachar.dto;

public class WorkerLoginRequest {
    private String mobile;
    private String otp;

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
