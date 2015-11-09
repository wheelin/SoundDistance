package com.pga.bt_standard;

import java.util.Vector;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.pga.bluetooth_base.BluetoothActivity;
import com.pga.bluetooth_base.BluetoothObjects;
import com.pga.carmanager.CarManagerActivity;
import com.pga.carmanager.DiagnosticDatasReadWrite;
import com.pga.carmanager.R;
import com.pga.carmanager.TroublesCodesReader;
import com.pga.login.LoginDatasReadWrite;
import com.pga.mysql.DatasTroubles;
import com.pga.mysql.MySQLConnect;

/** \brief
 * <b> Send command to OBD module for diagnostic trouble codes</b>
 * <p> This activity use the standard bluetooth communication</p>
 * <p> Send commands :
 * <ul>
 * <li>Send ATZ : Reset OBD module</li>
 * <li>Send ATSP0 : Set standard protocol</li>
 * <li>Send 0101 : Ask number of troubles code</li>
 * <li>Send 03 : Read troubles codes</li>
 * <li>Send 04 : Clear stored troubles code</li>
 * </ul>
 * </p>
 * 	\author	Emilie Gsponer
 * 	\version 1.0 - 17.01.2014  
 */

public class BtDiagnosticActivity extends Activity implements OnClickListener
{    
	//////////////////////////////////////////////////////////////////////////
	/* MEMBER FIELDS													  	*/
	//////////////////////////////////////////////////////////////////////////
	
	/* PUBLIC MEMBERS														*/
	public static final String DIAGNOSTIC_ACTIVITY 
		= BtDiagnosticActivity.class.getSimpleName(); 	///< Tag used by the bluetooth activity for starting diagnostic activity
	public final static int EXTRA_MESSAGE = 30;			///< Tag for extra message
	public final static int MSG_TAG = 31;				///< Tag for message
	
	/* PRIVATE MEMBERS														*/
	private ArrayAdapter<String> listAdapter = null; 	///< Contains list of troubles codes
	private ListView listView = null; 					///< Display troubles codes
	private Button bDiagnostic;							///< Button for diagnostic troubes codes
	private static String receivedMessage;				///< Contain message received from bluetooth communication
	private static int mId;								///< User id for sending datas to the database
	private static int DTC_ASK = 1;						///< Tag for identify diagnostic
	private static int DTC_ANALYSE = 2;					///< Tag for identify dtc number ask
	private static int dtc_number = 0;					///< Number of troubles codes
	private static boolean dtc_flag = false;			///< True if there is troubles codes, false other
	private static DatasTroubles db_troubles;			///< structure for sending troubles code to data base
	private static int dtc_read_number;					///< Number of readed troubles codes 
	private static Vector<String> db_troubles_array;	///< Store readed troubles codes
	private ProgressDialog ringProgressDialog;			///< Waiting pop up window
	private int config_nbr;								///< Number of config command to send
	private boolean config_flag = true;					///< True of OBD config not finish, false other
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) 
        {
        	switch (msg.what) 
        	{
        		/* Show toast message */
	            case BtComm.MESSAGE_TOAST:
	                Toast.makeText(getApplicationContext(), msg.getData().getString(BtComm.TOAST),Toast.LENGTH_SHORT).show();
	                if(msg.getData().getString(BtComm.TOAST).contains(BtComm.MSG_LOST))
	                {
	                	BluetoothObjects.mBtComm.stop();
	                }
	                break;
                /* Read received message from bluetooth communication */ 
            	case BtComm.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    /* construct a string from the valid bytes in the buffer */
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    receivedMessage += readMessage;
                    /* Wait to read prompt character*/
                    if(receivedMessage.contains(">"))
                    {
                    	/* Remove useless elements*/
                    	int pos = receivedMessage.indexOf(">");
                		receivedMessage = (String) receivedMessage.subSequence(0, pos);
                		receivedMessage = receivedMessage.replaceAll("\r", "");
                		receivedMessage = receivedMessage.replaceAll(">", "");
                		receivedMessage = receivedMessage.replaceAll(" ", "");
                		/* Test if config command must be send */
                		if(config_flag)
                		{
                			configureOBD();
                		}
                		else
                		{
                			/* Chek if message contains receive command 43 */
	                		if(receivedMessage.contains("43"))
	                		{
	                			/* Analyse the response */
	                    		int temp = receivedMessage.indexOf("43");
	                    		receivedMessage = receivedMessage.substring(temp+2);
	                    		analyseOBDResponse(receivedMessage,DTC_ANALYSE);
	                		}
	                		/* Check if message contain command 4101 */
	                		else if(receivedMessage.contains("4101"))
	                		{
	                			/* Analyse the response */
	                    		int temp = receivedMessage.indexOf("4101");
	                    		receivedMessage = receivedMessage.substring(temp+4);
	                    		analyseOBDResponse(receivedMessage,DTC_ASK);
	                		}
                		}
                		/* If there is troubles code, start waiting window */
        				if(dtc_flag)
        				{
        					dtc_flag = false;
        					ringProgressDialog = ProgressDialog.show(BtDiagnosticActivity.this, "Please wait ...", "Loading processing ...", true);
        					
        					/* Write trouble code diagnostic command */
        					BluetoothObjects.mBtComm.write(("03"+'\r').getBytes());
        				}
                    }
            		break;
            }
        }
    }; 	///< Handler used to receive message from the bluetooth communication
    
    //////////////////////////////////////////////////////////////////////////
	/* CLASS FUNCTIONS													 	*/
    //////////////////////////////////////////////////////////////////////////
    
	/* PUBLIC FUNCTIONS														*/
	/**
	 * \brief
	 * Create the activity. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		/* Link objects with layout */
		setContentView(R.layout.activity_diagnostic);
		bDiagnostic = (Button) findViewById(R.id.bDiagnosticA);
		listView = (ListView) findViewById(R.id.lvTroubles);
		
		/* Create objects */
		db_troubles = new DatasTroubles();
		db_troubles_array = new Vector<String>();
		listAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
		
        /* Set to click listener */
		bDiagnostic.setOnClickListener(this);
		listView.setClickable(true);
		listView.setOnItemLongClickListener(new OnItemLongClickListener() 
		{
	        @Override
	        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) 
	        {
	        	/* If an element of the list is selected, remove it from the list,
	        	 * the file and the databse
	        	 */
	        	db_troubles.id_user = mId;
	        	int pos = listAdapter.getItem(position).indexOf(" ");
	        	db_troubles.troubleCode = listAdapter.getItem(position).substring(0, pos);
	        	listAdapter.remove(listAdapter.getItem(position));
				listAdapter.notifyDataSetChanged();
				listView.setAdapter(listAdapter);
	        	DiagnosticDatasReadWrite.removeDatas(getApplicationContext(), position+1);
	        	MySQLConnect.deleteTrouble(db_troubles);
				return false;
	        }
		});
		listView.setAdapter(listAdapter);
		/* Connect to database */
		MySQLConnect.mysqlConnection();

	}
	
	/**
	 * \brief
	 * Read user id in data file. Draw stored troubles codes. And
	 * register BLE receiver
	 */
	@Override
	public void onStart() 
	{
	    super.onStart();
	    // Set text button depending of the bluetooth connexion state
	    if(BluetoothObjects.mBtComm!=null)
	    {
    		if(BluetoothObjects.mBtComm.getState() == BtComm.STATE_CONNECTED)
    		{
    			BluetoothObjects.mBtComm.changeHandler(mHandler);
				bDiagnostic.setText("Diagnostic troubles codes");
    		}
    		else
    		{
    			bDiagnostic.setText("Connect bluetooth");
    		}
	    }
	    else
			bDiagnostic.setText("Connect bluetooth");
	    /* Take user id */
		mId = Integer.parseInt(LoginDatasReadWrite.ReadDatas(getApplicationContext(), LoginDatasReadWrite.LOGIN_ID),10);
	    db_troubles.id_user = mId;
	    config_flag = true;
	    config_nbr = 0;
	    // Draw stored troubles codes
		fillTroublesList();
	}
	/**
	 * \brief
	 * Destroy waiting window and kill activity
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
    /**
     * \brief
     * Return to CarManagerActivity
     */
	@Override
	public void onBackPressed() 
	{
	    super.onBackPressed();
	    Intent returnBack = new Intent(this,CarManagerActivity.class);
	    startActivity(returnBack);
	    finish();
	}
	
	/** 
	 * \brief
	 * When diagnostic button clicked, start bluetooth activity if no connexion
	 * available, else start troubles codes diagnostic
	 */
	@Override
	public void onClick(View v) 
	{	
		if(v.getId() == R.id.bDiagnosticA)
		{
			dtc_number = 0;
			dtc_read_number = 0;
			
			/* If no connection made start bluetooth activity*/
			if(BluetoothObjects.mBtComm == null)
			{
				Intent intent = new Intent(this, BluetoothActivity.class);
				intent.putExtra(DIAGNOSTIC_ACTIVITY, true);
				startActivity(intent);
				finish();
			}
			/* If bluetooth not connected start bluetooth activity*/
			else if(BluetoothObjects.mBtComm.getState() != BtComm.STATE_CONNECTED)
			{
				Intent intent = new Intent(this, BluetoothActivity.class);
				intent.putExtra(DIAGNOSTIC_ACTIVITY, true);
				startActivity(intent);
				finish();
			}
			/* Send OBD configuration command */
			else
			{
				config_flag = true;
				config_nbr = 0;
				configureOBD();
			}
		}
	}
	
	/* PRIVATE FUNCTIONS														*/
    /**
     * \brief
     * Transform string containing multiple bytes in an int array
     * \param s string to transform
     * \return int array corresponding representing byte of the string
     */
	private int[] hexStringToByteArray(String s) 
	{
	    int len = s.length();
	    int[] data = new int[len / 2];
	    if(len%2 == 0)
	    {
		    for (int i = 0; i < len; i += 2) 
		    {
		        data[i/2] = ((Character.digit(s.charAt(i), 16) << 4)+ Character.digit(s.charAt(i+1), 16));
		    }
	    }
	    else
	    {
	    	data = null;
	    }
	    return data;
	}

	/**
	 * \brief
	 * Analyse string received from OBD command and show result
	 * \param msg string to analyse
	 * \param nbr current command number
	 */
	private void analyseOBDResponse(String msg,int nbr)
	{
		// Transfom reveceived message to int array
		int[] receive =  hexStringToByteArray(msg);
		if(nbr == DTC_ANALYSE)
		{
			// Read three troubles codes
			for(int i = 0; dtc_read_number<dtc_number && i<3;i++)
			{
				// Convert the message into a trouble code
				boolean found_flag = false;
				String trouble = convertToTroubleCode(receive[i*2],receive[i*2+1]);
				// Search if the trouble code is already in the external file
				for(int j = 0;j<DiagnosticDatasReadWrite.getTroublesNumber(getApplicationContext());j++)
				{
					if(DiagnosticDatasReadWrite.ReadDatas(getApplicationContext(),j+1).contains(trouble))
					{
						found_flag = true;
					}
				}
				// If the trouble code don't exist, add it in the file and in the list view
				if(!found_flag)
				{
					String troubleComplete = trouble+" :\n"+TroublesCodesReader.ReadTroubleCodeDescription(getApplicationContext(), trouble);
					listAdapter.add(troubleComplete);
					listAdapter.notifyDataSetChanged();
					listView.setAdapter(listAdapter);
					db_troubles_array.add(trouble);
					DiagnosticDatasReadWrite.WriteDatas(getApplicationContext(), troubleComplete);
				}
				dtc_read_number++;
			}
			// If all troubles codes announced have been read
			if(dtc_read_number >= dtc_number)
			{
				dtc_read_number = 0;
				// Send all troubles in the database
				for(int i=0;i<db_troubles_array.size();i++)
				{
					boolean bo = false;
					db_troubles.troubleCode = db_troubles_array.get(i);
					do
					{
						try 
						{
							Thread.sleep(100);
						} catch (InterruptedException e) 
						{
	
						}
						bo = MySQLConnect.sendTrouble(db_troubles);
					}while(!bo);
				}
				// Stop the waiting window
	        	if(ringProgressDialog!=null)
	        	{
	        		ringProgressDialog.dismiss();
	        	}
				db_troubles_array.clear();
				
				// Ask to user to start her car
	        	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
	        	alertDialogBuilder.setMessage("Diagnostic finished...\n\nStart your car please.\n\n\nIs your car enable now?")
	        	.setCancelable(false)
	        	.setPositiveButton("YES",
	        	new DialogInterface.OnClickListener()
	        	{
	        		public void onClick(DialogInterface dialog, int id)
	        		{
	        			// Write the clear troubles codes command
	        			BluetoothObjects.mBtComm.write(("04"+'\n').getBytes());
	        		}
	        	});
	        	AlertDialog alert = alertDialogBuilder.create();
	        	alert.show();
			}
		}
		// If OBD response to a trouble code number ask,
		// save number of troubles codes in a global variable
		else if(nbr == DTC_ASK)
		{
			dtc_number = receive[0] & 0x7F;
			if(dtc_number>0)
				dtc_flag = true;
			else
				dtc_flag = false;
		}
	}
	
	/**
	 * \brief
	 * Send AT commands to OBD module for initiate communication
	 */
	private void configureOBD()
	{
		if(config_nbr == 0)
			// Reset command
			BluetoothObjects.mBtComm.write(("ATZ"+'\r').getBytes());
		else if(config_nbr == 1)
			// Auto detect protocol
			BluetoothObjects.mBtComm.write(("ATSP0"+'\r').getBytes());
		else if(config_nbr == 2)
		{
			// Ask number of troubles codes stored
			BluetoothObjects.mBtComm.write(("0101"+'\r').getBytes());
		}
		config_nbr++;
		if(config_nbr > 2)
		{
			config_flag = false;
			config_nbr = 0;
		}
	}
	
	/**
	 * \brief
	 * Transform received bytes of a touble code in a string
	 * \param byteA first byte of the trouble code
	 * \param byteB second byte of the trouble code
	 * \return string of the trouble code
	 */
	private String convertToTroubleCode(int byteA, int byteB)
	{
		String trouble_code ="";
		switch(byteA>>6)
		{
			case 0:
				trouble_code = "P";
				break;
			case 1:
				trouble_code = "C";
				break;
			case 2:
				trouble_code = "B";
				break;
			case 3:
				trouble_code = "U";
				break;
		}
		trouble_code += Integer.toString((byteA>>4)&0x02,16);
		trouble_code += Integer.toString(byteA&0x0F,16);
		trouble_code += Integer.toString((byteB>>4),16);
		trouble_code += Integer.toString(byteB&0x0F,16);
		return trouble_code;
	}
	
	/**
	 * \brief
	 * Read the troubles codes stored in the external file and draw
	 * it in the view list
	 */
	private void fillTroublesList()
	{
		listAdapter.clear();
		for(int i=0;i<DiagnosticDatasReadWrite.getTroublesNumber(getApplicationContext());i++)
		{
			listAdapter.add(DiagnosticDatasReadWrite.ReadDatas(getApplicationContext(),i+1));
			listAdapter.notifyDataSetChanged();
			listView.setAdapter(listAdapter);
		}
	}
}