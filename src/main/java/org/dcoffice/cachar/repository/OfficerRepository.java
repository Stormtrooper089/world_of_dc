package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.Officer;
import org.dcoffice.cachar.entity.OfficerRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficerRepository extends MongoRepository<Officer, String> {

    Optional<Officer> findByEmployeeId(String employeeId);

    @Query("{ 'role': ?0, 'isActive': true }")
    List<Officer> findByRoleAndIsActiveTrue(OfficerRole role);

    List<Officer> findByIsActiveTrue();

    boolean existsByEmployeeId(String employeeId);

    @Query("{ 'department': ?0, 'isActive': true }")
    List<Officer> findByDepartmentAndIsActiveTrue(String department);

    List<Officer> findByRole(OfficerRole role);

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'isActive': true }")
    List<Officer> findActiveOfficersByNameContaining(String name);
}
