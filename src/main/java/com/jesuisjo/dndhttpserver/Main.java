package com.jesuisjo.dndhttpserver;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        final ExecutorService eventBusExecutor = Executors.newSingleThreadExecutor();
        EventBus eventBus = new AsyncEventBus(eventBusExecutor);

        final Server server = new Server(eventBus, 8080);
        final Gui gui = new Gui(eventBus);

        eventBus.register(new Object() {
            @Subscribe
            public void onWebRootDirectoriesAdded(WebRootDirectoriesAdded event) {
                server.addDirectory(event.getDirectories());
            }

            @Subscribe
            public void onServerHandlersAdded(final ServerStaticHandlersAdded event) {
                gui.notifyOfNewWebRoots(event.getDirectories());
            }

            @Subscribe
            public void onQuitApplicationRequested(QuitApplicationRequested event) {
                gui.dispose();
                server.stop();
                eventBusExecutor.shutdown();
            }
        });

        server.start();
        gui.start();
    }
}