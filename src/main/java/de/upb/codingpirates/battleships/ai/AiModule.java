package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.ListenerHandler;
import de.upb.codingpirates.battleships.client.network.AbstractClientModule;
import de.upb.codingpirates.battleships.client.network.ClientConnector;
import de.upb.codingpirates.battleships.network.ConnectionHandler;

public class AiModule<T extends ConnectionHandler> extends AbstractClientModule<ClientConnector> {

    public AiModule(Class<T> connectionHandler){
        super(ClientConnector.class);
    }

    @Override
    protected void configure() {
        super.configure();
        this.bind(ListenerHandler.class).toInstance(AiMain.aiMessageHandler.getInstance()); //todo correct
    }

}
