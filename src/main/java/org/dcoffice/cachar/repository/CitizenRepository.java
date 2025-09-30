package org.dcoffice.cachar.repository;


import org.dcoffice.cachar.entity.Citizen;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitizenRepository extends MongoRepository<Citizen, String> {

    Optional<Citizen> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);

    @Query(value = "{ 'isVerified': true }", count = true)
    Long countVerifiedCitizens();

    @Query("{ 'otp': { $ne: null }, 'otpExpiry': { $gt: ?0 } }")
    List<Citizen> findCitizensWithPendingOTP(LocalDateTime currentTime);

    List<Citizen> findByIsVerified(boolean isVerified);

    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<Citizen> findByNameContainingIgnoreCase(String name);

    @Query("{ 'email': { $regex: ?0$ } }")
    List<Citizen> findByEmailDomain(String domain);

    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 } }")
    List<Citizen> findByRegistrationDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    Long countByIsVerified(boolean isVerified);
}
