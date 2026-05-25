package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.WorkerLoginRequest;
import org.dcoffice.cachar.dto.WorkerSignupRequest;
import org.dcoffice.cachar.service.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@CrossOrigin(origins = "*")
public class WorkerAuthController {

    private static final Logger logger = LoggerFactory.getLogger(WorkerAuthController.class);

    @Autowired
    private WorkerService workerService;

    @PostMapping("/auth/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody WorkerLoginRequest request) {
        try {
            workerService.sendOtp(request.getMobile());
            Map<String, String> body = new HashMap<>();
            body.put("message", "OTP sent successfully");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            logger.error("Send OTP failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody(e.getMessage()));
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody WorkerLoginRequest request) {
        try {
            Map<String, Object> result = workerService.login(request.getMobile(), request.getOtp());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorBody(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorBody("User not found"));
        } catch (Exception e) {
            logger.error("Worker login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody(e.getMessage()));
        }
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@RequestBody WorkerSignupRequest request) {
        try {
            Map<String, Object> result = workerService.signup(
                    request.getMobile(), request.getName(), request.getAddress());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(errorBody(e.getMessage()));
        } catch (Exception e) {
            logger.error("Worker signup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody(e.getMessage()));
        }
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        return body;
    }
}
