package de.upb.codingpirates.battleships.ai.util;

import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

public class MissesFinder {
    //Logger
    private static final Logger logger = LogManager.getLogger();

    Ai ai;

    public MissesFinder(Ai ai) {
        this.ai = ai;
    }

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
