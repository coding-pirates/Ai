package de.upb.codingpirates.battleships.ai.util;

import java.util.Comparator;

/**
 * A class which implements the {@link Comparator} interface for sorting the heat points by their heat value.
 *
 * @author Benjamin Kasten
 */
public class TripleComparator implements Comparator<Triple> {

    /**
     * Inherited method which is used by a sort method.
     * @param t1
     * @param t2
     * @return a negative, positive or zero value which tells the comperator how to compare two objects.
     */
    @Override
    public int compare(Triple t1, Triple t2) {
        return Double.compare((double) t1.getVal3(), (double) t2.getVal3());
    }
}
