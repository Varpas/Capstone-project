package com.bookworm.repository;

import com.bookworm.domain.WishlistItem;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    @Query("select w from WishlistItem w join fetch w.book where w.user.id = :userId order by w.createdAt desc, w.id desc")
    List<WishlistItem> findByUser(@Param("userId") Long userId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @Query("select w.book.id from WishlistItem w where w.user.id = :userId and w.book.id in :bookIds")
    Set<Long> findBookIds(@Param("userId") Long userId, @Param("bookIds") Collection<Long> bookIds);

    @Modifying
    @Query("delete from WishlistItem w where w.user.id = :userId and w.book.id = :bookId")
    void deleteByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
