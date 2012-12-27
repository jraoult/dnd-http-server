package com.jesuisjo.dndhttpserver;


import com.google.common.eventbus.EventBus;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class Server {

    private static final String LISTENER_NAME = "dnd-http-server-listener";
    private final HttpServer m_httpServer = new HttpServer();
    private final EventBus m_eventBus;

    public Server(EventBus eventBus) {
        m_eventBus = eventBus;
    }

    public Server start(int port) throws IOException {
        addListener(port);
        m_httpServer.start();
        return this;
    }

    public Server changePort(int port) {
        m_httpServer.removeListener(LISTENER_NAME);
        addListener(port);
        m_eventBus.post(new ServerPortChanged(port));
        return this;
    }

    public Server addDirectory(Collection<Path> directories) {
        ServerConfiguration configuration = m_httpServer.getServerConfiguration();

        for (Path directory : directories) {
            StaticHttpHandler httpHandler = new StaticHttpHandler(directory.toString());
            configuration.addHttpHandler(httpHandler);
        }

        m_eventBus.post(new ServerStaticHandlersAdded(directories));

        return this;
    }

    public void stop() {
        m_httpServer.stop();
    }

    private void addListener(int port) {
        NetworkListener listener = new NetworkListener(LISTENER_NAME, "0.0.0.0", port);
        listener.getFileCache().setEnabled(false);
        m_httpServer.addListener(listener);
    }
}