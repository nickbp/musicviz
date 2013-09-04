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
import com.nickbp.viz.util.PrecalcColorUtil;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class HorizVisualizerImpl implements CanvasVisualizerImpl {
	private static final String TAG = "HorizVisualizerImpl";
	// what percent of the screen's width should the analyzer take up
	private static final float ANALYZER_WIDTH_PCT = 0.15f;
	private static final int VOICEPRINT_PX_WIDTH = 5;

	private final Paint analyzerPaint = new Paint();
	private final Paint voiceprintPaint = new Paint();
	private int analyzerWidth;
	private int analyzerLeft;
	private HorizBitmapScroller voiceprintBitmapScroller;

	public HorizVisualizerImpl() {
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

        float bottom = canvas.getHeight();
	    for (int datapt = 0; datapt < data.valCacheKeyBuffer.length; ++datapt) {
		    bottom = writePx(canvas, data, datapt, bottom);
	    }

        voiceprintBitmapScroller.renderAndScroll(canvas);
	}
	
	private float writePx(Canvas analyzerCanvas, DataBuffers data, int datapt, float bottom) {
		float top = bottom - data.bufferPxWidth[datapt];
		
		float uncacheableAnalyzerVal = data.timeSmoothedValBuffer[datapt];
		int cacheKey = data.valCacheKeyBuffer[datapt];

		analyzerPaint.setColor(
			PrecalcColorUtil.valueToColor(uncacheableAnalyzerVal,
			PrecalcColorUtil.valueToLum(uncacheableAnalyzerVal)));
		analyzerCanvas.drawRect(analyzerLeft, top,
			analyzerLeft + (uncacheableAnalyzerVal * analyzerWidth), bottom, analyzerPaint);
		
		voiceprintPaint.setColor(PrecalcColorUtil.PRECALCULATED_COLOR_BUFFER[cacheKey]);
		voiceprintBitmapScroller.drawRect(top, bottom, voiceprintPaint);
		
		// shift upwards (to the new bottom):
        return top;
	}

	/**
	 * Notifies the visualization that the display dimensions have changed.
	 * @return The data display width that should be used for future incoming data via
	 * {@link #render(DataBuffers, Canvas)}.
	 */
	public int resize(int viewWidth, int viewHeight) {
		Log.d(TAG, "size changed: w=" + viewWidth + ", h=" + viewHeight);
        analyzerWidth = (int)(viewWidth * ANALYZER_WIDTH_PCT);
        analyzerLeft = viewWidth - analyzerWidth;
        voiceprintBitmapScroller =
        	new HorizBitmapScroller(analyzerLeft, viewHeight, VOICEPRINT_PX_WIDTH);
		return viewHeight;
	}
}
