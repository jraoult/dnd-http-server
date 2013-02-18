package com.jesuisjo.dndhttpserver.gui;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.image.BufferedImage;

public interface GuiPlatformSpecificHandler {

    void install();

    void addRemoveHandlerMenuItem(MenuItem menuItem);

    void removeRemoveHandlerMenuItem(MenuItem menuItem);

    void showNoHandlerMenuItem();

    void hideNoHandlerMenuItem();

    void displayInfo(String message);

    void displayError(String message);

    void dispose();
}