package de.upb.codingpirates.battleships.ai.test;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests if also negative ship positions leads to correct placement.
 * Tests if also ships positions with high negative values can be placed.
 */
public class Ai_ShipPlacement_1_Test {
    static Ai ai = new Ai();

    @BeforeAll
    public static void create() {
        ai.setHeight(8);
        ai.setWidth(8);

        Collection<Point2D> ship1 = new ArrayList<>();
        ship1.add(new Point2D(-5, -6));
        ship1.add(new Point2D(-4, -6));
        ship1.add(new Point2D(-4, -5));
        ship1.add(new Point2D(-4, -4));
        ship1.add(new Point2D(-3, -4));


        Collection<Point2D> ship2 = new ArrayList<>();
        ship2.add(new Point2D(-300, -300));
        ship2.add(new Point2D(-301, -300));

        Map<Integer, ShipType> ships = new HashMap<>();

        ships.put(1, new ShipType(ship1));
        ships.put(2, new ShipType(ship2));

        ai.setShips(ships);


    }

    @Test
    public void place_ships_test() throws IOException {
        ai.placeShips();
    }
}
