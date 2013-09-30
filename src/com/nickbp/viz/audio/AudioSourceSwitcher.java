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

import com.nickbp.viz.util.DataBufferListener;
import com.nickbp.viz.util.DataBuffers;
import com.nickbp.viz.util.AudioSourceListener;

public class AudioSourceSwitcher {
	/**
	 * When empty audio data is being received, length of time to wait falling back from player data
	 * to microphone data. We immediately switch back to player data once it's observed non-zero.
	 */
	private static final int SECONDS_BEFORE_MIC_FALLBACK = 10;
	
    private final PlayerAudioSource playerDataSource = new PlayerAudioSource();
    private final AudioSource micDataSource = new MicrophoneAudioSource();
    
    private FallbackSwitcher switcher;
    private PlayerDataListener playerDataListener;
    private AudioSource.RawDataListener micDataListener;

	public void start(AudioSourceListener sourceListener, DataBufferListener dataListener) {
		switcher = new FallbackSwitcher(sourceListener);
		
    	playerDataListener =
    			new PlayerDataListener(switcher, dataListener, playerDataSource.getOutputSize());
    	playerDataSource.start(playerDataListener);
    	
    	micDataListener = new PassthruListener(dataListener, micDataSource.getOutputSize());
	}

	public void stop() {
		playerDataSource.stop();
		micDataSource.stop();
	}
	
	private class FallbackSwitcher {
	    private boolean playerOutputEnabled;
    	private final int playerDataTicksBeforeMicFallback;
    	private int ticksSincePlayerData;
	    private final AudioSourceListener sourceListener;
	    
	    public FallbackSwitcher(AudioSourceListener sourceListener) {
	    	playerOutputEnabled = true;
	    	playerDataTicksBeforeMicFallback =
	    			SECONDS_BEFORE_MIC_FALLBACK * playerDataSource.getDataRefreshRateHz();
	    	// Fuzz factor: Avoid switching inputs when data stream may still be initializing.
	    	ticksSincePlayerData = playerDataTicksBeforeMicFallback - 10;

	    	this.sourceListener = sourceListener; 
	    }
	    
	    public void handleFilledData() {
			if (ticksSincePlayerData != 0) {
				ticksSincePlayerData = 0;
				micDataSource.stop();
				sourceListener.onSourceSwitched(AudioSourceListener.SOURCE_TYPE_PLAYER);
				playerOutputEnabled = true;
			}
	    }
	    
	    public void handleEmptyData() {
			++ticksSincePlayerData;
			if (ticksSincePlayerData == playerDataTicksBeforeMicFallback) {
				playerOutputEnabled = false;
				micDataSource.start(micDataListener);
				sourceListener.onSourceSwitched(AudioSourceListener.SOURCE_TYPE_MICROPHONE);
			}
	    }
	    
	    public boolean isPlayerOutputEnabled() {
	    	return playerOutputEnabled;
	    }
	}
    
	private static class PlayerDataListener implements AudioSource.RawDataListener {
    	private final FallbackSwitcher switcher;
    	private final DataBufferListener sharedDataListener;
    	private final DataBuffers data;

    	private PlayerDataListener(FallbackSwitcher switcher, DataBufferListener dataListener,
    			int bufferSize) {
    		this.switcher = switcher;
    		this.sharedDataListener = dataListener;
    		data = new DataBuffers(bufferSize);
    	}
    	
		@Override
		public void onReceive(byte[] fft) {
			if (switcher.isPlayerOutputEnabled()) {
				if (data.updateData(fft)) {
					switcher.handleFilledData();
				} else {
					switcher.handleEmptyData();
				}
			} else {
				// muted, so just check fft locally. saves some computation of smoothed data.
				boolean valueFound = false;
				for (byte b : fft) {
					if (b != 0) {
						valueFound = true;
						break;
					}
				}
				
				if (valueFound) {
					// oh hey we found something! reset playerData and resume normal operation
					data.updateData(fft);
					switcher.handleFilledData();
				} else {
					switcher.handleEmptyData();
				}
			}
			
			if (switcher.isPlayerOutputEnabled()) {
				sharedDataListener.onReceive(data, false);
			}
		}
	}
	
	private class PassthruListener implements AudioSource.RawDataListener {
    	private final DataBufferListener sharedDataListener;
    	private final DataBuffers data;

    	private PassthruListener(DataBufferListener dataListener, int bufferSize) {
    		this.sharedDataListener = dataListener;
    		data = new DataBuffers(bufferSize);
    	}
    	
		@Override
		public void onReceive(byte[] fft) {
			data.updateData(fft);
			sharedDataListener.onReceive(data, true);
		}
	}
}
