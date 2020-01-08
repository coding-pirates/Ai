package de.upb.codingpirates.battleships.ai.util;

import java.util.Comparator;

public class TripleComparator implements Comparator<Triple> {

    @Override
    public int compare(Triple t1, Triple t2) {
        return Double.compare((double) t1.getVal3(), (double) t2.getVal3());
    }
}
