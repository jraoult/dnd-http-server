package com.jesuisjo.dndhttpserver.events;

public class ServerPortChangeFailed {

    private final int m_port;
    private final Exception m_cause;

    public ServerPortChangeFailed(int port, Exception cause) {
        m_port = port;
        m_cause = cause;
    }

    public int getPort() {
        return m_port;
    }

    public Exception getCause() {
        return m_cause;
    }
}