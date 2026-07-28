package com.family.bang.game;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RoleDistributionTest {
    @Test
    void usesStandardBaseGameDistributions() {
        assertCounts(4, 1, 0, 2, 1);
        assertCounts(5, 1, 1, 2, 1);
        assertCounts(6, 1, 1, 3, 1);
        assertCounts(7, 1, 2, 3, 1);
    }

    private void assertCounts(int players, long sheriff, long deputy, long outlaw, long renegade) {
        Map<Role, Long> counts = RoleDistribution.forPlayerCount(players).stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertThat(counts).containsEntry(Role.SHERIFF, sheriff)
                .containsEntry(Role.OUTLAW, outlaw).containsEntry(Role.RENEGADE, renegade);
        assertThat(counts.getOrDefault(Role.DEPUTY, 0L)).isEqualTo(deputy);
    }
}
