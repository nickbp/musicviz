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

/**
 * Produces audio spectrum data from the device, suitable for use by visualizations.
 */
public interface AudioSource {
	public interface RawDataListener {
		/**
		 * Accepts raw FFT data as would be produced by a {@link Visualizer} instance.
		 */
		public void onReceive(byte[] fft);
	}
	
	/**
	 * Starts capturing and retrieving audio data, forwarding it to the provided
	 * {@link RawDataListener}, or does nothing if recording is already started.
	 * Returns immediately once retrieval is set up.
	 * 
	 * @throws IllegalStateException if audio capture couldn't be enabled
	 */
	public void start(RawDataListener out);
	
	/**
	 * Stops retrieving audio data, or does nothing if retrieval is already stopped.
	 */
	public void stop();
	
	/**
	 * Returns the size of the output data that this instance will pass to {@link RawDataListener}s.
	 */
	public int getOutputSize();
}
