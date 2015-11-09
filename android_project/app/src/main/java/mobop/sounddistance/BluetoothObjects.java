package mobop.sounddistance;

/** \brief
 * <b> Contains static bluetooth objects </b>
 * <p> The goal is to have only one bluetooth object in all activities</p>
 * 	\author	Emilie Gsponer
 * 	\version 1.0 - 17.01.2014 
 */

public class BluetoothObjects 
{
	//////////////////////////////////////////////////////////////////////////
	/* MEMBER FIELDS													  	*/
	//////////////////////////////////////////////////////////////////////////
	
	/* PUBLIC MEMBERS														*/
		
	public static BtComm mBtComm = null;		///< Bluetooth object for standard communication
}
