package de.upb.codingpirates.battleships.ai.util;

import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;

import java.util.*;
import java.util.Map.Entry;

/**
 * Helper Class which handles and sorts the hits.
 *
 * @author Benjamin Kasten
 * @see SunkenShipsHandler
 */
public class HitsHandler {
    AI ai;

    /**
     * Constructor which gets the ai instance to know the gamestates
     *
     * @param ai the ai instance of the caller
     */
    public HitsHandler(AI ai) {
        this.ai = ai;
    }

    /**
     * Creates a map which maps from the clientID on their hit points
     *
     * @return The map with ordered sunks
     */
    public HashMap<Integer, List<Point2D>> sortTheHits() {

        HashMap<Integer, List<Point2D>> sortedHits = Maps.newHashMap();

        for (Shot i : ai.getHits()) {
            int clientId = i.getClientId();
            boolean success = false;
            for (Entry<Integer, List<Point2D>> entry : sortedHits.entrySet()) {
                if (entry.getKey() == clientId) {
                    entry.getValue().add(i.getTargetField());
                    success = true;
                }
            }
            if (!success)
                sortedHits.put(clientId, new LinkedList<>(Collections.singletonList(i.getTargetField())));
        }
        for (Client c : ai.getClientArrayList()) {
            if (!sortedHits.containsKey(c.getId()))
                sortedHits.put(c.getId(), new LinkedList<>(Collections.emptyList()));
        }
        return sortedHits;
    }

}
