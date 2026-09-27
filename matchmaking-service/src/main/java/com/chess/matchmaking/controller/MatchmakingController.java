package com.chess.matchmaking.controller;

import com.chess.common.security.RequestUserResolver;
import com.chess.matchmaking.dto.JoinQueueRequest;
import com.chess.matchmaking.dto.LeaveQueueRequest;
import com.chess.matchmaking.dto.QueueEntryResponse;
import com.chess.matchmaking.model.QueueEntry;
import com.chess.matchmaking.service.MatchmakingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/join")
    public ResponseEntity<QueueEntryResponse> join(
            @Valid @RequestBody JoinQueueRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = RequestUserResolver.resolve(userIdHeader, request.userId());
        QueueEntry entry = matchmakingService.joinQueue(userId, request.timeControl());
        return ResponseEntity.status(HttpStatus.CREATED).body(QueueEntryResponse.from(entry));
    }

    @DeleteMapping("/leave")
    public ResponseEntity<Void> leave(
            @Valid @RequestBody LeaveQueueRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = RequestUserResolver.resolve(userIdHeader, request.userId());
        matchmakingService.leaveQueue(userId);
        return ResponseEntity.noContent().build();
    }
}
