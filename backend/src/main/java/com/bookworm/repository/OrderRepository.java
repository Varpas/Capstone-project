package com.bookworm.repository;

import com.bookworm.domain.Order;
import com.bookworm.domain.OrderStatus;
import java.util.Collection;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /** Serialises payment and cancellation of the same order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :id and o.user.id = :userId")
    Optional<Order> findForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    @Query("""
            select distinct oi.book.id from OrderItem oi
            where oi.order.user.id = :userId and oi.order.status = com.bookworm.domain.OrderStatus.PAID
            """)
    Collection<Long> findPurchasedBookIds(@Param("userId") Long userId);
}
