package com.jesuisjo.dndhttpserver.gui;


import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenAccessor;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenManager;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotificationOverlayPanel {

    public static final Color SUCCESS_BG_COLOR = new Color(223, 240, 216, 00);
    public static final Color SUCCESS_FG_COLOR = new Color(70, 136, 71, 0);
    public static final Color ERROR_BG_COLOR = new Color(242, 222, 222, 0);
    public static final Color ERROR_FG_COLOR = new Color(185, 74, 0, 0);
    private final JPanel m_notificationPanel;
    private final JTextComponent m_notificationText;
    private final FxQueue m_fxQueue = new FxQueue(this);

    public NotificationOverlayPanel() {
        m_notificationPanel = new JPanel() {
            // used to manage opacity properly, see http://tips4java.wordpress.com/2009/05/31/backgrounds-with-transparency/
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        m_notificationPanel.setLayout(new BoxLayout(m_notificationPanel, BoxLayout.PAGE_AXIS));
        m_notificationPanel.setVisible(false);
        m_notificationPanel.setOpaque(false);

        m_notificationText = new JTextPane();
        m_notificationText.setEditable(false);
        m_notificationText.setOpaque(false);
        m_notificationText.setFocusable(false);
        m_notificationText.setHighlighter(null);
        m_notificationText.setDragEnabled(false);
        m_notificationText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        m_notificationPanel.add(m_notificationText);
    }

    public void install(JFrame jFrame) {
        final JLayeredPane layeredPane = jFrame.getLayeredPane();
        layeredPane.add(m_notificationPanel, JLayeredPane.MODAL_LAYER);
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                m_notificationPanel.setSize(e.getComponent().getWidth(), m_notificationText.getSize().height);
            }
        });
    }

    public void notifySuccess(String message) {
        m_notificationPanel.setBackground(SUCCESS_BG_COLOR);
        m_notificationText.setForeground(SUCCESS_FG_COLOR);
        m_notificationText.setText(message);

        m_fxQueue.fadeIn().delay(5000).fadeOut();
    }

    public void notifyError(String message) {
        m_notificationPanel.setBackground(ERROR_BG_COLOR);
        m_notificationText.setForeground(ERROR_FG_COLOR);
        m_notificationText.setText(message);

        m_fxQueue.fadeIn().delay(5000).fadeOut();
    }

    private void setOpacity(float opacity) {
        Color bgColor = m_notificationPanel.getBackground();
        Color fgColor = m_notificationText.getForeground();
        int alphaValue = Math.round(255 * opacity);
        m_notificationPanel.setBackground(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), alphaValue));
        m_notificationText.setForeground(new Color(fgColor.getRed(), fgColor.getGreen(), fgColor.getBlue(), alphaValue));
    }

    private static class FxQueue {

        static {
            Tween.registerAccessor(NotificationOverlayPanel.class, new NotificationOverlayPanelTweenAccessor());
        }

        final NotificationOverlayPanel m_notificationOverlayPanel;
        final TweenManager m_tweenManager = new TweenManager();
        final Queue<Tween> m_tweenQueue = new ArrayDeque<>();
        int m_pendingDelayValue = -1;

        FxQueue(NotificationOverlayPanel notificationOverlayPanel) {
            m_notificationOverlayPanel = notificationOverlayPanel;

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                long m_lastLoopTime = System.currentTimeMillis();

                @Override
                public void run() {
                    final long now = System.currentTimeMillis();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            m_tweenManager.update(now - m_lastLoopTime);
                            m_lastLoopTime = now;
                        }
                    });
                }
            }, 0, 40, TimeUnit.MILLISECONDS);
        }

        FxQueue fadeIn() {
            ForwardingMultiCallbacksList callbacksList = new ForwardingMultiCallbacksList();
            callbacksList.addCallback(new TweenCallback() {
                @Override
                public void onEvent(int type, BaseTween<?> source) {
                    if (type == TweenCallback.BEGIN) {
                        m_notificationOverlayPanel.m_notificationPanel.setVisible(true);
                    }
                }
            });
            Tween tween = buildOpacityTween().target(1);

            handlePendingDelay(tween);
            executeOrEnqueue(tween, callbacksList);
            return this;
        }

        FxQueue fadeOut() {
            Tween tween = buildOpacityTween().target(0);
            ForwardingMultiCallbacksList callbacksList = new ForwardingMultiCallbacksList();
            callbacksList.addCallback(new TweenCallback() {
                @Override
                public void onEvent(int type, BaseTween<?> source) {
                    if (type == TweenCallback.COMPLETE) {
                        m_notificationOverlayPanel.m_notificationPanel.setVisible(false);
                    }
                }
            });

            handlePendingDelay(tween);
            executeOrEnqueue(tween, callbacksList);
            return this;
        }

        FxQueue delay(int delay) {
            m_pendingDelayValue = delay;
            return this;
        }

        Tween buildOpacityTween() {
            return Tween.to(m_notificationOverlayPanel, NotificationOverlayPanelTweenAccessor.TYPE_OPACITY, 500);
        }

        void executeOrEnqueue(Tween tween, final ForwardingMultiCallbacksList callbacksList) {
            tween.setCallbackTriggers(TweenCallback.BEGIN | TweenCallback.COMPLETE);
            tween.setCallback(new TweenCallback() {
                @Override
                public void onEvent(int type, BaseTween<?> source) {
                    callbacksList.onEvent(type, source);
                    if (type == TweenCallback.COMPLETE) {
                        Tween nextTween = m_tweenQueue.poll();
                        if (nextTween != null) {
                            nextTween.start(m_tweenManager);
                        }
                    }
                }
            });

            if (m_tweenQueue.isEmpty()) {
                tween.start(m_tweenManager);
            } else {
                m_tweenQueue.offer(tween);
            }
        }

        void handlePendingDelay(Tween tween) {
            if (m_pendingDelayValue != -1) {
                tween.delay(m_pendingDelayValue);
                m_pendingDelayValue = -1;
            }
        }

        static class ForwardingMultiCallbacksList implements TweenCallback {

            private List<TweenCallback> m_tweenCallback = new ArrayList<>();

            void addCallback(TweenCallback tweenCallback) {
                m_tweenCallback.add(tweenCallback);
            }

            @Override
            public void onEvent(int type, BaseTween<?> source) {
                for (TweenCallback tweenCallback : m_tweenCallback) {
                    tweenCallback.onEvent(type, source);
                }
            }
        }

        static class NotificationOverlayPanelTweenAccessor implements TweenAccessor<NotificationOverlayPanel> {

            static int TYPE_OPACITY = 0;

            @Override
            public int getValues(NotificationOverlayPanel target, int tweenType, float[] returnValues) {
                if (tweenType != TYPE_OPACITY) {
                    return -1;
                }
                returnValues[0] = target.m_notificationPanel.getBackground().getAlpha() / 255f;
                return 1;
            }

            @Override
            public void setValues(NotificationOverlayPanel target, int tweenType, float[] newValues) {
                target.setOpacity(newValues[0]);
            }
        }
    }
}