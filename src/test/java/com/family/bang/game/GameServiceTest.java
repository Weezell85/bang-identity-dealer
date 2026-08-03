package com.family.bang.game;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {
    private final GameService service = new GameService(new SecureRandom());

    @Test
    void playersCanJoinAndLobbyContainsNamesOnly() {
        var game = service.createGame();
        var alice = service.join(game.gameCode(), " Alice ");
        var bob = service.join(game.gameCode(), "Bob");

        assertThat(service.lobby(game.gameCode(), alice.playerToken())).containsExactly("Alice", "Bob");
        assertThat(alice.playerToken()).isNotEqualTo(bob.playerToken());
        assertThatThrownBy(() -> service.join(game.gameCode(), "alice"))
                .isInstanceOf(GameException.class).hasMessageContaining("already in use");
    }

    @Test
    void listsExistingGameCodesInSortedOrder() {
        var first = service.createGame();
        var second = service.createGame();

        assertThat(service.gameCodes()).containsExactlyInAnyOrder(first.gameCode(), second.gameCode());
        assertThat(service.gameCodes()).isSorted();
    }

    @Test
    void onlyHostCanDealAndDealCanHappenOnlyOnce() {
        var game = service.createGame();
        List<GameService.JoinedPlayer> players = joinPlayers(game.gameCode(), 4);

        assertThatThrownBy(() -> service.deal(game.gameCode(), players.get(0).playerToken()))
                .isInstanceOf(GameException.class).hasMessage("Invalid token");
        service.deal(game.gameCode(), game.hostToken());
        assertThatThrownBy(() -> service.deal(game.gameCode(), game.hostToken()))
                .isInstanceOf(GameException.class).hasMessageContaining("already been dealt");
    }

    @Test
    void playersCanRetrieveOnlyTheRoleForTheirToken() {
        var game = service.createGame();
        List<GameService.JoinedPlayer> players = joinPlayers(game.gameCode(), 4);
        service.deal(game.gameCode(), game.hostToken());

        List<Role> roles = players.stream().map(p -> service.ownRole(game.gameCode(), p.playerToken())).toList();
        assertThat(roles).containsExactlyInAnyOrder(Role.SHERIFF, Role.RENEGADE, Role.OUTLAW, Role.OUTLAW);
        assertThatThrownBy(() -> service.ownRole(game.gameCode(), game.hostToken()))
                .isInstanceOf(GameException.class).hasMessage("Invalid token");
    }

    @Test
    void requiresFourToSevenPlayers() {
        var game = service.createGame();
        joinPlayers(game.gameCode(), 3);
        assertThatThrownBy(() -> service.deal(game.gameCode(), game.hostToken()))
                .hasMessageContaining("4 through 7");
    }

    private List<GameService.JoinedPlayer> joinPlayers(String code, int count) {
        List<GameService.JoinedPlayer> result = new ArrayList<>();
        for (int i = 1; i <= count; i++) result.add(service.join(code, "Player " + i));
        return result;
    }
}
