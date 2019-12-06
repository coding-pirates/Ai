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

//TODO getter und setter fehlen zum Teil

/**
 * Implements the logic of the Ai Player like placing ships and firing shots
 *
 * @author Benjamin Kasten
 */
public class Ai {
    int clientId;
    //GameState gameState;
    ClientConnector connector;
    String host; //ip address
    int port;
    Configuration config;
    int gameId;
    Map<Integer, ShipType> shipConfig;

    //convert the Collection clintList in a LinkedList for handling random shots is done in the
    Collection<Client> clientList;
    LinkedList<Client> clientArrayList = new LinkedList<>();
    Collection<Shot> hits;

    //PlayerUpdateNotificationInformation
    PlayerUpdateNotification updateNotification;


    //game field parameter
    int width;
    int height;

    /**
     * Is called by the {@link AiMain#main(String[])} for connecting to the server
     *
     * @param host IP Address
     * @param port Port number
     * @throws IOException
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
        this.config = config;
        this.height = config.HEIGHT;
        this.width = config.WIDTH;
        this.shipConfig = config.getShipTypes();
        //not used
        int hitpoints = config.HITPOINTS;
        int sunkpoints = config.SUNKPOINTS;
        long roundTime = config.ROUNDTIME;
        long visualizationTime = config.VISUALIZATIONTIME;
        int shotCount = config.SHOTCOUNT;

    }


    //remains false until a
    boolean successful = false;
    //gets ShipId an PLacementInfo for PlaceShipRequest
    Map<Integer, PlacementInfo> positions;

    /**
     * Calls the {link {@link #randomShipGuesser(Map)}} and if it returns true send the {@link PlaceShipsRequest}
     *
     * @throws IOException
     */
    public void placeShips() throws IOException {
        //TODO Funktionalität prüfen und testen, vor allem auf richtigen Ablauf der Schleifen achten
        while (successful == false) {
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
    //die übergebene Collection clientList in eine LinkedList clientArrayList überführen
    //für mehr Funktionalität in der playShots Methode

    /**
     * Only for converting the Client Collection of the {@link GameInitNotification} into a LinkedList to have better
     * fitting functionality .
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
        //update numberOfClients every time the method is calles because the number of connected clients could have changed
        int numberOfClients = clientArrayList.size();
        int shotCount = getShotCount();
        Collection<Shot> requestedShots = null;
        int shotClientId;

        //get a random client Id out of the connected clients clientArrayList
        //using a random index and checking if the index is different from Ais own index
        //while(true) is used because of the short method;
        //exits the loop if the randomIndex is not same same as the aiIndex
        //randomIndex should be different from aiIndex for preventing the ai shooting on its own field
        while (true) {
            int aiIndex = clientArrayList.indexOf(AiMain.ai); //get the index of the ai
            int randomIndex = (int) (Math.random() * numberOfClients);
            if (randomIndex != aiIndex) {
                shotClientId = clientArrayList.get(randomIndex).getId(); //shotClientId is the target for placing shots in the next part
                break;
            }
        }

        //placing the shots randomly until the max of shots is not reached
        //all shots will be placed on the field of only one opponents field(other client)
        int i = 1;
        while (i < shotCount) {

            Point2D aim = getRandomPoint2D(); //aim is one of the random points as a candidate for a shot

            //check if the requestedShot of this round or the hits contain the aim shot
            if (!requestedShots.contains(aim) & !updateNotification.getHits().contains(aim)) {
                //create a new shot object, add it to requestedShot Array and increase i
                Shot shot = new Shot(shotClientId, getRandomPoint2D());
                requestedShots.add(shot);
                i++;
            }
        }
        //create new shotsRequest Object with the requestedShots Collection
        ShotsRequest shotsRequest = new ShotsRequest(requestedShots);

        //send the shotsRequest object to the server
        connector.sendMessageToServer(shotsRequest);
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
     * Used by the connect method to set the connecter
     *
     * @param connector
     */
    public void setConnector(ClientConnector connector) {
        this.connector = connector;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getGameId() {
        return this.gameId;
    }

    public int getShotCount() {
        return this.config.SHOTCOUNT;
    }

    //wird direkt durch setConfig festgelegt
    public void setWidth(int _width) {
        this.width = _width;
    }

    public void setHeight(int _height) {
        this.height = _height;
    }


    //nicht nötig da schon durch this.PlayerUpdateNotification implementiert
    public void setHits(Collection<Shot> hits) {
        this.hits = hits;
    }

    //nicht nötig da schon durch this.PlayerUpdateNotification implementiert
    public Collection<Shot> getHits() {
        return this.hits;
    }

}