package de.upb.codingpirates.battleships.ai.test;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.util.Client;
import de.upb.codingpirates.battleships.logic.util.Point2D;
import de.upb.codingpirates.battleships.logic.util.Shot;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class AiTests {
    //added first Tests for placing Shots Method

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
        ai.setWidth(10);
        ai.setHeight(10);
        ai.setShotCount(3);

        Collection<Shot> hits = Collections.EMPTY_LIST;
        Map<Integer, Integer> points = Collections.emptyMap();
        Collection<Shot> sunk = Collections.EMPTY_LIST;
        ai.updateNotification = new PlayerUpdateNotification(hits, points, sunk);
        //System.out.println(ai.updateNotification.getHits());
        System.out.println("Test: calls placeShots");
        try {
            ai.placeShots();
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

}
