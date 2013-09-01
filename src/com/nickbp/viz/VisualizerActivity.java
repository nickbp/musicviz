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

    private final AudioSource audioSource = new AudioSource();
    private final Handler hideHandler = new Handler();
    private SystemUiHider systemUiHider;
    private CanvasVisualizerView vizView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	Log.d(TAG, "onCreate");
    	
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// Keep screen on while we're visible
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
    	vizView = new CanvasVisualizerView(this);
    	vizView.setInteractionListeners(new Runnable() {
			@Override
			public void run() {
				// When someone touches within the app view, also notify our hider.
	            setShowControls(!systemUiHider.isVisible());
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
	                    if (visible) {
	                        // Ensure everything's visible, schedule a hide().
	                    	setShowControls(true);
	                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
	                    }
	                }
	            });
    }
    
    /**
     * Depending on the value of {@code show}, shows or hides system controls.
     * These include the status bar at the top and back/home/appswitch at the bottom.
     *  
     * @param show If {@code true}, shows controls, otherwise hides them. 
     */
    private void setShowControls(boolean show) {
    	if (show) {
        	systemUiHider.show();
    		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	} else {
        	systemUiHider.hide();
    		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	}
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	// Hide controls on startup
		setShowControls(false);
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
        delayedHide(100);
    }

    /**
     * Schedules a call to hide system controls in [delay] milliseconds,
     * canceling any previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
    	Runnable hideRunnable = new Runnable() {
            @Override
            public void run() {
            	setShowControls(false);
            }
        };
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, delayMillis);
    }
}
