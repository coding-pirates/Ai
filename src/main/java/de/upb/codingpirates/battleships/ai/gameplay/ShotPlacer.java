package de.upb.codingpirates.battleships.ai.gameplay;

import com.google.common.collect.Lists;
import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.ai.logger.Markers;
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
    AI ai;

    public ShotPlacer(AI ai) {
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
        RandomPointCreator randomPointCreator = new RandomPointCreator(this.ai.getConfiguration());
        int numberOfClients = ai.getClientArrayList().size();
        int shotClientId;
        int aiIndex = -1;

        for (Client c : ai.getClientArrayList()) {
            if (c.getId() == ai.getAiClientId()) {
                aiIndex = ai.getClientArrayList().indexOf(c);
            }
        }

        Map<Integer, LinkedList<Point2D>> sortedHIts = new HitsHandler(this.ai).sortTheHits();
        while (true) {

            ArrayList<Client> client = new ArrayList<>(ai.getClientArrayList());
            Collections.shuffle(client);
            if (sortedHIts.get(client.get(0).getId()).size() == ai.getSizeOfPointsToHit()) continue;
            if (client.get(0).getId() != ai.getAiClientId()) {
                shotClientId = client.get(0).getId();
                logger.info(Markers.Ai_ShotPlacer, "Shooting on client with id: {} ", shotClientId);
                break;
            }
        }

        ArrayList<Shot> myShots = new ArrayList<>();

        //placing the shots randomly until the max of shots is not reached
        //all shots will be placed on the field of only one opponents field(other client)
        int i = 0;
        while (i < shotCount) {
            //logger.info(MARKER.Ai_ShotPlacer, "Trying to find  {}. shot this round", i + 1);

            Point2D aimPoint = randomPointCreator.getRandomPoint2D();

            Shot targetShot = new Shot(shotClientId, aimPoint);
            boolean alreadyChoosen = false;


            for (Shot s : ai.getRequestedShots()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    //logger.info(MARKER.Ai_ShotPlacer, "Shot was requested already: {}", targetShot);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            for (Shot s : ai.getHits()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    //logger.info(MARKER.Ai_ShotPlacer, "Shot is a hit already: {}", targetShot);
                    break;
                }
            }
            if (alreadyChoosen) continue;

            for (Shot s : ai.getMisses()) {
                if (PositionComparator.compareShots(s, targetShot)) {
                    alreadyChoosen = true;
                    //logger.info(MARKER.Ai_ShotPlacer, "Shot is a miss already: {}", s);
                    break;
                }
            }
            if (alreadyChoosen) continue;


            myShots.add(targetShot);
            ai.requestedShots.add(targetShot);
            logger.info(Markers.Ai_ShotPlacer, "Found shot {}", targetShot);
            i++;

        }

        //return the choosen shots
        logger.info("My shots: {}", myShots);
        return myShots;
    }


    //difficulty level 2 -----------------------------------------------------------------


    /**
     * Implements the hunt & target algorithm mentioned on datagenetics blog.
     *
     * @return shots to fire
     */
    public Collection<Shot> placeShots_2() {

        Collection<Shot> surroundingInv = new HashSet<>();


        Map<Integer, LinkedList<Point2D>> sortedHIts = new HitsHandler(this.ai).sortTheHits();
        SunkenShipsHandler sunkenShipsHandler = new SunkenShipsHandler(this.ai);

        Map<Integer, LinkedList<LinkedList<Point2D>>> connectedNotClean = new HashMap<>();
        for (Client c : ai.getClientArrayList()) {
            LinkedList<LinkedList<Point2D>> temp = sunkenShipsHandler.findConnectedPoints(sortedHIts.get(c.getId()), c.getId());
            connectedNotClean.put(c.getId(), temp);
        }

        System.out.println("Connected not cleaned:");
        System.out.println(connectedNotClean);

        Map<Integer, LinkedList<LinkedList<Point2D>>> connected = new HashMap<>();


        boolean isValid;

        for (Map.Entry<Integer, LinkedList<LinkedList<Point2D>>> entry : connectedNotClean.entrySet()) {

            if (ai.getSizeOfPointsToHit() == sortedHIts.get(entry.getKey()).size()) continue;

            if (entry.getKey() == ai.getAiClientId()) continue;
            int id = entry.getKey();
            LinkedList<LinkedList<Point2D>> clean = new LinkedList<>();
            for (LinkedList<Point2D> l : entry.getValue()) {
                isValid = true;
                for (Point2D p : l) {
                    for (Shot s : ai.getSunk()) {
                        if (PositionComparator.comparePointShot(p, s, id)) {
                            isValid = false;
                            //logger.debug("A sunk inside {}: {}", l, s);
                            break;
                        }
                    }
                    if (!isValid) break;
                }

                if (isValid) {
                    ArrayList<Point2D> temp = new ArrayList<>(l);
                    ArrayList<Point2D> temp1 = new ArrayList<>(new InvalidPointsHandler(this.ai).addSurroundingPointsToUsedPoints(temp));
                    for (Point2D p : temp1) {
                        surroundingInv.add(new Shot(id, p));
                    }

                    clean.add(l);
                    //logger.debug("Added {} to cleaned --> no sunk inside", l);
                }
            }
            connected.put(id, new LinkedList<>(clean));
        }
        //logger.debug("Connected Cleaned:");
        System.out.println(connected);

        Map<Integer, LinkedList<Set<Shot>>> pots = new HashMap<>();
        for (Map.Entry<Integer, LinkedList<LinkedList<Point2D>>> entry : connected.entrySet()) {

            // logger.debug("The connected hits of client {}:", entry.getKey());
            logger.debug(entry.getValue());

            if (entry.getKey() == ai.getAiClientId()) {
                //  logger.debug("Skip own id for shooting");
                continue;
            }
            LinkedList<Set<Shot>> temp = new LinkedList<>();
            int id = entry.getKey();
            //logger.debug("Client id : {}", id);

            for (LinkedList<Point2D> l : entry.getValue()) {

                Set<Shot> temp2 = new HashSet<>();
                for (Point2D p : l) {
                    //west
                    Shot west = new Shot(id, new Point2D(p.getX() - 1, p.getY()));

                    if (checkValid(west)) {
                        temp2.add(west);
                    }
                    //south
                    Shot south = new Shot(id, new Point2D(p.getX(), p.getY() - 1));

                    if (checkValid(south)) {
                        temp2.add(south);
                    }
                    //east
                    Shot east = new Shot(id, new Point2D(p.getX() + 1, p.getY()));

                    if (checkValid(east)) {
                        temp2.add(east);
                    }
                    //north
                    Shot north = new Shot(id, new Point2D(p.getX(), p.getY() + 1));

                    if (checkValid(north)) {
                        temp2.add(north);
                    }
                }
                if (!temp2.isEmpty()) {
                    temp.add(temp2);
                }
            }

            if (!temp.isEmpty()) {
                //logger.debug("Added this points {} of client {}", temp, id);
                pots.put(id, temp);
            }
        }

        Collection<Shot> myShots = new ArrayList<>();
        for (
                Map.Entry<Integer, LinkedList<Set<Shot>>> entry : pots.entrySet()) {
            for (Set<Shot> l : entry.getValue()) {
                for (Shot s : l) {
                    myShots.add(s);
                    logger.debug("Added shot {} to myShots", s);
                    ai.requestedShots.add(s);
                    if (myShots.size() == ai.getConfiguration().getShotCount()) {
                        return myShots;
                    }

                }
            }
        }
        while (myShots.size() < ai.getConfiguration().getShotCount()) {
            //get random shots


            Collections.shuffle(ai.getClientArrayList());


            int targetClientId = ai.getClientArrayList().get(0).getId();


            if (targetClientId != ai.getAiClientId()) {
                Shot targetShot = new Shot(targetClientId, new RandomPointCreator(this.ai.getConfiguration()).getRandomPoint2D());
                boolean valid = true;

                for (Shot s : surroundingInv) {
                    if (PositionComparator.compareShots(targetShot, s)) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) continue;

                for (Shot s : ai.getHits()) {
                    if (PositionComparator.compareShots(targetShot, s)) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) continue;

                for (Shot s : ai.getMisses()) {
                    if (PositionComparator.compareShots(targetShot, s)) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) continue;

                for (Shot s : ai.getRequestedShots()) {

                    if (PositionComparator.compareShots(targetShot, s)) {
                        valid = false;
                        break;
                    }

                }
                if (!valid) continue;

                myShots.add(targetShot);
                ai.requestedShots.add(targetShot);
                logger.debug("Added random shot {}", targetShot);
            }
        }
        return myShots;

    }

    public boolean checkValid(Shot s) {
        //logger.debug("Checking validity of shot {}", s);

        if (s.getTargetField().getX() < 0
                | s.getTargetField().getY() < 0
                | s.getTargetField().getX() > ai.getConfiguration().getWidth() - 1
                | s.getTargetField().getY() > ai.getConfiguration().getHeight() - 1) {
            //logger.debug("Doesnt fit the field");
            return false;

        }

        Map<Integer, LinkedList<Point2D>> sortedHits = new HitsHandler(this.ai).sortTheHits();

        for (Shot d : ai.getHits()) {
            if (PositionComparator.compareShots(s, d)) {
                //logger.debug("{} is a hit already", s);
                return false;
            }
        }

        for (Shot d : ai.getMisses()) {
            if (PositionComparator.compareShots(s, d)) {
                //logger.debug("{} is a miss already", s);
                return false;
            }
        }
        return true;
    }


    //difficulty level 3 ---------------------------------------------------------


    /**
     * Placing shots based on the relative value. Heatmaps will be created with relative values.
     *
     * @return shots to fire
     */
    public Collection<Shot> placeShots_Relative_3() {

        for (Client c : ai.getClientArrayList()) {
            InvalidPointsHandler invalidPointsHandler = new InvalidPointsHandler(this.ai);
            LinkedList<Point2D> inv = invalidPointsHandler.createInvalidPointsOne(c.getId());
            ai.addPointsToInvalid(inv, c.getId());
        }

        HeatmapCreator heatmapCreator = new HeatmapCreator(this.ai);
        ai.setHeatMapAllClients(heatmapCreator.createHeatmapAllClients());
        //valid targets are the clients which are connected and can be targets for firing shots
        Map<Integer, LinkedList<Integer>> validTargets = new HashMap<>();
        //search for valid targets: all clients which are not this ai and have ships and have less invalid points than the field has points
        for (Map.Entry<Integer, LinkedList<Integer>> entry : ai.getAllSunkenShipIds().entrySet()) {
            if (!(ai.getInvalidPointsAll().get(entry.getKey()).size() == (ai.getConfiguration().getWidth() * ai.getConfiguration().getHeight())
                    | entry.getValue().size() == ai.getConfiguration().getShips().size() | entry.getKey() == ai.getAiClientId())) {
                validTargets.put(entry.getKey(), entry.getValue());
            }
        }


        //allHeatVal is the collection of all valid points of all clients and those heat value
        LinkedList<Triple<Integer, Point2D, Double>> allHeatVal = Lists.newLinkedList();

        //using the class Triple store the triple in allHeatVal if the target is valid
        //for use of class Triple see the nested for loops
        for (Map.Entry<Integer, Double[][]> entry : ai.getHeatMapAllClients().entrySet()) {
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
                    logger.debug(Markers.Ai_ShotPlacer, "If this block is called something went wrong. {} is invalid", t);
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
            logger.info(Markers.Ai_ShotPlacer, "Added shot {} with value {}", targetShot, fieldVal);
            if (myShotsThisRound.size() >= ai.getConfiguration().getShotCount()) {
                break;
            }
        }
        ai.requestedShots.addAll(myShotsThisRound);

        return myShotsThisRound;


    }
}
