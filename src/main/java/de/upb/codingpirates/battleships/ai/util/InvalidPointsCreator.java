package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.ai.gameplay.ShipPlacer;
import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

public class InvalidPointsCreator {
    //Logger
    private static final Logger logger = LogManager.getLogger();

    Ai ai;

    public InvalidPointsCreator(Ai ai) {
        this.ai = ai;
    }


    /**
     * Computes the new updated collection of invalid Points of one client.
     * The old value of the last round has to be replaced in invalidPointsAll in class Ai by the return value
     *
     * @param clientId The clientId for computing the invalid points
     * @return the updated collection of invalid points of this client
     */

    public LinkedHashSet<Point2D> createInvalidPointsOne(int clientId) {
        logger.info(MARKER.AI, "Computing the invalid points of client : " + clientId);
        ai.getInvalidPointsAll().putIfAbsent(clientId, null);
        LinkedList<Shot> sortedSunkShotsTC = ai.getSortedSunk().get(clientId);
        ArrayList<Point2D> sortedSunkPointsTC = new ArrayList<>();
        for (Shot s : sortedSunkShotsTC) {
            sortedSunkPointsTC.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));
        }
        LinkedHashSet<Point2D> temp = new LinkedHashSet<>(addSurroundingPointsToUsedPoints(sortedSunkPointsTC));
        for (Shot s : ai.getMisses()) {
            if (s.getClientId() == clientId) {
                temp.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));
            }
        }
        for (Shot s : ai.getHits()) {
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
     * @param shipPos The positions of one ship
     */
    public LinkedHashSet<Point2D> addSurroundingPointsToUsedPoints(ArrayList<Point2D> shipPos) {
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

    /*
    public boolean contains(LinkedHashSet<Point2D> set, Point2D k) {
        for (Point2D p : set) {
            if (p.getX() == k.getX() & p.getY() == k.getY()) {
                return true;
            }
        }
        return false;
    }

     */


}
