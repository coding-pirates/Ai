package de.upb.codingpirates.battleships.ai.test;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.util.Point2D;
import de.upb.codingpirates.battleships.logic.util.ShipType;
import de.upb.codingpirates.battleships.logic.util.Shot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Ai2Test {
    static Ai ai = new Ai();



    @BeforeAll
    public static void create(){
        ai.setHeight(7);
        ai.setWidth(7);

        //shipConfig erstellen
        Map<Integer, ShipType> shipconfig = new HashMap<>();
        //ship1 id = 1
        ArrayList<Point2D> pos1 = new ArrayList<>();
        pos1.add(new Point2D(0,0));
        pos1.add(new Point2D(1,0));
        pos1.add(new Point2D(2,0));
        ShipType s1 = new ShipType(pos1);
        shipconfig.put(1, s1);
        //ship2 id = 2
        ArrayList<Point2D> pos2 = new ArrayList<>();
        pos2.add(new Point2D(0,0));
        pos2.add(new Point2D(1,0));
        pos2.add(new Point2D(1,1));
        ShipType s2 = new ShipType(pos2);
        shipconfig.put(2, s2);
        //ship3 id = 3
        ArrayList<Point2D> pos3 = new ArrayList<>();
        pos3.add(new Point2D(0,0));
        pos3.add(new Point2D(1,0));
        ShipType s3 = new ShipType(pos3);
        shipconfig.put(3, s3);
        //ship4 id = 4
        ArrayList<Point2D> pos4 = new ArrayList<>();
        pos4.add(new Point2D(0,0));
        pos4.add(new Point2D(0,1));
        pos4.add(new Point2D(0,2));
        ShipType s4 = new ShipType(pos4);
        shipconfig.put(4, s4);

        ai.setShipConfig(shipconfig);

        //sunk erstellen

        ArrayList<Shot> sunk = new ArrayList<>();
        sunk.add(new Shot(1, new Point2D(1,4)));
        sunk.add(new Shot(1, new Point2D(1,5)));
        sunk.add(new Shot(1, new Point2D(1,6)));
        sunk.add(new Shot(1, new Point2D(2,1)));
        sunk.add(new Shot(1, new Point2D(3,1)));
        sunk.add(new Shot(1, new Point2D(4,1)));
        sunk.add(new Shot(1, new Point2D(1,4)));
        sunk.add(new Shot(1, new Point2D(3,2)));
        sunk.add(new Shot(2, new Point2D(3,2)));
        sunk.add(new Shot(2, new Point2D(3,3)));
        ai.setSunk(sunk);
        ai.setSortedSunk(ai.sortTheSunk());





    }
    @Test
    public void create_Heatmap_one_Client_Test(){
        ai.createHeatmapOneClient(2);
    }

}
