package com.manish.smartcart.service;

import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.repository.OrderRepository;
import com.opencsv.CSVWriter;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerAnalyticsExportService {

    private final OrderRepository orderRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate; // NEW: Programmatic Transactions

    /**
     * Returns a non-blocking StreamingResponseBody.
     * We manually inject a TransactionTemplate to ensure the background thread
     * holds the database cursor open!
     */
    public StreamingResponseBody exportOrdersToCsvStream(Long sellerId) {
        log.info("Initiating high-performance CSV stream for Seller ID: {}", sellerId);

        return outputStream -> {

            // 🛑 CRITICAL: Write the UTF-8 Byte Order Mark (BOM) outside the transaction
            outputStream.write(new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF });

            // Ensure the background thread executes inside a strict Read-Only Transaction
            transactionTemplate.setReadOnly(true);
            transactionTemplate.executeWithoutResult(status -> {

                try (
                        OutputStreamWriter osWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                        CSVWriter csvWriter = new CSVWriter(osWriter)
                ) {
                    // 1. Write the Header Row
                    String[] header = { "Order ID", "Date", "Customer ID", "Items Count", "Status", "Razorpay Payment ID" };
                    csvWriter.writeNext(header);

                    // 2. Open the DB Stream (Fetches 500 rows at a time from PostgreSQL)
                    try (Stream<Order> orderStream = orderRepository.streamBySellerIdAndOrderStatus(sellerId, OrderStatus.DELIVERED)) {

                        orderStream.forEach(order -> {
                            String[] data = {
                                    order.getId().toString(),
                                    order.getOrderDate().toString(),
                                    order.getUser().getId().toString(),
                                    String.valueOf(order.getOrderItems().size()),
                                    order.getOrderStatus().name(),
                                    order.getRazorpayPaymentId() != null ? order.getRazorpayPaymentId() : "N/A"
                            };
                            csvWriter.writeNext(data);

                            // 🧹 DETACH ENTITY: Forces Hibernate to throw away the object from memory instantly
                            entityManager.detach(order);
                        });
                    }

                    log.info("Successfully completed high-performance CSV stream for Seller ID: {}", sellerId);

                } catch (Exception e) {
                    log.error("Fatal error during CSV streaming for Seller ID: {}", sellerId, e);
                    // The stream is broken, usually means the client canceled the download early.
                }
            });
        };
    }
}
