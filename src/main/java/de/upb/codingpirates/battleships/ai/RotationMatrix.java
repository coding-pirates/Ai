package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.logic.util.Point2D;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Helper Class for rotating ships and making all their coordinates positive
 *
 * @author Benjamin Kasten
 */
public class RotationMatrix {


    public ArrayList<Point2D> turn90(Collection<Point2D> positions) {
        ArrayList<Point2D> turned = new ArrayList<>();

        for (Point2D p : positions) {
            int newX = 0 * p.getX() + (-1) * p.getY();
            int newY = 1 * p.getX() + 0 * p.getY();
            turned.add(new Point2D(newX, newY));
        }

        return makePositive(turned);
    }

    private ArrayList<Point2D> makePositive(Collection<Point2D> turned) {
        ArrayList<Point2D> positiv90 = new ArrayList<>();
        boolean negativeX = false;
        boolean negativeY = false;
        for (Point2D p : turned) {
            if (p.getX() < 0) negativeX = true;
            if (p.getY() < 0) negativeY = true;
        }

        if (negativeX & !negativeY) {
            int xDis = 0;
            for (Point2D p : turned) {
                if (p.getX() < 0) {
                    xDis = p.getX();
                }

            }
            for (Point2D p : turned) {
                positiv90.add(new Point2D(p.getX() - xDis, p.getY()));
            }
        }
        if (!negativeX & negativeY) {
            int yDis = 0;
            for (Point2D p : turned) {
                if (p.getY() < 0) {
                    yDis = p.getY();
                }

            }
            for (Point2D p : turned) {
                positiv90.add(new Point2D(p.getX(), p.getY() - yDis ));
            }
        }
        if (negativeX & negativeY) {
            int xDis = 0;
            int yDis = 0;
            for (Point2D p : turned) {
                if (p.getX() < 0) {
                    xDis = p.getX();
                }
                if (p.getY() <0){
                    yDis = p.getY();
                }

            }
            for (Point2D p : turned) {
                positiv90.add(new Point2D(p.getX() - xDis, p.getY() - yDis));
            }
        }
        return positiv90;

    }
}
