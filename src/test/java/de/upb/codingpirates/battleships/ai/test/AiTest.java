package de.upb.codingpirates.battleships.ai.test;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;
import de.upb.codingpirates.battleships.logic.Shot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class AiTest {
    static Ai ai = new Ai();


    @BeforeAll
    public static void create() {


        //shipConfig erstellen
        Map<Integer, ShipType> shipconfig = new HashMap<>();
        //ship1 id = 1
        ArrayList<Point2D> pos1 = new ArrayList<>();
        pos1.add(new Point2D(0, 0));
        pos1.add(new Point2D(1, 0));
        pos1.add(new Point2D(2, 0));
        ShipType s1 = new ShipType(pos1);
        shipconfig.put(1, s1);
        //ship2 id = 2
        ArrayList<Point2D> pos2 = new ArrayList<>();
        pos2.add(new Point2D(0, 0));
        pos2.add(new Point2D(1, 0));
        pos2.add(new Point2D(1, 1));
        ShipType s2 = new ShipType(pos2);
        shipconfig.put(2, s2);
        //ship3 id = 3
        ArrayList<Point2D> pos3 = new ArrayList<>();
        pos3.add(new Point2D(0, 0));
        pos3.add(new Point2D(1, 0));
        ShipType s3 = new ShipType(pos3);
        shipconfig.put(3, s3);
        //ship4 id = 4
        ArrayList<Point2D> pos4 = new ArrayList<>();
        pos4.add(new Point2D(0, 0));
        pos4.add(new Point2D(0, 1));
        pos4.add(new Point2D(0, 2));
        ShipType s4 = new ShipType(pos4);
        shipconfig.put(4, s4);



        //Clients erstellen
        Client c1 = new Client(1, "c1");
        Client c2 = new Client(2, "c2");
        Client c3 = new Client(3, "c3");
        Collection<Client> clientList = new ArrayList<>();

        clientList.add(c1);
        clientList.add(c2);
        clientList.add(c3);


        //sunk erstellen

        ArrayList<Shot> sunk = new ArrayList<>();
        //Client 1's ships are already sunken
        sunk.add(new Shot(1, new Point2D(1, 4)));
        sunk.add(new Shot(1, new Point2D(1, 5)));
        sunk.add(new Shot(1, new Point2D(1, 6)));
        sunk.add(new Shot(1, new Point2D(2, 1)));
        sunk.add(new Shot(1, new Point2D(3, 1)));
        sunk.add(new Shot(1, new Point2D(4, 1)));
        sunk.add(new Shot(1, new Point2D(3, 2)));
        sunk.add(new Shot(1, new Point2D(3, 4)));
        sunk.add(new Shot(1, new Point2D(3, 5)));
        sunk.add(new Shot(1, new Point2D(5, 3)));
        sunk.add(new Shot(1, new Point2D(5, 4)));
        sunk.add(new Shot(1, new Point2D(6, 3)));


        sunk.add(new Shot(2, new Point2D(3, 2)));
        sunk.add(new Shot(2, new Point2D(3, 3)));
        sunk.add(new Shot(2, new Point2D(0, 1)));
        sunk.add(new Shot(2, new Point2D(1, 1)));
        sunk.add(new Shot(2, new Point2D(1, 0)));

        ai.setAiClientId(999);

        //Has to be set before creating heatmaps
        ai.setHeight(7);
        ai.setWidth(7);
        ai.setShipConfig(shipconfig);
        ai.setClientArrayList(clientList);
        ai.setSunk(sunk);
        ai.setSortedSunk(ai.sortTheSunk());
    }


    @Test
    public void sort_The_Sunk_Test() {
        HashMap<Integer, LinkedList<Shot>> sortedSunk = ai.sortTheSunk();
        assertEquals(sortedSunk.get(1).size(), 12);
        assertEquals(sortedSunk.get(2).size(), 5);


    }
    @Test
    public void create_heatmap_all_clients_Test(){
        ai.createHeatmapAllClients();
    }
    @Test
    public void create_Heatmap_one_Client_Test() {
        ai.createHeatmapOneClient(3);
    }

    @Test
    public void get_Sunken_shipIds_all_Clients() {
        assertEquals(3, ai.getSortedSunk().size());
        ai.findAllSunkenShipIds();
        Map<Integer, LinkedList<Integer>> sunken = ai.getAllSunkenShipIds();
        assertNotNull(sunken);
        assertEquals(sunken.size(), 3);
        assertEquals(sunken.get(1).size(), 4);
        assertEquals(sunken.get(2).size(), 2);
    }

    @Test
    public void getRandomPoint2D_Test() {
        Point2D randP = ai.getRandomPoint2D();
        assertNotNull(randP);
        assertTrue(randP.getX() < ai.getWidth());
        assertTrue(randP.getY() < ai.getHeight());
        assertTrue(randP.getX() >= 0);
        assertTrue(randP.getY() >= 0);
    }

    @Test
    public void create_LinkedList_One_Element_Test(){
        Shot shot = new Shot(111, new Point2D(12,4));
        List<Shot> list = ai.createArrayListOneArgument(shot);

        assertNotNull(list);
        assertEquals(list.size(), 1);
        assertSame(list.get(0).getClass(), Shot.class);
    }

}
