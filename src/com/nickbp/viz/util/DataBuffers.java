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
	
	// How much bass should be exaggerated compared to treble. Higher value = more exaggeration.
	private static final double SCALE = 0.8;
	
	// How quickly the smooth data should be able to change. Smaller value = slower.
	private static final float SMOOTHING_FALLOFF = 0.50f;
	
	/**
	 * Immediate spectrum data, with no smoothing beyond simple cleanup from the original FFT.
	 * Each value is an amplitude from 0.0f to 1.0f (inclusive).
	 */
	public final float[] buffer;
	
	/**
	 * Smoothed spectrum data, where values change more gradually with an eye to smooth movement
	 * over time. Otherwise the same type of data, with the same dimensions, as {@link buffer}.
	 */
	public final float[] timeSmoothedBuffer;
	
	/**
	 * This buffer is different from the two spectrum buffers. This is effectively a lookup table
	 * which provides scaling of spectrum data to the current display. This depends on the value
	 * last passed to {@link #updateViewScaling(int)}, which recomputes this table.
	 * 
	 * For each data point in {@link #buffer} and {@link #timeSmoothedBuffer}, this gives the
	 * display width, in fractional pixels, that that data point should be displayed as.
	 * For example, if bufferPxWidth[0] = 5.0f, then that means buffer[0] and timeSmoothedBuffer[0]
	 * could be shown on the screen as 5 pixels wide.
	 * 
	 * As such, this table has the same dimensions as {@link #buffer} and
	 * {@link #timeSmoothedBuffer}.
	 */
	public final float[] bufferPxWidth;

	/**
	 * This is a lovely precalculated table of FFT->spectrum values. Each FFT data point consists of
	 * an imaginary byte and a real byte. The magnitude of the point is calculated as 
	 * {@code magnitude = sqrt(real^2 + imaginary^2)}.
	 * 
	 * This table takes up around 16K floats * (sizeof(float) = 32b) = 64KByte.
	 */
	private static final float[] PRECALCULATED_MAGNITUDE_BUFFER =
		new float[16513];//(128 << 7) + 128 + 1
	static {
		final double maxCombinedVal = Math.sqrt(2 * (127 * 127));
		for (int i = 0; i <= 128; ++i) {
			for (int j = 0; j <= 128; ++j) {
				int key = (i << 7) + j;
				PRECALCULATED_MAGNITUDE_BUFFER[key] =
				    (float)(Math.sqrt((i * i) + (j * j)) / maxCombinedVal);
			}
		}
	}
	
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
		// Non-'endcap' values are being given as real+imaginary pairs, which need to be recombined.
		// The two 'endcap' values stay.
		// eg 6 -> 2 + (4 / 2) -> 4 (or 6/2+1)
		// eg 10 -> 2 + (8 / 2) -> 6 (or 10/2+1
		int keptDataSize = customFftSize / 2 + 1; 
		buffer = new float[keptDataSize];
		timeSmoothedBuffer = new float[keptDataSize];
		bufferPxWidth = new float[keptDataSize];
		
	}

	/**
	 * Processes the provided FFT data and updates {@link #buffer} and {@link #timeSmoothedBuffer}
	 * with it. Returns {@code true} if the passed FFT data contains any non-zero values.
	 * 
	 * @param fft The raw FFT data of the format produced by a {@link Visualizer}.
	 * @throws IllegalStateException if the provided buffer doesn't match the expected size provided
	 * by {@link #DataBuffers()} or {@link #DataBuffers(int)}.
	 */
	public boolean updateData(byte[] fft) {
        int expectfft = (buffer.length - 1) * 2; 
        if (expectfft != fft.length) {
            throw new IllegalStateException(
            		"Data size=" + fft.length + " doesn't match expected size=" + expectfft);
		}
		
		// first, save the endcaps, which are at 0 and 1
		// oddity with fft data: the value at fft[1] should actually be at the BACK of fft.
		buffer[0] = Math.abs(fft[0]) / 127.f;
		buffer[buffer.length - 1] = Math.abs(fft[1]) / 127.f;
		
		// then, combine and store the non-endcap real+imaginary pairs
		boolean valueFound = false;
		for (int ffti = 2, bufferi = 1; ffti < fft.length; ffti += 2, ++bufferi) {
			// fft[ffti] is real, fft[ffti+1] is imaginary
			int key = (Math.abs(fft[ffti]) << 7) + Math.abs(fft[ffti + 1]);
			if (key != 0) {
				// Found a non-zero value.
				valueFound = true;
			}
			buffer[bufferi] = PRECALCULATED_MAGNITUDE_BUFFER[key];
		}
		
		// update smoothed values using new raw values
		for (int i = 0; i < buffer.length; ++i) {
			//fft range is [-128,127], where val is abs(fft)
			//buffer[i] = Math.abs(fft[i]) / 128.f;
			// Go with a linear decrease in the analyzer - avoids the appearance of
			// disconnectedness between analyzer and voiceprint, without making the analyzer look
			// too jittery.
	        timeSmoothedBuffer[i] = Math.max(buffer[i], timeSmoothedBuffer[i] - SMOOTHING_FALLOFF);
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
		final int bufferLength = buffer.length;
        double multiplier = viewLength * (SCALE + 1) / Math.pow(bufferLength, SCALE + 1);
        for (int i = 0; i < bufferLength; ++i) {
            bufferPxWidth[i] = (float)(Math.pow(bufferLength - i, SCALE) * multiplier);
        }
		
		this.viewLength = viewLength;
	}
}
