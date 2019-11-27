package de.upb.codingpirates.battleships.ai;

import de.upb.codingpirates.battleships.client.AbstractClientModule;
import de.upb.codingpirates.battleships.client.Handler;

public class AiModule extends AbstractClientModule {
    @Override
    protected void configure() {
        super.configure();

        this.bind(Handler.class).toInstance(new Hnadler());
    }
}
