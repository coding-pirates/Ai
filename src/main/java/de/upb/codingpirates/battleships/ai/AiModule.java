package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.Handler;
import de.upb.codingpirates.battleships.client.network.AbstractClientModule;
import de.upb.codingpirates.battleships.client.network.ClientConnector;

public class AiModule extends AbstractClientModule<ClientConnector> {

    public AiModule() {
        super(ClientConnector.class);
    }

    @Override
    protected void configure() {
        super.configure();

        this.bind(Handler.class).toInstance(new AiMessageHandler());
    }

}
