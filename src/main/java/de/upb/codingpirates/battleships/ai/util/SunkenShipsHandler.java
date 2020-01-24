package de.upb.codingpirates.battleships.ai.util;

import com.google.common.collect.Lists;
import de.upb.codingpirates.battleships.ai.AI;
import de.upb.codingpirates.battleships.ai.logger.Markers;
import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;
import de.upb.codingpirates.battleships.logic.Shot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.Map.Entry;

/**
 * The class for handling sunken ships uding the hits and the sunk points
 *
 * @author Benjamin Kasten
 */
public class SunkenShipsHandler {
    private static final Logger logger = LogManager.getLogger();

    AI ai;

    /**
     * Constructor for {@link MissesFinder}. Gets an instance of the ai object which creates the {@link MissesFinder}
     * instance.
     *
     * @param ai The instance of the ai who called the constructor.
     */

    public SunkenShipsHandler(AI ai) {
        this.ai = ai;
    }

    //all points which are used for identification already
    public Collection<Point2D> used = new ArrayList<>();


    /**
     * Can be called for getting the id of sunken ships for each client.
     * Sets the sunkenShipIdsAll instead of returning the map
     *
     * @return sunken ship ids of each client
     */
    public Map<Integer, List<Integer>> findSunkenShipIdsAll() {
        Map<Integer, List<Integer>> allSunkenShipIds = new HashMap<>(); //maps from client id on the sunken ship ids

        HitsHandler hitsHandler = new HitsHandler(this.ai);
        Map<Integer, List<Point2D>> sortedHits = hitsHandler.sortTheHits();

        for (Entry<Integer, List<Point2D>> entry : sortedHits.entrySet()) {
            used.clear();
            int clientId = entry.getKey();
            List<List<Point2D>> sortedHitsByPosition = findConnectedPoints(entry.getValue());
            List<List<Point2D>> sortedSunkByPosition = getSunksOfHits(sortedHitsByPosition, clientId);


            List<Integer> sunkenShipIds = findIds(sortedSunkByPosition);
            allSunkenShipIds.put(clientId, sunkenShipIds);
            if (sunkenShipIds.isEmpty()) {
                logger.info(Markers.AI_SUNKEN_SHIPS_HANDLER, "Player {} has no sunken ships yet", clientId);
                continue;
            }

            logger.info(Markers.AI_SUNKEN_SHIPS_HANDLER, "Sunken ship ids of player {} are: ", clientId);

            for (int i : sunkenShipIds)
                System.out.print(i + " ");
            System.out.println();
        }
        return allSunkenShipIds;
    }

    /**
     * Computes all points of the sunken ships per client in one collection
     *
     * @return sunken ships per client in one collection
     */
    public Map<Integer, List<Point2D>> createSortedSunk() {
        Map<Integer, List<Point2D>> sortedSunkAll = new HashMap<>();
        for (Client c : ai.getClientArrayList()) {
            List<Point2D> sortedSunkOne = new LinkedList<>();
            List<Point2D> hitsTC = new LinkedList<>();

            for (Shot s : ai.getHits()) {
                if (s.getClientId() == c.getId()) {
                    hitsTC.add(s.getTargetField());
                }
            }

            List<List<Point2D>> connectedHits = findConnectedPoints(hitsTC);
            List<List<Point2D>> connectedSunks = getSunksOfHits(connectedHits, c.getId());
            for (List<Point2D> l : connectedSunks)
                sortedSunkOne.addAll(l);
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
    public List<List<Point2D>> findConnectedPoints(@Nonnull final List<Point2D> hitsThisClient) {
        List<List<Point2D>> sunkPositions = new LinkedList<>(); //the initial list which will be updated and returned at the end
        List<List<Point2D>> p; //the temporary list for edit

        //Algorithm for finding related points (points of ships)
        //1. Creating a initial allocation
        for (Point2D z : hitsThisClient) {
            boolean proofed = false;

            for (List<Point2D> h : sunkPositions) {
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
                    for (List<Point2D> h : sunkPositions) {
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
                    for (List<Point2D> k : sunkPositions) {
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

        //2. Using the initial allocation  of points the other relarted points will be searched and
        //  will be added to already related point allocations if they fit.

        p = new LinkedList<>(sunkPositions);

        boolean success = true;
        boolean findOne = false;

        while (success) {
            for (List<Point2D> a : sunkPositions) {
                for (List<Point2D> b : sunkPositions) {
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
                                List<Point2D> valueA = p.get(inA);
                                List<Point2D> valueB = p.get(inB);
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
                for (List<Point2D> a : p) {
                    for (List<Point2D> b : p) {
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
                                    List<Point2D> valueA = sunkPositions.get(inA);
                                    List<Point2D> valueB = sunkPositions.get(inB);
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

    /**
     * Computes the collection of hits which are sunk collections.
     *
     * @param sortedHitsByPosition the sorted hits which also includes not sunk collections.
     * @param clientId             the id of the client
     * @return only the sorted sunk
     */
    public List<List<Point2D>> getSunksOfHits(List<List<Point2D>> sortedHitsByPosition, int clientId) {
        List<List<Point2D>> sortedSunksByPosition = Lists.newLinkedList();
        boolean isSunkShip;
        for (List<Point2D> l : sortedHitsByPosition) {
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


    /**
     * Finds the ids of sunken ships of one client.
     *
     * @param sortedSunkByPosition The sorted sunk points of one client
     * @return the sunken ship ids of the client
     */
    public List<Integer> findIds(List<List<Point2D>> sortedSunkByPosition) {
        boolean isValid;
        List<Integer> sunkenShipIds = new LinkedList<>();
        Map<Integer, ShipType> ships = ai.getConfiguration().getShips();

        for (Entry<Integer, ShipType> entry : ships.entrySet()) {
            int shipId = entry.getKey();
            Rotator rotator = new Rotator(this.ai);
            List<List<Point2D>> t = rotator.rotateShips((List<Point2D>) entry.getValue().getPositions()); //schiff aus der config wird gedreht
            for (List<Point2D> a : sortedSunkByPosition) { //entry in all (a sunken ship)
                boolean find = false;
                for (List<Point2D> b : t) {//entry in t (a rotation of a ship of the shipconfig)
                    List<Point2D> bCopy = new ArrayList<>(b); //the ship which will be shifted over the field

                    if (a.size() == b.size()) {
                        List<Integer> xValues = new ArrayList<>();
                        List<Integer> yValues = new ArrayList<>();
                        for (Point2D z : bCopy) {
                            xValues.add(z.getX());
                            yValues.add(z.getY());
                        }
                        Collections.sort(xValues);
                        Collections.sort(yValues);
                        int maxX = xValues.get(xValues.size() - 1);
                        int maxY = yValues.get(yValues.size() - 1);
                        int initMaxX = xValues.get(xValues.size() - 1);
                        while (maxY < ai.getConfiguration().getHeight()) {
                            while (maxX < ai.getConfiguration().getWidth()) {
                                isValid = true;
                                for (Point2D s : bCopy) {
                                    for (Point2D f : used) {
                                        if (PositionComparator.comparePoints(s, f)) {
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
                                newPos.add(new Point2D(u.getX() - (ai.getConfiguration().getWidth() - initMaxX), u.getY() + 1));
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
     * @deprecated replaced by {@link #createSortedSunk()}
     */
    public HashMap<Integer, List<Point2D>> sortTheSunk() {
        HashMap<Integer, List<Point2D>> sortedSunk = new HashMap<>();

        for (Shot i : ai.getSunk()) {
            int clientId = i.getClientId();
            boolean success = false;
            for (Entry<Integer, List<Point2D>> entry : sortedSunk.entrySet()) {
                if (entry.getKey() == clientId) {
                    entry.getValue().add(i.getTargetField());
                    success = true;
                }
            }
            if (!success) {
                sortedSunk.put(clientId, new LinkedList<>(Collections.singletonList(i.getTargetField())));
            }
        }
        for (Client c : ai.getClientArrayList()) {
            if (!sortedSunk.containsKey(c.getId())) {
                List<Point2D> emptyList = new LinkedList<>();
                sortedSunk.put(c.getId(), emptyList);
            }
        }
        for (Entry<Integer, List<Point2D>> entry : sortedSunk.entrySet()) {
            if (entry.getValue().isEmpty()) {
                logger.info(Markers.AI_SUNKEN_SHIPS_HANDLER, "No sunk points of client {}", entry.getKey());
                continue;
            }
            logger.info(Markers.AI_SUNKEN_SHIPS_HANDLER, "Sunk points of client {} are", entry.getKey());
            for (Point2D s : entry.getValue()) {
                logger.info(Markers.AI_SUNKEN_SHIPS_HANDLER, s);
            }
        }
        return sortedSunk;
    }

}
