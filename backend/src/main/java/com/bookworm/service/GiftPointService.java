package com.bookworm.service;

import com.bookworm.domain.GiftPointTxn;
import com.bookworm.domain.Order;
import com.bookworm.domain.PointTxnType;
import com.bookworm.domain.User;
import com.bookworm.dto.Dto.GiftPointTxnDto;
import com.bookworm.dto.Dto.GiftPointsDto;
import com.bookworm.dto.Dto.PageResponse;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.GiftPointTxnRepository;
import com.bookworm.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GiftPointService {

    private final GiftPointTxnRepository txns;
    private final UserRepository users;

    public GiftPointService(GiftPointTxnRepository txns, UserRepository users) {
        this.txns = txns;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public GiftPointsDto get(Long userId, int page, int size) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Unknown user"));
        var result = txns.findByUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new GiftPointsDto(user.getGiftPoints(), PageResponse.of(result.map(t -> new GiftPointTxnDto(
                t.getId(), t.getOrder() == null ? null : t.getOrder().getId(), t.getPoints(), t.getType(),
                t.getCreatedAt()))));
    }

    /**
     * Changes the balance and writes the ledger entry. The caller must hold the user row lock
     * ({@link UserRepository#findByIdForUpdate}). The balance never goes below zero.
     *
     * @return the points actually applied (may be smaller in magnitude than requested for a debit)
     */
    public int apply(User lockedUser, Order order, int points, PointTxnType type) {
        int applied = points;
        if (lockedUser.getGiftPoints() + points < 0) {
            applied = -lockedUser.getGiftPoints();
        }
        if (applied == 0) {
            return 0;
        }
        lockedUser.setGiftPoints(lockedUser.getGiftPoints() + applied);
        GiftPointTxn t = new GiftPointTxn();
        t.setUser(lockedUser);
        t.setOrder(order);
        t.setPoints(applied);
        t.setType(type);
        txns.save(t);
        return applied;
    }
}
