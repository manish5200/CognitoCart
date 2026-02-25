package com.manish.smartcart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    @Value("${app.upload-dir:uploads/products/}")
    private String uploadDir;

    public String uploadImage(MultipartFile file) throws IOException {
        // 1. Ensure directory exists
        File folder = new File(uploadDir);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // 2. Generate a unique file name to avoid collisions
        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;

        // 3. Resolve the full path
        Path path = Paths.get(uploadDir + uniqueFileName);

        // 4. Save the file to disk
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        log.debug("File saved: {}", uniqueFileName);
        return uniqueFileName; // Return only the name to store in DB
    }
}
