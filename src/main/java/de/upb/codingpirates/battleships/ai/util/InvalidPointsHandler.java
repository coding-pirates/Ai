package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.ai.gameplay.ShipPlacer;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;

import java.util.*;

/**
 * Handles the invalid Points which can not be accessed anymore. Is called by {@link ShipPlacer} and
 * {@link HeatMapCreator}.
 *
 * @author Benjamin Kasten
 */
public class InvalidPointsHandler {
    AI ai;

    public InvalidPointsHandler(AI ai) {
        this.ai = ai;
    }


    /**
     * Computes the new updated collection of invalid Points of one client.
     * The old value of the last round has to be replaced in invalidPointsAll in class Ai by the return value
     *
     * @param clientId The clientId for computing the invalid points
     * @return the updated collection of invalid points of this client
     */

    public List<Point2D> createInvalidPointsOne(int clientId) {
        ai.getInvalidPointsAll().putIfAbsent(clientId, null);

        List<Point2D> sortedSunkPointsTC = ai.getSortedSunk().get(clientId);

        List<Point2D> temp = new LinkedList<>(addSurroundingPointsToUsedPoints(sortedSunkPointsTC));

        for (Shot s : ai.getMisses()) {
            if (s.getClientId() == clientId) {
                temp.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));
            }
        }
        return temp;
    }


    /**
     * Adds the surrounding points of one points collection to the usedPoints based on the rules for the game:
     * each ship must have a minimal distance of one point in each direction to other ships.
     * Used by {@link ShipPlacer#guessRandomShipPositions(Map)}
     *
     * @param shipPos The positions of one ship.
     * @return The set of invalid points around one ship.
     */
    public Set<Point2D> addSurroundingPointsToUsedPoints(List<Point2D> shipPos) {
        Set<Point2D> temp = new LinkedHashSet<>();
        for (Point2D point : shipPos) {
            int x = point.getX();
            int y = point.getY();

            //add all left neighbours
            temp.add(new Point2D(x - 1, y + 1));
            temp.add(new Point2D(x - 1, y));
            temp.add(new Point2D(x - 1, y - 1));

            //add all right neighbours
            temp.add(new Point2D(x + 1, y + 1));
            temp.add(new Point2D(x + 1, y));
            temp.add(new Point2D(x + 1, y - 1));

            //add the direct neighbours under and over
            temp.add(new Point2D(x, y + 1));
            temp.add(new Point2D(x, y - 1));
        }
        temp.removeIf(p -> p.getX() < 0 || p.getY() < 0);
        return temp;
    }
}
