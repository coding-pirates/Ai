package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;

/**
 * Includes static methods to to avoid long comparisons of points and shots.
 *
 * @author Benjamin Kasten
 */
public class PositionComparator {

    /**
     * Point2D to Point2D comparison
     *
     * @param p1 first point
     * @param p2 second point
     * @return true if first point == second point, else false
     */
    public static boolean comparePoints(Point2D p1, Point2D p2) {
        return p1.getX() == p2.getX() && p1.getY() == p2.getY();
    }

    /**
     * Shot to Shot comparison
     *
     * @param s1 first shot
     * @param s2 second shot
     * @return true if first == second, else false
     */
    public static boolean compareShots(Shot s1, Shot s2) {
        return s1.getTargetField().getX() == s2.getTargetField().getX() && s1.getTargetField().getY() == s2.getTargetField().getY() && s1.getClientId() == s2.getClientId();
    }

    /**
     * Point2D to shot comparison with id
     *
     * @param p  point
     * @param s  shot
     * @param id related id of point
     * @return true if point == targetfield of shot and id == id of shot, else false
     */
    public static boolean comparePointShot(Point2D p, Shot s, int id) {
        return p.getX() == s.getTargetField().getX() && p.getY() == s.getTargetField().getY() && id == s.getClientId();

    }
}
