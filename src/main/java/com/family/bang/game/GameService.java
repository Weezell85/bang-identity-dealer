package com.family.bang.game;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final SecureRandom random;

    public GameService() { this(new SecureRandom()); }

    GameService(SecureRandom random) { this.random = random; }

    public CreatedGame createGame() {
        String code;
        Game game;
        do {
            code = randomCode();
            game = new Game(code, token());
        } while (games.putIfAbsent(code, game) != null);
        return new CreatedGame(game.code, game.hostToken);
    }

    public JoinedPlayer join(String code, String rawName) {
        Game game = game(code);
        String name = rawName.trim();
        synchronized (game) {
            if (game.dealt) throw conflict("Roles have already been dealt");
            if (game.players.size() >= 7) throw conflict("The game is full");
            if (game.players.stream().anyMatch(p -> p.name.equalsIgnoreCase(name))) {
                throw conflict("That player name is already in use");
            }
            Player player = new Player(name, token());
            game.players.add(player);
            return new JoinedPlayer(player.name, player.token);
        }
    }

    public List<String> lobby(String code, String token) {
        Game game = game(code);
        synchronized (game) {
            requireParticipant(game, token);
            return game.players.stream().map(p -> p.name).toList();
        }
    }

    public void deal(String code, String hostToken) {
        Game game = game(code);
        synchronized (game) {
            if (!game.hostToken.equals(hostToken)) throw unauthorized();
            if (game.dealt) throw conflict("Roles have already been dealt");
            List<Role> roles;
            try {
                roles = RoleDistribution.forPlayerCount(game.players.size());
            } catch (IllegalArgumentException e) {
                throw conflict(e.getMessage());
            }
            Collections.shuffle(roles, random);
            for (int i = 0; i < roles.size(); i++) game.players.get(i).role = roles.get(i);
            game.dealt = true;
        }
    }

    public Role ownRole(String code, String playerToken) {
        Game game = game(code);
        synchronized (game) {
            Player player = game.players.stream().filter(p -> p.token.equals(playerToken)).findFirst()
                    .orElseThrow(GameService::unauthorized);
            if (!game.dealt) throw conflict("Roles have not been dealt yet");
            return player.role;
        }
    }

    private void requireParticipant(Game game, String token) {
        if (!game.hostToken.equals(token) && game.players.stream().noneMatch(p -> p.token.equals(token))) {
            throw unauthorized();
        }
    }

    private Game game(String rawCode) {
        Game game = games.get(rawCode.toUpperCase());
        if (game == null) throw new GameException(HttpStatus.NOT_FOUND, "Game not found");
        return game;
    }

    private String randomCode() {
        StringBuilder value = new StringBuilder(6);
        for (int i = 0; i < 6; i++) value.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        return value.toString();
    }

    private static String token() { return UUID.randomUUID().toString(); }
    private static GameException unauthorized() { return new GameException(HttpStatus.UNAUTHORIZED, "Invalid token"); }
    private static GameException conflict(String message) { return new GameException(HttpStatus.CONFLICT, message); }

    public record CreatedGame(String gameCode, String hostToken) {}
    public record JoinedPlayer(String playerName, String playerToken) {}
}
