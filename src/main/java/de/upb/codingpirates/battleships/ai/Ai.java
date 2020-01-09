package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.ai.gameplay.ShipPlacer;
import de.upb.codingpirates.battleships.ai.gameplay.ShotPlacer;
import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.ai.util.MissesFinder;
import de.upb.codingpirates.battleships.ai.util.SunkenShipsHandler;
import de.upb.codingpirates.battleships.client.ListenerHandler;
import de.upb.codingpirates.battleships.client.listener.*;
import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.client.network.ClientModule;
import de.upb.codingpirates.battleships.logic.*;
import de.upb.codingpirates.battleships.network.exceptions.BattleshipException;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.network.message.request.LobbyRequest;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.RequestBuilder;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import de.upb.codingpirates.battleships.network.message.response.GameJoinPlayerResponse;
import de.upb.codingpirates.battleships.network.message.response.LobbyResponse;
import de.upb.codingpirates.battleships.network.message.response.ServerJoinResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

//TODO some getter/setter are missing

/**
 * The model of the ai player. Stores the game configuration and values, handles the message sending,
 * the ship and shot placement.
 * <p>
 * Implements also the message listener interfaces and therefore also the message handling functionality.
 *
 * @author Benjamin Kasten
 */
public class Ai implements
        BattleshipsExceptionListener,
        ConnectionClosedReportListener,
        ErrorNotificationListener,
        FinishNotificationListener,
        GameInitNotificationListener,
        GameStartNotificationListener,
        LeaveNotificationListener,
        PauseNotificationListener,
        PlayerUpdateNotificationListener,
        RoundStartNotificationListener,
        TournamentFinishNotificationListener,
        GameJoinPlayerResponseListener,
        ServerJoinResponseListener,
        LobbyResponseListener {

    private static final Logger logger = LogManager.getLogger();

    int difficultyLevel;

    LinkedList<Client> clientArrayList = new LinkedList<>();
    Collection<Shot> hits = new ArrayList<>();

    //game field parameter
    int width;
    int height;
    int hitPoints;
    int sunkPoints;
    long visualizationTime;
    long roundTime;
    int shotCount;
    int maxPlayerCount;
    int penaltyMinusPoints;
    PenaltyType penaltyType;


    int aiClientId;
    int gameId;

    Ai instance = this;
    Map<Integer, ShipType> ships = new HashMap<>(); //all ships which have to be placed (shipConfig)

    Map<Integer, Integer> points = new HashMap<>(); //points of the clients
    //sunken ships
    Collection<Shot> sunk = new ArrayList<>(); //sunks which are updated every round
    Map<Integer, LinkedList<Shot>> sortedSunk = new HashMap<>(); //
    Map<Integer, LinkedList<Integer>> allSunkenShipIds = new HashMap<>();
    //heatmap
    Map<Integer, Double[][]> heatmapAllClients = new HashMap<>();
    //invalid points per client id
    Map<Integer, LinkedHashSet<Point2D>> invalidPointsAll = new HashMap<>();

    Collection<Shot> misses = new ArrayList<>(); //all misses of this player

    public Collection<Shot> requestedShotsLastRound = new ArrayList<>(); //the latest requested shots

    //the ClientConnector fot this ai instance
    private final ClientConnector tcpConnector = ClientApplication.create(new ClientModule<>(ClientConnector.class));

    /**
     * Constructor which is needed for register this ai instance as message listener.
     */
    public Ai() {
        ListenerHandler.registerListener(this);
    }


    /**
     * Is called every round for placing shots. Using a {@link ShotPlacer} object and the difficulty level,
     * the method calls the matching method for shot placement and sends the result (the
     * calculated shots) to the server using the {@link #sendMessage} method.
     *
     * @param difficultyLevel The difficulty level of this ai instance.
     * @throws IOException Server connection error
     */
    public void placeShots(int difficultyLevel) throws IOException {

        ShotPlacer shotPlacement = new ShotPlacer(this);

        Collection<Shot> myShots;

        switch (difficultyLevel) {
            case 1: {
                logger.info(MARKER.AI, "Difficulty Level 1 selected");
                myShots = shotPlacement.placeShots_1();
                sendMessage(new ShotsRequest(myShots));
                break;
            }
            case 2: {
                logger.info(MARKER.AI, "Difficulty Level 2 selected");
                myShots = shotPlacement.placeShots_2();
                sendMessage(new ShotsRequest(myShots));
                break;
            }
            case 3: {
                logger.info(MARKER.AI, "Difficulty level 3 selected");
                myShots = shotPlacement.placeShots_3_2();
                sendMessage(new ShotsRequest(myShots));
                break;
            }
            default: {
                logger.error(MARKER.AI, "The difficulty level ({}) is not valid, start again and use choose " +
                        "between the level 1, 2 or 3", difficultyLevel);
            }
        }
    }

    Map<Integer, PlacementInfo> positions; //ship positions of Ais field

    /**
     * Creates a {@link ShipPlacer} instance and calls {@link ShipPlacer#guessRandomShipPositions(Map)} method.
     * Sets the placement by calling {@link #setPositions(Map)} and calls {@link #sendMessage(Message)} for
     * sending the {@link PlaceShipsRequest} to the server.
     *
     * @throws IOException Connection error.
     */
    public void placeShips() throws IOException {
        ShipPlacer shipPlacer = new ShipPlacer(this);
        shipPlacer.placeShipDEBUG();
        //setPositions(shipPlacer.placeShipsRandomly());

        //sendMessage(new PlaceShipsRequest(getPositions()));
    }

    /**
     * Creates a new ArrayList(Shot) with only one element
     *
     * @param i The Shot object which will be the only object in the list
     * @return The "one element" list
     * @deprecated replaced by {@code new LinkedList<>(Collections.singletonList(i))}
     */
    public LinkedList<Shot> createArrayListOneArgument(Shot i) {
        LinkedList<Shot> list = new LinkedList<>();
        list.add(i);
        return list;
    }

    /**
     * Calculates the misses of last round and adds them to the misses collection
     */
    public void addMisses() {
        MissesFinder missesFinder = new MissesFinder(this);
        this.misses.addAll(missesFinder.computeMisses());
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
     * Can be used to send a message to the server.
     *
     * @param message message to send
     * @throws IOException server connection error
     */
    public void sendMessage(Message message) throws IOException {
        tcpConnector.sendMessageToServer(message);
    }

    public void addPointsToInvalid(Shot s) {
        invalidPointsAll.putIfAbsent(s.getClientId(), new LinkedHashSet<>());
        LinkedHashSet<Point2D> temp = new LinkedHashSet<>(this.invalidPointsAll.get(s.getClientId()));
        temp.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));

        invalidPointsAll.replace(s.getClientId(), temp);

    }

    public void addPointsToInvalid(Collection<Shot> shots) {
        logger.info("Added following points to invalid points: ");

        for (Shot s : shots) {
            invalidPointsAll.putIfAbsent(s.getClientId(), new LinkedHashSet<>());
            LinkedHashSet<Point2D> temp = new LinkedHashSet<>(this.invalidPointsAll.get(s.getClientId()));
            temp.add(new Point2D(s.getTargetField().getX(), s.getTargetField().getY()));
            invalidPointsAll.replace(s.getClientId(), temp);
            logger.info(s);
        }
    }


    //getter and setter -----------------------------------------------------------------------------------------------


    /**
     * Only for converting the Client Collection of the {@link GameInitNotification} into a LinkedList to have more
     * functions.
     * Called by {@link #onGameInitNotification(GameInitNotification, int)}
     *
     * @param clientList from the configuration
     */
    public void setClientArrayList(Collection<Client> clientList) {
        clientArrayList.addAll(clientList);
    }

    public LinkedList<Client> getClientArrayList() {
        return this.clientArrayList;
    }

    public void setSortedSunk(HashMap<Integer, LinkedList<Shot>> sortedSunk) {
        this.sortedSunk = sortedSunk;
    }

    public Map<Integer, LinkedList<Shot>> getSortedSunk() {
        return this.sortedSunk;
    }

    public void setMisses(Collection<Shot> misses) {
        this.misses = misses;
    }

    public Collection<Shot> getMisses() {
        return this.misses;
    }

    public void setAiClientId(int aiClientId) {
        this.aiClientId = aiClientId;
    }

    public int getAiClientId() {
        return this.aiClientId;
    }

    public void setShips(Map<Integer, ShipType> ships) {
        this.ships = ships;
    }

    public Map<Integer, ShipType> getShips() {
        return this.ships;
    }

    public ClientConnector getTcpConnector() {
        return tcpConnector;
    }

    public void setShotCount(int shotCount) {
        this.shotCount = shotCount;
    }

    public int getShotCount() {
        return this.shotCount;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getGameId() {
        return this.gameId;
    }

    public void setWidth(int _width) {
        this.width = _width;
    }

    public int getWidth() {
        return this.width;
    }

    public void setHeight(int _height) {
        this.height = _height;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHits(Collection<Shot> hits) {
        this.hits = hits;
    }

    public Collection<Shot> getHits() {
        return this.hits;
    }

    public void setSunk(Collection<Shot> sunk) {
        this.sunk = sunk;
    }

    public Collection<Shot> getSunk() {
        return this.sunk;
    }

    public Map<Integer, LinkedHashSet<Point2D>> getInvalidPointsAll() {
        return this.invalidPointsAll;
    }

    public void setPoints(Map<Integer, Integer> points) {
        this.points = points;
    }


    public void setPositions(Map<Integer, PlacementInfo> positions) {
        this.positions = positions;
    }

    public Map<Integer, PlacementInfo> getPositions() {
        return this.positions;
    }

    public void setHeatmapAllClients(Map<Integer, Double[][]> heatmap) {
        this.heatmapAllClients = heatmap;
    }

    public Map<Integer, Double[][]> getHeatmapAllClients() {
        return this.heatmapAllClients;
    }

    public void setSunkenShipIdsAll(Map<Integer, LinkedList<Integer>> sunkenIds) {
        this.allSunkenShipIds = sunkenIds;
    }

    public Map<Integer, LinkedList<Integer>> getAllSunkenShipIds() {
        return this.allSunkenShipIds;
    }

    public void setInstance(Ai instance) {
        this.instance = instance;
    }

    public Ai getInstance() {
        return instance;
    }

    public void setDifficultyLevel(int d) {
        this.difficultyLevel = d;
    }

    public int getDifficultyLevel() {
        return this.difficultyLevel;
    }

    public void setHitPoints(int hitPoints) {
        this.hitPoints = hitPoints;
    }

    public int getHitPoints() {
        return this.hitPoints;
    }

    private void setSunkPoints(int sunkPoints) {
        this.sunkPoints = sunkPoints;
    }

    public int getSunkPoints() {
        return this.sunkPoints;
    }

    private void setRoundTimer(long roundTime) {
        this.roundTime = roundTime;
    }

    public long getRoundTime() {
        return this.roundTime;
    }

    private void setVisualizationTime(long visualizationTime) {
        this.visualizationTime = visualizationTime;
    }

    public long getVisualizationTime() {
        return this.visualizationTime;
    }


    /**
     * Sets the configuration values.
     *
     * @param config The game configuration.
     */
    public void setConfig(Configuration config) {
        logger.info("Setting configuration...");

        this.setMaxPlayerCount(config.getMaxPlayerCount());
        logger.info("Maximum player count: {}", getMaxPlayerCount());
        this.setHeight(config.getHeight());
        logger.info("Height: {}", getHeight());
        this.setWidth(config.getWidth());
        logger.info("Width: {}", getWidth());
        this.setShotCount(config.getShotCount());
        logger.info("Shots per round: {}", getShotCount());
        this.setHitPoints(config.getHitPoints());
        logger.info("Points for a hit: {}", getHitPoints());
        this.setSunkPoints(config.getSunkPoints());
        logger.info("Points for a sunk: {}", getSunkPoints());
        this.setRoundTimer(config.getRoundTime());
        logger.info("RoundTime: {}", getRoundTime());
        this.setVisualizationTime(config.getVisualizationTime());
        logger.info("Visualization time: {}", getVisualizationTime());
        this.setPenaltyMinusPoints(config.getPenaltyMinusPoints());
        logger.info("PenaltyMinusPoints: {}", getPenaltyMinusPoints());
        this.setPenaltyType(config.getPenaltyKind());
        logger.info("PenaltyType: {}", getPenaltyType());
        this.setShips(config.getShips());

        logger.info("Number of ships: {}", getShips().size());

        logger.info("Setting configuration successful.");
    }


    //Message listening-------------------------------------------------------------------------


    @Override
    public void onConnectionClosedReport(ConnectionClosedReport message, int clientId) {
        logger.info(MARKER.AI, "ConnectionClosedReport");
        AiMain.close();
    }

    @Override
    public void onBattleshipException(BattleshipException error, int clientId) {
        logger.error(MARKER.AI, "BattleshipException");
    }

    @Override
    public void onErrorNotification(ErrorNotification message, int clientId) {
        logger.info(MARKER.AI, "ErrorNotification");
        logger.error(MARKER.AI, "Errortype: " + message.getErrorType());
        logger.error(MARKER.AI, "Error occurred in Message: " + message.getReferenceMessageId());
        logger.error(MARKER.AI, "Reason: " + message.getReason());
    }

    @Override
    public void onFinishNotification(FinishNotification message, int clientId) {
        logger.info(MARKER.AI, "FinishNotification");
        AiMain.close();
    }


    @Override
    public void onGameInitNotification(GameInitNotification message, int clientId) {
        logger.info(MARKER.AI, "GameInitNotification: got clients and configuration");
        logger.info(MARKER.AI, "Connected clients size: {}", message.getClientList().size());
        logger.info(MARKER.AI, "Connected clients are: ");
        for (Client c : message.getClientList()) {
            logger.info("Client name {}, client id {}", c.getName(), c.getId());
        }
        logger.debug("Own id is {}", getAiClientId());
        setConfig(message.getConfiguration());
        this.setClientArrayList(message.getClientList());
        try {
            logger.info("Trying to place ships");
            placeShips();
        } catch (IOException e) {
            logger.error("Ship placement failed");
            e.printStackTrace();
        }
    }

    @Override
    public void onGameStartNotification(GameStartNotification message, int clientId) {
        logger.info(MARKER.AI, "GameStartNotification: game started, first shot placement with difficulty level {}", this.getDifficultyLevel());
        try {
            logger.info("Trying to place shots");
            this.placeShots(this.getDifficultyLevel());
        } catch (IOException e) {
            logger.error("Shot placement failed");
            e.printStackTrace();
        }

    }

    @Override
    public void onLeaveNotification(LeaveNotification message, int clientId) {
        logger.info(MARKER.AI, "LeaveNotification: left player id is: " + message.getPlayerId());
        this.handleLeaveOfPlayer(message.getPlayerId());
    }

    @Override
    public void onPauseNotification(PauseNotification message, int clientId) {
        logger.info(MARKER.AI, "PauseNotification");
    }

    @Override
    public void onPlayerUpdateNotification(PlayerUpdateNotification message, int clientId) {

        logger.info(MARKER.AI, "PlayerUpdateNotification: getting updated hits, points and sunk");
        logger.debug("All Hits: ");
        if (message.getHits().isEmpty()) {
            logger.debug("no hits");
        } else {
            for (Shot s : message.getHits()) {
                logger.debug(s);
            }
        }
        this.setHits(message.getHits());

        logger.debug("All Sunk: ");
        if (message.getSunk().isEmpty()) {
            logger.debug("no sunk");
        } else {
            for (Shot s : message.getSunk()) {
                logger.debug(s);
            }
        }
        this.setSunk(message.getSunk());

        this.setPoints(message.getPoints());

        MissesFinder missesFinder = new MissesFinder(this);

        Collection<Shot> missesLastRound = missesFinder.computeMisses();

        this.misses.addAll(missesLastRound);
        logger.debug("Added these misses to misses");
        for (Shot s : missesLastRound){
            System.out.println(s);
        }

        //sortiere die sunks nach ihren Clients mit dem SunkenShipsHandler
        SunkenShipsHandler sunkenShipsHandler = new SunkenShipsHandler(AiMain.ai.getInstance());
        this.setSortedSunk(sunkenShipsHandler.sortTheSunk());
    }

    @Override
    public void onRoundStartNotification(RoundStartNotification message, int clientId) {
        logger.info(MARKER.AI, "RoundStartNotification: placing shots");
        try {
            AiMain.ai.placeShots(this.getDifficultyLevel());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTournamentFinishNotification(TournamentFinishNotification message, int clientId) {
        logger.info(MARKER.AI, "TournamentFinishNotification: ");
    }

    @Override
    public void onGameJoinPlayerResponse(GameJoinPlayerResponse message, int clientId) {
        logger.info(MARKER.AI, "GameJoinPlayerResponse: joined game with iId: {}", message.getGameId());
        this.setGameId(message.getGameId());
    }

    @Override
    public void onServerJoinResponse(ServerJoinResponse message, int clientId) {
        logger.info(MARKER.AI, "ServerJoinResponse, AiClientId is: {}", message.getClientId());
        this.setAiClientId(message.getClientId());
        try {
            this.tcpConnector.sendMessageToServer(new LobbyRequest());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLobbyResponse(LobbyResponse message, int clientId) {
        logger.info(MARKER.AI, "LobbyResponse");
        try {
            sendMessage(RequestBuilder.gameJoinPlayerRequest(0));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    public int getMaxPlayerCount() {
        return maxPlayerCount;
    }

    public void setPenaltyMinusPoints(int penaltyMinusPoints) {
        this.penaltyMinusPoints = penaltyMinusPoints;
    }

    public int getPenaltyMinusPoints() {
        return penaltyMinusPoints;
    }

    public void setPenaltyType(PenaltyType penaltyType) {
        this.penaltyType = penaltyType;
    }

    public PenaltyType getPenaltyType() {
        return penaltyType;
    }
}