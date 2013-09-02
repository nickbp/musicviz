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

package com.nickbp.viz;

import com.nickbp.viz.canvas.CanvasVisualizerView;
import com.nickbp.viz.hider.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

/**
 * The main/sole activity of the visualizer app.
 */
public class VisualizerActivity extends Activity {
    private static final String TAG = "VisualizerActivity";
    
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int VIEWCHANGE_HIDE_DELAY_MILLIS = 100;

    private final AudioSource audioSource = new AudioSource();
    private final Handler hideHandler = new Handler();
    private SystemUiHider systemUiHider;
    private CanvasVisualizerView vizView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	Log.d(TAG, "onCreate");
    	
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	
    	vizView = new CanvasVisualizerView(this);
    	vizView.setInteractionListeners(new Runnable() {
			@Override
			public void run() {
				// When someone touches within the app view, also notify our hider.
				if (systemUiHider.isVisible()) {
					delayedControls("interactionListener", false, VIEWCHANGE_HIDE_DELAY_MILLIS);
				}
			}
	    });
		setContentView(vizView);
	
	    // Set up an instance of SystemUiHider to control the system UI for this activity.
	    systemUiHider =
	    		SystemUiHider.getInstance(this, vizView, SystemUiHider.FLAG_HIDE_NAVIGATION);
	    systemUiHider.setup();
	    systemUiHider.setOnVisibilityChangeListener(
	    		new SystemUiHider.OnVisibilityChangeListener() {
	                @Override
	                @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	                public void onVisibilityChange(boolean visible) {
	                    // Ensure everything's visible now, then schedule a hide.
                    	if (visible && !systemUiHider.isVisible()) {
                    		delayedControls("onVisibilityChange", true, 0);
                    		delayedControls("onVisibilityChange", false, AUTO_HIDE_DELAY_MILLIS);
	                    }
	                }
	            });
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	// Hide controls on startup/rotate
        delayedControls("onStart", false, VIEWCHANGE_HIDE_DELAY_MILLIS);
        audioSource.start(vizView);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	audioSource.stop();
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedControls("onPostCreate", false, VIEWCHANGE_HIDE_DELAY_MILLIS);
    }

    /**
     * Schedules a call to show/hide system controls in [delay] milliseconds, canceling any
     * previously scheduled calls. It's best to have all hide/show commands go through this queue,
     * to avoid jerky control showing/hiding due to multiple conflicting simultaneous messages.
     */
    private void delayedControls(final String source, final boolean show, int delayMillis) {
    	Log.v(TAG, "controls (" + source + "): delayMs=" + delayMillis + ", show=" + show);
    	Runnable hideRunnable = new Runnable() {
            @Override
            public void run() {
        		Log.v(TAG, "controls (" + source + "): show=" + show);
            	if (show) {
                	systemUiHider.show();
            		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            	} else {
                	systemUiHider.hide();
            		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            	}
            }
        };
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, delayMillis);
    }
}
