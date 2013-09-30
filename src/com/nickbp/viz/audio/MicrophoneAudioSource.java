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
import com.nickbp.viz.util.FFT;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Produces audio spectrum data from the device microphone.
 */
public class MicrophoneAudioSource implements AudioSource {
	private static final String TAG = "MicrophoneAudioSource";
	// The system audio input device id.
	private static final int SOURCE_DEVICE = MediaRecorder.AudioSource.CAMCORDER;
	// "Guaranteed to work on all devices", and what we'd want anyway
	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	// "Guaranteed to be supported by devices"
	private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

	private static int SAMPLE_RATE_HZ;
	private static int BUF_SZ;
	static {
		// "44100Hz is the only rate that is guaranteed to work on all devices", but this leads to
		// a bit of overkill in data resolution. 11025 clips off too much treble, so skip that.
		int ratesToTry[] = { 16000, 22050, 44100 };
		for (int i = 0; i < ratesToTry.length; ++i) {
			int sizeOrStatus =
					AudioRecord.getMinBufferSize(ratesToTry[i], CHANNEL_CONFIG, AUDIO_FORMAT);
			if (sizeOrStatus != AudioRecord.ERROR_BAD_VALUE) {
				SAMPLE_RATE_HZ = ratesToTry[i];
				// FFT.java requires that this be a base 2 number:
				BUF_SZ = findNextBaseTwoAtOrBeyond(sizeOrStatus);
				Log.d(TAG, "Using sample rate=" + SAMPLE_RATE_HZ + "Hz, bufsize=" + BUF_SZ);
				break;
			}
		}
	}
	
	/**
	 * Returns the next base-2 number which is equal to or greater than {@code val}.
	 * Eg: 1023 returns 1024, and 1024 returns 1024.
	 */
	private static int findNextBaseTwoAtOrBeyond(int val) {
		int baseTwo = 2;
		while (baseTwo < val) {
			baseTwo *= 2;
		}
		return baseTwo;
	}
	
	private final AudioRecord audioSource;
	private Thread outputThread;
	
	/**
	 * A utility class for transforming microphone data to a spectrum before passing it to a
	 * {@link RawDataListener}. Allows us to hide some {@link AudioRecord} details from listeners.
	 */
	private class Outputter implements Runnable {
		private final AudioRecord audioSource;
		private final RawDataListener out;
		
		private Outputter(AudioRecord audioSource, RawDataListener out) {
			this.audioSource = audioSource;
			this.out = out;
		}
		
		@Override
		public void run() {
			short rawData[] = new short[BUF_SZ];
			byte fftData[] = new byte[BUF_SZ];
			FFT fft = new FFT(BUF_SZ);
			while (true) {
				if (audioSource.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
					break;
				}
				audioSource.read(rawData, 0, rawData.length);
				fft.forward(rawData);
				
				// Fill in indexes 2 thru end, to match Visualizer output. Drop the last value in
				// the original fft data, in favor of the first value.
				for (int i = 1; i < fftData.length/2; ++i) {
					fftData[i*2] = (byte)fft.real[i];
				}
				for (int i = 1; i < fftData.length/2; ++i) {
					fftData[i*2 + 1] = (byte)fft.imag[i];
				}
				
				out.onReceive(fftData);
			}
		}

	}
	
	public MicrophoneAudioSource() {
		audioSource = new AudioRecord(
				SOURCE_DEVICE, SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, BUF_SZ);
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
		if (audioSource.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			return;
		}
		if (audioSource.getState() != AudioRecord.STATE_INITIALIZED) {
			throw new IllegalStateException("Bad recording configuration");
		}
		Log.d(TAG, "Starting microphone recording with buffer size " + BUF_SZ);
		audioSource.startRecording();
		outputThread = new Thread(new Outputter(audioSource, out));
		outputThread.start();
	}
	
	/**
	 * Stops retrieving audio data, or does nothing if retrieval is already stopped.
	 */
	@Override
	public void stop() {
		if (audioSource.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
			return;
		}
		Log.d(TAG, "Stopping microphone recording");
		audioSource.stop();
		try {
			outputThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		outputThread = null;
	}

	/**
	 * Returns the size of the output data that this instance will pass to {@link RawDataListener}s.
	 */
	@Override
	public int getOutputSize() {
		return BUF_SZ;
	}
}
