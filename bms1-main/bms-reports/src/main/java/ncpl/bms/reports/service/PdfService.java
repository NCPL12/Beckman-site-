package ncpl.bms.reports.service;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.format.DateTimeFormatter;
import com.lowagie.text.Paragraph;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.model.dao.ReportTemplate;
import ncpl.bms.reports.model.dto.GroupDTO;
import ncpl.bms.reports.model.dto.ReportDTO;
import ncpl.bms.reports.util.DateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Date;
import com.lowagie.text.Document;
import java.util.regex.*;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.awt.Color;
@Component
@Slf4j
public class PdfService {

    @Autowired
    private ReportDataService reportDataService;

    @Autowired
    private ReportTemplateService templateService;

    @Value("${report.address}")
    private String address;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${report.heading}")
    private String reportHeading;

    @Autowired
    private DateConverter dateConverter;

    private List<Map<String, Object>> reportDataList = null;
//    public String getSubArea(Long templateId) {
//        String sql = "SELECT report_group FROM report_template WHERE id = ?";
//        return jdbcTemplate.queryForObject(sql, new Object[]{templateId}, String.class);
//    }
public String getRoomIdAndName(Long templateId) {
    String sql = "SELECT room_id, room_name FROM report_template WHERE id = ?";

    try {
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, templateId);

        if (result == null || result.isEmpty()) {
            return "Room ID & Name: N/A";
        }

        String roomId = result.get("room_id") != null ? result.get("room_id").toString().trim() : "N/A";
        String roomName = result.get("room_name") != null ? result.get("room_name").toString().trim() : "N/A";

        return "Room ID & Name: " + roomId + " & " + roomName;
    } catch (Exception e) {
        return "Room ID & Name: N/A";
    }
}

    public String getSubArea(Long templateId) {
        String sql = "SELECT report_group FROM report_template WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{templateId}, String.class);
    }


    private String convertMillisToDate(Long millis) {
        if (millis == null) {
            return "N/A"; // Return a default value for null timestamps
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        return sdf.format(new Date(millis));
    }

    public String getReportName(Long templateId) {
        String sql = "SELECT name FROM report_template WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{templateId}, String.class);
    }
    public String getDynamicReportHeading(Long templateId) {
        try {
            String roomInfo = getRoomIdAndName(templateId); // e.g., "Room ID & Name: BDC012 & Sample Room"
            if (roomInfo != null && roomInfo.contains(":")) {
                String[] parts = roomInfo.split(":");
                if (parts.length > 1) {
                    return "EMS Report - " + parts[1].trim();
                }
            }
            return "EMS Report";
        } catch (Exception e) {
            log.error("❌ Failed to generate static heading", e);
            return "EMS Report";
        }
    }

    // This method should already exist in your service

    private Map<String, String> getTableToHeaderMap() {
        String sql = "SELECT TABLE_NAME, Header_Name FROM beckman_room_data";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<String, String> map = new HashMap<>();

        for (Map<String, Object> row : rows) {
            String tableName = String.valueOf(row.get("TABLE_NAME")).trim();
            String headerName = String.valueOf(row.get("Header_Name")).trim();
            map.put(tableName, headerName);
        }

        return map;
    }

//    private void addColorLegend(Document document) throws DocumentException {
//        PdfPTable legendTable = new PdfPTable(2);
//        legendTable.setWidthPercentage(30f);
//        legendTable.setSpacingBefore(10);
//        legendTable.setHorizontalAlignment(Element.ALIGN_LEFT);
//        legendTable.setWidths(new int[]{1, 4}); // 1 part color box, 4 part text
//
//        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
//
//        addLegendItem(legendTable, Color.RED, "Above Range");
//        addLegendItem(legendTable, Color.white, "Within Range");
//        addLegendItem(legendTable, Color.CYAN, "Below Range");
//        // Light green for OK
//
//        document.add(legendTable);
//    }

//    private void addLegendItem(PdfPTable table, Color color, String label) {
//        PdfPCell colorCell = new PdfPCell();
//        colorCell.setBackgroundColor(color);
//        colorCell.setFixedHeight(10f); // Small box
//        colorCell.setBorder(Rectangle.NO_BORDER);
//        table.addCell(colorCell);
//
//        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA, 9)));
//        labelCell.setBorder(Rectangle.NO_BORDER);
//        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        table.addCell(labelCell);
//    }

    public Map<String, Map<String, Map<String, Object>>> calculateStatistics(Long templateId, String fromDate, String toDate) {
        List<Map<String, Object>> data = reportDataService.generateReportData(templateId, fromDate, toDate);

        Map<String, Map<String, Map<String, Object>>> result = new LinkedHashMap<>();

        for (String key : data.get(0).keySet()) {
            if (key.equalsIgnoreCase("timestamp")) continue;

            double maxVal = Double.NEGATIVE_INFINITY;
            double minVal = Double.POSITIVE_INFINITY;
            long maxTime = 0L, minTime = 0L;
            double total = 0;
            int count = 0;

            for (Map<String, Object> row : data) {
                Object valObj = row.get(key);
                Object timeObj = row.get("timestamp");

                if (valObj == null || timeObj == null) continue;

                try {
                    double val = Double.parseDouble(valObj.toString());

                    long time;
                    if (timeObj instanceof Timestamp) {
                        time = ((Timestamp) timeObj).getTime(); // ✅ proper timestamp conversion
                    } else {
                        time = Long.parseLong(timeObj.toString());
                    }

                    if (val > maxVal) {
                        maxVal = val;
                        maxTime = time;
                    }
                    if (val < minVal) {
                        minVal = val;
                        minTime = time;
                    }

                    total += val;
                    count++;
                } catch (Exception e) {
                    log.warn("⛔ Error parsing value or timestamp for parameter '{}': {}", key, e.getMessage());
                }
            }

            Map<String, Map<String, Object>> statMap = new LinkedHashMap<>();
            Map<String, Object> maxMap = new HashMap<>();
            Map<String, Object> minMap = new HashMap<>();
            Map<String, Object> avgMap = new HashMap<>();

            if (count > 0) {
                maxMap.put("value", (int) maxVal);
                maxMap.put("timestamp", maxTime);

                minMap.put("value", (int) minVal);
                minMap.put("timestamp", minTime);

                avgMap.put("value", (int) (total / count));
            } else {
                log.warn("⚠️ No valid data found for parameter '{}'", key);
            }

            statMap.put("max", maxMap);
            statMap.put("min", minMap);
            statMap.put("avg", avgMap);
            result.put(key, statMap);
        }

        return result;
    }

    // inside your generatePdf method (AFTER the signature)
    public void generatePdf(Long templateId, String fromDateTime, String toDate, String username, String assignedTo, String assigned_approver) throws Exception {
        long start = System.currentTimeMillis();
        System.out.println("⏱ [1] START PDF generation");

        reportDataList = reportDataService.generateReportData(templateId, fromDateTime, toDate);
        System.out.println("⏱ [2] Data fetched in " + (System.currentTimeMillis() - start) + " ms");

        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formattedFromDateTime = dateTimeFormatter.format(new Date(Long.parseLong(fromDateTime)));
        String formattedToDateTime = dateTimeFormatter.format(new Date(Long.parseLong(toDate)));

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, byteArrayOutputStream);
        System.out.println("⏱ [3] PDF Writer initialized in " + (System.currentTimeMillis() - start) + " ms");

        TablePageEvent event = new TablePageEvent(formattedFromDateTime, formattedToDateTime, username, templateId, this);
        writer.setPageEvent(event);
        System.out.println("⏱ [4] PageEvent set in " + (System.currentTimeMillis() - start) + " ms");

        document.open();
        System.out.println("⏱ [5] Document opened in " + (System.currentTimeMillis() - start) + " ms");

        Map<String, Object> stringObjectMap = reportDataList.get(0);
        int columnCount = stringObjectMap.size();
        int rowCount = 0;
        int rowsPerPage = 22;
        Map<String, Map<String, Map<String, Object>>> statistics = calculateStatistics(templateId, fromDateTime, toDate);
        System.out.println("⏱ [6] Statistics calculated in " + (System.currentTimeMillis() - start) + " ms");

        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(5);

        addTableHeader(templateId, stringObjectMap, table);
        Map<String, double[]> parameterRanges = extractParameterRanges(templateId);

        for (Map<String, Object> map : reportDataList) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object rawValue = entry.getValue();
                String valueStr = (rawValue == null || rawValue.toString().trim().isEmpty() || "null".equalsIgnoreCase(rawValue.toString())) ? "null" : rawValue.toString();
                if (entry.getKey().equalsIgnoreCase("timestamp")) {
                    try {
                        valueStr = convertMillisToDate(Long.parseLong(valueStr));
                    } catch (Exception ignored) {}
                }
                PdfPCell valueCell = new PdfPCell(new Phrase(valueStr));
                valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                try {
                    double value = Double.parseDouble(valueStr);
                    double[] range = parameterRanges.get(extractBaseParameter(entry.getKey()));
                    if (range != null) {
                        double from = range[0], to = range[1];
//                        if (value > to) valueCell.setBackgroundColor(CMYKColor.RED);
//                        else if (value < from) valueCell.setBackgroundColor(CMYKColor.CYAN);
                    }
                } catch (NumberFormatException ignored) {}
                table.addCell(valueCell);
            }
            rowCount++;
            if (rowCount % rowsPerPage == 0) {
                document.add(table);
                document.newPage();
                table = new PdfPTable(columnCount);
                table.setWidthPercentage(100f);
                table.setSpacingBefore(5);
                addTableHeader(templateId, stringObjectMap, table);
            }
        }

        if (rowCount % rowsPerPage != 0) {
            document.add(table);
            document.newPage();
        }

        System.out.println("⏱ [7] Table rows added in " + (System.currentTimeMillis() - start) + " ms");

        PdfPTable statisticsTable = new PdfPTable(columnCount);
        statisticsTable.setWidthPercentage(100f);
        statisticsTable.setSpacingBefore(10);

        addTableHeader(templateId, stringObjectMap, statisticsTable);
        addStatisticsRow("Max", statistics, statisticsTable);
        addStatisticsRow("Min", statistics, statisticsTable);
        addStatisticsRow("Avg", statistics, statisticsTable);

        document.add(statisticsTable);
//        addColorLegend(document);
        document.close();
        System.out.println("⏱ [8] Document closed. Total time so far: " + (System.currentTimeMillis() - start) + " ms");

        String dynamicHeading = getDynamicReportHeading(templateId);
        String cleanHeading = dynamicHeading.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
        String pdfFileName = cleanHeading + ".pdf";
        long currentTimeMillis = System.currentTimeMillis();
        String currentDateStr = Long.toString(currentTimeMillis);
        int chk = (assigned_approver == null || assigned_approver.trim().isEmpty()) ? 0 : 1;

        String sql = "INSERT INTO stored_reports (name, from_date, to_date, pdf_data, generated_by, generated_date, assigned_review, assigned_approver, is_approver_required) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, pdfFileName);
            ps.setTimestamp(2, new Timestamp(Long.parseLong(fromDateTime))); // ✅ Fix
            ps.setTimestamp(3, new Timestamp(Long.parseLong(toDate)));       // ✅ Fix
            ps.setBytes(4, byteArrayOutputStream.toByteArray());
            ps.setString(5, username);
            ps.setString(6, currentDateStr); // this is okay since it's a string
            ps.setString(7, assignedTo);
            ps.setString(8, assigned_approver);
            ps.setBoolean(9, chk == 1);
            return ps;
        });
        System.out.println("⏱ [9] PDF saved to DB in " + (System.currentTimeMillis() - start) + " ms");
    }




    //    private String extractBaseParameter(String columnName) {
    //        if (columnName.contains("_From_")) {
    //            return columnName.substring(0, columnName.indexOf("_From_"));
    //        }
    //        return columnName;
    //    }
    //private String extractUnit(String columnName) {
    //    if (columnName.contains("_Unit_")) {
    //        return columnName.substring(columnName.indexOf("_Unit_") + 6); // extract after "_Unit_"
    //    }
    //    return ""; // Return empty if no unit
    //}
    //    private String extractBaseParameter(String columnName) {
    //        String base = columnName;
    //        if (base.contains("_From_")) base = base.substring(0, base.indexOf("_From_"));
    //        if (base.contains("_To_")) base = base.substring(0, base.indexOf("_To_"));
    //        if (base.contains("_Unit_")) base = base.substring(0, base.indexOf("_Unit_"));
    //        return base;
    //    }

    private Map<String, String> extractFormattedParameterRanges(Long templateId) {
        ReportTemplate template = templateService.getById(templateId);
        if (template == null) {
            throw new RuntimeException("ReportTemplate not found for templateId: " + templateId);
        }

        List<String> parameters = template.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            throw new RuntimeException("No parameters found for templateId: " + templateId);
        }

        Map<String, String> formattedParameterRanges = new HashMap<>();

        for (String parameter : parameters) {
            String baseName = extractBaseParameter(parameter);
            if (baseName.startsWith("EMS_NEW_")) {
                baseName = baseName.substring("EMS_NEW_".length());
            }
            String unit = extractUnit(parameter);

            String formatted = unit.isEmpty() ? baseName : String.format("%s(%s)", baseName, unit);
            formattedParameterRanges.put(parameter, formatted);
        }
        return formattedParameterRanges;
    }

    private String removeSuffix(String columnName) {
        if (columnName.contains("_From_")) {
            return columnName.substring(0, columnName.indexOf("_From_"));
        }
        return columnName;
    }

    private static final Pattern RANGE_PATTERN = Pattern.compile("_From_(\\d+(?:\\.\\d+)?)_To_(\\d+(?:\\.\\d+)?)");
    private static final Pattern UNIT_PATTERN = Pattern.compile("_Unit_([a-zA-Z]+)$");

    private double getFromValue(String paramName) {
        Matcher matcher = RANGE_PATTERN.matcher(paramName);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return Double.NEGATIVE_INFINITY;
    }


    private double getToValue(String paramName) {
        Matcher matcher = RANGE_PATTERN.matcher(paramName);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(2));
        }
        return Double.POSITIVE_INFINITY;
    }

    private String extractUnit(String paramName) {
        Matcher matcher = UNIT_PATTERN.matcher(paramName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractBaseParameter(String columnName) {
        return columnName.replaceAll("(_From_.*|_Unit_.*)$", "");
    }


    private void addStatisticsRow(String label, Map<String, Map<String, Map<String, Object>>> statistics, PdfPTable table) {
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        List<String> parameterKeys = new ArrayList<>(statistics.keySet());

        boolean hasTimestamp = !label.equalsIgnoreCase("Avg");

        // Label cell
        PdfPCell labelCell = new PdfPCell(new Phrase(label, fontBold));
//            labelCell.setBackgroundColor(CMYKColor.YELLOW);
        labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (hasTimestamp) {
            labelCell.setRowspan(2); // Only Max & Min
        }
        table.addCell(labelCell);

        // Value Row
        for (String parameter : parameterKeys) {
            Map<String, Object> statData = statistics.get(parameter).get(label.toLowerCase());
            String valueStr = "null";

            if (statData != null && statData.get("value") != null) {
                valueStr = statData.get("value").toString();
            }

            PdfPCell valueCell = new PdfPCell(new Phrase(valueStr, fontNormal));
//                valueCell.setBackgroundColor(CMYKColor.YELLOW);
            valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(valueCell);
        }

        // Timestamp Row (for Max & Min only)
        if (hasTimestamp) {
            for (String parameter : parameterKeys) {
                Map<String, Object> statData = statistics.get(parameter).get(label.toLowerCase());
                String dateStr = "";

                if (statData != null && statData.get("timestamp") != null) {
                    try {
                        long millis = Long.parseLong(statData.get("timestamp").toString());
                        dateStr = convertMillisToDate(millis);
                    } catch (Exception ignored) {
                        dateStr = "N/A";
                    }
                }

                PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, fontNormal));
//                    dateCell.setBackgroundColor(CMYKColor.YELLOW);
                dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(dateCell);
            }
        }
    }

    private void addTableHeader(Long templateId, Map<String, Object> stringObjectMap, PdfPTable table) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE); // White header text
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(0, 123, 128));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        ReportTemplate template = templateService.getById(templateId);
        Map<String, String> tableToHeaderMap = getTableToHeaderMap();


        // Timestamp header
        cell.setPhrase(new Phrase("Timestamp", font));
        table.addCell(cell);

        // Parameter headers with range only if explicitly set
        for (String parameter : template.getParameters()) {
            String baseKey = extractBaseParameter(parameter);
            String headerLabel = tableToHeaderMap.getOrDefault(baseKey, baseKey); // HOT_SPOT_RH for example

            double fromValue = getFromValue(parameter);
            double toValue = getToValue(parameter);

            String unit = extractUnit(parameter); // get % from _Unit_%
            String formattedHeader = unit.isEmpty() ? headerLabel : headerLabel + "(" + unit + ")";

            if (fromValue != Double.NEGATIVE_INFINITY && toValue != Double.POSITIVE_INFINITY) {
                formattedHeader += String.format("\nRange: %.0f - %.0f", fromValue, toValue);
            }

            cell.setPhrase(new Phrase(formattedHeader, font));
            table.addCell(cell);
        }


        table.setHeaderRows(1);
    }

    private class TablePageEvent extends PdfPageEventHelper {

        private final String fromDateTime;
        private final String toDateTime;
        private final String username;
        private final Long templateId;
        private final PdfService pdfService;
        private String reviewedBy = "";
        private String reviewDate = "";
        private PdfTemplate totalPageTemplate;
        private BaseFont baseFont;


        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPageTemplate = writer.getDirectContent().createTemplate(50, 50);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (DocumentException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPageTemplate.beginText();
            totalPageTemplate.setFontAndSize(baseFont, 10);
            totalPageTemplate.setTextMatrix(0, 0);
            totalPageTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPageTemplate.endText();
        }

        public TablePageEvent(String fromDateTime, String toDateTime, String username, Long templateId, PdfService pdfService) {
            this.fromDateTime = fromDateTime;
            this.toDateTime = toDateTime;
            this.username = username;
            this.templateId = templateId;
            this.pdfService = pdfService;
            try {
                ReportDTO latestReport = pdfService.findLatestGeneratedReport(templateId, fromDateTime, toDateTime, username);
                if (latestReport != null) {
                    this.reviewedBy = latestReport.getReviewedBy();
                    if (latestReport.getReviewDate() != null) {
                        long millis = Long.parseLong(latestReport.getReviewDate());
                        this.reviewDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date(millis));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch review info for footer", e);
            }
        }



        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            try {
                Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
                Font fontContent = FontFactory.getFont(FontFactory.HELVETICA, 11);

                PdfPTable headerTable = new PdfPTable(3);
                headerTable.setWidthPercentage(100);
                headerTable.setWidths(new float[]{40f, 40f, 20f});  // Logo gets rightmost 20%

                Font fontAddress = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16); // CORRECT

                PdfPCell addressCell = new PdfPCell(new Paragraph(address, fontAddress));
                addressCell.setBorder(Rectangle.NO_BORDER);
                addressCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                addressCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                addressCell.setPadding(5);


                // Middle cell - optional (can leave blank or use for subtitle)
                PdfPCell centerCell = new PdfPCell(new Paragraph(""));
                centerCell.setBorder(Rectangle.NO_BORDER);

                // Right cell - Logo
                PdfPCell logoCell = new PdfPCell();
                try {
                    Image image = Image.getInstance(new ClassPathResource("static/images/logo1.png").getURL());
                    image.scaleToFit(90, 70);
                    image.setAlignment(Image.ALIGN_RIGHT);  // align right inside the cell
                    logoCell.addElement(image);
                } catch (IOException e) {
                    logoCell.addElement(new Paragraph("Logo"));
                }
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setPaddingRight(5);
                logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                // Title row (spans all columns)
//                String dynamicHeading = pdfService.getDynamicReportHeading(templateId);
//                PdfPCell titleCell = new PdfPCell(new Paragraph(dynamicHeading, fontTitle));
//                titleCell.setColspan(3);
//                titleCell.setBorder(Rectangle.NO_BORDER);
//                titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//                titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//                titleCell.setPaddingTop(5);
//                titleCell.setPaddingBottom(10);

                // Sub-info row (Room, Sensor, Date, etc.)
                PdfPCell infoCell = new PdfPCell();
                infoCell.setColspan(3);
                infoCell.setBorder(Rectangle.NO_BORDER);
                infoCell.setPaddingLeft(5);

                DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
                DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

                LocalDateTime from = LocalDateTime.parse(fromDateTime, inputFormat);
                LocalDateTime to = LocalDateTime.parse(toDateTime, inputFormat);

                String displayStartDate = from.format(dateFormat);
                String displayStartTime = from.format(timeFormat);
                String displayEndDate = to.format(dateFormat);
                String displayEndTime = to.format(timeFormat);

                Paragraph paragraph = new Paragraph();
                paragraph.setFont(fontContent);
                paragraph.setLeading(12f);
                String roomInfo = pdfService.getRoomIdAndName(templateId);
                paragraph.add(roomInfo + "\n");
                String groupName = pdfService.getSubArea(templateId);
                paragraph.add("Sensor ID : " + groupName + "\n");
                paragraph.add("Username  : " + username + "\n");
                paragraph.add("From: " + displayStartDate + " " + displayStartTime + " to " + displayEndDate + " " + displayEndTime + "\n");

                infoCell.addElement(paragraph);

                // Add to table
                headerTable.addCell(addressCell);
                headerTable.addCell(centerCell);
                headerTable.addCell(logoCell);
//                headerTable.addCell(titleCell);
                headerTable.addCell(infoCell);

                document.add(headerTable);

            } catch (Exception e) {
                throw new RuntimeException("Error creating PDF header", e);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.beginText();
            cb.setFontAndSize(baseFont, 10);

            int pageNumber = writer.getPageNumber();
            String text = "Printed On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm")) +
                    "\nPrinted By: " + username +
                    "\nPage No: " + pageNumber + " of ";

            float x = document.right() - 120;
            float y = document.bottom() - 10;

            // Multiline positioning manually
            cb.setTextMatrix(x, y + 20);
            cb.showText("Printed On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm")));

            cb.setTextMatrix(x, y + 8);
            cb.showText("Printed By: " + username);

            String pageText = "Page No: " + pageNumber + " of ";
            cb.setTextMatrix(x, y - 4);
            cb.showText(pageText);

            cb.endText();
            cb.addTemplate(totalPageTemplate, x + baseFont.getWidthPoint(pageText, 10), y - 4);
        }


    }
    public List<ReportDTO> getAllReports() {
        String sql = "SELECT id, name, from_date, to_date, generated_by, generated_date, is_approved, approved_by, approved_date, assigned_review, reviewed_by, review_date, is_approver_required, assigned_approver FROM stored_reports ORDER BY generated_date DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ReportDTO(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("from_date"),
                rs.getString("to_date"),
                null, // pdfData will be set in getReportById
                rs.getString("generated_by"),
                rs.getString("generated_date"),
                rs.getBoolean("is_approved"),
                rs.getString("approved_by"),
                rs.getString("approved_date"),
                rs.getString("assigned_review"),  // Retrieve assignedReview
                rs.getString("reviewed_by"),      // Retrieve reviewedBy
                rs.getString("review_date") ,      // Retrieve reviewDate
                rs.getBoolean("is_approver_required"),
                rs.getString("assigned_approver")
        ));
    }
    public ReportDTO getReportById(Long reportId) {
        String sql = "SELECT name, from_date, to_date, pdf_data, generated_by, generated_date, is_approved, approved_by, approved_date, assigned_review, reviewed_by, review_date, is_approver_required, assigned_approver FROM stored_reports WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{reportId}, (rs, rowNum) -> new ReportDTO(
                reportId,
                rs.getString("name"),
                rs.getString("from_date"),
                rs.getString("to_date"),
                rs.getBytes("pdf_data"),
                rs.getString("generated_by"),
                rs.getString("generated_date"),
                rs.getBoolean("is_approved"),
                rs.getString("approved_by"),
                rs.getString("approved_date"),
                rs.getString("assigned_review"),  // Retrieve assignedReview
                rs.getString("reviewed_by"),      // Retrieve reviewedBy
                rs.getString("review_date"),      // Retrieve reviewDate
                rs.getBoolean("is_approver_required"),
                rs.getString("assigned_approver")
        ));
    }
    public List<GroupDTO> getAllGroups() {
        String sql = "SELECT id, name FROM group_names";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GroupDTO(
                rs.getLong("id"),
                rs.getString("name")
        ));
    }

    public void stampReviewInfo(Long reportId, String reviewer) throws Exception {
        // Fetch the existing PDF from DB
        ReportDTO reportDTO = getReportById(reportId);
        if (reportDTO == null) {
            throw new Exception("Report not found");
        }

        // Read the PDF
        PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(reportDTO.getPdfData()));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfStamper pdfStamper = new PdfStamper(pdfReader, byteArrayOutputStream);

        // Prepare review info
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMMM-yyyy HH:mm:ss");
        String formattedDate = now.format(formatter);

        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        PdfPTable reviewTable = new PdfPTable(1);
        reviewTable.setTotalWidth(180);
        reviewTable.setWidthPercentage(100);

        PdfPCell reviewCell = new PdfPCell(new Phrase("\nReviewed By:Supervisor: " + reviewer + "\nDate: " + formattedDate, font));
        reviewCell.setBorder(Rectangle.NO_BORDER);
        reviewCell.setPadding(10);
        reviewCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        reviewTable.addCell(reviewCell);

        // Stamp on every page
        int totalPages = pdfReader.getNumberOfPages();
        for (int i = 1; i <= totalPages; i++) {
            PdfContentByte canvas = pdfStamper.getOverContent(i);
            Rectangle pageSize = pdfReader.getPageSize(i);

            float x = pageSize.getRight() - 260;
            float y = pageSize.getBottom() + 73;
            reviewTable.writeSelectedRows(0, -1, x, y, canvas);
        }
        pdfStamper.close();
        pdfReader.close();

        // Save updated PDF and update DB
        long reviewTimeMillis = System.currentTimeMillis();
        String sql = "UPDATE stored_reports SET pdf_data = ?, reviewed_by = ?, review_date = ? WHERE id = ?";
        jdbcTemplate.update(sql, byteArrayOutputStream.toByteArray(), reviewer, String.valueOf(reviewTimeMillis), reportId);
    }

    public void approveReport(Long reportId, String username) throws Exception {
        // Fetch the existing PDF from the database
        ReportDTO reportDTO = getReportById(reportId);
        if (reportDTO == null) {
            throw new Exception("Report not found");
        }

        // Read the existing PDF
        PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(reportDTO.getPdfData()));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfStamper pdfStamper = new PdfStamper(pdfReader, byteArrayOutputStream);

        // Define the approval table
        PdfPTable approvalTable = new PdfPTable(1);
        approvalTable.setTotalWidth(180); // Set fixed width
        approvalTable.setWidthPercentage(100);

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        // Get the current date
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMMM-yyyy HH:mm:ss");
        String formattedDate = currentDateTime.format(formatter);

        // Create Approval Cell with No Border
        PdfPCell approvalCell = new PdfPCell(new Phrase("\nApproved By:Supervisor: " + username +"\nDate: " + formattedDate, fontTitle));
        approvalCell.setBorder(Rectangle.NO_BORDER);
        approvalCell.setPadding(10);
        approvalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        approvalTable.addCell(approvalCell);

        int totalPages = pdfReader.getNumberOfPages();

        // Loop through all pages and stamp the approval table
        for (int i = 1; i <= totalPages; i++) {
            PdfContentByte content = pdfStamper.getOverContent(i);
            Rectangle pageSize = pdfReader.getPageSize(i);
            //                    float xPos = pageSize.getRight();
            float xPos = pageSize.getRight() - approvalTable.getTotalWidth() +210;

            float yPos = pageSize.getBottom() + 73;
            approvalTable.writeSelectedRows(0, -1, xPos, yPos, content);
        }


        // Close the PDF
        pdfStamper.close();
        pdfReader.close();

        // Update the database with the approved PDF
        long approvedTimeMillis = System.currentTimeMillis();
        String sql = "UPDATE stored_reports SET pdf_data = ?, is_approved = ?, approved_by = ?, approved_date = ? WHERE id = ?";
        jdbcTemplate.update(sql, byteArrayOutputStream.toByteArray(), true, username, String.valueOf(approvedTimeMillis), reportId);
    }

    //            public void reviewReport(Long reportId, String username) throws Exception {
    //                // Update the PDF and approval details in the database
    //                long reviewedTimeMillis = System.currentTimeMillis();
    //                String sql = "UPDATE stored_reports SET reviewed_by = ?, review_date = ? WHERE id = ?";
    //                jdbcTemplate.update(sql, username, String.valueOf(reviewedTimeMillis), reportId);
    //            }
    public void reviewReport(Long reportId, String username) throws Exception {
        stampReviewInfo(reportId, username);
    }

    private Map<String, double[]> extractParameterRanges(Long templateId) {
        ReportTemplate template = templateService.getById(templateId);
        Map<String, double[]> parameterRanges = new HashMap<>();

        for (String parameter : template.getParameters()) {
            String cleanParameter = extractBaseParameter(parameter); // same as you're already using
            double fromValue = getFromValue(parameter);
            double toValue = getToValue(parameter);
            parameterRanges.put(cleanParameter, new double[]{fromValue, toValue});
        }

        return parameterRanges;
    }
    public ReportDTO findLatestGeneratedReport(Long templateId, String fromDateTime, String toDateTime, String generatedBy) {
        String templateName = getReportName(templateId).replaceAll("[^a-zA-Z0-9]", "_");
        String likeName = templateName + "%";

        String sql = "SELECT id, name, from_date, to_date, pdf_data, generated_by, generated_date, is_approved, " +
                "approved_by, approved_date, assigned_review, reviewed_by, review_date, " +
                "is_approver_required, assigned_approver " +
                "FROM stored_reports WHERE name LIKE ? AND from_date = ? AND to_date = ? AND generated_by = ? " +
                "ORDER BY generated_date DESC ";

        List<ReportDTO> reports = jdbcTemplate.query(sql,
                new Object[]{likeName, fromDateTime, toDateTime, generatedBy},
                (rs, rowNum) -> new ReportDTO(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("from_date"),
                        rs.getString("to_date"),
                        rs.getBytes("pdf_data"),
                        rs.getString("generated_by"),
                        rs.getString("generated_date"),
                        rs.getBoolean("is_approved"),
                        rs.getString("approved_by"),
                        rs.getString("approved_date"),
                        rs.getString("assigned_review"),
                        rs.getString("reviewed_by"),
                        rs.getString("review_date"),
                        rs.getBoolean("is_approver_required"),
                        rs.getString("assigned_approver")
                )
        );

        return reports.isEmpty() ? null : reports.get(0);
    }

}