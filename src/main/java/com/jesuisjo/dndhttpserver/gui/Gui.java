package com.jesuisjo.dndhttpserver.gui;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableCollection;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.jesuisjo.dndhttpserver.events.AddWebRootDirectoriesRequest;
import com.jesuisjo.dndhttpserver.events.ChangeListeningPortRequest;
import com.jesuisjo.dndhttpserver.events.QuitApplicationRequest;
import com.jesuisjo.dndhttpserver.events.RemoveWebRootDirectoriesRequest;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.MenuItem;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Gui {

    private static final String APP_NAME = "DnD Http server";
    private static final String UPLOAD_ARROW_CHAR = "\uF0AA";

    private final Logger m_logger = Logger.getLogger(getClass().toString());

    private final EventBus m_eventBus;
    private final Font m_iconFont;
    private final BufferedImage m_appIcon;
    private final BufferedImage m_appIconSmall;
    private final ScheduledExecutorService m_fxQueueEventLoopExecutor = Executors.newSingleThreadScheduledExecutor();

    // all the following variables should only be accessed by the GUI thread
    private JFrame m_mainFrame;
    private GuiPlatformSpecificHandler m_guiHandler;

    private Map<Path, MenuItem> m_menuItemByPath = new HashMap<>();

    public Gui(EventBus eventBus) {
        m_eventBus = eventBus;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            m_logger.log(Level.WARNING, "Unable to install the look and feel", e);
        }

        Font iconFont = null;
        try (InputStream fontIs = Thread.currentThread().getContextClassLoader().getResourceAsStream("fontello.ttf")) {
            iconFont = Font.createFont(Font.TRUETYPE_FONT, fontIs);
        } catch (IOException | FontFormatException e) {
            m_logger.log(Level.SEVERE, "Unable to load a font ", e);
        }
        m_iconFont = iconFont;

        m_appIcon = buildIcon("app-icon.png");
        m_appIconSmall = buildIcon("app-icon-small.png");
    }

    /**
     * Initializes and displays the GUI.
     *
     * @return a future that is resolved when the GUI is ready
     */
    public ListenableFuture<Void> start() {
        final SettableFuture<Void> future = SettableFuture.create();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Runnable viewPortSettingScreenCommand = new Runnable() {
                    @Override
                    public void run() {
                        viewPortSettingScreen();
                    }
                };
                final Runnable onQuitAction = new Runnable() {
                            @Override
                            public void run() {
                                m_eventBus.post(new QuitApplicationRequest());
                            }
                        };

                m_mainFrame = new JFrame(APP_NAME);
                NotificationOverlayPanel notificationOverlayPanel = new NotificationOverlayPanel(m_fxQueueEventLoopExecutor);
                notificationOverlayPanel.install(m_mainFrame);

                try {
                    // is the apple extension available ?
                    Class.forName("com.apple.eawt.Application");
                    m_guiHandler = new OsXGuiHandler(m_mainFrame, m_appIcon, onQuitAction, viewPortSettingScreenCommand, notificationOverlayPanel);
                } catch (ClassNotFoundException e) {
                    m_guiHandler = new DefaultGuiHandler(m_mainFrame, APP_NAME, m_appIcon, m_appIconSmall, onQuitAction, viewPortSettingScreenCommand, notificationOverlayPanel);
                }

                m_guiHandler.install();

                m_mainFrame.setPreferredSize(new Dimension(300, 150));

                JPanel droppingPanel = new JPanel(new BorderLayout());
                droppingPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6), BorderFactory.createDashedBorder(Color.GRAY, 6f, 3f, 3f, false)));
                droppingPanel.setTransferHandler(
                        new TransferHandler() {
                            @Override
                            public int getSourceActions(JComponent c) {
                                return COPY_OR_MOVE;
                            }

                            @Override
                            public boolean canImport(TransferSupport support) {
                                try {
                                    return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
                                } catch (InvalidDnDOperationException e) {
                                    // implementation bug, on last call before drop, it is not possible to access the data
                                    // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6759788
                                    return true;
                                }
                            }

                            @Override
                            public boolean importData(TransferSupport support) {
                                try {
                                    Collection<File> directories = filterDirectories(support);

                                    if (directories.isEmpty()) {
                                        return false;
                                    }

                                    m_eventBus.post(new AddWebRootDirectoriesRequest(Collections2.transform(directories, new Function<File, Path>() {
                                        @Override
                                        public Path apply(@javax.annotation.Nullable File file) {
                                            return file.toPath();
                                        }
                                    })));

                                    return true;
                                } catch (UnsupportedFlavorException | IOException e) {
                                    m_logger.log(Level.SEVERE, "Unable to handle drop operation", e);
                                    return false;
                                }
                            }

                            private Collection<File> filterDirectories(TransferSupport support) throws UnsupportedFlavorException, IOException {
                                return Collections2.filter((List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor), new Predicate<File>() {
                                    @Override
                                    public boolean apply(@Nullable File file) {
                                        return file.isDirectory();
                                    }
                                });
                            }
                        });

                JLabel infoLabel = new JLabel("Drop web root directories here");
                infoLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
                infoLabel.setFont(infoLabel.getFont().deriveFont(16f));
                infoLabel.setForeground(Color.GRAY);

                droppingPanel.add(infoLabel, BorderLayout.NORTH);

                if (m_iconFont != null) {
                    JLabel iconLabel = new JLabel();
                    iconLabel.setFont(m_iconFont.deriveFont(60f));
                    iconLabel.setForeground(Color.GRAY);
                    iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    iconLabel.setText(UPLOAD_ARROW_CHAR);

                    droppingPanel.add(iconLabel);
                }

                m_mainFrame.add(droppingPanel);
                m_mainFrame.pack();
                m_mainFrame.setVisible(true);

                future.set(null);
            }
        });

        return future;
    }

    public void addRemoveHandlerButtons(final ImmutableCollection<Path> directories) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final Path directory : directories) {
                    MenuItem menuItem = new MenuItem("Unregister " + directory.toString());
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            m_eventBus.post(new RemoveWebRootDirectoriesRequest(Collections.singleton(directory)));
                        }
                    });
                    m_menuItemByPath.put(directory, menuItem);
                    m_guiHandler.addRemoveHandlerMenuItem(menuItem);
                }

                // remove the empty message from the menu
                if (!m_menuItemByPath.isEmpty()) {
                    m_guiHandler.hideNoHandlerMenuItem();
                }
            }
        });
    }

    public void removeRemoveHandlerButtons(final ImmutableCollection<Path> directories) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (Path directory : directories) {
                    m_guiHandler.removeRemoveHandlerMenuItem(m_menuItemByPath.remove(directory));
                }

                // add the empty message if there is no mapping anymore
                if (m_menuItemByPath.isEmpty()) {
                    m_guiHandler.showNoHandlerMenuItem();
                }
            }
        });
    }

    public void notifyOfNewWebRoots(final Collection<Path> directories) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_guiHandler.displayInfo("New web root directories registered :\n"
                        + Joiner.on("\n").join(Collections2.transform(directories, Functions.toStringFunction())));
            }
        });
    }

    public void notifyOfRemovedWebRoots(final Collection<Path> directories) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_guiHandler.displayInfo("Web root directories unregistered :\n"
                        + Joiner.on("\n").join(Collections2.transform(directories, Functions.toStringFunction())));
            }
        });
    }

    public void notifyOfNewListeningPort(final int port) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_guiHandler.displayInfo("The server is now listening on port " + port);
            }
        });
    }

    public void notifyOfServerStart(final int port) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_guiHandler.displayInfo("The server successfully started and is now listening on port " + port);
            }
        });
    }

    public void notifyOfUnderlyingServerError(final int port, final Exception cause) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_guiHandler.displayError(String.format("The server was not able to (re) start on port %d because of en exception : %s", port, cause.getMessage()));
            }
        });
    }

    public void dispose() {
        m_fxQueueEventLoopExecutor.shutdown();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_mainFrame.dispose();
                m_guiHandler.dispose();
            }
        });
    }

    private void viewPortSettingScreen() {
        String portStr = JOptionPane.showInputDialog("Please enter a new port number");
        if (!Strings.isNullOrEmpty(portStr)) {
            try {
                m_eventBus.post(new ChangeListeningPortRequest(Integer.parseInt(portStr)));
            } catch (NumberFormatException e) {

            }
        }
    }

    private BufferedImage buildIcon(String pathName) {
        BufferedImage appIcon = null;
        try (InputStream trayIconIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathName)) {
            return ImageIO.read(trayIconIs);
        } catch (IOException e) {
            m_logger.log(Level.SEVERE, "Unable to load the application icon", e);
        }
        return appIcon;
    }
}