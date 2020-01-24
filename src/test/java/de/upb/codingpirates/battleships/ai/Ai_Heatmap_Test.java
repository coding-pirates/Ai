package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.ai.gameplay.StandardShotPlacementStrategy;
import de.upb.codingpirates.battleships.ai.util.HeatMapCreator;
import de.upb.codingpirates.battleships.ai.util.SunkenShipsHandler;
import de.upb.codingpirates.battleships.logic.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Ai_Heatmap_Test {
    static AI ai = new AI("AiPlayer", StandardShotPlacementStrategy.HEAT_MAP);

    @BeforeAll
    public static void create() {
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

/*
        ai.requestedShotsLastRound.add(new Shot(3, new Point2D(6, 6)));
        ai.requestedShotsLastRound.add(new Shot(2, new Point2D(1, 6)));
        ai.requestedShotsLastRound.add(new Shot(3, new Point2D(3, 3)));
        ai.requestedShotsLastRound.add(new Shot(3, new Point2D(3, 5)));
        ai.requestedShotsLastRound.add(new Shot(3, new Point2D(4, 6)));


 */
        Collection<Shot> hits = new ArrayList<>();
        hits.add(new Shot(3, new Point2D(1, 1)));
        hits.add(new Shot(3, new Point2D(1, 2)));
        hits.add(new Shot(3, new Point2D(2, 2)));


        ai.setHits(hits);

        ai.setAiClientId(999);

        SunkenShipsHandler sunkenShipsHandler = new SunkenShipsHandler(ai);
        //Has to be set before creating heatmaps
        ai.setConfiguration(new Configuration.Builder()
                .ships(shipconfig)
                .width(7)
                .height(7)
                .build());
        ai.setClientArrayList(clientList);
        ai.setSunk(sunk);
        ai.setSortedSunk(sunkenShipsHandler.sortTheSunk());

    }

    @Test
    @Disabled
    public void create_heat_map_all_clients_Test() {
        HeatMapCreator heatmapCreator = new HeatMapCreator(ai);
        ai.setHeatMapAllClients(heatmapCreator.createHeatMapAllClients());
    }

    @Test
    @Disabled
    public void create_Heatmap_one_Client_Test() {
        HeatMapCreator heatmapCreator = new HeatMapCreator(ai);
        heatmapCreator.createHeatMapOneClient(3);
    }
}
