package mobop.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import mobop.sounddistance.R;
import utilities.FileReadWrite;

/** \brief
 *  Show the measure of the the index passed by intent
 *  Display is different for volume, distance and area
 * 	\author	Emilie Gsponer
 * 	28.12.2015
 */
public class MeasureResultActivity extends Activity {

    public final static String IndexMeasTag = "indexMeas";

    private int index;

    private Button btReturn;
    private TextView tvMeasureType;
    private ImageView ivDistance;
    private ImageView ivVolume;
    private ImageView ivSquare;
    private TextView tvXMeas;
    private TextView tvYMeas;
    private TextView tvZMeas;
    private TextView tvTitle;

    private FileReadWrite measureFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_result);

        //Get file index to display
        Intent intent = getIntent();
        index = intent.getIntExtra(IndexMeasTag,0);

        btReturn = (Button) findViewById(R.id.btReturn);
        btReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        tvMeasureType = (TextView) findViewById(R.id.tvMeasureType);
        tvXMeas = (TextView) findViewById(R.id.tvXMeas);
        tvYMeas = (TextView) findViewById(R.id.tvYMeas);
        tvZMeas = (TextView) findViewById(R.id.tvZMeas);
        tvTitle = (TextView) findViewById(R.id.tvMeasureTitle);

        ivDistance = (ImageView) findViewById(R.id.ivDistance);
        ivSquare = (ImageView) findViewById(R.id.ivSquare);
        ivVolume = (ImageView) findViewById(R.id.ivVolume);

        measureFile = new FileReadWrite();
        measureFile.CreateFile(FileReadWrite.FILE_NAME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int i=-1;

        //Read the file at given index
        String result;
        do
        {
            result = measureFile.ReadDatas();
            i++;
        }while(i<index && result != null);
        if(result!=null)
        {
            String[] array = result.split(",");
            if(array.length == 6) {
                tvTitle.setText(array[0]);

                //Configure display according to measure type
                switch (array[1]) {
                    case "Distance":
                        tvMeasureType.setText(array[1] + ": " + array[5] + " mm");
                        ivDistance.setVisibility(View.VISIBLE);
                        ivVolume.setVisibility(View.GONE);
                        ivSquare.setVisibility(View.GONE);
                        tvXMeas.setVisibility(View.VISIBLE);
                        tvXMeas.setText("x: " + array[2] + " mm");
                        tvYMeas.setVisibility(View.GONE);
                        tvZMeas.setVisibility(View.GONE);
                        break;
                    case "Area":
                        tvMeasureType.setText(array[1] + ": " + array[5] + " mm2");
                        ivDistance.setVisibility(View.GONE);
                        ivVolume.setVisibility(View.GONE);
                        ivSquare.setVisibility(View.VISIBLE);
                        tvXMeas.setVisibility(View.VISIBLE);
                        tvXMeas.setText("x: " + array[2] + " mm");
                        tvYMeas.setVisibility(View.VISIBLE);
                        tvYMeas.setText("y: " + array[3] + " mm");
                        tvZMeas.setVisibility(View.GONE);
                        break;
                    case "Volume":
                        tvMeasureType.setText(array[1] + ": " + array[5] + " mm3");
                        ivDistance.setVisibility(View.GONE);
                        ivVolume.setVisibility(View.VISIBLE);
                        ivSquare.setVisibility(View.GONE);
                        tvXMeas.setVisibility(View.VISIBLE);
                        tvXMeas.setText("x: " + array[2] + " mm");
                        tvYMeas.setVisibility(View.VISIBLE);
                        tvYMeas.setText("y: " + array[3] + " mm");
                        tvZMeas.setVisibility(View.VISIBLE);
                        tvZMeas.setText("z: " + array[4] + " mm");
                        break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
