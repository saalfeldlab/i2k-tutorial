/**
 *
 */
package org.saalfeldlab.i2ktutorial;

import static net.imglib2.img.basictypeaccess.AccessFlags.VOLATILE;
import static net.imglib2.type.PrimitiveType.BYTE;
import static net.imglib2.type.PrimitiveType.FLOAT;

import java.util.Set;

import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.Cache;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

/**
 * Some static helper methods for lazy processing of cached cell images.
 *
 * @author Stephan Saalfeld
 */
public class Lazy {

	private Lazy() {}

	/**
	 * Process an {@link UnsignedByteType} input to an
	 * {@link UnsignedByteType} output using a cell loader.
	 * This is the base method that all other variants refer to
	 * @param source
	 * @param sourceInterval
	 * @param blockSize
	 * @param loader
	 * @return
	 */
	public static <T extends NativeType<T>, A extends ArrayDataAccess<A>> RandomAccessibleInterval<T> process(
			final RandomAccessible<T> source,
			final Interval sourceInterval,
			final int[] blockSize,
			final UnaryComputerOpCellLoader<T, T, RandomAccessible<T>> loader,
			final T type,
			final Class<A> access,
			final PrimitiveType P,
			final Set<AccessFlags> accessFlags) {

		final long[] dimensions = Intervals.dimensionsAsLongArray(sourceInterval);
		final CellGrid grid = new CellGrid(dimensions, blockSize);

		final Cache<Long, Cell<A>> cache = new SoftRefLoaderCache<Long, Cell<A>>()
				.withLoader(LoadedCellCacheLoader.get(grid, loader, type, AccessFlags.setOf()));
		final CachedCellImg<T, A> img = new CachedCellImg<T, A>(grid, type, cache, ArrayDataAccessFactory.get(P, accessFlags));
		return img;
	}

	public static RandomAccessibleInterval<UnsignedByteType> processUnsignedByte(
			final RandomAccessible<UnsignedByteType> source,
			final Interval sourceInterval,
			final int[] blockSize,
			final UnaryComputerOp<RandomAccessible<UnsignedByteType>, RandomAccessibleInterval<UnsignedByteType>> op) {

		final UnaryComputerOpCellLoader<UnsignedByteType, UnsignedByteType, RandomAccessible<UnsignedByteType>> loader =
				new UnaryComputerOpCellLoader<UnsignedByteType, UnsignedByteType, RandomAccessible<UnsignedByteType>>(
					source,
					op);

		return process(source, sourceInterval, blockSize, loader, new UnsignedByteType(), ByteArray.class, BYTE, AccessFlags.setOf());
	}

	public static <O extends Op> RandomAccessibleInterval<UnsignedByteType> processVolatileUnsignedByte(
			final RandomAccessibleInterval<UnsignedByteType> source,
			final int[] blockSize,
			final OpService opService,
			final Class<O> opClass,
			final Object... opArgs) {

		final UnaryComputerOpCellLoader<UnsignedByteType, UnsignedByteType, RandomAccessible<UnsignedByteType>> loader =
				new UnaryComputerOpCellLoader<UnsignedByteType, UnsignedByteType, RandomAccessible<UnsignedByteType>>(
						source,
						UnsignedByteType::new,
						opService,
						opClass,
						opArgs);

		return process(source, source, blockSize, loader, new UnsignedByteType(), ByteArray.class, BYTE, AccessFlags.setOf());
	}

	public static <O extends Op> RandomAccessibleInterval<UnsignedByteType> processUnsignedByte(
			final RandomAccessibleInterval<UnsignedByteType> source,
			final int[] blockSize,
			final OpService opService,
			final Class<O> opClass,
			final Object... opArgs) {

		final UnaryComputerOpCellLoader<UnsignedByteType, UnsignedByteType, RandomAccessible<UnsignedByteType>> loader =
				new UnaryComputerOpCellLoader<UnsignedByteType, UnsignedByteType, RandomAccessible<UnsignedByteType>>(
						source,
						UnsignedByteType::new,
						opService,
						opClass,
						opArgs);

		return process(source, source, blockSize, loader, new UnsignedByteType(), ByteArray.class, BYTE, AccessFlags.setOf());
	}

	public static <O extends Op> RandomAccessibleInterval<FloatType> processVolatileFloat(
			final RandomAccessibleInterval<FloatType> source,
			final int[] blockSize,
			final OpService opService,
			final Class<O> opClass,
			final Object... opArgs) {

		final UnaryComputerOpCellLoader<FloatType, FloatType, RandomAccessible<FloatType>> loader =
				new UnaryComputerOpCellLoader<FloatType, FloatType, RandomAccessible<FloatType>>(
					source,
					FloatType::new,
					opService,
					opClass,
					opArgs);

		return process(source, source, blockSize, loader, new FloatType(), VolatileFloatArray.class, FLOAT, AccessFlags.setOf(VOLATILE));
	}

	public static <O extends Op> RandomAccessibleInterval<FloatType> processFloat(
			final RandomAccessible<FloatType> source,
			final Interval sourceInterval,
			final int[] blockSize,
			final OpService opService,
			final Class<O> opClass,
			final Object... opArgs) {

		final UnaryComputerOpCellLoader<FloatType, FloatType, RandomAccessible<FloatType>> loader =
				new UnaryComputerOpCellLoader<FloatType, FloatType, RandomAccessible<FloatType>>(
					source,
					FloatType::new,
					opService,
					opClass,
					opArgs);

		return process(source, sourceInterval, blockSize, loader, new FloatType(), FloatArray.class, FLOAT, AccessFlags.setOf());
	}

	public static RandomAccessibleInterval<FloatType> processFloat(
			final RandomAccessible<FloatType> source,
			final Interval sourceInterval,
			final int[] blockSize,
			final UnaryComputerOp<RandomAccessible<FloatType>, RandomAccessibleInterval<FloatType>> op) {

		final UnaryComputerOpCellLoader<FloatType, FloatType, RandomAccessible<FloatType>> loader =
				new UnaryComputerOpCellLoader<FloatType, FloatType, RandomAccessible<FloatType>>(
					source,
					op);

		return process(source, sourceInterval, blockSize, loader, new FloatType(), FloatArray.class, FLOAT, AccessFlags.setOf());
	}

	public static RandomAccessibleInterval<FloatType> processFloatVolatile(
			final RandomAccessible<FloatType> source,
			final Interval sourceInterval,
			final int[] blockSize,
			final UnaryComputerOp<RandomAccessible<FloatType>, RandomAccessibleInterval<FloatType>> op) {

		final UnaryComputerOpCellLoader<FloatType, FloatType, RandomAccessible<FloatType>> loader =
				new UnaryComputerOpCellLoader<FloatType, FloatType, RandomAccessible<FloatType>>(
					source,
					op);

		return process(source, sourceInterval, blockSize, loader, new FloatType(), VolatileFloatArray.class, FLOAT, AccessFlags.setOf(VOLATILE));
	}
}
