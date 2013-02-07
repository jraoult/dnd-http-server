package com.jesuisjo.dndhttpserver.gui;

import com.apple.eawt.AppEvent;
import com.apple.eawt.AppReOpenedListener;
import com.apple.eawt.Application;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

import javax.swing.JFrame;
import java.awt.Image;
import java.awt.PopupMenu;

public class OsXGuiHandler implements GuiPlatformSpecificHandler {

    private final Application m_application = Application.getApplication();
    private final JFrame m_mainFrame;

    public OsXGuiHandler(JFrame mainFrame) {
        m_mainFrame = mainFrame;
    }

    @Override
    public void setAppIcon(Image image) {
        m_application.setDockIconImage(image);
    }

    @Override
    public void setupWindowBehavior(final Runnable onQuitAction) {
        m_application.addAppEventListener(new AppReOpenedListener() {
            @Override
            public void appReOpened(AppEvent.AppReOpenedEvent appReOpenedEvent) {
                m_mainFrame.setVisible(true);
            }
        });

        m_application.setQuitHandler(new QuitHandler() {
            @Override
            public void handleQuitRequestWith(AppEvent.QuitEvent quitEvent, QuitResponse quitResponse) {
                onQuitAction.run();
                quitResponse.performQuit();
            }
        });
    }

    @Override
    public void installMenu(PopupMenu popupMenu) {
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