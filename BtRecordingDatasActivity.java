package com.pga.bt_standard;

import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pga.ble.BleRecordingDatasActivity;
import com.pga.bluetooth_base.BluetoothActivity;
import com.pga.bluetooth_base.BluetoothObjects;
import com.pga.carmanager.CarManagerActivity;
import com.pga.carmanager.OBDCommandsReader;
import com.pga.carmanager.R;
import com.pga.gps.GpsComm;
import com.pga.login.LoginDatasReadWrite;
import com.pga.mysql.DatasStruct;
import com.pga.mysql.MySQLConnect;

/** \brief
 * <b> Send command to OBD module for recording data in real time</b>
 * <p> This activity use the standard bluetooth communication</p>
 * <p> Send commands of the obd_datas file:
 * <ul>
 * <li>Send ATZ : Reset OBD module</li>
 * <li>Send ATSP0 : Set standard protocol </li>
 * <li>Send 010D : Ask current speed, receive 410D xx</li>
 * <li>Send 0146 : Ask current ambiant temperature, receive 4146 xx</li>
 * <li>Send 012F : Ask current fuel level, receive 412F xx</li>
 * <li>Send 010C : Ask current engine rpm, receive 410C xx xx</li>
 * <li>Send 015E : Ask current fuel rate, receive 415E xx xx</li>
 * <li>Send 0110 : Ask current Co2 ejected, receive 4110 xx xx</li>
 * </ul>
 * </p>
 * 	\author	Emilie Gsponer
 * 	\version 1.0 - 17.01.2014  
 */

public class BtRecordingDatasActivity extends Activity implements OnClickListener
{
	//////////////////////////////////////////////////////////////////////////
	/* MEMBER FIELDS													  	*/
	//////////////////////////////////////////////////////////////////////////
	
	/* PUBLIC MEMBERS														*/
	public final static int EXTRA_MESSAGE = 40;						///< Used for ididentify Gps activity response								
	public static final int TICK_DATABASE = 10000;					///< Datas are send to database each 10s
	public static final String RECORD_ACTIVITY 
				= BleRecordingDatasActivity.class.getSimpleName();	///< Tag used by the bluetooth activity for starting record activity

	/* PRIVATE MEMBERS														*/
	private TextView tvConnect;										///< Show the connected device
	private TextView tvGPSX;										///< Show Gps latitude
	private TextView tvGPSY;										///< Show Gps longitude
	private TextView tvSpeed;										///< Show car speed
	private TextView tvTemp;										///< Show car ambiant temperature
	private TextView tvFuelLvl;										///< Show car fuel level
	private TextView tvRpm;											///< Show car rpm
	private TextView tvFuelRate;									///< Show car fuel rate
	private TextView tvCo2;											///< Show car co2 rejected
	private Button btRecord;										///< Button for start/stop recording
	private DatasStruct dataStruct;									///< structure for database informations
	private GpsComm mGPS;											///< Gps communication object
	private String receivedMessage="";								///< Message received from bluetooth
	private Vector<String> myOBD;									///< OBD commands to send (read in external file)
	private int cmd_nbr = 0;										///< Number of command to send
	private int id;													///< User id for sending datas to database
	private String[] cmd;											///< Parsing of the readed command
	private boolean flag_start;										///< Test if start indicator must be send to database
	private static final int TICK_OBD= 100;							///< Send obd commands each 100 ms
	private boolean config_flag;									///< Test if configuration commands must be send
	private int config_nbr;											///< Number of configure commands to send
	private boolean stop_record;									///< Stop recording datas
	private boolean OBD_valid = true;								///< Test if obd module has send > character
	private Handler tmOBD;											///< Timer for OBD commands
	private Handler tmDataBase;										///< Timer for database sending
	private Runnable runOBD;										///< Runnable for the OBD timer
	private Runnable runDataBase;									///< Runnable for the Database timer
	private int averageSpeed = 0;									///< Average of speeds values
	private int speedNbr = 0;										///< Number of received speed
	private double averageFuelRate = 0;								///< Average of fuel rate values
	private int fuelRateNbr = 0;									///< Number of received fuel rate
	private double averageCO2 = 0;									///< Average of co2 values
	private int co2Nbr = 0;											///< Number of received co2
	
    private final Handler mHandler = new Handler() 
    {
        @Override
        public void handleMessage(Message msg) 
        {
            switch (msg.what) 
            {
            	// IF Gps want start, start gps activity for activate it
            	case GpsComm.HANDLER_ENABLE:
        			Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        			startActivityForResult(callGPSSettingIntent,EXTRA_MESSAGE);
        			finish();
            		break;
        		/* Read received message from bluetooth communication */ 
            	case BtComm.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    receivedMessage += new String(readBuf, 0, msg.arg1);
                    /* Wait to read prompt character*/
                    if(receivedMessage.contains(">"))
                    {
                    	int pos = receivedMessage.indexOf(">");
                		receivedMessage = (String) receivedMessage.subSequence(0, pos);
                		
                		// Test if config phase is finished
                    	if(!config_flag&&cmd !=null)
                    	{
                    		/* Remove useless elements*/
                    		receivedMessage = receivedMessage.replaceAll("\r", "");
                    		receivedMessage = receivedMessage.replaceAll(">", "");
                    		receivedMessage = receivedMessage.replaceAll(" ", "");
                    		// Make response frame for compare to the received frame
	                    	String cmdTag = cmd[1];
	                    	cmdTag = "4" + cmdTag.substring(1);
	                    	cmdTag = cmdTag.replaceAll("\r", "");
	                    	// Compare received frame
	                    	if(receivedMessage.contains(cmdTag))
	                    	{
	                    		// Analyse OBD response
	                    		int temp = receivedMessage.indexOf(cmdTag);
	                    		receivedMessage = receivedMessage.substring(temp+4);
	                    		analyseOBDResponse(receivedMessage);
	                    	}
	                    	cmd_nbr++;
	                    	if(cmd_nbr>=OBDCommandsReader.CMD_NBR)
	                    	{
	                    		cmd_nbr = 0;
	                    	}
	                    }
                    	OBD_valid = true;
                    }
            		break;
        		/* Show toast message */
	            case BtComm.MESSAGE_TOAST:
	                Toast.makeText(getApplicationContext(), msg.getData().getString(BtComm.TOAST),Toast.LENGTH_SHORT).show();
	                if(msg.getData().getString(BtComm.TOAST).contains(BtComm.MSG_LOST))
	                {
						stop_record = true;
						btRecord.setText("Start recording");
						BluetoothObjects.mBtComm.stop();
	                }
	                break;
	             // GPs has new location	
            	case GpsComm.HANDLER_DATAS:
            		// Take the new gps location and draw it
            		if(mGPS!=null && mGPS.getLocation()!=null)
            		{
	            		dataStruct.posX = mGPS.getLocation().getLatitude();
	            		dataStruct.posY = mGPS.getLocation().getLongitude();
	                    tvGPSX.setText("GPS lat : "+String.format("%.2f", mGPS.getLocation().getLatitude()));
	                    tvGPSY.setText("GPS long : "+String.format("%.2f", mGPS.getLocation().getLongitude()));
            		}
            		break;
            }
        }
    }; 	///< Handler used to receive message from the bluetooth communication and GPS
    
    //////////////////////////////////////////////////////////////////////////
	/* CLASS FUNCTIONS													 	*/
    //////////////////////////////////////////////////////////////////////////
    
	/* PUBLIC FUNCTIONS														*/
	/**
	 * \brief
	 * Create object and link with the layout
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		/* Link objects with the layout */
		setContentView(R.layout.activity_record_data);
		tvConnect = (TextView) findViewById(R.id.tvConnected);
		tvSpeed = (TextView) findViewById(R.id.tvSPEED);
		tvTemp = (TextView) findViewById(R.id.tvTEMP);
		tvFuelLvl = (TextView) findViewById(R.id.tvFUEL_LVL);
		tvRpm = (TextView) findViewById(R.id.tvRPM);
		tvFuelRate = (TextView) findViewById(R.id.tvFUEL_RATE);
		tvCo2 = (TextView) findViewById(R.id.tvCO2);
		tvGPSX = (TextView) findViewById(R.id.tvGpsX);
		tvGPSY = (TextView) findViewById(R.id.tvGpsy);
		btRecord = (Button) findViewById(R.id.btRecord);
		
		/* Create new objects */
		mGPS = new GpsComm(this,mHandler,getApplicationContext());
		dataStruct = new DatasStruct();
		tmOBD = new Handler();
		tmDataBase = new Handler();
		runDataBase = new Runnable() 
		{
			   @Override
			   public void run() 
			   {
				   /* Send datas to databse only if GPs correctly started */
				   if(mGPS !=null && mGPS.getLocation()!=null)
				   {
					   /* Test if this is the start of the traject*/
					   if(flag_start)
					   {
						   /* Send flag to database */
						   dataStruct.flag_debut_trajet = 1;
						   flag_start = false;
					   }
					   else
					   {
						   /* Clear flag */
						   dataStruct.flag_debut_trajet = 0;
					   }
					   /* Send value of car to database */
					   if(co2Nbr!=0)
					   		dataStruct.co2_instant = averageCO2/co2Nbr;
					   if(fuelRateNbr!=0)
						   dataStruct.consommation_instant = averageFuelRate/fuelRateNbr;
					   if(speedNbr!=0)
					   		dataStruct.vitesse_instant = averageSpeed/speedNbr;
					   averageCO2 = 0;
					   averageFuelRate = 0;
					   averageSpeed = 0;
					   co2Nbr = 0;
					   fuelRateNbr = 0;
					   speedNbr = 0;
					   boolean bo = false;
					   do
					   {
							try 
							{
								Thread.sleep(100);
							} catch (InterruptedException e) 
							{
		
							}
					   		bo = MySQLConnect.sendDat(dataStruct); 
					   }while(!bo);
				   }
				   /* If user stop record, stop timer, else
				    * continue tick
				    */
				   if(!stop_record)
					   tmDataBase.postDelayed(this, TICK_DATABASE);
			   }
		};
		runOBD = new Runnable() 
		{
			@Override
			public void run() 
			{
				// If OBD send > character
				if(OBD_valid)
				{
					// Send next command, config or recording
					OBD_valid = false;
					if(config_flag)
						configureOBD();
					else
						sendOBDCommand(cmd_nbr);
				}
				/* If user stop record, stop timer, else
				 * continue tick
				 */
				if(!stop_record)
					tmOBD.postDelayed(this, TICK_OBD);
			}
		};
		btRecord.setOnClickListener(this);
		/* Connect with data base */
		MySQLConnect.mysqlConnection();

	}
	/**
	 * \brief
	 * Start GPS location, read and store user id and obd commands to send,
	 * show the name of the connected device
	 */
	@Override
	public void onStart() 
	{
	    super.onStart();
		mGPS.GpsEnable();
		BluetoothObjects.mBtComm.changeHandler(mHandler);
		tvConnect.setText("Connected to : "+BtComm.mDevice.getName());
		myOBD = OBDCommandsReader.ReadDatas(getApplicationContext());
		id = Integer.parseInt(LoginDatasReadWrite.ReadDatas(getApplicationContext(), LoginDatasReadWrite.LOGIN_ID),10);
		dataStruct.id_user = id;
		flag_start = false;
		stop_record = true;
	}
	/**
	 * \brief
	 * Receive result of the gps enable request
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
	    // Check which request we're responding to
	    if (requestCode == EXTRA_MESSAGE) 
	    {
	        // Make sure the request was successful
	        if (resultCode == RESULT_CANCELED) 
	        {
	            mGPS.GpsEnable();
	        }
	    }
	}
	
	/**
	 * \brief
	 * Check if bluetooth is connected, if not start bluetooth activity.
	 * Else start/stop timer for sending OBD commands
	 * 
	 * \param v identifier of clicked button
	 */
	@Override
	public void onClick(View v) 
	{
		if(v.getId() == R.id.btRecord)
		{
			// Check bluetooth state
			if(BluetoothObjects.mBtComm.getState() != BtComm.STATE_CONNECTED)
			{
				BluetoothObjects.mBtComm.stop();
				// Start bluetooth activity
				Intent intent = new Intent(this, BluetoothActivity.class);
				intent.putExtra(RECORD_ACTIVITY, true);
				startActivity(intent);
				finish();
			}
			else
			{
				// If user start recording
				if(btRecord.getText().toString().contains("Start"))
				{
					flag_start = true;
					stop_record = false;
					config_flag = true;
					OBD_valid = true;
					config_nbr = 0;
					cmd_nbr = 0;
					// Start timers
					tmDataBase.postDelayed(runDataBase, TICK_DATABASE);
					tmOBD.postDelayed(runOBD, TICK_OBD);
					btRecord.setText("Stop recording");
				}
				// Else stop recording
				else
				{
					stop_record = true;
					btRecord.setText("Start recording");
				}
			}
		}
	}
	/** 
	 * \brief
	 * Return to CarManager activity
	 */
	@Override
	public void onBackPressed() 
	{
	    super.onBackPressed();
        stop_record = true;
	    Intent returnBack = new Intent(this,CarManagerActivity.class);
	    startActivity(returnBack);
	    finish();
	}
	/**
	 * brief
	 * Stop GPS location
	 */
    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        if(mGPS!=null)
        {
        	mGPS.stop();
        }

    }
	/* PRIVATE FUNCTIONS														*/
    
	/**
	 * \brief
	 * Ask bluetooth characteristic for receiving datas
	 * \param nbr command number to ask
	 */
	private void sendOBDCommand(int nbr)
	{
		cmd = myOBD.get(nbr).split(" ");
		cmd[1]+='\r';
		
		// Send command to OBD module
		BluetoothObjects.mBtComm.write(cmd[1].getBytes());
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
		config_nbr++;
		if(config_nbr > 1)
		{
			config_flag = false;
			config_nbr = 0;
		}
	}

	/**
	 * Analyse the bluetooth response and extract informations
	 * from the frame
	 * \param msg string to analyse
	 */
	private void analyseOBDResponse(String msg)
	{
		int[] receive =  hexStringToByteArray(msg);
		int resultInt = 0;
		double resultdbl = 0.0;
		// Extract result containing in the frame, apply result transformation.
		// The transformation depend of the command size and response size
		if(msg.length()==2 || msg.length()==4)
		{
			if(receive != null)
			{
				if(cmd.length==4 && receive.length==2)
				{
					resultdbl = formatOBDDouble(receive, Integer.parseInt(cmd[2]), Integer.parseInt(cmd[3]));
				}
				else if(cmd.length == 4 && receive.length==1)
				{
					resultInt= formatOBDIntMult(receive[0], Integer.parseInt(cmd[2]), Integer.parseInt(cmd[3]));
				}
				else if(cmd.length==3 && receive.length==1)
				{
					resultInt= formatOBDIntSub(receive[0], Integer.parseInt(cmd[2]));
				}
				else if(cmd.length==2 && receive.length==1)
				{
					resultInt = receive[0];
				}
				// Check which command was send for draw the results and 
				// make average of values
				switch(Integer.parseInt(cmd[0]))
				{
					case 0:	
						tvTemp.setText("Temp. "+resultInt+" °C");
						break;
					case 1:
						averageSpeed += resultInt;
						speedNbr++;
						tvSpeed.setText("Speed: "+resultInt+" km/h");
						break;
					case 2:
						tvRpm.setText("Rpm: "+(int)resultdbl+" rpm");
						break;
					case 3:
						averageFuelRate += resultdbl;
						fuelRateNbr++;
						tvFuelRate.setText("Fuel rate: "+String.format("%.1f",resultdbl)+"l/h");
						break;
					case 4:
						tvFuelLvl.setText("Fuel lvl: "+resultInt+" %");
						break;
					case 5:
						averageCO2 += resultdbl;
						co2Nbr++;
						tvCo2.setText("CO2: "+String.format("%.1f",resultdbl)+" g/s");
						cmd_nbr = 0;
						break;
				}
			}
		}
	}
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
	 * Make a substraction
	 * \param value base value
	 * \param substract substractor
	 * \return the substraction result
	 */
	private int formatOBDIntSub(int value,int substract)
	{
		return value-substract;
	}
	
	/**
	 * \brief
	 * Make a multiplication
	 * \param value base value
	 * \param multiplier value multiplier
	 * \param divider value divide
	 * \return the result of multiplication
	 */
	private int formatOBDIntMult(int value,int multiplier, int divider)
	{
		return (int)(value*multiplier)/divider;
	}
	
	/**
	 * \brief
	 * Make a operation for format result
	 * \param bytes two byte to transform
	 * \param multiplier value multiplier
	 * \param divider value divider
	 * \return the result of the operation
	 */
	private double formatOBDDouble(int[] bytes,int multiplier, int divider)
	{
		return ((bytes[0]*multiplier)+bytes[1])/divider;
	}
}
