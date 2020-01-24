package de.upb.codingpirates.battleships.ai.logger;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/** All-static class containing the various Log4j {@link Marker} instances used for logging purposes. */
public final class Markers {

    public final static Marker AI                      = MarkerManager.getMarker("AI");
    public final static Marker AI_SHORT_PLACER         = MarkerManager.getMarker("AI ShotPlacer");
    public final static Marker AI_MISSES_FINDER        = MarkerManager.getMarker("AI MissesFinder");
    public final static Marker AI_SUNKEN_SHIPS_HANDLER = MarkerManager.getMarker("AI SunkenShipsHandler");
    public final static Marker AI_SHIP_PLACER          = MarkerManager.getMarker("AI ShipPlacer");

    /* Prevent instantiation. */
    private Markers() {
    }
}
