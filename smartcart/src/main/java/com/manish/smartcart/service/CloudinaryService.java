package com.manish.smartcart.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
// CONCEPT: @RequiredArgsConstructor generates a constructor for all `final`
// fields.
@RequiredArgsConstructor
@Service
public class CloudinaryService {

    // CONCEPT: `final` + @RequiredArgsConstructor = constructor injection.
    // This is superior to @Autowired field injection because the dependency
    // is guaranteed to exist at construction time (immutable, testable).
    private final Cloudinary cloudinary;

    /*
     * Uploads a file to Cloudinary and returns the secure CDN URL.
     *
     * CONCEPT: MultipartFile is Spring's abstraction over an HTTP file upload.
     * We call .getBytes() to read the file as raw bytes and hand it to the SDK.
     *
     * The "folder" option organises files under a path in Cloudinary dashboard.
     * This is NOT a local folder — it's just a logical grouping on Cloudinary.
     */

    public String upload(MultipartFile file, String folder) {
        try {
            log.info("Uploading file '{}' to Cloudinary folder '{}'", file.getOriginalFilename(), folder);

            // ObjectUtils.asMap() is a Cloudinary utility to build a params Map cleanly.
            // "folder" → organizes by product/seller/avatar etc. in your Cloudinary media
            // library
            // "resource_type" → "auto" lets Cloudinary detect if it's image/video/raw

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto");

            // upload() returns a Map with all details from Cloudinary:
            // "secure_url" → the permanent https:// CDN link ← this is what we store in DB
            // "public_id" → e.g. "products/abc123" ← use this to delete later

            @SuppressWarnings("unchecked")
            Map<String, Object> results = cloudinary.uploader().upload(file.getBytes(), uploadOptions);

            String secureUrl = (String) results.get("secure_url");

            log.info("Upload successful. CDN URL: {}", secureUrl);
            return secureUrl; // Store this in Product.imageUrl

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Image upload failed. Please try again.");
        }
    }

    /*
     * Deletes an image from Cloudinary by its public_id.
     *
     * CONCEPT: When a product is deleted or its image is replaced, we MUST
     * delete the old image from Cloudinary too. Otherwise, orphaned files
     * accumulate and waste storage quota.
     *
     * The publicId looks like: "products/my-usb-hub-abc123"
     * You extract it from the stored imageUrl or save it separately in the DB.
     */

    public void delete(String publicId) {
        try {
            log.info("Deleting image from Cloudinary, public_id: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted successfully from Cloudinary");

        } catch (Exception e) {
            log.warn("Could not delete image from Cloudinary (url: {}): {}", publicId, e.getMessage());
        }
    }

    /*
     * Extracts the Cloudinary public_id from a secure_url.
     *
     * CONCEPT: Cloudinary URLs have a predictable format:
     * https://res.cloudinary.com/{cloud-name}/image/upload/v{version}/{public_id}.{
     * extension}
     *
     * Example:
     * URL:
     * https://res.cloudinary.com/mycloud/image/upload/v1234567/products/usb-hub.jpg
     * public_id: products/usb-hub ← NO extension, everything after
     * /upload/v{version}/
     *
     * We need the public_id to call cloudinary.uploader().destroy()
     */
    // PUBLIC: called by ProductController after upload to extract publicId from CDN URL.
    // The publicId is returned to the client so they can use it for deletion later.
    public String extractPublicId(String secureUrl) {
        // Split on "/upload/" to isolate the path segment after it
        // e.g. "https://res.cloudinary.com/cloud/image/upload/v1234/products/img.jpg"
        // → ["https://res.cloudinary.com/cloud/image", "v1234/products/img.jpg"]
        String[] parts = secureUrl.split("/upload/");
        if (parts.length < 2) {
            throw new RuntimeException("Invalid Cloudinary URL: " + secureUrl);
        }
        // parts[1] = "v1234567890/products/usb-hub.jpg"
        // We strip the version prefix (v + digits + /) and the file extension
        String pathWithVersion = parts[1]; // "v1234567890/products/usb-hub.jpg"
        String withoutVersion = pathWithVersion.replaceAll("^v\\d+/", ""); // "products/usb-hub.jpg"
        int lastDot = withoutVersion.lastIndexOf('.');
        return lastDot != -1
                ? withoutVersion.substring(0, lastDot) // "products/usb-hub"
                : withoutVersion;
    }
}
