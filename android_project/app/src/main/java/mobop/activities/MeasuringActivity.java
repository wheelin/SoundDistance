package mobop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.regex.*;
import java.util.Locale;

import mobop.sounddistance.R;
import utilities.BluetoothObjects;
import utilities.BtComm;
import utilities.FileReadWrite;
import utilities.Measure;


public class MeasuringActivity extends Activity {
    TextToSpeech tts = null;
    FileReadWrite fio;

    Measure meas;
    int numberOfReadings = 0;

    ImageView img = null;
    private int measureNum = 0;
    private int currentMeasureNum = 1;
    Handler h;
    Button bt;
    CheckBox cb;
    private boolean voiceEnabled = false;
    private String[] strArray = new String[4];

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
                            Log.e("Arduino recorded", Integer.toString(m));
                            Log.e("Arduino msg", "Setting x value");
                            break;
                        case 1:
                            meas.set_yDim(m);
                            Log.e("Arduino msg", "Setting y value");
                            break;
                        case 2:
                            meas.set_zDim(m);
                            Log.e("Arduino msg", "Setting z value");
                            break;
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
        cb = (CheckBox) findViewById(R.id.enableVoiceCheckBox);
        meas = new Measure(measureNum);
        fio = new FileReadWrite();

        strArray[0] = getString(R.string.first_measure_speaking);
        strArray[1] = getString(R.string.second_measure_speaking);
        strArray[2] = getString(R.string.third_measure_speaking);
        strArray[3] = getString(R.string.finished_measure_speaking);
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
        {
            Toast.makeText(getApplicationContext(), "Bluetooth error, reconnect", Toast.LENGTH_SHORT).show();
        }
    }

    public void takeMeasure(View view){
        bt.setActivated(false);
        switch (currentMeasureNum){
            case 1:
                if(voiceEnabled)
                    tts.speak(strArray[1], TextToSpeech.QUEUE_FLUSH, null, null);
                nextIteration = false;
                BluetoothObjects.mBtComm.write(("meas"+'\n').getBytes());
                if(measureNum == 1){
                    if(voiceEnabled)
                        tts.speak(strArray[3], TextToSpeech.QUEUE_ADD, null, null);
                    bt.setText("Finish");
                    currentMeasureNum += 3;
                    bt.setActivated(false);
                }
                else changeImage(1);
                break;
            case 2:
                if(measureNum > 1) {
                    if(voiceEnabled)
                        tts.speak(strArray[2], TextToSpeech.QUEUE_FLUSH, null, null);
                    BluetoothObjects.mBtComm.write(("meas" + '\n').getBytes());
                    changeImage(2);
                    nextIteration = false;
                }
                if(measureNum == 2){
                    if(voiceEnabled)
                        tts.speak(strArray[3], TextToSpeech.QUEUE_ADD, null, null);
                    bt.setText("Finish");
                    currentMeasureNum += 3;
                    bt.setActivated(false);
                } else changeImage(2);
                break;
            case 3:
                if(measureNum == 3) {

                    BluetoothObjects.mBtComm.write(("meas" + '\n').getBytes());
                    changeImage(3);
                    if(voiceEnabled)
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
                String name_part = Integer.toString(Calendar.getInstance().get(Calendar.HOUR))+ ":" +
                        Integer.toString(Calendar.getInstance().get(Calendar.MINUTE)) + "_" +
                        Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) + "/" +
                        Integer.toString(Calendar.getInstance().get(Calendar.MONTH)) + "/" +
                        Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
                meas.set_measName("measure" + "_" + name_part);
                Log.e("Arduino measure name", "Measure saved as " + meas.get_measName());
                meas.processMainResult();
                fio.CreateFile(FileReadWrite.FILE_NAME);
                fio.WriteDatas(meas.measureToString());
                fio.CloseFile();

                Intent intent = new Intent(getApplicationContext(), ResultListActivity.class);
                //Clear activity stack
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
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

    public void enableHelpVoice(View view){
        if(cb.isChecked()){
            voiceEnabled = true;
        }
        else if(!cb.isChecked()){
            voiceEnabled = false;
        }
        if(voiceEnabled){
            tts.speak(strArray[currentMeasureNum - 1], TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    public void setNextIterationTrue(){
        this.nextIteration = true;
    }

    private int interpreteResponse(String resp) {
        Log.e("Arduino parsing string", resp);
        Pattern p = Pattern.compile("\\d{3,4}");
        Matcher m = p.matcher(resp);
        boolean found = m.find();
        if (found) {
            Log.e("Arduino parsing", "Pattern matching successful");
            return Integer.parseInt(m.group());
        } else {
            Log.e("Arduino parsing", "Pattern matching failed");
            return -1;
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        BluetoothObjects.mBtComm.stop();
    }
}
