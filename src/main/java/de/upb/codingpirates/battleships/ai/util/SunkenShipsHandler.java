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


    /**
     * Can be called for getting the id of sunken ships for each client.
     * Sets the sunkenShipIdsAll instead of returning the map
     *
     * @return sunken ship ids of each client
     */
    public Map<Integer, LinkedList<Integer>> findSunkenShipIdsAll() {
        //logger.info(MARKER.AI, "Try finding sunken ShipIds");
        Map<Integer, LinkedList<Shot>> sortedSunk = ai.getSortedSunk();
        Map<Integer, LinkedList<Integer>> allSunkenShipIds = new HashMap<>(); //maps from client id on the sunken ship ids
        for (Map.Entry<Integer, LinkedList<Shot>> entry : sortedSunk.entrySet()) {
            int clientId = entry.getKey();
            LinkedList<LinkedList<Point2D>> a_ = findConnectedPoints(entry.getValue(), clientId);
            LinkedList<Integer> a = findIds(a_, clientId);
            allSunkenShipIds.put(clientId, a);
            if (a.isEmpty()) {
                logger.info(MARKER.AI, "Found no sunken ships of Client " + clientId);
            } else {
                logger.info(MARKER.AI, "Found sunken ships of Client " + clientId);
                for (int i : a) {
                    logger.info(MARKER.AI, "ShipId: " + i);
                }
            }
        }
        return allSunkenShipIds;
    }

    /**
     * Finds the points for each sunken ship for one client.
     *
     * @param hitsThisClient All shots on one client
     * @return sunken ship ids of this client
     */
    public LinkedList<LinkedList<Point2D>> findConnectedPoints(LinkedList<Shot> hitsThisClient, int clientId) {
        LinkedList<Point2D> sunk = new LinkedList<>(); // enthält alle shots als punkte
        LinkedList<LinkedList<Point2D>> all = new LinkedList<>(); //die initiale liste die wieder aktualisiert wird
        LinkedList<LinkedList<Point2D>> p; //die temporäre linkedlist zum bearbeiten

        for (Shot i : hitsThisClient) { //shots liste in punkte liste umwandeln
            sunk.add(new Point2D(i.getTargetField().getX(), i.getTargetField().getY()));
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
                }
                if (proofed) break;
            }
            if (proofed) continue;
            LinkedList<Point2D> temp = new LinkedList<>();
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

        logger.debug("Sunken positions of client {}", clientId);
        for (LinkedList<Point2D> l : all) {
            System.out.println("A sunk collection: ");
            for (Point2D z : l) {
                System.out.println(z);
            }
        }
        return all;

    }


    public LinkedList<Integer> findIds(LinkedList<LinkedList<Point2D>> sortedHitsByPosition, int clientId) {
        //3. Finden der zu den gefundenen Schiffsverteilungen passenden Schiffen aus der shipConfig
        //   Dazu wird jedes Schiff aus der config einmal in jeder Rotation über das Spielfeld geschoben
        //   Sobald ein Schiff aus der config mit einem der versenkten übereinstimmt, wird es in eine Collection
        //   aufgenommen
        Collection<Shot> sunk = ai.getSunk();

        LinkedList<LinkedList<Point2D>> all = Lists.newLinkedList();


        boolean isSunkShip;

        for (LinkedList<Point2D> l : sortedHitsByPosition) {
            isSunkShip = false;
            for (Point2D p : l) {
                for (Shot s : sunk) {
                    if (PositionComparator.comparePointShot(p, s, clientId)) {
                        all.add(l);
                        isSunkShip = true;
                        break;
                    }
                }
                if (isSunkShip) break;
            }
        }


        LinkedList<Integer> sunkenShipIds = new LinkedList<>();
        Map<Integer, ShipType> ships = ai.getShips();

        for (Map.Entry<Integer, ShipType> entry : ships.entrySet()) {
            int shipId = entry.getKey();
            Rotator rotator = new Rotator(this.ai);
            ArrayList<ArrayList<Point2D>> t = rotator.rotateShips((ArrayList<Point2D>) entry.getValue().getPositions()); //schiff aus der config wird gedreht
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
                        while (maxY < ai.getHeight()) {
                            while (maxX < ai.getWidth()) {
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
                if (find) break;

            }
        }
        return sunkenShipIds;
    }

    /**
     * Creates a map which maps from the clientID on their sunk points
     *
     * @return The map with ordered sunks
     * @deprecated Replaced with {@link HitsHandler#sortTheHits()}
     */
    public HashMap<Integer, LinkedList<Shot>> sortTheSunk() {
        //logger.info("Number of sunken points: {}", ai.getSunk().size());
        //logger.info(MARKER.AI, "Sorting the sunken ships by their clients...");
        HashMap<Integer, LinkedList<Shot>> sortedSunk = new HashMap<>();
        for (Shot i : ai.getSunk()) {
            int clientId = i.getClientId();
            boolean success = false;
            for (Map.Entry<Integer, LinkedList<Shot>> entry : sortedSunk.entrySet()) {
                if (entry.getKey() == clientId) {
                    entry.getValue().add(i);
                    success = true;
                }
            }
            if (!success) {
                //sortedSunk.put(clientId, ai.createArrayListOneArgument(i));
                sortedSunk.put(clientId, new LinkedList<>(Collections.singletonList(i)));
            }
        }
        for (Client c : ai.getClientArrayList()) {
            if (!sortedSunk.containsKey(c.getId())) {
                LinkedList<Shot> emptyList = new LinkedList<>();
                sortedSunk.put(c.getId(), emptyList);
            }
        }
        //logger.info(MARKER.AI, "Sorted the sunken ships by their clients.");
        for (Map.Entry<Integer, LinkedList<Shot>> entry : sortedSunk.entrySet()) {
            if (entry.getValue().isEmpty()) {
                logger.info("No sunk points of client {}", entry.getKey());
                continue;
            }
            logger.info("Sunk points of client {} are", entry.getKey());
            for (Shot s : entry.getValue()) {
                logger.info(s);
            }
        }
        return sortedSunk;
    }

}
