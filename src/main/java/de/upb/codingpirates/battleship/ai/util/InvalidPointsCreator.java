package de.upb.codingpirates.battleship.ai.util;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class InvalidPointsCreator {
    //Logger
    private static final Logger logger = LogManager.getLogger(Ai.class.getName());

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
        logger.info("Computing the invalid points of client : " + clientId);
        ai.getInvalidPointsAll().putIfAbsent(clientId, null);
        LinkedList<Shot> sortedSunkShotsTC = ai.getSortedSunk().get(clientId);
        ArrayList<Point2D> sortedSunkPointsTC = new ArrayList<>();
        for (Shot s : sortedSunkShotsTC) {
            sortedSunkPointsTC.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));
        }
        LinkedHashSet<Point2D> temp = new LinkedHashSet<>(ai.addSurroundingPointsToUsedPoints(sortedSunkPointsTC));
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

}
