package de.upb.codingpirates.battleships.ai.gameplay;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;

import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.logic.Shot;

public interface ShotPlacementStrategy {

    Collection<Shot> calculateShots(AI ai, int shotCount);

    String getName();

    @Nonnull
    static Optional<ShotPlacementStrategy> fromDifficultyLevel(final int difficultyLevel) {
        return StandardShotPlacementStrategy.fromDifficultyLevel(difficultyLevel);
    }
}
