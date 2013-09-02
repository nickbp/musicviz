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
import com.nickbp.viz.util.RgbColor;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class VerticalVisualizerImpl implements CanvasVisualizerImpl {
	private static final String TAG = "VerticalVisualizerImpl";
	// what percent of the screen's width should the analyzer take up
	private static final float ANALYZER_HEIGHT_PCT = 0.25f;
	private static final int VOICEPRINT_PX_WIDTH = 5;
	// cap on luminosity
	private static final float MAX_LUM = 0.5f;
	// set a curve on luminosity: exaggerate low values
	private static final double LUM_EXPONENT = 0.85;

	private final Paint analyzerPaint = new Paint();
	private final Paint voiceprintPaint = new Paint();
	private int analyzerHeight;
	private VerticalBitmapScroller voiceprintBitmapScroller;

	public VerticalVisualizerImpl() {
		analyzerPaint.setAntiAlias(false);
		analyzerPaint.setStyle(Paint.Style.FILL);
		voiceprintPaint.setAntiAlias(false);
		voiceprintPaint.setStyle(Paint.Style.FILL);
	}
	
	/**
	 * Given the provided new {@code data}, renders the visualization's current state onto the
	 * provided {@code canvas}.
	 */
	public void render(DataBuffers data, Canvas canvas) {
		//COORDINATE SYSTEM: 0,0 is TOP LEFT. SIZES ARE ALWAYS IN PX (no scaling/coord transforms)
        
        analyzerPaint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), analyzerPaint);

        RgbColor rgb = new RgbColor();
        float left = 0;
	    for (int datapt = 0; datapt < data.buffer.length; ++datapt) {
	    	left = writePx(canvas, data, datapt, rgb, left);
	    }

        voiceprintBitmapScroller.renderAndScroll(canvas);
	}
	
	private float writePx(Canvas analyzerCanvas, DataBuffers data, int datapt, RgbColor rgb, float left) {
		float right = left + data.bufferPxWidth[datapt];

		{
			float analyzerVal = data.timeSmoothedBuffer[datapt];
			float analyzerLum = Math.min(MAX_LUM, (float)Math.pow(analyzerVal, LUM_EXPONENT));
			int analyzerColor =
				rgb.setFromHsl((1 / 3.f) * (1 - analyzerVal), 1, analyzerLum).toIntColor();
			analyzerPaint.setColor(analyzerColor);
			analyzerCanvas.drawRect(left, analyzerHeight - (analyzerVal * analyzerHeight), right,
				analyzerHeight, analyzerPaint);
		}
		{
			float voiceprintVal = data.buffer[datapt];
			float voiceprintLum = Math.min(MAX_LUM, (float)Math.pow(voiceprintVal, LUM_EXPONENT));
			int voiceprintColor =
				rgb.setFromHsl((1 / 3.f) * (1 - voiceprintVal), 1, voiceprintLum).toIntColor();
			voiceprintPaint.setColor(voiceprintColor);
			voiceprintBitmapScroller.drawRect(left, right, voiceprintPaint);
		}

		// shift rightwards (to the new left):
        return right;
	}

	/**
	 * Notifies the visualization that the display dimensions have changed.
	 * @return The data display width that should be used for future incoming data via
	 * {@link #render(DataBuffers, Canvas)}.
	 */
	public int resize(int viewWidth, int viewHeight) {
		Log.d(TAG, "size changed: w=" + viewWidth + ", h=" + viewHeight);
        analyzerHeight = (int)(viewHeight * ANALYZER_HEIGHT_PCT);
        voiceprintBitmapScroller = new VerticalBitmapScroller(
        	viewWidth, viewHeight - analyzerHeight, analyzerHeight, VOICEPRINT_PX_WIDTH);
		return viewWidth;
	}
}
