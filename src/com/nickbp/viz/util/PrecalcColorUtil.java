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

import android.graphics.Color;

public class PrecalcColorUtil {
	// Exponent used for exaggerating the luminosity low values to make them more visible.
	private static final double LUM_EXPONENT = 0.85;
	
	// Maximum luminosity value
	private static final float MAX_LUM = 0.5f;
	
	/**
	 * This is a set of tables which have precalculated FFT->spectrum values, in cases where the
	 * lookup space is limited.
	 * 
	 * Each table takes up around 16K floats * (sizeof(float) = 32b) = 64KByte.
	 */
	private static int BUFFER_SIZE = 16513;//(128 << 7) + 128 + 1
	
	/**
	 * Each FFT data point consists of an imaginary byte and a real byte. The magnitude of the
	 * point is calculated as {@code magnitude = sqrt(real^2 + imaginary^2)}.
	 */
	public static final float[] PRECALCULATED_MAGNITUDE_BUFFER = new float[BUFFER_SIZE];
	
	/**
	 * The luminosity to be used for a given magnitude is directly derived from it.
	 */
	public static final float[] PRECALCULATED_LUMINOSITY_BUFFER = new float[BUFFER_SIZE];
	
	/**
	 * And finally, the color code is directly derived from the magnitude and the luminosity.
	 */
	public static final int[] PRECALCULATED_COLOR_BUFFER = new int[BUFFER_SIZE];
	
	static {
		final double maxCombinedVal = Math.sqrt(2 * (127 * 127));
		for (int i = 0; i <= 128; ++i) {
			for (int j = 0; j <= 128; ++j) {
				int key = (i << 7) + j;
				float value = (float)(Math.sqrt((i * i) + (j * j)) / maxCombinedVal);
				PRECALCULATED_MAGNITUDE_BUFFER[key] = value;
				PRECALCULATED_LUMINOSITY_BUFFER[key] = valueToLum(value);
				PRECALCULATED_COLOR_BUFFER[key] = valueToColor(
					PRECALCULATED_MAGNITUDE_BUFFER[key], PRECALCULATED_LUMINOSITY_BUFFER[key]);
			}
		}
	}
	
	private PrecalcColorUtil() {
	}
	
	/**
	 * Given a raw FFT real+imaginary pair, returns the buffer key for the pair.
	 */
	public static int fftToKey(byte real, byte imaginary) {
		return (Math.abs(real) << 7) + Math.abs(imaginary);
	}
	
	/**
	 * Given an FFT magnitude, returns an appropriate luminosity value.
	 */
	public static float valueToLum(float value) {
		return Math.min(MAX_LUM, (float)Math.pow(value, LUM_EXPONENT));
	}
	
	// HSL math

	private static final float ONE_SIXTH = 1 / 6.f;
	private static final float ONE_THIRD = 1 / 3.f;
	private static final float ONE_HALF = 1 / 2.f;
	private static final float TWO_THIRDS = 2 / 3.f;
	
	/**
	 * Given a calculated value and a desired luminosity for that value, returns an Android color
	 * code.
	 */
	public static int valueToColor(float value, float lum) {
		// Shortcut: S is always 1
		float H = ONE_THIRD * (1 - value);
		lum *= 2;
		if (lum < 1) {
			return Color.rgb(
					(int)(hueToRgbValWithP0(lum, H + ONE_THIRD) * 255),
					(int)(hueToRgbValWithP0(lum, H) * 255),
					(int)(hueToRgbValWithP0(lum, H - ONE_THIRD) * 255));
		} else {
			lum -= 1;
			return Color.rgb(
					(int)(hueToRgbValWithQ1(lum, H + ONE_THIRD) * 255),
					(int)(hueToRgbValWithQ1(lum, H) * 255),
					(int)(hueToRgbValWithQ1(lum, H - ONE_THIRD) * 255));
		}
	}

	private static float hueToRgbValWithP0(final float q, float t) {
		if (t < 0) {
			++t;
		}
		if (t < ONE_SIXTH) {
			return q * 6 * t;
		} else if (t < ONE_HALF) {
			return q;
		} else if (t < TWO_THIRDS) {
			return q * (TWO_THIRDS - t) * 6;
		} else {
			return 0;
		}
	}

	private static float hueToRgbValWithQ1(final float p, float t) {
		if (t < 0) {
			++t;
		}
		if (t < ONE_SIXTH) {
			return p + ((1 - p) * 6 * t);
		} else if (t < ONE_HALF) {
			return 1;
		} else if (t < TWO_THIRDS) {
			return p + ((1 - p) * (TWO_THIRDS - t) * 6);
		} else {
			return p;
		}
	}
}
