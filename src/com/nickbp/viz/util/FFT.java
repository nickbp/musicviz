/* Copyright (c) 2007 - 2008 by Damien Di Fede <ddf@compartmental.net>
 * Copyright (C) 2013 Nicholas Parker
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Library General Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139,
 * USA.
 */

package com.nickbp.viz.util;

/** FFT stands for Fast Fourier Transform. It is an efficient way to calculate the Complex Discrete
 * Fourier Transform. There is not much to say about this class other than the fact that when you
 * want to analyze the spectrum of an audio buffer you will almost always use this class. One
 * restriction of this class is that the audio buffers you want to analyze must have a length that
 * is a power of two. If you try to construct an FFT with a <code>timeSize</code> that is not a
 * power of two, an IllegalArgumentException will be thrown.
 * 
 * @author Damien Di Fede
 * @see <a href="http://www.dspguide.com/ch12.htm">The Fast Fourier Transform</a>
 * @see <a href="http://www.dspguide.com/ch8.htm">The Discrete Fourier Transform</a> */
public class FFT {
	private final int timeSize;
	public final float[] real;
	public final float[] imag;

	// lookup tables
	private final float[] sinlookup;
	private final float[] coslookup;
	private final int[] reverse;
	
	/** Constructs an FFT that will accept sample buffers that are <code>timeSize</code> long and
	 * have been recorded with a sample rate of <code>sampleRate</code>. <code>timeSize</code>
	 * <em>must</em> be a power of two. This will throw an exception if it is not.
	 * 
	 * @param timeSize the length of the sample buffers you will be analyzing */
	public FFT(int timeSize) {
		this.timeSize = timeSize;
		real = new float[timeSize];
		imag = new float[timeSize];
		if ((timeSize & (timeSize - 1)) != 0)
			throw new IllegalArgumentException("FFT: timeSize must be a power of two.");
		
		// set up the bit reversing table
		reverse = new int[timeSize];
		reverse[0] = 0;
		for (int limit = 1, bit = timeSize / 2; limit < timeSize; limit <<= 1, bit >>= 1)
			for (int i = 0; i < limit; i++)
				reverse[i + limit] = reverse[i] + bit;
		
		// build trig tables
		sinlookup = new float[timeSize];
		coslookup = new float[timeSize];
		for (int i = 0; i < timeSize; i++) {
			sinlookup[i] = (float)Math.sin(-(float)Math.PI / i);
			coslookup[i] = (float)Math.cos(-(float)Math.PI / i);
		}
	}

	  // performs an in-place fft on the data in the real and imag arrays
	  // bit reversing is not necessary as the data will already be bit reversed
	  private void fft()
	  {
	    for (int halfSize = 1; halfSize < real.length; halfSize *= 2)
	    {
	      // float k = -(float)Math.PI/halfSize;
	      // phase shift step
	      // float phaseShiftStepR = (float)Math.cos(k);
	      // float phaseShiftStepI = (float)Math.sin(k);
	      // using lookup table
	      float phaseShiftStepR = coslookup[halfSize];
	      float phaseShiftStepI = sinlookup[halfSize];
	      // current phase shift
	      float currentPhaseShiftR = 1.0f;
	      float currentPhaseShiftI = 0.0f;
	      for (int fftStep = 0; fftStep < halfSize; fftStep++)
	      {
	        for (int i = fftStep; i < real.length; i += 2 * halfSize)
	        {
	          int off = i + halfSize;
	          float tr = (currentPhaseShiftR * real[off]) - (currentPhaseShiftI * imag[off]);
	          float ti = (currentPhaseShiftR * imag[off]) + (currentPhaseShiftI * real[off]);
	          real[off] = real[i] - tr;
	          imag[off] = imag[i] - ti;
	          real[i] += tr;
	          imag[i] += ti;
	        }
	        float tmpR = currentPhaseShiftR;
	        currentPhaseShiftR = (tmpR * phaseShiftStepR) - (currentPhaseShiftI * phaseShiftStepI);
	        currentPhaseShiftI = (tmpR * phaseShiftStepI) + (currentPhaseShiftI * phaseShiftStepR);
	      }
	    }
	  }


	/** Performs a forward transform on <code>buffer</code>.
	 * 
	 * @param buffer the buffer to analyze */
	public void forward(short[] buffer) {
		if (buffer.length != timeSize) {
			throw new IllegalArgumentException("FFT.forward: The length of the passed sample " +
					"buffer must be equal to timeSize().");
		}
		// copy samples to real/imag in bit-reversed order
		for (int i = 0; i < buffer.length; i++) {
			real[i] = (byte)(buffer[reverse[i]] / 128);
			imag[i] = 0;
		}
		// perform the fft
		fft();
	}
}