package com.family.bang.game;

import java.util.ArrayList;
import java.util.List;

final class Game {
    final String code;
    final String hostToken;
    final List<Player> players = new ArrayList<>();
    boolean dealt;

    Game(String code, String hostToken) {
        this.code = code;
        this.hostToken = hostToken;
    }
}
