package you.dev.bluetoothminiprinter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

import you.dev.bluetoothminiprinter.components.BluetoothService;
import you.dev.bluetoothminiprinter.components.PermissionHandler;
import you.dev.bluetoothminiprinter.components.PrintHelper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int REQUEST_CONNECT_DEVICE = 101;
    private static final int REQUEST_CAMERA = 102;
    private static final int REQUEST_SELECT_PICTURE = 103;

    /* message types sent from the BluetoothService Handler */
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    private static final int MESSAGE_CONNECTION_LOST = 6;
    private static final int MESSAGE_UNABLE_CONNECT = 7;

    /* key names received from the BluetoothService Handler */
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBluetoothService = null;
    private EditText edtPrintText;
    private Button btnSelectPrinter, btnSendPrint, btnTestPrinter, btnPrintQrCode, btnSendImg, btnTakePicture, btnSelectImg;
    private FloatingActionButton btnRemoveImg;
    private ImageView imgPrintable;
    private ProgressBar progressBar;
    private Bitmap imgBitmap;
    private Uri imageUri;

    private boolean isRequestingBluetooth;

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

        /* check and request permissions */
        PermissionHandler.requestDefaultPermissions(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        /* if Bluetooth is not on, request that it be enabled. */
        /* otherwise, setup the session */
        if (!mBluetoothAdapter.isEnabled() && !isRequestingBluetooth) {
            isRequestingBluetooth = true;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHandler.PERMISSION_REQUEST_CODE) {
            /* check if all permissions has been granted */
            PermissionHandler.requestDefaultPermissions(this);
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
        btnRemoveImg = findViewById(R.id.btnRemoveImg);
        progressBar = findViewById(R.id.progressBar);

        /* disable interface until select printer */
        /*edtPrintText.setEnabled(false);
        btnSendPrint.setEnabled(false);
        btnTestPrinter.setEnabled(false);
        btnPrintQrCode.setEnabled(false);
        btnSendImg.setEnabled(false);
        btnTakePicture.setEnabled(false);
        btnSelectImg.setEnabled(false);
        btnRemoveImg.setEnabled(false);*/

        /* implement self click listener */
        btnSelectPrinter.setOnClickListener(this);
        btnSendPrint.setOnClickListener(this);
        btnTestPrinter.setOnClickListener(this);
        btnPrintQrCode.setOnClickListener(this);
        btnSendImg.setOnClickListener(this);
        btnTakePicture.setOnClickListener(this);
        btnSelectImg.setOnClickListener(this);
        btnRemoveImg.setOnClickListener(this);

        /* hide remove button */
        btnRemoveImg.hide();

        /* start service */
        mBluetoothService = new BluetoothService(mHandler);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE: {
                /* when DeviceListActivity returns with a device to connect */
                if (resultCode == Activity.RESULT_OK) {
                    /* start loading animation */
                    progressBar.setVisibility(View.VISIBLE);

                    /* get the device address */
                    if (data.getExtras() != null) {
                        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

                        /* get the bluetooth device object */
                        if (BluetoothAdapter.checkBluetoothAddress(address)) {
                            /* attempt to connect to the device */
                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                            mBluetoothService.connect(device);
                        }
                    }
                }
                break;
            }
            case REQUEST_ENABLE_BLUETOOTH: {
                isRequestingBluetooth = false;

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
            case REQUEST_CAMERA: {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        btnRemoveImg.show();
                        imgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        imgPrintable.setImageBitmap(imgBitmap);
                    } catch (IOException e) {
                        Log.e(getClass().getSimpleName(), e.getMessage(), e);
                    }
                } else {
                    Toast.makeText(this, getText(R.string.no_pictures), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_SELECT_PICTURE: {
                if (resultCode == Activity.RESULT_OK){
                    if (data != null && data.getData() != null) {
                        try {
                            btnRemoveImg.show();
                            imgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                            imgPrintable.setImageBitmap(imgBitmap);
                        } catch (IOException e) {
                            Log.e(getClass().getSimpleName(), e.getMessage(), e);
                        }
                    }
                } else {
                    Toast.makeText(this, getText(R.string.no_pictures), Toast.LENGTH_SHORT).show();
                }
                break;
            }
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
                if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                    Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                } else {
                    PrintHelper.printTest(mBluetoothService);
                }
                break;
            }
            case R.id.btnSendPrint: {
                String msg = edtPrintText.getText().toString();

                if (msg.length() > 0) {
                    PrintHelper.print(mBluetoothService, msg);
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.empty), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.btnPrintQrCode: {
                if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                    Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                } else {
                    printQrCode();
                }
                break;
            }
            case R.id.btnSendImg: {
                if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                    Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                } else {
                    printImage();
                }
                break;
            }
            case R.id.btnTakePicture: {
                openCamera();
                break;
            }
            case R.id.btnSelectImg: {
                selectPicture();
                break;
            }
            case R.id.btnRemoveImg: {
                removeImg();
                break;
            }
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
                        /* finish loading animation */
                        progressBar.setVisibility(View.GONE);

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
                        btnRemoveImg.setEnabled(true);
                    }
                    break;
                }
                case MESSAGE_DEVICE_NAME: {
                    String deviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                    break;
                }
                case MESSAGE_TOAST: {
                    /* finish loading animation */
                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                }
                case MESSAGE_CONNECTION_LOST: {
                    /* finish loading animation */
                    progressBar.setVisibility(View.GONE);

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
                    /* finish loading animation */
                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(getApplicationContext(), "Unable to connect device", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    };

    /*
     * Create QR Code
     */
    private void printQrCode() {
        try {
            /* validate text */
            String text = edtPrintText.getText().toString();
            if (text.equals("") || text.length() < 1) {
                Toast.makeText(this, getText(R.string.empty), Toast.LENGTH_SHORT).show();
                return;
            }

            /* parse text into qr code bytes to print */
            byte[] data = PrintHelper.getQrCodeByte(text);

            PrintHelper.print(mBluetoothService, data);
            PrintHelper.print(mBluetoothService, PrintHelper.setPrintAndFeed(30));
            PrintHelper.print(mBluetoothService, PrintHelper.setPaperCut(1));
            PrintHelper.print(mBluetoothService, PrintHelper.setPrinterInit());
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /*
     * Send img to print
     */
    private void printImage() {
        if (imgPrintable.getDrawable() != null && imgBitmap != null) {
            byte[] data = PrintHelper.getBytesToPrint(imgBitmap);

            PrintHelper.print(mBluetoothService, PrintHelper.ESC_Init);
            PrintHelper.print(mBluetoothService, PrintHelper.LF);
            PrintHelper.print(mBluetoothService, data);
            PrintHelper.print(mBluetoothService, PrintHelper.setPrintAndFeed(30));
            PrintHelper.print(mBluetoothService, PrintHelper.setPaperCut(1));
            PrintHelper.print(mBluetoothService, PrintHelper.setPrinterInit());
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, getString(R.string.camera_intent_title));
        values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.camera_intent_description));
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void selectPicture() {
        Intent intent = new Intent();

        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        startActivityForResult(Intent.createChooser(intent, "Select picture..."), REQUEST_SELECT_PICTURE);
    }

    private void removeImg() {
        imageUri = null;
        imgBitmap = null;
        imgPrintable.setImageBitmap(null);
        btnRemoveImg.hide();
    }
}
