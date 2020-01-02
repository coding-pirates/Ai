package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class HeatmapCreator {
    //Logger
    private static final Logger logger = LogManager.getLogger();

    Ai ai;

    public HeatmapCreator(Ai ai) {
        this.ai = ai;
    }

    /**
     * Creates a heatmap for each client and calls {@link HeatmapCreator#createHeatmapOneClient(int clientId)}.
     * In this version, the heatmaps will be created completely new by clearing the old heatmaps first.
     *
     * @return the
     * @see <a href="http://www.datagenetics.com/blog/december32011/">http://www.datagenetics.com/blog/december32011/</a>
     */

    public Map<Integer, Integer[][]> createHeatmapAllClients() {
        Map<Integer, Integer[][]> heatmapAllClients = new HashMap<>();
        InvalidPointsCreator invalidPointsCreator = new InvalidPointsCreator(this.ai);

        SunkenShipFinder sunkenShipFinder = new SunkenShipFinder(ai);
        heatmapAllClients.clear(); //delete the heatmaps of the last round
        ai.addMisses(); // compute the new misses for this round
        ai.setSunkenShipIdsAll(sunkenShipFinder.findSunkenShipIdsAll()); //compute the sunken ship Ids for every client
        for (Client client : ai.getClientArrayList()) {
            if (client.getId() == ai.getAiClientId()) {
                ai.getInvalidPointsAll().replace(client.getId(), invalidPointsCreator.createInvalidPointsOne(client.getId()));
                logger.info(MARKER.AI, "Skipped creating heatmap for own field");
                continue;
            }
            //create a heatmap for this client and put it into the heatmapAllClients map
            heatmapAllClients.put(client.getId(), createHeatmapOneClient(client.getId()));
        }
        return heatmapAllClients;


    }

    /**
     * Creates a Heatmap for one Client: assigns each Point its maximum occupancy by (not yet sunken) ships
     * <p>
     * <p>
     * The algorithm is based on <a href="http://www.datagenetics.com/blog/december32011/">http://www.datagenetics.com/blog/december32011/</a>
     *
     * @param clientId The clientId for whom the heatmap is to be created
     * @return a heatmap for the client
     */
    public Integer[][] createHeatmapOneClient(int clientId) {
        logger.info(MARKER.AI, "Create heatmap for client " + clientId);
        InvalidPointsCreator invalidPointsCreator = new InvalidPointsCreator(this.ai);
        ai.getInvalidPointsAll().replace(clientId, invalidPointsCreator.createInvalidPointsOne(clientId));
        Integer[][] heatmap = new Integer[ai.getHeight()][ai.getWidth()]; //heatmap array
        for (Integer[] integers : heatmap) {
            Arrays.fill(integers, 0);
        }

        LinkedHashSet<Point2D> invalidPointsThisClient = ai.getInvalidPointsAll().get(clientId);
        LinkedList<Integer> sunkenIdsThisClient = ai.getAllSunkenShipIds().get(clientId); // get the sunken ship Ids of this client

        Map<Integer, ShipType> shipConfig = ai.getShips();
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            logger.info(MARKER.AI, "Ship Id of shipConfig: " + entry.getKey());
            if (sunkenIdsThisClient.contains(entry.getKey())) {
                logger.info(MARKER.AI, "ship already sunk: " + entry.getKey());
                continue; //Wenn das Schiff versenkt ist betrachte nächstes Schiff
            }
            int shipId = entry.getKey(); //Schiffs Id
            //Koordinaten des aktuellen Schiffs
            ArrayList<Point2D> positions = (ArrayList<Point2D>) entry.getValue().getPositions();
            //Rotiere das aktuelle Schiff
            Rotator rotator = new Rotator(this.ai);
            ArrayList<ArrayList<Point2D>> rotated = rotator.rotateShips(positions);
            //Betrachte erstes rotiertes Schiff
            for (ArrayList<Point2D> tShips : rotated) {
                ArrayList<Point2D> cShip = new ArrayList<>(tShips); //kopiere erstes rotiertes Schiff
                ArrayList<Integer> xValues = new ArrayList<>(); //alle X werte des Schiffs
                ArrayList<Integer> yValues = new ArrayList<>(); //alle y Werte des Schiffs
                for (Point2D z : cShip) { //füge x und y den Listen hinzu
                    xValues.add(z.getX());
                    yValues.add(z.getY());
                }
                Collections.sort(xValues);//sortiere die x, y Listen
                Collections.sort(yValues);
                int maxX = xValues.get(xValues.size() - 1); //hole größtes x, y der Schiffe
                int maxY = yValues.get(yValues.size() - 1);
                int initMaxX = xValues.get(xValues.size() - 1);//lege initiales x, y fest
                int initMaxY = yValues.get(yValues.size() - 1);

                while (maxY < ai.getHeight()) {
                    while (maxX < ai.getWidth()) {
                        boolean valid = true;
                        //check if cShip fits on the field
                        for (Point2D p : cShip) { //jeder Punkt in cShip
                            for (Point2D s : invalidPointsThisClient) { //jeder invalid Point für diesen Client
                                if (p.getX() == s.getX() & p.getY() == s.getY()) {
                                    //wenn ein Punkt dem Shot Punkt gleich ist, mache nichts und schiebe Schiff einen weiter
                                    valid = false;
                                    break;
                                }
                            }
                            if (!valid) break;

                        }
                        if (valid) {
                            //increment the array positions by 1
                            for (Point2D i : cShip) {
                                int x = i.getX();
                                int y = i.getY();
                                heatmap[y][x]++;
                            }
                        }
                        ArrayList<Point2D> newPos = new ArrayList<>();
                        for (Point2D u : cShip) {
                            newPos.add(new Point2D(u.getX() + 1, u.getY()));
                        }
                        cShip = new ArrayList<>(newPos);
                        newPos.clear();
                        maxX++;
                    }
                    maxX = initMaxX;
                    ArrayList<Point2D> newPos = new ArrayList<>();
                    for (Point2D u : cShip) {
                        newPos.add(new Point2D(u.getX() - (ai.getWidth() - initMaxX), u.getY() + 1));
                    }
                    cShip = new ArrayList<>(newPos);
                    newPos.clear();
                    maxY++;
                }
            }
            logger.info(MARKER.AI, "Finished field with rotated versions of ship " + shipId);
        }
        logger.info(MARKER.AI, "Created heatmap of client: " + clientId);
        return heatmap;

    }
}