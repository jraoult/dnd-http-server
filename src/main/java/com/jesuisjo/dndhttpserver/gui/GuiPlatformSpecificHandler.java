package com.jesuisjo.dndhttpserver.gui;

import java.awt.Image;
import java.awt.MenuItem;

public interface GuiPlatformSpecificHandler {

    void installAppIcon(Image image);

    void installMenu();

    void addRemoveHandlerMenuItem(MenuItem menuItem);

    void removeRemoveHandlerMenuItem(MenuItem menuItem);

    void showNoHandlerMenuItem();

    void hideNoHandlerMenuItem();

    void installWindowBehavior();

    void displayInfoNotification(String caption, String message);

    void displayErrorNotification(String caption, String message);

    void dispose();
}