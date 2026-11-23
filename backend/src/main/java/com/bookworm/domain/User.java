package com.bookworm.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String phone;

    /** Cached balance. The gift_point_txns ledger is the source of truth. */
    @Column(name = "gift_points", nullable = false)
    private int giftPoints;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
