package you.dev.bluetoothminiprinter.components;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

/**
 * Handle bitmap conversion to byte[]
 */
public class PrintPicture {

	private static int[] p0 = new int[]{0, 128};
	private static int[] p1 = new int[]{0, 64};
	private static int[] p2 = new int[]{0, 32};
	private static int[] p3 = new int[]{0, 16};
	private static int[] p4 = new int[]{0, 8};
	private static int[] p5 = new int[]{0, 4};
	private static int[] p6 = new int[]{0, 2};

	/**
	 * Print bitmap into byte data
	 */
    public static byte[] POS_PrintBMP(Bitmap mBitmap, int nWidth, int nMode) {
		/* config bitmap size */
		int width = ((nWidth + 7) / 8) * 8;
		int height = mBitmap.getHeight() * width / mBitmap.getWidth();
		height = ((height + 7) / 8) * 8;

		/* resize bitmap */
		Bitmap rszBitmap = mBitmap;
		if (mBitmap.getWidth() != width){
			rszBitmap = PrintPicture.resizeImage(mBitmap, width, height);
		}
	
		Bitmap grayBitmap = PrintPicture.toGrayScale(rszBitmap);
		
		byte[] dithered = PrintPicture.thresholdToBWPic(grayBitmap);

		return PrintPicture.eachLinePixToCmd(dithered, width, nMode);
	}

	private static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
		/* get and set size scale */
    	int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float scaleWidth = (float) w / (float) width;
		float scaleHeight = (float) h / (float) height;

		/* config scale */
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		/* return resized bitmap */
		return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	}

	private static Bitmap toGrayScale(Bitmap bmpOriginal) {
		/* get bitmap size */
    	int height = bmpOriginal.getHeight();
		int width = bmpOriginal.getWidth();

		/* config grey scale */
		Bitmap bmpGrayScale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bmpGrayScale);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0.0F);
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		c.drawBitmap(bmpOriginal, 0.0F, 0.0F, paint);

		/* return scale */
		return bmpGrayScale;
	}

	private static byte[] thresholdToBWPic(Bitmap mBitmap) {
		int[] pixels = new int[mBitmap.getWidth() * mBitmap.getHeight()];
		byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight()];

		mBitmap.getPixels(pixels, 0, mBitmap.getWidth(), 0, 0, mBitmap.getWidth(), mBitmap.getHeight());
		format_K_threshold(pixels, mBitmap.getWidth(), mBitmap.getHeight(), data);

		return data;
	}

	private static void format_K_threshold(int[] orgPixels, int xSize, int ySize, byte[] desPixels) {
		int grayTotal = 0;
		int k = 0;
		int i, j, gray;

		for (i = 0; i < ySize; ++i) {
			for (j = 0; j < xSize; ++j) {
				gray = orgPixels[k] & 255;
				grayTotal += gray;
				++k;
			}
		}

		int grayave = grayTotal / ySize / xSize;
		k = 0;

		for (i = 0; i < ySize; ++i) {
			for (j = 0; j < xSize; ++j) {
				gray = orgPixels[k] & 255;

				if (gray > grayave) {
					desPixels[k] = 0;
				} else {
					desPixels[k] = 1;
				}

				++k;
			}
		}
	}

	private static byte[] eachLinePixToCmd(byte[] src, int nWidth, int nMode) {
		int nHeight = src.length / nWidth;
		int nBytesPerLine = nWidth / 8;
		byte[] data = new byte[nHeight * (8 + nBytesPerLine)];
		int k = 0;

		for (int i = 0; i < nHeight; ++i) {
			int offset = i * (8 + nBytesPerLine);
			data[offset] = 29;
			data[offset + 1] = 118;
			data[offset + 2] = 48;
			data[offset + 3] = (byte) (nMode & 1);
			data[offset + 4] = (byte) (nBytesPerLine % 256);
			data[offset + 5] = (byte) (nBytesPerLine / 256);
			data[offset + 6] = 1;
			data[offset + 7] = 0;

			for (int j = 0; j < nBytesPerLine; ++j) {
				data[offset + 8 + j] = (byte) (p0[src[k]] + p1[src[k + 1]] + p2[src[k + 2]] + p3[src[k + 3]] + p4[src[k + 4]] + p5[src[k + 5]] + p6[src[k + 6]] + src[k + 7]);
				k += 8;
			}
		}

		return data;
	}
}
