package org.dcoffice.cachar.service;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.dcoffice.cachar.entity.Complaint;
import org.dcoffice.cachar.entity.ComplaintDocument;
import org.dcoffice.cachar.exception.FileStorageException;
import org.dcoffice.cachar.repository.ComplaintDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Autowired
    private ComplaintDocumentRepository documentRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("File storage location initialized: {}", this.fileStorageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    public void storeFiles(Complaint complaint, List<MultipartFile> files) {
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                storeFile(complaint, file);         // Local storage
                storeFileToMongo(complaint, file);  // MongoDB GridFS storage
            }
        }
    }

    public ComplaintDocument storeFile(Complaint complaint, MultipartFile file) {
        validateFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = generateUniqueFileName(originalFileName);

        try {
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Invalid file name: " + originalFileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            ComplaintDocument document = new ComplaintDocument();
            document.setId(complaint.getId());
            document.setFileName(fileName);
            document.setOriginalFileName(originalFileName);
            document.setFileType(file.getContentType());
            document.setFilePath(targetLocation.toString());
            document.setFileSize(file.getSize());

            ComplaintDocument saved = documentRepository.save(document);
            logger.info("Stored file locally: {} for complaint: {}", originalFileName, complaint.getComplaintNumber());

            return saved;

        } catch (IOException e) {
            throw new FileStorageException("Could not store file: " + originalFileName, e);
        }
    }

    public void storeFileToMongo(Complaint complaint, MultipartFile file) {
        try {
            DBObject metadata = new BasicDBObject();
            metadata.put("complaintId", complaint.getComplaintNumber());
            metadata.put("originalFileName", file.getOriginalFilename());
            metadata.put("fileType", file.getContentType());
            metadata.put("fileSize", file.getSize());
            metadata.put("uploadedAt", LocalDateTime.now());

            gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType(), metadata);
            logger.info("Stored file in MongoDB: {} for complaint: {}", file.getOriginalFilename(), complaint.getComplaintNumber());
        } catch (IOException e) {
            logger.error("Failed to store file in MongoDB: {}", file.getOriginalFilename(), e);
            throw new FileStorageException("Could not store file in MongoDB: " + file.getOriginalFilename(), e);
        }
    }

    public byte[] loadFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new FileStorageException("Invalid file path: " + fileName);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Could not load file: " + fileName, e);
        }
    }

    public List<ComplaintDocument> getComplaintDocuments(Long complaintId) {
        return documentRepository.findByComplaintId(complaintId);
    }
    public List<ComplaintDocument> getComplaintDocuments(String complaintId) {
        return documentRepository.findByComplaintNumber(complaintId);
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException("Invalid file type. Only PDF, JPEG, PNG, and GIF files are allowed");
        }

        if (file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex + 1);
    }
}