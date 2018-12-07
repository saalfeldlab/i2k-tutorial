/**
 *
 */
package org.saalfeldlab.i2ktutorial;

import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.googlecloud.GoogleCloudClientSecretsCmdLinePrompt;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudClientSecretsPrompt;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudOAuth;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageClient;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import com.google.auth.Credentials;
import com.google.cloud.storage.Storage;

import bdv.util.BdvFunctions;
import bdv.util.volatiles.VolatileViews;
import ij.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.view.Views;

/**
 * N5 ImgLib2-Cache tutorial
 *
 * @author Stephan Saalfeld
 *
 */
public class I2KTutorial {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		new ImageJ();

		// First, let's open an N5 container
		N5FSReader n5 = new N5FSReader(
				"/home/saalfeld/projects/lauritzen/02/workspace.n5");
		System.out.println(Arrays.toString(n5.list("/")));

		// Open a volume that is too large for memory
		final RandomAccessibleInterval<UnsignedByteType> img =
				N5Utils.open(n5, "volumes/raw/s0");

		// Show it in ImageJ
		ImageJFunctions.show(img, "img in IJ");

		// Show it with BigDataViewer
		BdvFunctions.show(img, "img");

		// Stucks---doesn't it?  This is because chunks are loaded on demand and block pixel access.
		// Instead, use a type that responds regardless of whether it has been loaded or not, and
		// also knows about this fact.

		// Open with volatile access
		final RandomAccessibleInterval<VolatileUnsignedByteType> volatileImg =
				N5Utils.openVolatile(n5, "volumes/raw/s0");

		// Show it with BigDataViewer
		BdvFunctions.show(VolatileViews.wrapAsVolatile(volatileImg), "volatile img");

		// Use the cached volume as input for a filter whose output will again be cached
		final SimpleGaussRA<FloatType> gauss = new SimpleGaussRA<>(new double[] {5, 5, 0});
		final RandomAccessibleInterval<FloatType> filtered = Lazy.processFloat(
				Converters.convert(
						Views.extendMirrorSingle(img),
						new RealFloatConverter<>(),
						new FloatType()),
				img,
				new int[] {64, 64, 8},
				gauss);

		// You will have to adjust the contrast to 0--255 to see the output
		BdvFunctions.show(filtered, "filtered");


		//Now do the same thing on data coming from Google cloud
		final GoogleCloudClientSecretsPrompt clientSecretsPrompt = new GoogleCloudClientSecretsCmdLinePrompt();
		final GoogleCloudOAuth oauth = new GoogleCloudOAuth(clientSecretsPrompt);
		final Credentials credentials = oauth.getCredentials();
		final GoogleCloudStorageClient storageClient = new GoogleCloudStorageClient(credentials);
		final Storage storage = storageClient.create();

		N5GoogleCloudStorageReader googleN5 = new N5GoogleCloudStorageReader(storage, "lauritzen-02-n5");
//	System.out.println(Arrays.toString(googleN5.list("/")));

		//Open a volume that is too large for memory
		final RandomAccessibleInterval<UnsignedByteType> googleImg =
				N5Utils.openVolatile(n5, "volumes/raw/s3");

		BdvFunctions.show(VolatileViews.wrapAsVolatile(googleImg), "Google Img");

	}
}
