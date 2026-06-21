package com.manish.smartcart.sale.service;

import com.manish.smartcart.shared.enums.ApprovalStatus;
import com.manish.smartcart.shared.enums.EventStatus;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.sale.model.FlashSaleItem;
import com.manish.smartcart.sale.model.PlatformSaleEvent;
import com.manish.smartcart.product.model.ProductVariant;
import com.manish.smartcart.sale.repository.FlashSaleItemRepository;
import com.manish.smartcart.sale.repository.PlatformSaleEventRepository;
import com.manish.smartcart.product.repository.ProductVariantRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ─── WHAT IS THIS? ───────────────────────────────────────────────────────────
 * Parses a seller-uploaded CSV file and bulk-inserts Flash Sale items.

 * ─── EXPECTED CSV FORMAT (first row is the header, skipped): ─────────────────
 * variant_id, discount_percentage, max_units, max_units_per_user
 * 101,        20,                  50,        2
 * 102,        30,                  100,       1

 * ─── KEY DESIGN DECISIONS ────────────────────────────────────────────────────

 * 1. PARTIAL SUCCESS:
 *    If row 5 of 500 is invalid, rows 1-4 and 6-500 still get saved.
 *    One bad row does NOT abort the whole batch. This is how Shopify,
 *    Amazon Seller Central, and Flipkart's bulk import tools work.

 * 2. IDOR SECURITY CHECK (per row):
 *    IDOR = Insecure Direct Object Reference.
 *    Seller A cannot type Seller B's variant_id into the CSV to discount
 *    Seller B's products. We check ownership on every single row.

 * 3. BATCH INSERT:
 *    Instead of saving one item per row (N DB calls), we collect all valid
 *    items into a list and call saveAll() once (1 DB call). This is how you
 *    handle 500-row CSVs without hammering the database.

 * 4. ALL ITEMS START AS PENDING:
 *    Admin must approve them via reviewSellerSubmission() before they go live.
 *    This is the QC gate between Tier 2 (Seller) and the live sale.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerBulkSaleService {

    private final FlashSaleItemRepository flashSaleItemRepository;
    private final PlatformSaleEventRepository eventRepository;
    private final ProductVariantRepository variantRepository;

    // Column positions in the CSV (zero-indexed after header is skipped)
    private static final int COL_VARIANT_ID  = 0;
    private static final int COL_DISCOUNT    = 1;
    private static final int COL_MAX_UNITS   = 2;
    private static final int COL_MAX_PER_USER = 3;

    @Transactional
    public  String processBulkCsv(MultipartFile file, Long eventId, Long sellerId){
        // ─── FILE GUARDS ──────────────────────────────────────────────────────
        validateFile(file);

        // ─── EVENT GUARD ──────────────────────────────────────────────────────
        // Sellers cannot submit to a sale that has already ended.
        PlatformSaleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: ID " + eventId));

        if (event.getStatus() == EventStatus.ENDED) {
            throw new BusinessLogicException("Cannot submit items to an event that has already ended.");
        }

        // ─── CSV PARSING ──────────────────────────────────────────────────────
        List<FlashSaleItem> validItems = new ArrayList<>();
        int successCount = 0;
        int errorCount   = 0;
        int rowNumber    = 1;

        try(CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))){

            // 1. Read all rows into memory to facilitate bulk querying
            List<String[]> allRows = reader.readAll();
            if(allRows.isEmpty()){
                throw new BusinessLogicException("CSV file contains no data.");
            }

            // Remove the header row
            allRows.remove(0);

            // 2. Optimization: Extract all Variant IDs and perform a single pre-fetch query
            Set<Long> requestedVariantIds = extractVariantIds(allRows);
            Map<Long, ProductVariant> variantMap = variantRepository.findAllById(requestedVariantIds).stream()
                    .collect(Collectors.toMap(ProductVariant::getId, v -> v));

            for(String[] row : allRows){
                rowNumber++;
                try{
                    // Parse raw CSV strings into typed Java values
                    Long variantId = Long.parseLong(row[COL_VARIANT_ID].trim());
                    BigDecimal discount = BigDecimal.valueOf(Double.parseDouble(row[COL_DISCOUNT].trim()));
                    Integer maxUnits = Integer.parseInt(row[COL_MAX_UNITS].trim());
                    Integer maxPerUser = Integer.parseInt(row[COL_MAX_PER_USER].trim());
                    // ─── BUSINESS RULE VALIDATION ─────────────────────────────
                    validateBusinessRules(discount,maxUnits,maxPerUser);

                    ProductVariant variant = variantMap.get(variantId);
                    if(variant == null){
                        throw new IllegalArgumentException("Variant ID " + variantId + " not found.");
                    }
                    if(!variant.getProduct().getSellerId().equals(sellerId)){
                        throw new IllegalArgumentException("Security violation: Seller does not own Variant ID " + variantId);
                    }

                    validItems.add(
                            FlashSaleItem.builder()
                                    .platformSaleEvent(event)
                                    .productVariant(variant)
                                    .sellerId(sellerId)
                                    .discountPercentage(discount)
                                    .maxUnits(maxUnits)
                                    .maxUnitsPerUser(maxPerUser)
                                    .approvalStatus(ApprovalStatus.PENDING)
                                    .build());

                    successCount++;
                } catch (Exception rowEx) {
                    log.warn("Row {} skipped for Seller {}: {}", rowNumber, sellerId, rowEx.getMessage());
                    errorCount++;
                }
            }
            if(!validItems.isEmpty()){
                flashSaleItemRepository.saveAll(validItems);
                log.info("Bulk CSV processed for Seller {}. Saved: {}, Skipped: {}", sellerId, successCount, errorCount);
            }
        } catch (Exception e) {
            throw new BusinessLogicException("Failed to read CSV. Ensure it is a valid UTF-8 CSV: " + e.getMessage());
        }
        return String.format("✅ %d items submitted for Admin review. ❌ %d rows skipped.", successCount, errorCount);
    }


    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessLogicException("CSV file cannot be empty.");
        }
        String fileName = file.getOriginalFilename();
        if(fileName == null || !fileName.toLowerCase().endsWith(".csv")){
            throw new BusinessLogicException("Only .csv files are accepted.");
        }
    }

    private void validateBusinessRules(BigDecimal discount, Integer maxUnits, Integer maxPerUser) {
        if (discount.compareTo(BigDecimal.valueOf(5)) < 0 || discount.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("Discount must be 5%–90%.");
        }
        if (maxUnits < 1) {
            throw new IllegalArgumentException("maxUnits must be >= 1.");
        }
        if (maxPerUser < 1) {
            throw new IllegalArgumentException("maxUnitsPerUser must be >= 1.");
        }
    }

    private Set<Long> extractVariantIds(List<String[]> rows) {
        return rows.stream()
                .map(row -> {
                    try{
                        return Long.parseLong(row[COL_VARIANT_ID].trim());
                    }catch (NumberFormatException e){
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
