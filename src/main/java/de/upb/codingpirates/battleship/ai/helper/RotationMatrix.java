package de.upb.codingpirates.battleship.ai.helper;

import de.upb.codingpirates.battleships.logic.util.Point2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Helper Class for rotating ships and making all their coordinates positive
 * Uses a rotation matrix: rotating the ship object around the zero point
 * Used for getting all possible rotations of a ship
 *
 * @author Benjamin Kasten
 */
public class RotationMatrix {

    /**
     * Turns a ship (collection of points) 90 degree around the zeropoint and calls the {@link #makePositive}
     * method
     *
     * @param positions all points of a ship
     * @return a ArrayList of points, which includes the 90 degree turned and (now) positive points.
     */
    public ArrayList<Point2D> turn90(ArrayList<Point2D> positions) {
        ArrayList<Point2D> turned = new ArrayList<>();

        for (Point2D p : positions) {
            //uses a rotation matrix for 90 degree
            int newX = 0 * p.getX() + (-1) * p.getY();
            int newY = 1 * p.getX() + 0 * p.getY();
            turned.add(new Point2D(newX, newY));
        }

        return moveToZeroPoint(turned);
    }

    /**
     * Moves the ship to the field (only positive values and near to the x and y axis)
     * @param tp
     * @return
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

    /**
     * @deprecated moveToZeroPoint is working better
     * @param turned
     * @return
     */
    private ArrayList<Point2D> makePositive(ArrayList<Point2D> turned) {
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
                positiv90.add(new Point2D(p.getX(), p.getY() - yDis));
            }
        }
        if (negativeX & negativeY) {
            int xDis = 0;
            int yDis = 0;
            for (Point2D p : turned) {
                if (p.getX() < 0) {
                    xDis = p.getX();
                }
                if (p.getY() < 0) {
                    yDis = p.getY();
                }

            }
            for (Point2D p : turned) {
                positiv90.add(new Point2D(p.getX() - xDis, p.getY() - yDis));
            }
        }
        if (!negativeX & !negativeY) {
            return turned;
        }
        return positiv90;

    }
}
