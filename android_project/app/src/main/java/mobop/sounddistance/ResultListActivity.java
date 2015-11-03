package mobop.sounddistance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import utilities.FileReadWrite;

public class ResultListActivity extends Activity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    Activity mActivity;

    FileReadWrite measureFile;

    Button btMeasure;
    TextView tvEmptyMeas;

    final CharSequence[] items = { "Rename", "Delete"};
    AlertDialog.Builder builder;
    AlertDialog.Builder helpBuilder;
    AlertDialog mAlertDialog;
    View inflateLayout;
    EditText etRenameMeas;

    int indexToRename;

    List<String> measList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_list);

        mActivity = this;

        measList = new ArrayList<>();
        measureFile = new FileReadWrite();
        measureFile.CreateFile(FileReadWrite.FILE_NAME);

        /*Measure measure = new Measure("test1",1,39);
        measureFile.WriteDatas(measure.measureToString());

        measure = new Measure("test2",2,10,20);
        measureFile.WriteDatas(measure.measureToString());

        measure = new Measure("test3",1,30);
        measureFile.WriteDatas(measure.measureToString());*/

        btMeasure = (Button) findViewById(R.id.btResultList);
        expListView = (ExpandableListView) findViewById(R.id.lvExpList);
        tvEmptyMeas = (TextView) findViewById(R.id.tvNoMeasure);
        inflateLayout = getLayoutInflater().inflate(R.layout.rename_action_popup, null);
        etRenameMeas = (EditText) inflateLayout.findViewById(R.id.etRename);

        indexToRename = -1;

        btMeasure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MeasureChoiceActivity.class));
            }
        });

        builder = new AlertDialog.Builder(this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == 0)
                {
                    mAlertDialog.show();
                }
                else
                {
                    Log.e("hello", "Delete");
                    measureFile.deleteALineFromFile(indexToRename);
                    prepareListData();
                    listAdapter = new ExpandableListAdapter(mActivity, listDataHeader, listDataChild);
                    expListView.setAdapter(listAdapter);
                    listAdapter.notifyDataSetChanged();
                }
            }
        });

        helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setView(inflateLayout);
        helpBuilder.setTitle("Rename measure");
        helpBuilder.setPositiveButton("Apply",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if(indexToRename != -1)
                        {
                            Log.e("hello", etRenameMeas.getText().toString());
                            String[] meas = measList.get(indexToRename).split(",");
                            meas[0] = etRenameMeas.getText().toString();
                            measureFile.replaceALineFromFile(indexToRename,meas[0]+","+meas[1]+","+meas[2]+","+
                                    meas[3]+","+meas[4]+","+meas[5]+"\n");
                            indexToRename = -1;
                        }
                        prepareListData();
                        listAdapter = new ExpandableListAdapter(mActivity, listDataHeader, listDataChild);
                        expListView.setAdapter(listAdapter);
                        listAdapter.notifyDataSetChanged();
                        etRenameMeas.setText("");
                    }
                });
        helpBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Log.e("hello", "Cancel");
                        etRenameMeas.setText("");
                    }
                });
        mAlertDialog = helpBuilder.create();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e("hello", "list");
        if(measureFile.getFile().length() == 0)
        {
            tvEmptyMeas.setVisibility(View.VISIBLE);
            expListView.setVisibility(View.GONE);
        }
        else
        {
            tvEmptyMeas.setVisibility(View.GONE);
            expListView.setVisibility(View.VISIBLE);
            // preparing list data
            prepareListData();
            listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
            // setting list adapter
            expListView.setAdapter(listAdapter);
            expListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if(ExpandableListView.getPackedPositionType(id)==ExpandableListView.PACKED_POSITION_TYPE_GROUP)
                    {
                        indexToRename = position;
                        builder.show();
                    }
                    return false;
                }
            });
        }
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();
        List<String> child;
        String meas;
        measureFile = new FileReadWrite();
        measureFile.CreateFile(FileReadWrite.FILE_NAME);
        do {
            child = new ArrayList<>();
            meas = measureFile.ReadDatas();
            if(meas != null)
            {
                measList.add(meas);
                String[] array = meas.split(",");
                if(array.length == 6)
                {
                    listDataHeader.add(array[0]);
                    child.add("Measure type : "+array[1]);
                    if(Integer.valueOf(array[2])>0)
                        child.add("x: "+array[2]);
                    if(Integer.valueOf(array[3])>0)
                        child.add("y: "+array[3]);
                    if(Integer.valueOf(array[4])>0)
                        child.add("z: "+array[4]);
                    child.add("Result: "+array[5]);
                    listDataChild.put(array[0],child);
                }
            }
        }while(meas!=null);

        if(measList.size() == 0)
        {
            tvEmptyMeas.setVisibility(View.VISIBLE);
            expListView.setVisibility(View.GONE);
        }
        /*// Adding child data
        listDataHeader.add("Top 250");
        listDataHeader.add("Now Showing");
        listDataHeader.add("Coming Soon..");

        // Adding child data
        List<String> top250 = new ArrayList<String>();
        top250.add("The Shawshank Redemption");
        top250.add("The Godfather");
        top250.add("The Godfather: Part II");
        top250.add("Pulp Fiction");
        top250.add("The Good, the Bad and the Ugly");
        top250.add("The Dark Knight");
        top250.add("12 Angry Men");

        List<String> nowShowing = new ArrayList<String>();
        nowShowing.add("The Conjuring");
        nowShowing.add("Despicable Me 2");
        nowShowing.add("Turbo");
        nowShowing.add("Grown Ups 2");
        nowShowing.add("Red 2");
        nowShowing.add("The Wolverine");

        List<String> comingSoon = new ArrayList<String>();
        comingSoon.add("2 Guns");
        comingSoon.add("The Smurfs 2");
        comingSoon.add("The Spectacular Now");
        comingSoon.add("The Canyons");
        comingSoon.add("Europa Report");

        listDataChild.put(listDataHeader.get(0), top250); // Header, Child data
        listDataChild.put(listDataHeader.get(1), nowShowing);
        listDataChild.put(listDataHeader.get(2), comingSoon);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_result_list, menu);
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
