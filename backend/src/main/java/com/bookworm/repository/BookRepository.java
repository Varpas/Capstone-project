package com.bookworm.repository;

import com.bookworm.domain.Book;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    /** Row-locks the books in id order, so concurrent orders cannot oversell or deadlock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id in :ids order by b.id")
    List<Book> lockAllById(@Param("ids") Collection<Long> ids);

    List<Book> findAllByOrderByPublishedDateDescIdDesc(Pageable pageable);

    List<Book> findAllByOrderByCopiesSoldDescIdAsc(Pageable pageable);

    /** Same category or same author, excluding the book itself, best sellers first. */
    @Query("""
            select distinct b from Book b left join b.categories c
            where b.id <> :bookId and (b.author.id = :authorId or c.id in :categoryIds)
            order by b.copiesSold desc, b.id asc
            """)
    List<Book> findRelated(@Param("bookId") Long bookId, @Param("authorId") Long authorId,
                           @Param("categoryIds") Collection<Long> categoryIds, Pageable pageable);

    /** Books sharing a category or author with past purchases, excluding the purchases themselves. */
    @Query("""
            select distinct b from Book b left join b.categories c
            where (b.author.id in :authorIds or c.id in :categoryIds) and b.id not in :excludedIds
            order by b.copiesSold desc, b.id asc
            """)
    List<Book> findRecommended(@Param("authorIds") Collection<Long> authorIds,
                               @Param("categoryIds") Collection<Long> categoryIds,
                               @Param("excludedIds") Collection<Long> excludedIds, Pageable pageable);
}
