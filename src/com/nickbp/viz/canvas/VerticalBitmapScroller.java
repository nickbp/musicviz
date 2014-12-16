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
 * Handles a canvas that continuously shifts downwards.
 *
 * This implementation moves the active row upwards, then draws out the bitmap such that the active
 * row is always on the top edge of the output. After each render, the active row is moved upwards
 * by a predefined increment.
 *
 * Callers must fill a row using {@link #drawRect(float, float, Paint)}, then render/shift the
 * result using {@link #renderAndScroll(Canvas)}. This cycle repeats indefinitely.
 */
public class VerticalBitmapScroller {
    private final Bitmap bitmap;
    private final Canvas bitmapWrapper;
    private final int offsety;
    private final int scrollDist;

    /**
     * Marks the *right* edge of the current column.
     */
    private int currentRowTopEdge = 0;

    /**
     * Creates a new scroller whose display area is provided width/height, with a column size of
     * {@code scrollDist}. This is the column width used by {@link #drawRect(float, float, Paint)},
     * and the distance that {@link #renderAndScroll(Canvas)} shifts the active area each time it's
     * called.
     */
    public VerticalBitmapScroller(int width, int height, int offsety, int scrollDist) {
        this.offsety = offsety;
        this.scrollDist = scrollDist;
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmapWrapper = new Canvas(bitmap);
    }

    /**
     * Renders the current state to the provided {@link Canvas}, then shifts the active column to
     * the right by {@code scrollDist}.
     */
    public void renderAndScroll(Canvas canvas) {
        // first paint what's above "currentRowTopEdge" on the bottom edge of the display.
        // (this is the oldest data)

        int currentSeam = bitmap.getHeight() - currentRowTopEdge + offsety;

        // left, top, right, bottom
        Rect bitmapBounds = new Rect(0, 0, bitmap.getWidth(), currentRowTopEdge);
        Rect outputBounds = new Rect(0, currentSeam, bitmap.getWidth(), bitmap.getHeight() + offsety);
        canvas.drawBitmap(bitmap, bitmapBounds, outputBounds, null);

        // then paint what's below "currentRowTopEdge" on the top edge of the display.
        // (this is the newest data)

        bitmapBounds = new Rect(0, currentRowTopEdge, bitmap.getWidth(), bitmap.getHeight());
        outputBounds = new Rect(0, offsety, bitmap.getWidth(), currentSeam);
        canvas.drawBitmap(bitmap, bitmapBounds, outputBounds, null);

        // decrement row
        currentRowTopEdge -= scrollDist;
        if (currentRowTopEdge < 0) {
            currentRowTopEdge += bitmap.getHeight();
        }
    }

    /**
     * Draws a rectangle in the currently active column between {@code top} and {@code bottom},
     * using the provided {@code paint}. The width of the column is determined by {@code scrollDist}
     * in {@link #BitmapScroller(int, int, int)}.
     */
    public void drawRect(float left, float right, Paint paint) {
        int bottom = currentRowTopEdge + scrollDist;
        if (bottom > bitmapWrapper.getHeight()) {
            // wraparound. paint both halves of the split row on the edges of the canvas.
            // left, top, right, bottom
            bitmapWrapper.drawRect(left, 0, right, bottom - bitmapWrapper.getHeight(), paint);
            bitmapWrapper.drawRect(left, currentRowTopEdge, right, bitmapWrapper.getHeight(), paint);
        } else {
            bitmapWrapper.drawRect(left, currentRowTopEdge, right, bottom, paint);
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
