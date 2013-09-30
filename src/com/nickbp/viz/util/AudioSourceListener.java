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

public interface AudioSourceListener {
	/**
	 * Type used when the device output, such as music from a music player application, is the
	 * active data source.
	 */
	public static final int SOURCE_TYPE_PLAYER = 1;
	
	/**
	 * Type used when the device microphone is the active data source.
	 */
	public static final int SOURCE_TYPE_MICROPHONE = 2;
	
	/**
	 * Notifies that the source of audio data has changed.
	 * 
	 * @param sourceType source type, one of SOURCE_TYPE_PLAYER/MICROPHONE/NO_AUDIO
	 */
	public void onSourceSwitched(int sourceType);
}
