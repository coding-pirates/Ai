package de.upb.codingpirates.battleship.ai.util;

import de.upb.codingpirates.battleships.logic.Point2D;

import java.util.ArrayList;

/**
 * Helper Class for rotating ships and making all their coordinates positive
 * Uses a rotation matrix: rotating the ship object around the zero point
 * Used for getting all possible rotations of a ship
 *
 * @author Benjamin Kasten
 */
public class RotationMatrix {
    ZeroPointMover mover = new ZeroPointMover();

    /**
     * Turns a ship (collection of points) 90 degree around the zeropoint and calls the {@link ZeroPointMover#moveToZeroPoint(ArrayList)}
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

        return mover.moveToZeroPoint(turned);
    }


}
