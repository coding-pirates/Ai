package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.ai.gameplay.StandardShotPlacementStrategy;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests if Ship placement also works with ships with no (0,0) point.
 */
public class Ai_ShipPlacement_3_Test {
    static AI ai = new AI("AiPlayer", StandardShotPlacementStrategy.RANDOM);

    @BeforeAll
    public static void create() {

        Collection<Point2D> s1 = new ArrayList<>();

        s1.add(new Point2D(0, 1));
        s1.add(new Point2D(1, 1));
        s1.add(new Point2D(1, 0));

        Map<Integer, ShipType> ships = new HashMap<>();
        ships.put(1, new ShipType(s1));

        ai.setConfiguration(new Configuration.Builder()
                .ships(ships)
                .width(10)
                .height(10)
                .build());
    }

    @Test
    public void place_ships_test() {
        ai.placeShips();

        assertEquals(ai.getPositions().size(), ai.getConfiguration().getShips().values().size());
    }
}
