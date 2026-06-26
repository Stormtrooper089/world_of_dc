package org.dcoffice.cachar.entity;

import java.time.LocalDateTime;

public class AuctionDocument {
    private String fileName;
    private String fileUrl;
    private String fileType;
    private LocalDateTime uploadedAt;

    public AuctionDocument() {}

    public AuctionDocument(String fileName, String fileUrl, String fileType) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.uploadedAt = LocalDateTime.now();
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
