package com.jesuisjo.dndhttpserver;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class ServerStaticHandlersAdded {

    private final ImmutableList<Path> m_directories;

    public ServerStaticHandlersAdded(Collection<Path> directories) {
        m_directories = ImmutableList.copyOf(directories);
    }

    public ImmutableCollection<Path> getDirectories() {
        return m_directories;
    }
}