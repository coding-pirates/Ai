package de.upb.codingpirates.battleships.ai.test;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class AiShotPlacement_2_Test {
    static Ai ai = new Ai();
    @BeforeAll
    public static void create(){
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

        ai.requestedShotsLastRound.add(s1);
        ai.requestedShotsLastRound.add(s5);
        ai.requestedShotsLastRound.add(s6);


        Shot hit1 = new Shot(111, new Point2D(2, 1));
        Shot hit2 = new Shot(222, new Point2D(1, 3));
        Shot hit3 = new Shot(222, new Point2D(2, 3));
        Collection<Shot> temp1 = new ArrayList<>();
        temp1.add(hit1);
        temp1.add(hit2);
        temp1.add(hit3);
        ai.setHits(temp1);

        //shotcount
        ai.setShotCount(3);

        ai.setHeight(5);
        ai.setWidth(5);



    }
    @Test
    public void place_shots_level_2_test() throws IOException {
        ai.placeShots(2);
    }
}
