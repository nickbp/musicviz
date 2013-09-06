package com.nickbp.viz.canvas;

import android.graphics.Canvas;

import com.nickbp.viz.util.DataBuffers;
import com.nickbp.viz.util.DataLengths;

public interface CanvasVisualizerImpl {
	/**
	 * Given the provided new {@code data}, renders the visualization's current state onto the
	 * provided {@code canvas}.
	 */
	public void render(DataBuffers data, DataLengths lengths, Canvas canvas);

	/**
	 * Notifies the visualization that the display dimensions have changed.
	 * @return The data display width that should be used for future incoming data via
	 * {@link #render(DataBuffers, Canvas)}.
	 */
	public int resize(int viewWidth, int viewHeight);
}
