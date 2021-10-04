
package io.helidon.conference.jcon2021.server;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/loom")
public class LoomResource {
    private final long sleepSeconds;

    @Inject
    public LoomResource(@ConfigProperty(name = "loom.sleep-seconds", defaultValue = "1") long sleepSeconds) {
        this.sleepSeconds = sleepSeconds;
    }

    /**
     * Slow request
     *
     * @return done
     */
    @GET
    @Path("/slow")
    public String slowRequest() {
        try {
            TimeUnit.SECONDS.sleep(sleepSeconds);
        } catch (InterruptedException ignored) {
            // ignored for the sake of the example
        }
        return "done";
    }

    /**
     * Quick request
     *
     * @return done immediately
     */
    @GET
    @Path("/quick")
    public String quickRequest() {
        return "done";
    }

    @GET
    @Path("/thread")
    public String thread() {
        return Thread.currentThread().getName() + " (" + Thread.currentThread().isVirtual() + ")";
    }
}
