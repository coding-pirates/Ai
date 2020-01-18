package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;

public class PositionComparator {

    public static boolean comparePoints(Point2D p1, Point2D p2) {
        return p1.getX() == p2.getX() & p1.getY() == p2.getY();
    }

    public static boolean compareShots(Shot s1, Shot s2) {
        return s1.getTargetField().getX() == s2.getTargetField().getX() & s1.getTargetField().getY() == s2.getTargetField().getY() & s1.getClientId() == s2.getClientId();
    }

    public static boolean comparePointShot(Point2D p, Shot s) {
        return p.getX() == s.getTargetField().getX() & p.getY() == s.getTargetField().getY();
    }

    public static boolean comparePointShot(Point2D p, Shot s, int id) {
        return p.getX() == s.getTargetField().getX() & p.getY() == s.getTargetField().getY() & id == s.getClientId();

    }

    public static boolean compareTriple(Triple<Integer, Point2D, Double> t1, Triple<Integer, Point2D, Double> t2) {
        return t1.getVal1().equals(t2.getVal1()) & t1.getVal2().getX() == t2.getVal2().getX() & t1.getVal2().getY() == t2.getVal2().getY() & t1.getVal3().equals(t2.getVal3());
    }
}
