package mobop.sounddistance;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.regex.*;
import java.util.Locale;


public class MeasuringActivity extends Activity {
    TextToSpeech tts = null;

    Measure meas;
    int numberOfReadings = 0;

    ImageView img = null;
    private int measureNum = 0;
    private int currentMeasureNum = 1;
    Handler h;
    Button bt;

    boolean nextIteration = false;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
        		/* Show toast message */
                case BtComm.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(BtComm.TOAST), Toast.LENGTH_SHORT).show();
                    if (msg.getData().getString(BtComm.TOAST).contains(BtComm.MSG_LOST)) {
                        BluetoothObjects.mBtComm.stop();
                    }
                    break;
                /* Read received message from bluetooth communication */
                case BtComm.MESSAGE_READ:
                    bt.setActivated(true);
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.e("Arduino msg", readMessage);
                    int m = interpreteResponse(readMessage);
                    if(m == -1) return;
                    switch (numberOfReadings){
                        case 0:
                            meas.set_xDim(m);
                            break;
                        case 1:
                            meas.set_yDim(m);
                            break;
                        case 2:
                            meas.set_zDim(m);
                    }
                    numberOfReadings++;
                    currentMeasureNum++;
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measuring);
        img = (ImageView) findViewById(R.id.imageViewAxis);


        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });

        measureNum = Measure.loadTypePreferences(getApplicationContext());
        changeImage(0);
        h = new Handler();
        bt = (Button) findViewById(R.id.buttonTakeMeasure);
        meas = new Measure(measureNum);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(BluetoothObjects.mBtComm!=null)
        {
            if(BluetoothObjects.mBtComm.getState() == BtComm.STATE_CONNECTED)
            {
                BluetoothObjects.mBtComm.changeHandler(mHandler);
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Bluetooth error, reconnect", Toast.LENGTH_SHORT).show();
            }
        }
        else
            Toast.makeText(getApplicationContext(),"Bluetooth error, reconnect", Toast.LENGTH_SHORT).show();
    }

    public void takeMeasure(View view){
        String[] strArray = {getString(R.string.first_measure_speaking),
                getString(R.string.second_measure_speaking),
                getString(R.string.third_measure_speaking),
                getString(R.string.finished_measure_speaking)};
        bt.setActivated(false);
        switch (currentMeasureNum){
            case 1:
                tts.speak(strArray[0], TextToSpeech.QUEUE_FLUSH, null, null);
                nextIteration = false;
                BluetoothObjects.mBtComm.write(("MEAS"+'\r').getBytes());
                if(measureNum == 1){
                    tts.speak(strArray[3], TextToSpeech.QUEUE_ADD, null, null);
                    bt.setText("Finish");
                    currentMeasureNum += 3;
                    bt.setActivated(false);
                }
                break;
            case 2:
                if(measureNum > 1) {
                    tts.speak(strArray[1], TextToSpeech.QUEUE_FLUSH, null, null);
                    changeImage(1);
                    nextIteration = false;
                }
                if(measureNum == 2){
                    tts.speak(strArray[3], TextToSpeech.QUEUE_ADD, null, null);
                    bt.setText("Finish");
                    currentMeasureNum += 3;
                    bt.setActivated(false);
                }
                break;
            case 3:
                if(measureNum == 3) {
                    tts.speak(strArray[2], TextToSpeech.QUEUE_FLUSH, null, null);
                    changeImage(2);
                    tts.speak(strArray[3], TextToSpeech.QUEUE_ADD, null, null);
                    bt.setText("Finish");
                    bt.setActivated(false);
                }
                break;
            default:
                /*
                ** Now, we must integrate the measure object in an intent in order to give it
                * to the next activity
                 */
                startActivity(new Intent(getApplicationContext(), ResultListActivity.class));
                break;
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

    public void setNextIterationTrue(){
        this.nextIteration = true;
    }
    private int interpreteResponse(String resp){
        Pattern p;
        Matcher m;
        p = Pattern.compile("[0-9]{1,4}");
        m = p.matcher(resp);
        boolean found = m.matches();
        if(found){
            return Integer.parseInt(m.group());
        }
        else {
            return -1;
        }
    }
}
