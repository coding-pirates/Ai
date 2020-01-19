package de.upb.codingpirates.battleships.ai.util;

import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class HitsHandler {
    private static final Logger logger = LogManager.getLogger();

    AI ai;

    public HitsHandler(AI ai) {
        this.ai = ai;
    }

    /**
     * Creates a map which maps from the clientID on their hit points
     *
     * @return The map with ordered sunks
     */
    public HashMap<Integer, LinkedList<Point2D>> sortTheHits() {

        HashMap<Integer, LinkedList<Point2D>> sortedHits = Maps.newHashMap();

        for (Shot i : ai.getHits()) {
            int clientId = i.getClientId();
            boolean success = false;
            for (Map.Entry<Integer, LinkedList<Point2D>> entry : sortedHits.entrySet()) {
                if (entry.getKey() == clientId) {
                    entry.getValue().add(i.getTargetField());
                    success = true;
                }
            }
            if (!success) {
                //sortedSunk.put(clientId, ai.createArrayListOneArgument(i));
                sortedHits.put(clientId, new LinkedList<>(Collections.singletonList(i.getTargetField())));
            }
        }
        for (Client c : ai.getClientArrayList()) {
            if (!sortedHits.containsKey(c.getId())) {
                sortedHits.put(c.getId(), new LinkedList<>(Collections.emptyList()));
            }
        }
        //logger.info(MARKER.AI, "Sorted the sunken ships by their clients.");
        for (Map.Entry<Integer, LinkedList<Point2D>> entry : sortedHits.entrySet()) {
            if (entry.getValue().isEmpty()) {

                //logger.info(Markers.Ai_Hits, "Player {} has not yet been hit.", entry.getKey());

                continue;
            }
            //logger.info(Markers.Ai_Hits, "Player {} has been hit {} times.", entry.getKey(), entry.getValue().size());

        }
        return sortedHits;
    }

}
