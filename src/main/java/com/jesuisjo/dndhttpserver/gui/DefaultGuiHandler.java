package com.jesuisjo.dndhttpserver.gui;

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

public class DefaultGuiHandler extends AbstractGuiHandler {

    private final JFrame m_mainFrame;
    private final String m_appName;
    private final Image m_appIcon;
    private final SystemTray m_systemTray;
    private final Runnable m_onQuitAction;

    private TrayIcon m_trayIcon;

    public DefaultGuiHandler(JFrame mainFrame, String appName, Image appIcon, Runnable onViewPortSettingScreen, Runnable onQuitAction) {
        super(onViewPortSettingScreen, new PopupMenu(appName));

        m_mainFrame = mainFrame;
        m_appName = appName;
        m_appIcon = appIcon;
        m_onQuitAction = onQuitAction;
        m_systemTray = SystemTray.isSupported() ? SystemTray.getSystemTray() : null;
    }

    @Override
    public void installAppIcon(Image image) {
        m_mainFrame.setIconImage(image);
    }

    @Override
    public void installMenu() {
        MenuItem quitItem = new MenuItem("Quit " + m_appName);
        quitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_onQuitAction.run();
            }
        });

        PopupMenu popupMenu = getPopupMenu();
        popupMenu.add(buildChangePortItem());
        popupMenu.add(quitItem);
        popupMenu.addSeparator();
        popupMenu.add(NO_DIRECTORIES_REGISTERED_MENU_ITEM);

        if (m_systemTray != null) {
            // looks better that way instead of using setImageAutoSize
            Dimension trayIconSize = m_systemTray.getTrayIconSize();
            Image scaledAppIcon = m_appIcon.getScaledInstance(trayIconSize.width, trayIconSize.height, Image.SCALE_SMOOTH);
            TrayIcon trayIcon = new TrayIcon(scaledAppIcon, m_appName, popupMenu);
            trayIcon.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    m_mainFrame.setVisible(true);
                    m_mainFrame.setExtendedState(Frame.NORMAL);
                }
            });

            try {
                m_systemTray.add(trayIcon);
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
            m_trayIcon = trayIcon;
        }
    }

    @Override
    public void displayInfoNotification(String caption, String message) {
        if (m_trayIcon != null) {
            m_trayIcon.displayMessage(null, message, TrayIcon.MessageType.INFO);
        }
    }

    @Override
    public void displayErrorNotification(String caption, String message) {
        if (m_trayIcon != null) {
            m_trayIcon.displayMessage(null, message, TrayIcon.MessageType.ERROR);
        }
    }

    @Override
    public void installWindowBehavior() {
        m_mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
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
    }

    @Override
    public void dispose() {
        m_systemTray.remove(m_trayIcon);
    }
}