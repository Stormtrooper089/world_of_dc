package org.dcoffice.cachar.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.Officer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@Service
public class JwtService {

    @Value("${jwt.secret:change-me-very-secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:3600000}") // 1 hour default
    private long jwtExpirationMs;

    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
    }

    /**
     * Convenience method to generate the standard token for a Citizen.
     * Subject will be the citizen's id.
     */
    public String generateTokenForCitizen(Citizen citizen) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("role", "CITIZEN");
        claims.put("mobileNumber", citizen.getMobileNumber());
        return generateToken(citizen.getId(), claims);
    }

    public String generateTokenForOfficer(Officer officer) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("role", officer.getRole() != null ? officer.getRole().name() : "OFFICER");
        claims.put("employeeId", officer.getEmployeeId());
        return generateToken(officer.getId(), claims);
    }

    public Claims parseToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(token);
        return jws.getBody();
    }
}
