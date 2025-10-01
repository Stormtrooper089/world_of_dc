package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "complaint_files")
public class ComplaintFile {
    @Id
    private String id;
    private String complaintId;
    private String fileName;
    private LocalDateTime uploadedAt;
}