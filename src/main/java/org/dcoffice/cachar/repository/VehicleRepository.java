package org.dcoffice.cachar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.dcoffice.cachar.entity.VehicleDetails;

public interface VehicleRepository extends MongoRepository<VehicleDetails, String> {

    List<VehicleDetails> findByAcNoAndPsNo(String acNo, String psNo);

    List<VehicleDetails> findByPsName(String psName);

    Optional<VehicleDetails> findByVehicleNo(String vehicleNo);

    void deleteByPsName(String psName);

    void deleteByVehicleNo(String vehicleNo);

    @Query(value = "{}", fields = "{ 'vehicleNo': 1, '_id': 0 }")
    List<VehicleDetails> findAllVehicleNos();

}