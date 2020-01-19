package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.logic.Point2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Offset {

    public static int getXOffset(Collection<Point2D> c) {

        Collection<Integer> xValues = new ArrayList<>();

        for (Point2D p : c) {
            xValues.add(p.getX());
        }

        return Collections.min(xValues);
    }

    public static int getYOffset(Collection<Point2D> c) {
        Collection<Integer> yValues = new ArrayList<>();

        for (Point2D p : c) {
            yValues.add(p.getY());
        }

        return Collections.min(yValues);

    }
}
