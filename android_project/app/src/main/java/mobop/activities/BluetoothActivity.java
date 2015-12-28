package mobop.activities;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.Vector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import mobop.sounddistance.R;
import utilities.BluetoothObjects;
import utilities.BtComm;

/** \brief
 * <b>Show a list of paired bluetooth device </b>
 * <p> Launch a bluetooth connexion when a device is selected </p>
 * <p>
 * Support:
 * <ul>
 * <li>Bluetooth standard</li>
 * </ul>
 * </p>
 * 	\author	Emilie Gsponer
 * 	\version 1.0 - 14.10.2015
 */

public class BluetoothActivity extends Activity
{
    //////////////////////////////////////////////////////////////////////////
	/* MEMBER FIELDS													  	*/
    //////////////////////////////////////////////////////////////////////////

    /* LAYOUT WIDGETS														*/
    private ListView listView = null;					///< Display devices BtListAdapter

    /* PUBLIC MEMBERS														*/
    public static final String TAG = BluetoothActivity.class.getSimpleName(); ///< Tag of the activity for identify it in other activities

	/* PRIVATE MEMBERS														*/

    private static String mDeviceName = null; 					///< Name of the connected BLE device

    private static Intent mIntent = null; 						///< Intent for start other activites

    private BluetoothAdapter btAdapter = null;	    	///< Bluetooth standard connection adapter

    private Vector<BluetoothDevice> btDevices = null;	///< Contains list of bluetooth devices

    private ArrayAdapter<String> devicesListAdapter = null;	///< Contains list of bluetooth devices names and addresses

    private static ProgressDialog ringProgressDialog = null;	///< Waiting dialog pop-up

    private final String WAIT_WINDOW_TITLE ="Please wait ..."; 	///< Title of the wait window

    private final String WAIT_WINDOW_MSG = "Connecting ..."; 	///< Text of the wait window

    private static class IncommingHandler extends Handler {
        private final WeakReference<BluetoothActivity> mActivity;

        public IncommingHandler(BluetoothActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            BluetoothActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case BtComm.MESSAGE_STATE_CHANGE:
                        // Check if bluetooth standard is connected
                        switch (msg.arg1) {
                            case BtComm.STATE_CONNECTED:
                                // Stop the waiting window
                                if (ringProgressDialog != null) {
                                    ringProgressDialog.dismiss();
                                }
                                // Show connected device name in a toast
                                String BLEUTOOTH_CONNECTED = "Connected to ";
                                Toast.makeText(activity.getApplicationContext(), BLEUTOOTH_CONNECTED
                                        + mDeviceName, Toast.LENGTH_SHORT).show();
                                mIntent.putExtra(TAG, mDeviceName);
                                // Start new activity and destroy the current
                                activity.startActivity(mIntent);
                                activity.finish();
                                break;
                        }
                        break;
                    case BtComm.MESSAGE_TOAST:
                        ///Stop the waiting window
                        if (ringProgressDialog != null) {
                            ringProgressDialog.dismiss();
                        }
                        //Show bluetooth message in a toast
                        Toast.makeText(activity.getApplicationContext(), msg.getData().getString(BtComm.TOAST),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }

    private final IncommingHandler mHandler = new IncommingHandler(this);

    //////////////////////////////////////////////////////////////////////////
	/* CLASS FUNCTIONS													 	*/
    //////////////////////////////////////////////////////////////////////////

	/* PUBLIC FUNCTIONS														*/
    /**
     * \brief
     * Create the BluetoothActivity window and activitate bluetooth.
     * 	If bluetooth isn't avaliable on smartphone, the application quit
     * 	\param	savedInstanceState Initialize super constructor
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        /* Set up the window layout */
        setContentView(R.layout.activity_devices);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        /* Check if Bluetooth standard is supported */
        if(btAdapter == null)
        {
            String BT_NOT_SUPPORTED = "Bluetooth standard not supported";
            Toast.makeText(this, BT_NOT_SUPPORTED, Toast.LENGTH_SHORT).show();
        }
        else
        {
        	/* Create bluetooth standard communication objects */
            BluetoothObjects.mBtComm = new BtComm();
        }
        /* Check if no bluetooth is available in the smartphone */
        if(BluetoothObjects.mBtComm == null)
        {
        	/* Notify user and kill application */
            String BLEUTOOTH_NOT_SUPPORTED = "No Bluetooth available";
            Toast.makeText(this, BLEUTOOTH_NOT_SUPPORTED, Toast.LENGTH_LONG).show();
            finish();
        }
        /* If at least one bluetoothh type is supported */
        else
        {
        	/* Create list objects */
            btDevices = new Vector<>();
            devicesListAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);

            /* Initialize layout list view */
            listView = (ListView) findViewById(R.id.btDevices);
            listView.setClickable(true);
            listView.setOnItemClickListener(new OnItemClickListener()
            {
                /* Function called automatically when element of list view is clicked */
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {


                    mIntent = new Intent(BluetoothActivity.this, MeasuringActivity.class);

                    if(BluetoothObjects.mBtComm != null)
                    {
                        /* Connect bluetooth device */
                        mDeviceName = btDevices.elementAt(position).getName();
                        BluetoothObjects.mBtComm.connect(btDevices.elementAt(position));
                    }

	                /* Start the waiting window */
                    ringProgressDialog = ProgressDialog.show(BluetoothActivity.this, WAIT_WINDOW_TITLE, WAIT_WINDOW_MSG, true);
                }
            });
        }
    }

    /**
     * \brief
     * 	Display paired devices and scan BLE devices
     */
    @Override
    public void onStart()
    {
        super.onStart();
        /* Check if at least one bluetooth is supported */
        if(BluetoothObjects.mBtComm != null)
        {
	    	/* Check if bluetooth is enable in smartphone */
            if (!btAdapter.isEnabled())
            {
	        	/* Activate bluetooth without asking permission to user */
                btAdapter.enable();
            }
	        /* Wait activation */
            while(!btAdapter.isEnabled()){}
        }

        /* Initialize list view with "no devices" */
        String NO_DEVICE = "No devices found";
        devicesListAdapter.add(NO_DEVICE);
        listView.setAdapter(devicesListAdapter);

        if(BluetoothObjects.mBtComm != null)
        {
            if(btAdapter.isEnabled())
            {
		        /* Get a set of currently paired devices */
                Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

		        /* If there are paired devices, add each one to the list */
                if (pairedDevices.size() > 0)
                {
                    devicesListAdapter.clear();
                    for (BluetoothDevice device : pairedDevices)
                    {
                        devicesListAdapter.add(device.getName() + "\n" + device.getAddress());
                        btDevices.add(device);
                    }
                }
		        /* Change bluetooth standard handler for receiving messages in this activity */
                BluetoothObjects.mBtComm.changeHandler(mHandler);
            }
        }
    }

    /**
     * \brief
     * 	Stop the bluetooth communication and destroy the application
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        /* Stop wait window */
        if(ringProgressDialog!=null)
        {
            ringProgressDialog.dismiss();
        }
    }
}
