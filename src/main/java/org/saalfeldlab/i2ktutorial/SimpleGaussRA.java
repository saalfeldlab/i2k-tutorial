/*
 *
 */

package org.saalfeldlab.i2ktutorial;

import java.util.concurrent.Executors;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.gauss3.SeparableSymmetricConvolution;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

/**
 * Simple Gaussian filter Op
 *
 * @author Stephan Saalfeld
 * @author Christian Dietz (University of Konstanz)
 * @param <T> type of input and output
 */
@Plugin(type = Ops.Filter.Gauss.class, priority = 0.5)
public class SimpleGaussRA<T extends NumericType<T> & NativeType<T>> extends
	AbstractUnaryComputerOp<RandomAccessible<T>, RandomAccessibleInterval<T>>
	implements Ops.Filter.Gauss {

	@Parameter
	final private double[] sigmas;

	public SimpleGaussRA(final double[] sigmas) {

		this.sigmas = sigmas;
	}

	@Override
	public void compute(
			final RandomAccessible<T> input,
			final RandomAccessibleInterval<T> output) {

		try {
			SeparableSymmetricConvolution.convolve(
					Gauss3.halfkernels(sigmas),
					input,
					output,
					Executors.newSingleThreadExecutor());
		} catch (final IncompatibleTypeException e) {
			throw new RuntimeException(e);
		}
	}
}
