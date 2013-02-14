package com.jesuisjo.dndhttpserver.gui;


import com.google.common.util.concurrent.ListenableFuture;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class NotificationOverlayPanel {

    public static final Color SUCCESS_COLOR = new Color(91, 183, 91, 200);
    public static final Color FAIL_COLOR = new Color(257, 167, 50, 200);

    private final JPanel m_notificationPanel;
    private final JTextArea m_notificationText;
    private final ScheduledExecutorService m_timerExecutor = Executors.newSingleThreadScheduledExecutor();

    public NotificationOverlayPanel() {
        m_notificationPanel = new JPanel();
        m_notificationPanel.setLayout(new BoxLayout(m_notificationPanel, BoxLayout.PAGE_AXIS));
        m_notificationPanel.setBackground(FAIL_COLOR);

        m_notificationText = new JTextArea("Un text de notification");
        m_notificationText.setLineWrap(true);
        m_notificationText.setWrapStyleWord(true);
        m_notificationText.setEditable(false);
        m_notificationText.setOpaque(false);

        m_notificationPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        m_notificationPanel.add(m_notificationText);
        m_notificationPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    public void install(JFrame jFrame) {
        final JLayeredPane layeredPane = jFrame.getLayeredPane();
        layeredPane.add(m_notificationPanel, JLayeredPane.MODAL_LAYER);
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                m_notificationPanel.setSize(e.getComponent().getWidth(), m_notificationPanel.getPreferredSize().height);
                layeredPane.repaint();
            }
        });
    }

    public void notifySuccess(String message) {
        m_notificationPanel.setBackground(SUCCESS_COLOR);
        m_notificationText.setText(message);
        m_notificationPanel.setVisible(true);

        final Runnable action = new Runnable() {
            @Override
            public void run() {
                m_notificationPanel.setVisible(true);
            }
        };

        delay(action, 3);
    }

    public void notifyFail(String message) {
        m_notificationPanel.setBackground(FAIL_COLOR);
        m_notificationText.setText(message);
        m_notificationPanel.setVisible(true);
    }

    private ListenableFuture<Void> delay(final Runnable action, int delay) {

        ScheduledFuture<?> delayFuture = ListenableFuture.m_timerExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        future.set(null);
                        action.run();
                    }
                });
            }
        }, delay, TimeUnit.SECONDS);

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // nothing to do
            }

            @Override
            public void onFailure(Throwable t) {
                // on cancel, stop the delay
                delayFuture.cancel(false);
            }
        });

        return future;
    }
}