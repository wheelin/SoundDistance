package mobop.sounddistance;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MeasuringActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private BluetoothDevice dev = null;
    private BluetoothSocket socket = null;

    ImageView img = null;
    private int measureNum = 0;

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = "00:00:00:00:00:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measuring);
        img = (ImageView) findViewById(R.id.imageViewAxis);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        dev = btAdapter.getRemoteDevice(address);

        try {
            socket = dev.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (IOException e) {}
        try {
            socket.connect();
            outStream = socket.getOutputStream();
            inStream = socket.getInputStream();
        }
        catch (IOException e) {}
    }

    public void takeMeasure(View view){
        changeImage(4);
        if(measureNum > 0 && measureNum <=3) {
            for (int i = 0; i < measureNum; i++) {
                changeImage(i);
                Byte cmd = Byte.valueOf("measure");
                try {
                    outStream.write(cmd);
                } catch (IOException e) {
                }
                /*if the received value is valid
                * and the measure type is an area or a volum
                * we switch to the nex image*/
            }
        }
        else
        {
            /*fucking error, no correct measure type received from
            config
             */
        }
    }

    private void changeImage(int image_num){
        switch (image_num){
            case 0:
                img.setImageResource(R.drawable.three_axis_x);
                break;
            case 1:
                img.setImageResource(R.drawable.three_axis_y);
                break;
            case 2:
                img.setImageResource(R.drawable.three_axis_z);
                break;
            case 3:
                img.setImageResource(R.drawable.three_axis);
                break;
            default:
                img.setImageResource(R.drawable.three_axis);
                break;
        }
    }
}
