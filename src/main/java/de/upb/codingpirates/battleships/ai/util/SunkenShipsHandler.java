package de.upb.codingpirates.battleships.ai.util;

import com.google.common.collect.Lists;
import de.upb.codingpirates.battleships.ai.Ai;
import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * The class for computing the sunken ships out of the sunk collection.
 *
 * @author Benjamin Kasten
 */
public class SunkenShipsHandler {
    private static final Logger logger = LogManager.getLogger();
    Ai ai;


    /**
     * Constructor for {@link MissesFinder}. Gets an instance of the ai object which creates the {@link MissesFinder}
     * instance.
     *
     * @param ai The instance of the ai who called the constructor.
     */

    public SunkenShipsHandler(Ai ai) {
        this.ai = ai;
    }

    // alle Punkte, aus denen bereits ein Schiff identifiziert wurde. Mit diesen Punkten kann kein weiteres Schiff
    //identifiziert werden
    public Collection<Point2D> used = new ArrayList<>();


    /**
     * Can be called for getting the id of sunken ships for each client.
     * Sets the sunkenShipIdsAll instead of returning the map
     *
     * @return sunken ship ids of each client
     */
    public Map<Integer, LinkedList<Integer>> findSunkenShipIdsAll() {
        Map<Integer, LinkedList<Integer>> allSunkenShipIds = new HashMap<>(); //maps from client id on the sunken ship ids

        HitsHandler hitsHandler = new HitsHandler(this.ai);
        Map<Integer, LinkedList<Point2D>> sortedHits = hitsHandler.sortTheHits();

        for (Map.Entry<Integer, LinkedList<Point2D>> entry : sortedHits.entrySet()) {
            used.clear();
            int clientId = entry.getKey();
            LinkedList<LinkedList<Point2D>> sortedHitsByPosition = findConnectedPoints(entry.getValue(), clientId);
            LinkedList<LinkedList<Point2D>> sortedSunkByPosition = getSunksOfHits(sortedHitsByPosition, clientId);


            LinkedList<Integer> sunkenShipIds = findIds(sortedSunkByPosition, clientId);
            allSunkenShipIds.put(clientId, sunkenShipIds);
            if (sunkenShipIds.isEmpty()) {
                logger.info(MARKER.Ai_SunkenShips, "Player {} has no sunken ships yet", clientId);
                continue;
            }

            logger.info(MARKER.Ai_SunkenShips, "Sunken ships of player {} are: ", clientId);

            for (int i : sunkenShipIds) {
                logger.info(MARKER.Ai_SunkenShips, "ShipId: " + i);
            }


        }
        return allSunkenShipIds;
    }

    public HashMap<Integer, LinkedList<Point2D>> createSortedSunk() {
        HashMap<Integer, LinkedList<Point2D>> sortedSunkAll = new HashMap<>();
        for (Client c : ai.getClientArrayList()) {
            LinkedList<Point2D> sortedSunkOne = new LinkedList<>();
            LinkedList<Point2D> hitsTC = Lists.newLinkedList();

            for (Shot s : ai.getHits()) {
                if (s.getClientId() == c.getId()) {
                    hitsTC.add(s.getTargetField());
                }
            }

            LinkedList<LinkedList<Point2D>> connectedHits = findConnectedPoints(hitsTC, c.getId());
            LinkedList<LinkedList<Point2D>> connectedSunks = getSunksOfHits(connectedHits, c.getId());
            for (LinkedList<Point2D> l : connectedSunks) {
                sortedSunkOne.addAll(l);
            }
            sortedSunkAll.put(c.getId(), sortedSunkOne);

        }
        return sortedSunkAll;
    }

    /**
     * Finds the points for each sunken ship for one client.
     *
     * @param hitsThisClient All shots on one client
     * @return connected Points
     */
    public LinkedList<LinkedList<Point2D>> findConnectedPoints(LinkedList<Point2D> hitsThisClient, int clientId) {
        LinkedList<LinkedList<Point2D>> sunkPositions = new LinkedList<>(); //die initiale liste die wieder aktualisiert wird
        LinkedList<LinkedList<Point2D>> p; //die temporäre linkedlist zum bearbeiten


        //Algorithmus zum Finden von zusammenhängenden Punkten (Schiffe finden)
        //1. Bilden einer initialen Verteilung der Schiffe
        for (Point2D z : hitsThisClient) {
            boolean proofed = false;

            for (LinkedList<Point2D> h : sunkPositions) {
                for (Point2D j : h) {
                    if (z.getX() == j.getX() & z.getY() == j.getY()) {
                        proofed = true;
                        break;
                    }
                }
                if (proofed) break;
            }
            if (proofed) continue;
            LinkedList<Point2D> temp = new LinkedList<>();
            temp.add(z);

            for (Point2D t : hitsThisClient) {
                boolean used = false;
                for (Point2D x : hitsThisClient) {
                    for (LinkedList<Point2D> h : sunkPositions) {
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
                    for (LinkedList<Point2D> k : sunkPositions) {
                        for (Point2D u : k) {
                            if (u.getX() == t.getX() & u.getY() == t.getY()) {
                                checked = true;
                                break;
                            }
                        }
                        if (checked) break;
                    }
                    if (checked) break;
                    temp.add(t);
                }
            }
            sunkPositions.add(new LinkedList<>(temp));
            temp.clear();
        }

        //2. Ausgehend von der initialen Verteilung der Schiffe werden die anderen zugehörigen
        //   Punkte gesucht und passenden, schon zusammenhängenden Punkteverteilungen hinzugefügt
        //   Aufgrund der Notwendigkeit des Ersetzens des Iterables wird mit einer Kopie gearbeitet.
        //   Dann wird über die Kopie iteriert und die "haupt" Liste wird bearbeitet. Zwischen den Loops
        //   werden die Listen synchronisiert

        p = new LinkedList<>(sunkPositions);

        boolean success = true;
        boolean findOne = false;

        while (success) {
            for (LinkedList<Point2D> a : sunkPositions) {
                for (LinkedList<Point2D> b : sunkPositions) {
                    if (a == b) {
                        continue;
                    }
                    for (Point2D c : a) {
                        for (Point2D d : b) {
                            if ((c.getX() + 1 == d.getX() & c.getY() == d.getY())
                                    | (c.getX() - 1 == d.getX() & c.getY() == d.getY())
                                    | (c.getX() == d.getX() & c.getY() + 1 == d.getY())
                                    | (c.getX() == d.getX() & c.getY() - 1 == d.getY())) {
                                int inB = sunkPositions.indexOf(b);
                                int inA = sunkPositions.indexOf(a);
                                LinkedList<Point2D> valueA = p.get(inA);
                                LinkedList<Point2D> valueB = p.get(inB);
                                valueA.addAll(valueB);
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

            sunkPositions = p;

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
                                    LinkedList<Point2D> valueA = sunkPositions.get(inA);
                                    LinkedList<Point2D> valueB = sunkPositions.get(inB);
                                    valueA.addAll(valueB);
                                    sunkPositions.set(inA, valueA);
                                    sunkPositions.remove(inB);
                                    findOne = true;
                                    break;
                                }
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
        return sunkPositions;
    }

    public LinkedList<LinkedList<Point2D>> getSunksOfHits(LinkedList<LinkedList<Point2D>> sortedHitsByPosition,
                                                          int clientId) {
        LinkedList<LinkedList<Point2D>> sortedSunksByPosition = Lists.newLinkedList();
        boolean isSunkShip;
        for (LinkedList<Point2D> l : sortedHitsByPosition) {
            Collection<Shot> sunk = ai.getSunk();
            isSunkShip = false;
            for (Point2D p : l) {
                for (Shot s : sunk) {
                    if (PositionComparator.comparePointShot(p, s, clientId)) {
                        sortedSunksByPosition.add(l);
                        isSunkShip = true;
                        break;
                    }
                }
                if (isSunkShip) break;
            }
        }
        return sortedSunksByPosition;
    }


    public LinkedList<Integer> findIds(LinkedList<LinkedList<Point2D>> sortedSunkByPosition, int clientId) {
        //3. Finden der zu den gefundenen Schiffsverteilungen passenden Schiffen aus der shipConfig
        //   Dazu wird jedes Schiff aus der config einmal in jeder Rotation über das Spielfeld geschoben
        //   Sobald ein Schiff aus der config mit einem der versenkten übereinstimmt, wird es in eine Collection
        //   aufgenommen

        //logger.debug("The points which cannot be accessed for identification for ships of client {}", clientId);
        //System.out.println(used);
        boolean isValid;
        LinkedList<Integer> sunkenShipIds = new LinkedList<>();
        Map<Integer, ShipType> ships = ai.getShips();

        for (Map.Entry<Integer, ShipType> entry : ships.entrySet()) {
            int shipId = entry.getKey();
            Rotator rotator = new Rotator(this.ai);
            ArrayList<ArrayList<Point2D>> t = rotator.rotateShips((ArrayList<Point2D>) entry.getValue().getPositions()); //schiff aus der config wird gedreht
            for (LinkedList<Point2D> a : sortedSunkByPosition) { //erster Eintrag in all (erstes gesunkens Schiff)
                boolean find = false;
                for (ArrayList<Point2D> b : t) {//erster Eintrag in t (erstes rotiertes Schiff aus der shipconfig
                    ArrayList<Point2D> bCopy = new ArrayList<>(b); //das schiff in der pos, mit der es über das feld geschoben wird

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
                        while (maxY < ai.getHeight()) {
                            while (maxX < ai.getWidth()) {
                                isValid = true;
                                for (Point2D s : bCopy) {
                                    for (Point2D f : used) {
                                        if (PositionComparator.comparePoints(s, f)) {
                                            //logger.debug("{} can not be used for identification anymore", f);
                                            isValid = false;
                                            break;
                                        }
                                    }
                                    if (!isValid) {
                                        break;
                                    }
                                }


                                if (isValid) {
                                    Collection<Point2D> tempInv = new ArrayList<>();
                                    int size = 0;
                                    for (Point2D k : a) {
                                        for (Point2D i : bCopy) {
                                            if (PositionComparator.comparePoints(k, i)) {
                                                tempInv.add(i);
                                                size++;
                                            }
                                            if (size == a.size()) {
                                                sunkenShipIds.add(shipId);
                                                //logger.debug("Added to sunk: {}", shipId);
                                                find = true;
                                                break;
                                            }
                                        }
                                        if (find) {
                                            used.addAll(new ArrayList<>(tempInv));
                                            break;
                                        }
                                    }
                                }
                                if (find) {
                                    break;
                                }
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
                                newPos.add(new Point2D(u.getX() - (ai.getWidth() - initMaxX), u.getY() + 1));
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

                if (find) {
                    //logger.info("Find true - break");
                    break;
                }
            }
        }
        return sunkenShipIds;
    }

    /**
     * Creates a map which maps from the clientID on their sunk points
     *
     * @return The map with ordered sunks
     */
    public HashMap<Integer, LinkedList<Point2D>> sortTheSunk() {
        HashMap<Integer, LinkedList<Point2D>> sortedSunk = new HashMap<>();

        for (Shot i : ai.getSunk()) {
            int clientId = i.getClientId();
            boolean success = false;
            for (Map.Entry<Integer, LinkedList<Point2D>> entry : sortedSunk.entrySet()) {
                if (entry.getKey() == clientId) {
                    entry.getValue().add(i.getTargetField());
                    success = true;
                }
            }
            if (!success) {
                //sortedSunk.put(clientId, ai.createArrayListOneArgument(i));
                sortedSunk.put(clientId, new LinkedList<>(Collections.singletonList(i.getTargetField())));
            }
        }
        for (Client c : ai.getClientArrayList()) {
            if (!sortedSunk.containsKey(c.getId())) {
                LinkedList<Point2D> emptyList = new LinkedList<>();
                sortedSunk.put(c.getId(), emptyList);
            }
        }
        //logger.info( "Sorted the sunken ships by their clients.");
        for (Map.Entry<Integer, LinkedList<Point2D>> entry : sortedSunk.entrySet()) {
            if (entry.getValue().isEmpty()) {
                logger.info(MARKER.Ai_SunkenShips, "No sunk points of client {}", entry.getKey());
                continue;
            }
            logger.info(MARKER.Ai_SunkenShips, "Sunk points of client {} are", entry.getKey());
            for (Point2D s : entry.getValue()) {
                logger.info(MARKER.Ai_SunkenShips, s);
            }
        }
        return sortedSunk;
    }

}
