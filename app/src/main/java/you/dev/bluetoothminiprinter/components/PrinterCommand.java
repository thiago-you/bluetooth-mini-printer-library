package you.dev.bluetoothminiprinter.components;

import android.util.Log;

import java.io.UnsupportedEncodingException;

import you.dev.bluetoothminiprinter.MainActivity;

public class PrinterCommand {

    /**
     * Set printer initialization config
     */
    public static byte[] setPrinterInit() {
        return PrinterCommand.byteArraysToBytes(new byte[][] { Command.ESC_Init });
    }

    /**
     * Set print and feed config
     */
    public static byte[] setPrintAndFeed(int feed) {
        if (feed > 255 | feed < 0) {
            return null;
        }

        Command.ESC_J[2] = (byte) feed;
        return PrinterCommand.byteArraysToBytes(new byte[][] { Command.ESC_J });
    }

    /**
     * Set paper cut config
     */
    public static byte[] setPaperCut(int cut) {
        if (cut > 255 | cut < 0) {
            return null;
        }

        Command.GS_V_m_n[3] = (byte) cut;
        return PrinterCommand.byteArraysToBytes(new byte[][] { Command.GS_V_m_n });
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
            Log.e(MainActivity.class.getSimpleName(), e.getMessage(), e);
        }

        return dataByte;
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
}
