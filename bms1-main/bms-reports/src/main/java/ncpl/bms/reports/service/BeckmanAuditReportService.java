package ncpl.bms.reports.service;


    import com.lowagie.text.*;
        import com.lowagie.text.Font;
    import com.lowagie.text.Image;
    import com.lowagie.text.Rectangle;
    import com.lowagie.text.pdf.*;
        import lombok.extern.slf4j.Slf4j;
    import ncpl.bms.reports.model.dto.AuditLogDTO;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.core.io.ClassPathResource;
    import org.springframework.jdbc.core.JdbcTemplate;
    import org.springframework.stereotype.Service;

    import java.awt.*;
        import java.io.ByteArrayOutputStream;
    import java.io.IOException;
    import java.sql.ResultSet;
    import java.text.SimpleDateFormat;
    import java.util.Date;
    import java.util.List;
@Service
@Slf4j
public class BeckmanAuditReportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<AuditLogDTO> fetchAuditLogs(String startDate, String endDate) {
        String query = "SELECT [TIMESTAMP], [OPERATION], [TARGET], [SLOTNAME], [OLDVALUE], [VALUE], [USERNAME] " +
                "FROM[EMS_NEW_AUDITHISTORY] " +
                "WHERE [TIMESTAMP] BETWEEN ? AND ? ORDER BY [TIMESTAMP] ASC";

        return jdbcTemplate.query(query, new Object[]{startDate, endDate}, (ResultSet rs, int rowNum) -> {
            AuditLogDTO dto = new AuditLogDTO();
            dto.setTimestamp(rs.getString("TIMESTAMP"));
            dto.setOperation(rs.getString("OPERATION"));
            dto.setTarget(rs.getString("TARGET"));
            dto.setSlotName(rs.getString("SLOTNAME"));
            dto.setOldValue(rs.getString("OLDVALUE"));
            dto.setValue(rs.getString("VALUE"));
            dto.setUserName(rs.getString("USERNAME"));
            return dto;
        });
    }

    public void saveAuditReportPdf(byte[] pdfBytes, String startDate, String endDate) {
        try {
            if (pdfBytes != null && pdfBytes.length > 0) {
                String sql = "INSERT INTO StoredAuditReport (report_name, generated_on, report_data) VALUES (?, ?, ?)";

                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy");

                String formattedStart = outputFormat.format(inputFormat.parse(startDate));
                String formattedEnd = outputFormat.format(inputFormat.parse(endDate));

                String reportName = "Audit_Report_" + formattedStart + "_to_" + formattedEnd + ".pdf";
                Date now = new Date();

                jdbcTemplate.update(sql, reportName, new java.sql.Timestamp(now.getTime()), pdfBytes);
                log.info("Audit report saved with name: {}", reportName);
            } else {
                log.warn("Generated PDF is empty. Not saving to DB.");
            }
        } catch (Exception e) {
            log.error("Error saving audit report to DB", e);
        }
    }

    public byte[] getStoredAuditReportById(int reportId) {
        try {
            String sql = "SELECT report_data FROM StoredAuditReport WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{reportId}, byte[].class);
        } catch (Exception e) {
            log.error("Error fetching stored audit report from database, ID: " + reportId, e);
            return null;
        }
    }

    private String formatTimestamp(String rawTimestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date date = inputFormat.parse(rawTimestamp);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            return outputFormat.format(date);
        } catch (Exception e) {
            log.warn("Invalid timestamp format: " + rawTimestamp, e);
            return rawTimestamp;
        }
    }

    private PdfPCell createCenterCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    public byte[] generateAuditReportPdf(String startDate, String endDate) {
        List<AuditLogDTO> logs = fetchAuditLogs(startDate, endDate);

        if (logs.isEmpty()) {
            throw new RuntimeException("No audit logs found between the selected dates.");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 160, 50);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            writer.setPageEvent(new PdfPageEventHelper() {
                Image logo = null;
                Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
                Font dateFont = new Font(Font.HELVETICA, 11);
                Font footerFont = new Font(Font.HELVETICA, 9);

                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    try {
                        PdfContentByte cb = writer.getDirectContent();

                        // Header
                        PdfPTable headerTable = new PdfPTable(1); // Only 1 column to stack logo and title
                        headerTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                        headerTable.setLockedWidth(true);

// -- Logo Row --
                        PdfPCell logoCell = new PdfPCell();
                        logoCell.setBorder(Rectangle.NO_BORDER);
                        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                        if (logo == null) {
                            try {
                                logo = Image.getInstance(new ClassPathResource("static/images/logo1.png").getURL());
                                logo.scaleToFit(100, 50);
                            } catch (IOException e) {
                                log.error("Error loading logo image", e);
                            }
                        }

                        if (logo != null) {
                            logo.setAlignment(Image.ALIGN_LEFT);
                            logoCell.addElement(logo);
                        }

                        headerTable.addCell(logoCell);

// -- Title Row --
                        PdfPCell titleCell = new PdfPCell();
                        titleCell.setBorder(Rectangle.NO_BORDER);
                        titleCell.setPaddingTop(10f);
                        titleCell.setPaddingBottom(10f);

                        Paragraph titlePara = new Paragraph("Audit Report of EMS System", titleFont);
                        titlePara.setAlignment(Element.ALIGN_CENTER);

                        titleCell.addElement(titlePara);
                        headerTable.addCell(titleCell);

// -- Write the table to the PDF
                        headerTable.writeSelectedRows(0, -1, document.leftMargin(), document.getPageSize().getHeight() - 30, cb);

                        // Date Table
                        PdfPTable dateTable = new PdfPTable(2);
                        dateTable.setWidths(new float[]{1f, 1f});
                        dateTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                        dateTable.setLockedWidth(true);

                        String formattedStart = formatDate(startDate);
                        String formattedEnd = formatDate(endDate);

                        PdfPCell leftCell = new PdfPCell();
                        leftCell.setBorder(Rectangle.NO_BORDER);
                        leftCell.setPaddingBottom(5);
                        leftCell.addElement(new Paragraph("Start Date: " + formattedStart.split(" ")[0], dateFont));
                        leftCell.addElement(new Paragraph("Start Time: " + formattedStart.split(" ")[1], dateFont));

                        PdfPCell rightCell = new PdfPCell();
                        rightCell.setBorder(Rectangle.NO_BORDER);
                        rightCell.setPaddingBottom(5);
                        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

                        Paragraph endDatePara = new Paragraph("End Date: " + formattedEnd.split(" ")[0], dateFont);
                        endDatePara.setAlignment(Element.ALIGN_RIGHT);
                        Paragraph endTimePara = new Paragraph("End Time: " + formattedEnd.split(" ")[1], dateFont);
                        endTimePara.setAlignment(Element.ALIGN_RIGHT);

                        rightCell.addElement(endDatePara);
                        rightCell.addElement(endTimePara);

                        dateTable.addCell(leftCell);
                        dateTable.addCell(rightCell);
                        dateTable.writeSelectedRows(0, -1, document.leftMargin(), document.getPageSize().getHeight() - 120, cb);

                        // Footer
                        String generatedOn = "Generated on: " + new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());
                        String pageNumber = "Page " + writer.getPageNumber();

                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(generatedOn, footerFont), document.leftMargin(), 30, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(pageNumber, footerFont), (document.right() + document.left()) / 2, 30, 0);

                    } catch (Exception e) {
                        log.error("Header/footer generation error", e);
                    }
                }

                private String formatDate(String raw) {
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = inputFormat.parse(raw);
                        SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
                        return outputFormat.format(date);
                    } catch (Exception e) {
                        return raw;
                    }
                }
            });

            document.open();

            // Table Structure
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f, 4f, 2f, 2f, 2f, 2f});

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font cellFont = new Font(Font.HELVETICA, 9);

            String[] headers = {"Timestamp", "Operation", "Target", "Slot Name", "Old Value", "Value", "User Name"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }
            table.setHeaderRows(1);

            for (AuditLogDTO log : logs) {
                table.addCell(createCenterCell(formatTimestamp(log.getTimestamp()), cellFont));
                table.addCell(createCenterCell(log.getOperation(), cellFont));
                table.addCell(createCenterCell(log.getTarget(), cellFont));
                table.addCell(createCenterCell(log.getSlotName(), cellFont));
                table.addCell(createCenterCell(log.getOldValue(), cellFont));
                table.addCell(createCenterCell(log.getValue(), cellFont));
                table.addCell(createCenterCell(log.getUserName(), cellFont));
            }

            document.add(table);
            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generating audit report PDF", e);
            return null;
        }
    }

}