package org.dcoffice.cachar.service;

import org.apache.poi.ss.usermodel.*;
import org.dcoffice.cachar.entity.Member;
import org.dcoffice.cachar.entity.PollingParty;
import org.dcoffice.cachar.repository.PollingPartyRepository;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class PollingPartyExcelService {

    private final PollingPartyRepository repository;
    private final DataFormatter formatter = new DataFormatter();

    public PollingPartyExcelService(PollingPartyRepository repository) {
        this.repository = repository;
    }

    public void uploadExcel(InputStream inputStream) {

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            Map<String, Integer> headerMap = buildHeaderMap(headerRow);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;


                String psName = normalizePsName(get(row, headerMap, "POLLING STATION NAME"));

                if (psName.isBlank()) continue;
                PollingParty party = repository.findByPsNameIgnoreCase(psName)
                        .orElseGet(() -> {
                            PollingParty p = new PollingParty();
                            p.setPsName(psName);
                            p.setMembers(new ArrayList<>());
                            return p;
                        });

                String name = get(row, headerMap, "POLLING OFFICER");
                String duty = get(row, headerMap, "DUTY");
                String mobile = get(row, headerMap, "MOBILE");
                String groupCode = get(row, headerMap, "GROUP CODE");

                if (!name.isBlank()) {

                    Member member = new Member(
                            normalizeRole(duty),
                            name.trim(),
                            mobile.trim(),
                            groupCode.trim()
                    );

                    if (!alreadyExists(party.getMembers(), member)) {
                        party.getMembers().add(member);
                    }
                }

                party.setUploadTime(System.currentTimeMillis());
                repository.save(party);
            }

        } catch (Exception e) {
            throw new RuntimeException("Excel upload failed: " + e.getMessage(), e);
        }
    }

    // ================= HEADER MAPPING =================

    private Map<String, Integer> buildHeaderMap(Row headerRow) {

        Map<String, Integer> map = new HashMap<>();

        for (Cell cell : headerRow) {

            String header = formatter.formatCellValue(cell)
                    .trim()
                    .replaceAll("\\s+", " ")
                    .toUpperCase();

            map.put(header, cell.getColumnIndex());
        }

        List<String> requiredHeaders = Arrays.asList(
                "POLLING STATION NAME",
                "POLLING OFFICER",
                "DUTY"
        );

        for (String h : requiredHeaders) {
            if (!map.containsKey(h)) {
                throw new RuntimeException("Missing required column: " + h);
            }
        }

        return map;
    }

    // ================= VALUE FETCH =================

    private String get(Row row, Map<String, Integer> headerMap, String headerName) {

        Integer index = headerMap.get(headerName);
        if (index == null) return "";

        Cell cell = row.getCell(index);
        return (cell == null) ? "" : formatter.formatCellValue(cell).trim();
    }

    // ================= DUPLICATE CHECK =================

    private boolean alreadyExists(List<Member> members, Member newMember) {

        return members.stream().anyMatch(m ->
                m.getName().equalsIgnoreCase(newMember.getName()) &&
                        m.getRole().equalsIgnoreCase(newMember.getRole())
        );
    }

    // ================= ROLE NORMALIZATION =================

    private String normalizeRole(String duty) {

        if (duty == null) return "UNKNOWN";

        duty = duty.toLowerCase();

        if (duty.contains("presiding")) return "PRESIDING_OFFICER";
        if (duty.contains("first") || duty.contains("1")) return "POLLING_OFFICER_1";
        if (duty.contains("second") || duty.contains("2")) return "POLLING_OFFICER_2";
        if (duty.contains("third") || duty.contains("3")) return "POLLING_OFFICER_3";
        if (duty.contains("reserve")) return "RESERVE_OFFICER";

        return duty.toUpperCase();
    }

    // ================= PS NAME NORMALIZATION (CRITICAL) =================

    private String normalizePsName(String psName) {

        return psName
                .toUpperCase()
                .replaceAll("\\s+", " ")
                .replaceAll("\\(.*?\\)", "") // remove room variations
                .trim();
    }
}