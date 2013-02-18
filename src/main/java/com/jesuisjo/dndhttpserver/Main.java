package com.jesuisjo.dndhttpserver;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.MoreExecutors;

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
                server.addDirectories(request.getDirectories());
            }

            @Subscribe
            public void onRemoveWebRootDirectoriesRequest(RemoveWebRootDirectoriesRequest request) {
                server.removeDirectories(request.getDirectories());
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
                gui.addRemoveHandlerButtons(event.getDirectories());
                gui.notifyOfNewWebRoots(event.getDirectories());
            }

            @Subscribe
            public void onServerHandlersRemoved(ServerStaticHandlersRemoved event) {
                gui.removeRemoveHandlerButtons(event.getDirectories());
                gui.notifyOfRemovedWebRoots(event.getDirectories());
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
            public void onServerStartSucceed(ServerStartSucceed event) {
                gui.notifyOfServerStart(event.getPort());
            }

            @Subscribe
            public void onServerStartFailed(ServerStartFailed event) {
                gui.notifyOfUnderlyingServerError(event.getPort(), event.getCause());
            }
        });

        // starts the gui and wait for it to be totally initialized before starting the server
        // it allows the gui the behave correctly even if the server is really quick to start
        gui.start().addListener(new Runnable() {
            @Override
            public void run() {
                server.start(8080);
            }
        }, MoreExecutors.sameThreadExecutor());
    }
}