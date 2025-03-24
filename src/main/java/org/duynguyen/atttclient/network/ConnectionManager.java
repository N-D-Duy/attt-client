package org.duynguyen.atttclient.network;

import javafx.application.Platform;
import org.duynguyen.atttclient.presentation.widgets.ConnectionAlert;
import org.duynguyen.atttclient.utils.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionManager {
    private static final int MAX_RETRIES = 5;
    private static final int[] RETRY_DELAYS = {3, 5, 10, 15, 30}; 

    private final String host;
    private final int port;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private Session session;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ConnectionManager(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public CompletableFuture<Session> connect() {
        CompletableFuture<Session> future = new CompletableFuture<>();

        if (isConnecting.compareAndSet(false, true)) {
            retryCount.set(0);
            Platform.runLater(() -> ConnectionAlert.showConnecting(host, port));
            doConnect(future);
        } else {
            future.completeExceptionally(new IllegalStateException("Connection attempt already in progress"));
        }

        return future;
    }

    private void doConnect(CompletableFuture<Session> future) {
        try {
            session = new Session(host, port);
            boolean connected = session.connect();

            if (connected) {
                
                Platform.runLater(ConnectionAlert::showSuccess);
                isConnecting.set(false);
                future.complete(session);
            } else {
                
                handleConnectionFailure(future, "Could not connect to server");
            }
        } catch (Exception e) {
            
            handleConnectionFailure(future, e.getMessage());
        }
    }

    private void handleConnectionFailure(CompletableFuture<Session> future, String errorMessage) {
        int currentRetry = retryCount.incrementAndGet();
        Log.error("Connection attempt " + currentRetry + " failed: " + errorMessage);

        if (currentRetry < MAX_RETRIES) {
            int delaySeconds = RETRY_DELAYS[Math.min(currentRetry - 1, RETRY_DELAYS.length - 1)];
            Platform.runLater(() -> ConnectionAlert.showRetrying(host, port, currentRetry, MAX_RETRIES, delaySeconds));

            
            scheduler.schedule(() -> doConnect(future), delaySeconds, TimeUnit.SECONDS);
        } else {
            
            Platform.runLater(() -> ConnectionAlert.showFailed(host, port, () -> {
                retryCount.set(0);
                doConnect(future);
            }));
            isConnecting.set(false);
            future.completeExceptionally(new Exception("Failed to connect after " + MAX_RETRIES + " attempts"));
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
        if (session != null) {
            session.close();
        }
    }
}