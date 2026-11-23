package com.bookworm.repository;

import com.bookworm.domain.FollowedAuthor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowedAuthorRepository extends JpaRepository<FollowedAuthor, FollowedAuthor.Key> {
    List<FollowedAuthor> findByUserIdOrderByAuthorIdAsc(Long userId);

    boolean existsByUserIdAndAuthorId(Long userId, Long authorId);

    void deleteByUserIdAndAuthorId(Long userId, Long authorId);
}
