package de.upb.codingpirates.battleships.ai.test;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.ai.AiMain;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Shot;
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

public class AiShotPlacementRTest {
    //Todo BeforeAll Methode implementieren
    @Test
    public void placing_Shots_randomly_on_field_with_4_Clients_Test() {
        Ai ai = new Ai();
        //set parameters
        AiMain.setDifficultyLevel(3);

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
            ai.placeShots(AiMain.getDifficultyLevel());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collection<Shot> shots = ai.shotsRequest.getShots();
        for (Shot i : shots) {

            System.out.print(i.getTargetField().getX() + ",");
            System.out.println(i.getTargetField().getY());
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
