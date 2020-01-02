package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.ai.gameplay.ShipPlacer;
import de.upb.codingpirates.battleships.ai.gameplay.ShotPlacer;
import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.ai.util.MissesFinder;
import de.upb.codingpirates.battleships.ai.util.SunkenShipFinder;
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
 * Implements the logic of the Ai Player like placing ships and firing shots
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
    //Logger
    private static final Logger logger = LogManager.getLogger();

    int difficultyLevel;

    LinkedList<Client> clientArrayList = new LinkedList<>();
    Collection<Shot> hits = new ArrayList<>();

    //game field parameter
    int width;
    int height;
    Ai instance = this;
    int HITPOINTS;
    int SUNKPOINTS;
    int aiClientId;
    int gameId;
    Configuration config;
    Map<Integer, ShipType> ships = new HashMap<>();

    long VISUALIZATIONTIME;
    long ROUNDTIME;
    int SHOTCOUNT;


    //updated values
    Map<Integer, Integer> points = new HashMap<>();
    Collection<Shot> sunk = new ArrayList<>();
    //sunken Ships
    Map<Integer, LinkedList<Shot>> sortedSunk = new HashMap<>(); //
    Map<Integer, LinkedList<Integer>> allSunkenShipIds = new HashMap<>();
    //heatmap
    Map<Integer, Integer[][]> heatmapAllClients = new HashMap<>();
    //invalid points
    //A map which maps the client id on a collection with all invalid Points of this client
    Map<Integer, LinkedHashSet<Point2D>> invalidPointsAll = new HashMap<>();

    public PlayerUpdateNotification updateNotification;

    Collection<Shot> misses = new ArrayList<>();
    public Collection<Shot> requestedShotsLastRound = new ArrayList<>();

    private final ClientConnector tcpConnector = ClientApplication.create(new ClientModule<>(ClientConnector.class));
    private int maxPlayerCount;
    private int penaltyMinusPoints;
    private PenaltyType penaltyType;


    public Ai() {
        ListenerHandler.registerListener(this);
    }


    ShotPlacer shotPlacement = new ShotPlacer(this);

    public void placeShots(int difficultyLevel) throws IOException {
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
                myShots = shotPlacement.placeShots_3();
                sendMessage(new ShotsRequest(myShots));
                break;
            }
            default: {
                logger.error(MARKER.AI, "Input is not valid: " + difficultyLevel);
            }

        }
    }

    //ship positions of ais field
    Map<Integer, PlacementInfo> positions;

    public void placeShips() throws IOException {
        ShipPlacer shipPlacer = new ShipPlacer(this);
        setPositions(shipPlacer.placeShipsRandomly());
        sendMessage(new PlaceShipsRequest(getPositions()));
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

    public void addMisses() {
        MissesFinder missesFinder = new MissesFinder(getInstance());
        this.misses.addAll(missesFinder.computeMisses());
    }

    /**
     * Can be used to send a message to the server.
     *
     * @param message message to send
     * @throws IOException server error
     */
    public void sendMessage(Message message) throws IOException {
        tcpConnector.sendMessageToServer(message);
    }

    /**
     * Removes the client who left the game from the clientArrayList
     *
     * @param leftPlayerID ID of the PLayer who left the Game
     */
    public void handleLeaveOfPlayer(int leftPlayerID) {
        clientArrayList.removeIf(i -> i.getId() == leftPlayerID);
    }

    //getter and setter -----------------------------------------------------------------------------------------------

    public void setSortedSunk(HashMap<Integer, LinkedList<Shot>> sortedSunk) {
        this.sortedSunk = sortedSunk;
    }

    public Collection<Shot> getMisses() {
        return this.misses;
    }

    public void setMisses(Collection<Shot> misses) {
        this.misses = misses;
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
        logger.info(MARKER.AI, "Field width is {}", _width);
        this.width = _width;
    }

    public int getWidth() {
        return this.width;
    }

    public void setHeight(int _height) {
        logger.info(MARKER.AI, "Field height is {}", _height);
        this.height = _height;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHitpoints(int hitpoints) {
        logger.info(MARKER.AI, "Hitpoints: {}", hitpoints);
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

    public Map<Integer, LinkedHashSet<Point2D>> getInvalidPointsAll() {
        return this.invalidPointsAll;
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

    private void setSunkPoints(int sunkPoints) {
        logger.info(MARKER.AI, "SunkPoints: {}", sunkPoints);
        this.SUNKPOINTS = sunkPoints;
    }

    public Map<Integer, PlacementInfo> getPositions() {
        return this.positions;
    }

    public void setPositions(Map<Integer, PlacementInfo> positions) {
        this.positions = positions;
    }

    public Map<Integer, Integer[][]> getHeatmapAllClients() {
        return this.heatmapAllClients;
    }

    public void setHeatmapAllClients(Map<Integer, Integer[][]> heatmap) {
        this.heatmapAllClients = heatmap;
    }

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
        Configuration config = message.getConfiguration();

        this.setMaxPlayerCount(config.getMaxPlayerCount());
        this.setHeight(config.getHeight());
        this.setWidth(config.getWidth());
        this.setShotCount(config.getShotCount());
        this.setHitpoints(config.getHitPoints());
        this.setSunkPoints(config.getSunkPoints());
        this.setRoundTimer(config.getRoundTime());
        this.setVisualizationTime(config.getVisualizationTime());
        this.setPenaltyMinusPoints(config.getPenaltyMinusPoints());
        this.setPenaltyType(config.getPenaltyKind());

        this.setShips(config.getShips());

        this.setClientArrayList(message.getClientList());
        try {
            logger.info("Trying to place ships");
            AiMain.ai.placeShips();
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
            AiMain.ai.placeShots(this.getDifficultyLevel());
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
        SunkenShipFinder sunkenShipFinder = new SunkenShipFinder(AiMain.ai.getInstance());
        logger.info(MARKER.AI, "PlayerUpdateNotification: getting updated hits, points and sunk");
        this.setHits(message.getHits());
        this.setPoints(message.getPoints());
        this.setSunk(message.getSunk());
        //sortiere die sunks nach ihren Clients
        this.setSortedSunk(sunkenShipFinder.sortTheSunk());
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
        try {
            this.tcpConnector.sendMessageToServer(new LobbyRequest());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLobbyResponse(LobbyResponse message, int clientId) {
        logger.info(MARKER.AI, "LobbyResponse");
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