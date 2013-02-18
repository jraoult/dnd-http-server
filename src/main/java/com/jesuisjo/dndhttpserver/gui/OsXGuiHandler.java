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
    private final Image m_appIcon;

    public OsXGuiHandler(JFrame mainFrame, Image appIcon, Runnable onQuitAction, Runnable viewPortSettingScreenCommand, NotificationOverlayPanel notificationOverlayPanel) {
        super(mainFrame, appIcon, notificationOverlayPanel, new PopupMenu(), viewPortSettingScreenCommand, onQuitAction);
        m_mainFrame = mainFrame;
        m_appIcon = appIcon;
        m_onQuitAction = onQuitAction;
    }

    @Override
    public void install() {
        m_application.setDockIconImage(m_appIcon);

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

        PopupMenu popupMenu = getPopupMenu();
        popupMenu.add(buildChangePortMenuItem());
        popupMenu.addSeparator();
        popupMenu.add(NO_DIRECTORIES_REGISTERED_MENU_ITEM);
        m_application.setDockMenu(popupMenu);
    }

    @Override
    public void dispose() {

    }
}