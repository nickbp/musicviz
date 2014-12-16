/* Music Viz - Eye candy for your music on Android
 * Copyright (C) 2013 Nicholas Parker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package com.nickbp.viz.canvas;

import java.util.HashMap;
import java.util.Map;

import com.nickbp.viz.R;
import com.nickbp.viz.util.AudioSourceListener;
import com.nickbp.viz.util.DataBuffers;
import com.nickbp.viz.util.DataBufferListener;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * The {@link View} for a {@link HorizVisualizerImpl} or {@link VerticalVisualizerImpl}.
 * Handles user interaction and forwarding audio data.
 */
public class CanvasVisualizerView extends View implements DataBufferListener, AudioSourceListener {
    private static final String TAG = "CanvasVisualizerView";

    private final VisualizerSwapper vizSwapper = new VisualizerSwapper();

    private DataBuffers data;
    private int currentDataSource;
    private int sourceTextAlpha = 0;

    public CanvasVisualizerView(Context context) {
        super(context);
        setKeepScreenOn(true);
    }

    /**
     * Should be called after the constructor. Sets up touch/click listeners, and notifies the
     * provided {@code callOnInteraction} will be notified when the view is interacted with.
     *
     * This is preferable to calling setOnTouch/setOnClick directly, as those will override any
     * internally configured callbacks.
     */
    public void setInteractionListeners(final Runnable callOnInteraction) {
        setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vizSwapper.swap();
                    callOnInteraction.run();
                }
            });
        setOnTouchListener(
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        vizSwapper.swap();
                        callOnInteraction.run();
                        return true;
                    } else {
                        callOnInteraction.run();
                        return false;
                    }
                }
            });
    }

    @Override
    public void onReceive(DataBuffers buffers, boolean otherThread) {
        data = buffers;
        if (otherThread) {
            postInvalidate();
        } else {
            invalidate();
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        vizSwapper.updateSize(w, h);
    }

    @Override
    public void onSourceSwitched(int sourceType) {
        Log.d(TAG, "Data source switched to " + sourceType);
        currentDataSource = sourceType;
        sourceTextAlpha = 512;// leave text at full-alpha for a bit before fading
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null) {
            return;
        }
        vizSwapper.render(data, canvas);
        if (sourceTextAlpha > 0) {
            sourceTextAlpha = renderSourceText(canvas, getResources(), currentDataSource,
                    sourceTextAlpha > 255 ? 255 : sourceTextAlpha);
        }
    }

    private static int renderSourceText(Canvas canvas, Resources resources, int source, int alpha) {
        int headerId, messageId;
        switch (source) {
            case AudioSourceListener.SOURCE_TYPE_PLAYER:
                headerId = R.string.player_input_header;
                messageId = R.string.player_input_message;
                break;
            case AudioSourceListener.SOURCE_TYPE_MICROPHONE:
                headerId = R.string.microphone_input_header;
                messageId = R.string.microphone_input_message;
                break;
            default:
                throw new IllegalArgumentException("Unknown source id: " + source);
        }

        // Blatant use of arbitrary size constants that look nice...

        int maxTextWidth = (int)(canvas.getWidth() * .7);
        int maxTextHeight = (int)(canvas.getHeight() * .5);

        Paint p = new Paint();

        String header = resources.getText(headerId).toString();
        p.setTextAlign(Paint.Align.CENTER);
        p.setShadowLayer(5, 0, 0, Color.BLACK);
        p.setSubpixelText(true);
        p.setAntiAlias(true);
        p.setColor(Color.WHITE);
        p.setAlpha(alpha);// must be set after assigning color

        setTextSizeToFit(p, header, maxTextWidth, maxTextHeight);

        String message = resources.getText(messageId).toString();

        float centerX = canvas.getWidth() / 2;
        float centerY = canvas.getHeight() / 2;
        Rect headerBounds = new Rect();
        p.getTextBounds(header, 0, header.length(), headerBounds);

        if (message.isEmpty()) {
            // Draw header exactly at center point
            canvas.drawText(header, centerX, centerY + (headerBounds.height() / 2), p);
        } else {
            // Draw header slightly above center point
            canvas.drawText(header, centerX, centerY, p);

            p.setColor(Color.GRAY);
            p.setAlpha(alpha);// must be (re)set after assigning color

            // Go with a 2:1 size ratio, or shrink if the string is too long to fit at that size
            float preferredTextSize = p.getTextSize() / 2;
            setTextSizeToFit(p, message, maxTextWidth, maxTextHeight);
            p.setTextSize(Math.min(p.getTextSize(), preferredTextSize));

            canvas.drawText(message, centerX, centerY + headerBounds.height(), p);
        }

        return alpha - 5;
    }

    private static class TextSizeKey {
        private final String s;
        private final int width, height;

        private TextSizeKey(String s, int width, int height) {
            this.s = s;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TextSizeKey) {
                TextSizeKey k = (TextSizeKey)o;
                return k.s.equals(this.s) && k.width == this.width && k.height == this.height;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return width;
        }
    }

    private static final Map<TextSizeKey, Float> textSizeCache = new HashMap<TextSizeKey, Float>();
    private static void setTextSizeToFit(Paint p, String s, int width, int height) {
        TextSizeKey key = new TextSizeKey(s, width, height);
        Float val = textSizeCache.get(key);
        if (val != null) {
            p.setTextSize(val);
            return;
        }

        Rect bounds = new Rect();

        // Grow until one dimension exceeds limit
        float size = 1;
        do {
            size *= 2;
            p.setTextSize(size);
            p.getTextBounds(s, 0, s.length(), bounds);
        } while (bounds.width() < width && bounds.height() < height);

        // Shrink until both dimensions fit limit
        do {
            p.setTextSize(size--);
            p.getTextBounds(s, 0, s.length(), bounds);
        } while (bounds.width() > width || bounds.height() > height);

        textSizeCache.put(key, p.getTextSize());
    }

    private static class VisualizerSwapper {
        private CanvasVisualizerImpl viz;
        //TODO save default across sessions
        private boolean isHoriz = true;
        private int curWidth = 0, curHeight = 0;

        public VisualizerSwapper() {
            if (isHoriz) {
                viz = new HorizVisualizerImpl();
            } else {
                viz = new VerticalVisualizerImpl();
            }
        }

        public void render(DataBuffers data, Canvas canvas) {
            viz.render(data, canvas);
        }

        public void updateSize(int w, int h) {
            curWidth = w;
            curHeight = h;
            Log.d(TAG, "resize to w=" + curWidth + " h=" + curHeight);
            viz.resize(curWidth, curHeight);
        }

        public void swap() {
            isHoriz = !isHoriz;
            Log.d(TAG, "set horiz=" + isHoriz);
            if (isHoriz) {
                viz = new HorizVisualizerImpl();
            } else {
                viz = new VerticalVisualizerImpl();
            }
            viz.resize(curWidth, curHeight);
        }
    }
}
