package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class for calculating new misses of one player. Is called by {@link Ai}.
 *
 * @author Benjamin Kasten
 */
public class MissesFinder {
    private static final Logger logger = LogManager.getLogger();
    Ai ai;

    /**
     * Constructor for {@link MissesFinder}. Gets an instance of the ai object which creates the {@link MissesFinder}
     * instance.
     *
     * @param ai The instance of the ai who called the constructor.
     */
    public MissesFinder(Ai ai) {
        this.ai = ai;
    }

    /**
     * Computes the misses of the last round by getting the requested shots of the last round.
     *
     * @return The misses of the last round of the ai player.
     */
    public Collection<Shot> computeMisses() {
        logger.info(MARKER.AI, "Compute the misses of last round");
        Collection<Shot> tempMisses = new ArrayList<>();
        for (Shot s : ai.requestedShotsLastRound) {
            boolean miss = true; //assume the shot s is a miss
            for (Shot i : ai.getHits()) { //check if shot s is a miss
                if (i.getTargetField().getX() == s.getTargetField().getX() & i.getTargetField().getY() == s.getTargetField().getY() & s.getClientId() == i.getClientId()) {
                    miss = false; //no miss, its a hit
                    logger.info(MARKER.AI, "A hit {}", s);
                }
            }
            if (miss) {
                tempMisses.add(s); // if its not hit, its a miss
                logger.info(MARKER.AI, "A miss {}", s);
            }
        }
        logger.info(MARKER.AI, "Found {} misses last round.", tempMisses.size());
        return tempMisses;
    }


}
