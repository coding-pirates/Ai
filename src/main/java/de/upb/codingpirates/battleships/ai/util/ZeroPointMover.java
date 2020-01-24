package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.logic.Point2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper Class for making shifting the points of a ship until they are positive.
 *
 * @author Benjamin Kasten
 */
public class ZeroPointMover {

    /**
     * Moves the ship to the field (only positive values and as near as possible  to the x and y axis)
     *
     * @param tp the shot collection which should be shifted
     * @return the shifted collection
     */
    public List<Point2D> moveToZeroPoint(List<Point2D> tp) {
        List<Integer> xVal = new ArrayList<>();
        List<Integer> yVal = new ArrayList<>();
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
