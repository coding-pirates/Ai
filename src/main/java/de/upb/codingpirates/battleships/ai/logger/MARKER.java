package de.upb.codingpirates.battleships.ai.logger;

import de.upb.codingpirates.battleships.ai.gameplay.ShotPlacer;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class MARKER {
    public final static Marker AI = MarkerManager.getMarker("Ai");
    public final static  Marker AI_HEATMAP = MarkerManager.getMarker("Ai-HeatmapCreator");
    public final static  Marker Ai_SHOTPLACER = MarkerManager.getMarker("Ai- ShotPlacer");
}
