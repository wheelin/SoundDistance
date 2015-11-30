package mobop.sounddistance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
/** \brief
 * <b>Manage bluetooth standard communication</b>
 * <p> 
 * <ul>
 * <li>Connect/disconnect device</li>
 * <li>Use serial communication uuid address</li>
 * <li>Can't accept incomming connexion.Only this application can
 * made the connexion</li>
 * </ul>
 * </p>
 * 	\author	Emilie Gsponer
 * 	\version 1.0 - 17.01.2014  
 */
public class BtComm
{
	//////////////////////////////////////////////////////////////////////////
	/* MEMBER FIELDS													  	*/
	//////////////////////////////////////////////////////////////////////////
	
	/* PUBLIC MEMBERS														*/
    public static final int STATE_NONE = 20;       		///< we're doing nothing
    public static final int STATE_CONNECTING = 21; 		///< now initiating an outgoing connection
    public final static int STATE_CONNECTED = 22;  		///< now connected to a remote device
    public static final int MESSAGE_STATE_CHANGE = 23;	///< Notify connexion change state
    public static final int MESSAGE_READ = 24;			///< Notify something to read
    public static final int MESSAGE_DEVICE_NAME = 26;	///< Notify there is a connected device
    public static final int MESSAGE_TOAST = 25;			///< Notify want to show a toast
    private static int mState = STATE_NONE;				///< Set current connexion state to none
    public static final String DEVICE_NAME = "device_name"; ///< Store the connected device name
    public static final String TOAST = "toast";			///< Tag for toast message
	public static final String MSG_UNABLE = "Unable to connect"; ///< Tag for unable connexion
	public static final String MSG_LOST = "Device connection lost";	///< Tag for connexion lost
    public static BluetoothDevice mDevice;				///< Bluetooth device
    
	/* PRIVATE MEMBERS														*/
    private static final String TAG = "BtComm";			///< Tag used for debbug
    private static final UUID MY_UUID_SECURE
    		= UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); ///< Standard UUID for serial communication
    private static Handler mHandler;					///< Send message to activity
    private static ConnectThread mConnectThread;		///< Thread for the connexion
    private static ConnectedThread mConnectedThread;	///< Thread for the connected device

    private final static BluetoothAdapter mAdapter 
    		= BluetoothAdapter.getDefaultAdapter();
    

    //////////////////////////////////////////////////////////////////////////
	/* CLASS FUNCTIONS													 	*/
    //////////////////////////////////////////////////////////////////////////
    
	/* PUBLIC FUNCTIONS														*/
	/** \brief
	 * Change the receiving handler
	 * \param handler new handler 
	 */
    public void changeHandler(Handler handler)
    {
    	mHandler = handler;
    }
    /** \brief
     * Return the current connection state
     * \return current connection state
     */
    public synchronized int getState() 
    {
        return mState;
    }

    /** \brief
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() 
    {
    	Log.d(TAG, "start");
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
    }
    /** \brief
     * Start the ConnectThread to initiate a connection to a remote device.
     * \param device  The BluetoothDevice to connect
     * \param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device) 
    {
    	Log.d(TAG, "connect to: " + device);
    	mDevice = device;
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) 
        {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /** \brief
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * \param socket  The BluetoothSocket on which the connection was made
     * \param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) 
    {
    	Log.d(TAG, "connected");
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * \brief
     * Stop all threads
     */
    public synchronized void stop() 
    {
    	Log.d(TAG, "stop");
        if (mConnectThread != null) 
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) 
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    /** \brief
     * Write to the ConnectedThread in an unsynchronized manner
     * \param out The bytes to write
     * \see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) 
    {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized(BtComm.class) 
        {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    
	/* PRIVATE FUNCTIONS														*/
    /** \brief
     * Set the current state of the chat connection
     * \param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) 
    {
    	Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        /* Give the new state to the Handler so the UI Activity can update */
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }



    /** \brief
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() 
    {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, MSG_UNABLE);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BtComm.this.start();
    }

    /** \brief
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() 
    {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, MSG_LOST);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BtComm.this.start();
    }
    /** \brief
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread 
    {
        private final BluetoothSocket mmSocket;		///< Socket for bluetooth communication
        private final BluetoothDevice mmDevice;		///< Connected bluetooth device

        /**
         * \brief
         * Create new connect thread
         * \param device device to connect
         */
        public ConnectThread(BluetoothDevice device) 
        {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try 
            {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } 
            catch (IOException e) 
            {
            	Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }
        /**
         * \brief
         * Run the connect thread and connect the socket
         */
        public void run() 
        {
        	Log.i(TAG, "BEGIN mConnectThread ");
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try 
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } 
            catch (IOException e) 
            {
                // Close the socket
                try 
                {
                    mmSocket.close();
                } 
                catch (IOException e2) 
                {
                	Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BtComm.class) 
            {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }
        /**
         * \brief
         * Disconnect the bluetooth socket
         */
        public void cancel() 
        {
            try 
            {
                mmSocket.close();
            } 
            catch (IOException e) 
            {
            	Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /** \brief
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread 
    {
        private final BluetoothSocket mmSocket; ///< Socket for bluetooth communication
        private final InputStream mmInStream;	///< Input stream for incomming datas
        private final OutputStream mmOutStream; ///< Output stream for output datas

        /**
         * \brief
         * Create new thread for connected device
         * \param socket Socket for bluetooth communication
         */
        public ConnectedThread(BluetoothSocket socket) 
        {
        	Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try 
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } 
            catch (IOException e) 
            {
            	Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        /**
         * \brief
         * Run the connected thread
         */
        public void run() 
        {
        	Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) 
            {
                try 
                {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } 
                catch (IOException e) 
                {
                	Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    //start();
                    break;
                }
            }
        }

        /** \brief
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) 
        {
            try 
            {
                mmOutStream.write(buffer);
            } 
            catch (IOException e) 
            {
            	Log.e(TAG, "Exception during write", e);
            }
        }
        /**
         * \brief
         * Stop the connected thread
         */
        public void cancel() 
        {
            try 
            {
                mmSocket.close();
            } 
            catch (IOException e) 
            {
            	Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
