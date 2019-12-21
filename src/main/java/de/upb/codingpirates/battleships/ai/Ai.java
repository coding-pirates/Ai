package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleship.ai.helper.RotationMatrix;
import de.upb.codingpirates.battleships.client.network.ClientApplication;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.logic.util.*;
import de.upb.codingpirates.battleships.network.message.notification.GameInitNotification;
import de.upb.codingpirates.battleships.network.message.notification.PlayerUpdateNotification;
import de.upb.codingpirates.battleships.network.message.request.PlaceShipsRequest;
import de.upb.codingpirates.battleships.network.message.request.ShotsRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.jvm.hotspot.oops.ArrayKlass;

import java.io.IOException;
import java.util.*;
//TODO some getter/setter are missing

/**
 * Implements the logic of the Ai Player like placing ships and firing shots
 *
 * @author Benjamin Kasten
 */
public class Ai {
    //Logger
    private static final Logger logger = LogManager.getLogger(Ai.class.getName());

    //GameState gameState;
    ClientConnector connector;
    String host; //ip address
    int port;

    //convert the Collection clintList in a LinkedList for handling random shots is done in the
    Collection<Client> clientList;
    LinkedList<Client> clientArrayList = new LinkedList<>();
    Collection<Shot> hits = new ArrayList<>();

    //PlayerUpdateNotificationInformation
    //PlayerUpdateNotification updateNotification;


    //game field parameter
    int width;
    int height;

    int HITPOINTS;
    int SUNKPOINTS;

    int clientId;
    int gameId;
    Configuration config;
    Map<Integer, ShipType> shipConfig = new HashMap<>();

    long VISUALIZATIONTIME;
    long ROUNDTIME;

    int SHOTCOUNT;

    //updated values
    protected Map<Integer, Integer> points = new HashMap<>();
    protected Collection<Shot> choosenShots = new ArrayList<>();
    protected Collection<Shot> sunk = new ArrayList<>();

    //sunken Ships
    Map<Integer, LinkedList<Shot>> sortedSunk = new HashMap<>(); //
    Map<Integer, LinkedList<Integer>> allSunkenShipIds = new HashMap<>();


    //for testing purpose public
    public PlayerUpdateNotification updateNotification;
    public ShotsRequest shotsRequest;


    /**
     * Is called by the {@link AiMain#main(String[])} for connecting to the server
     *
     * @param host IP Address
     * @param port Port number
     * @throws IOException if something with the connection failed
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
        setHeight(config.HEIGHT);
        setWidth(config.WIDTH);
        setShipConfig(config.getShipTypes());
        setHitpoints(config.HITPOINTS);
        setSunkpoints(config.SUNKPOINTS);
        setRoundTimer(config.ROUNDTIME);
        setVisualizationTime(config.VISUALIZATIONTIME);
        setShotCount(config.SHOTCOUNT);

    }


    //remains false until a
    boolean successful = false;

    //the requested argument for the PlaceShipsRequest
    Map<Integer, PlacementInfo> positions = new HashMap<>();

    /**
     * Calls the {link {@link #randomShipGuesser(Map)}} and if it returns true send the {@link PlaceShipsRequest}
     *
     * @throws IOException
     */
    public void placeShips() throws IOException {
        while (!successful) {
            System.out.println("ps successful false");
            logger.info("placing ships failed");
            randomShipGuesser(getShipConfig());
        }
        logger.info("placing ships worked");
        PlaceShipsRequest placeShipsRequestMessage = new PlaceShipsRequest(getPositions());

        //connector.sendMessageToServer(placeShipsRequestMessage);
    }


    /**
     * contains all the Points which can not be accessed anymore like the distance to a ship or the ship positions;
     * is used for checking if a point can be used for placing a ship
     */
    ArrayList<Point2D> usedPoints = new ArrayList<>();

    /**
     * Is called by placeShips() and places the ships randomly on the field. Leave the loop if the placement is not valid.
     *
     * @param shipConfig map, which maps from the shipId to the ShipType (from the configuration)
     */
    private void randomShipGuesser(Map<Integer, ShipType> shipConfig) {
        //clear all used values from the recent call for safety
        usedPoints.clear();
        usedPoints.clear();
        positions.clear();

        logger.info("started random guesser");

        //for testing purpose
        System.out.println("height: " + getHeight());
        System.out.println("width: " + getWidth());

        //iterate through the the shipConfig Map for getting every key value pair
        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            //a logger
            logger.info("entry of shipConfig Map with shipID: " + entry.getKey());


            //ship Id
            int shipId = entry.getKey();
            //all points of the ship
            Collection<Point2D> shipPos = entry.getValue().getPosition();

            //for testing purpose
            for (Point2D j : shipPos) {
                System.out.print("oldX: " + j.getX());
                System.out.println(" oldY: " + j.getY());

            }


            //is the random point wich will be the point for the PlacementInfo
            Point2D guessPoint = getRandomPoint2D();

            //for testing purpose
            System.out.println("guessX: " + guessPoint.getX());
            System.out.println("guessY: " + guessPoint.getY());

            //the the distance from zeropoint to random guessPoint
            int distanceX = guessPoint.getX();
            int distanceY = guessPoint.getY();


            //the positions (points) of a ship well be stored here
            ArrayList<Point2D> tempShipPos = new ArrayList<>();

            //iterates through every point of the ship (all points in shipPos)
            for (Point2D i : shipPos) {
                //creating new coordinates by moving every point in x and y direction: The moving distance came from
                //the guessPoint
                int newX = i.getX() + distanceX;
                int newY = i.getY() + distanceY;
                //create a new point for the new coordinates
                Point2D newPoint = new Point2D(newX, newY);

                //for testing purpose
                System.out.print("newX: " + newX);
                System.out.println(" newY: " + newY);

                //check for each point in usePoints if the newPoint is already unavailable (is used)
                for (Point2D p : usedPoints) {
                    //if the newPoint is unavailable: delete usedPoints, positions and return
                    //-->starting the loop in placeShips again
                    if ((p.getX() == newPoint.getX()) & (p.getY() == newPoint.getY())) {
                        usedPoints.clear();
                        positions.clear();
                        logger.info("failed: newPoint already in usedPoints ");
                        return;

                    }
                }

                //if the newPoint is not unavailable, check if the coordinates fits to the field:
                // No negative values, no greater values as the fields height and width
                if (newPoint.getY() < 0 | newPoint.getX() < 0 |
                        newPoint.getX() > (width - 1) | newPoint.getY() > (height - 1)) {
                    //if the newPoint is unavailable: delete usedPoints, positions and return
                    //-->starting the loop in placeShips again
                    usedPoints.clear();
                    positions.clear();
                    logger.info("failed: newPoint coordinates do not fit the field ");
                    return;
                } else {
                    // if the newPoint is valid...
                    // ...add the point to the tempShipPos ArrayList
                    tempShipPos.add(newPoint);
                    //...add the point to the usedPoints ArrayList
                    usedPoints.add(newPoint);
                    //create a new PlacementInfo with the guessPoint (the guessPoint is valid)
                    PlacementInfo pInfo = new PlacementInfo(guessPoint, Rotation.NONE);
                    //add the shipId and the pInfo to positions Map
                    positions.put(shipId, pInfo);
                }
            }

            //after placing a ship, we have to add all surrounding points of the ship to the usedPoints Array
            //once they are in the usedPoints Array, they can not be used for placing ships anymore
            //addSurroundingPointsToUsedPoints(tempShipPos);
            usedPoints.addAll(addSurroundingPointsToUsedPoints(tempShipPos));


            //clear the tempShipPos Array for the next loop
            tempShipPos.clear();
        }
        //is called only if the placing of the ships in the positions Map worked for all ships
        //responsible for leaving the while loop in placeShips()
        setSuccessfulPlacement();
    }

    /**
     * Adds the surrounding points of one points collection to the usedPoints based on the rules for the game:
     * each ship must have a minimal distance of one point in each direction to other ships.
     * Used by {@link #randomShipGuesser(Map)}
     *
     * @param shipPos The positions of one ship
     */
    private ArrayList<Point2D> addSurroundingPointsToUsedPoints(ArrayList<Point2D> shipPos) {
        ArrayList<Point2D> temp = new ArrayList<>();
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
        return temp;


    }

    /**
     * Only for converting the Client Collection of the {@link GameInitNotification} into a LinkedList to have more
     * functions.
     * Called by {@link AiMessageHandler#handleGameInitNotification(GameInitNotification, int)}
     *
     * @param clientList from the configuration
     */
    public void setClientArrayList(Collection<Client> clientList) {
        clientArrayList.addAll(clientList);

    }

    /**
     * Creates a new ArrayList(Shot) with only one element
     *
     * @param i The Shot object which will be the only object in the list
     * @return The "one element" list
     */
    private LinkedList<Shot> createArrayListOneArgument(Shot i) {
        LinkedList<Shot> list = new LinkedList<>();
        list.add(i);
        return list;
    }


    /**
     * Creates a map which maps from the clientID on their sunken ships
     *
     * @return The map with ordered shots (sunk)
     */
    public HashMap<Integer, LinkedList<Shot>> sortTheSunk() {
        HashMap<Integer, LinkedList<Shot>> sortedSunk = new HashMap<>();
        for (Shot i : sunk) {
            int clientId = i.getClientId();
            boolean success = false;
            for (Map.Entry<Integer, LinkedList<Shot>> entry : sortedSunk.entrySet()) {
                if (entry.getKey() == clientId) {
                    entry.getValue().add(i);
                    success = true;
                }
            }
            if (!success) {
                sortedSunk.put(clientId, createArrayListOneArgument(i));
            }
        }
        return sortedSunk;
    }


    /**
     * Creates a collection of collections of all possible ship rotations
     *
     * @param ships Collection of points which represents a ship
     * @return allPossibleTurns ArrayList of arraylists for each possible rotation
     */
    private ArrayList<ArrayList<Point2D>> rotateShips(ArrayList<Point2D> ships) {
        RotationMatrix rotate = new RotationMatrix();
        ArrayList<ArrayList<Point2D>> allPossibleTurns = new ArrayList<>();
        ArrayList<Point2D> temp = new ArrayList<>();
        //no turn
        allPossibleTurns.add(rotate.moveToZeroPoint(ships));
        //90 degree
        allPossibleTurns.add(rotate.turn90(ships));
        //180 degree
        ArrayList<Point2D> temp180;
        temp180 = rotate.turn90(ships);
        temp180 = rotate.turn90(temp180);
        allPossibleTurns.add(temp180);

        temp.clear();
        //270 degree
        ArrayList<Point2D> temp270;
        temp270 = rotate.turn90(ships);
        temp270 = rotate.turn90(temp270);
        temp270 = rotate.turn90(temp270);
        allPossibleTurns.add(temp270);
        temp.clear();
        return allPossibleTurns;


    }

    /**
     *
     */
    public void createHeatmapOneClient(int clientId) {
        calcAllSunkenShipsIds();
        //todo Es fehlt dass die Umgebung von einem Feld Abstand uz jedem Punkt berücksichtigt wird
        Integer[][] heatmap = new Integer[getHeight()][getWidth()]; //heatmap array
        for (Integer[] integers : heatmap) {
            Arrays.fill(integers, 0);
        }


        LinkedList<Integer> sunkenIdsThisClient = getAllSunkenShipIds().get(clientId); // get the sunken ship Ids of this client
        ArrayList<Point2D> sunkenPointsThisClient = new ArrayList<>();
        for (Shot s : getSortedSunk().get(clientId)) {
            sunkenPointsThisClient.add(new Point2D(s.getPosition().getX(), s.getPosition().getY()));

        }
        ArrayList<Point2D> invalidPoints = addSurroundingPointsToUsedPoints(sunkenPointsThisClient);

        for (Map.Entry<Integer, ShipType> entry : getShipConfig().entrySet()) {
            if (sunkenIdsThisClient.contains(entry.getKey())) {
                continue; //Wenn das Schiff versenkt ist betrachte nächstes Schiff
            }
            int shipId = entry.getKey(); //Schiffs Id
            //Koordinaten des aktuellen Schiffs
            ArrayList<Point2D> positions = (ArrayList<Point2D>) entry.getValue().getPosition();
            //Rotiere das aktuelle Schiff
            ArrayList<ArrayList<Point2D>> rotated = rotateShips(positions);
            //Betrachte erstes rotiertes Schiff
            for (ArrayList<Point2D> tShips : rotated) {
                ArrayList<Point2D> cShip = new ArrayList<>(tShips); //kopiere erstes rotiertes Schiff
                ArrayList<Integer> xValues = new ArrayList<>(); //alle X werte des Schiff
                ArrayList<Integer> yValues = new ArrayList<>(); //alle y Werte des Schiffs
                for (Point2D z : cShip) { //füge x und y den Listen hinzu
                    xValues.add(z.getX());
                    yValues.add(z.getY());
                }
                Collections.sort(xValues);//sortiere die x, y Listen
                Collections.sort(yValues);
                int maxX = xValues.get(xValues.size() - 1); //hole größtes x, y der Schiffe
                int maxY = yValues.get(yValues.size() - 1);
                int initMaxX = xValues.get(xValues.size() - 1);//lege initiales x, y fest
                int initMaxY = yValues.get(yValues.size() - 1);

                while (maxY < getHeight()) {
                    boolean valid = true;

                    while (maxX < getWidth()) {
                        //check if cShip fits on the field
                        for (Point2D p : cShip) { //jeder Punkt in cShip
                            for (Point2D s : invalidPoints) { //jeder Shot aus den sunkenShotsThisClient
                                if (p.getX() == s.getX() & p.getY() == s.getY()) {
                                    //wenn ein Punkt dem Shot Punkt gleich ist, mache nichts und schiebe Schiff
                                    //einen weiter
                                    valid = false;
                                    break;
                                }
                            }
                            if (!valid) break;

                        }
                        if(valid){
                        //increment the array positions +1
                        for (Point2D i : cShip) {
                            int x = i.getX();
                            int y = i.getY();
                            heatmap[y][x]++;
                        }}

                        ArrayList<Point2D> newPos = new ArrayList<>();
                        for (Point2D u : cShip) {
                            newPos.add(new Point2D(u.getX() + 1, u.getY()));
                        }
                        cShip = new ArrayList<>(newPos);

                        newPos.clear();
                        maxX++;

                    }


                    maxX = initMaxX;
                    ArrayList<Point2D> newPos = new ArrayList<>();
                    for (Point2D u : cShip) {
                        newPos.add(new Point2D(u.getX() - (this.width - initMaxX), u.getY() + 1));
                    }
                    cShip = new ArrayList<>(newPos);
                    newPos.clear();
                    maxY++;

                }

            }


        }

    }

    /**
     * Can be called for getting the id of sunken ships for each client.
     * Sets the sunkenShipIdsAll instead of returning the map
     */
    public void calcAllSunkenShipsIds() {
        logger.info("Try finding sunken ShipIds");
        Map<Integer, LinkedList<Shot>> sortedSunk = getSortedSunk();
        Map<Integer, LinkedList<Integer>> allSunkenShipIds = new HashMap<>(); //maps from client id on the sunken ship ids
        for (Map.Entry<Integer, LinkedList<Shot>> entry : sortedSunk.entrySet()) {
            int clientId = entry.getKey();
            LinkedList<Integer> a = findSunkenShips(entry.getValue());
            allSunkenShipIds.put(clientId, a);
            logger.info("Found sunken ships of Client: " + clientId);
            for (int i : a) {
                logger.info("ShipId: " + i);
            }
        }


        setAllSunkenShipIds(allSunkenShipIds);
    }

    public void setAllSunkenShipIds(Map<Integer, LinkedList<Integer>> sunkenIds) {
        this.allSunkenShipIds = sunkenIds;
    }

    public Map<Integer, LinkedList<Integer>> getAllSunkenShipIds() {
        return this.allSunkenShipIds;
    }

    /**
     * Finds the sunken ship ids of one sunk collection
     * Called by {@link #calcAllSunkenShipsIds()}  for each client who has sunken ships
     *
     * @param shotsThisClient All shots on one client
     * @return Ids of the sunken ships
     */
    public LinkedList<Integer> findSunkenShips(LinkedList<Shot> shotsThisClient) {
        LinkedList<Point2D> sunk = new LinkedList<>(); // enthält alle shots als punkte
        LinkedList<LinkedList<Point2D>> all = new LinkedList<>(); //die initiale liste die wieder aktualisiert wird
        LinkedList<LinkedList<Point2D>> p; //die temporäre linkedlist zum bearbeiten

        for (Shot i : shotsThisClient) { //shots liste in punkte liste umwandeln
            sunk.add(new Point2D(i.getPosition().getX(), i.getPosition().getY()));
        }

        //Algorithmus zum Finden von zusammenhängenden Punkten (Schiffe finden)
        //1. Bilden einer initialen Verteilung der Schiffe
        for (Point2D z : sunk) {
            boolean proofed = false;

            for (LinkedList<Point2D> h : all) {
                for (Point2D j : h) {
                    if (z.getX() == j.getX() & z.getY() == j.getY()) {
                        proofed = true;
                        break;
                    }
                    if (proofed) break;
                }
                if (proofed) break;
            }
            if (proofed) continue;
            LinkedList<Point2D> temp = new LinkedList();
            temp.add(z);
            for (Point2D t : sunk) {
                boolean used = false;
                for (Point2D x : sunk) {
                    for (LinkedList<Point2D> h : all) {
                        for (Point2D j : h) {
                            if (z.getX() == j.getX() & z.getY() == j.getY()) {
                                used = true;
                                break;
                            }
                        }
                        if (used) break;
                    }
                    if (used) break;
                }
                if (used) continue;

                if (z == t) continue;
                boolean checked = false;
                if ((z.getX() + 1 == t.getX() & z.getY() == t.getY())
                        | (z.getX() - 1 == t.getX() & z.getY() == t.getY())
                        | (z.getX() == t.getX() & z.getY() + 1 == t.getY())
                        | (z.getX() == t.getX() & z.getY() - 1 == t.getY())) {
                    for (LinkedList<Point2D> k : all) {
                        for (Point2D u : k) {
                            if (u.getX() == t.getX() & u.getY() == t.getY()) {
                                checked = true;
                                break;
                            }
                            if (checked) break;
                        }
                        if (checked) break;
                    }
                    if (checked) break;
                    temp.add(t);

                }
            }
            all.add(new LinkedList<>(temp));
            temp.clear();
        }
        //2. Ausgehend von der initialen Verteilung der Schiffe werden die anderen zugehörigen
        //   Punkte gesucht und passenden, schon zusammenhängenden Punkteverteilungen hinzugefügt
        //   Aufgrund der Notwendigkeit des Ersetzens des Iterables wird mit einer Kopie gearbeitet.
        //   Dann wird über die Kopie iteriert und die "haupt" Liste wird bearbeitet. Zwischen den Loops
        //   werden die Listen synchronisiert
        p = new LinkedList<>(all);
        boolean success = true;
        boolean findOne = false;
        while (success) {
            for (LinkedList<Point2D> a : all) {
                for (LinkedList<Point2D> b : all) {
                    if (a == b) {
                        continue;
                    }
                    for (Point2D c : a) {
                        for (Point2D d : b) {
                            if ((c.getX() + 1 == d.getX() & c.getY() == d.getY())
                                    | (c.getX() - 1 == d.getX() & c.getY() == d.getY())
                                    | (c.getX() == d.getX() & c.getY() + 1 == d.getY())
                                    | (c.getX() == d.getX() & c.getY() - 1 == d.getY())) {
                                int inB = all.indexOf(b);
                                int inA = all.indexOf(a);
                                LinkedList<Point2D> valueA = p.get(inA);
                                LinkedList<Point2D> valueB = p.get(inB);
                                valueA.addAll(valueB);
                                //p.set(inA, valueA);
                                p.remove(inB);
                                findOne = true;
                                break;
                            }
                            if (findOne) break;
                        }
                        if (findOne) break;
                    }
                    if (findOne) break;
                }
                if (findOne) break;
            }
            all = p;

            if (findOne) {
                findOne = false;

                for (LinkedList<Point2D> a : p) {
                    for (LinkedList<Point2D> b : p) {
                        if (a == b) {
                            continue;
                        }
                        for (Point2D c : a) {
                            for (Point2D d : b) {
                                if ((c.getX() + 1 == d.getX() & c.getY() == d.getY())
                                        | (c.getX() - 1 == d.getX() & c.getY() == d.getY())
                                        | (c.getX() == d.getX() & c.getY() + 1 == d.getY())
                                        | (c.getX() == d.getX() & c.getY() - 1 == d.getY())) {
                                    int inB = p.indexOf(b);
                                    int inA = p.indexOf(a);
                                    LinkedList<Point2D> valueA = all.get(inA);
                                    LinkedList<Point2D> valueB = all.get(inB);
                                    valueA.addAll(valueB);
                                    all.set(inA, valueA);
                                    all.remove(inB);
                                    findOne = true;
                                    break;
                                }
                                if (findOne) break;
                            }
                            if (findOne) break;
                        }
                        if (findOne) break;
                    }
                    if (findOne) break;
                }

            }
            if (!findOne) success = false;

        }

        //3. Finden der zu den gefundenen Schiffsverteilungen passenden Schiffen aus der shipConfig
        //   Dazu wird jedes Schiff aus der config einmal in jeder Rotation über das Spielfeld geschoben
        //   Sobald ein Schiff aus der config mit einem der versenkten übereinstimmt, wird es in eine Collection
        //   aufgenommen
        LinkedList<Integer> sunkenShipIds = new LinkedList<>();
        Map<Integer, ShipType> shipConfig = this.getShipConfig();

        for (Map.Entry<Integer, ShipType> entry : shipConfig.entrySet()) {
            int shipId = entry.getKey();
            ArrayList<ArrayList<Point2D>> t = rotateShips((ArrayList<Point2D>) entry.getValue().getPosition()); //schiff aus der config wird gedreht
            for (LinkedList<Point2D> a : all) { //erster Eintrag in all (erstes gesunkens Schiff)
                boolean find = false;

                for (ArrayList<Point2D> b : t) {//erster Eintrag in t (erstes rotiertes Schiff aus der shipconfig

                    ArrayList<Point2D> bCopy = new ArrayList<>(b);
                    if (a.size() == b.size()) {
                        ArrayList<Integer> xValues = new ArrayList<>();
                        ArrayList<Integer> yValues = new ArrayList<>();
                        for (Point2D z : bCopy) {
                            xValues.add(z.getX());
                            yValues.add(z.getY());
                        }
                        Collections.sort(xValues);
                        Collections.sort(yValues);
                        int maxX = xValues.get(xValues.size() - 1);
                        int maxY = yValues.get(yValues.size() - 1);
                        int initMaxX = xValues.get(xValues.size() - 1);
                        int initMaxY = yValues.get(yValues.size() - 1);
                        while (maxY < this.height) {
                            while (maxX < this.width) {
                                int size = 0;
                                for (Point2D k : a) {
                                    for (Point2D i : bCopy) {
                                        if (k.getX() == i.getX() & k.getY() == i.getY()) {
                                            size++;
                                        } else {
                                            continue;
                                        }
                                        if (size == a.size()) {
                                            sunkenShipIds.add(shipId);
                                            find = true;
                                            break;
                                        }
                                    }
                                    if (find) break;
                                }
                                if (find) break;
                                ArrayList<Point2D> newPos = new ArrayList<>();
                                for (Point2D u : bCopy) {
                                    newPos.add(new Point2D(u.getX() + 1, u.getY()));
                                }
                                bCopy = new ArrayList<>(newPos);
                                newPos.clear();
                                maxX++;
                            }
                            maxX = initMaxX;
                            if (find) break;
                            ArrayList<Point2D> newPos = new ArrayList<>();
                            for (Point2D u : bCopy) {
                                newPos.add(new Point2D(u.getX() - (width - initMaxX), u.getY() + 1));
                            }
                            bCopy = new ArrayList<>(newPos);
                            newPos.clear();
                            maxY++;

                        }
                    } else {
                        break;
                    }
                    if (find) break;
                }
                if (find) break;

            }
        }
        return sunkenShipIds;

    }

    /**
     * Places shots randomly on the field of one opponent and sends the fitting message.
     *
     * @throws IOException
     */
    public void placeShotsRandom() throws IOException {
        //reset
        this.shotsRequest = null;
        this.choosenShots = null;
        //update numberOfClients every time the method is calls because the number of connected clients could have changed
        int numberOfClients = clientArrayList.size();
        int shotCount = getShotCount();
        int shotClientId;

        while (true) {

            int aiIndex = clientArrayList.indexOf(this); //get the index of the ai
            int randomIndex = (int) (Math.random() * numberOfClients);
            if (randomIndex != aiIndex) {
                shotClientId = clientArrayList.get(randomIndex).getId(); //shotClientId is the target for placing shots in the next part
                break;
            }
        }

        this.choosenShots = new ArrayList<>();

        ArrayList<Point2D> aimsThisRound = new ArrayList<>();

        ArrayList<Point2D> hitPoints = new ArrayList<>();

        for (Shot k : updateNotification.getHits()) {
            if (k.getClientId() == shotClientId) {
                hitPoints.add(k.getPosition());
            }
        }

        //placing the shots randomly until the max of shots is not reached
        //all shots will be placed on the field of only one opponents field(other client)
        int i = 0;
        while (i < shotCount) {

            Point2D aimPoint = getRandomPoint2D(); //aim is one of the random points as a candidate for a shot
            boolean alreadyChoosen = false;
            for (Point2D p : aimsThisRound) {
                if (p.getX() == aimPoint.getX() & p.getY() == aimPoint.getY()) {
                    alreadyChoosen = true;
                }
            }
            for (Point2D h : hitPoints) {
                if (h.getX() == aimPoint.getX() & h.getY() == aimPoint.getY()) {
                    alreadyChoosen = true;
                }
            }
            if (alreadyChoosen) continue;

            aimsThisRound.add(aimPoint);
            //create a new shot object, add it to requestedShot Array and increase i
            Shot shot = new Shot(shotClientId, aimPoint);
            choosenShots.add(shot);
            //System.out.println(choosenShots);
            i++;

        }
        //create new shotsRequest Object with the choosenShots Collection
        System.out.println(choosenShots.size() == this.SHOTCOUNT);

        this.shotsRequest = new ShotsRequest(choosenShots);

        //send the shotsRequest object to the server
        //TODO only for testing disabled
        //connector.sendMessageToServer(shotsRequest);
    }

    /**
     * Creates a random point related to the width and height of the game field
     *
     * @return Point2d Random Point with X and Y coordinates
     */
    public Point2D getRandomPoint2D() {
        int x = (int) (Math.random() * getWidth());
        int y = (int) (Math.random() * getHeight());
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
     * Used by the connect method to set the connector
     *
     * @param connector
     */
    public void setConnector(ClientConnector connector) {
        this.connector = connector;
    }

    public void setSortedSunk(HashMap<Integer, LinkedList<Shot>> sortedSunk) {
        this.sortedSunk = sortedSunk;
    }

    public Map<Integer, LinkedList<Shot>> getSortedSunk() {
        return this.sortedSunk;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setShipConfig(Map<Integer, ShipType> shipConfig) {
        this.shipConfig = shipConfig;
    }

    public Map<Integer, ShipType> getShipConfig() {
        return this.shipConfig;
    }

    public void setSuccessfulPlacement() {
        this.successful = true;
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

    public void setHitpoints(int hitpoints) {
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

    private void setSunkpoints(int sunkPoints) {
        this.SUNKPOINTS = sunkPoints;
    }

    public Map<Integer, PlacementInfo> getPositions() {
        return this.positions;
    }

}