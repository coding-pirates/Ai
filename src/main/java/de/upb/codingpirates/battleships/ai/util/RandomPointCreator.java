package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.Point2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates a random {@link Point2D} object.
 *
 * @author Benjamin Kasten
 */
public class RandomPointCreator {
    private static final Logger logger = LogManager.getLogger();
    Ai ai;

    /**
     * Constructor for {@link RandomPointCreator}. Gets an instance of the ai object which creates the {@link RandomPointCreator}
     * instance. Is used to get the right field parameters (width and height).
     *
     * @param ai The instance of the ai who called the constructor.
     */
    public RandomPointCreator(Ai ai) {
        this.ai = ai;
    }

    /**
     * Creates a random {@link Point2D} object related to the width and height of the game field of the ai.
     *
     * @return Point2d Random Point with X and Y coordinates
     */
    public Point2D getRandomPoint2D() {
        int x = (int) (Math.random() * ai.getWidth());
        int y = (int) (Math.random() * ai.getHeight());
        Point2D randPoint = new Point2D(x, y);
        //logger.info(MARKER.AI, "Random Point : {}", randPoint);
        return randPoint;
    }
}
