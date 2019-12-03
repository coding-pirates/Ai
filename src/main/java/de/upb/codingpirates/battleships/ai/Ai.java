package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.logic.util.*;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import org.checkerframework.framework.qual.Unused;

import java.io.IOException;
import java.util.*;

//TODO getter und setter fehlen zum Teil
public class Ai {
    int clientId;
    Timer timer;
    GameState gameState;
    ClientConnector connector;
    String host;
    int port;
    Configuration config;
    int gameId;
    Collection<Client> clientList;
    LinkedList<Client> clientArrayList = new LinkedList<>();
    Collection<Shot> hits;

    //PlayerUpdateNotificationInformation
    PlayerUpdateNotification updateNotification;


    //game field parameter
    int width;
    int height;

    public void aiConnect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        ClientConnector connector = ClientApplication.create(AiModule.class);
        connector.connect(host, port);
        connector.sendMessageToServer(null);
        setConnector(connector);
    }

    public void setConnector(ClientConnector connector) {
        this.connector = connector;
    }

    public void setClientId(int _clientId) {
        this.clientId = _clientId;
    }

    public void setConfig(Configuration _config) {
        //not used
        this.config = _config;
        int height = config.HEIGHT;
        int width = config.WIDTH;
        int hitpoints = config.HITPOINTS;
        int sunkpoints = config.SUNKPOINTS;
        long roundTime = config.ROUNDTIME;
        long visualizationTime = config.VISUALIZATIONTIME;
        int shotCount = config.SHOTCOUNT;
        Map<Integer, ShipType> shipConfig = config.getShipTypes();


    }

    public void setWidth(int _width) {
        this.width = _width;
    }

    public void setHeight(int _height) {
        this.height = _height;
    }


    boolean successful = false;
    //gets ShipId an PLacementInfo for PlaceShipRequest
    Map<Integer, PlacementInfo> positions;

    public void placeShips(Map<Integer, ShipType> shipConfig) throws IOException {
        //TODO Funktionalität prüfen und testen, vor allem auf richtigen Ablauf der Schleifen achten
        while (successful == false) {
            randomShipGuesser(shipConfig);
        }
        randomShipGuesser(shipConfig);
        PlaceShipsRequest placeShipsRequestMessage = new PlaceShipsRequest(positions);
        connector.sendMessageToServer(placeShipsRequestMessage);
    }


    ArrayList<Point2D> usedFields = new ArrayList<>();

    private void randomShipGuesser(Map<Integer, ShipType> shipConfig) {

        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {

            int shipId = entry.getKey();

            Collection<Point2D> shipPos = entry.getValue().getPosition();

            Point2D nullPunkt = null;

            for (Point2D p : shipPos) {
                if (p.getX() == 0 & p.getY() == 0) {
                    nullPunkt = p;
                    break;
                }
            }

            //random point for placing the ship
            Point2D guessPoint = getRandomPoint2D();

            int distanceX = guessPoint.getX();
            int distanceY = guessPoint.getY();

            Collection<Point2D> randomShipPos = new ArrayList<>();

            for (Point2D i : shipPos) {
                int newX = i.getX() + distanceX;
                int newY = i.getY() + distanceY;
                Point2D newPoint = new Point2D(newX, newY);
                if (usedFields.contains(newPoint)) {
                    usedFields.clear();
                    positions.clear();
                    return;
                } else {
                    randomShipPos.add(newPoint);
                    PlacementInfo pInfo = new PlacementInfo(guessPoint, Rotation.NONE);
                    positions.put(shipId, pInfo);
                }
            }
        }
        successful = true;

    }

    //die übergebene Collection clientList in eine LinkedList clientArrayList überführen
    //für mehr Fubktionalität
    public void transferClientList(Collection<Client> _clientList) {
        this.clientList = _clientList;
        for (Client i : this.clientList) {
            clientArrayList.add(i);
        }

    }


    public void placeShots() throws IOException {
        //nicht außerhalb damit numberOfClients bei jedem aufruf der methode erneut geupdated wird,
        //falls es eine LeaveNot. gab
        int numberOfClients = clientArrayList.size();
        //TODO placeShots sollte nach einem möglichen Treffer nicht wieder random schießen

        int maxShots = getShotCount();
        Collection<Shot> requestedShots = null;
        int shotClientId;

        //get a random client Id out of the connected clients clientArrayList
        //using a random index and checking if the index is different from Ais own index
        while (true) {
            int aiArrayIndex = clientArrayList.indexOf(AiMain.ai); //this.ai ??
            int randomIndex = (int) (Math.random() * numberOfClients);
            if (randomIndex != aiArrayIndex) {
                shotClientId = clientArrayList.get(aiArrayIndex).getId();
                break;
            }
        }

        //placing the shots randomly until the max of shots is not reached
        //all shots will be placed on the field of only one opponents field(other client)
        int i = 1;
        while (i < maxShots) {

            Point2D aim = getRandomPoint2D(); //aim is one of the random points as a candidate for a shot

            //check if the requestedShot of this round or the hits contain the aim shot
            if (!requestedShots.contains(aim) & !updateNotification.getHits().contains(aim)) {
                //create a new shot object, add it to requestedShot Array and increase i
                Shot shot = new Shot(shotClientId, getRandomPoint2D());
                requestedShots.add(shot);
                i++;
            }
        }
        //create new shotsrequest Object with the requestedShots Collection
        ShotsRequest shotsRequest = new ShotsRequest(requestedShots);

        //send the shotsRequest object to the server
        connector.sendMessageToServer(shotsRequest);
    }

    public Point2D getRandomPoint2D() {
        int x = (int) (Math.random() * this.width);
        int y = (int) (Math.random() * this.height);
        return new Point2D(x, y);
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