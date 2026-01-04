package com.manish.smartcart.util;

import org.springframework.web.multipart.MultipartFile;

public class FileValidator {

    public static void validateImage(MultipartFile file) {
        // 1. Check if empty
        if(file.isEmpty()){
            throw new RuntimeException("Cannot upload an empty file.");
        }
        // 2. Check File Size
        if(file.getSize()>AppConstants.MAX_FILE_SIZE){
            throw new RuntimeException("File is too large. Max limit is 5MB.");
        }
        // 3. Check Extension
        String originalFilename = file.getOriginalFilename();
        if(originalFilename==null || !originalFilename.contains(".")){
            throw new RuntimeException("Invalid file name.");
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if(!AppConstants.ALLOWED_EXTENSIONS.contains(extension)){
            throw new RuntimeException("Only JPG, PNG, and WEBP files are allowed.");
        }
        // 4. Check Content Type (MIME)
        String contentType = file.getContentType();
        if(contentType==null || !AppConstants.ALLOWED_MIME_TYPES.contains(contentType)){
            throw new RuntimeException("Invalid file content type.");
        }
    }
}
