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
 * Takes raw FFT data and transforms it into suitable spectrum data.
 */
public class DataBuffers {
	private static final String TAG = "DataBuffers";
	
	// How much lows/mids should be exaggerated compared to highs. Higher value = more exaggeration.
	private static final double VIEW_SCALING_BASS_EXAGGERATION = 1.5;
	
	// How quickly the smoothed data should be able to change. Smaller value = slower.
	private static final float TIME_SMOOTHING_FALLOFF = 0.15f;

	/**
	 * Immediate spectrum data, with no smoothing beyond simple cleanup from the original FFT.
	 * Each value is an amplitude from 0.0f to 1.0f (inclusive).
	 */
	public final int[] valCacheKeyBuffer;
	
	/**
	 * Smoothed spectrum data, where values change more gradually with an eye to smooth movement
	 * over time. Otherwise the same type of data, with the same dimensions, as
	 * {@link #valCacheKeyBuffer}.
	 */
	public final float[] timeSmoothedValBuffer;
	
	/**
	 * This buffer is different from the two spectrum buffers. This is effectively a lookup table
	 * which provides scaling of spectrum data to the current display. This depends on the value
	 * last passed to {@link #updateViewScaling(int)}, which recomputes this table.
	 * 
	 * For each data point in {@link #valCacheKeyBuffer} and {@link #timeSmoothedValBuffer}, this
	 * gives the display width, in fractional pixels, that that data point should be displayed as.
	 * For example, if bufferPxWidth[0] = 5.0f, then that means buffer[0] and timeSmoothedBuffer[0]
	 * could be shown on the screen as 5 pixels wide.
	 * 
	 * As such, this table has the same dimensions as {@link #valCacheKeyBuffer} and
	 * {@link #timeSmoothedValBuffer}.
	 */
	public final float[] bufferPxWidth;
	
	private int viewLength = -1;
	
	/**
	 * Creates a buffer instance which expects raw FFT data of size equal to
	 * {@link AudioSourceUtil#getMaxCaptureSize()}.
	 */
	public DataBuffers() {
		this(AudioSourceUtil.getMaxCaptureSize());
	}
	
	/**
	 * Creates a buffer instance which expects raw FFT data of size equal to {@code customFftSize}.
	 */
	public DataBuffers(int customFftSize) {
		// Skip the first two values, which are DC and Hz/2, respectively
		// Non-'endcap' values are being given as real+imaginary pairs, which need to be recombined.
		// eg 6 -> (4 / 2) -> 2 (or 6/2-1)
		// eg 10 -> (8 / 2) -> 4 (or 10/2-1)
		int keptDataSize = customFftSize / 2 - 1;
		valCacheKeyBuffer = new int[keptDataSize];
		timeSmoothedValBuffer = new float[keptDataSize];
		bufferPxWidth = new float[keptDataSize];
	}

	/**
	 * Processes the provided FFT data and updates {@link #valCacheKeyBuffer} and
	 * {@link #timeSmoothedValBuffer} with it. Returns {@code true} if the passed FFT data contains
	 * any non-zero values.
	 * 
	 * @param fft The raw FFT data of the format produced by a {@link Visualizer}.
	 * @throws IllegalStateException if the provided buffer doesn't match the expected size provided
	 * by {@link #DataBuffers()} or {@link #DataBuffers(int)}.
	 */
	public boolean updateData(byte[] fft) {
        int expectfft = (valCacheKeyBuffer.length + 1) * 2; 
        if (expectfft != fft.length) {
            throw new IllegalStateException(
            		"Data size=" + fft.length + " doesn't match expected size=" + expectfft);
		}
		
		// combine and store the non-endcap real+imaginary pairs (pairwise from idx 2 onwards)
		boolean valueFound = false;
		for (int ffti = 2, bufferi = 0; ffti < fft.length; ffti += 2, ++bufferi) {
			// fft[ffti] is real, fft[ffti+1] is imaginary
			int key = PrecalcColorUtil.fftToKey(fft[ffti], fft[ffti + 1]);
			if (key != 0) {
				// Found a non-zero value.
				valueFound = true;
			}
			valCacheKeyBuffer[bufferi] = key;

			// Update smoothed value using new raw value. Go with a linear decrease in the analyzer,
			// this avoids the appearance of disconnectedness between analyzer and voiceprint,
			// without making the analyzer look too jittery.
	        timeSmoothedValBuffer[bufferi] = Math.max(
	        	PrecalcColorUtil.PRECALCULATED_MAGNITUDE_BUFFER[key],
	        	timeSmoothedValBuffer[bufferi] - TIME_SMOOTHING_FALLOFF);
		}
		return valueFound;
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
		final int bufferLength = valCacheKeyBuffer.length;
        double multiplier = viewLength * (VIEW_SCALING_BASS_EXAGGERATION + 1) / Math.pow(bufferLength, VIEW_SCALING_BASS_EXAGGERATION + 1);
        for (int i = 0; i < bufferLength; ++i) {
            bufferPxWidth[i] = (float)(Math.pow(bufferLength - i, VIEW_SCALING_BASS_EXAGGERATION) * multiplier);
        }
		
		this.viewLength = viewLength;
	}
}
