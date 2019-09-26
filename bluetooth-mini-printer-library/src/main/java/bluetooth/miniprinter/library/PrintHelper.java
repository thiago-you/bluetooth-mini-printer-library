package bluetooth.miniprinter.library;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

@SuppressWarnings("unused WeakerAccess")
public class PrintHelper {

    /**
     * Default bytes
     */
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte NL = 0x0A;

    /**
     * QR Code default size
     */
    private static final int QR_WIDTH = 385;
    private static final int QR_HEIGHT = 385;

    /**
     * Img default size
     */
    private static final int IMG_WIDTH = 385;
    private static final int IMG_PAPER_MODE = 0;

    /**
     * Default bytes pos
     */
    private static int[] p0 = new int[] { 0, 128 };
    private static int[] p1 = new int[] { 0, 64 };
    private static int[] p2 = new int[] { 0, 32 };
    private static int[] p3 = new int[] { 0, 16 };
    private static int[] p4 = new int[] { 0, 8 };
    private static int[] p5 = new int[] { 0, 4 };
    private static int[] p6 = new int[] { 0, 2 };

    /**
     * New lines byte to prevent some printer device issues
     */
    public static byte[] newLine = new byte[] { '\n', '\n' };

    /**
     * Printer device initialization
     */
    public static byte[] ESC_Init = new byte[] { ESC, '@' };

    /**
     * Print order
     */
    public static byte[] LF = new byte[] { NL };

    /**
     * Print and paper commands
     */
    private static byte[] ESC_J = new byte[] { ESC, 'J', 0x00 };
    private static byte[] GS_V_m_n = new byte[] { GS, 'V', 'B', 0x00 };

    /**
     * Set printer initialization config
     */
    public static byte[] setPrinterInit() {
        return PrintHelper.byteArraysToBytes(new byte[][] { PrintHelper.ESC_Init });
    }

    /**
     * Set print and feed config
     */
    public static byte[] setPrintAndFeed(int feed) {
        if (feed > 255 | feed < 0) {
            return null;
        }

        PrintHelper.ESC_J[2] = (byte) feed;
        return PrintHelper.byteArraysToBytes(new byte[][] { PrintHelper.ESC_J });
    }

    /**
     * Set paper cut config
     */
    public static byte[] setPaperCut(int cut) {
        if (cut > 255 | cut < 0) {
            return null;
        }

        PrintHelper.GS_V_m_n[3] = (byte) cut;
        return PrintHelper.byteArraysToBytes(new byte[][] { PrintHelper.GS_V_m_n });
    }

    /**
     * Prepare bytes to print
     */
    public static byte[] getPrintData(String data, String encoding) {
        byte[] dataByte = null;

        if (data == null || data.equals("") || data.length() < 1) {
            return null;
        }

        try {
            dataByte = data.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            Log.e(PrintHelper.class.getSimpleName(), e.getMessage(), e);
        }

        return dataByte;
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
        formatKThreshold(pixels, mBitmap.getWidth(), mBitmap.getHeight(), data);

        return data;
    }

    private static void formatKThreshold(int[] orgPixels, int xSize, int ySize, byte[] desPixels) {
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

    /**
     * Parse bytes array into bytes
     */
    private static byte[] byteArraysToBytes(byte[][] data) {
        int length = 0;

        for (byte[] datum : data) {
            length += datum.length;
        }

        byte[] send = new byte[length];
        int k = 0;

        for (byte[] datum : data) {
            for (byte b : datum) {
                send[k++] = b;
            }
        }

        return send;
    }

    /**
     * Write byte to print
     */
    public static void write(BluetoothService mBluetoothService, byte[] data) {
        if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            mBluetoothService.write(data);
        }
    }

    /**
     * Send text to print
     */
    public static void print(BluetoothService mBluetoothService, String msg, String encoding) {
        if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            mBluetoothService.write(PrintHelper.getPrintData(msg, encoding));
            mBluetoothService.write(PrintHelper.LF);
            mBluetoothService.write(PrintHelper.newLine);
        }
    }

    /**
     * Send text to print
     */
    public static void print(BluetoothService mBluetoothService, String msg) {
        if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            mBluetoothService.write(PrintHelper.getPrintData(msg, StandardCharsets.ISO_8859_1.name()));
            mBluetoothService.write(PrintHelper.LF);
            mBluetoothService.write(PrintHelper.newLine);
        }
    }

    /**
     * Send data to print
     */
    public static void print(BluetoothService mBluetoothService, byte[] data) {
        if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            mBluetoothService.write(PrintHelper.ESC_Init);
            mBluetoothService.write(PrintHelper.LF);
            mBluetoothService.write(data);
            mBluetoothService.write(PrintHelper.setPrintAndFeed(30));
            mBluetoothService.write(PrintHelper.setPaperCut(1));
            mBluetoothService.write(PrintHelper.setPrinterInit());
            mBluetoothService.write(PrintHelper.newLine);
        }
    }

    /**
     * Send test data to print
     */
    public static void printTest(BluetoothService mBluetoothService) {
        if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            String msg = "Testing Mini Thermal Printer " +
                    "\n\n abcdefghijklmnopqrstuvxyz " +
                    "\n ABCDEFGHIJKLMNOPQRSTUVXYZ " +
                    "\n 0123456789 " +
                    "\n UTF-8/ISO_8859_1 Charset: áçéãẽõôÁÇÃÉ";

            mBluetoothService.write(PrintHelper.getPrintData(msg, StandardCharsets.ISO_8859_1.name()));
            mBluetoothService.write(PrintHelper.LF);
            mBluetoothService.write(PrintHelper.newLine);
        }
    }

    /**
     * Parse text into qr code byte data
     */
    public static byte[] getQrCodeByte(String text, String charset, Integer qrWidth, Integer qrHeight) {
        byte[] data = new byte[] {};

        try {
            /* validate default values */
            if (charset == null || charset.length() == 0) {
                charset = StandardCharsets.ISO_8859_1.name();
            }
            if (qrWidth == null || qrWidth <= 0) {
                qrWidth = PrintHelper.QR_WIDTH;
            }
            if (qrHeight == null || qrHeight <= 0) {
                qrHeight = PrintHelper.QR_HEIGHT;
            }

            Hashtable<EncodeHintType, String> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, charset);
            BitMatrix bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, qrWidth, qrHeight, hints);

            int[] pixels = new int[qrWidth * qrHeight];
            for (int y = 0; y < qrHeight; y++) {
                for (int x = 0; x < qrWidth; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * qrWidth + x] = 0xff000000;
                    } else {
                        pixels[y * qrWidth + x] = 0xffffffff;
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(qrWidth, qrHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, qrWidth, 0, 0, qrWidth, qrHeight);

            data = PrintHelper.getBytesToPrint(bitmap);
        } catch (WriterException e) {
            Log.e(PrintHelper.class.getSimpleName(), e.getMessage(), e);
        }

        return data;
    }

    /**
     * Parse text into qr code byte data
     */
    public static byte[] getQrCodeByte(String text) {
        byte[] data = new byte[] {};

        try {
            Hashtable<EncodeHintType, String> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.ISO_8859_1.name());
            BitMatrix bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, PrintHelper.QR_WIDTH, PrintHelper.QR_HEIGHT, hints);

            int[] pixels = new int[PrintHelper.QR_WIDTH * PrintHelper.QR_HEIGHT];
            for (int y = 0; y < PrintHelper.QR_HEIGHT; y++) {
                for (int x = 0; x < PrintHelper.QR_WIDTH; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * PrintHelper.QR_WIDTH + x] = 0xff000000;
                    } else {
                        pixels[y * PrintHelper.QR_WIDTH + x] = 0xffffffff;
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(PrintHelper.QR_WIDTH, PrintHelper.QR_HEIGHT, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, PrintHelper.QR_WIDTH, 0, 0, PrintHelper.QR_WIDTH, PrintHelper.QR_HEIGHT);

            data = PrintHelper.getBytesToPrint(bitmap);
        } catch (WriterException e) {
            Log.e(PrintHelper.class.getSimpleName(), e.getMessage(), e);
        }

        return data;
    }

    /**
     * Print bitmap into byte data
     */
    public static byte[] getBytesToPrint(Bitmap mBitmap, Integer imgWidth, Integer imgMode) {
        /* validate default values */
        if (imgWidth == null || imgWidth <= 0) {
            imgWidth = PrintHelper.IMG_WIDTH;
        }
        if (imgMode == null || imgMode <= 0) {
            imgMode = PrintHelper.IMG_PAPER_MODE;
        }

        /* config bitmap size */
        int width = ((imgWidth + 7) / 8) * 8;
        int height = mBitmap.getHeight() * width / mBitmap.getWidth();
        height = ((height + 7) / 8) * 8;

        /* resize bitmap */
        Bitmap rszBitmap = mBitmap;
        if (mBitmap.getWidth() != width){
            rszBitmap = PrintHelper.resizeImage(mBitmap, width, height);
        }

        Bitmap grayBitmap = PrintHelper.toGrayScale(rszBitmap);

        byte[] dithered = PrintHelper.thresholdToBWPic(grayBitmap);

        return PrintHelper.eachLinePixToCmd(dithered, width, imgMode);
    }

    /**
     * Print bitmap into byte data
     */
    public static byte[] getBytesToPrint(Bitmap mBitmap) {
        /* config bitmap size */
        int width = ((PrintHelper.IMG_WIDTH + 7) / 8) * 8;
        int height = mBitmap.getHeight() * width / mBitmap.getWidth();
        height = ((height + 7) / 8) * 8;

        /* resize bitmap */
        Bitmap rszBitmap = mBitmap;
        if (mBitmap.getWidth() != width){
            rszBitmap = PrintHelper.resizeImage(mBitmap, width, height);
        }

        Bitmap grayBitmap = PrintHelper.toGrayScale(rszBitmap);

        byte[] dithered = PrintHelper.thresholdToBWPic(grayBitmap);

        return PrintHelper.eachLinePixToCmd(dithered, width, PrintHelper.IMG_PAPER_MODE);
    }
}
