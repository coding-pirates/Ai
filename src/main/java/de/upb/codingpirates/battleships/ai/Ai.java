package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.logic.util.*;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;

import java.io.IOException;
import java.util.*;

//TODO getter und setter fehlen zum Teil
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

    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        ClientConnector connector = ClientApplication.create(AiModule.class);
        connector.connect(host, port);
        connector.sendMessageToServer(null);
        setConnector(connector);
    }


    public void setConfig(Configuration _config) {
        //not used
        this.config = _config;
        this.height = config.HEIGHT;
        this.width = config.WIDTH;
        int hitpoints = config.HITPOINTS;
        int sunkpoints = config.SUNKPOINTS;
        long roundTime = config.ROUNDTIME;
        long visualizationTime = config.VISUALIZATIONTIME;
        int shotCount = config.SHOTCOUNT;
        this.shipConfig = config.getShipTypes();

    }


    //remains false until a
    boolean successful = false;
    //gets ShipId an PLacementInfo for PlaceShipRequest
    Map<Integer, PlacementInfo> positions;

    public void placeShips() throws IOException {
        //TODO Funktionalität prüfen und testen, vor allem auf richtigen Ablauf der Schleifen achten
        while (successful == false) {
            randomShipGuesser(this.shipConfig);
        }
        PlaceShipsRequest placeShipsRequestMessage = new PlaceShipsRequest(positions);
        connector.sendMessageToServer(placeShipsRequestMessage);
    }


    ArrayList<Point2D> usedFields = new ArrayList<>();

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
    public void setClientArrayList(Collection<Client> clientList) {
        clientArrayList.addAll(clientList);

    }


    public void placeShots() throws IOException {
        //update numberOfClients every time the method is calles because the number of connected clients could have changed
        int numberOfClients = clientArrayList.size();
        int shotCount = getShotCount();
        Collection<Shot> requestedShots = null;
        int shotClientId;

        //get a random client Id out of the connected clients clientArrayList
        //using a random index and checking if the index is different from Ais own index
        //while(true) is used because of the short method; it exits the loop if the randomIndex is not same same as the aiIndex
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

    public Point2D getRandomPoint2D() {
        int x = (int) (Math.random() * this.width);
        int y = (int) (Math.random() * this.height);
        return new Point2D(x, y);
    }

    public void handleLeaveOfPlayer(int leftPlayerID) {
        for (Client i : clientArrayList) {
            if (i.getId() == leftPlayerID) {
                clientArrayList.remove(i);
            }
        }
    }


    public void setConnector(ClientConnector connector) {
        this.connector = connector;
    }

    public void setClientId(int _clientId) {
        this.clientId = _clientId;
    }

    public void setGameId(int _gameId) {
        this.gameId = _gameId;
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
//unused code here:
/*


        ArrayList<Point2D> usedFields = new ArrayList<>();
        ArrayList<ShipType> ships = (ArrayList) shipConfig.values();
        ArrayList<Point2D> yChange = new ArrayList<>(); //temporär; nur schiffe die nur in y richtung verschoben worden sind
        ArrayList<Point2D> xChange = new ArrayList<>(); //das ist die fertige Feld mit allen schiffen


        Map<Integer, PlacementInfo> psr = new HashMap<>();

        // für jedes Schiff in ships: hole dessen positions (Collection aus Points2D
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            width = AiMain.ai.width - 1;
            height = AiMain.ai.height - 1;

            //größe des schiffes richtung x und y größte punkte
            int maxX = -1;
            int maxY = -1;


            int ShipId = entry.getKey();
            PlacementInfo pInfo = null;
            Collection<Point2D> positions = entry.getValue().getPosition();
            //TODO herausfinden , wie weit man maximal nach y und x gehen darf
            //TODO height und width einschränken

            for (Point2D j : positions) {
                if (j.getX() > maxX) maxX = j.getX();
                if (j.getY() > maxY) maxY = j.getY();
            }

            //Parameter für Verschiebung y Richtung
            ArrayList<Integer> möglicheGrenzeX = null; //besteht aus besetzetn points über dem schiff
            int grenzeX = height;
            ArrayList<Integer> xWerte = new ArrayList();

            //mögliche Verschiebung des Schiffs in y Richtung berechnen
            if (xChange.isEmpty() == false) {

                for (Point2D i : positions) {
                    xWerte.add(i.getX());
                }
                for (Integer x : xWerte) {
                    for (Point2D k : xChange) {
                        if (k.getX() == x) {
                            möglicheGrenzeX.add(k.getY());
                        }
                    }
                }
                for (int z : möglicheGrenzeX) {
                    grenzeX = Collections.min(möglicheGrenzeX);
                }
                height = grenzeX;
            }

            //Verschiebung in y Richtung
            for (Point2D k : positions) {
                yChange.add(new Point2D(k.getX(), height - maxY));
            }

            // Variablen für Verschiebung x richtung
            ArrayList<Integer> möglicheGrenzeY = null; //besteht aus besetzten points rechts vom schiff
            int grenzeY = width;
            ArrayList<Integer> yWerte = new ArrayList();

            //Berechnen der möglichen Verschiebung in X Richtung
            if (yChange.isEmpty() == false) {

                for (Point2D i : positions) {
                    yWerte.add(i.getY());
                }
                for (Integer y : xWerte) {
                    for (Point2D k : yChange) {
                        if (k.getY() == y) {
                            möglicheGrenzeY.add(k.getX());
                        }
                    }
                }
                for (int z : möglicheGrenzeY) {
                    grenzeY = Collections.min(möglicheGrenzeY);
                }
                width = grenzeY;
            }

            //größe des schiffs in x und y richtung festlegen durch höchsten punkt jeweils


            for (Point2D l : yChange) {
                xChange.add(new Point2D(width - maxX, l.getY()));
            }

            for (Point2D m : positions) {
                if (m.getX() == 0 & m.getY() == 0) {
                    int pInfoX = width - maxX;
                    int pInfoY = height - maxY;
                    pInfo = new PlacementInfo(new Point2D(pInfoX, pInfoY), Rotation.NONE);

                }
            }

            psr.put(ShipId, pInfo);
        }


 */