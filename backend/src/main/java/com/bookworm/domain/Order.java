package com.bookworm.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "PurchaseOrder")
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "ship_first_name", nullable = false)
    private String shipFirstName;
    @Column(name = "ship_last_name", nullable = false)
    private String shipLastName;
    @Column(name = "ship_email")
    private String shipEmail;
    @Column(name = "ship_phone")
    private String shipPhone;
    @Column(name = "ship_address_line", nullable = false)
    private String shipAddressLine;
    @Column(name = "ship_city", nullable = false)
    private String shipCity;
    @Column(name = "ship_state", nullable = false)
    private String shipState;
    @Column(name = "ship_pin_code", nullable = false)
    private String shipPinCode;
    @Column(name = "ship_country", nullable = false)
    private String shipCountry;

    @Column(nullable = false)
    private BigDecimal subtotal;
    @Column(nullable = false)
    private BigDecimal tax;
    @Column(name = "delivery_charge", nullable = false)
    private BigDecimal deliveryCharge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "coupon_discount", nullable = false)
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    @Column(name = "points_redeemed", nullable = false)
    private int pointsRedeemed;
    @Column(name = "points_discount", nullable = false)
    private BigDecimal pointsDiscount = BigDecimal.ZERO;
    @Column(nullable = false)
    private BigDecimal total;
    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 50)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order")
    @org.hibernate.annotations.BatchSize(size = 50)
    @OrderBy("id ASC")
    private List<Payment> payments = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}
