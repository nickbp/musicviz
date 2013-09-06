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

import android.media.audiofx.Visualizer;
import android.util.Log;

public class AudioSourceUtil {
	private static final String TAG = "AudioSourceUtil";
	
	private AudioSourceUtil() {
	}
	
	/**
	 * Returns the largest available data width for system audio capture, suitable for passing to
	 * {@link Visualizer#setCaptureSize(int)}.
	 */
	public static int getMaxCaptureSize() {
		int[] range = Visualizer.getCaptureSizeRange();
		if (range.length != 2) {
			throw new IllegalStateException(
					"Expected 2 elements in capture size range, got " + range.length);
		}
		if (range[0] > range[1]) {
			Log.w(TAG, "Capture size range is backwards: [" + range[0] + ", " + range[1] + "]");
			int tmp = range[0];
			range[0] = range[1];
			range[1] = tmp;
		}
		Log.v(TAG, "Capture size range: [" + range[0] + ", " + range[1] + "]");
		return findLargestBaseTwoInInclusiveRange(range[0], range[1]);
	}
	
	private static int findLargestBaseTwoInInclusiveRange(int min, int max) {
		int largest = 0;
		for (int i = 2; i <= max; i *= 2) {
			if (i < min) {
				continue;
			}
			largest = i;
		}
		if (largest < min) {
			throw new IllegalStateException(
					"Unable to find a base two integer within [" + min + ", " + max + "]");
		}
		return largest;
	}
	
	/**
	 * Returns the maximum rate, in millihertz, that system audio may be captured. 
	 */
	public static int getMaxCaptureRateMilliHz() {
		int maxRate = Visualizer.getMaxCaptureRate();
		Log.v(TAG, "Max capture rate: " + maxRate + " milliHz");
		return Visualizer.getMaxCaptureRate();
	}
	
	/**
	 * Returns the number of values which will result from an FFT buffer of the provided size.
	 */
	public static int getKeptDataSize(int fftSize) {
		// Skip the first two values, which are DC and Hz/2, respectively
		// Non-'endcap' values are being given as real+imaginary pairs, which need to be recombined.
		// eg 6 -> (4 / 2) -> 2 (or 6/2-1)
		// eg 10 -> (8 / 2) -> 4 (or 10/2-1)
		return fftSize / 2 - 1;
	}
}
