package you.dev.bluetoothminiprinter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

import you.dev.bluetoothminiprinter.components.BluetoothService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int REQUEST_CONNECT_DEVICE = 101;

    /* encoding config */
    private static final String LATIN_CHARSET = "iso-8859-1";
    private static final int DEFAULT_COUNTRY_CODE = 55;

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
    private String mConnectedDeviceName;

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
                    SendDataByte(MainActivity.getPrintData(msg, LATIN_CHARSET));
                    SendDataByte(new byte[] { 0x0A });
                }else{
                    Toast.makeText(MainActivity.this, getString(R.string.empty), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.btnPrintQrCode:
                createImage();
                break;
            case R.id.btnSendImg:

                break;
            case R.id.btnTakePicture:

                break;
            case R.id.btnSelectImg:

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
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            /* disable btn and change text */
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

                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    /* save the connected device's name */
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_CONNECTION_LOST:
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
                case MESSAGE_UNABLE_CONNECT:
                    Toast.makeText(getApplicationContext(), "Unable to connect device", Toast.LENGTH_SHORT).show();
                    break;
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
    private void SendDataByte(byte[] data) {
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        } else {
            mBluetoothService.write(data);
        }
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
}
