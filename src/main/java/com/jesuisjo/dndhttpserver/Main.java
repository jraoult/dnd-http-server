package com.jesuisjo.dndhttpserver;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.jesuisjo.dndhttpserver.events.AddWebRootDirectoriesRequest;
import com.jesuisjo.dndhttpserver.events.ChangeListeningPortRequest;
import com.jesuisjo.dndhttpserver.events.QuitApplicationRequest;
import com.jesuisjo.dndhttpserver.events.RemoveWebRootDirectoriesRequest;
import com.jesuisjo.dndhttpserver.events.ServerPortChangeFailed;
import com.jesuisjo.dndhttpserver.events.ServerPortChanged;
import com.jesuisjo.dndhttpserver.events.ServerStartFailed;
import com.jesuisjo.dndhttpserver.events.ServerStartSucceed;
import com.jesuisjo.dndhttpserver.events.ServerStaticHandlersAdded;
import com.jesuisjo.dndhttpserver.events.ServerStaticHandlersRemoved;
import com.jesuisjo.dndhttpserver.gui.Gui;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        // since the server is not thread safe, this executor is going to be used for all the method calls made onto the
        // like that we externally enforce single thread scope
        final ExecutorService backendTasksExecutor = Executors.newSingleThreadExecutor();

        EventBus eventBus = new AsyncEventBus(backendTasksExecutor);

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
                backendTasksExecutor.shutdown();
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
        }, backendTasksExecutor);
    }
}