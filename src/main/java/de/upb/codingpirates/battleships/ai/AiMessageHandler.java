package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.Handler;
import de.upb.codingpirates.battleships.logic.util.Configuration;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.network.message.response.*;

import java.io.IOException;

public class AiMessageHandler implements Handler {
    int clientId;

    @Override
    public void handleGameInitNotification(GameInitNotification message) {
        Configuration config = message.getConfiguration();
        AiMain.ai.setConfig(config);
        //AiMain.ai.clientList = message.getClientList();
        AiMain.ai.setClientList(message.getClientList());
        try {
            //no need for handing over a shipConfiguration because it is set by the setConfig method
            AiMain.ai.placeShips();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void handleContinueNotification(ContinueNotification message) {
        //n.a.

    }

    @Override
    public void handleConnectionClosedReport(ConnectionClosedReport message) {
        //n.a.
    }

    @Override
    public void handleErrorNotification(ErrorNotification message) {
        //TODO soll die ai auf error Not. reagieren?

    }

    @Override
    public void handleFinishNotification(FinishNotification message) {
        //close connection and stop main method
        AiMain.close();

    }

    @Override
    public void handleGameJoinPlayer(GameJoinPlayerResponse message) {
        //set gameId from the message to the ai , so that the ai knows the gameid
        int gameId = message.getGameId();
        AiMain.ai.setGameId(gameId);
    }

    @Override
    public void handleGameJoinSpectator(GameJoinSpectatorResponse message) {
        //n.a.

    }

    @Override
    public void handleGameStartNotification(GameStartNotification message) {
        try {
            AiMain.ai.placeShots();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleLeaveNotification(LeaveNotification message) {
        //TODO den entsprechenden Client aus der clientList entfernen und
    }

    @Override
    public void handleLobbyResponse(LobbyResponse message) {
        // AIs werden vom Server (bzw. vom Ausrichter) direkt in ein Spiel geladen,
        // sodass sie sich nicht in der lobby
        // anmelden müsssen

    }

    @Override
    public void handlePauseNotification(PauseNotification message) {
        //n.a.

    }

    @Override
    public void handlePlaceShipsResponse(PlaceShipsResponse message) {
        //n.a. AI muss nicht auf korrekte Schiffsplatzierung reagieren
    }

    @Override
    public void handlePlayerUpdateNotification(PlayerUpdateNotification message) {
        //immer wenn es eine neue notification gibt, wird diese im ai model neu gesetzt bzw geupdated
        AiMain.ai.updateNotification = message;

        //TODO eine logische sinnvolle reaktion auf die Aktionen der Gegner fehlt

    }

    @Override
    public void handleSpectatorUpdateNotification(SpectatorUpdateNotification message) {
        //n.a.

    }

    @Override
    public void handlePointsResponse(PointsResponse message) {
        //n.a.

    }

    @Override
    public void handleRemainingTimeResponse(RemainingTimeResponse message) {
        //n.a.

    }

    @Override
    public void handleRoundStartNotification(RoundStartNotification message) {
        //TODO bei neuer RoundStartNotifitcation erneut placShot Mathode aufrufen

        try {
            AiMain.ai.placeShots();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void handleServerJoinResponse(ServerJoinResponse message) {
        clientId = message.getClientId();
        AiMain.ai.clientId = clientId;
    }

    @Override
    public void handleShotsResponse(ShotsResponse message) {
        //n.a.

    }

    @Override
    public void handleSpectatorGameStateResponse(SpectatorGameStateResponse message) {
        //n.a.

    }

    @Override
    public void handlePlayerGameStateResponse(PlayerGameStateResponse message) {
        //n.a.

    }
}
