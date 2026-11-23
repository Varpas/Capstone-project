package com.bookworm.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "followed_authors")
@IdClass(FollowedAuthor.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class FollowedAuthor {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "author_id")
    private Long authorId;

    public FollowedAuthor(Long userId, Long authorId) {
        this.userId = userId;
        this.authorId = authorId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long userId;
        private Long authorId;
    }
}
