package mobop.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mobop.sounddistance.R;
import utilities.ExpandableListAdapter;
import utilities.FileReadWrite;

/** \brief
 *  Display the list of measure stored on the external file
 * 	\author	Emilie Gsponer
 * 	17.11.2015
 */
public class ResultListActivity extends Activity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    Activity mActivity;

    FileReadWrite measureFile;

    Button btMeasure;
    TextView tvEmptyMeas;

    final CharSequence[] items = { "Rename", "View", "Delete"};
    AlertDialog.Builder builder;
    AlertDialog.Builder helpBuilder;
    AlertDialog mAlertDialog;
    View inflateLayout;
    EditText etRenameMeas;

    int indexToRename;

    List<String> measList;
    private int lastExpandedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_list);

        mActivity = this;

        measList = new ArrayList<>();
        measureFile = new FileReadWrite();
        measureFile.CreateFile(FileReadWrite.FILE_NAME);

        //Insert fake measure in file
        /*Measure measure = new Measure("test1",1,39);
        measureFile.WriteDatas(measure.measureToString());

        measure = new Measure("test2",2,20,50);
        measureFile.WriteDatas(measure.measureToString());

        measure = new Measure("test3",3,30,40,80);
        measureFile.WriteDatas(measure.measureToString());*/

        btMeasure = (Button) findViewById(R.id.btResultList);
        expListView = (ExpandableListView) findViewById(R.id.lvExpList);
        tvEmptyMeas = (TextView) findViewById(R.id.tvNoMeasure);
        inflateLayout = getLayoutInflater().inflate(R.layout.rename_action_popup, null);
        etRenameMeas = (EditText) inflateLayout.findViewById(R.id.etRename);

        indexToRename = -1;

        //Start measure activity
        btMeasure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MeasureChoiceActivity.class));
            }
        });

        //Create long press list item dialog
        builder = new AlertDialog.Builder(this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                //Rename measure
                if(item == 0)
                {
                    mAlertDialog.show();
                }
                //View measure
                else if (item == 1)
                {
                    Intent intent = new Intent(getApplicationContext(), MeasureResultActivity.class);
                    intent.putExtra(MeasureResultActivity.IndexMeasTag,indexToRename);
                    startActivity(intent);
                }
                //Delete measure
                else
                {
                    Log.e("hello", "Delete");
                    if(indexToRename != -1 && indexToRename < measureFile.getFile().length())
                        measureFile.deleteALineFromFile(indexToRename);
                    prepareListData();
                    if(!listDataHeader.isEmpty()) {
                        listAdapter = new ExpandableListAdapter(mActivity, listDataHeader, listDataChild);
                        expListView.setAdapter(listAdapter);
                        listAdapter.notifyDataSetChanged();
                        tvEmptyMeas.setVisibility(View.GONE);
                        expListView.setVisibility(View.VISIBLE);
                    }
                    else {
                        tvEmptyMeas.setVisibility(View.VISIBLE);
                        expListView.setVisibility(View.GONE);
                    }
                }
            }
        });

        //Create dialog to rename the measure
        helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setView(inflateLayout);
        helpBuilder.setTitle("Rename measure");
        helpBuilder.setPositiveButton("Apply",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if(indexToRename != -1 && indexToRename <measList.size())
                        {
                            Log.e("hello", etRenameMeas.getText().toString());
                            String[] meas = measList.get(indexToRename).split(",");
                            meas[0] = etRenameMeas.getText().toString();
                            measureFile.replaceALineFromFile(indexToRename,meas[0]+","+meas[1]+","+meas[2]+","+
                                    meas[3]+","+meas[4]+","+meas[5]);
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

        //No measure message
        if(measureFile.getFile().length() == 0) {
            tvEmptyMeas.setVisibility(View.VISIBLE);
            expListView.setVisibility(View.GONE);
        }
        else {
            tvEmptyMeas.setVisibility(View.GONE);
            expListView.setVisibility(View.VISIBLE);
            // preparing list data
            prepareListData();
            listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
            // setting list adapter
            expListView.setAdapter(listAdapter);
            //Expand only one measure at time
            expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                @Override
                public void onGroupExpand(int groupPosition) {
                    if (lastExpandedPosition != -1
                            && groupPosition != lastExpandedPosition) {
                        expListView.collapseGroup(lastExpandedPosition);
                    }
                    lastExpandedPosition = groupPosition;
                }
            });
            //Get item position on long click
            expListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    int delta = 0;
                    if(lastExpandedPosition<i)
                        delta = adapterView.getCount()-listDataHeader.size();
                    indexToRename = i-delta;
                    builder.show();
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
        //Read each measure to add it in the list
        do {
            child = new ArrayList<>();
            meas = measureFile.ReadDatas();
            if(meas != null)
            {
                measList.add(meas);
                String[] array = meas.split(",");
                //Add child and parent data
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
    }
}
