package io.helidon.conference.jcon2021.client.quick;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.config.Config;
import io.helidon.webclient.WebClient;

import io.netty.handler.timeout.ReadTimeoutException;

/**
 * The application main class.
 */
public final class Main {
    private static final AtomicInteger SUCCESS = new AtomicInteger();
    private static final AtomicInteger TIMEOUT = new AtomicInteger();
    private static final AtomicInteger ERROR = new AtomicInteger();

    /**
     * Cannot be instantiated.
     */
    private Main() {
    }

    /**
     * Application main entry point.
     * @param args command line arguments.
     */
    public static void main(final String[] args) throws InterruptedException {
        Config config = Config.create().get("loom");

        int threadCount = config.get("threads").asInt().orElse(5);
        int repeats = config.get("repeats").asInt().orElse(10_000);
        long monitorPeriodSeconds = config.get("monitor.period-seconds").asLong().orElse(1L);
        boolean monitorPrintHeader = config.get("monitor.print-header").asBoolean().orElse(true);
        int monitorPrintHeaderEvery = config.get("monitor.print-header-lines").asInt().orElse(10);

        // this is a mandatory configuration option
        String uri = config.get("client.uri").asString().get();

        WebClient webClient = WebClient.create(config.get("client"));

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < repeats; j++) {
                    run(webClient);
                }
            });
        }

        // start monitoring
        Monitor monitor = new Monitor(uri, monitorPeriodSeconds, monitorPrintHeader, monitorPrintHeaderEvery);
        Thread monitorThread = new Thread(monitor);
        monitorThread.setDaemon(true);
        monitorThread.start();


        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        monitor.finish();
    }

    private static void run(WebClient webClient) {
        try {
            webClient.get()
                    .request(String.class)
                    .await(10, TimeUnit.SECONDS);
            SUCCESS.incrementAndGet();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ReadTimeoutException) {
                TIMEOUT.incrementAndGet();
            } else {
                ERROR.incrementAndGet();
            }
        }
    }

    private static class Monitor implements Runnable {
        private final String uri;
        private final long sleepSeconds;
        private final boolean printHeader;
        private final int printHeaderEvery;
        private volatile boolean finish;
        private final CountDownLatch finishLatch = new CountDownLatch(1);
        private int lastCount = 0;
        private long lastRun;
        private int printCounter;

        private Monitor(String uri, long sleepSeconds, boolean printHeader, int printHeaderEvery) {
            this.uri = uri;
            this.sleepSeconds = sleepSeconds;
            this.printHeader = printHeader;
            this.printHeaderEvery = printHeaderEvery;
        }

        @Override
        public void run() {
            if (printHeader) {
                System.out.println("Monitoring requests to " + uri);
            }
            header();
            lastRun = System.nanoTime();
            while(!finish) {
                try {
                    TimeUnit.SECONDS.sleep(sleepSeconds);
                } catch (InterruptedException e) {
                    // we were interrupted, no need to move forward
                    return;
                }
                if (!finish) {
                    print();
                }
            }
            finishLatch.countDown();
        }

        private void header() {
            if (printHeader) {
                System.out.println("    Req/s    Success  Timeout  Error");
            }
        }

        private void print() {
            int currentCount = SUCCESS.get();
            long now = System.nanoTime();

            int countLambda = currentCount - lastCount;
            long timeLambda = now - lastRun;
            long timeLambdaSeconds = TimeUnit.NANOSECONDS.toSeconds(timeLambda);
            double perSecond = 0;
            if (timeLambdaSeconds != 0) {
                perSecond = (double) countLambda / timeLambdaSeconds;
            }

            System.out.printf("%9.2f %10d %8d %6d%n", perSecond, SUCCESS.get(), TIMEOUT.get(), ERROR.get());
            lastCount = currentCount;
            lastRun = now;
            printCounter++;
            if (printCounter % printHeaderEvery == 0) {
                header();
            }
        }

        private void finish() throws InterruptedException {
            finish = true;
            finishLatch.await();
            print();
            System.out.println("Finished");
        }
    }
}
