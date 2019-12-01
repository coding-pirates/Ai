package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.Handler;
import de.upb.codingpirates.battleships.client.network.AbstractClientModule;

public class AiModule extends AbstractClientModule {
    @Override
    protected void configure() {
        super.configure();

        this.bind(Handler.class).toInstance(new AiMessageHandler());
    }
}
