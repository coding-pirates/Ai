package de.upb.codingpirates.battleships.ai.logger;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Implements the markers for log4J logs.
 */
public class Markers {
    public final static Marker Ai_Main = MarkerManager.getMarker("Ai_Main");
    public final static Marker Ai = MarkerManager.getMarker("Ai");
    public final static Marker Ai_Heatmap = MarkerManager.getMarker("Ai_HeatmapCreator");
    public final static Marker Ai_ShotPlacer = MarkerManager.getMarker("Ai_ShotPlacer");
    public final static Marker Ai_Hits = MarkerManager.getMarker("Ai_HitsHandler");
    public final static Marker Ai_Misses = MarkerManager.getMarker("Ai_MissesFinder");
    public final static Marker Ai_SunkenShips = MarkerManager.getMarker("Ai_SunkenShipsHandler");
    public final static Marker Ai_ShipPlacer = MarkerManager.getMarker("ShipPlacer");

    private Markers() {
    }
}
