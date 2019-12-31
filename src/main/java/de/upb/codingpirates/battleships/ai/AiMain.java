package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.network.message.request.*;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Creates the ai Player Object and holds it alive while playing.
 * {@link Ai}. Is associated with only one Ai object int this version.
 *
 * @author Benjamin Kasten
 */
public class AiMain {
    static Timer timer = new Timer();
    public static Ai ai = new Ai();

    static String host;
    static int port;
    static int difficultyLevel;

    /**
     * Is called by the command line and creates an new Ai by calling {@link AiMain#createNewAiPlayer(String, int)}
     *
     * @param args host (Ip Adress of the server) and port
     * @throws IOException Network error
     */
    public static void main(String[] args) throws IOException {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            }
        }, 1L, 1L);
        //default: "192.168.0.234" 47345 3
        host = args[0]; // "localhost"
        port = Integer.parseInt(args[1]); // 47345
        setDifficultyLevel(Integer.parseInt(args[2])); // 3

        createNewAiPlayer(host, port);
    }

    /**
     * Is called by the {@link AiMain#main(String[])}  for creating a new Ai object and connecting it to the server
     * Sends the ServerJoinRequest message.
     *
     * @param host is the ip address of the server
     * @param port is the port number of the server
     * @throws IOException Network error
     */
    protected static void createNewAiPlayer(String host, int port) throws IOException {
        ai.setInstance(ai);
        ai.getTcpConnector().connect(host, port);
        //random ai name without any claim to be unique
        ai.getTcpConnector().sendMessageToServer(new ServerJoinRequest("EnginePlayer" + ((int) (Math.random() * 10000)), ClientType.PLAYER));
    }

    /**
     * Is called by the {@link Ai#onConnectionClosedReport(ConnectionClosedReport, int)}
     * when the game is finished or the connection failed.
     * (Closes the Ai because there is no sense for keeping it alive in these cases)
     */
    public static void close() {
        try {
            ai.getTcpConnector().disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //canceling the timer to stop the main method which keeps the ai alive
        timer.cancel();
        timer.purge();
    }

    public static void setDifficultyLevel(int d) {
        difficultyLevel = d;
    }

    public static int getDifficultyLevel() {
        return difficultyLevel;
    }


}
