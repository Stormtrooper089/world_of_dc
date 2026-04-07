package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.Materials;
import org.dcoffice.cachar.entity.Member;
import org.dcoffice.cachar.entity.PollingParty;
import org.dcoffice.cachar.repository.PollingPartyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PollingPartyService {

    @Autowired
    private PollingPartyRepository pollingPartyRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // ================= SEARCH =================

    public List<PollingParty> searchPollingParties(String psName, String mobile) {

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (hasText(psName)) {
            criteriaList.add(Criteria.where("psName")
                    .regex("^" + normalize(psName) + "$", "i")); // case-insensitive exact match
        }

        if (hasText(mobile)) {
            criteriaList.add(Criteria.where("members.mobile")
                    .is(mobile.trim()));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // 🔥 Sort by psName (NOT psNo anymore)
        query.with(Sort.by(Sort.Direction.ASC, "psName"));

        if (criteriaList.isEmpty()) {
            return pollingPartyRepository.findAll(Sort.by(Sort.Direction.ASC, "psName"));
        }

        List<PollingParty> results = mongoTemplate.find(query, PollingParty.class);

        // Remove duplicates safely
        return results.stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(
                        PollingParty::getId,
                        p -> p,
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    // ================= DISTINCT LISTS =================

    public List<String> getAllPollingStations() {
        return mongoTemplate
                .query(PollingParty.class)
                .distinct("psName")
                .as(String.class)
                .all()
                .stream()
                .filter(this::hasText)
                .map(this::normalize)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public List<String> getAllPartyNames() {
        return mongoTemplate
                .query(PollingParty.class)
                .distinct("partyNo")
                .as(String.class)
                .all()
                .stream()
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    // ================= MATERIALS =================

    public Materials getMaterialsByPsName(String psName) {

        String normalized = normalize(psName);

        PollingParty party = pollingPartyRepository
                .findByPsNameIgnoreCase(normalized)
                .orElseThrow(() -> new RuntimeException("No polling party found for psName: " + psName));

        return party.getMaterials() != null
                ? party.getMaterials()
                : Materials.defaultMaterials();
    }

    public Materials updateMaterialsByPsName(String psName, Materials materials) {

        String normalized = normalize(psName);

        PollingParty party = pollingPartyRepository
                .findByPsNameIgnoreCase(normalized)
                .orElseThrow(() -> new RuntimeException("No polling party found for psName: " + psName));

        materials.setSubmitted(true);

        if (materials.getSubmittedAt() == null) {
            materials.setSubmittedAt(System.currentTimeMillis());
        }

        party.setMaterials(materials);
        pollingPartyRepository.save(party);

        return materials;
    }

    // ================= MEMBERS =================

    public List<Member> getMembersByPsName(String psName) {

        String normalized = normalize(psName);

        PollingParty party = pollingPartyRepository
                .findByPsNameIgnoreCase(normalized)
                .orElseThrow(() -> new RuntimeException("No polling party found for psName: " + psName));

        return party.getMembers() != null
                ? party.getMembers()
                : new ArrayList<>();
    }

    public List<Member> updateMembersByPsName(String psName, List<Member> members) {

        String normalized = normalize(psName);

        PollingParty party = pollingPartyRepository
                .findByPsNameIgnoreCase(normalized)
                .orElseThrow(() -> new RuntimeException("No polling party found for psName: " + psName));

        party.setMembers(members);
        pollingPartyRepository.save(party);

        return members;
    }

    // ================= VEHICLE =================

    public String getVehicleIdByPsName(String psName) {

        String normalized = normalize(psName);

        PollingParty party = pollingPartyRepository
                .findByPsNameIgnoreCase(normalized)
                .orElseThrow(() -> new RuntimeException("No polling party found for psName: " + psName));

        return party.getVehicleId();
    }

    public String updateVehicleIdByPsName(String psName, String vehicleId) {

        String normalized = normalize(psName);

        PollingParty party = pollingPartyRepository
                .findByPsNameIgnoreCase(normalized)
                .orElseThrow(() -> new RuntimeException("No polling party found for psName: " + psName));

        party.setVehicleId(vehicleId);
        pollingPartyRepository.save(party);

        return vehicleId;
    }

    // ================= HELPERS =================

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" :
                value.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }
}