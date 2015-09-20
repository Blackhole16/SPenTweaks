package net.betabears.android.xposed.mods.sPenTweaks;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/*
 * Thanks http://no-magic.info/development-for-android-os/seekbar-in-preferences.html
 * Script from website modified below.
 */

public class SeekBarPreference extends Preference {

    private final String TAG = getClass().getName();

    private static final int DEFAULT_VALUE = 30;

    private int mMaxValue;
    private int mMinValue;
    private int mInterval;
    public static int mCurrentValue;
    private String mUnitsLeft;
    private String mUnitsRight;
    private SeekBar mSeekBar;

    private TextView mStatusText;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        mSeekBar = new SeekBar(context, attrs);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newValue = progress + mMinValue;
                if(newValue > mMaxValue)
                    newValue = mMaxValue;
                else if(newValue < mMinValue)
                    newValue = mMinValue;
                else if(mInterval != 1 && newValue % mInterval != 0)
                    newValue = Math.round(((float)newValue)/mInterval)*mInterval;
                if(!callChangeListener(newValue)){
                    seekBar.setProgress(mCurrentValue - mMinValue);
                    return;
                }
                mCurrentValue = newValue;
                mStatusText.setText(String.valueOf(newValue));
                persistInt(newValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                notifyChanged();
            }
        });
    }

    private void setValuesFromXml(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
        mMaxValue = array.getInt(R.styleable.SeekBarPreference_android_max, 100);
        mMinValue = array.getInt(R.styleable.SeekBarPreference_min, -100);
        mUnitsLeft = getString(array.getString(R.styleable.SeekBarPreference_unitsLeft), "");
        String units = getString(array.getString(R.styleable.SeekBarPreference_units), "");
        mUnitsRight = getString(array.getString(R.styleable.SeekBarPreference_unitsRight), units);
        mInterval = array.getInt(R.styleable.SeekBarPreference_interval, 5);
        array.recycle();
    }

    private String getString(String value, String defaultValue) {
        if(value == null) value = defaultValue;
        return value;
    }

    @Override
    protected View onCreateView(ViewGroup parent){
        RelativeLayout layout =  null;
        try {
            LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = (RelativeLayout)mInflater.inflate(R.layout.seek_bar_preference, parent, false);
        }
        catch(Exception e)
        {
            Log.e(TAG, "Error creating seek bar preference", e);
        }
        return layout;
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        try
        {
            ViewParent oldContainer = mSeekBar.getParent();
            ViewGroup newContainer = (ViewGroup) view.findViewById(R.id.seekBarPrefBarContainer);
            if (oldContainer != newContainer) {
                if (oldContainer != null) {
                    ((ViewGroup) oldContainer).removeView(mSeekBar);
                }
                newContainer.removeAllViews();
                newContainer.addView(mSeekBar, ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
        catch(Exception ex) {
            Log.e(TAG, "Error binding view: " + ex.toString());
        }
        updateView(view);
    }

    protected void updateView(View view) {
        try {
            RelativeLayout layout = (RelativeLayout)view;
            mStatusText = (TextView)layout.findViewById(R.id.seekBarPrefValue);
            mStatusText.setText(String.valueOf(mCurrentValue));
            mStatusText.setMinimumWidth(30);
            mSeekBar.setProgress(mCurrentValue - mMinValue);
            TextView unitsRight = (TextView)layout.findViewById(R.id.seekBarPrefUnitsRight);
            unitsRight.setText(mUnitsRight);
            TextView unitsLeft = (TextView)layout.findViewById(R.id.seekBarPrefUnitsLeft);
            unitsLeft.setText(mUnitsLeft);
        }
        catch(Exception e) {
            Log.e(TAG, "Error updating seek bar preference", e);
        }

    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index){
        int defaultValue = ta.getInt(index, DEFAULT_VALUE);
        return defaultValue;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if(restoreValue) {
            mCurrentValue = getPersistedInt(mCurrentValue);
        }
        else {
            int temp = 0;
            try {
                temp = (Integer)defaultValue;
            }
            catch(Exception ex) {
                Log.e(TAG, "Invalid default value: " + defaultValue.toString());
            }

            persistInt(temp);
            mCurrentValue = temp;
        }
    }

}