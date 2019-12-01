package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.logic.util.Client;
import de.upb.codingpirates.battleships.logic.util.ClientType;
import de.upb.codingpirates.battleships.logic.util.Configuration;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.request.ServerJoinRequest;

import java.io.IOException;
import java.util.*;

public class AiMain {
    static Timer timer = new Timer();

    static Ai ai = new Ai();

    public static void main(String[] args) throws IOException {

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            }
        }, 1L, 1L);
        createNewAiPlayer("localhost", 8008); //for building
        //createNewAiPlayer(args[0], Integer.parseInt(args[1])


    }

    public static void createNewAiPlayer(String host, int port) throws IOException {
        ai.aiConnect(host, port);
        ai.connector.sendMessageToServer(new ServerJoinRequest("AI Player", ClientType.PLAYER));
    }




}
