package com.manish.smartcart.model.order;

import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.user.Users;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {


    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem>orderItems = new ArrayList<OrderItem>();

    private LocalDateTime orderDate;

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus; // PENDING, CONFIRMED, SHIPPED, DELIVERED

    // --- PHASE 1: SHIPPING SNAPSHOT (IMMUTABLE) ---
    // These fields "freeze" the data at the moment of checkout
    private String shippingFullName;
    private String shippingPhone;
    private String shippingStreetAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;
    private String shippingCountry;
}
