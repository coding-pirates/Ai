package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.ai.gameplay.StandardShotPlacementStrategy;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

public final class AiShotPlacement_2_Test {

    private static AI ai = new AI("AiPlayer", StandardShotPlacementStrategy.HUNT_AND_TARGET);

    @BeforeAll
    public static void setUpClass() {
        //Clients erstellen
        Client client1 = new Client(111, "c1");
        Client client2 = new Client(222, "c2");
        Client client3 = new Client(333, "c3");
        Client clientAi = new Client(999, "AiPlayer");
        Collection<Client> temp = new ArrayList<>();
        temp.add(client1);
        temp.add(client2);
        temp.add(client3);
        temp.add(clientAi);

        ai.setClientArrayList(temp);
        //ai id festlegen
        ai.setAiClientId(999);
        //requested shots last round
        Shot s1 = new Shot(111, new Point2D(2, 1));
        Shot s5 = new Shot(111, new Point2D(3, 3));
        Shot s6 = new Shot(333, new Point2D(2, 0));

        ai.getRequestedShotsLastRound().add(s1);
        ai.getRequestedShotsLastRound().add(s5);
        ai.getRequestedShotsLastRound().add(s6);

        Shot hit1 = new Shot(111, new Point2D(2, 1));
        Shot hit2 = new Shot(222, new Point2D(1, 3));
        Shot hit3 = new Shot(222, new Point2D(2, 3));
        Collection<Shot> temp1 = new ArrayList<>();
        temp1.add(hit1);
        temp1.add(hit2);
        temp1.add(hit3);
        ai.setHits(temp1);

        ai.setConfiguration(new Configuration.Builder()
                .height(5)
                .width(5)
                .shotCount(3)
                .build());
    }

    @Test
    public void testPlaceShots() {
        ai.placeShots();
    }
}
