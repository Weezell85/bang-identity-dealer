package com.family.bang.api;

import com.family.bang.game.GameService;
import com.family.bang.game.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService games;

    public GameController(GameService games) { this.games = games; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameService.CreatedGame create() { return games.createGame(); }

    @PostMapping("/{gameCode}/players")
    @ResponseStatus(HttpStatus.CREATED)
    public GameService.JoinedPlayer join(@PathVariable String gameCode, @Valid @RequestBody JoinRequest request) {
        return games.join(gameCode, request.playerName());
    }

    @GetMapping("/{gameCode}/lobby")
    public LobbyResponse lobby(@PathVariable String gameCode,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        return new LobbyResponse(games.lobby(gameCode, bearer(authorization)));
    }

    @PostMapping("/{gameCode}/deal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deal(@PathVariable String gameCode,
                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        games.deal(gameCode, bearer(authorization));
    }

    @GetMapping("/{gameCode}/role")
    public RoleResponse role(@PathVariable String gameCode,
                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        return new RoleResponse(games.ownRole(gameCode, bearer(authorization)));
    }

    private static String bearer(String header) {
        if (header == null || !header.startsWith("Bearer ") || header.length() == 7) {
            throw new com.family.bang.game.GameException(HttpStatus.UNAUTHORIZED, "A bearer token is required");
        }
        return header.substring(7);
    }

    public record JoinRequest(@NotBlank @Size(max = 40) String playerName) {}
    public record LobbyResponse(List<String> players) {}
    public record RoleResponse(Role role) {}
}
