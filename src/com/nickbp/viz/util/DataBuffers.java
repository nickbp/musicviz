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

/**
 * Takes raw FFT data and transforms it into suitable spectrum data.
 */
public class DataBuffers {
    // How quickly the smoothed data should be able to change. Smaller value = slower.
    private static final float TIME_SMOOTHING_FALLOFF = 0.15f;

    /**
     * Immediate spectrum data, with no smoothing beyond simple cleanup from the original FFT.
     * Each value is an amplitude from 0.0f to 1.0f (inclusive).
     */
    public final float[] valBuffer;

    /**
     * Smoothed spectrum data, where values change more gradually with an eye to smooth movement
     * over time. Otherwise the same type of data, with the same dimensions, as
     * {@link #valBuffer}.
     */
    public final float[] timeSmoothedValBuffer;

    /**
     * Creates a buffer instance which expects raw FFT data of size equal to {@code customFftSize}.
     */
    public DataBuffers(int customFftSize) {
        int keptDataSize = getKeptDataSize(customFftSize);
        valBuffer = new float[keptDataSize];
        timeSmoothedValBuffer = new float[keptDataSize];
    }

    /**
     * Processes the provided FFT data and updates {@link #valBuffer} and
     * {@link #timeSmoothedValBuffer} with it. Returns {@code true} if the passed FFT data contains
     * any non-zero values.
     *
     * @param fft The raw FFT data of the format produced by a {@link Visualizer}.
     * @throws IllegalStateException if the provided buffer doesn't match the expected size provided
     * by {@link #DataBuffers()} or {@link #DataBuffers(int)}.
     */
    public boolean updateData(byte[] fft) {
        int expectfft = (valBuffer.length + 1) * 2;
        if (expectfft != fft.length) {
            throw new IllegalStateException(
                "Data size=" + fft.length + " doesn't match expected size=" + expectfft);
        }

        int key;
        float magnitude;
        boolean valueFound = false;
        // combine and store the non-endcap real+imaginary pairs (pairwise from idx 2 onwards)
        for (int ffti = 2, bufferi = 0; ffti < fft.length; ffti += 2, ++bufferi) {
            // fft[ffti] is real, fft[ffti+1] is imaginary
            key = PrecalcColorUtil.fftToKey(fft[ffti], fft[ffti + 1]);
            if (key != 0) {
                valueFound = true;
            }
            magnitude = PrecalcColorUtil.keyToMagnitude(key);
            valBuffer[bufferi] = magnitude;

            // Update smoothed value using new raw value. Go with a linear decrease in the analyzer,
            // this avoids the appearance of disconnectedness between analyzer and voiceprint,
            // without making the analyzer look too jittery.
            timeSmoothedValBuffer[bufferi] = Math.max(magnitude,
                    timeSmoothedValBuffer[bufferi] - TIME_SMOOTHING_FALLOFF);
        }
        return valueFound;
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
