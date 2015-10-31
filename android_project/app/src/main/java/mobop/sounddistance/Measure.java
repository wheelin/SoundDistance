package mobop.sounddistance;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by greg on 26.10.15.
 */
public class Measure {
    private final static String TYPE_TAG = "type_tag_meas";
    private int _measureType = 1;

    private int _xDim = -1;
    private int _yDim = -1;
    private int _zDim = -1;

    private int _mainResult = -1;

    private String _measName = "";
    private Calendar _date;

    public Measure(String measName, int measType, int...dim){
        Calendar c = Calendar.getInstance();
        set_measName(measName);
        set_measureType(measType);
        _date = new GregorianCalendar(c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR),
                c.get(Calendar.MINUTE));
        switch (dim.length){
            case 0:
                set_xDim(-1);
                set_yDim(-1);
                set_zDim(-1);
                set_mainResult(-1);
            case 1:
                set_xDim(dim[0]);
                set_yDim(-1);
                set_zDim(-1);
                set_mainResult(dim[0]);
                break;
            case 2:
                set_xDim(dim[0]);
                set_yDim(dim[1]);
                set_zDim(-1);
                set_mainResult(dim[0]*dim[1]);
                break;
            case 3:
                set_xDim(dim[0]);
                set_yDim(dim[1]);
                set_zDim(dim[2]);
                set_mainResult(dim[0]*dim[1]*dim[2]);
                break;
        }
    }

    public int get_measureType() {
        return _measureType;
    }

    public int get_xDim() {
        return _xDim;
    }

    public int get_yDim() {
        return _yDim;
    }

    public int get_zDim() {
        return _zDim;
    }

    public String get_measName() {
        return _measName;
    }

    public int get_mainResult() {
        return _mainResult;
    }

    public boolean set_measureType(int _measureType) {
        if (_measureType < 0 || _measureType > 3)
            return false;
        else {
            this._measureType = _measureType;
            return true;
        }
    }

    public boolean set_xDim(int xDim) {
        if (xDim < 20 || xDim > 4000)
            return true;
        else {
            this._xDim = xDim;
            return true;
        }
    }

    public boolean set_yDim(int yDim) {
        if (yDim < 20 || yDim > 4000)
            return false;
        else {
            this._yDim = yDim;
            return true;
        }
    }

    public boolean set_zDim(int zDim) {
        if (zDim < 20 || zDim > 4000)
            return false;
        else {
            this._zDim = zDim;
            return true;
        }
    }

    public void set_measName(String _measName) {
        this._measName = _measName;
    }

    public void set_mainResult(int _mainResult) {
        this._mainResult = _mainResult;
    }

    public String toString(){
        String mt = "";
        switch (_measureType){
            case 1:
                mt = "Distance";
                break;
            case 2:
                mt = "Area";
                break;
            case 3:
                mt = "Volume";
                break;
            default:
                break;
        }
        return _measName +
                mt + "," +
                String.valueOf(this._xDim) + "," +
                String.valueOf(_yDim) + "," +
                String.valueOf(_zDim) + "," +
                _date.toString();
    }

    public static void saveTypePreferences(int type, Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt(TYPE_TAG, type);
        editor.apply();
    }

    public static int loadTypePreferences(Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        return sharedPref.getInt(TYPE_TAG,-1);
    }
}
