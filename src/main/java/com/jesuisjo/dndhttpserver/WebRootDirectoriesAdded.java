package com.jesuisjo.dndhttpserver;

import com.google.common.collect.ImmutableList;

import java.nio.file.Path;
import java.util.List;

public class WebRootDirectoriesAdded {

    private final ImmutableList<Path> m_directories;

    public WebRootDirectoriesAdded(List<Path> directories) {
        m_directories = ImmutableList.copyOf(directories);
    }

    public ImmutableList<Path> getDirectories() {
        return m_directories;
    }
}