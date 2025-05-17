package ncpl.bms.reports.model.dto;

public class ReportDTO {
    private Long id;
    private String name;
    private String fromDate;
    private String toDate;
    private byte[] pdfData;

    private String generatedBy;
    private String generatedDate;

    private Boolean isApproved;
    private String approvedBy;
    private String approvedDate;

    private String assignedReview;
    private String reviewedBy;
    private String reviewDate;

    private Boolean isApproverRequired; // Add new field
    private String assignedApprover; // Add new field



//    // Constructors
//    public ReportDTO() {
//    }

    public ReportDTO(Long id, String name, String fromDate, String toDate, byte[] pdfData, String generatedBy,
                     String generatedDate, Boolean isApproved, String approvedBy, String approvedDate, String assignedReview,
                     String reviewedBy, String reviewDate, Boolean isApproverRequired, String assignedApprover) {
        this.id = id;
        this.name = name;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.pdfData = pdfData;
        this.generatedBy = generatedBy;
        this.generatedDate = generatedDate;
        this.isApproved = isApproved;
        this.approvedBy = approvedBy;
        this.approvedDate = approvedDate;
        this.assignedReview = assignedReview;
        this.reviewedBy = reviewedBy;
        this.reviewDate = reviewDate;
        this.isApproverRequired = isApproverRequired; // Assign new field
        this.assignedApprover = assignedApprover; // Assign new field
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public byte[] getPdfData() {
        return pdfData;
    }

    public void setPdfData(byte[] pdfData) {
        this.pdfData = pdfData;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(String generatedDate) {
        this.generatedDate = generatedDate;
    }

    public Boolean getIsApproved() {
        return isApproved;
    }

    public void setIsApproved(Boolean isApproved) {
        this.isApproved = isApproved;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(String approvedDate) {
        this.approvedDate = approvedDate;
    }


    public String getAssignedReview() {
          return assignedReview;
    }

    public void setAssignedReview(String assignedReview) {
        this.assignedReview = assignedReview;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(String reviewDate) {
        this.reviewDate = reviewDate;
    }

    public Boolean getIsApproverRequired() {
        return isApproverRequired;
    }

    public void setIsApproverRequired(Boolean isApproverRequired) {
        this.isApproverRequired = isApproverRequired;
    }

    public String getAssignedApprover() {
        return assignedApprover;
    }

    public void setAssignedApprover(String assignedApprover) {
        this.assignedApprover = assignedApprover;
    }


}


