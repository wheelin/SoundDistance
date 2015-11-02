package mobop.sounddistance;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

import utilities.TextToSpeechController;

public class MeasuringActivity extends Activity {
    TextToSpeech tts = null;

    ImageView img = null;
    private int measureNum = 0;
    private int currentMeasureNum = 1;
    Handler h;
    Button bt;

    boolean nextIteration = false;

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
    }

    public void takeMeasure(View view){
        String[] strArray = {getString(R.string.first_measure_speaking),
                getString(R.string.second_measure_speaking),
                getString(R.string.third_measure_speaking),
                getString(R.string.finished_measure_speaking)};
        /*h.postDelayed(new Runnable() {
            public void run() {
                setNextIterationTrue();
            }
        }, 5000);*/
        switch (currentMeasureNum){
            case 1:
                tts.speak(strArray[0], TextToSpeech.QUEUE_FLUSH, null, null);
                //while (nextIteration != true);
                nextIteration = false;
                if(measureNum == 1){
                    tts.speak(strArray[3], TextToSpeech.QUEUE_ADD, null, null);
                    bt.setText("Finish");
                    currentMeasureNum += 3;
                }
                break;
            case 2:
                if(measureNum > 1) {
                    tts.speak(strArray[1], TextToSpeech.QUEUE_FLUSH, null, null);
                    changeImage(1);
                    //while (nextIteration != true);
                    nextIteration = false;
                }
                if(measureNum == 2){
                    tts.speak(strArray[3], TextToSpeech.QUEUE_ADD, null, null);
                    bt.setText("Finish");
                    currentMeasureNum += 3;
                }
                break;
            case 3:
                if(measureNum == 3) {
                    tts.speak(strArray[2], TextToSpeech.QUEUE_FLUSH, null, null);
                    changeImage(2);
                    //while (nextIteration != true);
                    tts.speak(strArray[3], TextToSpeech.QUEUE_ADD, null, null);
                    bt.setText("Finish");
                }
                break;
            default:
                startActivity(new Intent(getApplicationContext(), ResultListActivity.class));
                break;
        }
        currentMeasureNum += 1;
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
}
