package com.jesuisjo.dndhttpserver.gui;

import com.google.common.collect.ImmutableList;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The default implementation of gui handler. It can be used as is for Windows and Linux.
 */
public class DefaultGuiHandler extends AbstractGuiHandler {

    private final Logger m_logger = Logger.getLogger(getClass().toString());
    private final String m_appName;
    private final Image m_appIconSmall;
    private final SystemTray m_systemTray;
    private TrayIcon m_trayIcon;

    public DefaultGuiHandler(JFrame mainFrame, String appName, Image appIcon, Image appIconSmall, Runnable onQuitAction, Runnable onViewPortSettingScreen, NotificationOverlayPanel notificationOverlayPanel) {
        super(mainFrame, appIcon, notificationOverlayPanel, new PopupMenu(appName), onViewPortSettingScreen, onQuitAction);
        m_appName = appName;
        m_appIconSmall = appIconSmall;
        m_systemTray = SystemTray.isSupported() ? SystemTray.getSystemTray() : null;
    }

    @Override
    public void install() {
        getMainFrame().setIconImages(ImmutableList.of(getAppIcon(), m_appIconSmall));

        MenuItem quitItem = new MenuItem("Quit " + m_appName);
        quitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getOnQuitAction().run();
            }
        });

        PopupMenu popupMenu = getPopupMenu();
        popupMenu.add(buildChangePortMenuItem());
        popupMenu.add(quitItem);
        popupMenu.addSeparator();
        popupMenu.add(NO_DIRECTORIES_REGISTERED_MENU_ITEM);

        if (m_systemTray != null) {
            // looks better that way instead of using setImageAutoSize
            Dimension trayIconSize = m_systemTray.getTrayIconSize();
            Image scaledAppIcon = getAppIcon().getScaledInstance(trayIconSize.width, trayIconSize.height, Image.SCALE_SMOOTH);
            TrayIcon trayIcon = new TrayIcon(scaledAppIcon, m_appName, popupMenu);
            trayIcon.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    getMainFrame().setVisible(true);
                    getMainFrame().setExtendedState(Frame.NORMAL);
                }
            });

            try {
                m_systemTray.add(trayIcon);
            } catch (AWTException e) {
                m_logger.log(Level.SEVERE, "Unable to install the tray icon", e);
            }
            m_trayIcon = trayIcon;
        }

        getMainFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        getMainFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                getMainFrame().setVisible(false);
            }

            @Override
            public void windowIconified(WindowEvent e) {
                getMainFrame().setVisible(false);
            }
        });
    }

    @Override
    public void dispose() {
        m_systemTray.remove(m_trayIcon);
    }
}