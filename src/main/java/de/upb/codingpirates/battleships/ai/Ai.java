package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleship.ai.helper.RotationMatrix;
import de.upb.codingpirates.battleship.ai.logger.MARKER;
import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.client.network.ClientModule;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.message.notification.GameInitNotification;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
//TODO some getter/setter are missing

/**
 * Implements the logic of the Ai Player like placing ships and firing shots
 *
 * @author Benjamin Kasten
 */
public class Ai {
    //Logger
    private static final Logger logger = LogManager.getLogger(Ai.class.getName());

    //GameState gameState;
    String host; //ip address
    int port;

    //convert the Collection clintList in a LinkedList for handling random shots is done in the
    Collection<Client> clientList;
    LinkedList<Client> clientArrayList = new LinkedList<>();
    Collection<Shot> hits = new ArrayList<>();

    //PlayerUpdateNotificationInformation
    //PlayerUpdateNotification updateNotification;


    //game field parameter
    int width;
    int height;

    int HITPOINTS;
    int SUNKPOINTS;

    int aiClientId;
    int gameId;
    Configuration config;
    Map<Integer, ShipType> shipConfig = new HashMap<>();

    long VISUALIZATIONTIME;
    long ROUNDTIME;

    int SHOTCOUNT;

    //updated values
    protected Map<Integer, Integer> points = new HashMap<>();
    protected Collection<Shot> sunk = new ArrayList<>();


    //sunken Ships
    Map<Integer, LinkedList<Shot>> sortedSunk = new HashMap<>(); //
    Map<Integer, LinkedList<Integer>> allSunkenShipIds = new HashMap<>();


    //for testing purpose public //todo getter setter
    public PlayerUpdateNotification updateNotification;

    public Collection<Shot> misses = new ArrayList<>();
    public Collection<Shot> requestedShotsLastRound = new ArrayList<>();

    private final ClientConnector tcpConnector = ClientApplication.create(new ClientModule<>(ClientConnector.class));
    public Ai instance = this;

    public ClientConnector getTcpConnector() {
        return tcpConnector;
    }


    /**
     * Is called by the {@link AiMain#main(String[])} for connecting to the server
     *
     * @param host IP Address
     * @param port Port number
     * @throws IOException if something with the connection failed
     */
    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        tcpConnector.connect(host, port);
    }

    /**
     * Is called by the {@link AiMessageHandler#onGameInitNotification(GameInitNotification, int)}
     * for setting giving the ai access to the game configuration.
     *
     * @param config configuration from {@link GameInitNotification}
     */
    public void setConfig(Configuration config) {
        setHeight(config.getHeight());
        setWidth(config.getWidth());
        setShipConfig(config.getShips());
        setHitpoints(config.getHitPoints());
        setSunkpoints(config.getSunkPoints());
        setRoundTimer(config.getRoundTime());
        setVisualizationTime(config.getVisualizationTime());
        setShotCount(config.getShotCount());

    }

    public void placeShots(int difficultyLevel) throws IOException {
        switch (difficultyLevel) {
            case 1: {
                logger.info("Difficulty Level 1 selected");
                placeShots_1();
                break;
            }
            case 2: {
                logger.info("Difficulty Level 2 selected");
                placeShots_2();
                break;
            }
            case 3: {
                logger.info("Difficulty level 3 selected");
                placeShots_3();
                break;
            }
            default: {
                logger.error("Input is not valid: " + difficultyLevel);
            }

        }
    }

    //falsch solange keine passende Platzierung der Schiffe gefunden wurde
    boolean successful = false;
    //the requested argument for the PlaceShipsRequest
    Map<Integer, PlacementInfo> positions = new HashMap<>();

    /**
     * Calls the {link {@link #randomShipGuesser(Map)}} and if it returns true send the {@link PlaceShipsRequest}
     *
     * @throws IOException Network error
     */
    public void placeShips() throws IOException {
        while (!successful) {
            logger.info("placing ships failed");
            randomShipGuesser(getShipConfig());
        }
        logger.info("placing ships worked");

        PlaceShipsRequest placeShipsRequestMessage = new PlaceShipsRequest(getPositions());
        //todo made PlaceShipsRequest public

        tcpConnector.sendMessageToServer(placeShipsRequestMessage);
    }


    /**
     * contains all the Points which can not be accessed anymore like the distance to a ship or the ship positions;
     * is used for checking if a point can be used for placing a ship
     */
    ArrayList<Point2D> usedPoints = new ArrayList<>();

    /**
     * Is called by placeShips() and places the ships randomly on the field. Leave the loop if the placement is not valid.
     *
     * @param shipConfig map, which maps from the shipId to the ShipType (from the configuration)
     */
    private void randomShipGuesser(Map<Integer, ShipType> shipConfig) {
        //clear all used values from the recent call for safety
        usedPoints.clear();
        positions.clear();

        logger.info("started random guesser");

        //for testing purpose
        logger.info("height: {}", getHeight());
        logger.info("width: {}", getWidth());

        //iterate through the the shipConfig Map for getting every key value pair
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            //a logger
            logger.info("entry of shipConfig Map with shipID: " + entry.getKey());


            //ship Id
            int shipId = entry.getKey();
            //all points of the ship
            Collection<Point2D> shipPos = entry.getValue().getPositions();

            //for testing purpose
            for (Point2D j : shipPos) {
                logger.info("old point: {}", j);
            }


            //is the random point wich will be the point for the PlacementInfo
            Point2D guessPoint = getRandomPoint2D();

            //for testing purpose
            logger.info("Guess point: {}", guessPoint);

            //the the distance from zeropoint to random guessPoint
            int distanceX = guessPoint.getX();
            int distanceY = guessPoint.getY();


            //the positions (points) of a ship well be stored here
            ArrayList<Point2D> tempShipPos = new ArrayList<>();

            //iterates through every point of the ship (all points in shipPos)
            for (Point2D i : shipPos) {
                //creating new coordinates by moving every point in x and y direction: The moving distance came from
                //the guessPoint
                int newX = i.getX() + distanceX;
                int newY = i.getY() + distanceY;
                //create a new point for the new coordinates
                Point2D newPoint = new Point2D(newX, newY);

                //for testing purpose
                logger.info("new point: {}", newPoint);

                //check for each point in usePoints if the newPoint is already unavailable (is used)
                for (Point2D p : usedPoints) {
                    //if the newPoint is unavailable: delete usedPoints, positions and return
                    //-->starting the loop in placeShips again
                    if ((p.getX() == newPoint.getX()) & (p.getY() == newPoint.getY())) {
                        usedPoints.clear();
                        positions.clear();
                        logger.info("failed: new guess point already in usedPoints ");
                        return;

                    }
                }

                //if the newPoint is not unavailable, check if the coordinates fits to the field:
                // No negative values, no greater values as the fields height and width
                if (newPoint.getY() < 0 | newPoint.getX() < 0 |
                        newPoint.getX() > (width - 1) | newPoint.getY() > (height - 1)) {
                    //if the newPoint is unavailable: delete usedPoints, positions and return
                    //-->starting the loop in placeShips again
                    usedPoints.clear();
                    positions.clear();
                    logger.info("failed: new point coordinates do not fit the field ");
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

            //after placing a ship, we have to add all surrounding points of the ship to the usedPoints Array
            //once they are in the usedPoints Array, they can not be used for placing ships anymore
            //addSurroundingPointsToUsedPoints(tempShipPos);
            usedPoints.addAll(addSurroundingPointsToUsedPoints(tempShipPos));


            //clear the tempShipPos Array for the next loop
            tempShipPos.clear();
        }
        //is called only if the placing of the ships in the positions Map worked for all ships
        //responsible for leaving the while loop in placeShips()
        setSuccessfulPlacement();
    }

    /**
     * Adds the surrounding points of one points collection to the usedPoints based on the rules for the game:
     * each ship must have a minimal distance of one point in each direction to other ships.
     * Used by {@link #randomShipGuesser(Map)}
     *
     * @param shipPos The positions of one ship
     */
    private LinkedHashSet<Point2D> addSurroundingPointsToUsedPoints(ArrayList<Point2D> shipPos) {
        LinkedHashSet<Point2D> temp = new LinkedHashSet<>();
        for (Point2D point : shipPos) {
            int x = point.getX();
            int y = point.getY();

            //add all left neighbours
            temp.add(new Point2D(x - 1, y + 1));
            temp.add(new Point2D(x - 1, y));
            temp.add(new Point2D(x - 1, y - 1));

            //add all right neighbours
            temp.add(new Point2D(x + 1, y + 1));
            temp.add(new Point2D(x + 1, y));
            temp.add(new Point2D(x + 1, y - 1));

            //add the direct neighbours under and over
            temp.add(new Point2D(x, y + 1));
            temp.add(new Point2D(x, y - 1));
        }
        temp.removeIf(p -> p.getX() < 0 | p.getY() < 0);
        return temp;


    }

    /**
     * Only for converting the Client Collection of the {@link GameInitNotification} into a LinkedList to have more
     * functions.
     * Called by {@link AiMessageHandler#onGameInitNotification(GameInitNotification, int)}
     *
     * @param clientList from the configuration
     */
    public void setClientArrayList(Collection<Client> clientList) {
        clientArrayList.addAll(clientList);

    }

    public LinkedList<Client> getClientArrayList() {
        return this.clientArrayList;
    }

    /**
     * Creates a new ArrayList(Shot) with only one element
     *
     * @param i The Shot object which will be the only object in the list
     * @return The "one element" list
     */
    public LinkedList<Shot> createArrayListOneArgument(Shot i) {
        LinkedList<Shot> list = new LinkedList<>();
        list.add(i);
        return list;
    }


    /**
     * Creates a map which maps from the clientID on their sunken ships
     *
     * @return The map with ordered shots (sunk)
     */
    public HashMap<Integer, LinkedList<Shot>> sortTheSunk() {
        logger.info("Sorting the sunken ships by their clients...");
        HashMap<Integer, LinkedList<Shot>> sortedSunk = new HashMap<>();
        for (Shot i : sunk) {
            int clientId = i.getClientId();
            boolean success = false;
            for (Map.Entry<Integer, LinkedList<Shot>> entry : sortedSunk.entrySet()) {
                if (entry.getKey() == clientId) {
                    entry.getValue().add(i);
                    success = true;
                }
            }
            if (!success) {
                sortedSunk.put(clientId, createArrayListOneArgument(i));
            }
        }
        for (Client c : this.clientArrayList) {
            if (!sortedSunk.containsKey(c.getId())) {
                LinkedList<Shot> emptyList = new LinkedList<>();
                sortedSunk.put(c.getId(), emptyList);
            }
        }
        logger.info("Sorted the sunken ships by their clients.");
        return sortedSunk;
    }


    /**
     * Creates a collection of collections of all possible ship rotations
     *
     * @param ships Collection of points which represents a ship
     * @return allPossibleTurns ArrayList of arraylists for each possible rotation
     */
    private ArrayList<ArrayList<Point2D>> rotateShips(ArrayList<Point2D> ships) {
        RotationMatrix rotate = new RotationMatrix();
        ArrayList<ArrayList<Point2D>> allPossibleTurns = new ArrayList<>();
        //no turn
        allPossibleTurns.add(rotate.moveToZeroPoint(ships));
        //90 degree
        allPossibleTurns.add(rotate.turn90(ships));
        //180 degree
        ArrayList<Point2D> temp180;
        temp180 = rotate.turn90(ships);
        temp180 = rotate.turn90(temp180);
        allPossibleTurns.add(temp180);

        //270 degree
        ArrayList<Point2D> temp270;
        temp270 = rotate.turn90(ships);
        temp270 = rotate.turn90(temp270);
        temp270 = rotate.turn90(temp270);
        allPossibleTurns.add(temp270);
        return allPossibleTurns;


    }

    /**
     * Computes all misses this Ai Player and adds them to all misses this round
     */
    public void calcAndAddMisses() {
        logger.info("Compute the misses of this round");
        Collection<Shot> tempMisses = new ArrayList<>();
        for (Shot s : requestedShotsLastRound) {
            boolean miss = true; //assume the shot s is a miss
            for (Shot i : getHits()) { //check if shot s is a miss
                if (i.getTargetField().getX() == s.getTargetField().getX() & i.getTargetField().getY() == s.getTargetField().getY() & s.getClientId() == i.getClientId()) {
                    miss = false; //no miss, its a hit
                    logger.info("A hit {}", s);
                }
            }
            if (miss) {
                tempMisses.add(s); // if its not hit, its a miss
                logger.info("A miss {}", s);
            }
        }
        this.misses.addAll(tempMisses); //add all new misses to all misses of the game
        logger.info("Found {} misses last round.", tempMisses.size());
        tempMisses.clear(); //not necessary


    }

    //A map which maps the client id on a collection with all invalid Points of this client
    Map<Integer, LinkedHashSet<Point2D>> invalidPointsAll = new HashMap<>();

    /**
     * Computes the invalid Points of one client and replaces the current entry of the last round in invalidPointsAll
     * by the new updated collection of invalid points.
     *
     * @param clientId The clientId for computing the invalid points
     */

    public void createAnsSetInvalidPointsOne(int clientId) {
        logger.info(MARKER.invalid_points, "Computing the invalid points of client : " + clientId);
        invalidPointsAll.putIfAbsent(clientId, null);
        LinkedList<Shot> sortedSunkShotsTC = getSortedSunk().get(clientId);
        ArrayList<Point2D> sortedSunkPointsTC = new ArrayList<>();
        for (Shot s : sortedSunkShotsTC) {
            sortedSunkPointsTC.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));
        }
        LinkedHashSet<Point2D> temp = new LinkedHashSet<>(addSurroundingPointsToUsedPoints(sortedSunkPointsTC));
        for (Shot s : this.misses) {
            if (s.getClientId() == clientId) {
                temp.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));
            }
        }
        for (Shot s : getHits()) {
            if (s.getClientId() == clientId) {
                temp.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));
            }
        }
        invalidPointsAll.replace(clientId, temp);
    }


    /**
     * Places shots randomly on the field of one opponent and sends the {@link ShotsRequest} message.
     * <p>
     * Difficulty level 1.
     *
     * @throws IOException Network error (see report)
     */
    public void placeShots_1() throws IOException {

        //update numberOfClients every time the method is called because the number of connected clients could have changed
        int numberOfClients = clientArrayList.size();
        int shotClientId;

        int aiIndex = -1;
        for (Client c : clientArrayList) {
            if (c.getId() == aiClientId) {
                aiIndex = clientArrayList.indexOf(c);
            }
        }

        while (true) {

            int randomIndex = (int) (Math.random() * numberOfClients);
            if (randomIndex != aiIndex) {
                shotClientId = clientArrayList.get(randomIndex).getId(); //shotClientId is the target for placing shots in the next part
                logger.info("Shooting on client with id: {} ", shotClientId);
                break;
            }
        }

        Collection<Shot> choosenShots = new ArrayList<>();

        ArrayList<Point2D> aimsThisRound = new ArrayList<>();

        //placing the shots randomly until the max of shots is not reached
        //all shots will be placed on the field of only one opponents field(other client)
        int i = 0;
        while (i < getShotCount()) {
            logger.info("Trying to find  {}. shot", i + 1);

            Point2D aimPoint = getRandomPoint2D(); //aim is one of the random points as a candidate for a shot
            boolean alreadyChoosen = false;
            for (Point2D p : aimsThisRound) {
                if (p.getX() == aimPoint.getX() & p.getY() == aimPoint.getY()) {
                    alreadyChoosen = true;
                    logger.info("Shot was already selected this round" + p);
                }
            }
            for (Shot h : getHits()) {
                if (h.getTargetField().getX() == aimPoint.getX() & h.getTargetField().getY() == aimPoint.getY() & h.getClientId() == shotClientId) {
                    alreadyChoosen = true;
                    logger.info("Shot is already a hit " + h);
                }
            }
            for (Shot s : getMisses()) {
                if (s.getClientId() == shotClientId & s.getTargetField().getX() == aimPoint.getX() & s.getTargetField().getY() == aimPoint.getY()) {
                    alreadyChoosen = true;
                    logger.info("Shot is already a miss " + s);
                }
            }
            if (alreadyChoosen) continue;

            aimsThisRound.add(aimPoint);
            //create a new shot object, add it to requestedShot Array and increase i
            Shot shot = new Shot(shotClientId, aimPoint);
            choosenShots.add(shot);
            i++;
            logger.info("Found shot {}", shot);

        }

        //send the shotsRequest object to the server
        tcpConnector.sendMessageToServer(new ShotsRequest(choosenShots));
    }

    /**
     * Places shots using the hunt and target algorithm.
     * Difficulty level 2.
     *
     * @throws IOException Network error (see report)
     */
    public void placeShots_2() throws IOException {
        logger.info(MARKER.shot_placement, "Placing shots with difficulty level 2");
        Collection<Shot> myShots = new ArrayList<>();
        calcAndAddMisses();
        int shotClientId = 222;

        int aiIndex;
        for (Client c : clientArrayList) {
            if (c.getId() == aiClientId) {
                aiIndex = clientArrayList.indexOf(c);
                logger.info("Ai Index: {}.", aiIndex);
            }
        }

        Set<Point2D> pot = new HashSet<>();
        for (Shot s : getHits()) {
            if (s.getClientId() == shotClientId) {
                logger.info("Looking for all neighbours of Shot {}", s);
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
                        for (Shot h : getHits()) {
                            if (h.getTargetField().getX() == p.getX() & h.getTargetField().getY() == p.getY() & h.getClientId() == shotClientId) {
                                isHitOrMiss = true;
                                break;
                            }

                        }
                        if (isHitOrMiss) continue;
                        for (Shot h : getMisses()) {
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
                    logger.info("Added {} to potential hits", p);
                }
                if (pot.size() < getShotCount()) {
                    logger.info("There are less potential hits ({}) as possible shots ({})", pot.size(), getShotCount());
                    for (Point2D p : pot) {
                        myShots.add(new Shot(shotClientId, new Point2D(p.getX(), p.getY())));
                        logger.info("Added {} to myShots", p);
                    }
                    while (true) {
                        boolean invalid = false;
                        Point2D point = getRandomPoint2D();

                        for (Shot h : getHits()) {
                            if (h.getTargetField().getX() == point.getX() & h.getTargetField().getY() == point.getY() & h.getClientId() == shotClientId) {
                                invalid = true;
                                break;
                            }

                        }
                        if (invalid) continue;
                        for (Shot h : getMisses()) {
                            if (h.getTargetField().getX() == point.getX() & h.getTargetField().getY() == point.getY() & h.getClientId() == shotClientId) {
                                invalid = true;
                                break;
                            }

                        }
                        if (invalid) continue;
                        else {
                            myShots.add(new Shot(shotClientId, point));
                            logger.info("Added random shot {} to myShots", point);
                        }
                        if (myShots.size() == getShotCount()) break;
                    }
                } else {
                    logger.info("There are more potential hits ({}) as possible shots ({})", pot.size(), getShotCount());
                    for (Point2D p : pot) {
                        myShots.add(new Shot(shotClientId, p));
                        logger.info("Added {} to myShots", p);
                        if (myShots.size() == getShotCount()) break;
                    }
                }
            }
            if (myShots.size() == getShotCount()) break;
        }
        pot.clear();
        requestedShotsLastRound.clear();
        requestedShotsLastRound.addAll(myShots);
        tcpConnector.sendMessageToServer(new ShotsRequest(myShots));


    }

    /**
     * The approach in this version is to shoot a the field with the maximum of invalid points.
     * Sends the {@link ShotsRequest} message tho the server.
     * <p>
     * Difficulty level 3.
     *
     *
     * <p>
     * The approach explained more detailed:
     */
    public void placeShots_3() throws IOException {
        logger.info(MARKER.shot_placement, "Placing shots with difficulty level 3");
        createHeatmapAllClients();


        Map<Integer, LinkedList<Integer>> validTargets = new HashMap<>();

        for (Map.Entry<Integer, LinkedList<Integer>> entry : getAllSunkenShipIds().entrySet()) {
            if (!(invalidPointsAll.get(entry.getKey()).size() == (getWidth() * getHeight())
                    | entry.getValue().size() == shipConfig.size() | entry.getKey() == getAiClientId())) {
                logger.info("A valid target is client: {}", entry.getKey());
                validTargets.put(entry.getKey(), entry.getValue());
            }
        }
        Map<Integer, Integer> invalidPointsSize = new HashMap<>();

        for (Map.Entry<Integer, LinkedHashSet<Point2D>> entry : invalidPointsAll.entrySet()) {
            logger.info("Client {} has {} invalid fields", entry.getKey(), entry.getValue().size());
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
            for (Map.Entry<Integer, Integer> entry : invalidPointsSizeOrdered.entrySet()) {
                int countShotsOne = 0;
                int targetClient = entry.getKey();
                logger.info("Shooting now on client {} with {} invalid fields", targetClient, entry.getValue());

                //get the heatmap of this client
                Integer[][] targetHeatmap = heatmapAllClients.get(targetClient);

                Map<Point2D, Integer> pointsToValue = new LinkedHashMap<>();

                for (int row = 0; row < targetHeatmap.length; row++) {
                    for (int col = 0; col < targetHeatmap[row].length; col++) {
                        pointsToValue.put(new Point2D(col, row), targetHeatmap[row][col]);
                    }
                }
                LinkedHashMap<Point2D, Integer> pointsToValueOrdered = pointsToValue.entrySet()
                        .stream()
                        .sorted((Map.Entry.<Point2D, Integer>comparingByValue().reversed()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                Map<Point2D, Integer> temp = new LinkedHashMap<>(pointsToValueOrdered);
                for (Map.Entry<Point2D, Integer> entryy : temp.entrySet()) {
                    if (entryy.getValue() <= 0) {
                        pointsToValueOrdered.remove(entryy.getKey());
                    }
                }


                for (Map.Entry<Point2D, Integer> entryy : pointsToValueOrdered.entrySet()) {

                    Shot targetShot = (new Shot(targetClient, new Point2D(entryy.getKey().getX(), entryy.getKey().getY())));
                    boolean isInvalid = false;
                    for (Point2D p : invalidPointsAll.get(entry.getKey())) {
                        if (entryy.getKey().getX() == p.getX() & entryy.getKey().getY() == p.getY()) {
                            isInvalid = true;
                            break;

                        }
                    }
                    if (isInvalid) continue;
                    myShotsThisRound.add(targetShot);
                    logger.info("Added shot {} with value {} to myShots", targetShot, entryy.getValue());
                    countShotsOne++;
                    if (myShotsThisRound.size() == getShotCount()) {
                        logger.info("{} shots on client {}", countShotsOne, entry.getKey());
                        tcpConnector.sendMessageToServer(new ShotsRequest(myShotsThisRound));
                        requestedShotsLastRound.clear(); //leere die Liste von dieser Runde
                        requestedShotsLastRound.addAll(myShotsThisRound);
                        return;

                    }
                }
                counter++;
                logger.info("{} shots on client {}", countShotsOne, entry.getKey());

            }
        }
    }

    /**
     * Creates a heatmap for each client and calls {@link Ai#createHeatmapOneClient(int clientId)}.
     * In this version, the heatmaps will be created completely new by clearing the old heatmaps first.
     *
     * @see <a href="http://www.datagenetics.com/blog/december32011/">http://www.datagenetics.com/blog/december32011/</a>
     */
    Map<Integer, Integer[][]> heatmapAllClients = new HashMap<>();

    public void createHeatmapAllClients() {
        heatmapAllClients.clear(); //delete the heatmaps of the last round
        calcAndAddMisses(); // compute the new misses for this round
        findAllSunkenShipIds(); //compute the sunken ship Ids for every client
        for (Client client : this.getClientArrayList()) {
            if (client.getId() == getAiClientId()) {
                createAnsSetInvalidPointsOne(client.getId());
                logger.info("Skipped creating heatmap for own field");
                continue;
            }
            //create a heatmap for this client and put it into the heatmapAllClients map
            heatmapAllClients.put(client.getId(), createHeatmapOneClient(client.getId()));
        }


    }

    /**
     * Creates a Heatmap for one Client: assigns each Point its maximum occupancy by (not yet sunken) ships
     * <p>
     * <p>
     * The algorithm is based on <a href="http://www.datagenetics.com/blog/december32011/">http://www.datagenetics.com/blog/december32011/</a>
     *
     * @param clientId The clientId for whom the heatmap is to be created
     * @return a heatmap for the client
     */
    public Integer[][] createHeatmapOneClient(int clientId) {
        logger.info(MARKER.heatmap, "Create heatmap for client " + clientId);
        createAnsSetInvalidPointsOne(clientId);

        Integer[][] heatmap = new Integer[getHeight()][getWidth()]; //heatmap array
        for (Integer[] integers : heatmap) {
            Arrays.fill(integers, 0);
        }

        LinkedHashSet<Point2D> invalidPointsThisClient = invalidPointsAll.get(clientId);
        LinkedList<Integer> sunkenIdsThisClient = getAllSunkenShipIds().get(clientId); // get the sunken ship Ids of this client


        Map<Integer, ShipType> shipConfig = getShipConfig();
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            logger.info("Ship Id of shipConfig: " + entry.getKey());
            if (sunkenIdsThisClient.contains(entry.getKey())) {
                logger.info(MARKER.check, "ship already sunk: " + entry.getKey());
                continue; //Wenn das Schiff versenkt ist betrachte nächstes Schiff
            }
            int shipId = entry.getKey(); //Schiffs Id
            //Koordinaten des aktuellen Schiffs
            ArrayList<Point2D> positions = (ArrayList<Point2D>) entry.getValue().getPositions();
            //Rotiere das aktuelle Schiff
            ArrayList<ArrayList<Point2D>> rotated = rotateShips(positions);
            //Betrachte erstes rotiertes Schiff
            for (ArrayList<Point2D> tShips : rotated) {
                ArrayList<Point2D> cShip = new ArrayList<>(tShips); //kopiere erstes rotiertes Schiff
                ArrayList<Integer> xValues = new ArrayList<>(); //alle X werte des Schiffs
                ArrayList<Integer> yValues = new ArrayList<>(); //alle y Werte des Schiffs
                for (Point2D z : cShip) { //füge x und y den Listen hinzu
                    xValues.add(z.getX());
                    yValues.add(z.getY());
                }
                Collections.sort(xValues);//sortiere die x, y Listen
                Collections.sort(yValues);
                int maxX = xValues.get(xValues.size() - 1); //hole größtes x, y der Schiffe
                int maxY = yValues.get(yValues.size() - 1);
                int initMaxX = xValues.get(xValues.size() - 1);//lege initiales x, y fest
                int initMaxY = yValues.get(yValues.size() - 1);

                while (maxY < getHeight()) {

                    while (maxX < getWidth()) {
                        boolean valid = true;
                        //check if cShip fits on the field
                        for (Point2D p : cShip) { //jeder Punkt in cShip
                            for (Point2D s : invalidPointsThisClient) { //jeder invalid Point für diesen Client
                                if (p.getX() == s.getX() & p.getY() == s.getY()) {
                                    //wenn ein Punkt dem Shot Punkt gleich ist, mache nichts und schiebe Schiff
                                    //einen weiter
                                    valid = false;
                                    break;
                                }
                            }
                            if (!valid) break;

                        }
                        if (valid) {
                            //increment the array positions +1
                            for (Point2D i : cShip) {
                                int x = i.getX();
                                int y = i.getY();
                                heatmap[y][x]++;
                            }
                        }

                        ArrayList<Point2D> newPos = new ArrayList<>();
                        for (Point2D u : cShip) {
                            newPos.add(new Point2D(u.getX() + 1, u.getY()));
                        }
                        cShip = new ArrayList<>(newPos);

                        newPos.clear();
                        maxX++;

                    }

                    maxX = initMaxX;
                    ArrayList<Point2D> newPos = new ArrayList<>();
                    for (Point2D u : cShip) {
                        newPos.add(new Point2D(u.getX() - (this.width - initMaxX), u.getY() + 1));
                    }
                    cShip = new ArrayList<>(newPos);
                    newPos.clear();
                    maxY++;

                }

            }
            logger.info("Finished field with rotated versions of ship " + shipId);


        }

        logger.info(MARKER.heatmap, "Created heatmap of client: " + clientId);

        return heatmap;


    }

    /**
     * Can be called for getting the id of sunken ships for each client.
     * Sets the sunkenShipIdsAll instead of returning the map
     */
    public void findAllSunkenShipIds() {
        logger.info("Try finding sunken ShipIds");
        Map<Integer, LinkedList<Shot>> sortedSunk = getSortedSunk();
        Map<Integer, LinkedList<Integer>> allSunkenShipIds = new HashMap<>(); //maps from client id on the sunken ship ids
        for (Map.Entry<Integer, LinkedList<Shot>> entry : sortedSunk.entrySet()) {
            int clientId = entry.getKey();
            LinkedList<Integer> a = findSunkenShips(entry.getValue());
            allSunkenShipIds.put(clientId, a);
            if (a.isEmpty()) {
                logger.info("Found no sunken ships of Client " + clientId);
            } else {
                logger.info("Found sunken ships of Client " + clientId);
                for (int i : a) {
                    logger.info("ShipId: " + i);
                }
            }
        }
        setAllSunkenShipIds(allSunkenShipIds);
    }

    public void setAllSunkenShipIds(Map<Integer, LinkedList<Integer>> sunkenIds) {
        this.allSunkenShipIds = sunkenIds;
    }

    public Map<Integer, LinkedList<Integer>> getAllSunkenShipIds() {
        return this.allSunkenShipIds;
    }

    /**
     * Finds the sunken ship ids of one sunk collection
     * Called by {@link #findAllSunkenShipIds()}  for each client who has sunken ships
     *
     * @param shotsThisClient All shots on one client
     * @return Ids of the sunken ships
     */
    public LinkedList<Integer> findSunkenShips(LinkedList<Shot> shotsThisClient) {
        LinkedList<Point2D> sunk = new LinkedList<>(); // enthält alle shots als punkte
        LinkedList<LinkedList<Point2D>> all = new LinkedList<>(); //die initiale liste die wieder aktualisiert wird
        LinkedList<LinkedList<Point2D>> p; //die temporäre linkedlist zum bearbeiten

        for (Shot i : shotsThisClient) { //shots liste in punkte liste umwandeln
            sunk.add(new Point2D(i.getTargetField().getX(), i.getTargetField().getY()));
        }

        //Algorithmus zum Finden von zusammenhängenden Punkten (Schiffe finden)
        //1. Bilden einer initialen Verteilung der Schiffe
        for (Point2D z : sunk) {
            boolean proofed = false;

            for (LinkedList<Point2D> h : all) {
                for (Point2D j : h) {
                    if (z.getX() == j.getX() & z.getY() == j.getY()) {
                        proofed = true;
                        break;
                    }
                }
                if (proofed) break;
            }
            if (proofed) continue;
            LinkedList<Point2D> temp = new LinkedList<>();
            temp.add(z);
            for (Point2D t : sunk) {
                boolean used = false;
                for (Point2D x : sunk) {
                    for (LinkedList<Point2D> h : all) {
                        for (Point2D j : h) {
                            if (z.getX() == j.getX() & z.getY() == j.getY()) {
                                used = true;
                                break;
                            }
                        }
                        if (used) break;
                    }
                    if (used) break;
                }
                if (used) continue;

                if (z == t) continue;
                boolean checked = false;
                if ((z.getX() + 1 == t.getX() & z.getY() == t.getY())
                        | (z.getX() - 1 == t.getX() & z.getY() == t.getY())
                        | (z.getX() == t.getX() & z.getY() + 1 == t.getY())
                        | (z.getX() == t.getX() & z.getY() - 1 == t.getY())) {
                    for (LinkedList<Point2D> k : all) {
                        for (Point2D u : k) {
                            if (u.getX() == t.getX() & u.getY() == t.getY()) {
                                checked = true;
                                break;
                            }
                        }
                        if (checked) break;
                    }
                    if (checked) break;
                    temp.add(t);

                }
            }
            all.add(new LinkedList<>(temp));
            temp.clear();
        }
        //2. Ausgehend von der initialen Verteilung der Schiffe werden die anderen zugehörigen
        //   Punkte gesucht und passenden, schon zusammenhängenden Punkteverteilungen hinzugefügt
        //   Aufgrund der Notwendigkeit des Ersetzens des Iterables wird mit einer Kopie gearbeitet.
        //   Dann wird über die Kopie iteriert und die "haupt" Liste wird bearbeitet. Zwischen den Loops
        //   werden die Listen synchronisiert
        p = new LinkedList<>(all);
        boolean success = true;
        boolean findOne = false;
        while (success) {
            for (LinkedList<Point2D> a : all) {
                for (LinkedList<Point2D> b : all) {
                    if (a == b) {
                        continue;
                    }
                    for (Point2D c : a) {
                        for (Point2D d : b) {
                            if ((c.getX() + 1 == d.getX() & c.getY() == d.getY())
                                    | (c.getX() - 1 == d.getX() & c.getY() == d.getY())
                                    | (c.getX() == d.getX() & c.getY() + 1 == d.getY())
                                    | (c.getX() == d.getX() & c.getY() - 1 == d.getY())) {
                                int inB = all.indexOf(b);
                                int inA = all.indexOf(a);
                                LinkedList<Point2D> valueA = p.get(inA);
                                LinkedList<Point2D> valueB = p.get(inB);
                                valueA.addAll(valueB);
                                //p.set(inA, valueA);
                                p.remove(inB);
                                findOne = true;
                                break;
                            }
                            if (findOne) break;
                        }
                        if (findOne) break;
                    }
                    if (findOne) break;
                }
                if (findOne) break;
            }
            all = p;

            if (findOne) {
                findOne = false;

                for (LinkedList<Point2D> a : p) {
                    for (LinkedList<Point2D> b : p) {
                        if (a == b) {
                            continue;
                        }
                        for (Point2D c : a) {
                            for (Point2D d : b) {
                                if ((c.getX() + 1 == d.getX() & c.getY() == d.getY())
                                        | (c.getX() - 1 == d.getX() & c.getY() == d.getY())
                                        | (c.getX() == d.getX() & c.getY() + 1 == d.getY())
                                        | (c.getX() == d.getX() & c.getY() - 1 == d.getY())) {
                                    int inB = p.indexOf(b);
                                    int inA = p.indexOf(a);
                                    LinkedList<Point2D> valueA = all.get(inA);
                                    LinkedList<Point2D> valueB = all.get(inB);
                                    valueA.addAll(valueB);
                                    all.set(inA, valueA);
                                    all.remove(inB);
                                    findOne = true;
                                    break;
                                }
                            }
                            if (findOne) break;
                        }
                        if (findOne) break;
                    }
                    if (findOne) break;
                }

            }
            if (!findOne) success = false;

        }

        //3. Finden der zu den gefundenen Schiffsverteilungen passenden Schiffen aus der shipConfig
        //   Dazu wird jedes Schiff aus der config einmal in jeder Rotation über das Spielfeld geschoben
        //   Sobald ein Schiff aus der config mit einem der versenkten übereinstimmt, wird es in eine Collection
        //   aufgenommen
        LinkedList<Integer> sunkenShipIds = new LinkedList<>();
        Map<Integer, ShipType> shipConfig = this.getShipConfig();

        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            int shipId = entry.getKey();
            ArrayList<ArrayList<Point2D>> t = rotateShips((ArrayList<Point2D>) entry.getValue().getPositions()); //schiff aus der config wird gedreht
            for (LinkedList<Point2D> a : all) { //erster Eintrag in all (erstes gesunkens Schiff)
                boolean find = false;

                for (ArrayList<Point2D> b : t) {//erster Eintrag in t (erstes rotiertes Schiff aus der shipconfig

                    ArrayList<Point2D> bCopy = new ArrayList<>(b);
                    if (a.size() == b.size()) {
                        ArrayList<Integer> xValues = new ArrayList<>();
                        ArrayList<Integer> yValues = new ArrayList<>();
                        for (Point2D z : bCopy) {
                            xValues.add(z.getX());
                            yValues.add(z.getY());
                        }
                        Collections.sort(xValues);
                        Collections.sort(yValues);
                        int maxX = xValues.get(xValues.size() - 1);
                        int maxY = yValues.get(yValues.size() - 1);
                        int initMaxX = xValues.get(xValues.size() - 1);
                        int initMaxY = yValues.get(yValues.size() - 1);
                        while (maxY < this.height) {
                            while (maxX < this.width) {
                                int size = 0;
                                for (Point2D k : a) {
                                    for (Point2D i : bCopy) {
                                        if (k.getX() == i.getX() & k.getY() == i.getY()) {
                                            size++;
                                        } else {
                                            continue;
                                        }
                                        if (size == a.size()) {
                                            sunkenShipIds.add(shipId);
                                            find = true;
                                            break;
                                        }
                                    }
                                    if (find) break;
                                }
                                if (find) break;
                                ArrayList<Point2D> newPos = new ArrayList<>();
                                for (Point2D u : bCopy) {
                                    newPos.add(new Point2D(u.getX() + 1, u.getY()));
                                }
                                bCopy = new ArrayList<>(newPos);
                                newPos.clear();
                                maxX++;
                            }
                            maxX = initMaxX;
                            if (find) break;
                            ArrayList<Point2D> newPos = new ArrayList<>();
                            for (Point2D u : bCopy) {
                                newPos.add(new Point2D(u.getX() - (width - initMaxX), u.getY() + 1));
                            }
                            bCopy = new ArrayList<>(newPos);
                            newPos.clear();
                            maxY++;

                        }
                    } else {
                        break;
                    }
                    if (find) break;
                }
                if (find) break;

            }
        }
        return sunkenShipIds;

    }

    /**
     * Creates a random point related to the width and height of the game field
     *
     * @return Point2d Random Point with X and Y coordinates
     */
    public Point2D getRandomPoint2D() {
        int x = (int) (Math.random() * getWidth());
        int y = (int) (Math.random() * getHeight());
        Point2D randPoint = new Point2D(x, y);
        logger.info("Getting a random Point2d : {}", randPoint);
        return randPoint;
    }

    /**
     * Removes the client who left the game from the clientArrayList
     *
     * @param leftPlayerID ID of the PLayer who left the Game
     */
    public void handleLeaveOfPlayer(int leftPlayerID) {
        clientArrayList.removeIf(i -> i.getId() == leftPlayerID);
    }


    public void setSortedSunk(HashMap<Integer, LinkedList<Shot>> sortedSunk) {
        this.sortedSunk = sortedSunk;
    }


    public Collection<Shot> getMisses() {
        return this.misses;
    }

    public Map<Integer, LinkedList<Shot>> getSortedSunk() {
        return this.sortedSunk;
    }

    public void setAiClientId(int aiClientId) {
        this.aiClientId = aiClientId;
    }

    public int getAiClientId() {
        return this.aiClientId;
    }

    public void setShipConfig(Map<Integer, ShipType> shipConfig) {
        this.shipConfig = shipConfig;
    }

    public Map<Integer, ShipType> getShipConfig() {
        return this.shipConfig;
    }

    public void setSuccessfulPlacement() {
        this.successful = true;
    }


    public void setShotCount(int shotCount) {
        this.SHOTCOUNT = shotCount;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getGameId() {
        return this.gameId;
    }

    public int getShotCount() {
        return this.SHOTCOUNT;
    }

    public void setWidth(int _width) {
        logger.info("Field width is {}", _width);
        this.width = _width;
    }

    public int getWidth() {
        return this.width;
    }

    public void setHeight(int _height) {
        logger.info("Field height is {}", _height);
        this.height = _height;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHitpoints(int hitpoints) {
        this.HITPOINTS = hitpoints;
    }

    public void setHits(Collection<Shot> hits) {
        this.hits = hits;
    }

    public Collection<Shot> getHits() {
        return this.hits;
    }

    public Collection<Shot> getSunk() {
        return this.sunk;
    }

    public void setPoints(Map<Integer, Integer> points) {
        this.points = points;
    }

    public void setSunk(Collection<Shot> sunk) {
        this.sunk = sunk;
    }

    private void setVisualizationTime(long visualizationTime) {
        this.VISUALIZATIONTIME = visualizationTime;
    }

    private void setRoundTimer(long roundTime) {
        this.ROUNDTIME = roundTime;
    }

    private void setSunkpoints(int sunkPoints) {
        this.SUNKPOINTS = sunkPoints;
    }

    public Map<Integer, PlacementInfo> getPositions() {
        return this.positions;
    }

}