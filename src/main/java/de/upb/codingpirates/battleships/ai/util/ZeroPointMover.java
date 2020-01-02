package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.logic.Point2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Helper Class for making shifting the points of a ship until they are positive.
 *
 * @author Benjamin Kasten
 */
public class ZeroPointMover {
    private static final Logger logger = LogManager.getLogger();

    /**
     * Moves the ship to the field (only positive values and as near as possible  to the x and y axis)
     *
     * @param tp the shot collection which should be shifted
     * @return the shifted collection
     */
    public ArrayList<Point2D> moveToZeroPoint(ArrayList<Point2D> tp) {
        ArrayList<Integer> xVal = new ArrayList<>();
        ArrayList<Integer> yVal = new ArrayList<>();
        for (Point2D i : tp) {
            xVal.add(i.getX());
            yVal.add(i.getY());

            Collections.sort(xVal);
            Collections.sort(yVal);
        }

        int minX = xVal.get(0);
        int minY = yVal.get(0);
        ArrayList<Point2D> zp = new ArrayList<>();

        for (Point2D p : tp) {
            zp.add(new Point2D(p.getX() - minX, p.getY() - minY));
        }
        return zp;


    }
}
