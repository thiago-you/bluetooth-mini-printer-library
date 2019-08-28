package you.dev.bluetoothminiprinter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

import you.dev.bluetoothminiprinter.components.BluetoothService;
import you.dev.bluetoothminiprinter.components.Command;
import you.dev.bluetoothminiprinter.components.PrintPicture;
import you.dev.bluetoothminiprinter.components.PrinterCommand;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int REQUEST_CONNECT_DEVICE = 101;

    /* encoding config */
    private static final String LATIN_CHARSET = "iso-8859-1";

    /* QR Code size */
    private static final int QR_WIDTH = 350;
    private static final int QR_HEIGHT = 350;

    /* message types sent from the BluetoothService Handler */
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_CONNECTION_LOST = 6;
    public static final int MESSAGE_UNABLE_CONNECT = 7;

    /* key names received from the BluetoothService Handler */
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBluetoothService = null;
    private EditText edtPrintText;
    private Button btnSelectPrinter, btnSendPrint, btnTestPrinter, btnPrintQrCode, btnSendImg, btnTakePicture, btnSelectImg;
    private ImageView imgPrintable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        /* if the adapter is null, then Bluetooth is not supported */
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available on your device", Toast.LENGTH_LONG).show();
            finish();
        }

        initUI();
    }

    @Override
    protected void onStart() {
        super.onStart();

        /* if Bluetooth is not on, request that it be enabled. */
        /* otherwise, setup the session */
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            if (mBluetoothService == null) {
                initUI();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                mBluetoothService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /* stop the Bluetooth services */
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    /**
     * Initialize UI
     */
    private void initUI() {
        /* init interface views */
        btnSelectPrinter = findViewById(R.id.btnSelectPrinter);
        edtPrintText = findViewById(R.id.edtPrintText);
        btnSendPrint = findViewById(R.id.btnSendPrint);
        btnTestPrinter = findViewById(R.id.btnTestPrinter);
        btnPrintQrCode = findViewById(R.id.btnPrintQrCode);
        btnSendImg = findViewById(R.id.btnSendImg);
        btnTakePicture = findViewById(R.id.btnTakePicture);
        btnSelectImg = findViewById(R.id.btnSelectImg);
        imgPrintable = findViewById(R.id.imgPrintable);

        /* disable interface until select printer */
        edtPrintText.setEnabled(false);
        btnSendPrint.setEnabled(false);
        btnTestPrinter.setEnabled(false);
        btnPrintQrCode.setEnabled(false);
        btnSendImg.setEnabled(false);
        btnTakePicture.setEnabled(false);
        btnSelectImg.setEnabled(false);

        /* implement self click listener */
        btnSelectPrinter.setOnClickListener(this);
        btnSendPrint.setOnClickListener(this);
        btnTestPrinter.setOnClickListener(this);
        btnPrintQrCode.setOnClickListener(this);
        btnSendImg.setOnClickListener(this);
        btnTakePicture.setOnClickListener(this);
        btnSelectImg.setOnClickListener(this);

        /* start service */
        mBluetoothService = new BluetoothService(this, mHandler);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                /* when DeviceListActivity returns with a device to connect */
                if (resultCode == Activity.RESULT_OK) {
                    /* get the device address */
                    if (data.getExtras() != null) {
                        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

                        /* get the bluetooth device object */
                        if (BluetoothAdapter.checkBluetoothAddress(address)) {
                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

                            // attempt to connect to the device
                            mBluetoothService.connect(device);
                        }
                    }
                }
                break;
            case REQUEST_ENABLE_BLUETOOTH:
                /* when the request to enable Bluetooth returns */
                if (resultCode == Activity.RESULT_OK) {
                    /* bluetooth is now enabled, so set up a session */
                    initUI();
                } else {
                    /* user did not enable Bluetooth or an error occurred */
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSelectPrinter: {
                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                break;
            }
            case R.id.btnTestPrinter: {
                printTest();
                break;
            }
            case R.id.btnSendPrint: {
                String msg = edtPrintText.getText().toString();
                if (msg.length()>0) {
                    sendDataByte(PrinterCommand.getPrintData(msg, LATIN_CHARSET));
                    sendDataByte(Command.LF);
                }else{
                    Toast.makeText(MainActivity.this, getString(R.string.empty), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.btnPrintQrCode:
                printQrCode();
                break;
            case R.id.btnSendImg:
                printImage();
                break;
            case R.id.btnTakePicture:
                Log.e(getClass().getSimpleName(), "Take Picture Not implemented Yet");
                break;
            case R.id.btnSelectImg:
                Log.e(getClass().getSimpleName(), "Select Image Not implemented Yet");
                break;
            default:
                break;
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE: {
                    /* disable btn and change text */
                    if (msg.arg1 == BluetoothService.STATE_CONNECTED) {
                        btnSelectPrinter.setText(getText(R.string.btn_is_connected));
                        btnSelectPrinter.setEnabled(false);

                        /* enable interface */
                        edtPrintText.setEnabled(true);
                        btnSendPrint.setEnabled(true);
                        btnTestPrinter.setEnabled(true);
                        btnPrintQrCode.setEnabled(true);
                        btnSendImg.setEnabled(true);
                        btnTakePicture.setEnabled(true);
                        btnSelectImg.setEnabled(true);
                    }
                    break;
                }
                case MESSAGE_DEVICE_NAME: {
                    String deviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                    break;
                }
                case MESSAGE_TOAST: {
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                }
                case MESSAGE_CONNECTION_LOST: {
                    Toast.makeText(getApplicationContext(), "Device connection was lost", Toast.LENGTH_SHORT).show();

                    /* disable interface until select printer again */
                    edtPrintText.setEnabled(false);
                    btnSendPrint.setEnabled(false);
                    btnTestPrinter.setEnabled(false);
                    btnPrintQrCode.setEnabled(false);
                    btnSendImg.setEnabled(false);
                    btnTakePicture.setEnabled(false);
                    btnSelectImg.setEnabled(false);
                    break;
                }
                case MESSAGE_UNABLE_CONNECT: {
                    Toast.makeText(getApplicationContext(), "Unable to connect device", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    };

    /**
     * Print test string
     */
    private void printTest() {
        String msg = "Testing Mini Thermal Printer \n\n abcdefghijklmnopqrstuvxyz \n ABCDEFGHIJKLMNOPQRSTUVXYZ \n UTF-8 Charset: áçéãẽõôÁÇÃÉ";
        sendDataString(msg);
    }

    /**
     * Send data as String
     */
    private void sendDataString(String data) {
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        } else {
            if (data.length() > 0) {
                mBluetoothService.write(data.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Send data as byte
     */
    private void sendDataByte(byte[] data) {
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        } else {
            mBluetoothService.write(data);
        }
    }

    /*
     * Create QR Code
     */
    private void printQrCode() {
        try {
            String text = edtPrintText.getText().toString();

            if (text.equals("") || text.length() < 1) {
                Toast.makeText(this, getText(R.string.empty), Toast.LENGTH_SHORT).show();
                return;
            }

            Hashtable<EncodeHintType, String> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, LATIN_CHARSET);
            BitMatrix bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);

            int[] pixels = new int[QR_WIDTH * QR_HEIGHT];
            for (int y = 0; y < QR_HEIGHT; y++) {
                for (int x = 0; x < QR_WIDTH; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * QR_WIDTH + x] = 0xff000000;
                    } else {
                        pixels[y * QR_WIDTH + x] = 0xffffffff;
                    }

                }
            }

            Bitmap bitmap = Bitmap.createBitmap(QR_WIDTH, QR_HEIGHT, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, QR_WIDTH, 0, 0, QR_WIDTH, QR_HEIGHT);
            byte[] data = PrintPicture.POS_PrintBMP(bitmap, 384, 0);

            sendDataByte(data);
            sendDataByte(PrinterCommand.POS_Set_PrtAndFeedPaper(30));
            sendDataByte(PrinterCommand.POS_Set_Cut(1));
            sendDataByte(PrinterCommand.POS_Set_PrtInit());
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /*
     * Send img to print
     */
    private void printImage() {
        Bitmap mBitmap = ((BitmapDrawable) imgPrintable.getDrawable()).getBitmap();

        int nMode = 0;
        int nPaperWidth = 384;

        if (mBitmap != null) {
            byte[] data = PrintPicture.POS_PrintBMP(mBitmap, nPaperWidth, nMode);

            sendDataByte(Command.ESC_Init);
            sendDataByte(Command.LF);
            sendDataByte(data);
            sendDataByte(PrinterCommand.POS_Set_PrtAndFeedPaper(30));
            sendDataByte(PrinterCommand.POS_Set_Cut(1));
            sendDataByte(PrinterCommand.POS_Set_PrtInit());
        }
    }
}
