package de.upb.codingpirates.battleships.ai.util;

import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.ai.AI;
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
    AI ai;

    /**
     * /**
     * Constructor for {@link HeatmapCreator}. Gets an instance of the ai object which creates the {@link HeatmapCreator}
     * instance.
     *
     * @param ai The instance of the ai who called the constructor.
     */
    public HeatmapCreator(AI ai) {
        this.ai = ai;
    }

    /**
     * Creates a heatmap for each client and calls {@link HeatmapCreator#createHeatmapOneClient(int)}.
     * In this version, the heatmaps will be created completely new by clearing the old heatmaps first.
     *
     * @return the created heatmaps of all clients
     * @see <a href="http://www.datagenetics.com/blog/december32011/">http://www.datagenetics.com/blog/december32011/</a>
     */

    public Map<Integer, Double[][]> createHeatmapAllClients() {
        Map<Integer, Double[][]> heatmapAllClients = Maps.newConcurrentMap();

        SunkenShipsHandler sunkenShipsHandler = new SunkenShipsHandler(ai);

        ai.setSunkenShipIdsAll(sunkenShipsHandler.findSunkenShipIdsAll()); //compute the sunken ship Ids for every client

        for (Client client : ai.getClientArrayList()) {
            if (client.getId() == ai.getAiClientId()) continue;
            heatmapAllClients.put(client.getId(), createHeatmapOneClient(client.getId()));
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
    public Double[][] createHeatmapOneClient(int clientId) {

        Integer[][] heatmap = new Integer[ai.getConfiguration().getHeight()][ai.getConfiguration().getWidth()]; //heatmap array
        for (Integer[] integers : heatmap) {
            Arrays.fill(integers, 0);
        }

        LinkedList<Point2D> invalidPointsThisClient = ai.getInvalidPointsAll().get(clientId);

        LinkedList<Integer> sunkenIdsThisClient = ai.getAllSunkenShipIds().get(clientId); // get the sunken ship Ids of this client

        Map<Integer, ShipType> shipConfig = ai.getConfiguration().getShips();
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            if (sunkenIdsThisClient.contains(entry.getKey())) {
                continue; //if this ship is sunk, take the next
            }
            int shipId = entry.getKey(); //ship id
            //coordinates of the ship
            ArrayList<Point2D> positions = (ArrayList<Point2D>) entry.getValue().getPositions();
            //rotate the ship
            ArrayList<ArrayList<Point2D>> rotated = new Rotator(this.ai).rotateShips(positions);
            //Iterate through each rotation
            for (ArrayList<Point2D> tShips : rotated) {
                ArrayList<Point2D> cShip = new ArrayList<>(tShips); //copy first ship
                ArrayList<Integer> xValues = new ArrayList<>(); //all x values
                ArrayList<Integer> yValues = new ArrayList<>(); //all y values
                for (Point2D z : cShip) { //put all x and y values to the list above
                    xValues.add(z.getX());
                    yValues.add(z.getY());
                }
                int maxX = Collections.max(xValues);
                int maxY = Collections.max(yValues);

                int initMaxX = Collections.max(xValues); //the inital max x value (needed for shifting)
                /*
                Collections.sort(xValues);//sort the lists
                Collections.sort(yValues);//~
                int maxX = xValues.get(xValues.size() - 1); //hole größtes x, y der Schiffe
                int maxY = yValues.get(yValues.size() - 1);
                int initMaxX = xValues.get(xValues.size() - 1);//lege initiales x, y fest
                int initMaxY = yValues.get(yValues.size() - 1);

                 */

                while (maxY < ai.getConfiguration().getHeight()) {
                    while (maxX < ai.getConfiguration().getWidth()) {
                        boolean valid = true;
                        //check if cShip fits on the field
                        for (Point2D p : cShip) {
                            for (Point2D s : invalidPointsThisClient) { //each invalid point for this ckient
                                if (p.getX() == s.getX() & p.getY() == s.getY()) {
                                    //is not valid for next steps if one point is invalid
                                    valid = false;
                                    break;
                                }
                            }
                            if (!valid) break;
                        }
                        if (valid) {
                            //increment the array positions by 1 if positions are valid
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
                        newPos.add(new Point2D(u.getX() - (ai.getConfiguration().getWidth() - initMaxX), u.getY() + 1));
                    }
                    cShip = new ArrayList<>(newPos);
                    newPos.clear();
                    maxY++;
                }
            }
        }
        //By definition the heatvalue of each point of dead player is 0
        if (ai.getAllSunkenShipIds().get(clientId).size() == ai.getConfiguration().getShips().size()) {
            for (Integer[] i : heatmap) {
                Arrays.fill(i, 0);
            }
        }

        /*
        Double[][] dHeatmap = new Double[ai.getConfiguration().getHeight()][ai.getConfiguration().getWidth()];

        for (int i = 0; i < heatmap.length; i++) {
            for (int j = 0; j < heatmap[i].length; j++) {
                dHeatmap[i][j] = (double) heatmap[i][j];
            }
        }

         */

        if (clientId == ai.getAiClientId()) {
            System.out.println(String.format("Heatmap of my field (%s)", ai.getAiClientId()));
        }

        System.out.println("Heatmap of player " + clientId);

        Double[][] probHeatmap = createProbHeatmap(heatmap);
        printHeatmap(probHeatmap, clientId);
        return probHeatmap;

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

        Double[][] probHeat = new Double[ai.getConfiguration().getHeight()][ai.getConfiguration().getWidth()];

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
    public void printHeatmap(Double[][] probHeat, int clientId) {
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
}


