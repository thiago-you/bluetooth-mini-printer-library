package bluetooth.miniprinter.library;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices
 */
@SuppressWarnings("unused WeakerAccess")
public class BluetoothService {
    /**
     * Name for the SDP record when creating server socket
     */
    private static final String NAME = "MiniThermalPrinter";

    /**
     * Unique UUID for this application
     */
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Current connection state
     */
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Message code
     */
    public static final int MSG_STATE_CHANGE = 1;
    public static final int MSG_READ = 2;
    public static final int MSG_WRITE = 3;
    public static final int MSG_DEVICE_NAME = 4;
    public static final int MSG_TOAST = 5;
    public static final int MSG_CONNECTION_LOST = 6;
    public static final int MSG_UNABLE_CONNECT = 7;

    /**
     * Key names to send back messages to activity
     */
    public static final String KEY_DEVICE_NAME = "device_name";
    public static final String KEY_TOAST_MSG = "toast_msg";

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    
    /**
     * Constructor. Prepares a new BTPrinter session
     *
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothService(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the connection
     *
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        mState = state;

        /* give the new state to the Handler so the UI Activity can update */
        mHandler.obtainMessage(BluetoothService.MSG_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        /* cancel any thread attempting to make a connection */
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        /* cancel any thread currently running a connection */
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        /* start the thread to listen on a BluetoothServerSocket */
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }

        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device
     *
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        /* cancel any thread attempting to make a connection */
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        /* cancel any thread currently running a connection */
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        /* start the thread to connect with the given device */
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        /* cancel the thread that completed the connection */
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        /* cancel any thread currently running a connection */
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        /* cancel the accept thread because we only want to connect to one device */
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        /* start the thread to manage the connection and perform transmissions */
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        /* send the name of the connected device back to the UI Activity */
        Message msg = mHandler.obtainMessage(BluetoothService.MSG_DEVICE_NAME);

        Bundle bundle = new Bundle();
        bundle.putString(BluetoothService.KEY_DEVICE_NAME, device.getName());

        msg.setData(bundle);

        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        setState(STATE_NONE);

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        /* create temporary object */
        ConnectedThread r;

        /* synchronize a copy of the ConnectedThread */
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        Message msg = mHandler.obtainMessage(BluetoothService.MSG_TOAST);

        Bundle bundle = new Bundle();
        bundle.putString(BluetoothService.KEY_TOAST_MSG, "Unable to connect device");

        msg.setData(bundle);

        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity
     */
    private void connectionLost() {
        Message msg = mHandler.obtainMessage(BluetoothService.MSG_TOAST);

        Bundle bundle = new Bundle();
        bundle.putString(BluetoothService.KEY_TOAST_MSG, "Device connection was lost");

        msg.setData(bundle);

        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled)
     */
    private class AcceptThread extends Thread {
        /* the local server socket */
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            /* create a new listening server socket */
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket;

            /* listen to the server socket if we're not connected */
            while (mState != STATE_CONNECTED) {
                try {
                    /* this is a blocking call and will only return on a successful connection or an exception */
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(), "accept() failed", e);
                    break;
                }

                /* if a connection was accepted */
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                /* situation normal. Start the connected thread */
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                /* either not ready or already connected. Terminate new socket. */
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(getClass().getSimpleName(), "Could not close unwanted socket", e);
                                }

                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            /* get a BluetoothSocket for a connection with the given BluetoothDevice */
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "create() failed", e);
            }

            mmSocket = tmp;
        }

        @Override
        public void run() {
            setName("ConnectThread");

            /* always cancel discovery because it will slow down a connection */
            mAdapter.cancelDiscovery();

            /* make a connection to the BluetoothSocket */
            try {
                /* blocking call and only return on successful connection/exception */
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();

                /* close the socket */
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(getClass().getSimpleName(), "unable to close() socket during connection failure", e2);
                }

                /* start the service over to restart listening mode */
                BluetoothService.this.start();

                return;
            }

            /* reset the ConnectThread because we're done */
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            /* start the connected thread */
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device
     * It handles all incoming and outgoing transmissions
     */
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            /* get the BluetoothSocket input and output streams */
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            int bytes;

            /* keep listening to the InputStream while connected */
            while (true) {
                try {
                    byte[] buffer = new byte[256];

                    /* read from the InputStream */
                    bytes = mmInStream.read(buffer);

                    if (bytes > 0) {
                        /* send the obtained bytes to the UI Activity */
                        mHandler.obtainMessage(BluetoothService.MSG_READ, bytes, -1, buffer).sendToTarget();
                    } else {
                        connectionLost();

                        if (mState != STATE_NONE) {
                            /* start the service over to restart listening mode */
                            BluetoothService.this.start();
                        }

                        break;
                    }
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(), "disconnected", e);
                    connectionLost();

                    if (mState != STATE_NONE) {
                        /* start the service over to restart listening mode */
                        BluetoothService.this.start();
                    }

                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream
         *
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                /* cancels Chinese character mode (FS .) */
                mmOutStream.write(0x1C);
                mmOutStream.write(0x2E);

                /* select character code table (ESC t n) - n = 16(0x10) for WPC1252 */
                mmOutStream.write(0x1B);
                mmOutStream.write(0x74);
                mmOutStream.write(0x10);

                /* write and flush buffer */
                mmOutStream.write(buffer);

                /* write data */
                mmOutStream.flush();

                /* share the sent message back to the UI Activity */
                mHandler.obtainMessage(BluetoothService.MSG_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "close() of connect socket failed", e);
            }
        }
    }
}
