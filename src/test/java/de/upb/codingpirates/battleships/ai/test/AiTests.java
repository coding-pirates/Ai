package de.upb.codingpirates.battleships.ai.test;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.util.Client;
import de.upb.codingpirates.battleships.logic.util.Point2D;
import de.upb.codingpirates.battleships.logic.util.ShipType;
import de.upb.codingpirates.battleships.logic.util.Shot;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class AiTests {
    //added first Tests for placing Shots Method
    @Test
    public void sort_The_Sunk_Test() {
        Ai ai = new Ai();
        Collection<Shot> sunk = new ArrayList<>();
        Shot s1 = new Shot(1, new Point2D(0, 0));
        Shot s2 = new Shot(1, new Point2D(1, 0));
        Shot s3 = new Shot(1, new Point2D(1, 1));
        Shot s4 = new Shot(1, new Point2D(3, 4));
        Shot s5 = new Shot(1, new Point2D(4, 4));

        sunk.add(s1);
        sunk.add(s2);
        sunk.add(s3);
        sunk.add(s4);
        sunk.add(s5);

        ai.setSunk(sunk);
        HashMap<Integer, LinkedList<Shot>> sorted = ai.sortTheSunk();
    }

    @Test
    public void count_sunk_ships_Test() {
        Ai ai = new Ai();
        LinkedList<Shot> sunk = new LinkedList<>();
        Shot s1 = new Shot(1, new Point2D(0, 0));
        Shot s6 = new Shot(1, new Point2D(5, 4));
        Shot s2 = new Shot(1, new Point2D(1, 0));
        Shot s3 = new Shot(1, new Point2D(1, 1));
        Shot s4 = new Shot(1, new Point2D(3, 4));
        Shot s5 = new Shot(1, new Point2D(4, 4));

        sunk.add(s1);
        sunk.add(s2);
        sunk.add(s6);
        sunk.add(s3);
        sunk.add(s4);
        sunk.add(s5);

        ai.setSunk(sunk);


        LinkedList<LinkedList<Point2D>> ships = ai.findSunkenShips(1,  sunk);


    }


    @Test
    public void getRandomPoint2D_Test() {
        Ai ai = new Ai();

        Point2D randP = ai.getRandomPoint2D();
        assertTrue(randP instanceof Point2D);
    }

    @Test
    public void placing_Shots_randomly_on_field_with_4_Clients_Test() {
        Ai ai = new Ai();
        //set parameters
        Client a = new Client(1, "a");
        Client b = new Client(2, "b");
        Client c = new Client(3, "c");
        Client d = new Client(4, "d");
        ArrayList<Client> clientList = new ArrayList<>();
        clientList.add(a);
        clientList.add(b);
        clientList.add(c);
        clientList.add(d);

        ai.setClientArrayList(clientList);
        ai.setWidth(100);
        ai.setHeight(100);
        ai.setShotCount(10000);

        Collection<Shot> hits = Collections.EMPTY_LIST;
        Map<Integer, Integer> points = Collections.emptyMap();
        Collection<Shot> sunk = Collections.EMPTY_LIST;
        ai.updateNotification = new PlayerUpdateNotification(hits, points, sunk);
        //System.out.println(ai.updateNotification.getHits());
        System.out.println("Test: calls placeShots");
        try {
            ai.placeShotsRandom();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collection<Shot> shots = ai.shotsRequest.getShots();
        for (Shot i : shots) {

            System.out.print(i.getPosition().getX() + ",");
            System.out.println(i.getPosition().getY());
            System.out.println(i.getClientId());
            System.out.println();
        }

        assertNotNull(ai.shotsRequest);
        assertTrue(ai.shotsRequest instanceof Message);
        assertTrue(ai.shotsRequest instanceof ShotsRequest);
        assertEquals(ai.getShotCount(), ai.shotsRequest.getShots().size());
        //Map<Integer, ShipType> shipTypes;

        //Configuration config = new Configuration(5,10, 10, 3, 1, 5, 1000, 1000,  )
    }

    @Test
    public void place_Ship_should_return_random_Map_Integer_PlacementInfo() {
        Ai ai = new Ai();
        ai.setHeight(8);
        ai.setWidth(8);
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
        try {
            ai.placeShips();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(ai.getPositions().keySet().size() == ai.getPositions().values().size());
        assertFalse(ai.getPositions().values().isEmpty());
        assertFalse(ai.getPositions().values().isEmpty());

        //in that case 3 entrys
        assertTrue(ai.getPositions().size() == 3);


    }

}
