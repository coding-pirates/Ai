package de.upb.codingpirates.battleships.ai.gameplay;

import com.google.common.collect.Lists;
import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.ai.util.*;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Implements the 3 possible shot methods with difficulty levels 1, 2 or 3.
 */
public class ShotPlacer {
    private static final Logger logger = LogManager.getLogger();
    Ai ai;

    public ShotPlacer(Ai ai) {
        this.ai = ai;
    }

    //difficulty level 1

    /**
     * Places shots randomly on the field of one opponent and sends the {@link ShotsRequest} message.
     * <p>
     * Difficulty level 1.
     *
     * @return shots
     */
    public Collection<Shot> placeShots_1(int shotCount) {
        RandomPointCreator randomPointCreator = new RandomPointCreator(this.ai);
        int numberOfClients = ai.getClientArrayList().size();
        int shotClientId;
        int aiIndex = -1;

        for (Client c : ai.getClientArrayList()) {
            if (c.getId() == ai.getAiClientId()) {
                aiIndex = ai.getClientArrayList().indexOf(c);
            }
        }

        while (true) {
            int randomIndex = (int) (Math.random() * numberOfClients);
            if (randomIndex != aiIndex) {
                shotClientId = ai.getClientArrayList().get(randomIndex).getId(); //shotClientId is the target for placing shots in the next part
                logger.info(MARKER.Ai_ShotPlacer, "Shooting on client with id: {} ", shotClientId);
                break;
            }
        }

        ArrayList<Shot> myShots = new ArrayList<>();

        //placing the shots randomly until the max of shots is not reached
        //all shots will be placed on the field of only one opponents field(other client)
        int i = 0;
        while (i < shotCount) {
            logger.info(MARKER.Ai_ShotPlacer, "Trying to find  {}. shot this round", i + 1);

            Point2D aimPoint = randomPointCreator.getRandomPoint2D();

            Shot targetShot = new Shot(shotClientId, aimPoint);
            boolean alreadyChoosen = false;


            for (Shot s : ai.getRequestedShots()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    logger.info(MARKER.Ai_ShotPlacer, "Shot was requested already: {}", targetShot);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            for (Shot s : ai.getHits()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    logger.info(MARKER.Ai_ShotPlacer, "Shot is a hit already: {}", targetShot);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            for (Shot s : ai.getMisses()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    logger.info(MARKER.Ai_ShotPlacer, "Shot is a miss already: {}", s);
                    break;
                }
            }
            if (alreadyChoosen) continue;


            myShots.add(targetShot);
            ai.requestedShots.add(targetShot);
            logger.info(MARKER.Ai_ShotPlacer, "Found shot {}", targetShot);
            i++;

        }

        //return the choosen shots
        logger.info("My shots: {}", myShots);
        return myShots;
    }


    //difficulty level 2 -----------------------------------------------------------------


    /**
     * Places shots using the hunt and target algorithm.
     * Difficulty level 2.
     *
     * @return requested shots
     */
    public Collection<Shot> placeShots_2() {
        logger.info(MARKER.Ai_ShotPlacer, "Placing shots with difficulty level 2");
        SunkenShipsHandler sunkenShipsHandler = new SunkenShipsHandler(this.ai);
        HitsHandler hitsHandler = new HitsHandler(this.ai);

        Map<Integer, LinkedList<Point2D>> sortedHits = hitsHandler.sortTheHits();
        Map<Integer, LinkedList<LinkedList<Point2D>>> sortedSunk = new HashMap<>();
        for (Map.Entry<Integer, LinkedList<Point2D>> entry : sortedHits.entrySet()) {
            int clientId = entry.getKey();
            LinkedList<LinkedList<Point2D>> sortedHitsByPosition = sunkenShipsHandler.findConnectedPoints(entry.getValue(), clientId);
            LinkedList<LinkedList<Point2D>> sortedSunkByPosition = sunkenShipsHandler.getSunksOfHits(sortedHitsByPosition, clientId);
            sortedSunk.put(clientId, sortedSunkByPosition);
        }

        Collection<Shot> myShots = new ArrayList<>();
        List<List<Shot>> pots = new ArrayList<>();

        logger.debug("Size hits: {}", ai.getHits().size());
        for (Shot s : ai.getHits()) {
            if (s.getClientId() != ai.getAiClientId()) {
                int id = s.getClientId();
                List<Shot> temp = new ArrayList<>();
                //west
                Shot west = new Shot(id, new Point2D(s.getTargetField().getX() - 1, s.getTargetField().getY()));

                if (checkValid(west)) {
                    temp.add(west);
                }
                //south
                Shot south = new Shot(id, new Point2D(s.getTargetField().getX(), s.getTargetField().getY() - 1));

                if (checkValid(south)) {
                    temp.add(south);
                }
                //east
                Shot east = new Shot(id, new Point2D(s.getTargetField().getX() + 1, s.getTargetField().getY()));

                if (checkValid(east)) {
                    temp.add(east);
                }
                //north
                Shot north = new Shot(id, new Point2D(s.getTargetField().getX(), s.getTargetField().getY() + 1));

                if (checkValid(north)) {
                    temp.add(north);
                }

                if (!temp.isEmpty()) {
                    logger.debug("temp is not empty: {}", temp);
                    pots.add(temp);
                }

            }
        }

        logger.debug("potential array: {}", pots);
        Collections.shuffle(pots); //shuffle the lists in pots
        for (List<Shot> l : pots) {
            for (Shot s : l) {
                myShots.add(s);
                logger.info("Added shot {} to myShots", s);
                ai.getRequestedShots().add(s);
                if (myShots.size() == ai.getShotCount()) {
                    return myShots;
                }
            }
        }
        if (myShots.size() < ai.getShotCount()) {
            logger.debug("Placing shots randomly");
            myShots.addAll(this.placeShots_1(ai.getShotCount() - myShots.size()));
            return myShots;
        }

        return myShots;
    }

    public boolean checkValid(Shot s) {
        boolean isValid = true;
        if (s.getTargetField().getX() < 0
                | s.getTargetField().getY() < 0
                | s.getTargetField().getX() > ai.getWidth() - 1
                | s.getTargetField().getY() > ai.getHeight() - 1) {
            isValid = false;
        }
        if (isValid) {
            for (Shot h : ai.getHits()) {
                if (PositionComparator.compareShots(s, h)) {
                    isValid = false;
                    break;
                }
            }
            for (Shot h : ai.getRequestedShots()) {
                if (PositionComparator.compareShots(s, h)) {
                    isValid = false;
                    break;
                }
            }

            for (Point2D h : ai.getSortedSunk().get(s.getClientId())) {
                if (PositionComparator.comparePointShot(h, s)) {
                    isValid = false;
                    break;
                }
            }
        }
        return isValid;
    }


    //difficulty level 3 ---------------------------------------------------------


    /**
     * Placing shots based on the relative value. Heatmaps will be created with relative values.
     *
     * @return
     */
    public Collection<Shot> placeShots_Relative_3() {

        for (Client c : ai.getClientArrayList()) {
            InvalidPointsHandler invalidPointsHandler = new InvalidPointsHandler(this.ai);
            LinkedList<Point2D> inv = invalidPointsHandler.createInvalidPointsOne(c.getId());
            ai.addPointsToInvalid(inv, c.getId());
        }

        HeatmapCreator heatmapCreator = new HeatmapCreator(this.ai);
        ai.setHeatmapAllClients(heatmapCreator.createHeatmapAllClients());
        //valid targets are the clients which are connected and can be targets for firing shots
        Map<Integer, LinkedList<Integer>> validTargets = new HashMap<>();
        //search for valid targets: all clients which are not this ai and have ships and have less invalid points than the field has points
        for (Map.Entry<Integer, LinkedList<Integer>> entry : ai.getAllSunkenShipIds().entrySet()) {
            if (!(ai.getInvalidPointsAll().get(entry.getKey()).size() == (ai.getWidth() * ai.getHeight())
                    | entry.getValue().size() == ai.getShips().size() | entry.getKey() == ai.getAiClientId())) {
                validTargets.put(entry.getKey(), entry.getValue());
            }
        }


        //allHeatVal is the collection of all valid points of all clients and those heat value
        LinkedList<Triple<Integer, Point2D, Double>> allHeatVal = Lists.newLinkedList();

        //using the class Triple store the triple in allHeatVal if the target is valid
        //for use of class Triple see the nested for loops
        for (Map.Entry<Integer, Double[][]> entry : ai.getHeatmapAllClients().entrySet()) {
            int clientId = entry.getKey();
            if (!validTargets.containsKey(clientId)) continue;
            for (int i = 0; i < entry.getValue().length; i++) {
                for (int j = 0; j < entry.getValue()[i].length; j++) {
                    //Triple is used like that to order and store the values of each heat point
                    Triple<Integer, Point2D, Double> t = new Triple<>(clientId, new Point2D(j, i), entry.getValue()[i][j]);
                    allHeatVal.add(t);
                }
            }
        }

        //remove all zero values of the heat points collection
        allHeatVal.removeIf((Triple<Integer, Point2D, Double> t) -> t.getVal3() == (double) 0);

        //using the TripleComparator class we can sort the the triple objects by their heat value
        allHeatVal.sort(new TripleComparator().reversed());


        ai.setAllHeatVal(allHeatVal);


        //all shots which will be fired this round
        Collection<Shot> myShotsThisRound = Lists.newArrayList();

        boolean valid;

        for (Triple<Integer, Point2D, Double> t : allHeatVal) {

            boolean isHit;

            for (Shot s : ai.getRequestedShots()) {
                isHit = false;
                for (Shot r : ai.getHits()) {
                    if (PositionComparator.compareShots(s, r)) {
                        isHit = true;
                        break;
                    }
                }
                if (!isHit) {
                    ai.addPointsToInvalid(s.getTargetField(), s.getClientId());
                    ai.getMisses().add(s);
                }
            }

            valid = true;

            int clientId = t.getVal1(); //client id
            Point2D p = t.getVal2(); //heat point
            double fieldVal = t.getVal3(); //heat value

            for (Point2D g : ai.getInvalidPointsAll().get(clientId)) {
                if (g.getX() == p.getX() & g.getY() == p.getY()) {
                    logger.debug(MARKER.Ai_ShotPlacer, "If this block is called something went wrong. {} is invalid", t);
                    valid = false;
                    break;
                }
            }
            for (Shot k : ai.getHits()) {
                if (k.getTargetField().getX() == p.getX() & k.getTargetField().getY() == p.getY() & k.getClientId() == clientId) {
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;

            Shot targetShot = new Shot(clientId, p);
            myShotsThisRound.add(targetShot);
            logger.info(MARKER.Ai_ShotPlacer, "Added shot {} with value {}", targetShot, fieldVal);
            if (myShotsThisRound.size() >= ai.getShotCount()) {
                break;
            }
        }
        ai.requestedShots.addAll(myShotsThisRound);

        return myShotsThisRound;


    }
}
