package com.jesuisjo.dndhttpserver;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.eventbus.EventBus;

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
import javax.swing.WindowConstants;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Gui {

    private static final String APP_NAME = "DnD Http server";
    private static final String UPLOAD_ARROW_CHAR = "\uF0AA";
    private final Logger m_logger = Logger.getLogger(getClass().toString());
    private final EventBus m_eventBus;
    private final Font m_iconFont;
    private final BufferedImage m_appIcon;
    // all the following variables should only be accessed by the GUI thread
    private JFrame m_mainFrame;
    private SystemTray m_systemTray;
    private TrayIcon m_trayIcon;

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

        BufferedImage appIcon = null;
        try (InputStream trayIconIs = Thread.currentThread().getContextClassLoader().getResourceAsStream("app-icon.png")) {
            appIcon = ImageIO.read(trayIconIs);
        } catch (IOException e) {
            m_logger.log(Level.SEVERE, "Unable to load the application icon", e);
        }
        m_appIcon = appIcon;
    }

    public void start() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (SystemTray.isSupported()) {
                    try {
                        m_systemTray = SystemTray.getSystemTray();
                        Dimension trayIconSize = m_systemTray.getTrayIconSize();
                        final PopupMenu popupMenu = new PopupMenu(APP_NAME);
                        final MenuItem changePortItem = new MenuItem("Change listening port");
                        final MenuItem quitItem = new MenuItem("Quit " + APP_NAME);

                        changePortItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                viewPortSettingScreen();
                            }
                        });
                        quitItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                m_eventBus.post(new QuitApplicationRequest());
                            }
                        });

                        popupMenu.add(changePortItem);
                        popupMenu.add(quitItem);

                        // looks better that way instead of using setImageAutoSize
                        Image scaledAppIcon = m_appIcon.getScaledInstance(trayIconSize.width, trayIconSize.height, Image.SCALE_SMOOTH);
                        TrayIcon trayIcon = new TrayIcon(scaledAppIcon, APP_NAME, popupMenu);
                        trayIcon.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                m_mainFrame.setVisible(true);
                                m_mainFrame.setExtendedState(Frame.NORMAL);
                            }
                        });

                        m_systemTray.add(trayIcon);
                        m_trayIcon = trayIcon;
                    } catch (AWTException e) {
                        m_logger.log(Level.SEVERE, "Can not install tray", e);
                    }
                }

                m_mainFrame = new JFrame(APP_NAME);
                m_mainFrame.setIconImage(m_appIcon);
                m_mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                m_mainFrame.setPreferredSize(new Dimension(300, 300));
                m_mainFrame.getRootPane().setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

                m_mainFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        m_mainFrame.setVisible(false);
                    }

                    @Override
                    public void windowIconified(WindowEvent e) {
                        m_mainFrame.setVisible(false);
                    }
                });

                JPanel droppingPanel = new JPanel(new BorderLayout());
                droppingPanel.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 6f, 3f, 3f, false));
                droppingPanel.setTransferHandler(
                        new

                                TransferHandler() {
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
            }
        });
    }

    public void notifyOfNewWebRoots(final Collection<Path> directories) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                displayInfoMessage("New web root directories registered :\n"
                        + Joiner.on("\n").join(Collections2.transform(directories, Functions.toStringFunction())));
            }
        });
    }

    public void notifyOfNewListeningPort(final int port) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                displayInfoMessage("The server is now listening on port " + port);
            }
        });
    }

    public void notifyOfUnderlyingServerError(final int port, final Exception cause) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                displayInfoMessage(String.format("The server was not able to (re) start on port %d because of en exception : %s", port, cause.getMessage()));
            }
        });
    }

    public void dispose() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_mainFrame.dispose();
                m_systemTray.remove(m_trayIcon);
            }
        });
    }

    private void displayInfoMessage(String message) {
        if (m_trayIcon != null) {
            m_trayIcon.displayMessage(null, message, TrayIcon.MessageType.INFO);
        }
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
}