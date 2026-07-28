package com.family.bang.game;

import java.util.ArrayList;
import java.util.List;

public final class RoleDistribution {
    private RoleDistribution() {}

    public static List<Role> forPlayerCount(int playerCount) {
        if (playerCount < 4 || playerCount > 7) {
            throw new IllegalArgumentException("Bang supports 4 through 7 players");
        }
        List<Role> roles = new ArrayList<>(List.of(Role.SHERIFF, Role.RENEGADE));
        int deputies = playerCount >= 7 ? 2 : playerCount >= 5 ? 1 : 0;
        int outlaws = playerCount - 2 - deputies;
        for (int i = 0; i < deputies; i++) roles.add(Role.DEPUTY);
        for (int i = 0; i < outlaws; i++) roles.add(Role.OUTLAW);
        return roles;
    }
}
