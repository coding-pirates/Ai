package de.upb.codingpirates.battleships.ai.test;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.util.Client;
import de.upb.codingpirates.battleships.logic.util.Point2D;
import de.upb.codingpirates.battleships.logic.util.ShipType;
import de.upb.codingpirates.battleships.logic.util.Shot;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class AiTests {
    //create some needed objects and values
    @BeforeAll
    public static void create() {
        //todo implementation needed

    }

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
    public void get_Sunken_all_Clients() {
        Ai ai = new Ai();
        LinkedList<Shot> sunk = new LinkedList<>(); //shotsThisClient
        ai.setWidth(5);
        ai.setHeight(5);

        //nur bei Spieler 2 und 3 wurden Schiffe versenkt
        Shot s1 = new Shot(2, new Point2D(1, 3));
        Shot s2 = new Shot(2, new Point2D(1, 4));

        Shot s3 = new Shot(2, new Point2D(3, 3));
        Shot s4 = new Shot(2, new Point2D(4, 3));
        Shot s5 = new Shot(2, new Point2D(4, 4));

        Shot s6 = new Shot(3, new Point2D(1, 0));
        Shot s7 = new Shot(3, new Point2D(2, 0));
        Shot s8 = new Shot(3, new Point2D(2, 1));
        Shot s9 = new Shot(3, new Point2D(3, 0));
        sunk.add(s2);
        sunk.add(s1);
        sunk.add(s5);
        sunk.add(s4);
        sunk.add(s3);
        sunk.add(s6);
        sunk.add(s7);
        sunk.add(s8);
        sunk.add(s9);
        ai.setSunk(sunk);

        //shipConfig erstellen
        Map<Integer, ShipType> shipConfig = new HashMap<>();

        //ship 1
        Collection<Point2D> p1 = new ArrayList<>();
        p1.add(new Point2D(0, 0));
        p1.add(new Point2D(0, 1));

        ShipType sh1 = new ShipType(p1);

        //ship 2
        Collection<Point2D> p2 = new ArrayList<>();
        p2.add(new Point2D(0, 1));
        p2.add(new Point2D(0, 0));
        p2.add(new Point2D(1, 0));

        ShipType sh2 = new ShipType(p2);

        //ship 3
        Collection<Point2D> p3 = new ArrayList<>();
        p3.add(new Point2D(0, 0));
        p3.add(new Point2D(1, 0));
        p3.add(new Point2D(2, 0));
        p3.add(new Point2D(1, 1));

        ShipType sh3 = new ShipType(p3);

        shipConfig.put(1, sh1);
        shipConfig.put(2, sh2);
        shipConfig.put(3, sh3);

        ai.setShipConfig(shipConfig);

        ai.setSunk(sunk);
        ai.setSortedSunk(ai.sortTheSunk());
        assertEquals(2, ai.getSortedSunk().size()); //

        Map<Integer, LinkedList<Integer>> sunken = ai.getSunkenShipsAllClients();
        assertNotNull(sunken);
        assertEquals(2, sunken.size());
        assertEquals(2, sunken.get(2).size());
        assertEquals(1, sunken.get(3).size());

        assertEquals(1, (int) sunken.get(2).get(0));
        assertEquals(2, (int) sunken.get(2).get(1));
        assertEquals(3, (int) sunken.get(3).get(0));


    }

    @Test
    public void find_sunk_ships_one_Client_Test() {
        Ai ai = new Ai();
        LinkedList<Shot> sunk = new LinkedList<>(); //shotsThisClient
        ai.setWidth(5);
        ai.setHeight(5);

        //ship 1
        Collection<Point2D> p1 = new ArrayList<>();
        p1.add(new Point2D(0, 0));
        p1.add(new Point2D(0, 1));

        ShipType sh1 = new ShipType(p1);

        //ship 2
        Collection<Point2D> p2 = new ArrayList<>();
        p2.add(new Point2D(0, 1));
        p2.add(new Point2D(0, 0));
        p2.add(new Point2D(1, 0));

        ShipType sh2 = new ShipType(p2);

        //ship 3
        Collection<Point2D> p3 = new ArrayList<>();
        p3.add(new Point2D(0, 0));
        p3.add(new Point2D(1, 0));
        p3.add(new Point2D(2, 0));
        p3.add(new Point2D(1, 1));


        ShipType sh3 = new ShipType(p3);

        Map<Integer, ShipType> shipConfig = new HashMap<>();
        shipConfig.put(1, sh1);
        shipConfig.put(2, sh2);
        shipConfig.put(4, sh3);


        ai.setShipConfig(shipConfig);


        Shot s1 = new Shot(1, new Point2D(1, 3));
        Shot s2 = new Shot(1, new Point2D(1, 4));

        Shot s3 = new Shot(1, new Point2D(3, 3));
        Shot s4 = new Shot(1, new Point2D(4, 3));
        Shot s5 = new Shot(1, new Point2D(4, 4));

        Shot s6 = new Shot(1, new Point2D(1, 0));
        Shot s7 = new Shot(1, new Point2D(2, 0));
        Shot s8 = new Shot(1, new Point2D(2, 1));
        Shot s9 = new Shot(1, new Point2D(3, 0));
        sunk.add(s2);
        sunk.add(s1);
        sunk.add(s5);
        sunk.add(s4);
        sunk.add(s3);
        sunk.add(s6);
        sunk.add(s7);
        sunk.add(s8);
        sunk.add(s9);
        ai.setSunk(sunk);


        LinkedList<Integer> ships = ai.findSunkenShips(sunk);

        /*
        Shot s1 = new Shot(1, new Point2D(0, 0));
        Shot s6 = new Shot(1, new Point2D(5, 4));
        Shot s2 = new Shot(1, new Point2D(1, 0));
        Shot s3 = new Shot(1, new Point2D(1, 1));
        Shot s4 = new Shot(1, new Point2D(3, 4));
        Shot s5 = new Shot(1, new Point2D(4, 4));

         */
        /*
        Shot s1 = new Shot(1, new Point2D(1, 2));
        Shot s6 = new Shot(1, new Point2D(1, 3));
        Shot s2 = new Shot(1, new Point2D(1, 4));


        Shot s5 = new Shot(1, new Point2D(4, 2));
        Shot s7 = new Shot(1, new Point2D(4, 3));
        Shot s8 = new Shot(1, new Point2D(5, 2));
        Shot s9 = new Shot(1, new Point2D(6, 2));
        Shot s10 = new Shot(1, new Point2D(6, 3));

        Shot s11 = new Shot(1, new Point2D(10, 10));
        Shot s12 = new Shot(1, new Point2D(11, 10));
        Shot s13 = new Shot(1, new Point2D(12, 10));

         */





        /*
        sunk.add(s2);
        sunk.add(s13);
        sunk.add(s5);
        sunk.add(s1);
        sunk.add(s11);
        sunk.add(s6);
        sunk.add(s12);
        sunk.add(s7);
        sunk.add(s8);
        sunk.add(s9);
        sunk.add(s10);

         */


    }


    @Test
    public void getRandomPoint2D_Test() {
        Ai ai = new Ai();
        ai.setWidth(10);
        ai.setHeight(10);

        Point2D randP = ai.getRandomPoint2D();
        assertNotNull(randP);
        assertTrue(randP.getX() < ai.getWidth());
        assertTrue(randP.getY() < ai.getHeight());
        assertTrue(randP.getX() >= 0);
        assertTrue(randP.getY() >= 0);
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

        assertEquals(ai.getPositions().keySet().size(), ai.getPositions().values().size());
        assertFalse(ai.getPositions().values().isEmpty());
        assertFalse(ai.getPositions().values().isEmpty());

        //in that case 3 entrys
        assertEquals(3, ai.getPositions().size());


    }

}
