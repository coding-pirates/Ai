package de.upb.codingpirates.battleships.ai.gameplay;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.ai.logger.Markers;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.Shot;
import de.upb.codingpirates.battleships.ai.util.*;

import static java.util.stream.Collectors.toList;

public enum StandardShotPlacementStrategy implements ShotPlacementStrategy {

    RANDOM(1) {
        private Client selectRandomOpponent(final AI ai) {
            final Map<Integer, List<Point2D>> sortedHits = new HitsHandler(ai).sortTheHits();

            final List<Client> remainingOpponents =
                ai.getClientArrayList()
                    .stream()
                    .filter(client -> client.getId() != ai.getAiClientId())
                    .filter(client -> sortedHits.get(client.getId()).size() != ai.getSizeOfPointsToHit())
                    .collect(toList());
            return remainingOpponents.get(RAND.nextInt(remainingOpponents.size()));
        }

        @Override
        public Collection<Shot> calculateShots(final AI ai, final int shotCount) {
            final int targetOpponentId = selectRandomOpponent(ai).getId();

            final List<Shot> myShots = new ArrayList<>(shotCount);

            //placing the shots randomly until the max of shots is not reached
            //all shots will be placed on the field of only one opponents field(other client)
            int i = 0;
            while (i < shotCount) {
                Point2D aimPoint = new RandomPointCreator(ai.getConfiguration()).getRandomPoint2D();

                Shot targetShot = new Shot(targetOpponentId, aimPoint);
                boolean alreadyChosen = false;

                for (Shot s : ai.getRequestedShots()) {
                    if (PositionComparator.compareShots(s, targetShot)) {
                        alreadyChosen = true;
                        break;
                    }
                }
                if (alreadyChosen) continue;

                for (Shot s : ai.getHits()) {
                    if (PositionComparator.compareShots(s, targetShot)) {
                        alreadyChosen = true;
                        break;
                    }
                }
                if (alreadyChosen) continue;

                for (Shot s : ai.getMisses()) {
                    if (PositionComparator.compareShots(s, targetShot)) {
                        alreadyChosen = true;
                        break;
                    }
                }
                if (alreadyChosen) continue;

                myShots.add(targetShot);
                ai.requestedShots.add(targetShot);
                LOGGER.info(Markers.AI_SHOT_PLACER, "Found shot {}", targetShot);
                i++;
            }
            return myShots;
        }
    },

    /**
     * Implements the hunt and target algorithm mentioned in the following blog post:
     * http://www.datagenetics.com/blog/december32011/
     */
    HUNT_AND_TARGET(2) {
        public boolean checkValid(final AI ai, final Shot s) {
            if (s.getTargetField().getX() < 0
                    || s.getTargetField().getY() < 0
                    || s.getTargetField().getX() > ai.getConfiguration().getWidth() - 1
                    || s.getTargetField().getY() > ai.getConfiguration().getHeight() - 1)
                return false;

            for (Shot d : ai.getHits()) {
                if (PositionComparator.compareShots(s, d))
                    return false;
            }

            for (Shot d : ai.getMisses()) {
                if (PositionComparator.compareShots(s, d))
                    return false;
            }
            return true;
        }

        @Override
        public Collection<Shot> calculateShots(final AI ai, final int shotCount) {
            Collection<Shot> surroundingInv = new HashSet<>();

            Map<Integer, List<Point2D>> sortedHIts = new HitsHandler(ai).sortTheHits();
            SunkenShipsHandler sunkenShipsHandler = new SunkenShipsHandler(ai);

            Map<Integer, List<List<Point2D>>> connectedNotClean = new HashMap<>();
            for (Client c : ai.getClientArrayList()) {
                List<List<Point2D>> temp = sunkenShipsHandler.findConnectedPoints(sortedHIts.get(c.getId()), c.getId());
                connectedNotClean.put(c.getId(), temp);
            }


            //connected collection includes all related hits which are valid for use in next step
            Map<Integer, List<List<Point2D>>> connected = new HashMap<>();

            boolean isValid;

            for (Entry<Integer, List<List<Point2D>>> entry : connectedNotClean.entrySet()) {

                //replace with idDead attribute of clients could be useful
                //shooting on a field of a dead player has no sense
                if (ai.getSizeOfPointsToHit() == sortedHIts.get(entry.getKey()).size()) {
                    continue;
                }
                //own connected hit are not valid
                if (entry.getKey() == ai.getAiClientId()) {
                    continue;
                }
                int id = entry.getKey();
                List<List<Point2D>> clean = new LinkedList<>();
                for (List<Point2D> l : entry.getValue()) {
                    isValid = true;
                    for (Point2D p : l) {
                        for (Shot s : ai.getSunk()) {
                            if (PositionComparator.comparePointShot(p, s, id)) {
                                isValid = false;
                                break;
                            }
                        }
                        if (!isValid) break;
                    }

                    if (isValid) {
                        List<Point2D> temp = new ArrayList<>(l);
                        List<Point2D> temp1 = new ArrayList<>(new InvalidPointsHandler(ai).addSurroundingPointsToUsedPoints(temp));
                        for (Point2D p : temp1) {
                            surroundingInv.add(new Shot(id, p));
                        }
                        clean.add(l);
                    }
                }
                connected.put(id, new LinkedList<>(clean));
            }

            //pots is the map which stores the potential surrounding hits for each opponent
            Map<Integer, List<Set<Shot>>> pots = new HashMap<>();
            for (Entry<Integer, List<List<Point2D>>> entry : connected.entrySet()) {
                if (entry.getKey() == ai.getAiClientId())
                    continue;
                List<Set<Shot>> temp = new LinkedList<>();
                int id = entry.getKey();

                for (List<Point2D> l : entry.getValue()) {
                    Set<Shot> temp2 = new HashSet<>();
                    for (Point2D p : l) {
                        Shot west = new Shot(id, new Point2D(p.getX() - 1, p.getY()));
                        if (checkValid(ai, west))
                            temp2.add(west);

                        Shot south = new Shot(id, new Point2D(p.getX(), p.getY() - 1));
                        if (checkValid(ai, south))
                            temp2.add(south);

                        Shot east = new Shot(id, new Point2D(p.getX() + 1, p.getY()));
                        if (checkValid(ai, east))
                            temp2.add(east);

                        Shot north = new Shot(id, new Point2D(p.getX(), p.getY() + 1));
                        if (checkValid(ai, north))
                            temp2.add(north);
                    }
                    if (!temp2.isEmpty())
                        temp.add(temp2);
                }

                if (!temp.isEmpty())
                    pots.put(id, temp);
            }


            Collection<Shot> myShots = new ArrayList<>();

            //add possible shots myShots
            for (Entry<Integer, List<Set<Shot>>> entry : pots.entrySet()) {
                for (Set<Shot> l : entry.getValue()) {
                    for (Shot s : l) {
                        myShots.add(s);
                        LOGGER.debug("Added shot {} to myShots", s);
                        ai.requestedShots.add(s);
                        if (myShots.size() == ai.getConfiguration().getShotCount())
                            return myShots;
                    }
                }
            }

            //if there are not enough shots in pots, shoot random
            while (myShots.size() < ai.getConfiguration().getShotCount()) {
                //get random shots: shuffle client list first and take first element
                Collections.shuffle(ai.getClientArrayList());

                int targetClientId = ai.getClientArrayList().get(0).getId();

                if (targetClientId != ai.getAiClientId()) {
                    Shot targetShot = new Shot(targetClientId, new RandomPointCreator(ai.getConfiguration()).getRandomPoint2D());

                    boolean valid = true;

                    //check if random shot is valid
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
                    LOGGER.debug("Added random shot {}", targetShot);
                }
            }
            return myShots;
        }
    },

    /** Places {@link Shot}s using a heat map for each opponent. */
    HEAT_MAP(3) {
        @Override
        public Collection<Shot> calculateShots(final AI ai, final int shotCount) {
            for (Client c : ai.getClientArrayList()) {
                InvalidPointsHandler invalidPointsHandler = new InvalidPointsHandler(ai);
                List<Point2D> inv = invalidPointsHandler.createInvalidPointsOne(c.getId());
                ai.addPointsToInvalid(inv, c.getId());
            }

            HeatMapCreator heatmapCreator = new HeatMapCreator(ai);
            ai.setHeatMapAllClients(heatmapCreator.createHeatMapAllClients());
            //valid targets are the clients which are connected and can be targets for firing shots
            Map<Integer, List<Integer>> validTargets = new HashMap<>();
            //search for valid targets: all clients which are not this ai and have ships and have less invalid points than the field has points
            for (Entry<Integer, List<Integer>> entry : ai.getAllSunkenShipIds().entrySet()) {
                if (!(ai.getInvalidPointsAll().get(entry.getKey()).size() == (ai.getConfiguration().getWidth() * ai.getConfiguration().getHeight())
                        | entry.getValue().size() == ai.getConfiguration().getShips().size() | entry.getKey() == ai.getAiClientId())) {
                    validTargets.put(entry.getKey(), entry.getValue());
                }
            }


            //allHeatVal is the collection of all valid points of all clients and those heat value
            List<Triple<Integer, Point2D, Double>> allHeatVal = Lists.newLinkedList();

            //using the class Triple store the triple in allHeatVal if the target is valid
            //Triple objects can be compared using the comparator interface
            //for use of class Triple see the nested for loops
            for (Entry<Integer, Double[][]> entry : ai.getHeatMapAllClients().entrySet()) {
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

                boolean valid = true;

                int clientId = t.getVal1();
                Point2D p = t.getVal2(); //heat point
                double fieldVal = t.getVal3(); //heat value

                for (Point2D g : ai.getInvalidPointsAll().get(clientId)) {
                    if (g.getX() == p.getX() & g.getY() == p.getY()) {
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
                LOGGER.info(Markers.AI_SHOT_PLACER, "Added shot {} with value {}", targetShot, fieldVal);
                if (myShotsThisRound.size() >= ai.getConfiguration().getShotCount()) {
                    break;
                }
            }
            ai.requestedShots.addAll(myShotsThisRound);

            return myShotsThisRound;
        }
    };

    private final int difficultyLevel;

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final Random RAND = new Random();

    StandardShotPlacementStrategy(final int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    @Nonnull
    static Optional<ShotPlacementStrategy> fromDifficultyLevel(final int difficultyLevel) {
        return Stream
            .of(values())
            .filter(strategy -> strategy.getDifficultyLevel() == difficultyLevel)
            .map(ShotPlacementStrategy.class::cast)
            .findAny();
    }

    @Override
    public String getName() {
        return name();
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }
}
