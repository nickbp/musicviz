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

package com.nickbp.viz.audio;

import com.nickbp.viz.audio.AudioSource.RawDataListener;

import android.media.audiofx.Visualizer;
import android.util.Log;

/**
 * Produces audio spectrum data from the output of any applications producing audio, such as music
 * players or games.
 */
public class PlayerAudioSource implements AudioSource {
	private static final String TAG = "PlayerAudioSource";
	// The system audio session id:
	private static final int VIZ_SESSION = 0;
	private final Visualizer audioSource;
	
	/**
	 * A utility class for directly forwarding audio data to a {@link RawDataListener}.
	 * Allows us to hide some {@link Visualizer} details from listeners.
	 */
	private static class PassthruListener implements Visualizer.OnDataCaptureListener {
		private final RawDataListener out;
		
		private PassthruListener(RawDataListener out) {
			this.out = out;
		}
		
		@Override
		public void onFftDataCapture(
				Visualizer visualizer, byte[] fft, int samplingRate) {
			out.onReceive(fft);
		}

		@Override
		public void onWaveFormDataCapture(
				Visualizer visualizer, byte[] waveform, int samplingRate) {
			throw new IllegalStateException("Waveform not supported.");
		}
	}
	
	public PlayerAudioSource() {
		/* Note: Despite using the default normalized scaling mode (SCALING_MODE_NORMALIZED),
		 * it apparently still falls apart if the volume is too low, dropping off with volume.
		 * Oh well~ */ 
		audioSource = new Visualizer(VIZ_SESSION);
	}
	
	/**
	 * Starts capturing and retrieving audio data, forwarding it to the provided
	 * {@link RawDataListener}, or does nothing if recording is already started.
	 * Returns immediately once retrieval is set up.
	 * 
	 * @throws IllegalStateException if audio capture couldn't be enabled
	 */
	@Override
	public void start(RawDataListener out) {
		if (audioSource.getEnabled()) {
			return;
		}
		int largestSize = getOutputSize();
		Log.d(TAG, "Using captureSize " + largestSize);
		if (audioSource.setCaptureSize(largestSize) != Visualizer.SUCCESS) {
			throw new IllegalStateException("Bad capture size: " + largestSize);
		}
		int maxMilliHz = getDataRefreshRateHz();
		Log.d(TAG, "Starting viz with hz=" + maxMilliHz);
		audioSource.setDataCaptureListener(new PassthruListener(out), maxMilliHz * 1000, false, true);
		audioSource.setEnabled(true);
	}
	
	/**
	 * Stops retrieving audio data, or does nothing if retrieval is already stopped.
	 */
	@Override
	public void stop() {
		if (!audioSource.getEnabled()) {
			return;
		}
		Log.d(TAG, "Stopping viz");
		audioSource.setDataCaptureListener(null, 0, false, false);
		audioSource.setEnabled(false);
	}

	/**
	 * Returns the maximum rate, in Hz, that system audio may be captured. 
	 */
	public int getDataRefreshRateHz() {
		int maxRateHz = Visualizer.getMaxCaptureRate() / 1000;
		Log.v(TAG, "Max capture rate: " + maxRateHz + " Hz");
		return maxRateHz;
	}
	
	/**
	 * Returns the largest available data width for system audio capture, suitable for passing to
	 * {@link Visualizer#setCaptureSize(int)}.
	 */
	public int getOutputSize() {
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
}
