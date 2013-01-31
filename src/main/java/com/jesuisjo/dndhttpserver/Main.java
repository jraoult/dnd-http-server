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

        final Server server = new Server(eventBus);
        final Gui gui = new Gui(eventBus);

        eventBus.register(new Object() {
            @Subscribe
            public void onAddWebRootDirectoriesRequest(AddWebRootDirectoriesRequest request) {
                server.addDirectory(request.getDirectories());
            }

            @Subscribe
            public void onChangeListeningPortRequest(ChangeListeningPortRequest request) {
                server.changePort(request.getPort());
            }

            @Subscribe
            public void onQuitApplicationRequest(QuitApplicationRequest request) {
                gui.dispose();
                server.stop();
                eventBusExecutor.shutdown();
            }

            @Subscribe
            public void onServerHandlersAdded(ServerStaticHandlersAdded event) {
                gui.notifyOfNewWebRoots(event.getDirectories());
            }

            @Subscribe
            public void onServerPortChanged(ServerPortChanged event) {
                gui.notifyOfNewListeningPort(event.getPort());
            }

            @Subscribe
            public void onServerPortChangeFailed(ServerPortChangeFailed event) {
                gui.notifyOfUnderlyingServerError(event.getPort(), event.getCause());
            }

            @Subscribe
            public void onServerStartFailed(ServerStartFailed event) {
                gui.notifyOfUnderlyingServerError(event.getPort(), event.getCause());
            }
        });

        gui.start();
        server.start(8080);
    }
}