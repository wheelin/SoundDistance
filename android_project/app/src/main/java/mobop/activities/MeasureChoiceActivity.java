package mobop.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

import mobop.sounddistance.R;
import utilities.Measure;

public class MeasureChoiceActivity extends Activity {

    private RadioButton rbDistance;
    private RadioButton rbSurface;
    private RadioButton rbVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_choice);

        Button btApply = (Button) findViewById(R.id.btApplyChoice);
        rbDistance = (RadioButton) findViewById(R.id.rbDistance);
        rbSurface = (RadioButton) findViewById(R.id.rbSurface);
        rbVolume = (RadioButton) findViewById(R.id.rbVolumes);

        btApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int type = 0;
                if (rbDistance.isChecked())
                    type = 1;
                else if (rbSurface.isChecked())
                    type = 2;
                else if (rbVolume.isChecked())
                    type = 3;
                Measure.saveTypePreferences(type, getApplicationContext());

                startActivity(new Intent(getApplicationContext(), BluetoothActivity.class));
            }
        });
    }
}
