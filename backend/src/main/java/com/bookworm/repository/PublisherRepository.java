package com.bookworm.repository;

import com.bookworm.domain.Publisher;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    List<Publisher> findAllByOrderByNameAsc();
}
