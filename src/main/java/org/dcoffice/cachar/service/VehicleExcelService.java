package org.dcoffice.cachar.service;

import org.apache.poi.ss.usermodel.*;
import org.dcoffice.cachar.entity.VehicleDetails;
import org.dcoffice.cachar.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class VehicleExcelService {

    private final VehicleRepository repository;

    public VehicleExcelService(VehicleRepository repository) {
        this.repository = repository;
    }

    public void uploadExcel(InputStream inputStream) {

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<VehicleDetails> list = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                VehicleDetails v = new VehicleDetails();

                // 🚗 Vehicle fields
                v.setVehicleType(get(row, 2));
                v.setVehicleNo(get(row, 3));

                // 👤 Driver parsing
                String driverField = get(row, 4);
                extractDriverDetails(driverField, v);

                // 🏫 PS parsing (from route column)
                String psField = get(row, 5);
                extractPsDetails(psField, v);

                // 🛣️ Store full route as well
                v.setRoute(psField);

                // ⚠️ Fields not present in new Excel
                v.setAcNo("");
                v.setRemarks("");
                v.setCapacity(0);

                v.setUploadTime(System.currentTimeMillis());

                list.add(v);
            }

            repository.saveAll(list);

        } catch (Exception e) {
            throw new RuntimeException("Vehicle Excel upload failed: " + e.getMessage());
        }
    }

    // =========================
    // 🔧 DRIVER PARSER
    // =========================
    private void extractDriverDetails(String driverField, VehicleDetails v) {

        if (driverField == null || driverField.trim().isEmpty()) {
            v.setDriverName("");
            v.setDriverMobile("");
            return;
        }

        driverField = driverField.trim();

        try {
            // 🔹 Normalize (replace newline, tabs with space)
            driverField = driverField.replaceAll("[\\n\\r\\t]+", " ").trim();

            // 🔹 Extract mobile (10-digit number)
            String mobile = "";
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\\b\\d{10}\\b")
                    .matcher(driverField);

            if (matcher.find()) {
                mobile = matcher.group();
            }

            // 🔹 Remove mobile from string to get name
            String name = driverField.replace(mobile, "").replace("-", "").trim();

            v.setDriverName(name);
            v.setDriverMobile(mobile);

        } catch (Exception e) {
            v.setDriverName(driverField);
            v.setDriverMobile("");
        }
    }

    // =========================
    // 🔧 PS PARSER
    // =========================
    private void extractPsDetails(String psField, VehicleDetails v) {

        if (psField == null || psField.trim().isEmpty()) {
            v.setPsNo("");
            v.setPsName("");
            return;
        }

        psField = psField.trim();

        try {
            // Remove "PS No." prefix if present
            if (psField.toLowerCase().startsWith("ps no.")) {
                psField = psField.substring(6).trim();
            }

            // Split into PS No and Name
            String[] parts = psField.split(" - ", 2);

            String psNo = parts.length > 0 ? parts[0].trim() : "";
            String psName = parts.length > 1 ? parts[1].trim() : "";

            v.setPsNo(psNo);
            v.setPsName(psName);

        } catch (Exception e) {
            v.setPsNo("");
            v.setPsName(psField);
        }
    }

    // =========================
    // 🔧 COMMON CELL READER
    // =========================
    private String get(Row row, int index) {

        Cell cell = row.getCell(index);
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            default:
                return "";
        }
    }
}