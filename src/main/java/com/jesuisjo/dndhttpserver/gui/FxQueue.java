package com.jesuisjo.dndhttpserver.gui;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenManager;

import javax.swing.SwingUtilities;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FxQueue {

    static {
        Tween.registerAccessor(AnimatedOpacityHandler.class, new OpacityTweenAccessor());
    }

    private final AnimatedOpacityHandler m_animatedOpacityHandler;
    private final TweenManager m_tweenManager = new TweenManager();
    private final Queue<Tween> m_tweenQueue = new ArrayDeque<>();
    private final ScheduledExecutorService m_eventLoopExecutor;
    private int m_pendingDelayValue = -1;
    private ScheduledFuture<?> m_eventLoopFuture;

    public FxQueue(AnimatedOpacityHandler animatedOpacityHandler, ScheduledExecutorService eventLoopExecutor) {
        m_animatedOpacityHandler = animatedOpacityHandler;
        m_eventLoopExecutor = eventLoopExecutor;
    }

    private void ensureEventLoopStarted() {
        if (m_eventLoopFuture == null || m_eventLoopFuture.isCancelled()) {
            m_eventLoopFuture = m_eventLoopExecutor.scheduleAtFixedRate(new Runnable() {
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
    }

    public FxQueue fadeIn() {
        ForwardingMultiCallbacksList callbacksList = new ForwardingMultiCallbacksList();
        callbacksList.addCallback(new TweenCallback() {
            @Override
            public void onEvent(int type, BaseTween<?> source) {
                if (type == TweenCallback.BEGIN) {
                    m_animatedOpacityHandler.begin();
                }
            }
        });
        Tween tween = buildOpacityTween().target(1);

        handlePendingDelay(tween);
        executeOrEnqueue(tween, callbacksList);
        return this;
    }

    public FxQueue fadeOut() {
        Tween tween = buildOpacityTween().target(0);
        ForwardingMultiCallbacksList callbacksList = new ForwardingMultiCallbacksList();
        callbacksList.addCallback(new TweenCallback() {
            @Override
            public void onEvent(int type, BaseTween<?> source) {
                if (type == TweenCallback.COMPLETE) {
                    m_animatedOpacityHandler.complete();
                }
            }
        });

        handlePendingDelay(tween);
        executeOrEnqueue(tween, callbacksList);
        return this;
    }

    public FxQueue delay(int delay) {
        m_pendingDelayValue = delay;
        return this;
    }

    private Tween buildOpacityTween() {
        return Tween.to(m_animatedOpacityHandler, OpacityTweenAccessor.TYPE_OPACITY, 500);
    }

    private void executeOrEnqueue(Tween tween, final ForwardingMultiCallbacksList callbacksList) {
        ensureEventLoopStarted();

        tween.setCallbackTriggers(TweenCallback.BEGIN | TweenCallback.COMPLETE);
        tween.setCallback(new TweenCallback() {
            @Override
            public void onEvent(int type, BaseTween<?> source) {
                callbacksList.onEvent(type, source);
                if (type == TweenCallback.COMPLETE) {
                    Tween nextTween = m_tweenQueue.poll();
                    if (nextTween != null) {
                        nextTween.start(m_tweenManager);
                    } else {
                        m_eventLoopFuture.cancel(true);
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

    private void handlePendingDelay(Tween tween) {
        if (m_pendingDelayValue != -1) {
            tween.delay(m_pendingDelayValue);
            m_pendingDelayValue = -1;
        }
    }

    public interface AnimatedPropertyHandler<T> {

        T getCurrent();

        void begin();

        void progress(T value);

        void complete();

    }

    private static class ForwardingMultiCallbacksList implements TweenCallback {

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
}