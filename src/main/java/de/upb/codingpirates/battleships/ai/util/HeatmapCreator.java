package de.upb.codingpirates.battleships.ai.util;

import com.google.common.collect.Lists;
import com.sun.java.accessibility.util.AccessibilityListenerList;
import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.ai.gameplay.ShotPlacer;
import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Creates heatmaps for clients.
 *
 * @author Benjamin Kasten
 */
public class HeatmapCreator {
    private static final Logger logger = LogManager.getLogger();
    Ai ai;

    /**
     * /**
     * Constructor for {@link HeatmapCreator}. Gets an instance of the ai object which creates the {@link HeatmapCreator}
     * instance.
     *
     * @param ai The instance of the ai who called the constructor.
     */
    public HeatmapCreator(Ai ai) {
        this.ai = ai;
    }

    /**
     * Creates a heatmap for each client and calls {@link HeatmapCreator#createHeatmapOneClient(int, int)}.
     * In this version, the heatmaps will be created completely new by clearing the old heatmaps first.
     *
     * @return the created heatmaps of all clients
     * @see <a href="http://www.datagenetics.com/blog/december32011/">http://www.datagenetics.com/blog/december32011/</a>
     */

    public Map<Integer, Double[][]> createHeatmapAllClients(int k) {
        Map<Integer, Double[][]> heatmapAllClients = new HashMap<>();

        SunkenShipsHandler sunkenShipsHandler = new SunkenShipsHandler(ai);

        //ai.addMisses(); // compute the new misses for this round

        ai.setSunkenShipIdsAll(sunkenShipsHandler.findSunkenShipIdsAll()); //compute the sunken ship Ids for every client

        for (Client client : ai.getClientArrayList()) {
            heatmapAllClients.put(client.getId(), createHeatmapOneClient(client.getId(), k));
        }
        //printHeatmapsAll(heatmapAllClients);
        return heatmapAllClients;


    }


    /**
     * Creates a Heatmap for one Client: assigns each Point its maximum occupancy by (not yet sunken) ships
     * <p>
     * The algorithm is based on <a href="http://www.datagenetics.com/blog/december32011/">http://www.datagenetics.com/blog/december32011/</a>
     *
     * @param clientId The clientId for whom the heatmap is to be created
     * @return a heatmap for the client
     */
    public Double[][] createHeatmapOneClient(int clientId, int k) {
        logger.info(MARKER.AI, "Create heatmap for client " + clientId);

        Integer[][] heatmap = new Integer[ai.getHeight()][ai.getWidth()]; //heatmap array
        for (Integer[] integers : heatmap) {
            Arrays.fill(integers, 0);
        }

        LinkedList<Point2D> invalidPointsThisClient = ai.getInvalidPointsAll().get(clientId);
        /*
        logger.debug("Inv points of client: {}", clientId);
        for (Point2D p :invalidPointsThisClient){
            logger.debug(p);
        }
        */

        LinkedList<Integer> sunkenIdsThisClient = ai.getAllSunkenShipIds().get(clientId); // get the sunken ship Ids of this client

        Map<Integer, ShipType> shipConfig = ai.getShips();
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            if (sunkenIdsThisClient.contains(entry.getKey())) {
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
            //logger.info(MARKER.AI, "Finished field with rotated versions of ship " + shipId);
        }
        if (ai.getAllSunkenShipIds().get(clientId).size() == ai.getShips().size()) {
            for (Integer[] i : heatmap) {
                Arrays.fill(i, 0);
            }
        }

        //die heatmap mit absoluten Werten
        Double[][] dHeatmap = new Double[ai.getHeight()][ai.getWidth()];

        for (int i = 0; i < heatmap.length; i++) {
            for (int j = 0; j < heatmap[i].length; j++) {
                dHeatmap[i][j] = (double) heatmap[i][j];
            }
        }
        logger.info(MARKER.AI, "Created heatmap of client " + clientId);
        if (k == 2) {
            Double[][] probHeatmap = createProbHeatmap(heatmap);
            printHeatmap(probHeatmap);
            return probHeatmap;
        }
        printHeatmap(dHeatmap);
        return dHeatmap;
    }

    /**
     * Creates a relative heatmap based on all possible placement positions. Useful for comparing heatmaps of multiple
     * clients.
     *
     * @param heatmap the absolute heatmap
     * @return teh relative heatmap
     */
    public Double[][] createProbHeatmap(Integer[][] heatmap) {
        int counter = 0;

        for (Integer[] i : heatmap) {
            for (int j : i) {
                counter = counter + j;
            }
        }

        Double[][] probHeat = new Double[ai.getHeight()][ai.getWidth()];

        for (Double[] i : probHeat) {
            Arrays.fill(i, (double) 0);
        }

        for (int i = 0; i < heatmap.length; i++) {
            for (int j = 0; j < heatmap[i].length; j++) {
                if (counter == 0) {
                    probHeat[i][j] = (double) 0;
                } else {
                    probHeat[i][j] = (double) heatmap[i][j] / (double) counter;
                }
            }

        }
        return probHeat;
    }

    /**
     * Prints a heatmap.
     *
     * @param probHeat the heatmap to print
     */
    public void printHeatmap(Double[][] probHeat) {
        for (int i = probHeat.length - 1; i >= 0; i--) {
            for (double j : probHeat[i]) {
                if (j == 0.0000) {
                    System.out.print("------  ");
                } else {
                    System.out.print(String.format("%.4f", j) + "  ");
                }
            }
            System.out.println();
        }

    }


    /**
     * @param heatmaps
     * @deprecated
     */
    public void printHeatmapsAll(Map<Integer, Double[][]> heatmaps) {
        logger.info("Print the heatmaps:");
        for (Map.Entry<Integer, Double[][]> entry : heatmaps.entrySet()) {
            int clientId = entry.getKey();
            System.out.println("Heatmap of client " + clientId);

            for (int i = entry.getValue().length - 1; i >= 0; i--) {
                for (Double j : entry.getValue()[i]) {
                    if (j == 0) {
                        System.out.print("---  ");
                    } else {
                        System.out.print(String.format("%03d", j) + "  ");
                    }
                }
                System.out.println();
                //System.out.println(Arrays.toString(entry.getValue()[i]) + " ");
            }
        }
    }

}


