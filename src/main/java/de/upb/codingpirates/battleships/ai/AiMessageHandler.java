package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.Handler;
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
public class AiMessageHandler implements Handler {
    Logger logger = LogManager.getLogger();

    int aiClientId;

    @Override
    public void handleGameInitNotification(GameInitNotification message, int id) {
        Configuration config = message.getConfiguration();
        AiMain.ai.setConfig(config);
        //AiMain.ai.clientList = message.getClientList();
        AiMain.ai.setClientArrayList(message.getClientList());
        try {
            //no need for handing over a shipConfiguration because it is set by the setConfig method
            AiMain.ai.placeShips();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void handleContinueNotification(ContinueNotification message, int id) {
        //n.a.

    }

    @Override
    public void handleConnectionClosedReport(ConnectionClosedReport message, int id) {
        AiMain.close();
    }

    @Override
    public void handleErrorNotification(ErrorNotification message, int id) {
        //TODO soll die ai auf error Not. reagieren?

    }

    @Override
    public void handleFinishNotification(FinishNotification message, int id) {
        //close connection and stop main method
        AiMain.close();

    }

    @Override
    public void handleGameJoinPlayer(GameJoinPlayerResponse message, int id) {
        //set gameId from the message to the ai , so that the ai knows the gameid
        int gameId = message.getGameId();
        AiMain.ai.setGameId(gameId);
    }

    @Override
    public void handleGameJoinSpectator(GameJoinSpectatorResponse message, int id) {
        //n.a.

    }

    @Override
    public void handleGameStartNotification(GameStartNotification message, int id) {
        try {
            AiMain.ai.placeShotsRandom();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleLeaveNotification(LeaveNotification message, int id) {
        int leftPlayerId = message.getPlayerId();
        AiMain.ai.handleLeaveOfPlayer(leftPlayerId);

    }

    @Override
    public void handleLobbyResponse(LobbyResponse message, int id) {
        // AIs werden vom Server (bzw. vom Ausrichter) direkt in ein Spiel geladen,
        // sodass sie sich nicht in der lobby
        // anmelden müsssen

    }

    @Override
    public void handlePauseNotification(PauseNotification message, int id) {
        //n.a.

    }

    @Override
    public void handlePlaceShipsResponse(PlaceShipsResponse message, int id) {
        //n.a. AI muss nicht auf korrekte Schiffsplatzierung reagieren
    }

    @Override
    public void handlePlayerUpdateNotification(PlayerUpdateNotification message, int id) {
        //immer wenn es eine neue notification gibt, wird diese im ai model neu gesetzt bzw geupdated
        AiMain.ai.updateNotification = message;

        AiMain.ai.setHits(message.getHits());
        AiMain.ai.setPoints(message.getPoints());
        AiMain.ai.setSunk(message.getSunk());
        AiMain.ai.setSortedSunk(AiMain.ai.sortTheSunk()); //sortiere die sunks nach ihren Clients

    }

    @Override
    public void handleSpectatorUpdateNotification(SpectatorUpdateNotification message, int id) {
        //n.a.

    }

    @Override
    public void handlePointsResponse(PointsResponse message, int id) {
        //n.a.

    }

    @Override
    public void handleRemainingTimeResponse(RemainingTimeResponse message, int id) {
        //n.a.

    }

    @Override
    public void handleRoundStartNotification(RoundStartNotification message, int id) {
        try {
            AiMain.ai.placeShotsRandom();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void handleServerJoinResponse(ServerJoinResponse message, int id) {

        AiMain.ai.setAiClientId(message.getClientId());
    }

    @Override
    public void handleShotsResponse(ShotsResponse message, int id) {
        //n.a.

    }

    @Override
    public void handleSpectatorGameStateResponse(SpectatorGameStateResponse message, int id) {
        //n.a.

    }

    @Override
    public void handlePlayerGameStateResponse(PlayerGameStateResponse message, int id) {

    }

    @Override
    public void handleBattleshipException(BattleshipException exception, int clientId) {
        //n.a
    }
}
