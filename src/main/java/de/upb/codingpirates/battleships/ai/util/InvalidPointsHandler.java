package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.ai.gameplay.ShipPlacer;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Handles the invalid Points which can not be accessed anymore. Is called by {@link ShipPlacer} and
 * {@link HeatmapCreator}.
 *
 * @author Benjamin Kasten
 */
public class InvalidPointsHandler {
    //Logger
    private static final Logger logger = LogManager.getLogger();

    Ai ai;

    public InvalidPointsHandler(Ai ai) {
        this.ai = ai;
    }


    /**
     * Computes the new updated collection of invalid Points of one client.
     * The old value of the last round has to be replaced in invalidPointsAll in class Ai by the return value
     *
     * @param clientId The clientId for computing the invalid points
     * @return the updated collection of invalid points of this client
     */

    public LinkedList<Point2D> createInvalidPointsOne(int clientId) {

        ai.getInvalidPointsAll().putIfAbsent(clientId, null);

        LinkedList<Point2D> sortedSunkPointsTC = ai.getSortedSunk().get(clientId);

        LinkedList<Point2D> temp = new LinkedList<>(addSurroundingPointsToUsedPoints(sortedSunkPointsTC));

        for (Shot s : ai.getMisses()) {
            if (s.getClientId() == clientId) {
                temp.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));
            }
        }

        //logger.info("Size of invalid points of client {}: {} ", clientId, temp.size());
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
    public LinkedHashSet<Point2D> addSurroundingPointsToUsedPoints(List<Point2D> shipPos) {
        //todo prevent multiple insertion of the same point
        LinkedHashSet<Point2D> temp = new LinkedHashSet<>();
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
        temp.removeIf(p -> p.getX() < 0 | p.getY() < 0);
        return temp;
    }
}
