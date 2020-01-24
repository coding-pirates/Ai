package de.upb.codingpirates.battleships.ai;

import com.google.common.annotations.VisibleForTesting;
import de.upb.codingpirates.battleships.ai.gameplay.ShipPlacer;
import de.upb.codingpirates.battleships.ai.gameplay.ShotPlacementStrategy;
import de.upb.codingpirates.battleships.ai.gameplay.StandardShotPlacementStrategy;
import de.upb.codingpirates.battleships.ai.logger.Markers;
import de.upb.codingpirates.battleships.ai.util.HeatMapCreator;
import de.upb.codingpirates.battleships.ai.util.MissesFinder;
import de.upb.codingpirates.battleships.ai.util.SunkenShipsHandler;
import de.upb.codingpirates.battleships.ai.util.Triple;
import de.upb.codingpirates.battleships.client.ListenerHandler;
import de.upb.codingpirates.battleships.client.listener.*;
import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
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

import javax.annotation.Nonnull;
import java.util.*;

/**
 * The model of the ai player. Stores the game configuration and values, handles the message sending,
 * the ship and shot placement.
 * <p>
 * Implements the message listener interfaces and therefore also the message handling functionality.
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

    Map<Integer, Integer> points = new HashMap<>(); //points of the clients

    Collection<Shot> sunk = new ArrayList<>(); //sunks which are updated every round
    Map<Integer, List<Point2D>> sortedSunk = new HashMap<>(); //sunks sorted by their clients
    Map<Integer, List<Integer>> allSunkenShipIds = new HashMap<>(); //sunk ship ids by their clients

    Map<Integer, double[][]> heatMapAllClients = new HashMap<>();

    Map<Integer, List<Point2D>> invalidPointsAll = new HashMap<>(); //invalid points per client id

    Collection<Shot> misses = new ArrayList<>(); //all misses of this player

    public Collection<Shot> requestedShotsLastRound = new ArrayList<>(); //the latest requested shots
    public Collection<Shot> requestedShots = new ArrayList<>(); //all requested shots

    private Configuration configuration;

    private final ClientConnector tcpConnector = ClientApplication.create();

    @Nonnull
    private final String name;

    private final ShotPlacementStrategy shotPlacementStrategy;

    private static final Logger logger = LogManager.getLogger();

    public AI(@Nonnull final String name, final ShotPlacementStrategy shotPlacementStrategy) {
        this.name                  = name;
        this.shotPlacementStrategy = shotPlacementStrategy;

        ListenerHandler.registerListener(this);
    }

    static Timer timer = new Timer();

    public static void main(@Nonnull final String[] args) {
        //timer is not necessary when implementing the AutoClosable interface
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            }
        }, 1L, 1L);

        String name;
        String host;
        String port;
        int difficulty;


        //interactive scanner is disabled (Endabgabe)
        if (args.length == 0) {
            //use this default config for Ai if no arguments were passed
            name = "EP" + (Math.random() * 100000);
            host = "localhost";
            port = "33100";
            difficulty = 3;
            /*
            System.out.println("Give this engine player a name: ");
            while (true) {

                name = new Scanner(System.in).next();
                if (name == null) {
                    System.err.println("The name has to be a word and cannot start with a number. Try again...");
                    continue;
                }
                break;
            }

            System.out.println("The host (ip address or domain) to connect to: ");
            while (true) {
                host = new Scanner(System.in).next();
                if (host == null) {
                    System.err.println("You need a host to connect this engine player. Try again...");
                    continue;
                }
                break;
            }

            System.out.println("Give me the port of the server: ");
            while (true) {
                port = new Scanner(System.in).next();
                if (port == null) {
                    System.err.println("Give me a valid port please. Try again...");
                    continue;
                }
                try {
                    Integer.parseInt(port);
                } catch (Exception e) {
                    System.err.println("Please type in a valid port. It has to be a number. Try again...");
                    continue;
                }
                break;
            }

            System.out.println("Choose between the difficulty levels 1, 2 or 3, or type help for information about the difficulty level.");
            while (true) {
                String input = new Scanner(System.in).next();
                if (input.equals("help")) {
                    System.out.print("Explanations of the difficulty level:\n" +
                            "1: Random: Shots will be fired randomly.\n" +
                            "2: Hunt & Target: The main goal is to sink ships. Based on the hits, the algorithm tries to hit all surrounding fields\n" +
                            "until the ship is sunk. If there are no hits anymore, shots will be fired randomly. Use this if points for a sunken ship are high.\n" +
                            "3: Heatmap: Shots will be fired on the field with the highest probability. Choose this algorithm if sunk points are low\n" +
                            "or you have only one opponent.\n");
                    continue;
                }
                switch (input) {
                    case "1": {
                        difficulty = 1;
                        break;
                    }
                    case "2": {
                        difficulty = 2;
                        break;
                    }
                    case "3": {
                        difficulty = 3;
                        break;
                    }
                    default:
                        System.err.println("Please type in a valid level or type help for getting more information. Try again...");
                        continue;
                }
                break;
            }


             */
        } else {
            //if arguments were passed, use them
            if (args.length != 4) {
                System.err.println("Use this order: host port difficultyLevel name");
                System.exit(1);
            }
            name = args[3];
            difficulty = Integer.parseInt(args[2]);
            host = args[0];
            port = args[1];

        }
        final ShotPlacementStrategy shotPlacementStrategy =
            ShotPlacementStrategy
                .fromDifficultyLevel(difficulty)
                .orElseThrow(
                    () -> new RuntimeException(String.format(Locale.ROOT, "Invalid difficultyLevel: '%d'", difficulty)));
        final AI ai = new AI(name, shotPlacementStrategy);

        ai.connect(host, Integer.parseInt(port));

    }

    /**
     * Disconnects this ai instance from the server.
     */
    @Override
    public void close() {
        tcpConnector.disconnect();
    }

    /**
     * Connects the ai instance with the server.
     *
     * @param host the ip address or domain of the server.
     * @param port the server port.
     */
    public void connect(@Nonnull final String host, final int port) {
        tcpConnector.connect(host, port, ()->sendMessage(RequestBuilder.serverJoinRequest(name, ClientType.PLAYER)),null);
    }

    /**
     * Is called every round for placing shots. Using a object and the difficulty level,
     * the method calls the matching method for shot placement and sends the result (the
     * calculated shots) to the server using the {@link #sendMessage} method.
     * <p>
     * Difficulty level sets shot placement algorithm:
     * case 1: random
     * case 2: hunt and target
     * case 3: (extended) heatmap
     */
    public void placeShots() {
        final Collection<Shot> shots = shotPlacementStrategy.calculateShots(this, getConfiguration().getShotCount());

        logger.info("Calculated the following shots: {}", shots);
        sendMessage(RequestBuilder.shotsRequest(shots));

    }

    /**
     * Creates a {@link ShipPlacer} instance and calls {@link ShipPlacer#guessRandomShipPositions(Map)} method.
     * Sets the placement by calling {@link #setPositions(Map)} and calls {@link #sendMessage(Message)} (Message)} for
     * sending the {@link PlaceShipsRequest} to the server.
     */
    public void placeShips() {
        ShipPlacer shipPlacer = new ShipPlacer(this);

        setPositions(shipPlacer.placeShipsRandomly());

        sendMessage(RequestBuilder.placeShipsRequest(getPositions()));
    }

    /**
     * Calculates the misses of last round and adds them to the misses collection
     *
     * @deprecated misses can be added manually
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
     */
    public void sendMessage(Message message) {
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
        logger.info(Markers.AI, "------------------------------Round {}------------------------------", this.roundCounter);
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
                logger.debug(Markers.AI, "No new hits.");
            } else {
                logger.debug(Markers.AI, "New hits are:");
                for (Shot s : this.getUpdate().getHits()) {
                    logger.debug(Markers.AI, s);
                }
            }
            this.setHits(this.getUpdate().getHits());

            if (this.getUpdate().getSunk().isEmpty()) {
                logger.debug(Markers.AI, "No new sunks.");
            } else {
                logger.debug(Markers.AI, "New sunks are: ");
                for (Shot s : this.getUpdate().getSunk()) {
                    logger.debug(Markers.AI, s);
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
        logger.info(Markers.AI, "ServerJoinResponse, AiClientId is: {}", message.getClientId());
        this.setAiClientId(message.getClientId());
        this.tcpConnector.sendMessageToServer(RequestBuilder.lobbyRequest());
    }

    @Override
    public void onLobbyResponse(LobbyResponse message, int clientId) {
        logger.info(Markers.AI, "------------------------------LobbyResponse------------------------------");
    }

    @Override
    public void onGameJoinPlayerResponse(GameJoinPlayerResponse message, int clientId) {
        logger.info(Markers.AI, "GameJoinPlayerResponse: joined game with iId: {}", message.getGameId());
        this.setGameId(message.getGameId());
    }

    @Override
    public void onGameInitNotification(GameInitNotification message, int clientId) {
        logger.info(Markers.AI, "GameInitNotification: got clients and configuration");
        logger.info(Markers.AI, "Connected clients size: {}", message.getClientList().size());
        logger.info(Markers.AI, "Connected clients are: ");
        for (Client c : message.getClientList()) {
            logger.info(Markers.AI, "Client name {}, client id {}", c.getName(), c.getId());
        }
        logger.debug(Markers.AI, "Own id is {}, selected ShotPlacementStrategy is {}.", getAiClientId(), shotPlacementStrategy.getName());
        configuration = message.getConfiguration();
        for (Map.Entry<Integer, ShipType> entry : configuration.getShips().entrySet()) {
            sizeOfPointsToHit = sizeOfPointsToHit + entry.getValue().getPositions().size();
        }
        logger.debug("Size of points which has to be hit for loosing a game: {}", this.sizeOfPointsToHit);
        this.setClientArrayList(message.getClientList());

        logger.info(Markers.AI, "Trying to place ships");
        placeShips();
    }

    @Override
    public void onGameStartNotification(GameStartNotification message, int clientId) {
        logger.info(Markers.AI, "------------------------------GameStartNotification------------------------------");
        increaseRoundCounter();
        updateValues();

        logger.debug("Placing shots with ShotPlacementStrategy '{}'.", shotPlacementStrategy.getName());
        placeShots();
    }

    boolean isFirstCall = true;

    @Override
    public void onPlayerUpdateNotification(PlayerUpdateNotification message, int clientId) {
        setUpdate(message);
        if (!isFirstCall) {
            increaseRoundCounter();
        }
        logger.info(Markers.AI, "Size all requested shots until now: {}", getRequestedShots().size());
        isFirstCall = false;
        logger.debug(Markers.AI, "------------------------------PlayerUpdateNotification------------------------------");
    }

    @Override
    public void onRoundStartNotification(RoundStartNotification message, int clientId) {
        logger.info(Markers.AI, "------------------------------RoundStartNotification------------------------------");
        logger.info(Markers.AI, "A player has to be hit {} times until he has lost.", this.sizeOfPointsToHit);
        logger.info(Markers.AI, "Own id is: {}", this.getAiClientId());
        updateValues();

        placeShots();
    }

    @Override
    public void onFinishNotification(FinishNotification message, int clientId) {
        logger.info(Markers.AI, "------------------------------FinishNotification------------------------------");
        updateValues();
        if (shotPlacementStrategy == StandardShotPlacementStrategy.HEAT_MAP) {
            logger.info(Markers.AI, "Calculate heatmap in finished state:");
            HeatMapCreator heatmapCreator = new HeatMapCreator(this);
            this.setHeatMapAllClients(heatmapCreator.createHeatMapAllClients());
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

        close();
    }

    @Override
    public void onConnectionClosedReport(ConnectionClosedReport message, int clientId) {
        logger.info(Markers.AI, "------------------------------ConnectionClosedReport------------------------------");

        close();
    }

    @Override
    public void onBattleshipException(BattleshipException error, int clientId) {
        logger.error(Markers.AI, "BattleshipException");
    }

    @Override
    public void onErrorNotification(ErrorNotification message, int clientId) {
        logger.error(Markers.AI, "Received an ErrorNotification with type {} in Message {}. Reason: {} ", message.getErrorType(), message.getReferenceMessageId(),
                message.getReason());
    }


    @Override
    public void onLeaveNotification(LeaveNotification message, int clientId) {
        logger.info(Markers.AI, "LeaveNotification: left player id is: " + message.getPlayerId());
        this.handleLeaveOfPlayer(message.getPlayerId());
    }

    @Override
    public void onPauseNotification(PauseNotification message, int clientId) {
        logger.info(Markers.AI, "PauseNotification");
    }

    PlayerUpdateNotification update;


    @Override
    public void onTournamentFinishNotification(TournamentFinishNotification message, int clientId) {
        logger.info(Markers.AI, "TournamentFinishNotification: ");
    }
    // </editor-fold>

    /**
     * Converts the clientList directly in an arrayList with more functions.
     *
     * @param clientList of the configuration
     */
    public void setClientArrayList(Collection<Client> clientList) {
        clientArrayList.addAll(clientList);
    }

    public LinkedList<Client> getClientArrayList() {
        return this.clientArrayList;
    }

    public void setSortedSunk(Map<Integer, List<Point2D>> sortedSunk) {
        this.sortedSunk = sortedSunk;
    }

    public Map<Integer, List<Point2D>> getSortedSunk() {
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

    /**
     * Not a usual setter. Adds new hits to all hits.
     *
     * @param hits the new hits each round
     */
    public void setHits(Collection<Shot> hits) {
        this.hits.addAll(hits);
    }

    public Collection<Shot> getHits() {
        return this.hits;
    }

    public void setSunk(Collection<Shot> sunk) {
        this.sunk.addAll(sunk);
    }

    public Collection<Shot> getSunk() {
        return this.sunk;
    }

    public Map<Integer, List<Point2D>> getInvalidPointsAll() {
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

    public void setHeatMapAllClients(Map<Integer, double[][]> heatMap) {
        this.heatMapAllClients = heatMap;
    }

    public Map<Integer, double[][]> getHeatMapAllClients() {
        return this.heatMapAllClients;
    }

    public void setSunkenShipIdsAll(Map<Integer, List<Integer>> sunkenIds) {
        this.allSunkenShipIds = sunkenIds;
    }

    public Map<Integer, List<Integer>> getAllSunkenShipIds() {
        return this.allSunkenShipIds;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @VisibleForTesting
    void setConfiguration(@Nonnull final Configuration configuration) {
        this.configuration = configuration;
    }

    public void setAllHeatVal(List<Triple<Integer, Point2D, Double>> allHeatVal) {
        this.allHeatVal = allHeatVal;
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

    public int getSizeOfPointsToHit() {
        return this.sizeOfPointsToHit;
    }
}