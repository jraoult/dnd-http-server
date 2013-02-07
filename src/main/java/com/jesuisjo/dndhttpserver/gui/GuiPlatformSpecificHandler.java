package com.jesuisjo.dndhttpserver.gui;

import java.awt.Image;
import java.awt.PopupMenu;

public interface GuiPlatformSpecificHandler {

    void setAppIcon(Image image);

    void installMenu(PopupMenu popupMenu);

    void setupWindowBehavior(Runnable onQuitAction);

    void displayInfoNotification(String caption, String message);

    void displayErrorNotification(String caption, String message);

    void dispose();
}