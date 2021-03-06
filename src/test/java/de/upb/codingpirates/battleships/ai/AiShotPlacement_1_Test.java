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

public final class AiShotPlacement_1_Test {

    private static AI ai = new AI("AiPlayer", StandardShotPlacementStrategy.RANDOM);

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

        ai.setClientList(temp);

        //ai id festlegen
        ai.setAiClientId(999);

        //hits
        Shot hit1 = new Shot(111, new Point2D(2, 1));
        Shot hit2 = new Shot(222, new Point2D(1, 3));
        Shot hit3 = new Shot(222, new Point2D(2, 3));
        Collection<Shot> temp1 = new ArrayList<>();
        temp1.add(hit1);
        temp1.add(hit2);
        temp1.add(hit3);
        ai.setHits(temp1);

        //misses

        Shot miss1 = new Shot(111, new Point2D(0, 1));
        Shot miss2 = new Shot(111, new Point2D(3, 3));
        Shot miss3 = new Shot(333, new Point2D(2, 0));
        Collection<Shot> temp2 = new ArrayList<>();
        temp2.add(miss1);
        temp2.add(miss2);
        temp2.add(miss3);
        ai.setMisses(temp2);

        ai.setConfiguration(
            new Configuration.Builder()
                .shotCount(3)
                .width(5)
                .height(5)
                .build());
    }

    @Test
    public void testPlaceShots() {
        ai.placeShots();
    }
}
