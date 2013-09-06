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

package com.nickbp.viz.util;

import android.util.Log;

/**
 * Handles scaling of bass vs treble.
 * This is calculated as "given datapoint x, how wide should it be?"
 */
public class DataLengths {
	private static final String TAG = "DataLengths";
	
	// How much lows/mids should be exaggerated compared to highs. Higher value = more exaggeration.
	private static final double VIEW_SCALING_BASS_EXAGGERATION = 1.5;
	
	/**
	 * This is effectively a lookup table which provides scaling of spectrum data to the current
	 * display. This depends on the value last passed to {@link #updateViewScaling(int)}, which
	 * recomputes this table.
	 * 
	 * For each data point in {@link DataBuffers#valBuffer} and
	 * {@link DataBuffers#timeSmoothedValBuffer}, this gives the display width, in fractional
	 * pixels, that that data point should be displayed as. For example, if bufferPxWidth[0] = 5.0f,
	 * then that means buffer[0] and timeSmoothedBuffer[0] could be shown on the screen as 5 pixels
	 * wide.
	 * 
	 * As such, this table has the same dimensions as {@link DataBuffers#valBuffer} and
	 * {@link DataBuffers#timeSmoothedValBuffer}.
	 */
	public final float[] bufferPxWidth;
	
	private int viewLength = -1;
	
	/**
	 * Creates an instance which expects raw FFT data of size equal to
	 * {@link AudioSourceUtil#getMaxCaptureSize()}.
	 */
	public DataLengths() {
		this(AudioSourceUtil.getMaxCaptureSize());
	}

	/**
	 * Creates an instance which expects raw FFT data of size equal to {@code customFftSize}.
	 */
	public DataLengths(int customFftSize) {
		bufferPxWidth = new float[AudioSourceUtil.getKeptDataSize(customFftSize)];
	}
	
	/**
	 * After the display has resized, this may be called to update the {@link #bufferPxWidth}
	 * precalculated display scaling.
	 * @param viewLength The new display width, in pixels, to calculate against.
	 */
	public void updateViewScaling(int viewLength) {
		if (this.viewLength == viewLength) {
			return;
		}

		Log.v(TAG, "Updating scaling for length: " + viewLength);
        // Formula:
		//   pxlen = (dataLen - dataI)^scale / dataLen^scale
        // Integrate over dataI from 0 to dataLen:
		//   sum(pxlen) = dataLen / (scale + 1)
        // Scaled formula:
		//   pxlen = (dataLen - dataI)^scale * viewLen * (scale + 1) / dataLen^(scale + 1)
		final int bufferLength = bufferPxWidth.length;
        double multiplier =
        	viewLength * (VIEW_SCALING_BASS_EXAGGERATION + 1) /
        	Math.pow(bufferLength, VIEW_SCALING_BASS_EXAGGERATION + 1);
        for (int i = 0; i < bufferLength; ++i) {
            bufferPxWidth[i] = (float)(Math.pow(bufferLength - i, VIEW_SCALING_BASS_EXAGGERATION) * multiplier);
        }
		
		this.viewLength = viewLength;
	}
}
