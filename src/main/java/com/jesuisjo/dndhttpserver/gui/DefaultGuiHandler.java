package com.jesuisjo.dndhttpserver.gui;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DefaultGuiHandler implements GuiPlatformSpecificHandler {

    private final JFrame m_mainFrame;
    private final SystemTray m_systemTray;
    private final String m_appName;
    private final Image m_appIcon;

    private TrayIcon m_trayIcon;

    public DefaultGuiHandler(JFrame mainFrame, String appName, Image appIcon) {
        m_mainFrame = mainFrame;
        m_systemTray = SystemTray.isSupported() ? SystemTray.getSystemTray() : null;
        m_appName = appName;
        m_appIcon = appIcon;
    }

    @Override
    public void installMenu(PopupMenu popupMenu) {
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
    public void setAppIcon(Image image) {
        m_mainFrame.setIconImage(image);
    }

    @Override
    public void dispose() {
        m_systemTray.remove(m_trayIcon);
    }

    @Override
    public void setupWindowBehavior(Runnable onQuitAction) {
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
}