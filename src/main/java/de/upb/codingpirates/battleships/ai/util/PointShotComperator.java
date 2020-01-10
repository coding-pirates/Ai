package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;

public class PointShotComperator {

    public static boolean comparePoints(Point2D p1 , Point2D p2){
        return p1.getX() == p2.getX() & p1.getY() == p2.getY();
    }

    public static boolean compareShots(Shot s1, Shot s2){
        return s1.getTargetField().getX() == s2.getTargetField().getX() & s1.getTargetField().getY() == s2.getTargetField().getY();
    }
}
