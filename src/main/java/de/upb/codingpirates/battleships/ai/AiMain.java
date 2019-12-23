package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.Properties;
import de.upb.codingpirates.battleships.network.message.request.*;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Creates an new Ai object and handles the information exchange between the {@link AiMessageHandler} and the
 * {@link Ai}. Is associated with only one Ai object int this version.
 *
 * @author Benjamin Kasten
 */
public class AiMain {
    static Timer timer = new Timer();
    static Ai ai = new Ai();
    static AiMessageHandler aiMessageHandler = new AiMessageHandler();

    /**
     * Is called by the command line and creates an new Ai by calling {@link AiMain#createNewAiPlayer(String, int)}
     *
     * @param args host (Ip Adress of the server) and port
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            }
        }, 1L, 1L);
        int port = Properties.PORT; //default port
        createNewAiPlayer("localhost", 47345);


    }

    /**
     * Is called by the {@link AiMain#main(String[])}  for creating a new Ai object and connecting it to the server
     * Sends the ServerJoinRequest message
     *
     * @param host is the ip address of the server
     * @param port is the port number of the server
     * @throws IOException
     */
    protected static void createNewAiPlayer(String host, int port) throws IOException {
        ai.connect(host, port);
        //ai client name is AI Player
        //Create the ServerJoinRequest after connecting to the
        ai.connector.sendMessageToServer(new ServerJoinRequest("AI Player", ClientType.PLAYER));
    }

    /**
     * Is called by the {@link AiMessageHandler} when the game is finished or the connection failed.
     * (Closes the Ai because there is no sense for keeping it alive in these cases)
     */
    public static void close() {
        try {
            ai.connector.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //canceling the timer to stop the main method which keeps the ai alive
        timer.cancel();
        timer.purge();
    }


}
