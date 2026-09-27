package com.chess.auth.service;

import com.chess.auth.dto.GameResultUpdateRequest;
import com.chess.auth.dto.UpdateProfileRequest;
import com.chess.auth.model.User;
import com.chess.auth.repository.UserRepository;
import com.chess.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found: " + id));
    }

    @Transactional
    public User updateProfile(UUID id, UpdateProfileRequest request) {
        User user = getById(id);

        if (request.username() != null && !request.username().isBlank()
                && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw ApiException.conflict("Username is already taken");
            }
            user.setUsername(request.username());
        }

        if (request.email() != null && !request.email().isBlank()
                && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw ApiException.conflict("Email is already registered");
            }
            user.setEmail(request.email());
        }

        return userRepository.save(user);
    }

    @Transactional
    public User applyGameResult(UUID id, GameResultUpdateRequest request) {
        User user = getById(id);

        user.setEloRating(request.eloRating());
        user.setGamesPlayed(user.getGamesPlayed() + 1);

        switch (request.result()) {
            case WON -> user.setGamesWon(user.getGamesWon() + 1);
            case LOST -> user.setGamesLost(user.getGamesLost() + 1);
            case DRAWN -> user.setGamesDrawn(user.getGamesDrawn() + 1);
        }

        return userRepository.save(user);
    }
}
