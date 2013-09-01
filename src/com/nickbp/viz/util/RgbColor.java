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

/**
 * A utility for quickly converting from HSL to RGB color spaces.
 */
public class RgbColor {
	private static final float ONE_SIXTH = 1 / 6.f;
	private static final float ONE_THIRD = 1 / 3.f;
	private static final float ONE_HALF = 1 / 2.f;
	private static final float TWO_THIRDS = 2 / 3.f;
	
	/**
	 * Constructs a new RGB color instance. Defaults to black.
	 */
	public RgbColor() {
		R = G = B = 0;
	}
	
	/**
	 * The red component of the color. Range: 0.0 to 1.0. 
	 */
	public float R;
	/**
	 * The green component of the color. Range: 0.0 to 1.0. 
	 */
	public float G;
	/**
	 * The blue component of the color. Range: 0.0 to 1.0. 
	 */
	public float B;
	
	/**
	 * Returns an android-style int color for the current R/G/B values.
	 */
	public int toIntColor() {
		return Color.rgb((int)(R * 255), (int)(G * 255), (int)(B * 255));
	}
	
	/**
	 * Given the provided HSL color values, converts and saves the equivalent RGB values to the
	 * appropriately named local members.
	 */
	public RgbColor setFromHsl(final float H, final float S, final float L) {
		if (S == 0) {
			R = G = B = L;
		} else {
			float q = (L < 0.5) ? L * (1 + S) : (L + S) - (L * S);
			float p = (2 * L) - q;
			R = hueToRgbVal(p, q, H + ONE_THIRD);
			G = hueToRgbVal(p, q, H);
			B = hueToRgbVal(p, q, H - ONE_THIRD);
		}
		return this;
	}

	private static float hueToRgbVal(final float p, final float q, float t) {
		if (t < 0) {
			++t;
		}
		if (t > 1) {
			--t;
		}
		if (t < ONE_SIXTH) {
			return p + ((q - p) * 6 * t);
		} else if (t < ONE_HALF) {
			return q;
		} else if (t < TWO_THIRDS) {
			return p + ((q - p) * (TWO_THIRDS - t) * 6);
		} else {
			return p;
		}
	}
	
	public String toString() {
		return "rgb[" + R + ", " + G + ", " + B + "]";
	}
}
