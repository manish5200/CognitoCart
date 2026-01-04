package com.manish.smartcart.model.order;

import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.user.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem>orderItems = new ArrayList<OrderItem>();

    private LocalDateTime orderDate;
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus; // PENDING, CONFIRMED, SHIPPED, DELIVERED

    @Embedded
    private Address shippingAddress;
}
