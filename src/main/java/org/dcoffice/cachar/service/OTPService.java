package org.dcoffice.cachar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class OTPService {

    private static final Logger logger = LoggerFactory.getLogger(OTPService.class);
    private final Random random = new Random();

    public String generateOTP() {
        return("111111");// for now till otp service is
        //return String.format("%06d", random.nextInt(1000000));
    }

    public void sendOTP(String mobileNumber, String otp) {
        // In production, integrate with SMS gateway like TextLocal, MSG91, AWS SNS, etc.
        logger.info("Sending OTP {} to mobile number: {}", otp, mobileNumber);

        // Example integration with SMS gateway:
        /*
        try {
            String message = String.format(
                "Your OTP for Cachar District Complaint Registration is: %s. Valid for 5 minutes. Do not share with anyone.",
                otp
            );

            // Call SMS gateway API
            // smsGatewayClient.sendSMS(mobileNumber, message);

        } catch (Exception e) {
            logger.error("Failed to send OTP to {}: {}", mobileNumber, e.getMessage());
            throw new RuntimeException("Failed to send OTP", e);
        }
        */
    }

    public boolean isValidOTPFormat(String otp) {
        return otp != null && otp.matches("\\d{6}");
    }
}
