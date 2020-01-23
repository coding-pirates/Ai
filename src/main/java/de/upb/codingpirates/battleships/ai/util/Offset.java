package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.logic.Point2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Handles the offset values of a ship point collection
 *
 * @author Benjamin Kasten
 */
public class Offset {

    /**
     * Calculates the x offset
     *
     * @param c the ship point collection
     * @return the x offset
     */
    public static int getXOffset(Collection<Point2D> c) {

        Collection<Integer> xValues = new ArrayList<>();

        for (Point2D p : c) {
            xValues.add(p.getX());
        }

        return Collections.min(xValues);
    }

    /**
     * Calculates the y offset
     *
     * @param c the ship point collection
     * @return the y offset
     */
    public static int getYOffset(Collection<Point2D> c) {
        Collection<Integer> yValues = new ArrayList<>();

        for (Point2D p : c) {
            yValues.add(p.getY());
        }

        return Collections.min(yValues);

    }
}
