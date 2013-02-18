package com.jesuisjo.dndhttpserver.events;

public class ServerStartSucceed {

    private final int m_port;

    public ServerStartSucceed(int port) {
        m_port = port;
    }

    public int getPort() {
        return m_port;
    }
}
