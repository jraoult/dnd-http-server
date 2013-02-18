package com.jesuisjo.dndhttpserver.events;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

import java.nio.file.Path;
import java.util.Collection;

public class ServerStaticHandlersRemoved {

    private final ImmutableList<Path> m_directories;

    public ServerStaticHandlersRemoved(Collection<Path> directories) {
        m_directories = ImmutableList.copyOf(directories);
    }

    public ImmutableCollection<Path> getDirectories() {
        return m_directories;
    }
}