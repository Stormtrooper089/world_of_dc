// OfficerService.java
package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.Officer;
import org.dcoffice.cachar.entity.OfficerRole;
import org.dcoffice.cachar.exception.OfficerNotFoundException;
import org.dcoffice.cachar.repository.OfficerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OfficerService {

    private static final Logger logger = LoggerFactory.getLogger(OfficerService.class);

    @Autowired
    private OfficerRepository officerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Officer createOfficer(Officer officer) {
        if (officerRepository.existsByEmployeeId(officer.getEmployeeId())) {
            throw new IllegalArgumentException("Officer with employee ID already exists: " + officer.getEmployeeId());
        }

        officer.setPassword(passwordEncoder.encode(officer.getPassword()));
        Officer savedOfficer = officerRepository.save(officer);
        logger.info("Created new officer: {} with employee ID: {}", officer.getName(), officer.getEmployeeId());
        return savedOfficer;
    }

    public Optional<Officer> findById(Long id) {
        return officerRepository.findById(String.valueOf(id));
    }

    public Officer getOfficerById(Long id) {
        return officerRepository.findById(String.valueOf(id))
                .orElseThrow(() -> new OfficerNotFoundException("Officer not found with ID: " + id));
    }

    public Optional<Officer> findByEmployeeId(String employeeId) {
        return officerRepository.findByEmployeeId(employeeId);
    }

    public Officer getOfficerByEmployeeId(String employeeId) {
        return officerRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new OfficerNotFoundException("Officer not found with employee ID: " + employeeId));
    }

    public List<Officer> findActiveOfficers() {
        return officerRepository.findByIsActiveTrue();
    }

    public List<Officer> findOfficersByRole(OfficerRole role) {
        return officerRepository.findByRoleAndIsActiveTrue(role);
    }

    public List<Officer> findOfficersByDepartment(String department) {
        return officerRepository.findByDepartmentAndIsActiveTrue(department);
    }

    public Officer updateOfficer(Officer officer) {
        Officer existingOfficer = getOfficerById(Long.valueOf(officer.getId()));

        existingOfficer.setName(officer.getName());
        existingOfficer.setEmail(officer.getEmail());
        existingOfficer.setMobileNumber(officer.getMobileNumber());
        existingOfficer.setDesignation(officer.getDesignation());
        existingOfficer.setDepartment(officer.getDepartment());
        existingOfficer.setRole(officer.getRole());

        return officerRepository.save(existingOfficer);
    }

    public void deactivateOfficer(Long officerId) {
        Officer officer = getOfficerById(officerId);
        officer.setActive(false);
        officerRepository.save(officer);
        logger.info("Deactivated officer: {} with ID: {}", officer.getName(), officerId);
    }

    public void activateOfficer(Long officerId) {
        Officer officer = getOfficerById(officerId);
        officer.setActive(true);
        officerRepository.save(officer);
        logger.info("Activated officer: {} with ID: {}", officer.getName(), officerId);
    }

    public boolean validatePassword(String employeeId, String rawPassword) {
        Optional<Officer> officerOpt = findByEmployeeId(employeeId);
        if (officerOpt.isPresent()) {
            return passwordEncoder.matches(rawPassword, officerOpt.get().getPassword());
        }
        return false;
    }
}