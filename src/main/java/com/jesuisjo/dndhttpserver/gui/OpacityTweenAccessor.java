package com.jesuisjo.dndhttpserver.gui;

import aurelienribon.tweenengine.TweenAccessor;

public class OpacityTweenAccessor implements TweenAccessor<FxQueue.AnimatedPropertyHandler<Float>> {

    public static int TYPE_OPACITY = 0;

    @Override
    public int getValues(FxQueue.AnimatedPropertyHandler<Float> target, int tweenType, float[] returnValues) {
        if (tweenType != TYPE_OPACITY) {
            return -1;
        }
        returnValues[0] = target.getCurrent();
        return 1;
    }

    @Override
    public void setValues(FxQueue.AnimatedPropertyHandler<Float> target, int tweenType, float[] newValues) {
        target.progress(newValues[0]);
    }
}
