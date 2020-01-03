package de.upb.codingpirates.battleships.ai.gameplay;

import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.ai.util.InvalidPointsHandler;
import de.upb.codingpirates.battleships.ai.util.RandomPointCreator;
import de.upb.codingpirates.battleships.ai.util.ZeroPointMover;
import de.upb.codingpirates.battleships.logic.PlacementInfo;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Rotation;
import de.upb.codingpirates.battleships.logic.ShipType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ShipPlacer {
    Ai ai;
    //Logger
    private static final Logger logger = LogManager.getLogger();

    /**
     * Constructor for {@link ShipPlacer}. Gets an instance of the ai object which creates the {@link ShipPlacer}
     * instance.
     *
     * @param ai The instance of the ai who called the constructor.
     */
    public ShipPlacer(Ai ai) {
        this.ai = ai;
    }

    //contains all the Points which can not be accessed anymore like the distance to a ship or the ship positions;
    //is used for checking if a point can be used for placing a ship
    ArrayList<Point2D> usedPoints = new ArrayList<>();
    //remains false until a valid ship placement is found
    boolean successful = false;
    //the requested argument for the PlaceShipsRequest
    Map<Integer, PlacementInfo> positions = new HashMap<>();

    /**
     * Calls the {link {@link #guessRandomShipPositions(Map)}} method.
     *
     * @return positions of placed ships
     */
    public Map<Integer, PlacementInfo> placeShipsRandomly() {
        while (!successful) {
            logger.info(MARKER.AI, "Placing ships failed");
            guessRandomShipPositions(ai.getShips());
        }
        logger.info(MARKER.AI, "Placing ships successful");
        return positions;
    }

    /**
     * Is called by placeShips() and places the ships randomly on the field. Leave the loop if the placement is not valid.
     *
     * @param shipConfig A map, which maps from the shipId to the ShipType (from the configuration)
     */
    public void guessRandomShipPositions(Map<Integer, ShipType> shipConfig) {
        //clear all used values from the recent call for safety
        usedPoints.clear();
        positions.clear();

        logger.info(MARKER.AI, "Started random guesser");
        logger.info(MARKER.AI, "Field height: {}", ai.getHeight());
        logger.info(MARKER.AI, "Field width: {}", ai.getWidth());

        //iterate through the the shipConfig Map for getting every key value pair
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            logger.info(MARKER.AI, "Entry of shipConfig Map with shipID: " + entry.getKey());
            //ship Id
            int shipId = entry.getKey();
            //all points of the ship
            Collection<Point2D> ship = entry.getValue().getPositions();

            for (Point2D j : ship) {
                logger.info(MARKER.AI, "Old point: {}", j);
            }

            //making all points positive using ZeroPointMover
            ZeroPointMover zeroPointMover = new ZeroPointMover();
            //new positive ship values
            Collection<Point2D> shipPositive = new ArrayList<>(zeroPointMover.moveToZeroPoint((ArrayList<Point2D>) ship));

            for (Point2D j : shipPositive) {
                logger.info(MARKER.AI, "New positive point: {}", j);
            }

            //use a RandomPointCreator to get a random point
            RandomPointCreator randomPointCreator = new RandomPointCreator(this.ai);

            //is the random point which will be the point for the PlacementInfo
            Point2D guessPoint = randomPointCreator.getRandomPoint2D();

            //for testing purpose
            logger.info(MARKER.AI, "Guess point: {}", guessPoint);

            //the the distance from zeropoint to random guessPoint
            int distanceX = guessPoint.getX();
            int distanceY = guessPoint.getY();

            //the positions (points) of a ship will be stored here
            ArrayList<Point2D> tempShipPos = new ArrayList<>();

            //iterates through every point of the ship (all points in shipPos)
            for (Point2D i : shipPositive) {
                //creating new coordinates by moving every point in x and y direction: The moving distance came from
                //the guessPoint
                int newX = i.getX() + distanceX;
                int newY = i.getY() + distanceY;
                //create a new point for the new coordinates
                Point2D newPoint = new Point2D(newX, newY);

                //for testing purpose
                logger.info(MARKER.AI, "New potential placement point: {}", newPoint);

                //check for each point in usePoints if the newPoint is already unavailable (is used)
                for (Point2D p : usedPoints) {
                    //if the newPoint is unavailable: delete usedPoints, positions and return
                    //-->starting the loop in placeShips again
                    if ((p.getX() == newPoint.getX()) & (p.getY() == newPoint.getY())) {
                        usedPoints.clear();
                        positions.clear();
                        logger.info(MARKER.AI, "Failed: new guess point already is not accessible. ");
                        return;

                    }
                }

                //if the newPoint is not unavailable, check if the coordinates fits to the field:
                // No negative values, no greater values as the fields height and width
                if (newPoint.getY() < 0 | newPoint.getX() < 0 |
                        newPoint.getX() > (ai.getWidth() - 1) | newPoint.getY() > (ai.getHeight() - 1)) {
                    //if the newPoint is unavailable: delete usedPoints, positions and return
                    //-->starting the loop in placeShips again
                    usedPoints.clear();
                    positions.clear();
                    logger.info(MARKER.AI, "Failed: new point coordinates do not fit the field ");
                    return;
                } else {
                    // if the newPoint is valid...
                    // ...add the point to the tempShipPos ArrayList
                    tempShipPos.add(newPoint);
                    //...add the point to the usedPoints ArrayList
                    usedPoints.add(newPoint);
                    //create a new PlacementInfo with the guessPoint (the guessPoint is valid)
                    PlacementInfo pInfo = new PlacementInfo(guessPoint, Rotation.NONE);
                    //add the shipId and the pInfo to positions Map
                    positions.put(shipId, pInfo);
                }
            }

            InvalidPointsHandler invalidPointsHandler = new InvalidPointsHandler(this.ai);
            //after placing a ship, we have to add all surrounding points of the ship to the usedPoints Array
            //once they are in the usedPoints Array, they can not be used for placing ships anymore
            usedPoints.addAll(invalidPointsHandler.addSurroundingPointsToUsedPoints(tempShipPos));
            //clear the tempShipPos Array for the next loop
            tempShipPos.clear();
        }
        //is called only if the placing of the ships in the positions Map worked for all ships
        //responsible for leaving the while loop in placeShips()
        successful = true;
    }


}
