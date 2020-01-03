package de.upb.codingpirates.battleships.ai.test;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.ai.util.RandomPointCreator;
import de.upb.codingpirates.battleships.logic.Point2D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Ai_RandomPointCreator_Test {
    static Ai ai = new Ai();

    @BeforeAll
    public static void create() {
        ai.setWidth(10);
        ai.setHeight(5);
    }

    @Test
    public void getRandomPoint2D_Test() {
        RandomPointCreator randomPointCreator = new RandomPointCreator(ai);
        Point2D randP = randomPointCreator.getRandomPoint2D();
        assertNotNull(randP);
        assertTrue(randP.getX() < ai.getWidth());
        assertTrue(randP.getY() < ai.getHeight());
        assertTrue(randP.getX() >= 0);
        assertTrue(randP.getY() >= 0);
    }

}
