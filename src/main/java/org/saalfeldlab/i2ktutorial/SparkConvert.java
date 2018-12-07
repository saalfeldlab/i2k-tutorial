/**
 *
 */
package org.saalfeldlab.i2ktutorial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

/**
 * @author Stephan Saalfeld
 *
 */
public class SparkConvert {

	public static final void saveTIFFSeries(
			final JavaSparkContext sc,
			final String urlFormat,
			final String n5Path,
			final String datasetName,
			final long[] min,
			final long[] size,
			final int[] blockSize,
			final long firstSliceIndex) throws IOException {

		final N5Writer n5 = new N5FSWriter(n5Path);

        final int[] slicesDatasetBlockSize = new int[]{
        		blockSize[0] * 8,
        		blockSize[1] * 8,
        		1};
        n5.createDataset(
        		datasetName,
        		size,
        		slicesDatasetBlockSize,
        		DataType.UINT8,
        		new GzipCompression());
		final ArrayList<Long> slices = new ArrayList<>();
		for (long z = min[2]; z < min[2] + size[2]; ++z)
			slices.add(z);

		final JavaRDD<Long> rddSlices = sc.parallelize(slices);

		rddSlices.foreach(sliceIndex -> {

			final ImagePlus imp = IJ.openImage(String.format(urlFormat, sliceIndex + firstSliceIndex));
			if (imp == null)
				return;

			@SuppressWarnings({ "unchecked", "rawtypes" })
			final RandomAccessibleInterval<UnsignedByteType> slice =
					Views.offsetInterval(
							(RandomAccessibleInterval)ImagePlusImgs.from(imp),
							new long[]{
									min[0],
									min[1]},
							new long[]{
									size[0],
									size[1]});
			final N5Writer n5Local = new N5FSWriter(n5Path);
			N5Utils.saveBlock(
					Views.addDimension(slice, 0, 0),
					n5Local,
					datasetName,
					new long[]{0, 0, sliceIndex - min[2]});
		});
	}


	/**
	 * Copy an existing N5 dataset into another with a different blockSize.
	 *
	 * Parallelizes over blocks of [max(input, output)] to reduce redundant
	 * loading.  If blockSizes are integer multiples of each other, no
	 * redundant loading will happen.
	 *
	 * @param sc
	 * @param n5Path
	 * @param datasetName
	 * @param outDatasetName
	 * @param outBlockSize
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static final void reSave(
			final JavaSparkContext sc,
			final String n5Path,
			final String datasetName,
			final String outDatasetName,
			final int[] outBlockSize) throws IOException {

		final N5Writer n5 = new N5FSWriter(n5Path);

		final DatasetAttributes attributes = n5.getDatasetAttributes(datasetName);
		final int n = attributes.getNumDimensions();
		final int[] blockSize = attributes.getBlockSize();

		n5.createDataset(
				outDatasetName,
				attributes.getDimensions(),
				outBlockSize,
				attributes.getDataType(),
				attributes.getCompression());

		/* grid block size for parallelization to minimize double loading of blocks */
		final int[] gridBlockSize = new int[outBlockSize.length];
		Arrays.setAll(gridBlockSize, i -> Math.max(blockSize[i], outBlockSize[i]));

		final JavaRDD<long[][]> rdd =
				sc.parallelize(
						Grid.create(
								attributes.getDimensions(),
								gridBlockSize,
								outBlockSize));

		rdd.foreach(
				gridBlock -> {
					final N5Writer n5Writer = new N5FSWriter(n5Path);
					final RandomAccessibleInterval<?> source = N5Utils.open(n5Writer, datasetName);
					@SuppressWarnings("rawtypes")
					final RandomAccessibleInterval sourceGridBlock = Views.offsetInterval(source, gridBlock[0], gridBlock[1]);
					N5Utils.saveBlock(sourceGridBlock, n5Writer, outDatasetName, gridBlock[2]);
				});
	}

}
