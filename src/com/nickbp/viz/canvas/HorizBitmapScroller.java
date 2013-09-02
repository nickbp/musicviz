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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Handles a canvas that continuously shifts leftwards.
 * 
 * This implementation moves the active column to the right, then draws out the bitmap such that the
 * active column is always on the right edge of the output. After each render, the active column is
 * moved to the right by a predefined increment.
 * 
 * Callers must fill a column using {@link #drawRect(float, float, Paint)}, then render/shift the
 * result using {@link #renderAndScroll(Canvas)}. This cycle repeats indefinitely.
 */
public class HorizBitmapScroller {
	private final Bitmap bitmap;
	private final Canvas bitmapWrapper;
	private final int scrollDist;
	
	/**
	 * Marks the *right* edge of the current column.
	 */
	private int currentColRightEdge = 0;
	
	/**
	 * Creates a new scroller whose display area is provided width/height, with a column size of
	 * {@code scrollDist}. This is the column width used by {@link #drawRect(float, float, Paint)},
	 * and the distance that {@link #renderAndScroll(Canvas)} shifts the active area each time it's
	 * called.
	 */
	public HorizBitmapScroller(int width, int height, int scrollDist) {
        this.scrollDist = scrollDist;
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmapWrapper = new Canvas(bitmap);
	}
	
	/**
	 * Renders the current state to the provided {@link Canvas}, then shifts the active column to
	 * the right by {@code scrollDist}.
	 */
	public void renderAndScroll(Canvas canvas) {
		// first paint what's to the right of "currentColRightEdge" on the left edge of the display.
		// (this is the oldest data)
		
		int currentSeam = bitmap.getWidth() - currentColRightEdge;
		
		// left, top, right, bottom
		Rect bitmapBounds = new Rect(currentColRightEdge, 0, bitmap.getWidth(), bitmap.getHeight());
		Rect outputBounds = new Rect(0, 0, currentSeam, bitmap.getHeight());
		canvas.drawBitmap(bitmap, bitmapBounds, outputBounds, null);
		
		// then paint what's to the left of "currentColRightEdge" on the right edge of the display.
		// (this is the newest data)
		
		bitmapBounds = new Rect(0, 0, currentColRightEdge, bitmap.getHeight());
		outputBounds = new Rect(currentSeam, 0, bitmap.getWidth(), bitmap.getHeight());
		canvas.drawBitmap(bitmap, bitmapBounds, outputBounds, null);
		
		// increment column
        currentColRightEdge = (currentColRightEdge + scrollDist) % bitmap.getWidth();
	}
	
	/**
	 * Draws a rectangle in the currently active column between {@code top} and {@code bottom},
	 * using the provided {@code paint}. The width of the column is determined by {@code scrollDist}
	 * in {@link #BitmapScroller(int, int, int)}.
	 */
	public void drawRect(float top, float bottom, Paint paint) {
		int left = currentColRightEdge - scrollDist;
		if (left < 0) {
			// wraparound. paint both halves of the split column on the edges of the canvas.
			// left, top, right, bottom
			bitmapWrapper.drawRect(left + bitmapWrapper.getWidth(), top,
				bitmapWrapper.getWidth(), bottom, paint);
			bitmapWrapper.drawRect(0, top, currentColRightEdge, bottom, paint);
		} else {
			bitmapWrapper.drawRect(left, top, currentColRightEdge, bottom, paint);
		}
	}
	
	/**
	 * Clears/paints the entire display with the provided {@code color}. This effectively clears all
	 * previous drawing.
	 */
	public void clear(int color) {
		bitmapWrapper.drawColor(color);
	}
}
