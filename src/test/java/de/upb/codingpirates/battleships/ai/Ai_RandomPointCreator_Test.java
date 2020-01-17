package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.ai.util.RandomPointCreator;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.Point2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Ai_RandomPointCreator_Test {

    @Test
    public void getRandomPoint2D_Test() {
        final Configuration configuration = new Configuration.Builder()
            .width(10)
            .height(10)
            .build();
        final RandomPointCreator randomPointCreator = new RandomPointCreator(configuration);

        Point2D randP = randomPointCreator.getRandomPoint2D();
        assertNotNull(randP);
        assertTrue(randP.getX() < configuration.getWidth());
        assertTrue(randP.getY() < configuration.getHeight());
        assertTrue(randP.getX() >= 0);
        assertTrue(randP.getY() >= 0);
    }
}
