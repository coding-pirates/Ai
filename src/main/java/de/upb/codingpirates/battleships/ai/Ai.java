package de.upb.codingpirates.battleships.ai;

import com.google.inject.internal.cglib.core.$MethodInfoTransformer;
import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.logic.util.*;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;

import java.io.IOException;
import java.util.*;

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

    public void placeShips(Map<Integer, ShipType> shipConfig) throws IOException {
        //TODO implment the automatically positioning of the ships fitting to the placeShipsRequestMessage
        PlaceShipsRequest placeShipsRequestMessage = new PlaceShipsRequest(null); //nicht fertig
        connector.sendMessageToServer(placeShipsRequestMessage);
    }

    public void transferClientList(Collection<Client> _clientList) {
        this.clientList = _clientList;

        for (Client i : this.clientList) {
            clientArrayList.add(i);
        }

    }

    public void placeShots() throws IOException {
        //TODO sobald die erste PlayerUpdateNotification kommt, also nach der ersten Runde, muss geprüft werden,
        //TODO dass nicht bereits getroffene Felder erneut beschossen werden
        //TODO also prüfe, ob der random shot in den hits drin ist
        int maxShots = getShotCount();
        Collection<Shot> requestedShots = null;
        int numberOfClients = clientArrayList.size();
        int shotClientId;
        while (true) {
            int randomIndex = (int) (Math.random() * numberOfClients);
            shotClientId = clientArrayList.getFirst().getId();
            if (shotClientId != this.clientId) {
                break;
            }
        }

        int i = 1;
        while (i < maxShots) {
            Shot shot = new Shot(shotClientId, getRandomPoint2D());
            requestedShots.add(shot);
            i++;
        }

        ShotsRequest shotsRequest = new ShotsRequest(requestedShots);

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

}
