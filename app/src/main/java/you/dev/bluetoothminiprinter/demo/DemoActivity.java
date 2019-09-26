package you.dev.bluetoothminiprinter.demo;

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

import you.dev.bluetoothminiprinter.R;
import you.dev.bluetoothminiprinter.components.BluetoothService;
import you.dev.bluetoothminiprinter.components.PermissionHandler;
import you.dev.bluetoothminiprinter.components.PrintHelper;

public class DemoActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int REQUEST_CONNECT_DEVICE = 101;
    private static final int REQUEST_CAMERA = 102;
    private static final int REQUEST_SELECT_PICTURE = 103;

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
        setContentView(R.layout.activity_demo);

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
        edtPrintText.setEnabled(false);
        btnSendPrint.setEnabled(false);
        btnTestPrinter.setEnabled(false);
        btnPrintQrCode.setEnabled(false);
        btnSendImg.setEnabled(false);
        btnTakePicture.setEnabled(false);
        btnSelectImg.setEnabled(false);
        btnRemoveImg.setEnabled(false);

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
        super.onActivityResult(requestCode, resultCode, data);

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
                }
                break;
            }
            case REQUEST_SELECT_PICTURE: {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        try {
                            btnRemoveImg.show();
                            imgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                            imgPrintable.setImageBitmap(imgBitmap);
                        } catch (IOException e) {
                            Log.e(getClass().getSimpleName(), e.getMessage(), e);
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        /* can be refactored to use switch statement in application */
        if (id == R.id.btnSelectPrinter) {
            Intent intent = new Intent(DemoActivity.this, DeviceListActivity.class);
            startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
        } else if (id == R.id.btnTestPrinter) {
            if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            } else {
                PrintHelper.printTest(mBluetoothService);
            }
        } else if (id == R.id.btnSendPrint) {
            String msg = edtPrintText.getText().toString();

            if (msg.length() > 0) {
                PrintHelper.print(mBluetoothService, msg);
            } else {
                Toast.makeText(DemoActivity.this, getString(R.string.empty), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.btnPrintQrCode) {
            if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            } else {
                printQrCode();
            }
        } else if (id == R.id.btnSendImg) {
            if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
                Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            } else {
                printImage();
            }
        } else if (id == R.id.btnTakePicture) {
            openCamera();
        } else if (id == R.id.btnSelectImg) {
            selectPicture();
        } else if (id == R.id.btnRemoveImg) {
            removeImg();
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MSG_STATE_CHANGE: {
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
                case BluetoothService.MSG_DEVICE_NAME: {
                    String deviceName = msg.getData().getString(BluetoothService.KEY_DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                    break;
                }
                case BluetoothService.MSG_TOAST: {
                    /* finish loading animation */
                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.KEY_TOAST_MSG), Toast.LENGTH_SHORT).show();
                    break;
                }
                case BluetoothService.MSG_CONNECTION_LOST: {
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
                case BluetoothService.MSG_UNABLE_CONNECT: {
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

            /* write data with default bytes */
            PrintHelper.print(mBluetoothService, data);

            /* manual write all bytes */
            //PrintHelper.write(mBluetoothService, PrintHelper.ESC_Init);
            //PrintHelper.write(mBluetoothService, PrintHelper.LF);
            //PrintHelper.write(mBluetoothService, data);
            //PrintHelper.write(mBluetoothService, PrintHelper.setPrintAndFeed(30));
            //PrintHelper.write(mBluetoothService, PrintHelper.setPaperCut(1));
            //PrintHelper.write(mBluetoothService, PrintHelper.setPrinterInit());
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

            /* write data with default bytes */
            PrintHelper.print(mBluetoothService, data);

            /* manual write all bytes */
            //PrintHelper.write(mBluetoothService, PrintHelper.ESC_Init);
            //PrintHelper.write(mBluetoothService, PrintHelper.LF);
            //PrintHelper.write(mBluetoothService, data);
            //PrintHelper.write(mBluetoothService, PrintHelper.setPrintAndFeed(30));
            //PrintHelper.write(mBluetoothService, PrintHelper.setPaperCut(1));
            //PrintHelper.write(mBluetoothService, PrintHelper.setPrinterInit());
        } else {
            Toast.makeText(this, R.string.no_img_printable, Toast.LENGTH_SHORT).show();
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
