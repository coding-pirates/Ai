package de.upb.codingpirates.battleships.ai.util;

import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Creates heat maps for clients.
 *
 * @author Benjamin Kasten
 */
public final class HeatMapCreator {

    @Nonnull
    private final AI ai;

    /**
     * /**
     * Constructor for {@link HeatMapCreator}. Gets an instance of the ai object which creates the {@link HeatMapCreator}
     * instance.
     *
     * @param ai The instance of the ai who called the constructor.
     */
    public HeatMapCreator(@Nonnull final AI ai) {
        this.ai = ai;
    }

    /**
     * Creates a heat map for each client and calls {@link HeatMapCreator#createHeatMapOneClient(int)}.
     * In this version, the heat maps will be created completely new by clearing the old heat maps first.
     *
     * @return the created heat maps of all clients
     * @see <a href="http://www.datagenetics.com/blog/december32011/">http://www.datagenetics.com/blog/december32011/</a>
     */
    public Map<Integer, double[][]> createHeatMapAllClients() {
        Map<Integer, double[][]> heatMapAllClients = Maps.newConcurrentMap();

        SunkenShipsHandler sunkenShipsHandler = new SunkenShipsHandler(ai);

        ai.setSunkenShipIdsAll(sunkenShipsHandler.findSunkenShipIdsAll()); //compute the sunken ship Ids for every client

        for (Client client : ai.getClientArrayList()) {
            if (client.getId() == ai.getAiClientId()) continue;
            heatMapAllClients.put(client.getId(), createHeatMapOneClient(client.getId()));
        }
        return heatMapAllClients;
    }


    /**
     * Creates a Heatmap for one Client: assigns each Point its maximum occupancy by (not yet sunken) ships
     * <p>
     * The algorithm is based on <a href="http://www.datagenetics.com/blog/december32011/">http://www.datagenetics.com/blog/december32011/</a>
     *
     * @param clientId The clientId for whom the heatmap is to be created
     * @return a heatmap for the client
     */
    public double[][] createHeatMapOneClient(final int clientId) {

        List<Point2D> thisHits = new HitsHandler(this.ai).sortTheHits().get(clientId); // die hits des clients
        List<List<Point2D>> thisConHits = new SunkenShipsHandler(this.ai).findConnectedPoints(thisHits); // connected hits
        List<List<Point2D>> thisConSunk = new SunkenShipsHandler(this.ai).getSunksOfHits(thisConHits, clientId); //connected sunks

        boolean focusedMode = false;

        System.out.println("Check for focused mode: ");
        System.out.println("All hits:" + thisHits);
        System.out.println("This conSunk" + thisConSunk);
        if (!(thisConHits.size() == thisConSunk.size())) {
            for (Point2D p : thisHits) {
                if (thisConSunk.isEmpty()) {
                    focusedMode = true;
                    break;
                }
                for (List<Point2D> l : thisConSunk) {
                    for (Point2D k : l) {
                        if (!PositionComparator.comparePoints(p, k)) {
                            focusedMode = true;
                            break;
                        }
                    }
                    if (focusedMode) break;
                }
                if (focusedMode) break;
            }
        }
        System.out.println("Focused Mode: " + focusedMode);


        int[][] heatMap = new int[ai.getConfiguration().getHeight()][ai.getConfiguration().getWidth()];

        List<Point2D> invalidPointsThisClient = ai.getInvalidPointsAll().get(clientId);

        List<Integer> sunkenIdsThisClient = ai.getAllSunkenShipIds().get(clientId); // get the sunken ship Ids of this client

        Map<Integer, ShipType> shipConfig = ai.getConfiguration().getShips();
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            if (sunkenIdsThisClient.contains(entry.getKey())) {
                continue; //if this ship is sunk, take the next
            }
            //coordinates of the ship
            List<Point2D> positions = new ArrayList<>(entry.getValue().getPositions());
            //rotate the ship
            List<List<Point2D>> rotated = new Rotator().rotateShips(positions);
            //Iterate through each rotation
            for (List<Point2D> tShips : rotated) {
                ArrayList<Point2D> cShip = new ArrayList<>(tShips); //copy first ship
                ArrayList<Integer> xValues = new ArrayList<>(); //all x values
                ArrayList<Integer> yValues = new ArrayList<>(); //all y values
                for (Point2D z : cShip) { //put all x and y values to the list above
                    xValues.add(z.getX());
                    yValues.add(z.getY());
                }
                int maxX = Collections.max(xValues);
                int maxY = Collections.max(yValues);

                int initMaxX = Collections.max(xValues); //the initial max x value (needed for shifting)

                while (maxY < ai.getConfiguration().getHeight()) {
                    while (maxX < ai.getConfiguration().getWidth()) {
                        boolean valid = true;
                        //check if cShip fits on the field
                        for (Point2D p : cShip) {
                            for (Point2D s : invalidPointsThisClient) { //each invalid point for this client
                                if (p.getX() == s.getX() & p.getY() == s.getY()) {
                                    //is not valid for next steps if one point is invalid
                                    valid = false;
                                    break;
                                }
                            }
                            if (!valid) break;
                        }

                        //check focusedMode

                        if (focusedMode) {
                            boolean included = false;
                            for (List<Point2D> l : thisConHits) {
                                int counter = 0;
                                for (Point2D p : l) {
                                    for (Point2D k : cShip) {
                                        if (PositionComparator.comparePoints(p, k)) {
                                            counter += 1;
                                        }
                                    }
                                }
                                if (counter == l.size()) {
                                    included = true;
                                    break;
                                }

                            }
                            if (!included) {
                                valid = false;
                            }
                        }
                        if (valid) {
                            //increment the array positions by 1 if positions are valid
                            for (Point2D i : cShip) {
                                int x = i.getX();
                                int y = i.getY();
                                heatMap[y][x]++;
                            }
                        }
                        List<Point2D> newPos = new ArrayList<>();
                        for (Point2D u : cShip) {
                            newPos.add(new Point2D(u.getX() + 1, u.getY()));
                        }
                        cShip = new ArrayList<>(newPos);
                        newPos.clear();
                        maxX++;
                    }
                    maxX = initMaxX;
                    List<Point2D> newPos = new ArrayList<>();
                    for (Point2D u : cShip) {
                        newPos.add(new Point2D(u.getX() - (ai.getConfiguration().getWidth() - initMaxX), u.getY() + 1));
                    }
                    cShip = new ArrayList<>(newPos);
                    newPos.clear();
                    maxY++;
                }
            }
        }
        //By definition the heat value of each point of dead player is 0
        if (ai.getAllSunkenShipIds().get(clientId).size() == ai.getConfiguration().getShips().size()) {
            for (int[] i : heatMap) {
                Arrays.fill(i, 0);
            }
        }
        if (clientId == ai.getAiClientId())
            System.out.println(String.format("Heat map of my field (%s)", ai.getAiClientId()));

        System.out.println("Heat map of player " + clientId);

        double[][] probHeatMap = createProbHeatMap(heatMap);
        printHeatMap(probHeatMap);
        return probHeatMap;
    }

    /**
     * Creates a relative heatMap based on all possible placement positions. Useful for comparing heatmaps of multiple
     * clients.
     *
     * @param heatMap the absolute heatMap
     * @return teh relative heatMap
     */
    @Nonnull
    public double[][] createProbHeatMap(@Nonnull final int[][] heatMap) {
        int counter = 0;

        for (int[] i : heatMap) {
            for (int j : i) {
                counter = counter + j;
            }
        }
        double[][] probHeat = new double[ai.getConfiguration().getHeight()][ai.getConfiguration().getWidth()];
        for (int i = 0; i < heatMap.length; i++) {
            for (int j = 0; j < heatMap[i].length; j++)
                probHeat[i][j] = (counter == 0) ? 0 : ((double) heatMap[i][j] / (double) counter);
        }
        return probHeat;
    }

    /**
     * Prints a heat map.
     *
     * @param probHeat the heat map to print
     */
    public void printHeatMap(@Nonnull final double[][] probHeat) {
        for (int i = probHeat.length - 1; i >= 0; i--) {
            for (double j : probHeat[i]) {
                if (j == 0) {
                    System.out.print("------  ");
                } else {
                    System.out.print(String.format(Locale.ROOT, "%.4f", j) + "  ");
                }
            }
            System.out.println();
        }
    }
}


