package com.manish.smartcart.order.service;

import com.manish.smartcart.order.dto.ShipmentRequest;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.model.OrderItem;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates shipments on Shiprocket and retrieves AWB (Airway Bill) codes.
 * <p>
 * LAYMAN: Like registering a parcel at the courier office and getting a
 * tracking slip (AWB). That slip number is what the customer uses to track
 * their package on Shiprocket's website.
 * <p>
 * PRODUCTION EDGE CASES HANDLED:
 * 1. Map.of() only supports 10 entries — we use HashMap (no limit).
 * 2. Token expired mid-request (401) → auto-refresh and retry ONCE.
 * 3. Shiprocket API down (5xx) → clear error, do not crash the app.
 * 4. AWB not assigned immediately (async courier allocation) → poll once after 2s.
 * 5. Pincode not serviceable → fail before creating order (save Shiprocket quota).
 * 6. Duplicate order_id → use our human-readable orderNumber (globally unique).
 * 7. Null/blank shipping fields → safe fallbacks prevent NPE.
 * 8. Deleted products in order items → use frozen snapshots, not live data.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiprocketOrderService{

    @Qualifier("shiprocketRestClient")
    private final RestClient restClient;

    private final ShiprocketAuthService shiprocketAuthService;

    /**
     * Main entry point. Creates a Shiprocket order and returns the AWB code.
     * Handles token expiry automatically via one retry with a fresh token.
     */
    public String createShipmentAndGetAwb(Order order, ShipmentRequest request){
        String token = shiprocketAuthService.getToken();
        try{
            return attemptCreate(order, request, token);
        }catch (TokenExpiredException e){
            // 401 from Shiprocket: our Redis-cached token expired just before the call.
            // Refresh once and retry. If it fails again, it's a real error — don't retry infinitely.
            log.warn("Shiprocket token expired mid-request for Order#{}. Refreshing and retrying...",
                    order.getOrderNumber());
            String freshToken = shiprocketAuthService.forceRefreshToken();
            return attemptCreate(order, request, freshToken);
        }catch (BusinessLogicException e){
            throw e; // Re-throw our own clean exceptions as-is
        } catch (Exception e) {
            // Unexpected failure (network timeout, JSON parse error, etc.)
            log.error("Unexpected error creating Shiprocket shipment for Order#{}", order.getOrderNumber(), e);
            throw new BusinessLogicException(
                    "Shiprocket shipment creation failed unexpectedly. Please retry or create manually.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PRIVATE INTERNAL METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Makes the actual HTTP call to Shiprocket with the given token.
     * Throws TokenExpiredException on 401 so the caller can refresh and retry.
     */
    private String attemptCreate(Order order, ShipmentRequest request, String token) {
        // STEP 1: Serviceability check before wasting a Shiprocket order creation call.
        checkServiceability(order.getShippingZipCode(), request.getWeightInKg(), token);

        // STEP 2: Build the payload.
        // WHY HashMap: Map.of() has a 10-entry Java hard limit. Our payload has 18 fields.
        Map<String, Object> payload = buildPayload(order, request);

        log.info("Calling Shiprocket API to create order for Order#{}", order.getOrderNumber());

        Map<String, Object> response;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = restClient.post()
                    .uri("/orders/create/adhoc")
                    .header("Authorization", "Bearer " + token)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            response = raw;
        }catch (HttpClientErrorException e){
            if(e.getStatusCode().value() == 401){
                // Signal the caller to refresh the token and retry
                throw new TokenExpiredException("Shiprocket 401: " + e.getMessage());
            }
            log.error("Shiprocket 4xx error for Order#{}: {}", order.getOrderNumber(), e.getResponseBodyAsString());
            throw new BusinessLogicException(
                    "Shiprocket rejected the shipment request (HTTP " + e.getStatusCode().value() + "). " +
                            "Check if order details are complete.");
        }catch (HttpServerErrorException e){
            log.error("Shiprocket 5xx error — their servers are down: {}", e.getStatusCode());
            throw new BusinessLogicException(
                    "Shiprocket API is currently unavailable (HTTP " + e.getStatusCode().value() + "). " +
                            "Please retry in a few minutes or attach AWB manually.");
        }

        // STEP 3: Extract AWB (with async fallback polling)
        return extractAwb(response, order.getOrderNumber(), token);
    }


    /**
     * Checks if any courier can deliver to the customer's pincode.
     * <p>
     * WHY: Some remote/rural pincodes have no serviceable couriers.
     * Failing here gives a clear, actionable error to the admin instead of
     * a cryptic rejection after Shiprocket creates the order but can't assign a courier.
     * <p>
     * RESILIENCE: If the serviceability API itself fails (Shiprocket internal error),
     * we log and proceed — better to attempt than to block on their API instability.
     */
    private void checkServiceability(String pincode, Double weight, String token) {
        if(pincode == null || pincode.isBlank()){
            log.warn("Pincode is blank. Skipping serviceability check.");
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/courier/serviceability/?pickup_postcode=110001&delivery_postcode={pin}&weight={w}&cod=0",
                            pincode, weight)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if(response != null){

                Object courierId = response.get("recommended_courier_id");

                if(courierId == null){
                    throw new BusinessLogicException(
                            "No courier available for pincode: " + pincode +
                                    ". Ask the customer to use a nearby alternative address.");
                }
            }
        }catch (BusinessLogicException e){
            throw e;
        }catch (Exception e){
            // Non-fatal — serviceability API failure must not block shipment creation
            log.warn("Shiprocket serviceability check failed for pincode {}. Proceeding anyway.", pincode, e);
        }
    }

    /**
     * Builds the complete Shiprocket order payload using our Order entity data.
     *
     * KEY DECISIONS:
     * - Uses order.getOrderNumber() as Shiprocket's order_id → globally unique, human-readable.
     * - Uses frozen shipping snapshots (not live user address) → consistent with checkout data.
     * - Uses frozen order item snapshots → safe even if product was deleted after checkout.
     * - safeGet() protects all optional string fields from NPE.
     */
    private Map<String, Object> buildPayload(Order order, ShipmentRequest request) {
        Map<String, Object> payload = new HashMap<>();

        // Order identity — Shiprocket will reject duplicate order_id values (idempotency)
        payload.put("order_id",    order.getOrderNumber());
        payload.put("order_date",  order.getOrderDate().toString());
        payload.put("pickup_location", "Primary"); // Warehouse name in your Shiprocket dashboard

        // Billing/Shipping — always use our frozen checkout snapshot, not the live user address
        // Reason: user may have changed address after placing the order
        payload.put("billing_customer_name", safeGet(order.getShippingFullName(), "Customer"));
        payload.put("billing_address",       safeGet(order.getShippingStreetAddress(), "N/A"));
        payload.put("billing_city",          safeGet(order.getShippingCity(), "N/A"));
        payload.put("billing_pincode",       safeGet(order.getShippingZipCode(), "000000"));
        payload.put("billing_state",         safeGet(order.getShippingState(), "N/A"));
        payload.put("billing_country",       safeGet(order.getShippingCountry(), "India"));
        payload.put("billing_email",         order.getUser().getEmail());
        payload.put("billing_phone",         safeGet(order.getShippingPhone(), "0000000000"));
        payload.put("shipping_is_billing",   true); // Ship-to == Bill-to for standard e-commerce

        // Line items — built from frozen snapshots (product may be soft-deleted by now)
        payload.put("order_items", buildLineItems(order));


        // Payment — always Prepaid (we use Razorpay, no COD)
        payload.put("payment_method", "Prepaid");
        payload.put("sub_total",      order.getTotalAmount());

        // Parcel dimensions — from admin's ShipmentRequest (accurate per package)
        payload.put("length",  request.getLengthCm());
        payload.put("breadth", request.getBreadthCm());
        payload.put("height",  request.getHeightCm());
        payload.put("weight",  request.getWeightInKg());

        return payload;
    }

    /**
     * Builds Shiprocket line items from our OrderItem frozen snapshots.
     * NEVER reads live product data — safe for deleted/renamed products.
     */
    private List<Map<String, Object>> buildLineItems(Order order){
        List<Map<String, Object>> items = new ArrayList<>();
        for(OrderItem item : order.getOrderItems()){
            Map<String, Object> line = new HashMap<>();
            line.put("name",          safeGet(item.getProductNameSnapshot(), "Product"));
            line.put("sku",           safeGet(item.getSkuSnapshot(), "SKU-UNKNOWN"));
            line.put("units",         item.getQuantity());
            line.put("selling_price", item.getPriceAtPurchase());
            items.add(line);
        }
        return  items;
    }

    /**
     * Extracts the AWB code from Shiprocket's response.
     *
     * ASYNC EDGE CASE: Shiprocket sometimes creates the order successfully
     * but hasn't assigned a courier/AWB yet (happens during peak traffic or remote pincodes).
     * We wait 2 seconds and poll once before giving up gracefully.
     */
    private String extractAwb(Map<String, Object> response, String orderNumber, String token) {
        if(response == null){
            throw new BusinessLogicException(
                    "Shiprocket returned an empty response for Order#" + orderNumber + ". Check Shiprocket dashboard.");
        }

        String awb = parseAwb(response);

        // Happy path — AWB assigned synchronously
        if(awb != null){
            log.info("Shiprocket AWB {} assigned for Order#{}", awb, orderNumber);
            return awb;
        }

        // ASYNC FALLBACK: Wait 2 seconds then poll Shiprocket for the AWB
        log.warn("AWB not immediately assigned for Order#{}. Polling in 2s...", orderNumber);
        try {
            Thread.sleep(2000);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }

        // Poll: GET /orders/show/{orderNumber}
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> polledResponse  = restClient.get()
                    .uri("/orders/show/{id}", orderNumber)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            String polledAwb = parseAwb(polledResponse);
            if (polledAwb != null) {
                log.info("Shiprocket AWB {} retrieved via polling for Order#{}", polledAwb, orderNumber);
                return polledAwb;
            }
        }catch (Exception e){
            log.error("Shiprocket AWB polling failed for Order#{}", orderNumber, e);
        }
        // Both attempts failed — give admin a clear action
        throw new BusinessLogicException(
                "Shiprocket accepted Order#" + orderNumber + " but hasn't assigned an AWB yet. " +
                        "Check Shiprocket dashboard and update the tracking number manually from the admin panel.");
    }


    /**
     * Safely parses the AWB code from a Shiprocket response map.
     * Returns null if AWB is absent, blank, or "0" (Shiprocket's "not assigned" value).
     */
    private String parseAwb(Map<String, Object> response) {
        if(response == null)return null;
        Object awbRaw = response.get("awb_code");
        if(awbRaw == null)return null;
        String awbCode = String.valueOf(awbRaw).trim();
        return (!awbCode.isBlank() && !awbCode.equals("0")) ? awbCode : null;
    }

    /** Null-safe string getter. Returns fallback if value is null or blank. */
    private String safeGet(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

}