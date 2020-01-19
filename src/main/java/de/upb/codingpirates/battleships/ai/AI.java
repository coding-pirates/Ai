package de.upb.codingpirates.battleships.ai;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.ai.gameplay.ShipPlacer;
import de.upb.codingpirates.battleships.ai.gameplay.ShotPlacer;
import de.upb.codingpirates.battleships.ai.logger.Markers;
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
import de.upb.codingpirates.battleships.network.message.request.ServerJoinRequest;
import de.upb.codingpirates.battleships.network.message.response.GameJoinPlayerResponse;
import de.upb.codingpirates.battleships.network.message.response.LobbyResponse;
import de.upb.codingpirates.battleships.network.message.response.ServerJoinResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * The model of the ai player. Stores the game configuration and values, handles the message sending,
 * the ship and shot placement.
 * <p>
 * Implements also the message listener interfaces and therefore also the message handling functionality.
 *
 * @author Benjamin Kasten
 */
public class AI implements AutoCloseable,

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

    public List<Triple<Integer, Point2D, Double>> allHeatVal;

    LinkedList<Client> clientArrayList = new LinkedList<>();
    Collection<Shot> hits = new ArrayList<>();

    int aiClientId;
    int gameId;

    int roundCounter = 0;

    Map<Integer, PlacementInfo> positions; //ship positions of Ais field

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

    private Configuration configuration;

    private final ClientConnector tcpConnector = ClientApplication.create(new ClientModule<>(ClientConnector.class));

    @Nonnull
    private final String name;

    private final int difficultyLevel;

    private static final Logger logger = LogManager.getLogger();

    /**
     * Constructor which is needed for register this ai instance as message listener.
     */
    public AI(@Nonnull final String name, final int difficultyLevel) {
        this.name = name;
        this.difficultyLevel = difficultyLevel;

        ListenerHandler.registerListener(this);
    }

    static Timer timer = new Timer();
    public static void main(@Nonnull final String[] args) throws IOException {

        if (args.length != 4) {
            System.err.println("host ip difficultyLevel aiName");
            System.exit(1);
        }
        final AI ai = new AI(args[3], Integer.parseInt(args[2]));

        ai.connect(args[0], Integer.parseInt(args[1]));
    }

    @Override
    public void close() throws IOException {
        tcpConnector.disconnect();
    }

    public void connect(@Nonnull final String host, final int port) throws IOException {
        tcpConnector.connect(host, port);

        sendMessage(new ServerJoinRequest(name, ClientType.PLAYER));
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
     * @throws IOException Server connection error
     */
    public void placeShots() throws IOException {
        ShotPlacer shotPlacement = new ShotPlacer(this);

        Collection<Shot> myShots = Collections.emptyList();
        switch (difficultyLevel) {
            case 1:
                logger.info(Markers.Ai, "Difficulty Level 1 (Random) selected");
                myShots = shotPlacement.placeShots_1(this.getConfiguration().getShotCount());
                break;
            case 2:
                logger.info(Markers.Ai, "Difficulty Level 2 (Hunt & Target) selected");
                myShots = shotPlacement.placeShots_2();
                break;
            case 3:
                logger.info(Markers.Ai, "Difficulty level 3 (HeatMap) selected");
                myShots = shotPlacement.placeShots_Relative_3();
                break;
            default:
                logger.error(Markers.Ai, "The difficulty level ({}) is not valid, run again and use choose " +
                        "between the level 1, 2 or 3", difficultyLevel);
                close();
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
        logger.info(Markers.Ai, "------------------------------Round {}------------------------------", this.roundCounter);
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
                logger.debug(Markers.Ai, "No new hits.");
            } else {
                logger.debug(Markers.Ai, "New hits are:");
                for (Shot s : this.getUpdate().getHits()) {
                    logger.debug(Markers.Ai, s);
                }
            }
            this.setHits(this.getUpdate().getHits());

            if (this.getUpdate().getSunk().isEmpty()) {
                logger.debug(Markers.Ai, "No new sunks.");
            } else {
                logger.debug(Markers.Ai, "New sunks are: ");
                for (Shot s : this.getUpdate().getSunk()) {
                    logger.debug(Markers.Ai, s);
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

    // <editor-fold desc="Listeners">
    @Override
    public void onServerJoinResponse(ServerJoinResponse message, int clientId) {
        logger.info(Markers.Ai, "ServerJoinResponse, AiClientId is: {}", message.getClientId());
        this.setAiClientId(message.getClientId());
        try {
            this.tcpConnector.sendMessageToServer(RequestBuilder.lobbyRequest());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLobbyResponse(LobbyResponse message, int clientId) {
        logger.info(Markers.Ai, "------------------------------LobbyResponse------------------------------");
    }

    @Override
    public void onGameJoinPlayerResponse(GameJoinPlayerResponse message, int clientId) {
        logger.info(Markers.Ai, "GameJoinPlayerResponse: joined game with iId: {}", message.getGameId());
        this.setGameId(message.getGameId());
    }

    @Override
    public void onGameInitNotification(GameInitNotification message, int clientId) {
        logger.info(Markers.Ai, "GameInitNotification: got clients and configuration");
        logger.info(Markers.Ai, "Connected clients size: {}", message.getClientList().size());
        logger.info(Markers.Ai, "Connected clients are: ");
        for (Client c : message.getClientList()) {
            logger.info(Markers.Ai, "Client name {}, client id {}", c.getName(), c.getId());
        }
        logger.debug(Markers.Ai, "Own id is {}, selected difficulty level is {}.", getAiClientId(), getDifficultyLevel());
        configuration = message.getConfiguration();
        for (Map.Entry<Integer, ShipType> entry : configuration.getShips().entrySet()) {
            sizeOfPointsToHit = sizeOfPointsToHit + entry.getValue().getPositions().size();
        }
        logger.debug("Size of points which has to be hit for loosing a game: {}", this.sizeOfPointsToHit);
        this.setClientArrayList(message.getClientList());
        try {
            logger.info(Markers.Ai, "Trying to place ships");
            placeShips();
        } catch (IOException e) {
            logger.error(Markers.Ai, "Ship placement failed. Time was not enough or ships do not fit the field size.");
            e.printStackTrace();
        }
    }

    @Override
    public void onGameStartNotification(GameStartNotification message, int clientId) {
        logger.info(Markers.Ai, "------------------------------GameStartNotification------------------------------");
        increaseRoundCounter();
        updateValues();
        try {
            //logger.info("Trying to place shots");
            placeShots();
        } catch (IOException e) {
            logger.error(Markers.Ai, "Shot placement failed");
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
        logger.info(Markers.Ai, "Size all requested shots until now: {}", getRequestedShots().size());
        isFirstCall = false;
        logger.debug(Markers.Ai, "------------------------------PlayerUpdateNotification------------------------------");
    }

    @Override
    public void onRoundStartNotification(RoundStartNotification message, int clientId) {
        logger.info(Markers.Ai, "------------------------------RoundStartNotification------------------------------");
        logger.info(Markers.Ai, "A player has to be hit {} times until he has lost.", this.sizeOfPointsToHit);
        logger.info(Markers.Ai, "Own id is: {}", this.getAiClientId());
        updateValues();
        try {
            placeShots();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onFinishNotification(FinishNotification message, int clientId) {
        logger.info(Markers.Ai, "------------------------------FinishNotification------------------------------");
        updateValues();
        if (this.getDifficultyLevel() == 3) {
            logger.info(Markers.Ai, "Calculate heatmap in finished state:");
            HeatmapCreator heatmapCreator = new HeatmapCreator(this);
            this.setHeatMapAllClients(heatmapCreator.createHeatmapAllClients());
        }
        System.out.printf("Own Id: %d%n", this.getAiClientId());
        System.out.println("Points: ");
        for (Map.Entry<Integer, Integer> entry : message.getPoints().entrySet()) {
            System.out.println(entry);
        }
        System.out.println("Winner: ");
        for (int i : message.getWinner()) {
            System.out.println(i);
        }

        try {
            close();
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void onConnectionClosedReport(ConnectionClosedReport message, int clientId) {
        logger.info(Markers.Ai, "------------------------------ConnectionClosedReport------------------------------");

        try {
            close();
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void onBattleshipException(BattleshipException error, int clientId) {
        logger.error(Markers.Ai, "BattleshipException");
    }

    @Override
    public void onErrorNotification(ErrorNotification message, int clientId) {
        logger.error(Markers.Ai, "Received an ErrorNotification with type {} in Message {}. Reason: {} ", message.getErrorType(), message.getReferenceMessageId(),
                message.getReason());
    }


    @Override
    public void onLeaveNotification(LeaveNotification message, int clientId) {
        logger.info(Markers.Ai, "LeaveNotification: left player id is: " + message.getPlayerId());
        this.handleLeaveOfPlayer(message.getPlayerId());
    }

    @Override
    public void onPauseNotification(PauseNotification message, int clientId) {
        logger.info(Markers.Ai, "PauseNotification");
    }

    PlayerUpdateNotification update;


    @Override
    public void onTournamentFinishNotification(TournamentFinishNotification message, int clientId) {
        logger.info(Markers.Ai, "TournamentFinishNotification: ");
    }
    // </editor-fold>

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

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getGameId() {
        return this.gameId;
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

    public void setHeatMapAllClients(Map<Integer, Double[][]> heatMap) {
        this.heatmapAllClients = heatMap;
    }

    public Map<Integer, Double[][]> getHeatMapAllClients() {
        return this.heatmapAllClients;
    }

    public void setSunkenShipIdsAll(Map<Integer, LinkedList<Integer>> sunkenIds) {
        this.allSunkenShipIds = sunkenIds;
    }

    public Map<Integer, LinkedList<Integer>> getAllSunkenShipIds() {
        return this.allSunkenShipIds;
    }

    public int getDifficultyLevel() {
        return this.difficultyLevel;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @VisibleForTesting
    void setConfiguration(@Nonnull final Configuration configuration) {
        this.configuration = configuration;
    }

    public void setAllHeatVal(LinkedList<Triple<Integer, Point2D, Double>> allHeatVal) {
        this.allHeatVal = allHeatVal;
    }

    public List<Triple<Integer, Point2D, Double>> getAllHeatVal() {
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