package com.jesuisjo.dndhttpserver.gui;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class AbstractGuiHandler implements GuiPlatformSpecificHandler {

    protected static final MenuItem NO_DIRECTORIES_REGISTERED_MENU_ITEM = new MenuItem("No web root registered yet");

    static {
        NO_DIRECTORIES_REGISTERED_MENU_ITEM.setEnabled(false);
    }

    private final Runnable m_onViewPortSettingScreen;
    private final PopupMenu m_popupMenu;

    protected AbstractGuiHandler(Runnable onViewPortSettingScreen, PopupMenu popupMenu) {
        m_onViewPortSettingScreen = onViewPortSettingScreen;
        m_popupMenu = popupMenu;
    }

    public void addRemoveHandlerMenuItem(MenuItem menuItem) {
        m_popupMenu.add(menuItem);
    }

    public void removeRemoveHandlerMenuItem(MenuItem menuItem) {
        m_popupMenu.remove(menuItem);
    }

    public void showNoHandlerMenuItem() {
        m_popupMenu.add(DefaultGuiHandler.NO_DIRECTORIES_REGISTERED_MENU_ITEM);
    }

    public void hideNoHandlerMenuItem() {
        m_popupMenu.remove(DefaultGuiHandler.NO_DIRECTORIES_REGISTERED_MENU_ITEM);
    }

    protected MenuItem buildChangePortItem() {
        MenuItem changePortItem = new MenuItem("Change listening port");
        changePortItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_onViewPortSettingScreen.run();
            }
        });

        return changePortItem;
    }

    protected PopupMenu getPopupMenu() {
        return m_popupMenu;
    }

    protected Runnable getOnViewPortSettingScreen() {
        return m_onViewPortSettingScreen;
    }
}