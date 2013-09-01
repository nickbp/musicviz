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
 * Handles two adjacent bitmap+canvas pairs, which continuously scroll past the screen, swapping
 * places as necessary to maintain continuity.
 * 
 * This implementation moves the bitmaps to the left, in a predefined column increment. Callers
 * must fill a column using {@link #drawRect(float, float, Paint)}, then render a snapshot of the
 * filled column using {@link #renderAndScroll(Canvas)}. After each render call, the scroller
 * automatically shifts the render to the next column, ready to be filled. This cycle repeats
 * indefinitely.
 */
public class BitmapScroller {
	private final Bitmap bitmap1, bitmap2;
	private final Canvas canvas1, canvas2;
	private final int scrollDist;
	
	// marks the left edge of the window from the left edge of BM1,
	// or the right edge of the window from the left edge of BM2.
	private int scrollLocation = 0;
	
	/**
	 * Creates a new scroller whose display area is provided width/height, with a column size of
	 * {@code scrollDist}. This is the column width used by {@link #drawRect(float, float, Paint)},
	 * and the distance that {@link #renderAndScroll(Canvas)} shifts the active area each time it's
	 * called.
	 */
	public BitmapScroller(int width, int height, int scrollDist) {
        this.scrollDist = scrollDist;
        bitmap1 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        canvas1 = new Canvas(bitmap1);
        canvas2 = new Canvas(bitmap2);
	}
	
	/**
	 * Renders the current state to the provided {@link Canvas}, then shifts the active column to
	 * the right by {@code scrollDist}.
	 */
	public void renderAndScroll(Canvas canvas) {
		int scrollLocationWithinBitmap = scrollLocation % bitmap1.getWidth();
		if (scrollLocation >= bitmap1.getWidth()) {
			//bitmap2 is left of bitmap1
			renderBitmaps(canvas, scrollLocationWithinBitmap, bitmap2, bitmap1);
		} else {
			//bitmap1 is left of bitmap2
			renderBitmaps(canvas, scrollLocationWithinBitmap, bitmap1, bitmap2);
		}
        scrollLocation = (scrollLocation + scrollDist) % (2 * bitmap1.getWidth());
	}
	
	private void renderBitmaps(Canvas canvas, int scrollLocation, Bitmap leftBitmap,
			Bitmap rightBitmap) {
		// left, top, right, bottom
		Rect leftBitmapRemainder =
			new Rect(scrollLocation, 0, leftBitmap.getWidth(), leftBitmap.getHeight());
		Rect leftBitmapOutputBounds =
			new Rect(0, 0, leftBitmap.getWidth() - scrollLocation, leftBitmap.getHeight());
		canvas.drawBitmap(leftBitmap, leftBitmapRemainder, leftBitmapOutputBounds, null);
		
		Rect rightBitmapRemainder =
			new Rect(0, 0, scrollLocation, rightBitmap.getHeight());
		Rect rightBitmapOutputBounds =
			new Rect(leftBitmap.getWidth() - scrollLocation, 0,
				rightBitmap.getWidth(), rightBitmap.getHeight());
		canvas.drawBitmap(rightBitmap, rightBitmapRemainder, rightBitmapOutputBounds, null);
	}
	
	/**
	 * Draws a rectangle in the currently active column between {@code top} and {@code bottom},
	 * using the provided {@code paint}. The width of the column is determined by {@code scrollDist}
	 * in {@link #BitmapScroller(int, int, int)}.
	 */
	public void drawRect(float top, float bottom, Paint paint) {
		int left = scrollLocation - scrollDist;
		if (left < bitmap1.getWidth()) {
			//canvas2 is left of canvas1 (THIS ONE'S GOOD)
			//drawRect(left, top, bottom, paint, canvas2, canvas1);
			// left, top, right, bottom
			drawRect(top, bottom, paint, left, canvas2, canvas1);
		} else {
			//canvas1 is left of canvas2 (THIS ONE'S BROKE)
			//drawRect(left, top, bottom, paint, canvas1, canvas2);
			// left, top, right, bottom
			left %= canvas1.getWidth();
			drawRect(top, bottom, paint, left, canvas1, canvas2);
		}
	}
	
	private void drawRect(float top, float bottom, Paint paint, int left, Canvas leftCanvas, Canvas rightCanvas) {
		int right = left + scrollDist;
		if (right > leftCanvas.getWidth()) {
			leftCanvas.drawRect(left, top, leftCanvas.getWidth(), bottom, paint);
			rightCanvas.drawRect(0, top, right % leftCanvas.getWidth(), bottom, paint);
		} else {
			leftCanvas.drawRect(left, top, right, bottom, paint);
		}
	}
	
	/**
	 * Clears/paints the entire display with the provided {@code color}. This effectively clears all
	 * previous drawing.
	 */
	public void clear(int color) {
		canvas1.drawColor(color);
		canvas2.drawColor(color);
	}
}
