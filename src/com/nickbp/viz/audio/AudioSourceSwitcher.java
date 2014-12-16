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
     * When empty data is being received, length of time to wait until switching to mic.
     * This is relatively large to avoid switching away from audio when it's just quiet for a bit.
     */
    private static final int SECONDS_BEFORE_MIC_START = 5;
    /**
     * When non-empty data is being received, length of time to wait until switching to player.
     * We want to switch fairly quickly when the user has started music, but we don't want to switch
     * just because eg a notification sound occurred.
     */
    private static final int SECONDS_BEFORE_MIC_STOP = 3;

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
        private final int playerDataTicksBeforeMicStart;
        private final int playerDataTicksBeforeMicStop;
        private final AudioSourceListener sourceListener;

        private boolean usingPlayerOutput;
        private int ticksSinceSwitchingSources;

        public FallbackSwitcher(AudioSourceListener sourceListener) {
            playerDataTicksBeforeMicStart =
                SECONDS_BEFORE_MIC_START * playerDataSource.getDataRefreshRateHz();
            playerDataTicksBeforeMicStop =
                SECONDS_BEFORE_MIC_STOP * playerDataSource.getDataRefreshRateHz();
            this.sourceListener = sourceListener;

            usingPlayerOutput = true;
            // Start with a fuzz factor:
            //   Give player stream some time to init, before immediately switching to mic data.
            ticksSinceSwitchingSources = playerDataTicksBeforeMicStart - 10;
        }

        public void handleFilledData() {
            if (!usingPlayerOutput) {
                // Not using player, but it's producing audio!
                ++ticksSinceSwitchingSources;
                if (ticksSinceSwitchingSources == playerDataTicksBeforeMicStop) {
                    // Player's been active long enough, switch to it.
                    micDataSource.stop();
                    sourceListener.onSourceSwitched(AudioSourceListener.SOURCE_TYPE_PLAYER);
                    usingPlayerOutput = true;
                    ticksSinceSwitchingSources = 0;
                }
            } else {
                // Reset any past increments from brief player activity
                ticksSinceSwitchingSources = 0;
            }
        }

        public void handleEmptyData() {
            if (usingPlayerOutput) {
                // Using player, but it's not producing any audio!
                ++ticksSinceSwitchingSources;
                if (ticksSinceSwitchingSources == playerDataTicksBeforeMicStart) {
                    // We've waited long enough, switch to mic.
                    micDataSource.start(micDataListener);
                    sourceListener.onSourceSwitched(AudioSourceListener.SOURCE_TYPE_MICROPHONE);
                    usingPlayerOutput = false;
                    ticksSinceSwitchingSources = 0;
                }
            } else {
                // Reset any past increments from brief player inactivity
                ticksSinceSwitchingSources = 0;
            }
        }

        public boolean isPlayerOutputEnabled() {
            return usingPlayerOutput;
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
