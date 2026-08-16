package com.manish.smartcart.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manish.smartcart.order.dto.ShipmentRequest;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integrates with Delhivery's logistics API to auto-generate AWB (tracking numbers).
 * <p>
 * LAYMAN: Delhivery is India's largest courier. We tell them "ship this order to
 * this address" and they give us back an AWB slip number. Customer uses that number
 * to track their package on Delhivery's website.
 * <p>
 * KEY DIFFERENCE from Shiprocket:
 * - Static API token (no JWT expiry, no Redis caching needed &mdash; simpler).
 * - Body must be form-encoded: format=json&data={url-encoded JSON} &mdash; NOT raw JSON.
 * - Jackson ObjectMapper serializes payload &rarr; we URL-encode it via Spring's MultiValueMap.
 * <p>
 * STAGING vs PROD:
 * - Staging: <a href="https://staging-express.delhivery.com">...</a> (safe for testing, no real shipments)
 * - Prod:    <a href="https://track.delhivery.com">...</a>
 * Controlled entirely by delhivery.base-url in application.yml &mdash; zero code changes needed.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class DelhiveryShipmentService {

    @Qualifier("delhiveryRestClient")
    private final RestClient restClient;

    // Jackson — converts our payload Map to JSON string for the form body
    private final ObjectMapper objectMapper;

    @Value("${delhivery.api-token}")
    private String apiToken;

    @Value("${delhivery.warehouse-name:Primary}")
    private String warehouseName;

    @Value("${delhivery.mock:false}")
    private boolean mockMode;

    // ─── Public API ───────────────────────────────────────────────────────────
    /**
     * Creates a shipment on Delhivery and returns the AWB (waybill) number.
     * <p>
     * FLOW:
     * 1. [Optional] Check if mock mode enabled → return fake AWB instantly.
     * 2. Serviceability check: verify courier reaches the customer's pincode.
     * 3. Create shipment → Delhivery auto-assigns waybill → return it.
     *
     * @param order   the confirmed order entity (with shipping snapshot fields)
     * @param request admin-provided shipment metadata (dimensions, weight, etc.)
     * @return AWB tracking number (e.g. "1234567890")
     */
    public String createShipmentAndGetAwb(Order order, ShipmentRequest request){

        // MOCK MODE: Returns a fake AWB during local development.
        // Prevents real Delhivery API calls when staging credentials aren't set up yet.
        // Set delhivery.mock=false in production.
        if(mockMode){
            String fakeAwb = "MOCK-DLV-" + order.getOrderNumber();
            log.warn("[DELHIVERY][MOCK] Mock mode active. Returning fake AWB: {} for Order#{}",
                    fakeAwb, order.getOrderNumber());
            return fakeAwb;
        }

        log.info("[DELHIVERY] Starting shipment creation for Order#{} | Courier: Delhivery | Pincode: {}",
                order.getOrderNumber(), order.getShippingZipCode());

        // STEP 1: Fail fast — check if courier serves this pincode before API quota usage
        checkServiceability(order.getShippingZipCode());

        // STEP 2: Build payload and call Delhivery create.json
        return createShipment(order, request);
    }

    // ─── Private Implementation ───────────────────────────────────────────────
    /**
     * Verifies that Delhivery can deliver to the customer's pincode for Prepaid mode.
     * <p>
     * WHY: Remote pincodes (e.g., NE India, J&K, island territories) may not
     * be serviceable for express delivery. Checking upfront gives admin a clear
     * error ("pincode not supported") instead of Delhivery silently failing later.
     * <p>
     * RESILIENCE: Serviceability check failure is non-fatal. If Delhivery's
     * serviceability API is down, we log a warning and proceed. Better to attempt
     * shipment than to block the entire flow on a transient API outage.
     */
    private void checkServiceability(String pincode) {
        if(pincode == null || pincode.isBlank()){
            log.warn("[DELHIVERY][SERVICEABILITY] Pincode is blank — skipping check.");
            return;
        }
        try{
            log.debug("[DELHIVERY][SERVICEABILITY] Checking if pincode {} is serviceable...", pincode);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/c/api/pin-codes/json/?filter_codes={pin}&token={token}", pincode, apiToken)
                    .retrieve()
                    .body(Map.class);

            if(response == null){
                log.warn("[DELHIVERY][SERVICEABILITY] Null response for pincode {}. Proceeding anyway.", pincode);
                return;
            }

            // Response has "delivery_codes" list. Each entry has postal_code.prepaid = "Y"/"N"
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deliveryCodes =  (List<Map<String, Object>>) response.get("delivery_codes");

            if(deliveryCodes == null || deliveryCodes.isEmpty()){
                log.error("[DELHIVERY][SERVICEABILITY] Pincode {} not found in Delhivery network.", pincode);
                throw new BusinessLogicException(
                        "Pincode " + pincode + " is not serviceable by Delhivery. " +
                                "Please ask the customer to update their delivery address.");
            }

            // Check if Prepaid shipments are supported (we always use Prepaid since we use Razorpay)
            @SuppressWarnings("unchecked")
            Map<String, Object> postalCode =
                    (Map<String, Object>) deliveryCodes.get(0).get("postal_code");

            String prepaidStatus = postalCode != null ? (String) postalCode.get("prepaid") : "N";

            if (!"Y".equalsIgnoreCase(prepaidStatus)) {
                log.error("[DELHIVERY][SERVICEABILITY] Pincode {} does not support Prepaid delivery.", pincode);
                throw new BusinessLogicException(
                        "Prepaid delivery not available for pincode " + pincode +
                                ". Ask customer to use a nearby serviceable address.");
            }

            log.info("[DELHIVERY][SERVICEABILITY] Pincode {} is serviceable for Prepaid delivery. ✓", pincode);

        }catch (BusinessLogicException e){
            throw e; // Re-throw our clean errors as-is
        }catch (Exception e){
            // Non-fatal: log and proceed. Serviceability API failure should NOT block shipment.
            log.warn("[DELHIVERY][SERVICEABILITY] Check failed for pincode {} — proceeding with shipment. Reason: {}",
                    pincode, e.getMessage());
        }
    }

    /**
     * Calls Delhivery's POST /api/cmu/create.json to create the shipment.
     * <p>
     * DELHIVERY BODY FORMAT (non-standard — differs from Shiprocket):
     * Content-Type: application/x-www-form-urlencoded
     * Body: format=json&data={url_encoded_json_payload}
     * <p>
     * Spring's MultiValueMap + FORM_URLENCODED content type handles the URL-encoding
     * automatically — we just provide the serialized JSON string as the "data" value.
     */
    private String createShipment(Order order, ShipmentRequest request) {
        String jsonPayload = buildJsonPayload(order, request);

        // Wrap in Delhivery's required form structure: format=json&data=...
        // Spring's FormHttpMessageConverter automatically URL-encodes the values.
        MultiValueMap<String, String> formBody = new LinkedMultiValueMap<>();
        formBody.add("format", "json");
        formBody.add("data", jsonPayload);

        log.debug("[DELHIVERY][CREATE] Sending shipment payload for Order#{}: {}", order.getOrderNumber(), jsonPayload);

        try{
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/cmu/create.json")
                    .header("Authorization", "Token " + apiToken) // Static token — no expiry, no refresh needed
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formBody)
                    .retrieve()
                    .body(Map.class);
            log.debug("[DELHIVERY][CREATE] Raw response for Order#{}: {}", order.getOrderNumber(), response);
            return extractWaybill(response, order.getOrderNumber());
        }catch (HttpClientErrorException e){
            log.error("[DELHIVERY][CREATE] 4xx error for Order#{} | HTTP {} | Body: {}",
                    order.getOrderNumber(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessLogicException(
                    "Delhivery rejected the shipment request (HTTP " + e.getStatusCode().value() + "). " +
                            "Check warehouse name, pincode, and order details.");
        }catch (HttpServerErrorException e){
            log.error("[DELHIVERY][CREATE] 5xx error — Delhivery servers down | HTTP {} | Order#{}",
                    e.getStatusCode(), order.getOrderNumber());
            throw new BusinessLogicException(
                    "Delhivery API is currently unavailable (HTTP " + e.getStatusCode().value() + "). " +
                            "Please retry in a few minutes or enter the AWB manually.");
        }catch (Exception e){
            log.error("[DELHIVERY][CREATE] Unexpected error creating shipment for Order#{}",
                    order.getOrderNumber(), e);
            throw new BusinessLogicException(
                    "Unexpected error while creating Delhivery shipment. Please retry.");
        }
    }

    /**
     * Serializes our Order + ShipmentRequest into the JSON structure Delhivery expects.
     * <p>
     * DESIGN: Uses frozen snapshot fields from OrderItem (productNameSnapshot, skuSnapshot)
     * and from Order (shippingFullName, shippingStreetAddress, etc.) — safe even if the
     * product was deleted or the user changed their address after placing the order.
     * <p>
     * WHY HashMap (not Map.of()): Map.of() has a hard 10-entry limit in Java.
     * Our payload exceeds that. HashMap has no such limit.
     */
    private String buildJsonPayload(Order order, ShipmentRequest request) {
        // Build the shipment object — Delhivery uses a flat structure per shipment
        Map<String, Object> shipment = new HashMap<>();
        shipment.put("name",          safeGet(order.getShippingFullName(), "Customer"));
        shipment.put("add",           buildFullAddress(order)); // Delhivery wants one combined address string
        shipment.put("pin",           safeGet(order.getShippingZipCode(), "000000"));
        shipment.put("phone",         safeGet(order.getShippingPhone(), "0000000000"));
        shipment.put("order",         order.getOrderNumber()); // Must be globally unique — Delhivery rejects duplicates
        shipment.put("payment_mode",  "Prepaid");              // Always Prepaid — we use Razorpay, never COD
        shipment.put("total_amount",  order.getTotalAmount());
        shipment.put("cod_amount",    0);                      // Zero for Prepaid
        shipment.put("weight",        request.getWeightInKg());
        shipment.put("shipment_length", request.getLengthCm());
        shipment.put("shipment_width",  request.getBreadthCm());
        shipment.put("shipment_height", request.getHeightCm());
        shipment.put("products_desc", buildProductsDescription(order)); // Brief description for customs/NDR
        // Waybill intentionally omitted → Delhivery auto-assigns one (simpler and avoids allocation issues)

        // Top-level payload wrapping the shipment array + pickup location
        Map<String, Object> payload = new HashMap<>();
        payload.put("shipments",       List.of(shipment));
        payload.put("pickup_location", Map.of("name", warehouseName)); // Must match exact name in Delhivery dashboard

        try{
            return objectMapper.writeValueAsString(payload);
        }catch (JsonProcessingException e){
            log.error("[DELHIVERY][PAYLOAD] JSON serialization failed for Order#{}", order.getOrderNumber(), e);
            throw new BusinessLogicException("Failed to build Delhivery shipment payload.");
        }
    }

    /**
     * Combines the shipping address fields into one string — Delhivery's "add" field
     * expects a single address string, not individual city/state fields like Shiprocket.
     */
    private String buildFullAddress(Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.getShippingStreetAddress() != null) sb.append(order.getShippingStreetAddress());
        if (order.getShippingCity() != null)          sb.append(", ").append(order.getShippingCity());
        if (order.getShippingState() != null)         sb.append(", ").append(order.getShippingState());
        if (order.getShippingCountry() != null)       sb.append(", ").append(order.getShippingCountry());
        String result = sb.toString().trim();
        return result.isEmpty() ? "Address Not Available" : result;
    }

    /**
     * Builds a brief product summary string from frozen OrderItem snapshots.
     * Delhivery uses this for NDR (non-delivery) calls and customs declarations.
     * Max 3 items shown; extra items appended as "+N more".
     */
    private String buildProductsDescription(Order order) {
        var items = order.getOrderItems();
        if(items == null || items.isEmpty()) return "E-commerce Order";

        int limit = Math.min(items.size(), 3);
        StringBuilder desc = new StringBuilder();
        for(int i = 0; i < limit; ++i){
            if(i > 0) desc.append(", ");
            desc.append(items.get(i).getProductNameSnapshot());
        }
        if(items.size() > 3){
            desc.append(" +").append(items.size() - 3).append(" more");
        }
        return desc.toString();
    }

    /**
     * Parses the waybill from Delhivery's response.
     * <p>
     * SUCCESS RESPONSE STRUCTURE:
     * { "packages": [ { "status": "Success", "waybill": "1234567890", ... } ] }
     * <p>
     * ERROR RESPONSE STRUCTURE:
     * { "packages": [ { "status": "Error", "remarks": ["Order ID already exists"], ... } ] }
     * <p>
     * WHY DETAILED PARSING: Delhivery returns HTTP 200 even on logical errors.
     * We must inspect the payload to detect real failures.
     */
    private String extractWaybill(Map<String, Object> response, String orderNumber) {
        if(response == null){
            log.error("[DELHIVERY][WAYBILL] Null response from Delhivery for Order#{}", orderNumber);
            throw new BusinessLogicException(
                    "Delhivery returned an empty response for Order#" + orderNumber + ". Check Delhivery dashboard.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> packages = (List<Map<String, Object>>) response.get("packages");

        if (packages == null || packages.isEmpty()) {
            log.error("[DELHIVERY][WAYBILL] No 'packages' in response for Order#{}. Full response: {}",
                    orderNumber, response);
            throw new BusinessLogicException(
                    "Delhivery response missing package data for Order#" + orderNumber + ".");
        }

        Map<String, Object> pkg = packages.get(0);
        String status = (String) pkg.get("status");


        // Delhivery returns HTTP 200 even for business errors — check "status" field explicitly
        if (!"Success".equalsIgnoreCase(status)) {
            Object remarks = pkg.get("remarks"); // List<String> with error reason
            log.error("[DELHIVERY][WAYBILL] Shipment creation failed for Order#{} | Status: {} | Remarks: {}",
                    orderNumber, status, remarks);
            throw new BusinessLogicException(
                    "Delhivery rejected the shipment for Order#" + orderNumber + ". Reason: " + remarks +
                            ". Check Delhivery dashboard or retry with different details.");
        }

        String waybill = (String) pkg.get("waybill");
        if (waybill == null || waybill.isBlank()) {
            log.error("[DELHIVERY][WAYBILL] Status=Success but waybill is null for Order#{}", orderNumber);
            throw new BusinessLogicException(
                    "Delhivery confirmed the order but returned no waybill for Order#" + orderNumber +
                            ". Check Delhivery dashboard and enter AWB manually.");
        }

        log.info("[DELHIVERY][WAYBILL] ✓ AWB {} assigned for Order#{}", waybill, orderNumber);
        return waybill;
    }

    /** Null-safe string getter. Returns fallback if value is null or blank. */
    private String safeGet(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
