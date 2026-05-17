package com.manish.smartcart.service;

import com.manish.smartcart.dto.admin.*;
import com.manish.smartcart.dto.seller.SellerSummaryResponse;
import com.manish.smartcart.enums.KycStatus;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.user.SellerProfile;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.repository.SellerProfileRepository;
import com.manish.smartcart.service.email.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    // CONCEPT: "Terminal States" — once an order reaches these, it is IMMUTABLE.
    // Just like you cannot un-deliver a package or un-cancel a ticket.
    // We use a Set for O(1) lookup instead of chaining multiple if-else conditions.
    private static final java.util.Set<OrderStatus> IMMUTABLE_STATES = java.util.Set.of(
            OrderStatus.DELIVERED,       // Job done — cannot go back
            OrderStatus.CANCELLED,       // Canceled orders stay canceled
            OrderStatus.RETURNED,        // Item is back at warehouse
            OrderStatus.REFUNDED         // Money returned — audit trail must not change
    );

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final EmailTemplateBuilder emailTemplateBuilder;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public DashboardResponse getAdminStats(int stockThreshold,int pageNumber,int pageSize) {
        // 1. Calculate Metrics
        BigDecimal revenue = orderRepository.calculateRevenue();// Using your JPQL query
        Long successful = orderRepository.countByOrderStatus(OrderStatus.DELIVERED);
        Long canceled = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);

        // 2. Fetch Low Stock Products (threshold < 5) and  Map to LowStockResponse DTO
        List<Product> products = productRepository.findByStockQuantityLessThan(stockThreshold);
        List<LowStockResponse>lowStockResponse = products.stream()
                .map(product -> new LowStockResponse(
                        product.getId(),
                        product.getProductName(),
                        product.getStockQuantity(),
                        product.getSellerId(),
                        product.getSku()
                )).toList();

        //3. Identify Top Sellers (Requesting top 5) using Pageable
        // Spring Data Pageable handles the LIMIT and OFFSET in the background
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        //Object[] array of Product Class and Total Quant -> from repo
        List<Object[]>topSellersRaw = productRepository.findToSellingProducts(pageable);
        List<TopProductDTO>topSellingProducts = topSellersRaw.stream()
                .map(result ->{
                       Product p = (Product) result[0];
                       Long totalQuantity =  (Long) result[1];
                       return new TopProductDTO(
                               p.getId(),
                               p.getProductName(),
                               p.getPrice(),
                               totalQuantity
                        );
                }).toList();

        // ADD THIS: Calculate the trend for the last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<DailyRevenueDTO> dailyTrend = orderRepository.getDailyRevenueTrend(sevenDaysAgo);
        // 4. Return the combined Dashboard
        return new DashboardResponse(
                revenue != null ? revenue : BigDecimal.ZERO,
                successful,
                canceled,
                lowStockResponse,
                topSellingProducts,
                dailyTrend
        );
    }

    // Play with the order
    @Transactional
    public Order changeTheStatusOfOrders(StatusChangeRequest statusChangeRequest) {
        Order order = orderRepository.findById(statusChangeRequest.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + statusChangeRequest.getOrderId()));

        // GUARD: Reject any modification attempt on terminal-state orders.
        // This prevents accidental data corruption from admin panel mistakes.

        if(IMMUTABLE_STATES.contains(order.getOrderStatus())) {
            throw new BusinessLogicException(
                    "Order #" + order.getId() + " is in a terminal state ("
                            + order.getOrderStatus() + ") and cannot be modified.");
        }

        try {
            // Convert String to Enum safely
            OrderStatus newStatus = OrderStatus.valueOf(
                    statusChangeRequest.getOrderStatus().toUpperCase());

            order.setOrderStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessLogicException(
                    "Invalid order status: '" + statusChangeRequest.getOrderStatus()
                    + "'. Valid values: " + java.util.Arrays.toString(OrderStatus.values()));
        }
        return orderRepository.save(order);
    }

    private static final Map<KycStatus, Set<KycStatus>> ALLOWED_KYC_TRANSITIONS = Map.of(
            KycStatus.PENDING,   Set.of(KycStatus.IN_REVIEW),
            KycStatus.IN_REVIEW, Set.of(KycStatus.VERIFIED, KycStatus.REJECTED),
            KycStatus.VERIFIED,  Set.of(KycStatus.SUSPENDED),
            KycStatus.REJECTED,  Set.of(KycStatus.IN_REVIEW),
            KycStatus.SUSPENDED, Set.of(KycStatus.VERIFIED)
    );

    @Transactional
    public SellerProfile updateSellerKyc(Long sellerId, KycUpdateRequest request) {
        SellerProfile seller = sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller profile not found for ID: " + sellerId));

        KycStatus current = seller.getKycStatus();
        KycStatus next = request.getStatus();

        Set<KycStatus>allowed = ALLOWED_KYC_TRANSITIONS.get(current);
        if(!allowed.contains(next)){
            throw new BusinessLogicException(
                    "Invalid KYC transition: " + current + " → " + next
                            + ". Allowed from " + current + ": " + allowed);
        }

        if((next == KycStatus.REJECTED || next == KycStatus.SUSPENDED)
        && (request.getAdminComment() == null || request.getAdminComment().isBlank())){
            throw new BusinessLogicException(
                    "adminComment is required when rejecting or suspending a seller.");
        }

        seller.setKycStatus(next);
        SellerProfile saved = sellerProfileRepository.save(seller);

        // Email fires
        sendKycDecisionEmail(saved, next, request.getAdminComment());
        return saved;
    }

    

    private void sendKycDecisionEmail(
            SellerProfile profile,
            KycStatus newStatus,
            String adminComment) {

        if(newStatus != KycStatus.VERIFIED
        && newStatus != KycStatus.SUSPENDED
        && newStatus != KycStatus.REJECTED){
            return;  // IN_REVIEW is intermediate — no email needed
        }

        try{
            String subject = switch(newStatus){
                case VERIFIED -> "🎉 KYC Approved — Welcome to CognitoCart Sellers!";
                case REJECTED ->  "❌ KYC Update — Action Required";
                case SUSPENDED ->  "⚠️ Your seller account has been suspended";
                default        -> "KYC Status Update";
            };

            String body = emailTemplateBuilder.buildSellerKycDecisionEmail(
                    profile.getUser().getFullName(),
                    newStatus.name(),
                    adminComment);
            emailService.sendMail(profile.getUser().getEmail(), subject, body, "CognitoCart KYC");
        }catch (Exception e){
            // Email failure must NOT rollback the DB transaction
            // The KYC status change already committed — just log
            log.warn("KYC decision email failed for sellerId={}: {}", profile.getId(), e.getMessage());
        }

    }

    @Transactional(readOnly = true)
    public List<SellerSummaryResponse> getAllSellers() {
        return sellerProfileRepository.findAllWithUser()
                .stream().map(this::toSellerSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<SellerSummaryResponse> getPendingKycSellers() {
        return sellerProfileRepository
                .findByKycStatusIn(List.of(KycStatus.PENDING, KycStatus.IN_REVIEW))
                .stream().map(this::toSellerSummary).toList();
    }

    private SellerSummaryResponse toSellerSummary(SellerProfile sp) {
        return SellerSummaryResponse.builder()
                .sellerId(sp.getId())
                .fullName(sp.getUser().getFullName())
                .email(sp.getUser().getEmail())
                .storeName(sp.getStoreName())
                .gstin(sp.getGstin())
                .kycStatus(sp.getKycStatus())
                .registeredAt(sp.getCreatedAt())
                .build();
    }

}
