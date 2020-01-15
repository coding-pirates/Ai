package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.ai.logger.MARKER;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.network.message.request.RequestBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Creates the ai Player Object and holds it alive while playing.
 * Is associated with only one  {@link Ai} instance this version.
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
     * Is called by the command line and creates an new Ai by calling {@link AiMain#connect(String, int)}
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

        ipAddress = args[0];
        port = Integer.parseInt(args[1]);
        ai.setDifficultyLevel(Integer.parseInt(args[2]));
        aiName = args[3];

        connect(ipAddress, port);

    }

    /**
     * Is called by the {@link AiMain#main(String[])}  for connecting the ai instance with the server
     * Sends the ServerJoinRequest message.
     *
     * @param ipAddress The ip address of the server
     * @param port      The port number of the server
     * @throws IOException Network error
     */
    public static void connect(String ipAddress, int port) throws IOException {

        ai.setInstance(ai);
        logger.info(MARKER.Ai_Main, "Connect new Engine Player with name: {}", aiName);
        ai.getTcpConnector().connect(ipAddress, port);
        ai.sendMessage(RequestBuilder.serverJoinRequest(aiName, ClientType.PLAYER));

    }

    /**
     * Is called by the {@link Ai#onConnectionClosedReport(ConnectionClosedReport, int)}
     * when the game is finished or the connection failed.
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
