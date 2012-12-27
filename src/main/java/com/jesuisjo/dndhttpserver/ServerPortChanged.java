package com.jesuisjo.dndhttpserver;

public class ServerPortChanged {

    private final int m_port;

    public ServerPortChanged(int port) {
        m_port = port;
    }

    public int getPort() {
        return m_port;
    }
}
