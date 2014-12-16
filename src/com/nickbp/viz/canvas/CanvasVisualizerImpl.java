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

import android.graphics.Canvas;

import com.nickbp.viz.util.DataBuffers;

public interface CanvasVisualizerImpl {
    /**
     * Given the provided new {@code data}, renders the visualization's current state onto the
     * provided {@code canvas}.
     */
    public void render(DataBuffers data, Canvas canvas);

    /**
     * Notifies the visualization that the display dimensions have changed.
     */
    public void resize(int viewWidth, int viewHeight);
}
