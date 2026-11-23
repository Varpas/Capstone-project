package com.bookworm.repository;

import com.bookworm.domain.OrderItem;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Book ids ranked by units sold in paid orders since the given instant. */
    @Query("""
            select oi.book.id from OrderItem oi
            where oi.order.status = com.bookworm.domain.OrderStatus.PAID and oi.order.createdAt >= :since
            group by oi.book.id
            order by sum(oi.quantity) desc, oi.book.id asc
            """)
    List<Long> findTopSellerIds(@Param("since") Instant since, Pageable pageable);
}
