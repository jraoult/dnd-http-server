package com.jesuisjo.dndhttpserver.gui;


import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ScheduledExecutorService;

public class NotificationOverlayPanel {

    public static final Color SUCCESS_BG_COLOR = new Color(223, 240, 216, 0);
    public static final Color SUCCESS_FG_COLOR = new Color(70, 136, 71, 0);
    public static final Color ERROR_BG_COLOR = new Color(242, 222, 222, 0);
    public static final Color ERROR_FG_COLOR = new Color(185, 74, 0, 0);
    private final JPanel m_notificationPanel;
    private final JTextComponent m_notificationText;
    private final FxQueue m_fxQueue;

    public NotificationOverlayPanel(ScheduledExecutorService fxQueueEventLoopExecutor) {
        m_fxQueue = new FxQueue(new AnimatedOpacityHandler() {

            @Override
            public Float getCurrent() {
                return getOpacity();
            }

            @Override
            public void begin() {
                m_notificationPanel.setVisible(true);
            }

            @Override
            public void progress(Float value) {
                setOpacity(value);
            }

            @Override
            public void complete() {
                m_notificationPanel.setVisible(false);
            }
        }, fxQueueEventLoopExecutor);

        m_notificationPanel = new JPanel() {
            // used to manage opacity properly, see http://tips4java.wordpress.com/2009/05/31/backgrounds-with-transparency/
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };

        m_notificationPanel.setLayout(new BorderLayout());
        m_notificationPanel.setVisible(false);
        m_notificationPanel.setOpaque(false);

        m_notificationText = new JTextPane();
        m_notificationText.setEditable(false);
        m_notificationText.setOpaque(false);
        m_notificationText.setFocusable(false);
        m_notificationText.setHighlighter(null);
        m_notificationText.setDragEnabled(false);
        m_notificationText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        m_notificationPanel.add(m_notificationText, BorderLayout.PAGE_START);

        // dismiss the notification on click
        m_notificationText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                m_fxQueue.fadeOut();
            }
        });
    }

    public void install(JFrame jFrame) {
        final JLayeredPane layeredPane = jFrame.getLayeredPane();
        layeredPane.add(m_notificationPanel, JLayeredPane.MODAL_LAYER);
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                m_notificationPanel.setSize(e.getComponent().getWidth(), m_notificationPanel.getHeight());
            }
        });
    }

    public void notifySuccess(String message) {
        m_notificationPanel.setBackground(SUCCESS_BG_COLOR);
        m_notificationText.setForeground(SUCCESS_FG_COLOR);
        setTextAndResize(message);

        m_fxQueue.fadeIn();
    }

    public void notifyError(String message) {
        m_notificationPanel.setBackground(ERROR_BG_COLOR);
        m_notificationText.setForeground(ERROR_FG_COLOR);
        setTextAndResize(message);

        m_fxQueue.fadeIn();
    }

    private float getOpacity() {
        return m_notificationPanel.getBackground().getAlpha() / 255f;
    }

    private void setOpacity(float opacity) {
        Color bgColor = m_notificationPanel.getBackground();
        Color fgColor = m_notificationText.getForeground();
        int alphaValue = Math.round(255 * opacity);
        m_notificationPanel.setBackground(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), alphaValue));
        m_notificationText.setForeground(new Color(fgColor.getRed(), fgColor.getGreen(), fgColor.getBlue(), alphaValue));
    }

    private void setTextAndResize(String message) {
        m_notificationText.setSize(m_notificationPanel.getWidth(), Integer.MAX_VALUE);
        m_notificationText.setText(message);
        m_notificationPanel.setSize(m_notificationPanel.getWidth(), m_notificationText.getPreferredSize().height);
    }

}