package com.jesuisjo.dndhttpserver;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Gui {

    private static final String APP_NAME = "DnD Http server";
    private static final String UPLOAD_ARROW_CHAR = "\uE75B";
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
                        final MenuItem quitItem = new MenuItem("Quit " + APP_NAME);

                        quitItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                m_eventBus.post(new QuitApplicationRequested());
                            }
                        });
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
                droppingPanel.setBorder(BorderFactory.createDashedBorder(Color.LIGHT_GRAY, 6f, 3f, 3f, false));
                droppingPanel.setTransferHandler(
                        new

                                TransferHandler() {
                                    @Override
                                    public int getSourceActions(JComponent c) {
                                        return COPY_OR_MOVE;
                                    }

                                    @Override
                                    public boolean canImport(TransferSupport support) {
                                        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
                                    }

                                    @Override
                                    public boolean importData(TransferSupport support) {
                                        try {
                                            List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                                            if (files.isEmpty()) {
                                                return false;
                                            }

                                            m_eventBus.post(new WebRootDirectoriesAdded(Lists.transform(files, new Function<File, Path>() {
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


                                });

                JLabel infoLabel = new JLabel("Drop web root directories here");
                infoLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
                infoLabel.setFont(infoLabel.getFont().deriveFont(16f));
                infoLabel.setForeground(Color.LIGHT_GRAY);

                droppingPanel.add(infoLabel, BorderLayout.NORTH);

                if (m_iconFont != null) {
                    JLabel iconLabel = new JLabel();
                    iconLabel.setFont(m_iconFont.deriveFont(60f));
                    iconLabel.setForeground(Color.LIGHT_GRAY);
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

    public void notifyOfNewWebRoots(final List<Path> directories) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (m_trayIcon != null) {
                    m_trayIcon.displayMessage(null, "New web root directories registered :\n"
                            + Joiner.on("\n").join(Lists.transform(directories, Functions.toStringFunction())),
                            TrayIcon.MessageType.INFO);
                }
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
}
