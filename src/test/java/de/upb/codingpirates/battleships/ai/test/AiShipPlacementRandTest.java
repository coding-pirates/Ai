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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AiShipPlacementRandTest {
    static Ai ai = new Ai();
    @BeforeAll
    public static void create(){

        ai.setHeight(6);
        ai.setWidth(6);
        Collection<Point2D> shipPos1 = new ArrayList<>();
        shipPos1.add(new Point2D(0, 0));
        shipPos1.add(new Point2D(1, 0));
        shipPos1.add(new Point2D(1, 1));

        Collection<Point2D> shipPos2 = new ArrayList<>();
        shipPos2.add(new Point2D(0, 0));
        shipPos2.add(new Point2D(1, 0));
        shipPos2.add(new Point2D(2, 0));
        shipPos2.add(new Point2D(2, 1));

        Collection<Point2D> shipPos3 = new ArrayList<>();
        shipPos3.add(new Point2D(0, 0));
        shipPos3.add(new Point2D(1, 0));
        shipPos3.add(new Point2D(1, 1));
        shipPos3.add(new Point2D(2, 1));
        shipPos3.add(new Point2D(2, 2));


        ShipType ship1 = new ShipType(shipPos1);
        ShipType ship2 = new ShipType(shipPos2);
        ShipType ship3 = new ShipType(shipPos3);


        Map<Integer, ShipType> shipConfig = new HashMap<Integer, ShipType>();
        shipConfig.put(1, ship1);
        shipConfig.put(2, ship2);
        shipConfig.put(3, ship3);

        ai.setShipConfig(shipConfig);

    }

    @Test
    public void place_Ship_should_return_random_Map_Integer_PlacementInfo() {

        try{
            ai.placeShips();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(ai.getPositions().keySet().size(), ai.getPositions().values().size());
        assertFalse(ai.getPositions().values().isEmpty());
        assertFalse(ai.getPositions().values().isEmpty());

        //in this case 3 entries
        assertEquals(3, ai.getPositions().size());


    }

}
