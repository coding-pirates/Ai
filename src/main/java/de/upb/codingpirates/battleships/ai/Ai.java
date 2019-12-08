package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.logic.util.*;
import de.upb.codingpirates.battleships.network.message.notification.GameInitNotification;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

//TODO some getter/setter are missing

/**
 * Implements the logic of the Ai Player like placing ships and firing shots
 *
 * @author Benjamin Kasten
 */
public class Ai {
    //Logger
    private static final Logger logger = Logger.getLogger(Ai.class.getName());

    //GameState gameState;
    ClientConnector connector;
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

    int clientId;
    int gameId;
    Configuration config;
    Map<Integer, ShipType> shipConfig = new HashMap<>();

    long VISUALIZATIONTIME;
    long ROUNDTIME;

    int SHOTCOUNT;

    //updated values
    protected Map<Integer, Integer> points = new HashMap<>();
    protected Collection<Shot> choosenShots = new ArrayList<>();
    protected Collection<Shot> sunk = new ArrayList<>();


    //for testing purpose public
    public PlayerUpdateNotification updateNotification;
    public ShotsRequest shotsRequest;


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
        ClientConnector connector = ClientApplication.create(AiModule.class);
        connector.connect(host, port);
        setConnector(connector);
    }

    /**
     * Is called by the {@link AiMessageHandler#handleGameInitNotification(GameInitNotification, int)}
     * for setting giving the ai access to the game configuration.
     *
     * @param config configuration from {@link GameInitNotification}
     */
    public void setConfig(Configuration config) {
        setHeight(config.HEIGHT);
        setWidth(config.WIDTH);
        setShipConfig(config.getShipTypes());
        setHitpoints(config.HITPOINTS);
        setSunkpoints(config.SUNKPOINTS);
        setRoundTimer(config.ROUNDTIME);
        setVisualizationTime(config.VISUALIZATIONTIME);
        setShotCount(config.SHOTCOUNT);

    }


    //remains false until a
    boolean successful = false;
    //gets ShipId an PlacementInfo for PlaceShipRequest
    Map<Integer, PlacementInfo> positions;

    /**
     * Calls the {link {@link #randomShipGuesser(Map)}} and if it returns true send the {@link PlaceShipsRequest}
     *
     * @throws IOException
     */
    public void placeShips() throws IOException {
        //TODO Funktionalität prüfen und testen, vor allem auf richtigen Ablauf der Schleifen achten
        while (!successful) {
            randomShipGuesser(this.shipConfig);
        }
        PlaceShipsRequest placeShipsRequestMessage = new PlaceShipsRequest(positions);
        connector.sendMessageToServer(placeShipsRequestMessage);
    }


    ArrayList<Point2D> usedFields = new ArrayList<>();

    /**
     * Is calles by placeShips() and places the ships randomly on the field. Leave the loop if the placement is not valid.
     *
     * @param shipConfig the map of Integer (ShipId) ShipType (Collection<{@link Point2D}>) from the configuration
     */
    private void randomShipGuesser(Map<Integer, ShipType> shipConfig) {

        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {

            int shipId = entry.getKey();

            Collection<Point2D> shipPos = entry.getValue().getPosition();


            //random point for placing the ship
            Point2D guessPoint = getRandomPoint2D();

            int distanceX = guessPoint.getX();
            int distanceY = guessPoint.getY();

            Collection<Point2D> randomShipPos = new ArrayList<>();

            for (Point2D i : shipPos) {
                int newX = i.getX() + distanceX;
                int newY = i.getY() + distanceY;
                Point2D newPoint = new Point2D(newX, newY);
                //checks if... 1. newPoint is already unavailable for placing a ship,
                // 2. y coordinate is smaller than 0
                // 3. x coordinate is smaller than 0
                // try again to find fitting points if one of the statements is true

                if (usedFields.contains(newPoint) | newPoint.getY() < 0 | newPoint.getX() < 0) {
                    usedFields.clear();
                    positions.clear();
                    return;
                } else { // if the point is valid add the point to the randomShipPos ArrayList and put it to positions map
                    randomShipPos.add(newPoint);
                    PlacementInfo pInfo = new PlacementInfo(guessPoint, Rotation.NONE);
                    positions.put(shipId, pInfo);
                }
            }
        }
        successful = true;
        return;

    }

    /**
     * Only for converting the Client Collection of the {@link GameInitNotification} into a LinkedList to have more
     * functions.
     * Called by {@link AiMessageHandler#handleGameInitNotification(GameInitNotification, int)}
     *
     * @param clientList from the configuration
     */
    public void setClientArrayList(Collection<Client> clientList) {
        clientArrayList.addAll(clientList);

    }

    /**
     * Places shots randomly on the field of one opponent and sends the fitting message.
     *
     * @throws IOException
     */
    public void placeShots() throws IOException {
        //reset the 
        this.shotsRequest = null;
        this.choosenShots = null;
        //update numberOfClients every time the method is calls because the number of connected clients could have changed
        int numberOfClients = clientArrayList.size();
        int shotCount = getShotCount();
        int shotClientId;

        //get a random client Id out of the connected clients clientArrayList
        //using a random index and checking if the index is different from Ais own index
        //while(true) is used because of the short method;
        //exits the loop if the randomIndex is not same same as the aiIndex
        //randomIndex should be different from aiIndex for preventing the ai shooting on its own field
        while (true) {

            int aiIndex = clientArrayList.indexOf(this); //get the index of the ai
            int randomIndex = (int) (Math.random() * numberOfClients);
            if (randomIndex != aiIndex) {
                shotClientId = clientArrayList.get(randomIndex).getId(); //shotClientId is the target for placing shots in the next part
                break;
            }
        }

        this.choosenShots = new ArrayList<>();

        ArrayList<Point2D> aimsThisRound = new ArrayList<>();

        ArrayList<Point2D> hitPoints = new ArrayList<>();

        for (Shot k : updateNotification.getHits()) {
            if (k.getClientId() == shotClientId) {
                hitPoints.add(k.getPosition());
            }
        }

        //placing the shots randomly until the max of shots is not reached
        //all shots will be placed on the field of only one opponents field(other client)
        int i = 0;
        while (i < shotCount) {

            Point2D aimPoint = getRandomPoint2D(); //aim is one of the random points as a candidate for a shot
            boolean alreadyChoosen = false;
            for (Point2D p : aimsThisRound) {
                if (p.getX() == aimPoint.getX() & p.getY() == aimPoint.getY()) {
                    alreadyChoosen = true;
                }
            }
            for (Point2D h : hitPoints) {
                if (h.getX() == aimPoint.getX() & h.getY() == aimPoint.getY()) {
                    alreadyChoosen = true;
                }
            }
            if (alreadyChoosen) continue;

            aimsThisRound.add(aimPoint);
            //create a new shot object, add it to requestedShot Array and increase i
            Shot shot = new Shot(shotClientId, aimPoint);
            choosenShots.add(shot);
            //System.out.println(choosenShots);
            i++;

        }
        //create new shotsRequest Object with the choosenShots Collection
        System.out.println(choosenShots.size() == this.SHOTCOUNT);

        this.shotsRequest = new ShotsRequest(choosenShots);

        //send the shotsRequest object to the server
        //TODO only for testing disabled
        //connector.sendMessageToServer(shotsRequest);
    }

    /**
     * Creates a random point related to the width and height of the game field
     *
     * @return Point2d Random Point with X and Y coordinates
     */
    public Point2D getRandomPoint2D() {
        int x = (int) (Math.random() * this.width);
        int y = (int) (Math.random() * this.height);
        return new Point2D(x, y);
    }

    /**
     * Removes the client who left the game from the clientArrayList
     *
     * @param leftPlayerID ID of the PLayer who left the Game
     */
    public void handleLeaveOfPlayer(int leftPlayerID) {
        clientArrayList.removeIf(i -> i.getId() == leftPlayerID);
    }

    /**
     * Used by the connect method to set the connector
     *
     * @param connector
     */
    public void setConnector(ClientConnector connector) {
        this.connector = connector;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setShipConfig(Map<Integer, ShipType> shipConfig) {
        this.shipConfig = shipConfig;
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
        this.width = _width;
    }

    public void setHeight(int _height) {
        this.height = _height;
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

}