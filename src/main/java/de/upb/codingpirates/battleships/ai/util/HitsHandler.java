package de.upb.codingpirates.battleships.ai.util;

import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class HitsHandler {
    private static final Logger logger = LogManager.getLogger();

    Ai ai;

    public HitsHandler(Ai ai) {
        this.ai = ai;
    }

    /**
     * Creates a map which maps from the clientID on their hit points
     *
     * @return The map with ordered sunks
     */
    public HashMap<Integer, LinkedList<Shot>> sortTheHits() {

        HashMap<Integer, LinkedList<Shot>> sortedHits = Maps.newHashMap();
        for (Shot i : ai.getHits()) {
            int clientId = i.getClientId();
            boolean success = false;
            for (Map.Entry<Integer, LinkedList<Shot>> entry : sortedHits.entrySet()) {
                if (entry.getKey() == clientId) {
                    entry.getValue().add(i);
                    success = true;
                }
            }
            if (!success) {
                //sortedSunk.put(clientId, ai.createArrayListOneArgument(i));
                sortedHits.put(clientId, new LinkedList<>(Collections.singletonList(i)));
            }
        }
        for (Client c : ai.getClientArrayList()) {
            if (!sortedHits.containsKey(c.getId())) {
                sortedHits.put(c.getId(), new LinkedList<>(Collections.emptyList()));
            }
        }
        //logger.info(MARKER.AI, "Sorted the sunken ships by their clients.");
        for (Map.Entry<Integer, LinkedList<Shot>> entry : sortedHits.entrySet()) {
            if (entry.getValue().isEmpty()) {
                if (entry.getKey() == ai.getAiClientId()) {
                    logger.info("I ({}) have not yet been hit", ai.getAiClientId());
                } else {
                    logger.info("Player {} has not yet been hit.", entry.getKey());
                }
                continue;

            }
            if (entry.getKey() == ai.getAiClientId()) {
                logger.info("I ({}) have been hit {} timer", entry.getKey(), entry.getValue().size());
            } else {
                logger.info("Player {} has been hit {} times.", entry.getKey(), entry.getValue().size());
            }
        }
        return sortedHits;
    }

}
