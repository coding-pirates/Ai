package de.upb.codingpirates.battleships.ai;

import GamePlay.ShipPlacer;
import GamePlay.ShotPlacer;
import GamePlay.SunkenShipFinder;
import de.upb.codingpirates.battleship.ai.util.RotationMatrix;
import de.upb.codingpirates.battleship.ai.util.ZeroPointMover;
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
    private static final Logger logger = LogManager.getLogger(Ai.class.getName());


    //GameState gameState;
    String host; //ip address
    int port;

    //convert the Collection clintList in a LinkedList for handling random shots is done in the
    Collection<Client> clientList;
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
    Map<Integer, ShipType> shipConfig = new HashMap<>();

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


    public Ai() {
        ListenerHandler.registerListener(this);
    }


    /**
     * Is called by the {@link #onGameInitNotification(GameInitNotification, int)}
     * for setting giving the ai access to the game configuration.
     *
     * @param config configuration from {@link GameInitNotification}
     */
    public void setConfig(Configuration config) {
        setHeight(config.getHeight());
        setWidth(config.getWidth());
        setShipConfig(config.getShips());
        setHitpoints(config.getHitPoints());
        setSunkPoints(config.getSunkPoints());
        setRoundTimer(config.getRoundTime());
        setVisualizationTime(config.getVisualizationTime());
        setShotCount(config.getShotCount());

    }

    ShotPlacer shotPlacement = new ShotPlacer(this);

    public void placeShots(int difficultyLevel) throws IOException {
        Collection<Shot> myShots;

        switch (difficultyLevel) {
            case 1: {
                logger.info("Difficulty Level 1 selected");
                myShots = shotPlacement.placeShots_1();
                sendMessage(new ShotsRequest(myShots));
                break;
            }
            case 2: {
                logger.info("Difficulty Level 2 selected");
                myShots = shotPlacement.placeShots_2();
                sendMessage(new ShotsRequest(myShots));
                break;
            }
            case 3: {
                logger.info("Difficulty level 3 selected");
                myShots = shotPlacement.placeShots_3();
                sendMessage(new ShotsRequest(myShots));
                break;
            }
            default: {
                logger.error("Input is not valid: " + difficultyLevel);
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
     * Adds the surrounding points of one points collection to the usedPoints based on the rules for the game:
     * each ship must have a minimal distance of one point in each direction to other ships.
     * Used by {@link ShipPlacer#guessRandomShipPositions(Map)}
     *
     * @param shipPos The positions of one ship
     */
    public LinkedHashSet<Point2D> addSurroundingPointsToUsedPoints(ArrayList<Point2D> shipPos) {
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


    //todo add to RotationMatrix Class

    /**
     * Creates a collection of collections of all possible ship rotations
     *
     * @param ships Collection of points which represents a ship
     * @return allPossibleTurns ArrayList of arraylists for each possible rotation
     */
    public ArrayList<ArrayList<Point2D>> rotateShips(ArrayList<Point2D> ships) {
        RotationMatrix rotate = new RotationMatrix();
        ZeroPointMover mover = new ZeroPointMover();
        ArrayList<ArrayList<Point2D>> allPossibleTurns = new ArrayList<>();
        //no turn
        allPossibleTurns.add(mover.moveToZeroPoint(ships));
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

    public void setShipConfig(Map<Integer, ShipType> shipConfig) {
        this.shipConfig = shipConfig;
    }

    public Map<Integer, ShipType> getShipConfig() {
        return this.shipConfig;
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
        logger.info("Hitpoints: {}", hitpoints);
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
        logger.info("SunkPoints: {}", sunkPoints);
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


    //Message listening-------------------------------------------------------------------------


    @Override
    public void onConnectionClosedReport(ConnectionClosedReport message, int clientId) {
        logger.info("ConnectionClosedReport");
        AiMain.close();
    }

    @Override
    public void onBattleshipException(BattleshipException error, int clientId) {
        logger.error("BattleshipException");
    }

    @Override
    public void onErrorNotification(ErrorNotification message, int clientId) {
        logger.info("ErrorNotification");
        logger.error("Errortype: " + message.getErrorType());
        logger.error("Error occurred in Message: " + message.getReferenceMessageId());
        logger.error("Reason: " + message.getReason());
    }

    @Override
    public void onFinishNotification(FinishNotification message, int clientId) {
        logger.info("FinishNotification");
        AiMain.close();
    }

    @Override
    public void onGameInitNotification(GameInitNotification message, int clientId) {
        logger.info("GameInitNotification");
        Configuration config = message.getConfiguration();
        AiMain.ai.setConfig(config);
        AiMain.ai.setClientArrayList(message.getClientList());
        try {
            AiMain.ai.placeShips();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGameStartNotification(GameStartNotification message, int clientId) {
        logger.info("GameStartNotification");
        try {
            AiMain.ai.placeShots(AiMain.getDifficultyLevel());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onLeaveNotification(LeaveNotification message, int clientId) {
        logger.info("LeaveNotification, left Player: " + message.getPlayerId());
        int leftPlayerId = message.getPlayerId();
        AiMain.ai.handleLeaveOfPlayer(leftPlayerId);
    }

    @Override
    public void onPauseNotification(PauseNotification message, int clientId) {
        logger.info("PauseNotification");
    }

    @Override
    public void onPlayerUpdateNotification(PlayerUpdateNotification message, int clientId) {
        SunkenShipFinder sunkenShipFinder = new SunkenShipFinder(AiMain.ai.getInstance());
        logger.info("PlayerUpdateNotification");
        AiMain.ai.updateNotification = message;
        AiMain.ai.setHits(message.getHits());
        AiMain.ai.setPoints(message.getPoints());
        AiMain.ai.setSunk(message.getSunk());
        AiMain.ai.setSortedSunk(sunkenShipFinder.sortTheSunk()); //sortiere die sunks nach ihren Clients

    }

    @Override
    public void onRoundStartNotification(RoundStartNotification message, int clientId) {
        logger.info("RoundStartNotification");
        try {
            AiMain.ai.placeShots(AiMain.getDifficultyLevel());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onTournamentFinishNotification(TournamentFinishNotification message, int clientId) {
        logger.info("TournamentFinishNotification");

    }

    @Override
    public void onGameJoinPlayerResponse(GameJoinPlayerResponse message, int clientId) {
        logger.info("GameJoinPlayerResponse, GameId: {}", message.getGameId());
        int gameId = message.getGameId();
        AiMain.ai.setGameId(gameId);
    }

    @Override
    public void onServerJoinResponse(ServerJoinResponse message, int clientId) throws IOException {
        logger.info("ServerJoinResponse");
        this.tcpConnector.sendMessageToServer(new LobbyRequest());
    }

    @Override
    public void onLobbyResponse(LobbyResponse message, int clientId) {
        logger.info("LobbyResponse");
    }
}