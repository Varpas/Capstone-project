package com.bookworm.repository;

import com.bookworm.domain.GiftPointTxn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GiftPointTxnRepository extends JpaRepository<GiftPointTxn, Long> {
    Page<GiftPointTxn> findByUserId(Long userId, Pageable pageable);
}
