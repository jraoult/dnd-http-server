package com.jesuisjo.dndhttpserver;


import com.google.common.eventbus.EventBus;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Server {

    private static final String LISTENER_NAME = "dnd-http-server-listener";

    private final HttpServer m_httpServer = new HttpServer();
    private final EventBus m_eventBus;
    private final ConcurrentMap<Path, StaticHttpHandler> m_handlerByPath = new ConcurrentHashMap<>();

    public Server(EventBus eventBus) {
        m_eventBus = eventBus;
    }

    public Server start(int port) {
        m_httpServer.addListener(buildListener(port));
        try {
            m_httpServer.start();
            m_eventBus.post(new ServerStartSucceed(port));
        } catch (IOException e) {
            m_eventBus.post(new ServerStartFailed(port, e));
        }
        return this;
    }

    public Server changePort(int port) {
        m_httpServer.stop();
        m_httpServer.removeListener(LISTENER_NAME);
        m_httpServer.addListener(buildListener(port));
        try {
            m_httpServer.start();
            m_eventBus.post(new ServerPortChanged(port));
        } catch (IOException e) {
            m_eventBus.post(new ServerPortChangeFailed(port, e));
        }
        return this;
    }

    public Server addDirectories(Collection<Path> directories) {
        ServerConfiguration configuration = m_httpServer.getServerConfiguration();

        for (Path directory : directories) {
            StaticHttpHandler httpHandler = new StaticHttpHandler(directory.toString());

            // only add the handler if there was no mapping
            if (m_handlerByPath.putIfAbsent(directory, httpHandler) == null) {
                configuration.addHttpHandler(httpHandler);
            }
        }

        m_eventBus.post(new ServerStaticHandlersAdded(directories));

        return this;
    }


    public Server removeDirectories(Collection<Path> directories) {
        ServerConfiguration configuration = m_httpServer.getServerConfiguration();

        for (Path directory : directories) {
            configuration.removeHttpHandler(m_handlerByPath.remove(directory));
        }

        m_eventBus.post(new ServerStaticHandlersRemoved(directories));

        return this;
    }

    public void stop() {
        m_httpServer.stop();
    }

    private NetworkListener buildListener(int port) {
        NetworkListener listener = new NetworkListener(LISTENER_NAME, NetworkListener.DEFAULT_NETWORK_HOST, port);
        listener.getFileCache().setEnabled(false);
        return listener;
    }
}