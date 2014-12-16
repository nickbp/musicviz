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

import com.nickbp.viz.util.DataBuffers;
import com.nickbp.viz.util.DataLengths;
import com.nickbp.viz.util.PrecalcColorUtil;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class VerticalVisualizerImpl implements CanvasVisualizerImpl {
    private static final String TAG = "VerticalVisualizerImpl";
    // what percent of the screen's width should the analyzer take up
    private static final float ANALYZER_HEIGHT_PCT = 0.25f;
    private static final int VOICEPRINT_PX_WIDTH = 5;

    private final Paint fillPaint = new Paint();
    private final DataLengths lengths = new DataLengths();
    private int analyzerHeight;
    private VerticalBitmapScroller voiceprintBitmapScroller;
    private int viewWidth;

    public VerticalVisualizerImpl() {
        fillPaint.setAntiAlias(false);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Given the provided new {@code data}, renders the visualization's current state onto the
     * provided {@code canvas}.
     */
    public void render(DataBuffers data, Canvas canvas) {
        //COORDINATE SYSTEM: 0,0 is TOP LEFT. SIZES ARE ALWAYS IN PX (no scaling/coord transforms)

        fillPaint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), fillPaint);

        float left = 0;
        float bufferPxWidth[] = lengths.getScaledLengths(data.valBuffer.length, viewWidth);
        for (int datapt = 0; datapt < data.valBuffer.length; ++datapt) {
            left = writePx(canvas, data, bufferPxWidth, datapt, left);
        }

        voiceprintBitmapScroller.renderAndScroll(canvas);
    }

    private float writePx(Canvas analyzerCanvas, DataBuffers data, float bufferPxWidth[],
            int datapt, float left) {
        float right = left + bufferPxWidth[datapt];

        float analyzerVal = data.timeSmoothedValBuffer[datapt];
        fillPaint.setColor(PrecalcColorUtil.magnitudeToColor(analyzerVal));
        analyzerCanvas.drawRect(left, analyzerHeight - (analyzerVal * analyzerHeight),
                right, analyzerHeight, fillPaint);

        fillPaint.setColor(PrecalcColorUtil.magnitudeToColor(data.valBuffer[datapt]));
        voiceprintBitmapScroller.drawRect(left, right, fillPaint);

        // shift rightwards (to the new left):
        return right;
    }

    /**
     * Notifies the visualization that the display dimensions have changed.
     * @return The data display width that should be used for future incoming data via
     * {@link #render(DataBuffers, Canvas)}.
     */
    public void resize(int viewWidth, int viewHeight) {
        Log.d(TAG, "size changed: w=" + viewWidth + ", h=" + viewHeight);
        analyzerHeight = (int)(viewHeight * ANALYZER_HEIGHT_PCT);
        voiceprintBitmapScroller = new VerticalBitmapScroller(
            viewWidth, viewHeight - analyzerHeight, analyzerHeight, VOICEPRINT_PX_WIDTH);
        this.viewWidth = viewWidth;
    }
}
