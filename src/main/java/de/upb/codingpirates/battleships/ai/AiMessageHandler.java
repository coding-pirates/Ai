package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.ListenerHandler;
import de.upb.codingpirates.battleships.client.listener.*;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.network.exceptions.BattleshipException;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.network.message.response.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;



/**
 * Handles incoming messages from the server.
 *
 * @author Paul Becker
 * @author Benjamin Kasten
 */
public class AiMessageHandler extends ListenerHandler implements
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
        GameJoinPlayerResponseListener {

    private static final Logger logger = LogManager.getLogger();

    public AiMessageHandler() {
        ListenerHandler.registerListener(this);
    }

    public AiMessageHandler getInstance() {
        return AiMain.aiMessageHandler;
    }

    int aiClientId;


    @Override
    public void onConnectionClosedReport(ConnectionClosedReport message, int clientId) {
        logger.info("ConnectionClosedReport");
        AiMain.close();
    }

    @Override
    public void onBattleshipException(BattleshipException error, int clientId) {
        logger.error("BattleshipException");
        //error.printStackTrace();

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
            AiMain.ai.placeShotsRandom();
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
        logger.info("PlayerUpdateNotification");

        AiMain.ai.updateNotification = message;
        AiMain.ai.setHits(message.getHits());
        AiMain.ai.setPoints(message.getPoints());
        AiMain.ai.setSunk(message.getSunk());
        AiMain.ai.setSortedSunk(AiMain.ai.sortTheSunk()); //sortiere die sunks nach ihren Clients

    }

    @Override
    public void onRoundStartNotification(RoundStartNotification message, int clientId) {
        logger.info("RoundStartNotification");
        try {
            AiMain.ai.placeShotsRandom();
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
        //set gameId from the message to the ai , so that the ai knows the gameid
        logger.info("GameJoinPlayerResponse, GameId: " + message.getGameId());
        int gameId = message.getGameId();
        AiMain.ai.setGameId(gameId);

    }
}
