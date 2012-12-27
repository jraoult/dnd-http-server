package com.jesuisjo.dndhttpserver;

public class ChangeListeningPortRequest {

    private final int m_port;

    public ChangeListeningPortRequest(int port) {
        m_port = port;
    }

    public int getPort() {
        return m_port;
    }
}