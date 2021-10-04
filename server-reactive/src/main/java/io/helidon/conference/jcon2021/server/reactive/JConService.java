package io.helidon.conference.jcon2021.server.reactive;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.helidon.common.configurable.ScheduledThreadPoolSupplier;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

class JConService implements Service {
    private final Integer sleepSeconds;
    private final ScheduledExecutorService scheduledExecutorService;

    JConService(Config config) {
        this.sleepSeconds = config.get("loom.sleep-seconds").asInt().orElse(1);
        this.scheduledExecutorService = ScheduledThreadPoolSupplier.builder()
                .corePoolSize(5)
                .prestart(true)
                .build()
                .get();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/slow", this::slow)
                .get("/quick", this::quick);
    }

    private void quick(ServerRequest req, ServerResponse res) {
        res.send("done");
    }

    private void slow(ServerRequest req, ServerResponse res) {
        scheduledExecutorService.schedule(() -> res.send("done"), sleepSeconds, TimeUnit.SECONDS);
    }
}
