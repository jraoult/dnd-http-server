package com.jesuisjo.dndhttpserver;


import com.google.common.eventbus.EventBus;
import com.jesuisjo.dndhttpserver.events.ServerPortChangeFailed;
import com.jesuisjo.dndhttpserver.events.ServerPortChanged;
import com.jesuisjo.dndhttpserver.events.ServerStartFailed;
import com.jesuisjo.dndhttpserver.events.ServerStartSucceed;
import com.jesuisjo.dndhttpserver.events.ServerStaticHandlersAdded;
import com.jesuisjo.dndhttpserver.events.ServerStaticHandlersRemoved;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A non thread safe http server facade.
 *
 * Should always be accessed by the same thread after instantiation.
 */
public class Server {

    private static final String LISTENER_NAME = "dnd-http-server-listener";

    private final HttpServer m_httpServer = new HttpServer();
    private final EventBus m_eventBus;
    private final Set<Path> m_paths = new HashSet<>();
    private final StaticHttpHandler m_staticHttpHandler = new StaticHttpHandler();

    public Server(EventBus eventBus) {
        m_eventBus = eventBus;
    }

    public Server start(int port) {
        m_httpServer.addListener(buildListener(port));
        try {
            m_httpServer.getServerConfiguration().addHttpHandler(m_staticHttpHandler);
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
        for (Path directory : directories) {

            // only add the handler if there was no mapping
            if (m_paths.add(directory)) {
                m_staticHttpHandler.addDocRoot(directory.toFile());
            }
        }

        m_eventBus.post(new ServerStaticHandlersAdded(directories));

        return this;
    }


    public Server removeDirectories(Collection<Path> directories) {
        for (Path directory : directories) {
            if (m_paths.remove(directory)) {
                m_staticHttpHandler.removeDocRoot(directory.toFile());
            }
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