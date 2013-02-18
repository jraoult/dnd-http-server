package com.jesuisjo.dndhttpserver.gui;

import javax.swing.JFrame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class AbstractGuiHandler implements GuiPlatformSpecificHandler {

    protected static final MenuItem NO_DIRECTORIES_REGISTERED_MENU_ITEM = new MenuItem("No web root registered yet");

    static {
        NO_DIRECTORIES_REGISTERED_MENU_ITEM.setEnabled(false);
    }

    private final NotificationOverlayPanel m_notificationOverlayPanel;
    private final JFrame m_mainFrame;
    private final Image m_appIcon;
    private final Runnable m_onQuitAction;
    private final Runnable m_viewPortSettingScreenCommand;
    private final PopupMenu m_popupMenu;

    protected AbstractGuiHandler(JFrame mainFrame, Image appIcon, NotificationOverlayPanel notificationOverlayPanel,
                                 PopupMenu popupMenu, Runnable viewPortSettingScreenCommand, Runnable onQuitAction) {
        m_viewPortSettingScreenCommand = viewPortSettingScreenCommand;
        m_popupMenu = popupMenu;
        m_notificationOverlayPanel = notificationOverlayPanel;
        m_onQuitAction = onQuitAction;
        m_appIcon = appIcon;
        m_mainFrame = mainFrame;
    }

    @Override
    public void addRemoveHandlerMenuItem(MenuItem menuItem) {
        m_popupMenu.add(menuItem);
    }

    @Override
    public void removeRemoveHandlerMenuItem(MenuItem menuItem) {
        m_popupMenu.remove(menuItem);
    }

    @Override
    public void showNoHandlerMenuItem() {
        m_popupMenu.add(NO_DIRECTORIES_REGISTERED_MENU_ITEM);
    }

    @Override
    public void hideNoHandlerMenuItem() {
        m_popupMenu.remove(NO_DIRECTORIES_REGISTERED_MENU_ITEM);
    }

    @Override
    public void displayInfo(String message) {
        m_notificationOverlayPanel.notifySuccess(message);
    }

    @Override
    public void displayError(String message) {
        m_notificationOverlayPanel.notifyError(message);
    }

    protected MenuItem buildChangePortMenuItem() {
        MenuItem changePortItem = new MenuItem("Change listening port");
        changePortItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_viewPortSettingScreenCommand.run();
            }
        });

        return changePortItem;
    }

    protected JFrame getMainFrame() {
        return m_mainFrame;
    }

    protected Image getAppIcon() {
        return m_appIcon;
    }

    protected Runnable getOnQuitAction() {
        return m_onQuitAction;
    }

    protected PopupMenu getPopupMenu() {
        return m_popupMenu;
    }
}