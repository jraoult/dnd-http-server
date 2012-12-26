package com.jesuisjo.dndhttpserver;


import com.google.common.eventbus.EventBus;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Server {

    private final HttpServer m_httpServer = new HttpServer();
    private final EventBus m_eventBus;
    private final int m_port;

    public Server(EventBus eventBus, int port) {
        m_eventBus = eventBus;
        m_port = port;
    }

    public Server start() throws IOException {
        NetworkListener listener = new NetworkListener("dnd-http-server", "0.0.0.0", m_port);
        listener.getFileCache().setEnabled(false);
        m_httpServer.addListener(listener);
        m_httpServer.start();
        return this;
    }

    public Server addDirectory(List<Path> directories) {
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
}