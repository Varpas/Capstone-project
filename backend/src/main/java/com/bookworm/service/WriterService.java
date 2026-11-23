package com.bookworm.service;

import com.bookworm.domain.Author;
import com.bookworm.domain.FollowedAuthor;
import com.bookworm.dto.Dto.AuthorDto;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.AuthorRepository;
import com.bookworm.repository.FollowedAuthorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "My Writers": the authors a user follows. */
@Service
@Transactional
public class WriterService {

    private final FollowedAuthorRepository followed;
    private final AuthorRepository authors;

    public WriterService(FollowedAuthorRepository followed, AuthorRepository authors) {
        this.followed = followed;
        this.authors = authors;
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> list(Long userId) {
        List<Long> ids = followed.findByUserIdOrderByAuthorIdAsc(userId).stream().map(FollowedAuthor::getAuthorId)
                .toList();
        return authors.findAllById(ids).stream()
                .sorted(java.util.Comparator.comparing(Author::getId))
                .map(a -> new AuthorDto(a.getId(), a.getName(), a.getBio(), a.getPhotoUrl(), true))
                .toList();
    }

    public void follow(Long userId, Long authorId) {
        if (!authors.existsById(authorId)) {
            throw ApiException.notFound("Author not found");
        }
        if (!followed.existsByUserIdAndAuthorId(userId, authorId)) {
            followed.save(new FollowedAuthor(userId, authorId));
        }
    }

    public void unfollow(Long userId, Long authorId) {
        followed.deleteByUserIdAndAuthorId(userId, authorId);
    }
}
