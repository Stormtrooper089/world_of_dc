// OfficerService.java
package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.Officer;
import org.dcoffice.cachar.entity.OfficerRole;
import org.dcoffice.cachar.entity.EmployeeCategory;
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

        officer.setEmployeeCategory(resolveEmployeeCategory(officer));
        officer.setPassword(passwordEncoder.encode(officer.getPassword()));
        // Don't override the approved status - preserve what was set by the caller
        // For direct creation, set approved=true before calling this method if needed
        Officer savedOfficer = officerRepository.save(officer);
        logger.info("Created new officer: {} with employee ID: {}, approved: {}", 
            officer.getName(), officer.getEmployeeId(), officer.isApproved());
        return savedOfficer;
    }

    /**
     * Signup a new officer. Officer will be created with isApproved=false, requiring admin approval.
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
        List<Officer> officers = officerRepository.findByIsActiveTrue();
        officers.forEach(this::ensureEmployeeCategory);
        return officers;
    }

    public List<Officer> findActiveOfficers(EmployeeCategory employeeCategory) {
        if (employeeCategory == null) {
            return findActiveOfficers();
        }
        return findActiveOfficers().stream()
                .filter(officer -> employeeCategory.equals(resolveEmployeeCategory(officer)))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Search active officers by name (case-insensitive partial match)
     */
    public List<Officer> searchOfficersByName(String nameQuery) {
        if (nameQuery == null || nameQuery.trim().isEmpty()) {
            return officerRepository.findByIsActiveTrue();
        }
        List<Officer> officers = officerRepository.findActiveOfficersByNameContaining(nameQuery.trim());
        officers.forEach(this::ensureEmployeeCategory);
        return officers;
    }

    public List<Officer> searchOfficersByName(String nameQuery, EmployeeCategory employeeCategory) {
        return searchOfficersByName(nameQuery).stream()
                .filter(officer -> employeeCategory == null || employeeCategory.equals(resolveEmployeeCategory(officer)))
                .collect(java.util.stream.Collectors.toList());
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
        existingOfficer.setEmployeeCategory(resolveEmployeeCategory(officer));
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

    private void ensureEmployeeCategory(Officer officer) {
        if (officer.getEmployeeCategory() == null) {
            officer.setEmployeeCategory(resolveEmployeeCategory(officer));
        }
    }

    private EmployeeCategory resolveEmployeeCategory(Officer officer) {
        if (officer.getEmployeeCategory() != null && officer.getEmployeeCategory() != EmployeeCategory.OTHER) {
            return officer.getEmployeeCategory();
        }
        String text = ((officer.getDepartment() == null ? "" : officer.getDepartment()) + " "
                + (officer.getDesignation() == null ? "" : officer.getDesignation())).toLowerCase();
        if (text.contains("sanitation") || text.contains("waste") || text.contains("clean")) {
            return EmployeeCategory.SANITATION;
        }
        if (text.contains("finance") || text.contains("account") || text.contains("tax")) {
            return EmployeeCategory.FINANCE;
        }
        if (text.contains("driver") || text.contains("vehicle")) {
            return EmployeeCategory.DRIVER;
        }
        if (text.contains("engineer") || text.contains("works") || text.contains("pwd")) {
            return EmployeeCategory.ENGINEERING;
        }
        if (text.contains("field") || text.contains("supervisor")) {
            return EmployeeCategory.FIELD_STAFF;
        }
        if (text.contains("admin") || text.contains("commissioner") || text.contains("office")) {
            return EmployeeCategory.ADMINISTRATION;
        }
        if (text.contains("it") || text.contains("system")) {
            return EmployeeCategory.IT_SUPPORT;
        }
        return EmployeeCategory.OTHER;
    }

}
