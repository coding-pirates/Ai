package de.upb.codingpirates.battleship.ai.logger;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class MARKER {
    public final static Marker invalid_points = MarkerManager.getMarker("SETTING INVALID POINTS");
    public final static Marker shot_placement = MarkerManager.getMarker("SHOT PLACEMENT");
    public final static Marker heatmap = MarkerManager.getMarker("HEATMAP");
    public final static Marker check = MarkerManager.getMarker("CHECK");
}
