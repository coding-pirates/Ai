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
import java.util.stream.Collectors;

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
    public Collection<Shot> placeShots_1() {
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
                logger.info(MARKER.AI, "Shooting on client with id: {} ", shotClientId);
                break;
            }
        }

        ArrayList<Shot> myShots = new ArrayList<>();

        //placing the shots randomly until the max of shots is not reached
        //all shots will be placed on the field of only one opponents field(other client)
        int i = 0;
        while (i < ai.getShotCount()) {
            logger.info(MARKER.AI, "Trying to find  {}. shot", i + 1);

            Point2D aimPoint = randomPointCreator.getRandomPoint2D();

            Shot targetShot = new Shot(shotClientId, aimPoint);
            boolean alreadyChoosen = false;

            for (Shot s : ai.requestedShots) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    logger.info("Shot was requested already: {}", targetShot);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            for (Shot h : ai.getHits()) {
                if (h.getTargetField().getX() == aimPoint.getX() & h.getTargetField().getY() == aimPoint.getY() & h.getClientId() == shotClientId) {
                    alreadyChoosen = true;
                    logger.info(MARKER.AI, "Shot is a hit already: {}", targetShot);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            for (Shot s : ai.getMisses()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    logger.info(MARKER.AI, "Shot is a miss already: {}", s);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            myShots.add(targetShot);
            ai.requestedShots.add(targetShot);
            logger.info(MARKER.AI, "Found shot {}", targetShot);
            i++;

        }

        //return the choosen shots
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
        logger.info(MARKER.AI, "Placing shots with difficulty level 2");
        RandomPointCreator randomPointCreator = new RandomPointCreator(this.ai);
        Collection<Shot> myShots = new ArrayList<>();
        //ai.addMisses();
        int shotClientId = 222;

        int aiIndex;
        for (Client c : ai.getClientArrayList()) {
            if (c.getId() == ai.getAiClientId()) {
                aiIndex = ai.getClientArrayList().indexOf(c);
                logger.info(MARKER.AI, "Ai Index: {}.", aiIndex);
            }
        }

        Set<Point2D> pot = new HashSet<>();
        for (Shot s : ai.getHits()) {
            if (s.getClientId() == shotClientId) {
                logger.info(MARKER.AI, "Looking for all neighbours of Shot {}", s);
                ArrayList<Point2D> temp = new ArrayList<>();
                //west
                temp.add(new Point2D(s.getTargetField().getX() - 1, s.getTargetField().getY()));
                //south
                temp.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY() - 1));
                //east
                temp.add(new Point2D(s.getTargetField().getX() + 1, s.getTargetField().getY()));
                //north
                temp.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY() + 1));

                boolean isHitOrMiss = false;
                for (Point2D p : temp) {
                    if (p.getX() >= 0 & p.getY() >= 0) {
                        for (Shot h : ai.getHits()) {
                            if (h.getTargetField().getX() == p.getX() & h.getTargetField().getY() == p.getY() & h.getClientId() == shotClientId) {
                                isHitOrMiss = true;
                                break;
                            }

                        }
                        if (isHitOrMiss) continue;
                        for (Shot h : ai.getMisses()) {
                            if (h.getTargetField().getX() == p.getX() & h.getTargetField().getY() == p.getY() & h.getClientId() == shotClientId) {
                                isHitOrMiss = true;
                                break;
                            }

                        }
                        if (isHitOrMiss) continue;
                    } else {
                        continue;
                    }
                    pot.add(p);
                    logger.info(MARKER.AI, "Added {} to potential hits", p);
                }
                if (pot.size() < ai.getShotCount()) {
                    logger.info(MARKER.AI, "There are less potential hits ({}) as possible shots ({})", pot.size(), ai.getShotCount());
                    for (Point2D p : pot) {
                        myShots.add(new Shot(shotClientId, new Point2D(p.getX(), p.getY())));
                        logger.info(MARKER.AI, "Added {} to myShots", p);
                    }
                    while (true) {
                        boolean invalid = false;
                        Point2D point = randomPointCreator.getRandomPoint2D();

                        for (Shot h : ai.getHits()) {
                            if (h.getTargetField().getX() == point.getX() & h.getTargetField().getY() == point.getY() & h.getClientId() == shotClientId) {
                                invalid = true;
                                break;
                            }

                        }
                        if (invalid) continue;
                        for (Shot h : ai.getMisses()) {
                            if (h.getTargetField().getX() == point.getX() & h.getTargetField().getY() == point.getY() & h.getClientId() == shotClientId) {
                                invalid = true;
                                break;
                            }

                        }
                        if (invalid) continue;
                        else {
                            myShots.add(new Shot(shotClientId, point));
                            logger.info(MARKER.AI, "Added random shot {} to myShots", point);
                        }
                        if (myShots.size() == ai.getShotCount()) break;
                    }
                } else {
                    logger.info(MARKER.AI, "There are more potential hits ({}) as possible shots ({})", pot.size(), ai.getShotCount());
                    for (Point2D p : pot) {
                        myShots.add(new Shot(shotClientId, p));
                        logger.info(MARKER.AI, "Added {} to myShots", p);
                        if (myShots.size() == ai.getShotCount()) break;
                    }
                }
            }
            if (myShots.size() == ai.getShotCount()) break;
        }
        pot.clear();
        ai.requestedShotsLastRound.clear();
        ai.requestedShotsLastRound.addAll(myShots);
        return myShots;

    }


    //difficulty level 3 ---------------------------------------------------------


    /**
     * Placing shots based on the relative value. Heatmaps will be created with relative values.
     *
     * @return
     */
    public Collection<Shot> placeShots_Relative_3() {
        //logger.info("Placing shots with difficulty level 3");

        for (Client c : ai.getClientArrayList()) {
            InvalidPointsHandler invalidPointsHandler = new InvalidPointsHandler(this.ai);
            LinkedList<Point2D> inv = invalidPointsHandler.createInvalidPointsOne(c.getId());
            ai.addPointsToInvalid(inv, c.getId());
        }

        HeatmapCreator heatmapCreator = new HeatmapCreator(this.ai);
        ai.setHeatmapAllClients(heatmapCreator.createHeatmapAllClients(2)); //Value 2 is the signal for the heatmap creator to create a relative heatmap.

        //valid targets are the clients which are connected and can be targets for firing shots
        Map<Integer, LinkedList<Integer>> validTargets = new HashMap<>();
        //search for valid targets: all clients which are not this ai and have ships and have less invalid points than the field has points
        for (Map.Entry<Integer, LinkedList<Integer>> entry : ai.getAllSunkenShipIds().entrySet()) {
            if (!(ai.getInvalidPointsAll().get(entry.getKey()).size() == (ai.getWidth() * ai.getHeight())
                    | entry.getValue().size() == ai.getShips().size() | entry.getKey() == ai.getAiClientId())) {
                //logger.info(MARKER.AI, "A valid target is client: {}", entry.getKey());
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


        //using an iterator to remove hits from allHeatVal
        ListIterator<Triple<Integer, Point2D, Double>> iterator = allHeatVal.listIterator();
        for (Shot s : ai.getHits()) {
            while (iterator.hasNext()) {
                Triple<Integer, Point2D, Double> t = iterator.next();
                if (t.getVal1() == s.getClientId() & t.getVal2().getX() == s.getTargetField().getX() & t.getVal2().getY() == s.getTargetField().getY()) {
                    iterator.remove();
                    //logger.debug("Removed {} because its a hit", t);
                }
            }
        }

        //remove all zero values of the heat points collection
        allHeatVal.removeIf((Triple t) -> (double) t.getVal3() == (double) 0);
        //logger.debug("Size allHeatVal after removing hits and zero values: {}", allHeatVal.size());

        //using the TripleComparator class we can sort the the triple objects by their heat value
        allHeatVal.sort(new TripleComparator().reversed());


        logger.debug("Size of valid points to shoot: {}", allHeatVal.size());
        logger.debug("Highest valid heat point is: {}", allHeatVal.get(0));
        //all shots which will be fired this round
        Collection<Shot> myShotsThisRound = Lists.newArrayList();

        boolean valid;

        for (Triple<Integer, Point2D, Double> t : allHeatVal) {

            boolean isHit;

            for (Shot s : ai.requestedShots) {
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
                    logger.debug("If this block is called something went wrong. {} is invalid", t);
                    valid = false;
                    break;
                }
            }
            for (Shot k : ai.getHits()) {
                if (k.getTargetField().getX() == p.getX() & k.getTargetField().getY() == p.getY()) {
                    logger.debug("If this block is called something went wrong. {} is already a hit", p);
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;

            Shot targetShot = new Shot(clientId, p);
            myShotsThisRound.add(targetShot);
            logger.info("Added shot {} with value {} to myShots", targetShot, fieldVal);
            if (myShotsThisRound.size() >= ai.getShotCount()) {
                break;
            }
        }
        //logger.info("Size requested shots this round: {}", myShotsThisRound.size());
        ai.requestedShots.addAll(myShotsThisRound);

        return myShotsThisRound;


    }


    /**
     * The approach in this version is to shoot a the field with the maximum of invalid points.
     * Heatmaps will be created with absolute values.
     * <p>
     * Difficulty level 3.
     *
     * <p>
     * The approach explained more detailed: n.a.
     *
     * @return shots
     * @deprecated placeShots_3_2 uses relative values
     */
    public Collection<Shot> placeShots_Absolute_3() {
        logger.info(MARKER.AI, "Placing shots with difficulty level 3");


        //We have to add the shots of the last round directly to the invalidPointsAll before we create new heatmaps

        for (Shot s : ai.requestedShotsLastRound) {
            ai.addPointsToInvalid(s.getTargetField(), s.getClientId());
        }
        HeatmapCreator heatmapCreator = new HeatmapCreator(this.ai);
        ai.setHeatmapAllClients(heatmapCreator.createHeatmapAllClients(1));

        Map<Integer, LinkedList<Integer>> validTargets = new HashMap<>();

        for (Map.Entry<Integer, LinkedList<Integer>> entry : ai.getAllSunkenShipIds().entrySet()) {
            if (!(ai.getInvalidPointsAll().get(entry.getKey()).size() == (ai.getWidth() * ai.getHeight())
                    | entry.getValue().size() == ai.getShips().size() | entry.getKey() == ai.getAiClientId())) {
                logger.info(MARKER.AI, "A valid target is client: {}", entry.getKey());
                validTargets.put(entry.getKey(), entry.getValue());
            }
        }
        Map<Integer, Integer> invalidPointsSize = new HashMap<>();

        for (Map.Entry<Integer, LinkedList<Point2D>> entry : ai.getInvalidPointsAll().entrySet()) {
            logger.info(MARKER.AI, "Client {} has {} invalid fields", entry.getKey(), entry.getValue().size());
            if (validTargets.containsKey(entry.getKey())) {
                invalidPointsSize.put(entry.getKey(), entry.getValue().size());
            }
        }

        LinkedHashMap<Integer, Integer> invalidPointsSizeOrdered = invalidPointsSize.entrySet()
                .stream()
                .sorted((Map.Entry.<Integer, Integer>comparingByValue().reversed()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        Collection<Shot> myShotsThisRound = new ArrayList<>();
        int counter = 0;
        while (counter < validTargets.keySet().size()) {
            ai.setHeatmapAllClients(heatmapCreator.createHeatmapAllClients(1));
            for (Map.Entry<Integer, Integer> entry : invalidPointsSizeOrdered.entrySet()) {
                int countShotsOne = 0;
                int targetClient = entry.getKey();
                logger.info(MARKER.AI, "Now shooting on client {} with {} invalid fields", targetClient, entry.getValue());

                //get the heatmap of this client
                Double[][] targetHeatmap = ai.getHeatmapAllClients().get(targetClient);

                Map<Point2D, Double> pointsToValue = new LinkedHashMap<>(); //ordnet jedem Punkt seinen Wert zu

                for (int row = 0; row < targetHeatmap.length; row++) {
                    for (int col = 0; col < targetHeatmap[row].length; col++) {
                        pointsToValue.put(new Point2D(col, row), targetHeatmap[row][col]);
                    }
                }
                //sortiere die pointsToValue nach ihrem value
                LinkedHashMap<Point2D, Double> pointsToValueOrdered = pointsToValue.entrySet()
                        .stream()
                        .sorted((Map.Entry.<Point2D, Double>comparingByValue().reversed()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                Map<Point2D, Double> temp = new LinkedHashMap<>(pointsToValueOrdered);
                //entferne die Punkte aus den pointsToValueOrdered die kleiner gleich 0 sind
                for (Map.Entry<Point2D, Double> entryy : temp.entrySet()) {
                    if (entryy.getValue() <= 0) {
                        pointsToValueOrdered.remove(entryy.getKey());
                    }
                }

                for (Map.Entry<Point2D, Double> entryy : pointsToValueOrdered.entrySet()) {

                    Shot targetShot = (new Shot(targetClient, new Point2D(entryy.getKey().getX(), entryy.getKey().getY())));
                    logger.debug("Target shot is {} with value {}", targetShot, entryy.getValue());
                    boolean isInvalid = false;
                    for (Point2D p : ai.getInvalidPointsAll().get(entry.getKey())) {
                        if (entryy.getKey().getX() == p.getX() & entryy.getKey().getY() == p.getY()) {
                            isInvalid = true;
                            logger.debug("Target shot is invalid ");
                            break;
                        }
                    }
                    if (isInvalid) continue;
                    myShotsThisRound.add(targetShot);
                    logger.info(MARKER.Ai_SHOTPLACER, "Added shot {} with value {} to myShots", targetShot, entryy.getValue());
                    countShotsOne++;
                    if (myShotsThisRound.size() == ai.getShotCount()) {
                        logger.info(MARKER.Ai_SHOTPLACER, "{} shots on client {}", countShotsOne, entry.getKey());
                        ai.requestedShotsLastRound.clear(); //leere die Liste von dieser Runde
                        ai.requestedShotsLastRound.addAll(myShotsThisRound);

                        ai.getInvalidPointsAll().get(targetClient).add(targetShot.getTargetField());
                        return myShotsThisRound;

                    }
                }
                counter++;
                logger.info(MARKER.AI, "{} shot(s) on client {}", countShotsOne, entry.getKey());
            }
        }
        return myShotsThisRound;
    }
}
