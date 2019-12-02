package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.Handler;
import de.upb.codingpirates.battleships.logic.util.Client;
import de.upb.codingpirates.battleships.logic.util.Configuration;
import de.upb.codingpirates.battleships.network.message.notification.*;
import de.upb.codingpirates.battleships.network.message.report.ConnectionClosedReport;
import de.upb.codingpirates.battleships.network.message.response.*;

import java.io.IOException;
import java.util.Collection;

public class AiMessageHandler implements Handler {
    //int clientId;

    @Override
    public void handleGameInitNotification(GameInitNotification message){
        Configuration _config = message.getConfiguration();
        AiMain.ai.setConfig(_config);
        AiMain.ai.clientList = message.getClientList();
        try {
            AiMain.ai.placeShips(_config.getShipTypes());
        }catch (IOException e){
            e.printStackTrace();
        }
        //TODO Platzieren der Schiffe fehlt


    }

    @Override
    public void handleContinueNotification(ContinueNotification message) {

    }

    @Override
    public void handleConnectionClosedReport(ConnectionClosedReport message) {
    }

    @Override
    public void handleErrorNotification(ErrorNotification message) {

    }

    @Override
    public void handleFinishNotification(FinishNotification message) {

    }

    @Override
    public void handleGameJoinPlayer(GameJoinPlayerResponse message) {
        int _gameId = message.getGameId();
        AiMain.ai.setGameId(_gameId);
    }

    @Override
    public void handleGameJoinSpectator(GameJoinSpectatorResponse message) {

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

    }

    @Override
    public void handleLobbyResponse(LobbyResponse message) {

    }

    @Override
    public void handlePauseNotification(PauseNotification message) {

    }

    @Override
    public void handlePlaceShipsResponse(PlaceShipsResponse message) {

    }

    @Override
    public void handlePlayerUpdateNotification(PlayerUpdateNotification message) {
        AiMain.ai.updateNotification = message;
        //TODO eine logische sinnvolle reaktion auf die Aktionen der Gegner fehlt

    }

    @Override
    public void handleSpectatorUpdateNotification(SpectatorUpdateNotification message) {

    }

    @Override
    public void handlePointsResponse(PointsResponse message) {

    }

    @Override
    public void handleRemainingTimeResponse(RemainingTimeResponse message) {

    }

    @Override
    public void handleRoundStartNotification(RoundStartNotification message) {


    }

    @Override
    public void handleServerJoinResponse(ServerJoinResponse message) {
        AiMain.ai.clientId = message.getClientId();

    }

    @Override
    public void handleShotsResponse(ShotsResponse message) {

    }

    @Override
    public void handleSpectatorGameStateResponse(SpectatorGameStateResponse message) {

    }

    @Override
    public void handlePlayerGameStateResponse(PlayerGameStateResponse message) {

    }
}
