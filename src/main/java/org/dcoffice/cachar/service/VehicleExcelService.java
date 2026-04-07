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

                // 🆔 AC NO (Code column - keep as-is)
                v.setAcNo(get(row, 1));

                // 🚗 Vehicle fields
                v.setVehicleType(get(row, 2));
                v.setVehicleNo(get(row, 3));

                // 👤 Driver parsing
                extractDriverDetails(get(row, 4), v);

                // 🏫 PS fields (now separate columns)
                String psNo = get(row, 5);
                String psName = get(row, 6);

                v.setPsNo(cleanPsNo(psNo));
                v.setPsName(psName.trim());

                // 🛣️ Route (optional combined field)
                v.setRoute("PS-" + v.getPsNo() + " " + v.getPsName());

                // ⚠️ Defaults
                v.setRemarks("");
                v.setCapacity(0);

                v.setUploadTime(System.currentTimeMillis());

                list.add(v);
            }

            repository.saveAll(list);

        } catch (Exception e) {
            throw new RuntimeException("Vehicle Excel upload failed: " + e.getMessage(), e);
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
            // Normalize (handle multiline cells)
            driverField = driverField.replaceAll("[\\n\\r\\t]+", " ").trim();

            // Extract mobile (10-digit number)
            String mobile = "";
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\\b\\d{10}\\b")
                    .matcher(driverField);

            if (matcher.find()) {
                mobile = matcher.group();
            }

            // Remove mobile to get name
            String name = driverField.replace(mobile, "")
                    .replace("-", "")
                    .trim();

            v.setDriverName(name);
            v.setDriverMobile(mobile);

        } catch (Exception e) {
            v.setDriverName(driverField);
            v.setDriverMobile("");
        }
    }

    // =========================
    // 🔧 CLEAN PS NO
    // =========================
    private String cleanPsNo(String psNo) {

        if (psNo == null || psNo.trim().isEmpty()) return "";

        // Extract only digits
        String cleaned = psNo.replaceAll("[^0-9]", "");

        if (cleaned.isEmpty()) return "";

        // Pad to 3 digits (001, 002, ...)
        return String.format("%03d", Integer.parseInt(cleaned));
    }

    // =========================
    // 🔧 COMMON CELL READER
    // =========================
    private String get(Row row, int index) {

        Cell cell = row.getCell(index);
        if (cell == null) return "";

        switch (cell.getCellType()) {

            case STRING:
                return cell.getStringCellValue()
                        .replace("\r", "")
                        .trim();

            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            default:
                return "";
        }
    }
}