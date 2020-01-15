package de.upb.codingpirates.battleships.ai;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.ai.gameplay.ShipPlacer;
import de.upb.codingpirates.battleships.ai.gameplay.ShotPlacer;
import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.ai.util.HeatmapCreator;
import de.upb.codingpirates.battleships.ai.util.MissesFinder;
import de.upb.codingpirates.battleships.ai.util.SunkenShipsHandler;
import de.upb.codingpirates.battleships.ai.util.Triple;
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
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.RequestBuilder;
import de.upb.codingpirates.battleships.network.message.response.GameJoinPlayerResponse;
import de.upb.codingpirates.battleships.network.message.response.LobbyResponse;
import de.upb.codingpirates.battleships.network.message.response.ServerJoinResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

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
    public LinkedList<Triple<Integer, Point2D, Double>> allHeatVal;

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

    Ai instance = this;
    int aiClientId;
    int gameId;

    int roundCounter = 0;

    Map<Integer, PlacementInfo> positions; //ship positions of Ais field

    Map<Integer, ShipType> ships = Maps.newHashMap(); //all ships which have to be placed (shipConfig)

    int sizeOfPointsToHit;

    Map<Integer, Integer> points = Maps.newHashMap(); //points of the clients

    Collection<Shot> sunk = Lists.newArrayList(); //sunks which are updated every round
    Map<Integer, LinkedList<Point2D>> sortedSunk = Maps.newHashMap(); //sunks sorted by their clients
    Map<Integer, LinkedList<Integer>> allSunkenShipIds = Maps.newHashMap(); //sunk ship ids by their clients

    Map<Integer, Double[][]> heatmapAllClients = Maps.newHashMap(); //heatmaps

    Map<Integer, LinkedList<Point2D>> invalidPointsAll = Maps.newHashMap(); //invalid points per client id

    Collection<Shot> misses = Lists.newArrayList(); //all misses of this player

    public Collection<Shot> requestedShotsLastRound = Lists.newArrayList(); //the latest requested shots
    public Collection<Shot> requestedShots = Lists.newArrayList(); //all requested shots

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
     * <p>
     * Difficulty level sets shot placement algorithm:
     * case 1: random
     * case 2: hunt and target
     * case 3: (extended) heatmap
     *
     * @param difficultyLevel The difficulty level of this ai instance.
     * @throws IOException Server connection error
     */
    public void placeShots(int difficultyLevel) throws IOException {

        ShotPlacer shotPlacement = new ShotPlacer(this);

        Collection<Shot> myShots = Collections.emptyList();

        switch (difficultyLevel) {
            case 1: {
                logger.info(MARKER.Ai, "Difficulty Level 1 (Random) selected");
                myShots = shotPlacement.placeShots_1(this.getShotCount());
                break;
            }
            case 2: {
                logger.info(MARKER.Ai, "Difficulty Level 2 (Hunt & Target) selected");
                myShots = shotPlacement.placeShots_2();
                break;
            }
            case 3: {
                logger.info(MARKER.Ai, "Difficulty level 3 (HeatMap) selected");
                myShots = shotPlacement.placeShots_Relative_3();
                break;
            }
            default: {
                //todo schon im vorhinein die eingabe von anderen werten verbieten (Scanner Implementierung)
                logger.error(MARKER.Ai, "The difficulty level ({}) is not valid, run again and use choose " +
                        "between the level 1, 2 or 3", difficultyLevel);
                AiMain.close();
            }
        }
        sendMessage(RequestBuilder.shotsRequest(myShots));

    }


    /**
     * Creates a {@link ShipPlacer} instance and calls {@link ShipPlacer#guessRandomShipPositions(Map)} method.
     * Sets the placement by calling {@link #setPositions(Map)} and calls {@link #sendMessage(Message)} (Message)} for
     * sending the {@link PlaceShipsRequest} to the server.
     *
     * @throws IOException Connection error.
     */
    public void placeShips() throws IOException {
        ShipPlacer shipPlacer = new ShipPlacer(this);

        setPositions(shipPlacer.placeShipsRandomly());

        sendMessage(RequestBuilder.placeShipsRequest(getPositions()));
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

    public void addPointsToInvalid(Point2D point, int clientId) {
        invalidPointsAll.putIfAbsent(clientId, new LinkedList<>(Collections.emptyList()));
        LinkedList<Point2D> temp = new LinkedList<>(this.invalidPointsAll.get(clientId));
        temp.add(new Point2D(point.getX(), point.getY()));

        invalidPointsAll.replace(clientId, temp);

    }

    public void addPointsToInvalid(Collection<Point2D> points, int clientId) {
        if (points == null | Objects.requireNonNull(points).isEmpty()) {
            invalidPointsAll.put(clientId, new LinkedList<>(Collections.emptyList()));
        }
        for (Point2D point : points) {
            invalidPointsAll.putIfAbsent(clientId, new LinkedList<>(Collections.emptyList()));
            LinkedList<Point2D> temp = new LinkedList<>(this.invalidPointsAll.get(clientId));
            temp.add(point);
            invalidPointsAll.replace(clientId, temp);
        }
    }

    public void increaseRoundCounter() {
        this.roundCounter++;
        System.out.println();
        logger.info(MARKER.Ai, "------------------------------Round {}------------------------------", this.roundCounter);
        System.out.println();
    }


    /**
     * Updating the game values if they need to be updated.
     */
    private void updateValues() {

        if (update == null) {
            this.setHits(new ArrayList<>());
            this.setSunk(new ArrayList<>());
        } else {
            if (this.getUpdate().getHits().isEmpty()) {
                logger.debug(MARKER.Ai, "No new hits.");
            } else {
                logger.debug(MARKER.Ai, "New hits are:");
                for (Shot s : this.getUpdate().getHits()) {
                    logger.debug(MARKER.Ai, s);
                }
            }
            this.setHits(this.getUpdate().getHits());

            if (this.getUpdate().getSunk().isEmpty()) {
                logger.debug(MARKER.Ai, "No new sunks.");
            } else {
                logger.debug(MARKER.Ai, "New sunks are: ");
                for (Shot s : this.getUpdate().getSunk()) {
                    logger.debug(MARKER.Ai, s);
                }
            }
            this.setSunk(this.getUpdate().getSunk());
        }

        MissesFinder missesFinder = new MissesFinder(this);

        Collection<Shot> missesLastRound = missesFinder.computeMissesAll();

        this.setMisses(missesLastRound);

        //sort the points of sunk ships by their client using a SunkenShipsHandler
        SunkenShipsHandler sunkenShipsHandler = new SunkenShipsHandler(this);
        this.setSortedSunk(sunkenShipsHandler.createSortedSunk());
    }

    //Message listening-------------------------------------------------------------------------


    @Override
    public void onServerJoinResponse(ServerJoinResponse message, int clientId) {
        logger.info(MARKER.Ai, "ServerJoinResponse, AiClientId is: {}", message.getClientId());
        this.setAiClientId(message.getClientId());
        try {
            this.tcpConnector.sendMessageToServer(RequestBuilder.lobbyRequest());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLobbyResponse(LobbyResponse message, int clientId) {
        logger.info(MARKER.Ai, "------------------------------LobbyResponse------------------------------");
    }

    @Override
    public void onGameJoinPlayerResponse(GameJoinPlayerResponse message, int clientId) {
        logger.info(MARKER.Ai, "GameJoinPlayerResponse: joined game with iId: {}", message.getGameId());
        this.setGameId(message.getGameId());
    }

    @Override
    public void onGameInitNotification(GameInitNotification message, int clientId) {
        logger.info(MARKER.Ai, "GameInitNotification: got clients and configuration");
        logger.info(MARKER.Ai, "Connected clients size: {}", message.getClientList().size());
        logger.info(MARKER.Ai, "Connected clients are: ");
        for (Client c : message.getClientList()) {
            logger.info(MARKER.Ai, "Client name {}, client id {}", c.getName(), c.getId());
        }
        logger.debug(MARKER.Ai, "Own id is {}, selected difficulty level is {}.", getAiClientId(), getDifficultyLevel());
        setConfig(message.getConfiguration());
        logger.debug("Size of points which has to be hit for loosing a game: {}", this.sizeOfPointsToHit);
        this.setClientArrayList(message.getClientList());
        try {
            logger.info(MARKER.Ai, "Trying to place ships");
            placeShips();
        } catch (IOException e) {
            logger.error(MARKER.Ai, "Ship placement failed. Time was not enough or ships do not fit the field size.");
            e.printStackTrace();
        }
    }

    @Override
    public void onGameStartNotification(GameStartNotification message, int clientId) {
        logger.info(MARKER.Ai, "------------------------------GameStartNotification------------------------------");
        increaseRoundCounter();
        updateValues();
        try {
            this.placeShots(this.getDifficultyLevel());
        } catch (IOException e) {
            logger.error(MARKER.Ai, "Shot placement failed");
            e.printStackTrace();
        }
    }

    boolean isFirstCall = true;

    @Override
    public void onPlayerUpdateNotification(PlayerUpdateNotification message, int clientId) {
        setUpdate(message);
        if (!isFirstCall) {
            increaseRoundCounter();
        }
        logger.info(MARKER.Ai, "Size all requested shots until now: {}", getRequestedShots().size());
        isFirstCall = false;
        logger.debug(MARKER.Ai, "------------------------------PlayerUpdateNotification------------------------------");
    }

    @Override
    public void onRoundStartNotification(RoundStartNotification message, int clientId) {
        logger.info(MARKER.Ai, "------------------------------RoundStartNotification------------------------------");
        logger.info(MARKER.Ai, "A player has to be hit {} times until he has lost.", this.sizeOfPointsToHit);
        logger.info(MARKER.Ai, "Own id is: {}", this.getAiClientId());
        updateValues();
        try {
            this.placeShots(this.getDifficultyLevel());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onFinishNotification(FinishNotification message, int clientId) {
        logger.info(MARKER.Ai, "------------------------------FinishNotification------------------------------");
        updateValues();
        if (this.getDifficultyLevel() == 3) {
            logger.info(MARKER.Ai, "Calculate heatmap in finished state:");
            HeatmapCreator heatmapCreator = new HeatmapCreator(this);
            this.setHeatmapAllClients(heatmapCreator.createHeatmapAllClients());
        }
        System.out.println("Points: ");
        for (Map.Entry<Integer, Integer> entry : message.getPoints().entrySet()) {
            System.out.println(entry);
        }
        System.out.println("Winner: ");
        for (int i : message.getWinner()) {
            System.out.println(i);
        }
        AiMain.close();
    }

    @Override
    public void onConnectionClosedReport(ConnectionClosedReport message, int clientId) {
        logger.info(MARKER.Ai, "------------------------------ConnectionClosedReport------------------------------");
        AiMain.close();
    }

    @Override
    public void onBattleshipException(BattleshipException error, int clientId) {
        logger.error(MARKER.Ai, "BattleshipException");
    }

    @Override
    public void onErrorNotification(ErrorNotification message, int clientId) {
        logger.error(MARKER.Ai, "Received an ErrorNotification with type {} in Message {}. Reason: {} ", message.getErrorType(), message.getReferenceMessageId(),
                message.getReason());
    }


    @Override
    public void onLeaveNotification(LeaveNotification message, int clientId) {
        logger.info(MARKER.Ai, "LeaveNotification: left player id is: " + message.getPlayerId());
        this.handleLeaveOfPlayer(message.getPlayerId());
    }

    @Override
    public void onPauseNotification(PauseNotification message, int clientId) {
        logger.info(MARKER.Ai, "PauseNotification");
    }

    PlayerUpdateNotification update;


    @Override
    public void onTournamentFinishNotification(TournamentFinishNotification message, int clientId) {
        logger.info(MARKER.Ai, "TournamentFinishNotification: ");
    }


    //getter and setter -----------------------------------------------------------------------------------------------


    /**
     * Only for converting the Client Collection of the {@link GameInitNotification} into a LinkedList to have more
     * functions.
     * Called by {@link #onGameInitNotification(GameInitNotification, int)}
     *
     * @param clientList of the configuration
     */
    public void setClientArrayList(Collection<Client> clientList) {
        clientArrayList.addAll(clientList);
    }

    public LinkedList<Client> getClientArrayList() {
        return this.clientArrayList;
    }

    public void setSortedSunk(HashMap<Integer, LinkedList<Point2D>> sortedSunk) {
        this.sortedSunk = sortedSunk;
    }

    public Map<Integer, LinkedList<Point2D>> getSortedSunk() {
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
        this.hits.addAll(hits);
        //logger.debug("Size all Hits: {}", this.hits.size());

    }

    public Collection<Shot> getHits() {
        return this.hits;
    }

    public void setSunk(Collection<Shot> sunk) {
        this.sunk.addAll(sunk);
        //logger.debug("Size all Sunk: {}", this.sunk.size());
    }

    public Collection<Shot> getSunk() {
        return this.sunk;
    }

    public Map<Integer, LinkedList<Point2D>> getInvalidPointsAll() {
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
        logger.info(MARKER.Ai, "Setting configuration...");

        this.setMaxPlayerCount(config.getMaxPlayerCount());
        logger.info(MARKER.Ai, "Maximum player count: {}", getMaxPlayerCount());
        this.setHeight(config.getHeight());
        logger.info(MARKER.Ai, "Height: {}", getHeight());
        this.setWidth(config.getWidth());
        logger.info(MARKER.Ai, "Width: {}", getWidth());
        this.setShotCount(config.getShotCount());
        logger.info(MARKER.Ai, "Shots per round: {}", getShotCount());
        this.setHitPoints(config.getHitPoints());
        this.setSunkPoints(config.getSunkPoints());
        this.setRoundTimer(config.getRoundTime());
        logger.info(MARKER.Ai, "RoundTime: {}", getRoundTime());
        this.setVisualizationTime(config.getVisualizationTime());
        logger.info(MARKER.Ai, "Visualization time: {}", getVisualizationTime());
        this.setPenaltyMinusPoints(config.getPenaltyMinusPoints());
        this.setPenaltyType(config.getPenaltyKind());
        this.setShips(config.getShips());

        logger.info(MARKER.Ai, "Number of ships: {}", getShips().size());

        for (Map.Entry<Integer, ShipType> entry : getShips().entrySet()) {
            sizeOfPointsToHit = sizeOfPointsToHit + entry.getValue().getPositions().size();
        }

        logger.info(MARKER.Ai, "Setting configuration successful.");

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

    public void setAllHeatVal(LinkedList<Triple<Integer, Point2D, Double>> allHeatVal) {
        this.allHeatVal = allHeatVal;
    }

    public LinkedList<Triple<Integer, Point2D, Double>> getAllHeatVal() {
        return this.allHeatVal;
    }

    public void setUpdate(PlayerUpdateNotification message) {
        this.update = message;
    }

    public PlayerUpdateNotification getUpdate() {
        return this.update;
    }

    public Collection<Shot> getRequestedShots() {
        return this.requestedShots;
    }
}