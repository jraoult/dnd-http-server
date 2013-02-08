package com.jesuisjo.dndhttpserver.gui;

import com.apple.eawt.AppEvent;
import com.apple.eawt.AppReOpenedListener;
import com.apple.eawt.Application;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

import javax.swing.JFrame;
import java.awt.Image;
import java.awt.PopupMenu;

public class OsXGuiHandler extends AbstractGuiHandler {

    private final Application m_application = Application.getApplication();
    private final JFrame m_mainFrame;
    private final Runnable m_onQuitAction;

    public OsXGuiHandler(JFrame mainFrame, Runnable onViewPortSettingScreen, Runnable onQuitAction) {
        super(onViewPortSettingScreen, new PopupMenu());
        m_mainFrame = mainFrame;
        m_onQuitAction = onQuitAction;
    }

    @Override
    public void installAppIcon(Image image) {
        m_application.setDockIconImage(image);
    }

    @Override
    public void installWindowBehavior() {
        m_application.addAppEventListener(new AppReOpenedListener() {
            @Override
            public void appReOpened(AppEvent.AppReOpenedEvent appReOpenedEvent) {
                m_mainFrame.setVisible(true);
            }
        });

        m_application.setQuitHandler(new QuitHandler() {
            @Override
            public void handleQuitRequestWith(AppEvent.QuitEvent quitEvent, QuitResponse quitResponse) {
                m_onQuitAction.run();
                quitResponse.performQuit();
            }
        });
    }

    @Override
    public void installMenu() {
        PopupMenu popupMenu = getPopupMenu();
        popupMenu.add(buildChangePortItem());
        popupMenu.addSeparator();
        popupMenu.add(NO_DIRECTORIES_REGISTERED_MENU_ITEM);
        m_application.setDockMenu(popupMenu);
    }

    @Override
    public void displayInfoNotification(String caption, String message) {

    }

    @Override
    public void displayErrorNotification(String caption, String message) {

    }

    @Override
    public void dispose() {

    }
}