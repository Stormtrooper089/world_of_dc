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
        officer.setApproved(true); // Auto-approve officers for testing
        Officer savedOfficer = officerRepository.save(officer);
        logger.info("Created new officer: {} with employee ID: {}", officer.getName(), officer.getEmployeeId());
        return savedOfficer;
    }

    /**
     * Signup a new officer. Officer will be created with isApproved=true for testing.
     */
    public Officer signupOfficer(Officer officer) {
        officer.setApproved(false);
        officer.setActive(true);
        return createOfficer(officer);
    }

    /**
     * Approve an officer (admin action)
     */
    public Officer approveOfficer(String officerId, String approverEmployeeId, org.dcoffice.cachar.entity.OfficerRole assignedRole) {
        Officer officer = getOfficerById(officerId);
        officer.setApproved(true);
        if (assignedRole != null) officer.setRole(assignedRole);
        Officer saved = officerRepository.save(officer);
        logger.info("Officer {} approved by {}", officer.getEmployeeId(), approverEmployeeId);
        return saved;
    }

    /**
     * Authenticate officer and ensure they are approved before allowing login
     */
    public Officer authenticateOfficer(String employeeId, String rawPassword) {
        Officer officer = getOfficerByEmployeeId(employeeId);
        if (!officer.isApproved()) {
            throw new IllegalStateException("Officer not approved by admin");
        }

        if (!passwordEncoder.matches(rawPassword, officer.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return officer;
    }

    public Optional<Officer> findById(String id) {
        return officerRepository.findById(id);
    }

    public Officer getOfficerById(String id) {
        return officerRepository.findById(id)
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

    /**
     * Search active officers by name (case-insensitive partial match)
     */
    public List<Officer> searchOfficersByName(String nameQuery) {
        if (nameQuery == null || nameQuery.trim().isEmpty()) {
            return officerRepository.findByIsActiveTrue();
        }
        return officerRepository.findActiveOfficersByNameContaining(nameQuery.trim());
    }

    public List<Officer> findPendingApprovals() {
        return officerRepository.findByIsApprovedFalseAndIsActiveTrue();
    }

    public void rejectOfficer(String officerId, String approverEmployeeId) {
        Officer officer = getOfficerById(officerId);
        officer.setActive(false);
        officerRepository.save(officer);
        logger.info("Officer {} rejected by {}", officer.getEmployeeId(), approverEmployeeId);
    }

    public List<Officer> findOfficersByRole(OfficerRole role) {
        return officerRepository.findByRoleAndIsActiveTrue(role);
    }

    public List<Officer> findOfficersByDepartment(String department) {
        return officerRepository.findByDepartmentAndIsActiveTrue(department);
    }

    public Officer updateOfficer(Officer officer) {
        Officer existingOfficer = getOfficerById(officer.getId());

        existingOfficer.setName(officer.getName());
        existingOfficer.setEmail(officer.getEmail());
        existingOfficer.setMobileNumber(officer.getMobileNumber());
        existingOfficer.setDesignation(officer.getDesignation());
        existingOfficer.setDepartment(officer.getDepartment());
        existingOfficer.setRole(officer.getRole());

        return officerRepository.save(existingOfficer);
    }

    public void deactivateOfficer(String officerId) {
        Officer officer = getOfficerById(officerId);
        officer.setActive(false);
        officerRepository.save(officer);
        logger.info("Deactivated officer: {} with ID: {}", officer.getName(), officerId);
    }

    public void activateOfficer(String officerId) {
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

    /**
     * Get the default officer for citizen complaints
     */
    public Officer getDefaultOfficerOrNull() throws OfficerNotFoundException {
        Optional<Officer> defaultOfficer = officerRepository.findDefaultOfficer();
        if (defaultOfficer.isPresent()) {
            return defaultOfficer.get();
        }
      
        throw new OfficerNotFoundException("No default officer found. Please set a default officer.");
    }

}