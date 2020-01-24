package de.upb.codingpirates.battleships.ai.util;

import java.util.Random;

import javax.annotation.Nonnull;

import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.Point2D;

/**
 * Creates a random {@link Point2D} object.
 *
 * @author Benjamin Kasten
 */
public final class RandomPointCreator {

    @Nonnull
    private final Configuration configuration;

    private final Random random = new Random();

    /**
     * Constructor for {@link RandomPointCreator}. Gets an instance of the {@link Configuration} object which creates
     * the {@link RandomPointCreator} instance. Is used to get the right field parameters (width and height).
     *
     * @param configuration The {@link Configuration} for which random {@link Point2D} objects on the playing field are
     *                      to be created.
     */
    public RandomPointCreator(@Nonnull final Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Creates a random {@link Point2D} object related to the width and height of the game field of the ai.
     *
     * @return Point2d Random Point with X and Y coordinates
     */
    public Point2D getRandomPoint2D() {
        return new Point2D(random.nextInt(configuration.getWidth()), random.nextInt(configuration.getHeight()));
    }
}
