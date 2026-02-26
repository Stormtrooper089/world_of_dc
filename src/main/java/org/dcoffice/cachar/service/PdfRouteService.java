package org.dcoffice.cachar.service;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.dcoffice.cachar.dto.ClusterResultDTO;
import org.dcoffice.cachar.entity.PollingStation;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class PdfRouteService {

    private static final String OUTPUT_DIR =
            "D:\\Tech\\world_of_dc\\generated-pdfs";
    private static final String FILE_NAME = "route-plan.pdf";

    public byte[] generateClusterRoutePdf(
            Integer lacNo,
            int clusterSize,
            List<ClusterResultDTO> clusters) {

        try {
            // Ensure directory exists
            File dir = new File(OUTPUT_DIR);
            if (!dir.exists()) dir.mkdirs();

            File pdfFile = new File(dir, FILE_NAME);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer =
                    new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            document.add(new Paragraph("Polling Station Route Plan")
                    .setBold()
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            for (ClusterResultDTO dto : clusters) {

                document.add(new Paragraph(
                        "Route Plan – Cluster " + dto.getClusterId())
                        .setBold()
                        .setFontSize(13));

                List<PollingStation> route =
                        dto.getRouteOrder();
                Map<String, Double> distanceMap =
                        dto.getDistanceMatrix();

                int step = 1;

                // ISBT → first PS
                PollingStation first = route.get(0);

                document.add(new Paragraph(
                        step++ + ". ISBT  ---->  "
                                + first.getStationName()
                                + " (" + dto.getIsbtToFirstStationKm() + " km)"
                ).setMarginLeft(20));

                // PS → PS
                for (int i = 0; i < route.size() - 1; i++) {
                    PollingStation from = route.get(i);
                    PollingStation to = route.get(i + 1);

                    String key =
                            "PS " + from.getPsNo()
                                    + " → PS " + to.getPsNo();

                    double km =
                            distanceMap.getOrDefault(key, 0.0);

                    document.add(new Paragraph(
                            step++ + ". "
                                    + from.getStationName()
                                    + "  ---->  "
                                    + to.getStationName()
                                    + " (" + km + " km)"
                    ).setMarginLeft(20));
                }

                document.add(new Paragraph(
                        "\nTotal Distance: "
                                + dto.getTotalRouteDistanceKm()
                                + " km"));

                document.add(new Paragraph(
                        "Estimated Time: "
                                + dto.getEstimatedTravelTimeHours()
                                + " hrs"));

                document.add(new Paragraph("\n----------------------------------------\n"));
            }

            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate route PDF", e);
        }
    }
}
