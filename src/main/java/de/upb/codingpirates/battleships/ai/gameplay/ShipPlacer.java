package de.upb.codingpirates.battleships.ai.gameplay;

import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.ai.logger.Markers;
import de.upb.codingpirates.battleships.ai.util.InvalidPointsHandler;
import de.upb.codingpirates.battleships.ai.util.RandomPointCreator;
import de.upb.codingpirates.battleships.ai.util.ZeroPointMover;
import de.upb.codingpirates.battleships.logic.PlacementInfo;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Rotation;
import de.upb.codingpirates.battleships.logic.ShipType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.Map.Entry;

/**
 * Creates a random ship placement.
 *
 * @author Benjamin Kasten
 */
public final class ShipPlacer {

    private final AI ai;

    private static final Logger logger = LogManager.getLogger();

    /**
     * Constructor for {@link ShipPlacer}. Gets an instance of the ai object which creates the {@link ShipPlacer}
     * instance.
     *
     * @param ai The instance of the ai who called the constructor.
     */
    public ShipPlacer(@Nonnull final AI ai) {
        this.ai = ai;
    }

    //contains all the Points which can not be accessed anymore like the distance to a ship or the ship positions;
    //is used for checking if a point can be used for placing a ship
    List<Point2D> usedPoints = new ArrayList<>();
    //remains false until a valid ship placement is found
    boolean successful = false;
    //the requested argument for the PlaceShipsRequest
    Map<Integer, PlacementInfo> positions = new HashMap<>();

    Map<Integer, List<Point2D>> placedShipMap = new HashMap<>();

    /**
     * Calls the {link {@link #guessRandomShipPositions(Map)}} method.
     *
     * @return positions of placed ships
     */
    public Map<Integer, PlacementInfo> placeShipsRandomly() {
        logger.info("Try placing ships");
        while (!successful)
            guessRandomShipPositions(ai.getConfiguration().getShips());
        logger.info(Markers.AI_SHIP_PLACER, "Placing ships successful");
        shipPrinter();
        return positions;
    }

    public void shipPrinter() {
        List<Point2D> placedShipsPos = new ArrayList<>();

        logger.debug("Placed ships are:");
        for (Entry<Integer, List<Point2D>> entry : placedShipMap.entrySet()) {
            placedShipsPos.addAll(entry.getValue());
            logger.debug("Ship positions of ship : " + entry.getKey());
            for (Point2D p : entry.getValue()) {
                logger.debug(p);
            }
        }
        String[][] field = new String[ai.getConfiguration().getHeight()][ai.getConfiguration().getWidth()];
        for (String[] i : field) {
            Arrays.fill(i, "-");
        }
        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[i].length; j++) {
                boolean ship = false;
                for (Point2D p : placedShipsPos) {
                    if (p.getY() == i & p.getX() == j) {
                        field[i][j] = "-";
                        ship = true;
                        break;
                    }
                }
                if (!ship) {
                    field[i][j] = "X";
                }
            }
        }
        List<String[]> list = Arrays.asList(field);
        Collections.reverse(list);

        logger.info("Printing ship placement map");
        for (String[] row : list) {
            StringJoiner sj = new StringJoiner(" | ");
            for (String col : row) {
                if (col.equals("-")) {
                    sj.add("X");

                } else {
                    sj.add("-");
                }
            }
            System.out.println(sj.toString());
        }
    }

    /**
     * Is called by placeShips() and places the ships randomly on the field. Leave the loop if the placement is not valid.
     *
     * @param shipConfig A map, which maps from the shipId to the ShipType (from the configuration)
     */
    public void guessRandomShipPositions(@Nonnull final Map<Integer, ShipType> shipConfig) {
        //clear all used values from the recent call for safety
        usedPoints.clear();
        positions.clear();

        //iterate through the the shipConfig Map for getting every key value pair
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            int xOffset = Collections.min(entry.getValue().getPositions(), Comparator.comparingInt(Point2D::getX)).getX();
            int yOffset = Collections.min(entry.getValue().getPositions(), Comparator.comparingInt(Point2D::getY)).getY();
            logger.debug("Offset x: {} ", xOffset);
            logger.debug("Offset y: {}", yOffset);

            //ship Id
            int shipId = entry.getKey();
            //all points of the ship
            Collection<Point2D> ship = entry.getValue().getPositions();

            //making all points positive using ZeroPointMo
            //new positive ship values
            Collection<Point2D> shipPositive = new ArrayList<>(new ZeroPointMover().moveToZeroPoint((ArrayList<Point2D>) ship));

            //use a RandomPointCreator to get a random point
            //the random point for positioning the ship
            Point2D guessPoint = new RandomPointCreator(ai.getConfiguration()).getRandomPoint2D();

            //the the distance from zeropoint to random guessPoint
            int distanceX = guessPoint.getX();
            int distanceY = guessPoint.getY();

            //the positions (points) of a ship will be stored here
            List<Point2D> tempShipPos = new ArrayList<>();

            //iterates through every point of the ship (all points in shipPos)
            for (Point2D i : shipPositive) {
                //creating new coordinates by moving every point in x and y direction: The moving distance came from
                //the guessPoint
                int newX = i.getX() + distanceX;
                int newY = i.getY() + distanceY;
                //create a new point for the new coordinates
                Point2D newPoint = new Point2D(newX, newY);

                //logger.info(MARKER.Ai_ShipPlacer, "New potential placement point: {}", newPoint);

                //check for each point in usePoints if the newPoint is already unavailable (is used)
                for (Point2D p : usedPoints) {
                    //if the newPoint is unavailable: delete usedPoints, positions and return
                    //-->starting the loop in placeShips again
                    if ((p.getX() == newPoint.getX()) & (p.getY() == newPoint.getY())) {
                        usedPoints.clear();
                        positions.clear();
                        placedShipMap.clear();
                        return;
                    }
                }

                //if the newPoint is not unavailable, check if the coordinates fits to the field:
                // No negative values, no greater values as the fields height and width
                if (newPoint.getY() < 0 | newPoint.getX() < 0 |
                        newPoint.getX() > (ai.getConfiguration().getWidth() - 1) | newPoint.getY() > (ai.getConfiguration().getHeight() - 1)) {
                    //if the newPoint is unavailable: delete usedPoints, positions and return
                    //-->starting the loop in placeShips again
                    usedPoints.clear();
                    positions.clear();
                    placedShipMap.clear();
                    return;
                } else {
                    // if the newPoint is valid...
                    // ...add the point to the tempShipPos ArrayList
                    tempShipPos.add(newPoint);
                    //...add the point to the usedPoints ArrayList
                    usedPoints.add(newPoint);
                }
            }

            InvalidPointsHandler invalidPointsHandler = new InvalidPointsHandler(this.ai);
            //after placing a ship, we have to add all surrounding points of the ship to the usedPoints Array
            //once they are in the usedPoints Array, they can not be used for placing ships anymore
            usedPoints.addAll(invalidPointsHandler.addSurroundingPointsToUsedPoints(tempShipPos));

            //find the placement info point position (the bottom left point)
            int minX = ai.getConfiguration().getWidth() + 1; // a value which cannot be reached
            int minY = ai.getConfiguration().getHeight() + 1; // ~

            //search for the lowest y position
            for (Point2D p : tempShipPos) {
                if (p.getY() < minY) {
                    minY = p.getY();
                }
            }
            //search for the lowest x position in y row
            for (Point2D p : tempShipPos) {
                if (p.getX() < minX & p.getY() == minY) {
                    minX = p.getX();
                }
            }
            PlacementInfo pInfo = new PlacementInfo(new Point2D(minX, minY), Rotation.NONE);

            //PlacementInfo pInfo = new PlacementInfo(new Point2D(minX - xOffset, minY - yOffset), Rotation.NONE);
            logger.debug("pInfo of ship {}: {}", ship, pInfo);

            positions.put(shipId, pInfo);
            placedShipMap.put(shipId, new ArrayList<>(tempShipPos));

            //clear the tempShipPos Array for the next loop
            tempShipPos.clear();

        }
        //is called only if the placing of the ships in the positions Map worked for all ships
        //responsible for leaving the while loop
        successful = true;
    }
}
