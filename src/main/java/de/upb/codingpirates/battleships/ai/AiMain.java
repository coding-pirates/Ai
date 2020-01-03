package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.network.message.request.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Creates the ai Player Object and holds it alive while playing.
 * Is associated with only one  {@link Ai} instance int this version.
 *
 * @author Benjamin Kasten
 */
public class AiMain {
    private static final Logger logger = LogManager.getLogger();

    static Timer timer = new Timer();
    public static Ai ai = new Ai();
    static String aiName;

    static String ipAddress;
    static int port;

    /**
     * Is called by the command line and creates an new Ai by calling {@link AiMain#createNewAiPlayer(String, int)}
     *
     * @param args ipAddress (Ip address of the server), port and difficulty level
     * @throws IOException Network connection error
     */
    public static void main(String[] args) throws IOException {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            }
        }, 1L, 1L);
        //default (reference server): "swtpra.cs.upb.de" 33101 3
        //default: "192.168.0.234" 47345 3
        ipAddress = args[0];
        port = Integer.parseInt(args[1]);
        ai.setDifficultyLevel(Integer.parseInt(args[2]));
        createNewAiPlayer(ipAddress, port);
    }

    /**
     * Is called by the {@link AiMain#main(String[])}  for creating a new Ai object and connecting it to the server
     * Sends the ServerJoinRequest message.
     *
     * @param ipAddress The ip address of the server
     * @param port      The port number of the server
     * @throws IOException Network error
     */
    public static void createNewAiPlayer(String ipAddress, int port) throws IOException {
        aiName = "EnginePlayer" + ((int) (Math.random() * 1000000));   //random ai name without any claim to be unique
        logger.info(MARKER.AI, "Creating new Engine Player with name: {}", aiName);
        ai.setInstance(ai);
        ai.getTcpConnector().connect(ipAddress, port);
        ai.getTcpConnector().sendMessageToServer(new ServerJoinRequest(aiName, ClientType.PLAYER));
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
}
