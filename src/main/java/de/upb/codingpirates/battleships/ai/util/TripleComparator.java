package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.logic.Point2D;

import java.util.Comparator;

/**
 * A class which implements the {@link Comparator} interface for sorting the heat points by their heat value.
 *
 * @author Benjamin Kasten
 * @see Triple
 */
public class TripleComparator implements Comparator<Triple<Integer, Point2D, Double>> {

    /**
     * Inherited method which is used by a sort method.
     *
     * @param t1 first Triple
     * @param t2 second Triple
     * @return a negative, positive or zero value which tells the comparator how to compare two objects.
     */
    @Override
    public int compare(Triple t1, Triple t2) {
        return Double.compare((double) t1.getVal3(), (double) t2.getVal3());
    }
}
