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

package com.nickbp.viz.canvas;

import com.nickbp.viz.R;
import com.nickbp.viz.util.AudioSourceUtil;
import com.nickbp.viz.util.DataBuffers;
import com.nickbp.viz.util.DataListener;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

/**
 * The {@link View} for a {@link CanvasVisualizerImpl}.
 * Handles user interaction and forwarding audio data.
 */
public class CanvasVisualizerView extends View implements DataListener {
	private static final int SECONDS_BEFORE_NO_MUSIC_MESSAGE = 5;
	private static final int TICKS_BEFORE_NO_MUSIC_MESSAGE =
			SECONDS_BEFORE_NO_MUSIC_MESSAGE * (AudioSourceUtil.getMaxCaptureRateMilliHz() / 1000);
	
	private final DataBuffers data = new DataBuffers();
	private final CanvasVisualizerImpl visImpl = new CanvasVisualizerImpl();
	private int ticksSinceDataLastFound = 0;
	
	public CanvasVisualizerView(Context context) {
		super(context);
	}
	
	/**
	 * Should be called after the constructor. Sets up touch/click listeners, and notifies the
	 * provided {@code callOnInteraction} will be notified when the view is interacted with.
	 * 
	 * This is preferable to calling setOnTouch/setOnClick directly, as those will override any
	 * internally configured callbacks.
	 */
	public void setInteractionListeners(final Runnable callOnInteraction) {
        setOnClickListener(new View.OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			visImpl.mirror();
    			callOnInteraction.run();
    		}
		});
        setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					visImpl.mirror();
	    			callOnInteraction.run();
					return true;
				} else {
	    			callOnInteraction.run();
					return false;
				}
			}
		});
	}

	@Override
	public void onReceive(final byte[] fft) {
		if (!data.updateData(fft)) {
			++ticksSinceDataLastFound;
		} else {
			ticksSinceDataLastFound = 0;
		}
		invalidate();
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		visImpl.render(data, canvas);
		if (ticksSinceDataLastFound > TICKS_BEFORE_NO_MUSIC_MESSAGE) {
			renderNoAudioText(canvas);
		}
	}
	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		int viewLength = visImpl.onViewResize(w, h);
		data.updateViewScaling(viewLength);
	}
	
	private void renderNoAudioText(Canvas canvas) {
		// Blatant use of arbitrary constants that look nice...
		String header = getResources().getText(R.string.no_audio_header).toString();
		Paint p = new Paint();
		p.setColor(Color.WHITE);
		p.setTextAlign(Paint.Align.CENTER);
		p.setTextSize(Math.min(canvas.getHeight(), canvas.getWidth()) / 10);
		p.setShadowLayer(5, 0, 0, Color.BLACK);
		p.setSubpixelText(true);
		p.setAntiAlias(true);
		canvas.drawText(header, canvas.getWidth() / 2, canvas.getHeight() / 2, p);
		
		Rect headerBounds = new Rect();
		p.getTextBounds(header, 0, header.length(), headerBounds);
		
		p.setColor(Color.GRAY);
		p.setTextSize(p.getTextSize() / 3);
		String message = getResources().getText(R.string.no_audio_message).toString();
		canvas.drawText(message,
				canvas.getWidth() / 2, canvas.getHeight() / 2 + headerBounds.height(), p);
	}
}
