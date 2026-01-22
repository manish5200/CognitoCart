package com.manish.smartcart.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * FileValidator
 ---------------
 * RESPONSIBILITY:
 * - Perform ONLY validation
 * - Do NOT extract data
 * - Do NOT perform uploads
 ---------------
 * DESIGN RULE:
 * Validation must be:
 * - deterministic
 * - side-effect free
 * - fail-fast
 --------------
 * This is CRITICAL for:
 * - Kafka consumers
 * - async workers
 * - S3 / CDN pipelines
 */
@Slf4j
public final class FileValidator {

    // ---- Single source of truth for constraints ----
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "webp"
    );

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private FileValidator() {
        // Utility class — prevent instantiation
    }

    /**
     * Validates an uploaded image.
     ------------------
     * IMPORTANT:
     * - No return value
     * - Throws exception on failure
     * - Caller decides how to handle error (API / Kafka / worker)
     */
    public static void validateImage(MultipartFile file) {

        // 1️⃣ Null / empty check (fail fast)
        if (file == null || file.isEmpty()) {
            log.warn("File validation failed: empty file");
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }

        // 2️⃣ Size check
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File validation failed: size={} bytes", file.getSize());
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        // 3️⃣ Filename check
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            log.warn("File validation failed: invalid filename");
            throw new IllegalArgumentException("Invalid file name");
        }

        // 4️⃣ Extension check
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("File validation failed: invalid extension={}", extension);
            throw new IllegalArgumentException(
                    "Only JPG, PNG, and WEBP files are allowed"
            );
        }

        // 5️⃣ MIME type check (security-critical)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            log.warn("File validation failed: invalid contentType={}", contentType);
            throw new IllegalArgumentException("Invalid file content type");
        }

        log.debug("File validation successful: {}", originalFilename);
    }

    /**
     * Extracts file extension from filename.
     -----------------
     * NOTE:
     * - Private helper
     * - NOT part of validation API
     */
    private static String extractExtension(String filename) {
        return filename
                .substring(filename.lastIndexOf('.') + 1)
                .toLowerCase();
    }
}
