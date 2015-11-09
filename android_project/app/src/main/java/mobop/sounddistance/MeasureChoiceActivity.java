package mobop.sounddistance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_measure_choice, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
